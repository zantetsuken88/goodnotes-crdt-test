package dev.kodobo.crdtgraphchallenge.service;

import dev.kodobo.crdtgraphchallenge.model.Edge;
import dev.kodobo.crdtgraphchallenge.model.Node;
import dev.kodobo.crdtgraphchallenge.model.ReadOnlyGraph;
import dev.kodobo.crdtgraphchallenge.model.State;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class GraphServiceTest {
    private Clock clock;
    private GraphService graphService;
    private State localState;

    @BeforeEach
    void setUp() {
        clock = Clock.systemDefaultZone();
        localState = new State();
        graphService = new GraphService(localState);
    }

    @Test
    public void canBuildGraphFromFile() {
        populateGraphFromFile();

        assertThat(graphService.getGraph().getNodes()).hasSize(4);
        assertThat(localState.getNodesAdded().values())
                .extracting(Node::getLabel)
                .containsExactlyInAnyOrder("node1", "node2", "node3", "node4");
        assertThat(localState.getEdgesAdded())
                .extracting(Edge::getSourceNodeLabel, Edge::getDestinationNodeLabel)
                .containsExactlyInAnyOrder(
                        tuple("node1", "node2"),
                        tuple("node1", "node3"),
                        tuple("node1", "node4"),
                        tuple("node2", "node1"),
                        tuple("node2", "node4"),
                        tuple("node3", "node1"),
                        tuple("node4", "node1"),
                        tuple("node4", "node2")
                );
        assertThat(graphService.getState().getNodesRemoved()).isEmpty();
        assertThat(graphService.getState().getEdgesRemoved()).isEmpty();
    }

    @Test
    public void canAddNode() {
        State state = graphService.getState();
        Node shouldBeNull = state.getAddedNode("Test Add Node");
        assertThat(shouldBeNull).isNull();

        graphService.addNode("Test Add Node", LocalDateTime.now(clock));

        Node result = state.getAddedNode("Test Add Node");
        assertThat(result).isNotNull();
        assertThat(result.getLabel()).isEqualTo("Test Add Node");
    }

    @Test
    public void canAddEdge() {
        State state = graphService.getState();
        String sourceLabel = "Source";
        String destinationLabel = "Destination";
        LocalDateTime now = LocalDateTime.now(clock);

        graphService.addNode(sourceLabel, now);
        graphService.addNode(destinationLabel, now);

        Edge shouldBeNull = state.getAddedEdge(sourceLabel, destinationLabel);
        assertThat(shouldBeNull).isNull();

        graphService.addEdgePair(sourceLabel, destinationLabel, now);

        Edge result = state.getAddedEdge(sourceLabel, destinationLabel);
        Edge reverseRes = state.getAddedEdge(destinationLabel, sourceLabel);

        assertThat(result).isNotNull();
        assertThat(result.getSourceNodeLabel()).isEqualTo(sourceLabel);
        assertThat(result.getDestinationNodeLabel()).isEqualTo(destinationLabel);
        assertThat(reverseRes).isNotNull();
        assertThat(reverseRes.getSourceNodeLabel()).isEqualTo(destinationLabel);
        assertThat(reverseRes.getDestinationNodeLabel()).isEqualTo(sourceLabel);
    }

    @Test
    public void canFetchConnectedNodes() {
        LocalDateTime now = LocalDateTime.now(clock);
        graphService.addNode("source", now);
        String[] connectedNodes = { "conn0", "conn1", "conn2", "conn3", "conn4" };
        Arrays.stream(connectedNodes).forEach(n -> {
            graphService.addNode(n, now);
            graphService.addEdgePair("source", n, now);
        });

        assertThat(graphService.getConnectedNodes("source"))
                .extracting("label")
                .containsExactlyInAnyOrder(connectedNodes);
    }

    @Test
    public void canRemoveNode() {
        String testRemoveNode = "Test Remove Node";
        State state = graphService.getState();

        graphService.addNode(testRemoveNode, LocalDateTime.now(clock));
        Node added = state.getAddedNode(testRemoveNode);

        assertThat(added).isNotNull();
        assertThat(added.getLabel()).isEqualTo(testRemoveNode);

        graphService.removeNode(added, LocalDateTime.now(clock));
        boolean result = graphService.getGraph().hasNode(testRemoveNode);

        assertThat(added.getTimestamp()).isBefore(state.getRemovedNode(testRemoveNode).getTimestamp());
        assertThat(result).isFalse();
    }

    @Test
    public void canRemoveEdge() {
        String source = "source";
        String destination = "destination";
        State state = graphService.getState();

        graphService.addNode(source, LocalDateTime.now(clock));
        graphService.addNode(destination, LocalDateTime.now(clock));
        graphService.addEdgePair(source, destination, LocalDateTime.now(clock));

        Edge added = state.getAddedEdge(source, destination);
        assertThat(added).isNotNull();

        graphService.removeEdgePair(added.getSourceNodeLabel(), added.getDestinationNodeLabel(), LocalDateTime.now(clock));

        boolean result = graphService.getGraph().hasEdge(source, destination);
        LocalDateTime edgeAddedAt = added.getTimestamp();
        LocalDateTime edgeRemovedAt = state.getRemovedEdge(source, destination).getTimestamp();
        assertThat(edgeAddedAt).isBefore(edgeRemovedAt);
        assertThat(result).isFalse();
    }

    @Test
    public void canReAddPreviouslyRemovedNode() {
        graphService.addNode("node", LocalDateTime.now(clock));

        boolean added = graphService.getGraph().hasNode("node");
        Node node = graphService.getGraph().getNode("node");
        assertThat(added).isTrue();
        graphService.removeNode(node, LocalDateTime.now(clock));
        boolean removed = graphService.getGraph().hasNode("node");
        assertThat(removed).isFalse();

        graphService.addNode("node", LocalDateTime.now(clock));
        boolean reAdded = graphService.getGraph().hasNode("node");
        assertThat(reAdded).isTrue();
    }

    @Test
    public void removingNodeRemovesAssociatedEdges() {
        populateGraphFromFile();
        State state = graphService.getState();
        List<Node> node2Edges = graphService.getConnectedNodes("node2");
        List<Node> node4Edges = graphService.getConnectedNodes("node4");
        Edge edge1 = state.getAddedEdge("node2", "node4");
        Node node = state.getAddedNode("node2");
        assertThat(node2Edges).extracting("label").containsExactlyInAnyOrder("node1", "node4");
        assertThat(node4Edges).extracting("label").containsOnlyOnce("node2");
        assertThat(edge1).isNotNull();

        graphService.removeNode(node, LocalDateTime.now(clock));
        List<Node> rmNode4Edges = graphService.getConnectedNodes("node4");
        assertThat(rmNode4Edges).extracting("label").doesNotContain("node2");
    }

    @Test
    public void canFindShortestPathBetweenTwoNodes() {
        populateGraphFromFile();
        ReadOnlyGraph graph = graphService.getGraph();
        List<String> shortestRoute = graph.findShortestRoute("node3", "node4");
        assertThat(shortestRoute).contains("node4", "node1", "node3");
    }

    private void populateGraphFromFile() {
        try {
            String contents = FileUtils.readFileToString(
                    new File(
                            Objects.requireNonNull(
                                            this.getClass().getClassLoader()
                                                    .getResource("testData.txt"))
                                    .toURI()), StandardCharsets.UTF_8);
            // We collect all the edges in a map first because the nodes need to be added before we can add the edges
            Map<String, String> edges = new LinkedHashMap<>();
            Arrays.stream(contents.split("\n"))
                    .forEach(n -> {
                        String[] split = n.split(":");
                        String sourceNode = split[0].strip();
                        graphService.addNode(sourceNode, LocalDateTime.now(clock));
                        edges.put(sourceNode, split[1]);
                    });
            edges.forEach((k, v) -> Arrays.stream(v.split(",")).forEach(e -> graphService.addEdgePair(k, e.strip(), LocalDateTime.now(clock))));
        } catch (Exception e) {
            throw new RuntimeException("Error parsing input file", e);
        }
    }
}
