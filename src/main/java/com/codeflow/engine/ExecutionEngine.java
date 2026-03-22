package com.codeflow.engine;


import com.codeflow.model.ExecutionContext;
import com.codeflow.model.FlowNode;

import java.util.List;

public class ExecutionEngine {
    private List<FlowNode> nodes;
    private ExecutionContext context = new ExecutionContext();

    public ExecutionEngine(List<FlowNode> nodes) {
        this.nodes = nodes;
    }

    public FlowNode step() {
        if (context.currentStep >= nodes.size()) return null;

        FlowNode node = nodes.get(context.currentStep);
        context.currentStep++;

        // Simulate execution
        System.out.println("Executing: " + node.label);

        return node;
    }
}
