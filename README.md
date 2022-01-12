# GoodNotes - CRDT Graph Challenge

Hi, I'm Lee Stewart and welcome to my solution to the GoodNotes tech test. I'll attempt to outline how I've addressed 
each of the requirements below:

---
**Note**

As a personal preference, in my code I refer to vertices as "nodes" as this makes it easier for me to visualise the structure of the graph.

---

## Graph Deliverables:
There are essentially two types of graph implemented in this solution:
 - The `State` class represents a graph as it defined in terms of a CRDT. To quote the Shapiro, Preguica and Zawirski study linked [here](https://hal.inria.fr/inria-00555588/PDF/techreport.pdf) and documented [here](https://github.com/pfrazee/crdt_notes):
> A graph is a pair of sets (V,E) (called vertices and edges respectively) such that E ⊆ V × V.

In order to satisfy the deliverable of a "Last Write Wins" approach to merging replicas, we must base our sets on the LWW-element-set which in turn is a variant on the 2P-set.

> An alternative LWW-based approach, which we call LWW-element-Set, attaches a timestamp to each element (rather than to the whole set). Consider add-set A and remove-set R, each containing (element, timestamp) pairs.

As such, combining these two approaches leads to my solution - the LWW-element-graph, a LWW implementation based on the below-quoted 2P2P-Graph

> A 2P2P-Graph is the combination of two 2P-Sets; as we showed, the dependencies between them are resolved by causal delivery. Dependencies between addEdge and removeEdge, and between addVertex and removeVertex are resolved as in 2P-Set. Therefore, this construct is a CRDT.

Looking at the `State` class, you will see 2 pairs of sets, one each for adds and removals of vertices and edges respectively.

When the `State` class performs a union of these 2 pairs of sets, the results are merged and returned as a `ReadOnlyGraph`:
- The `ReadOnlyGraph` class represents a graph as a typical adjacency list. This implementation is unweighted, and undirected - however the use of classes to represent both nodes and edges allows for scalability to introduce these concepts.

As suggested by the class name, we do not perform operations directly on the `ReadOnlyGraph` - this graph is produced on the fly as a union of the existing `State` as held by the replica. 

The brief outlines the following deliverables which have been addressed by my solution:

- Add a vertex/edge
  - We perform these actions on the `State` via `GraphService:53`.
  - This is tested on `GraphServiceTest:59`.
- Remove a vertex/edge
  - Performed on the `State` via function at `GraphService:76`
  - Tested at `GraphServiceTest:113`
- Check if a vertex is in the graph
  - We are interested in the actual result here, so we check at `ReadOnlyGraph:23`.
  - There's an additional function (not requested) to check for the existence of an edge at `ReadOnlyGraph:27`
  - These functions are used as assertions across `GraphServiceTest` and alongside null-check assertions in the above add/remove tests.
- Query for all vertices connected to a vertex
  - A method is provided at `ReadOnlyGraph:35` - the adjacency list structure makes this quite simple to implement since each node is mapped to a list of connected nodes.
  - This is tested at `GraphServiceTest:104`
- Find any path between two vertices
  - I went a step further here and implemented a _shortest_ path function at `ReadOnlyGraph:39`.
  - This is tested at `GraphServiceTest:199`
- Merge with concurrent changes from other graph/replica
  - This is covered in more detail below.

### commutative, associative, and idempotent.
