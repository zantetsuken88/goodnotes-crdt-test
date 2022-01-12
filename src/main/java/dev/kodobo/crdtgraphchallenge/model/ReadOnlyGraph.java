package dev.kodobo.crdtgraphchallenge.model;
import java.util.*;

public class ReadOnlyGraph {
    private final Map<Node, List<Node>> graph;

    public ReadOnlyGraph(Map<Node, List<Node>> graph) {
        this.graph = graph;
    }

    public Map<Node, List<Node>> getGraph() {
        return graph;
    }

    public Node getNode(String label) {
        return graph.keySet()
                .stream()
                .filter(n -> n.getLabel().equals(label))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No nodes matching the given label!"));
    }

    public boolean hasNode(String label) {
        return graph.keySet().stream().anyMatch(n -> n.getLabel().equals(label));
    }

    public boolean hasEdge(String source, String destination) {
        if (hasNode(source) && hasNode(destination)) {
            return getConnectedNodes(source).contains(getNode(destination));
        } else {
            return false;
        }
    }

    public List<Node> getConnectedNodes(String label) {
        return graph.get(getNode(label));
    }

    public List<String> findShortestRoute(String root, String dest) {
        Map<String, VisitedNode> visitedNodes = breadthFirstSearch(root);
        Stack<String> routeStack = new Stack<>();
        List<String> route = new LinkedList<>();
        StringBuilder routeBuilder = new StringBuilder();

        route.add(dest);
        if (visitedNodes.containsKey(dest)) {
            VisitedNode currentNode = visitedNodes.get(dest);
            String parent = currentNode.getParentNode();
            routeStack.push(dest);
            while (!parent.equals("Start")) {
                routeStack.push(parent);
                route.add(parent);
                currentNode = visitedNodes.get(parent);
                parent = currentNode.getParentNode();
            }

            while(!routeStack.empty()) {
                String node = routeStack.pop();
                routeBuilder.append(node);
                if (routeStack.size() > 0 ) {
                   routeBuilder.append(" --> ");
                }
            }
        }
        System.out.println(routeBuilder);
        return route;
    }

    public Set<String> depthFirstSearch(String root) {
        Set<String> visited = new LinkedHashSet<>();
        Stack<String> path = new Stack<>();
        path.push(root);

        while (!path.empty()) {
            String node = path.pop();
            if (!visited.contains(node)) {
                visited.add(node);
                for (Node n : getConnectedNodes(node)) {
                    path.push(n.getLabel());
                }
            }
        }
        return visited;
    }

    private Map<String, VisitedNode> breadthFirstSearch(String root) {
        Map<String, VisitedNode> visited = new LinkedHashMap<>();
        Queue<String> queue = new LinkedList<>();

        queue.add(root);
        visited.put(root, new VisitedNode(0, "Start"));

        while (!queue.isEmpty()) {
            String node = queue.poll();

            for (Node n : getConnectedNodes(node)) {
                if (!visited.containsKey(n.getLabel())) {
                    int distanceFromRoot = visited.get(node).getDistanceFromRoot() + 1;
                    visited.put(n.getLabel(), new VisitedNode(distanceFromRoot, node));
                    queue.add(n.getLabel());
                }
            }
        }
        return visited;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        graph.keySet()
                .forEach(node -> {
                    builder.append(node.getLabel()).append(": [ ");
                    graph.get(node).forEach(conn -> builder.append(conn.getLabel()).append(" "));
                    builder.append("] \n");
                });
        return builder.toString();
    }
}
