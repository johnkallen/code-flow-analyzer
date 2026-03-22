package com.codeflow.model;


import com.codeflow.enums.NodeType;

public class FlowNode {
    public String id;
    public String label;
    public NodeType type;

    public FlowNode(String id, String label, NodeType type) {
        this.id = id;
        this.label = label;
        this.type = type;
    }
}
