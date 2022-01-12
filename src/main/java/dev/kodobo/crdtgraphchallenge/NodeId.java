package dev.kodobo.crdtgraphchallenge;

import java.util.concurrent.atomic.AtomicInteger;

public class NodeId {
    private static final AtomicInteger COUNT = new AtomicInteger(0);
    private final Integer id;

    public NodeId() {
        id = COUNT.incrementAndGet();
    }

    public Integer getId() {
        return id;
    }
}
