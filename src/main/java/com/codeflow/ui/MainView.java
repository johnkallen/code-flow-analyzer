package com.codeflow.ui;

import com.codeflow.engine.ExecutionEngine;
import com.codeflow.model.FlowNode;
import com.codeflow.model.StepEvent;
import com.codeflow.parser.CodeParser;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

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

        // Create SplitPane with titled containers
        SplitPane split = new SplitPane();
        split.getItems().addAll(codeEditor, flowChartView.getView());
        split.setDividerPositions(0.3);

        // Wrap Code Editor in a VBox with a title
        VBox codeContainer = new VBox(5);
        Label codeLabel = new Label("Code");
        codeLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5 10; -fx-background-color: #e0e0e0;");
        codeContainer.getChildren().addAll(
                codeLabel,
                codeEditor
        );
        codeContainer.setStyle("-fx-padding: 5; -fx-background-color: #f0f0f0;"); // Add background

        // Wrap Flowchart View in a VBox with a title
        VBox flowchartContainer = new VBox(5);
        Label flowLabel = new Label("Flowchart");
        flowLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5 10; -fx-background-color: #e0e0e0;");
        flowchartContainer.getChildren().addAll(
                flowLabel,
                flowChartView.getView()
        );
        flowchartContainer.setStyle("-fx-padding: 5; -fx-background-color: #f0f0f0;"); // Add background

        // Wrap Variables Panel in a VBox with a title
        VBox variableContainer = new VBox(5);
        Label varLabel = new Label("Variables");
        varLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5 10; -fx-background-color: #e0e0e0;");
        variableContainer.getChildren().addAll(
                varLabel,
                variablePanel.getView()
        );
        variableContainer.setStyle("-fx-padding: 5; -fx-background-color: #f0f0f0;"); // Match style

        // Create HBox for controls
        HBox controls = new HBox(10);
        controls.getChildren().addAll(
                new Button("Analyze"),
                new Button("Step"),
                new Button("Fit"),
                statusLabel
        );

        // Create spacer to push Export button to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button exportBtn = new Button("Export");
        controls.getChildren().addAll(spacer, exportBtn);
        controls.setStyle("-fx-padding: 25 0 0 0;");

        // Set layout
        root.setTop(variableContainer);
        root.setCenter(split);
        root.setBottom(controls);

        // Button actions
        ((Button) controls.getChildren().get(0)).setOnAction(e -> analyzeCode());
        ((Button) controls.getChildren().get(1)).setOnAction(e -> step());
        ((Button) controls.getChildren().get(2)).setOnAction(e -> flowChartView.fitToScreen());

        exportBtn.setOnAction(e -> {

            // Step 1: Choose directory
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory = directoryChooser.showDialog(root.getScene().getWindow());

            if (selectedDirectory == null) {
                statusLabel.setText("Export cancelled.");
                return;
            }

            // Step 2: Choose filename
            TextInputDialog filenameDialog = new TextInputDialog("flowchart");
            filenameDialog.setTitle("Export to draw.io");
            filenameDialog.setHeaderText("Enter filename (without .drawio extension):");
            filenameDialog.setContentText("Filename:");

            Optional<String> result = filenameDialog.showAndWait();
            result.ifPresent(filename -> {
                String fullPath = selectedDirectory.getAbsolutePath() + File.separator + filename + ".drawio";

                // Step 3: Generate and save XML
                String xmlContent = flowChartView.generateDrawIOXML();
                if (xmlContent.isEmpty()) {
                    statusLabel.setText("No flowchart to export.");
                    return;
                }

                try {
                    Path path = Paths.get(fullPath);
                    Files.writeString(path, xmlContent);
                    statusLabel.setText("Exported to " + fullPath);
                } catch (Exception ex) {
                    statusLabel.setText("Export failed: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        });


        // Optional: Style Export button
        (controls.getChildren().get(controls.getChildren().size() - 1))
                .setStyle("-fx-padding: 5 15 5 15;");


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