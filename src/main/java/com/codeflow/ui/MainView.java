package com.codeflow.ui;

import com.codeflow.engine.ExecutionEngine;
import com.codeflow.enums.NodeType;
import com.codeflow.model.FlowEdge;
import com.codeflow.model.FlowNode;
import com.codeflow.model.StepEvent;
import com.codeflow.parser.CodeParser;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainView {
    private BorderPane root = new BorderPane();

    private final TextArea codeEditor = new TextArea();
    private final VBox variablePanel = new VBox();
    private final Pane flowPane = new Pane();
    private final Label statusLabel = new Label("Paste code and click Analyze.");

    private ExecutionEngine engine;
    private CodeParser.ParseResult lastResult;

    private final Map<String, Shape> nodeShapes = new HashMap<>();
    private final Map<String, Line> edgeLines = new HashMap<>();

    public MainView() {

        // Top panel (variables)
        variablePanel.setPrefHeight(120);
        variablePanel.setStyle("-fx-background-color: #eeeeee; -fx-padding: 10; -fx-spacing: 5;");

        flowPane.setPrefSize(1000, 1000);
        flowPane.setStyle("-fx-background-color: white;");
        codeEditor.setPrefWidth(500);

        // Split center
        SplitPane split = new SplitPane();
        split.getItems().addAll(codeEditor, flowPane);
        split.setDividerPositions(0.3);

        // Bottom controls
        Button analyzeBtn = new Button("Analyze");
        Button stepBtn = new Button("Step");
        Button exportBtn = new Button("Export");

        HBox controls = new HBox(10, analyzeBtn, stepBtn, statusLabel, exportBtn);
        controls.setStyle("-fx-padding: 10;");

        root.setTop(variablePanel);
        root.setCenter(split);
        root.setBottom(controls);

        // Hook actions
        analyzeBtn.setOnAction(e -> analyzeCode());
        stepBtn.setOnAction(e -> step());
    }

    public Parent getRoot() {
        return root;
    }

    private void analyzeCode() {
        try {
            CodeParser parser = new CodeParser();
            lastResult = parser.parse(codeEditor.getText());

            engine = new ExecutionEngine(lastResult.flowNodes, lastResult.variables);
            updateVariables(engine.getVariables());

            drawFlow(lastResult.flowNodes, lastResult.flowEdges);

            statusLabel.setText("Analyze complete. Nodes: " + lastResult.flowNodes.size());

        } catch (Exception ex) {
            engine = null;
            lastResult = null;
            variablePanel.getChildren().clear();
            flowPane.getChildren().clear();
            nodeShapes.clear();
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

        clearHighlights();
        updateVariables(engine.getVariables());

        if (event.type == StepEvent.StepType.COMPLETE) {
            statusLabel.setText("Execution complete.");
            return;
        }

        if (event.type == StepEvent.StepType.NODE) {
            highlightNode(event.nodeId);
            FlowNode node = findNodeById(lastResult.flowNodes, event.nodeId);
            statusLabel.setText(node == null ? "Step" : "Node: " + node.label);
            return;
        }

        if (event.type == StepEvent.StepType.EDGE) {
            highlightEdge(event.edgeFromId, event.edgeToId);
            statusLabel.setText("Transition");
        }
    }

    private void clearHighlights() {
        nodeShapes.values().forEach(shape -> {
            shape.setFill(Color.WHITE);
            shape.setStroke(Color.BLACK);
            shape.setStrokeWidth(2);
        });

        edgeLines.values().forEach(line -> {
            line.setStroke(Color.BLACK);
            line.setStrokeWidth(2);
        });
    }

    private void highlightNode(String nodeId) {
        Shape shape = nodeShapes.get(nodeId);
        if (shape != null) {
            shape.setFill(Color.YELLOW);
            shape.setStroke(Color.BLACK);
            shape.setStrokeWidth(2);
        }
    }

    private void highlightEdge(String fromId, String toId) {
        Line line = edgeLines.get(fromId + "->" + toId);
        if (line != null) {
            line.setStroke(Color.RED);
            line.setStrokeWidth(4);
        }
    }

    private void updateVariables(Map<String, Object> vars) {
        variablePanel.getChildren().clear();

        if (vars.isEmpty()) {
            variablePanel.getChildren().add(new Label("No variables found."));
            return;
        }

        vars.forEach((k, v) -> variablePanel.getChildren().add(new Label(k + " = " + v)));
    }



    private void drawFlow(List<FlowNode> nodes, List<FlowEdge> edges) {
        flowPane.getChildren().clear();
        nodeShapes.clear();
        edgeLines.clear();

        for (FlowEdge edge : edges) {
            if (edge.toId == null) {
                continue;
            }

            FlowNode from = findNodeById(nodes, edge.fromId);
            FlowNode to = findNodeById(nodes, edge.toId);

            if (from == null || to == null) {
                continue;
            }

            double fromX = from.x + (from.width / 2);
            double fromY = from.y + from.height;
            double toX = to.x + (to.width / 2);
            double toY = to.y;

            Line line = new Line(fromX, fromY, toX, toY);
            line.setStroke(Color.BLACK);
            line.setStrokeWidth(2);

            flowPane.getChildren().add(line);
            edgeLines.put(edge.key(), line);

            if (edge.label != null && !edge.label.isBlank()) {
                Text edgeText = new Text(edge.label);
                edgeText.setX((fromX + toX) / 2 + 5);
                edgeText.setY((fromY + toY) / 2 - 5);
                flowPane.getChildren().add(edgeText);
            }
        }

        for (FlowNode node : nodes) {
            Shape shape = createShape(node);
            shape.setFill(Color.WHITE);
            shape.setStroke(Color.BLACK);
            shape.setStrokeWidth(2);

            flowPane.getChildren().add(shape);
            nodeShapes.put(node.id, shape);

            // Add text to Shape
            Text text = new Text(node.label);
            text.setWrappingWidth(node.width - 20);
            text.setTextAlignment(TextAlignment.CENTER);

            double textWidth = text.getLayoutBounds().getWidth();
            double textHeight = text.getLayoutBounds().getHeight();

            double textX = node.x + (node.width - Math.min(textWidth, node.width - 20)) / 2;
            double textY = node.y + (node.height / 2) + (textHeight / 4);

            text.setX(textX);
            text.setY(textY);

            flowPane.getChildren().add(text);
        }
    }

    private Shape createShape(FlowNode node) {
        if (node.type == NodeType.DECISION) {
            Polygon diamond = new Polygon(
                    node.x + (node.width / 2), node.y,
                    node.x + node.width, node.y + (node.height / 2),
                    node.x + (node.width / 2), node.y + node.height,
                    node.x, node.y + (node.height / 2)
            );
            return diamond;
        }

        return new Rectangle(node.x, node.y, node.width, node.height);
    }


    private FlowNode findNodeById(List<FlowNode> nodes, String id) {
        for (FlowNode node : nodes) {
            if (node.id.equals(id)) {
                return node;
            }
        }
        return null;
    }

    private void highlight(FlowNode node) {
        nodeShapes.values().forEach(shape -> {
            shape.setFill(Color.WHITE);
            shape.setStroke(Color.BLACK);
            shape.setStrokeWidth(2);
        });

        Shape shape = nodeShapes.get(node.id);
        if (shape != null) {
            shape.setFill(Color.YELLOW);
        }
    }
}
