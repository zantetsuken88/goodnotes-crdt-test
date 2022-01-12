package dev.kodobo.crdtgraphchallenge.model;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class State {
    private final Map<UUID, Node> nodesAdded;
    private final Map<UUID, Node> nodesRemoved;
    private final Set<Edge> edgesAdded;
    private final Set<Edge> edgesRemoved;

    public State() {
        nodesAdded = new LinkedHashMap<>();
        nodesRemoved = new LinkedHashMap<>();
        edgesAdded = new LinkedHashSet<>();
        edgesRemoved = new LinkedHashSet<>();
    }

    public State(
            Map<UUID, Node> nodesAdded,
            Map<UUID, Node> nodesRemoved,
            Set<Edge> edgesAdded,
            Set<Edge> edgesRemoved
    ) {
        this.nodesAdded = nodesAdded;
        this.nodesRemoved = nodesRemoved;
        this.edgesAdded = edgesAdded;
        this.edgesRemoved = edgesRemoved;
    }

    public Map<UUID, Node> getNodesAdded() {
        return nodesAdded;
    }

    public Map<UUID, Node> getNodesRemoved() {
        return nodesRemoved;
    }

    public Set<Edge> getEdgesAdded() {
        return edgesAdded;
    }

    public Set<Edge> getEdgesRemoved() {
        return edgesRemoved;
    }

    public Node getAddedNode(UUID nodeUid) {
        return getNodesAdded().get(nodeUid);
    }

    public Node getAddedNode(String label) {
        return getNodesAdded().values().stream()
                .filter(v -> v.equals(label))
                .findFirst()
                .orElse(null);
    }

    public Node getRemovedNode(UUID nodeUid) {
        return getNodesRemoved().get(nodeUid);
    }

    public Node getRemovedNode(String label) {
        return getNodesRemoved().values().stream()
                .filter(v -> v.equals(label))
                .findFirst()
                .orElse(null);
    }

    public Edge getAddedEdge(String source, String destination) {
        return getEdgesAdded()
                .stream()
                .filter(e -> e.equals(source, destination))
                .findFirst()
                .orElse(null);
    }

    public Edge getRemovedEdge(String source, String destination) {
        return getEdgesRemoved()
                .stream()
                .filter(e -> e.equals(source, destination))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return Objects.equals(nodesAdded, state.nodesAdded) &&
                Objects.equals(nodesRemoved, state.nodesRemoved) &&
                Objects.equals(edgesAdded, state.edgesAdded) &&
                Objects.equals(edgesRemoved, state.edgesRemoved);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodesAdded, nodesRemoved, edgesAdded, edgesRemoved);
    }
}
