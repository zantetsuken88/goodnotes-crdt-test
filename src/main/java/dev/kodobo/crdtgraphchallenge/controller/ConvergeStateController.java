package dev.kodobo.crdtgraphchallenge.controller;

import dev.kodobo.crdtgraphchallenge.model.ReadOnlyGraph;
import dev.kodobo.crdtgraphchallenge.model.State;
import dev.kodobo.crdtgraphchallenge.model.Node;
import dev.kodobo.crdtgraphchallenge.service.ConvergeStateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/kodobo")
public class ConvergeStateController {
    private final ConvergeStateService convergeStateService;

    public ConvergeStateController(ConvergeStateService convergeStateService) {
        this.convergeStateService = convergeStateService;
    }

//    @GetMapping(value="/state")
//    public ResponseEntity<Graph> getState() {
//        Graph state = convergeStateService.getLocalState();
//        return ResponseEntity.ok(state);
//    }

    @GetMapping(value="/result")
    public ResponseEntity<ReadOnlyGraph> getResult() {
        ReadOnlyGraph result = convergeStateService.getResult();
        return ResponseEntity.ok(result);
    }

    @PutMapping(value = "state")
    public void mergeState(@RequestBody State state) {
        convergeStateService.merge(state);
    }
}
