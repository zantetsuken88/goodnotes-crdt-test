package dev.kodobo.crdtgraphchallenge.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Node {
    private final UUID nodeUuid;
    private final String label;
    private final LocalDateTime timestamp;

    public Node(UUID nodeUid, String label, LocalDateTime timestamp) {
        this.nodeUuid = nodeUid;
        this.label = label;
        this.timestamp = timestamp;
    }

    public UUID getNodeUuid() {
        return nodeUuid;
    }

    public String getLabel() {
        return label;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return label.equals(node.getLabel());
    }

    public boolean equals(String label) {
        return this.label.equals(label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLabel());
    }

    public Node determineLatest(Node comparator) {
        if (comparator == null) {
            return this;
        }
        return comparator.getTimestamp().isAfter(this.getTimestamp())
                ? comparator
                : this;
    }

}
