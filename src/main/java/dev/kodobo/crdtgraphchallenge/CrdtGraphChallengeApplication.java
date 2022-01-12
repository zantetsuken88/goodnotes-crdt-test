package dev.kodobo.crdtgraphchallenge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CrdtGraphChallengeApplication {


//    public static void traverseDepthFirst(ReadOnlyGraph set, String root) {
//        Set<String> dfs = set.depthFirstSearch(root);
//        System.out.println("Completed depth-first traversal: ");
//
//        String path = dfs.stream()
//                .map(n -> String.format("[ %s (%s) ]", n, set.getNode(n).getNodeUuid()))
//                .collect(Collectors.joining(" --> "));
//        System.out.print(path + "\n\n");
//    }
//
//    public static void traverseBreadthFirst(ReadOnlyGraph set, String root) {
//        Map<String, VisitedNode> bfs = set.breadthFirstSearch(set, root);
//        System.out.println("Completed breadth-first traversal: ");
//
//        String path = bfs.keySet().stream()
//                .map(e -> String.format("[ %s (%s) ]", e, set.getNode(e).getNodeUuid()))
//                .collect(Collectors.joining(" --> "));
//        System.out.println(path);
//    }

    public static void main(String[] args) {
        SpringApplication.run(CrdtGraphChallengeApplication.class, args);
    }

}
