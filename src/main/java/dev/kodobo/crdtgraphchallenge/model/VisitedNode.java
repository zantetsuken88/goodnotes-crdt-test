package dev.kodobo.crdtgraphchallenge.model;

public class VisitedNode {
    private final Integer distanceFromRoot;
    private final String parentNode;

    VisitedNode(Integer distanceFromRoot, String parentNode) {
        this.distanceFromRoot = distanceFromRoot;
        this.parentNode = parentNode;
    }

    public Integer getDistanceFromRoot() {
        return distanceFromRoot;
    }

    public String getParentNode() {
        return parentNode;
    }
}
