package dev.kodobo.crdtgraphchallenge.service;

import dev.kodobo.crdtgraphchallenge.helpers.FixedIdGenerator;
import dev.kodobo.crdtgraphchallenge.model.Edge;
import dev.kodobo.crdtgraphchallenge.model.ReadOnlyGraph;
import dev.kodobo.crdtgraphchallenge.model.State;
import dev.kodobo.crdtgraphchallenge.model.Node;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GraphService {
    private final State localState;
    private final FixedIdGenerator idGenerator = new FixedIdGenerator();

    public GraphService(State localState) {
        this.localState = localState;
    }

    public ReadOnlyGraph getGraph() {
        List<Node> nodes = unionNodes();
        List<Edge> edges = unionEdges();
        Map<Node, List<Node>> graph = new LinkedHashMap<>();

        nodes.forEach(n -> {
            // Stream through the list of nodes and collect their matching edges to a list
            // whilst asserting that both nodes must exist for there to be an edge that links them.
            // This should allow an edge to be added out of sequence by one replica but not to be displayed
            // by a replica that hasn't received the add node operation yet
            List<Node> nEdges = edges.stream()
                    .filter(e -> e.getSourceNodeLabel().equals(n.getLabel()))
                    .map(e -> getLatestNode(e.getDestinationNodeLabel()))
                    .filter(nodes::contains)
                    .collect(Collectors.toList());
            graph.put(n, nEdges);
        });
        return new ReadOnlyGraph(graph);
    }

    public State getState() {
        return localState;
    }

    public List<Node> getConnectedNodes(String label) {
        return getGraph().getConnectedNodes(label);
    }

    // if previously added, the put operation should replace the Node with a new timestamp to indicate
    // there was a more recent "add" operation. We need to be aware of this in case of concurrent remove operations.
    public void addNode(String label, LocalDateTime timestamp) {
        // The UUID will be fixed based on the provided label - so in this instance there's no need for a separate
        // method to re-add a removed node. However, the UUID field could be useful if the contents of each node
        // were changed to require non-unique contents - in which case we could overload the method and provide a
        // UUID.
        UUID nodeUid = idGenerator.generateId(label);
        localState.getNodesAdded().put(nodeUid, new Node(nodeUid, label, timestamp));
    }

    public void addEdgePair(String sourceNodeLabel, String destinationNodeLabel, LocalDateTime timestamp) {
        addEdge(sourceNodeLabel, destinationNodeLabel, timestamp);
        addEdge(destinationNodeLabel, sourceNodeLabel, timestamp);
    }

    private void addEdge(String sourceNodeLabel, String destinationNodeLabel, LocalDateTime timestamp) {
        // if the edge already exists in the set, we need to remove it first to ensure the values are updated;
        Edge existing = localState.getAddedEdge(sourceNodeLabel, destinationNodeLabel);
        if (existing != null) {
            localState.getEdgesAdded().remove(existing);
        }
        localState.getEdgesAdded().add(new Edge(sourceNodeLabel, destinationNodeLabel, timestamp));
    }

    public void removeNode(Node node, LocalDateTime timestamp) {
        // As with adding - we put the operation in and ask questions later.
        localState.getNodesRemoved().put(node.getNodeUuid(), new Node(node.getNodeUuid(), node.getLabel(), timestamp));
    }

    public void removeEdgePair(String sourceNodeLabel, String destinationNodeLabel, LocalDateTime timestamp) {
        removeEdge(sourceNodeLabel, destinationNodeLabel, timestamp);
        removeEdge(destinationNodeLabel, sourceNodeLabel, timestamp);
    }

    private void removeEdge(String sourceNodeLabel, String destinationNodeLabel, LocalDateTime timestamp) {
        Edge existing = localState.getRemovedEdge(sourceNodeLabel, destinationNodeLabel);
        if (existing != null) {
            localState.getEdgesRemoved().remove(existing);
        }
        localState.getEdgesRemoved().add(new Edge(sourceNodeLabel, destinationNodeLabel, timestamp));
    }

    public void mergeWithRemote(State remote) {
        mergeNodes(localState.getNodesAdded(), remote.getNodesAdded());
        mergeNodes(localState.getNodesRemoved(), remote.getNodesRemoved());
        mergeEdges(localState.getEdgesAdded(), remote.getEdgesAdded());
        mergeEdges(localState.getEdgesRemoved(), remote.getEdgesRemoved());
    }

    private List<Node> unionNodes() {
        Map<UUID, Node> addSet = localState.getNodesAdded();
        Map<UUID, Node> removeSet = localState.getNodesRemoved();

        List<Node> nodes = new LinkedList<>();
        // if the remove set doesn't contain the uuid at all - we consider the node present.
        nodes.addAll(addSet.entrySet()
                .stream()
                .filter(e -> !removeSet.containsKey(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList()));
        // otherwise, if the entry is in the remove set, we must only add if the time added is more recent.
        nodes.addAll(addSet.entrySet()
                .stream()
                .filter(e -> removeSet.containsKey(e.getKey()))
                .filter(e -> e.getValue().getTimestamp().isAfter(localState.getRemovedNode(e.getKey()).getTimestamp()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList()));
        return nodes;
    }

    private List<Edge> unionEdges() {
        Set<Edge> addSet = localState.getEdgesAdded();
        Set<Edge> removeSet = localState.getEdgesRemoved();

        List<Edge> edges = new LinkedList<>();
        edges.addAll(addSet
                .stream()
                .filter(e -> !removeSet.contains(e))
                .collect(Collectors.toList()));
        edges.addAll(addSet
                .stream()
                .filter(removeSet::contains)
                .filter(e -> e.getTimestamp().isAfter(localState.getRemovedEdge(e.getSourceNodeLabel(), e.getDestinationNodeLabel()).getTimestamp()))
                .collect(Collectors.toList()));
        return edges;
    }

    // Get the latest entry of a node from either set. This method is used privately to
    // perform operations on nodes such as add/remove and merge
    private Node getLatestNode(UUID nodeUid) {
        Node node = localState.getAddedNode(nodeUid);
        Node removed = localState.getRemovedNode(nodeUid);
        if (node != null) {
            return node.determineLatest(removed);
        } else {
            return removed;
        }
    }

    private Node getLatestNode(String label) {
        Node node = localState.getAddedNode(label);
        if (node != null) {
            return getLatestNode(node.getNodeUuid());
        }
        return null;
    }

    private Edge getLatestEdge(String source, String destination) {
        Edge edge = localState.getAddedEdge(source, destination);
        Edge removed = localState.getRemovedEdge(source, destination);
        if (edge != null) {
            return edge.determineLatest(removed);
        } else {
            return removed;
        }
    }

    private void mergeNodes(Map<UUID, Node> local, Map<UUID, Node> remote) {
        remote.forEach((k, v) -> {
            if (local.containsValue(v)) {
                local.put(k, v.determineLatest(local.get(k)));
            } else {
                local.put(k, v);
            }
        });
    }

    private void mergeEdges(Set<Edge> local, Set<Edge> remote) {
        remote.forEach(e -> {
            if (local.contains(e)) {
                Edge latest = getLatestEdge(e.getSourceNodeLabel(), e.getDestinationNodeLabel());
                local.remove(e);
                local.add(latest);
            } else {
                local.add(e);
            }
        });
    }
}
