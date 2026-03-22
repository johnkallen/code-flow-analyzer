package com.codeflow.ui;

import com.codeflow.engine.ExecutionEngine;
import com.codeflow.model.FlowNode;
import com.codeflow.parser.CodeParser;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
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

    private TextArea codeEditor = new TextArea();
    private VBox variablePanel = new VBox();
    private Pane flowPane = new Pane();

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
        System.out.println("Analyzing Code... ");
        String code = codeEditor.getText();
        CodeParser parser = new CodeParser();

        var result = parser.parse(code);

        updateVariables(result.variables);
        drawFlow(result.flowNodes);
    }

    private void step() {
        if (engine == null) return;
        System.out.println("Step button... ");

        FlowNode node = engine.step();
        if (node != null) {
            highlight(node);
        }
    }

    private void updateVariables(Map<String, Object> vars) {
        variablePanel.getChildren().clear();

        vars.forEach((k, v) -> {
            Label label = new Label(k + " = " + v);
            variablePanel.getChildren().add(label);
        });
    }


    private void drawFlow(List<FlowNode> nodes) {
        flowPane.getChildren().clear();
        nodeShapes.clear();

        int y = 20;

        for (FlowNode node : nodes) {
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

            shape.setLayoutX(50);
            shape.setLayoutY(y);
            shape.setFill(javafx.scene.paint.Color.WHITE);
            shape.setStroke(javafx.scene.paint.Color.BLACK);
            shape.setStrokeWidth(2);

            Text text = new Text(node.label);
            text.setLayoutX(60);
            text.setLayoutY(y + 30);

            flowPane.getChildren().add(shape);

            if (y > 20) {
                Line line = new Line(150, y - 50, 150, y);
                line.setStroke(javafx.scene.paint.Color.BLACK);
                line.setStrokeWidth(2);
                flowPane.getChildren().add(line);
            }

            flowPane.getChildren().add(text);
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
