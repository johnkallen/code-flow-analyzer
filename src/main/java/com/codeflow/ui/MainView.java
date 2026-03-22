package com.codeflow.ui;

import com.codeflow.engine.ExecutionEngine;
import com.codeflow.enums.NodeType;
import com.codeflow.model.FlowNode;
import com.codeflow.parser.CodeParser;
import javafx.geometry.Pos;
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
    private Map<String, Shape> nodeShapes = new HashMap<>();

    public MainView() {

        // Top panel (variables)
        variablePanel.setPrefHeight(120);
        variablePanel.setStyle("-fx-background-color: #eeeeee; -fx-padding: 10;");

        // Split center
        SplitPane split = new SplitPane();
        flowPane.setPrefSize(600, 1000);
        flowPane.setStyle("-fx-background-color: white;");
        codeEditor.setPrefWidth(500);

        split.getItems().addAll(codeEditor, flowPane);
        split.setDividerPositions(0.5);

        // Bottom controls
        Button analyzeBtn = new Button("Analyze");
        Button stepBtn = new Button("Step");
        Button exportBtn = new Button("Export");

        HBox controls = new HBox(10, analyzeBtn, stepBtn, exportBtn);
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
            CodeParser.ParseResult result = parser.parse(codeEditor.getText());

            engine = new ExecutionEngine(result.flowNodes);

            updateVariables(result.variables);
            drawFlow(result.flowNodes);

            statusLabel.setText("Analyze complete. Nodes: " + result.flowNodes.size());
            System.out.println("Engine created with " + result.flowNodes.size() + " nodes.");

        } catch (Exception ex) {
            engine = null;
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

        FlowNode node = engine.step();
        if (node == null) {
            statusLabel.setText("Execution complete.");
            return;
        }

        highlight(node);
        statusLabel.setText("Stepped: " + node.label);
    }


    private void updateVariables(Map<String, Object> vars) {
        variablePanel.getChildren().clear();

        if (vars.isEmpty()) {
            variablePanel.getChildren().add(new Label("No variables found."));
            return;
        }

        vars.forEach((k, v) -> variablePanel.getChildren().add(new Label(k + " = " + v)));
    }



    private void drawFlow(List<FlowNode> nodes) {
        flowPane.getChildren().clear();
        nodeShapes.clear();

        int y = 20;

        for (FlowNode node : nodes) {

            if (y > 20) {
                Line line = new Line(150, y - 50, 150, y);
                line.setStroke(Color.BLACK);
                line.setStrokeWidth(2);
                flowPane.getChildren().add(line);
            }

            StackPane nodePane = new StackPane();
            nodePane.setLayoutX(50);
            nodePane.setLayoutY(y);
            nodePane.setPrefSize(200, 50);
            nodePane.setMinSize(200, 50);
            nodePane.setMaxSize(200, 50);

            // Create Shape and Text
            Shape shape;
            switch (node.type) {
                case DECISION:
                    shape = new Polygon(
                            100, 0,
                            200, 25,
                            100, 50,
                            0, 25
                    );
                    break;

                case END:
                    shape = new Rectangle(200, 50);
                    break;

                case START:
                case PROCESS:
                default:
                    shape = new Rectangle(200, 50);
                    break;
            }

            shape.setFill(javafx.scene.paint.Color.WHITE);
            shape.setStroke(javafx.scene.paint.Color.BLACK);
            shape.setStrokeWidth(2);

            // Create centered label
            Label label = new Label(node.label);
            label.setWrapText(true);
            label.setMaxWidth(180);
            label.setAlignment(javafx.geometry.Pos.CENTER);
            label.setStyle("-fx-text-fill: black;");

            // Add shape + label
            nodePane.getChildren().addAll(shape, label);
            StackPane.setAlignment(shape, Pos.CENTER);
            StackPane.setAlignment(label, Pos.CENTER);

            // Add to flow pane
            flowPane.getChildren().add(nodePane);

            // Save shape for highlighting
            nodeShapes.put(node.id, shape);

            y += 100;
        }
    }

    private void highlight(FlowNode node) {
        nodeShapes.values().forEach(shape -> {
            shape.setFill(javafx.scene.paint.Color.WHITE);
            shape.setStroke(javafx.scene.paint.Color.BLACK);
            shape.setStrokeWidth(2);
        });

        Shape shape = nodeShapes.get(node.id);
        if (shape != null) {
            shape.setFill(javafx.scene.paint.Color.YELLOW);
            shape.setStroke(javafx.scene.paint.Color.BLACK);
            shape.setStrokeWidth(2);
        }
    }
}
