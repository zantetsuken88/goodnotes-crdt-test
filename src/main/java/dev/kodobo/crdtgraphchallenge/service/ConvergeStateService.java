package dev.kodobo.crdtgraphchallenge.service;

import dev.kodobo.crdtgraphchallenge.model.ReadOnlyGraph;
import dev.kodobo.crdtgraphchallenge.model.State;
import org.springframework.stereotype.Service;

@Service
public class ConvergeStateService {
    private final GraphService graphService;

    public ConvergeStateService(GraphService graphService) {
        this.graphService = graphService;
    }

    public State getLocalState() {
        return graphService.getState();
    }

    public ReadOnlyGraph getResult() {
        return graphService.getGraph();
    }

    public void merge(State remoteState) {
        graphService.mergeWithRemote(remoteState);
    }
}
