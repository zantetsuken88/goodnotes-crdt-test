package dev.kodobo.crdtgraphchallenge.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Edge {
    private final String sourceNodeLabel;
    private final String destinationNodeLabel;
    private final LocalDateTime timestamp;

    public Edge(
            String sourceNodeLabel,
            String destinationNodeLabel,
            LocalDateTime timestamp) {
        this.sourceNodeLabel = sourceNodeLabel;
        this.destinationNodeLabel = destinationNodeLabel;
        this.timestamp = timestamp;
    }

    public String getSourceNodeLabel() {
        return sourceNodeLabel;
    }

    public String getDestinationNodeLabel() {
        return destinationNodeLabel;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return sourceNodeLabel.equals(edge.sourceNodeLabel) && destinationNodeLabel.equals(edge.destinationNodeLabel);
    }

    public boolean equals(String sourceNodeLabel, String destinationNodeLabel) {
        return this.sourceNodeLabel.equals(sourceNodeLabel) && this.destinationNodeLabel.equals(destinationNodeLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceNodeLabel, destinationNodeLabel);
    }

    public Edge determineLatest(Edge comparator) {
        if (comparator == null) {
            return this;
        } else {
            return this.getTimestamp().isAfter(comparator.getTimestamp()) ? this : comparator;
        }
    }
}
