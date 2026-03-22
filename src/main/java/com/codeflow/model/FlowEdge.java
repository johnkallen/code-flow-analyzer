package com.codeflow.model;

public class FlowEdge {
    public String fromId;
    public String toId;
    public String label;

    public FlowEdge(String fromId, String toId, String label) {
        this.fromId = fromId;
        this.toId = toId;
        this.label = label;
    }
}
