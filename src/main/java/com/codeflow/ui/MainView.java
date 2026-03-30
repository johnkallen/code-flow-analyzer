package com.codeflow.ui;

import com.codeflow.engine.ExecutionEngine;
import com.codeflow.model.FlowNode;
import com.codeflow.model.StepEvent;
import com.codeflow.parser.CodeParser;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class MainView {

    private final BorderPane root = new BorderPane();

    private final TextArea codeEditor = new TextArea();
    private final VariablePanel variablePanel = new VariablePanel();
    private final FlowChartView flowChartView = new FlowChartView();
    private final Label statusLabel = new Label("Paste code and click Analyze.");

    private ExecutionEngine engine;
    private CodeParser.ParseResult lastResult;

    public MainView() {

        codeEditor.setPrefWidth(500);

        SplitPane split = new SplitPane();
        split.getItems().addAll(codeEditor, flowChartView.getView());
        split.setDividerPositions(0.3);

        Button analyzeBtn = new Button("Analyze");
        Button stepBtn = new Button("Step");
        Button fitBtn = new Button("Fit");
        Button exportBtn = new Button("Export");

        HBox controls = new HBox(10, analyzeBtn, stepBtn, fitBtn, statusLabel, exportBtn);
        controls.setStyle("-fx-padding: 10;");

        root.setTop(variablePanel.getView());
        root.setCenter(split);
        root.setBottom(controls);

        analyzeBtn.setOnAction(e -> analyzeCode());
        stepBtn.setOnAction(e -> step());
        fitBtn.setOnAction(e -> flowChartView.fitToScreen());

        variablePanel.setVariableChangeListener((name, value) -> {
            if (engine == null) {
                statusLabel.setText("Analyze code first.");
                return;
            }

            try {
                engine.setVariable(name, value);
                statusLabel.setText("Updated variable: " + name + " = " + value);
            } catch (Exception ex) {
                statusLabel.setText("Failed to update variable: " + ex.getMessage());
                throw ex;
            }
        });
    }

    public Parent getRoot() {
        return root;
    }

    private void analyzeCode() {
        try {
            CodeParser parser = new CodeParser();
            lastResult = parser.parse(codeEditor.getText());

            engine = new ExecutionEngine(lastResult.flowNodes, lastResult.variables);

            variablePanel.updateVariables(engine.getVariables());
            flowChartView.drawFlow(lastResult.flowNodes, lastResult.flowEdges);

            statusLabel.setText("Analyze complete. Nodes: " + lastResult.flowNodes.size());

        } catch (Exception ex) {
            engine = null;
            lastResult = null;

            variablePanel.clear();
            flowChartView.clear();

            statusLabel.setText("Analyze failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void step() {
        if (engine == null) {
            statusLabel.setText("Engine not initialized. Click Analyze first.");
            return;
        }

        StepEvent event = engine.step();

        flowChartView.clearHighlights();
        variablePanel.updateVariables(engine.getVariables());

        if (event.type == StepEvent.StepType.COMPLETE) {
            statusLabel.setText("Execution complete.");
            return;
        }

        if (event.type == StepEvent.StepType.NODE) {
            flowChartView.highlightNode(event.nodeId);
            FlowNode node = flowChartView.findNodeById(lastResult.flowNodes, event.nodeId);
            statusLabel.setText(node == null ? "Ready to execute node" : "Node: " + node.label);
            return;
        }

        if (event.type == StepEvent.StepType.EDGE) {
            flowChartView.highlightEdge(event.edgeFromId, event.edgeToId);
            statusLabel.setText("Transition");
        }
    }
}