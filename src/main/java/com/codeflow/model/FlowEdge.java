package com.codeflow.model;

public class FlowEdge {
    public String from;
    public String to;
    public String label; // "true", "false", etc.

    public FlowEdge(String from, String to, String label) {
        this.from = from;
        this.to = to;
        this.label = label;
    }
}
