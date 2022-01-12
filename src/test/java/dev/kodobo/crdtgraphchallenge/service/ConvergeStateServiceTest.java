package dev.kodobo.crdtgraphchallenge.service;

import dev.kodobo.crdtgraphchallenge.model.Node;
import dev.kodobo.crdtgraphchallenge.model.ReadOnlyGraph;
import dev.kodobo.crdtgraphchallenge.model.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConvergeStateServiceTest {
    private ConvergeStateService convergeStateService;
    private GraphService graphService;
    private State localState = new State();
    private Clock clock;

    private GraphService replicaA;
    private GraphService replicaB;
    private GraphService replicaC;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.EPOCH, ZoneId.systemDefault());
        graphService = new GraphService(localState);
        convergeStateService = new ConvergeStateService(graphService);
        localState = initialState();
    }

    @Test
    public void canGetState() {
        localState = convergeStateService.getLocalState();
        assertThat(localState.getNodesAdded().values())
                .extracting(Node::getLabel)
                .containsExactlyInAnyOrder("one", "two", "three");
        assertThat(localState.getEdgesAdded())
                .hasSize(6);
    }

    @Test
    public void canMergeState() {
        GraphService replica = instantiateReplica();
        Clock replicaClock = Clock.offset(clock, Duration.ofMinutes(1));

        replica.addNode("four", LocalDateTime.now(replicaClock));
        replica.addEdgePair("one", "four", LocalDateTime.now(replicaClock));

        ReadOnlyGraph initial = convergeStateService.getResult();
        assertThat(initial.getGraph().keySet())
                .extracting(Node::getLabel)
                .containsExactlyInAnyOrder("one", "two", "three");
        assertThat(initial.hasEdge("one", "four")).isFalse();

        convergeStateService.merge(replica.getState());

        ReadOnlyGraph result = convergeStateService.getResult();

        assertThat(result.getGraph().keySet())
                .extracting(Node::getLabel)
                .containsExactlyInAnyOrder("one", "two", "three", "four");
        assertThat(result.hasEdge("one", "four")).isTrue();
    }

    @Test
    public void canRetrieveResult() {
        Map<String, String[]> expectedEdges = new LinkedHashMap<>();
        expectedEdges.put("one", new String[] {"two", "three"});
        expectedEdges.put("two", new String[] {"one", "three"});
        expectedEdges.put("three", new String[] {"one", "two"});

        ReadOnlyGraph result = convergeStateService.getResult();
        Map<Node, List<Node>> nodes = result.getGraph();
        assertThat(nodes).hasSize(3);
        assertThat(nodes.keySet())
                .extracting(Node::getLabel)
                .containsExactlyInAnyOrder("one", "two", "three");
        nodes.forEach((n, e) -> assertThat(e)
                .extracting(Node::getLabel)
                .containsExactlyInAnyOrder(expectedEdges.get(n.getLabel())));
    }

    /*
        Thanks to the eventual consistency provided by the Graph, we should be able to guarantee that the same
        operations arriving out of sequence will resolve themselves eventually.
        We'll demonstrate this in the following tests by performing a set of operations between three "replicas" (or in this case,
        simply new instances of GraphService initialised to identical states) which do not naturally commute if
        performed out of sequence.

        Replica A will add two new nodes: "four" and "five", and edges to join them to nodes "one" and "three" respectively.
        Replica B will add an edge between nodes "four" and "five" added by A.
        Replica C will delete the edge between "one" and "two", "one" and "three" and "one" and "five" before finally
        removing node "one"

        Our resulting graph should look as follows regardless of the order
        in which the replica states reach our local state:-
        node: "two" - edges: "three"
        node: "three" - edges: "two", "five"
        node: "four" - edges: "five"
        node: "five" - edges: "three", "four"
        */

    @Test
    public void replicaStatesCanBeReceivedInSequence() {
        // First:
        initialiseMergeTest();
        // Our local state will finally connect to the network and will receive all the replica states in the order
        // they happened.
        convergeStateService.merge(replicaA.getState());
        convergeStateService.merge(replicaB.getState());
        convergeStateService.merge(replicaC.getState());
        ReadOnlyGraph result = convergeStateService.getResult();
        assertThat(result.getGraph().keySet())
                .extracting(Node::getLabel)
                .containsExactlyInAnyOrder("two", "three", "four", "five");

        assertThat(result.getConnectedNodes("two"))
                .extracting(Node::getLabel)
                .containsExactlyInAnyOrder("three");

        assertThat(result.getConnectedNodes("three"))
                .extracting(Node::getLabel)
                .containsExactlyInAnyOrder("two", "five");

        assertThat(result.getConnectedNodes("four"))
                .extracting(Node::getLabel)
                .containsExactlyInAnyOrder("five");

        assertThat(result.getConnectedNodes("five"))
                .extracting(Node::getLabel)
                .containsExactlyInAnyOrder("three", "four");
    }

    @Test
    public void replicaStatesCanBeReceivedOutOfSequence() {
        // first:
        initialiseMergeTest();

        // We receive B's state first which was to add an edge between nodes 4 and 5. This will work because
        // B already merged in A's state, so we receive both updates via B.
        convergeStateService.merge(replicaB.getState());
        ReadOnlyGraph r1 = convergeStateService.getResult();
        assertThat(r1.getGraph().keySet()).extracting(Node::getLabel).containsExactly("one", "two", "three", "four", "five");
        assertThat(r1.hasEdge("four", "five")).isTrue();

        // We now receive C's update which was to remove the edges between "one" and it's connected nodes, then to delete "one",
        // again - we won't have an issue here because we received all of A's updates via B.
        convergeStateService.merge(replicaC.getState());
        ReadOnlyGraph r2 = convergeStateService.getResult();
        assertThat(r2.hasNode("one")).isFalse();
        assertThat(r2.hasEdge("one", "two")).isFalse();
        assertThat(r2.hasEdge("one", "three")).isFalse();
        assertThat(r2.hasEdge("one", "five")).isFalse();

         /* Finally, we receive A's update - but this update is to add in nodes "four" and "five" which already exist,
         it will also try to add an edge between "one" and "five" because A's state doesn't include the removal
         of "one". We should find that the result does not change because the graph resolves a conflict of
         concurrent addition and removal in a last write wins basis. The order we receive the messages won't matter
         because they are timestamped at the time the replica executes the operation.
         We assert below that the results are identical as the previous test. note: the order the elements appear
         in the list may differ, but does not matter in this case since we do not traverse the graph based on index. */

        convergeStateService.merge(replicaA.getState());
        ReadOnlyGraph r3 = convergeStateService.getResult();
        assertThat(r3.getGraph().keySet())
                .extracting(Node::getLabel)
                .containsExactlyInAnyOrder("two", "three", "four", "five");

        assertThat(r3.getConnectedNodes("two"))
                .extracting(Node::getLabel)
                .containsExactlyInAnyOrder("three");

        assertThat(r3.getConnectedNodes("three"))
                .extracting(Node::getLabel)
                .containsExactlyInAnyOrder("two", "five");

        assertThat(r3.getConnectedNodes("four"))
                .extracting(Node::getLabel)
                .containsExactlyInAnyOrder("five");

        assertThat(r3.getConnectedNodes("five"))
                .extracting(Node::getLabel)
                .containsExactlyInAnyOrder("three", "four");
    }

    /*
    source: https://github.com/pfrazee/crdt_notes

    Because of the invariant E ⊆ V × V, operations on vertices and edges are not independent. An edge may be added only
    if the corresponding vertices exist; conversely, a vertex may be removed only if it supports no edge.
    What should happen upon concurrent addEdge(u,v) || removeVertex(u)? We see three possibilities:
    (i) Give precedence to removeVertex(u): all edges to or from u are removed as a side effect. This it is easy to
    implement, by using tombstones for removed vertices.[...] Therefore, we choose Option (i)
    -
    In order to demonstrate the above, we'll set the initial state as normal, add new node "four", and we will then
    add an edge between "two" and "four". We will then have the following operations run concurrently:
    - Locally we will attempt to add another edge between "three" and "four"
    - A replica will attempt to remove the node "four"

    We expect removals to be favoured. As such, our conflict should be resolved with the removal of node "four",
    and by extension the implicit removal of the edges from "four" to both "two" and "three" if it was added.
     */
    @Test
    public void concurrentAddEdgeRemoveNodeFavoursRemove() {
        replicaA = instantiateReplica();
        localState = initialState();
        assertThat(convergeStateService.getResult().getGraph().keySet())
                .extracting(Node::getLabel)
                .containsExactly("one", "two", "three");

        LocalDateTime offsetFive = LocalDateTime.now(Clock.offset(clock, Duration.ofMinutes(5)));
        graphService.addNode("four", offsetFive);
        graphService.addEdgePair("two", "four", offsetFive.plusSeconds(1));

        LocalDateTime offsetSix = LocalDateTime.now(Clock.offset(clock, Duration.ofMinutes(6)));
        Node four = convergeStateService.getResult().getNode("four");
        replicaA.removeNode(four, offsetSix);
        graphService.addEdgePair("three", "four", offsetSix);

        convergeStateService.merge(replicaA.getState());
        ReadOnlyGraph result = convergeStateService.getResult();
        assertThat(result.hasNode("four")).isFalse();
        assertThat(result.hasEdge("two", "four")).isFalse();
        assertThat(result.hasEdge("four", "two")).isFalse();
        assertThat(result.hasEdge("three", "four")).isFalse();
        assertThat(result.hasEdge("four", "three")).isFalse();
    }

    private GraphService instantiateReplica() {
        State remoteState = new State();
        GraphService replica = new GraphService(remoteState);
        initialState(replica);
        return replica;
    }

    private State initialState(GraphService gService) {
        LocalDateTime initialTime = LocalDateTime.now(clock);

        gService.addNode("one", initialTime);
        gService.addNode("two", initialTime);
        gService.addNode("three", initialTime);

        gService.addEdgePair("one", "two", initialTime);
        gService.addEdgePair("one", "three", initialTime);
        gService.addEdgePair("two", "three", initialTime);

        return gService.getState();
    }

    private State initialState() {
        return initialState(graphService);
    }

    private void initialiseMergeTest() {
        replicaA = instantiateReplica();
        replicaB = instantiateReplica();
        replicaC = instantiateReplica();
        localState = initialState();
        assertThat(convergeStateService.getResult().getGraph().keySet())
                .extracting(Node::getLabel)
                .containsExactly("one", "two", "three");

        LocalDateTime offsetFive = LocalDateTime.now(Clock.offset(clock, Duration.ofMinutes(5)));

        replicaA.addNode("four", offsetFive);
        replicaA.addNode("five", offsetFive.plusSeconds(1));
        replicaA.addEdgePair("one", "four", offsetFive.plusSeconds(2));
        replicaA.addEdgePair("three", "five", offsetFive.plusSeconds(3));

        // We'll assume that the connection between replica A and B is good, so the merge should go ahead
        // immediately, and B will receive A's updates before performing operations
        replicaB.mergeWithRemote(replicaA.getState());
        LocalDateTime offsetTen = LocalDateTime.now(Clock.offset(clock, Duration.ofMinutes(10)));
        replicaB.addEdgePair("four", "five", offsetTen);

        // Replica C will also receive all updates
        replicaC.mergeWithRemote(replicaA.getState());
        replicaC.mergeWithRemote(replicaB.getState());
        LocalDateTime offsetTwelve = LocalDateTime.now(Clock.offset(clock, Duration.ofMinutes(12)));
        replicaC.removeEdgePair("one", "two", offsetTwelve.plusSeconds(1));
        replicaC.removeEdgePair("one", "three", offsetTwelve.plusSeconds(2));
        replicaC.removeEdgePair("one", "five", offsetTwelve.plusSeconds(3));
        replicaC.removeNode(replicaC.getGraph().getNode("one"), offsetTwelve.plusSeconds(4));
    }
}
