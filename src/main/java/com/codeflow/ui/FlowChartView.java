package com.codeflow.ui;

import com.codeflow.enums.NodeType;
import com.codeflow.model.FlowEdge;
import com.codeflow.model.FlowNode;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.util.*;

public class FlowChartView {

    private final Pane root = new Pane();
    private final Group contentGroup = new Group();

    private final Map<String, Shape> nodeShapes = new HashMap<>();
    private final Map<String, List<Line>> edgeLines = new HashMap<>();

    private double scale = 1.0;
    private double lastPanX;
    private double lastPanY;

    private static final double MIN_SCALE = 0.3;
    private static final double MAX_SCALE = 3.0;
    private static final double ZOOM_FACTOR = 1.1;
    private static final double FIT_PADDING = 40.0;

    public FlowChartView() {
        root.setPrefSize(1000, 1000);
        root.setStyle("-fx-background-color: white;");
        root.getChildren().add(contentGroup);

        setupZoom();
        setupPan();
    }

    public Pane getView() {
        return root;
    }

    public void clear() {
        contentGroup.getChildren().clear();
        nodeShapes.clear();
        edgeLines.clear();
    }

    public void drawFlow(List<FlowNode> nodes, List<FlowEdge> edges) {
        clear();

        for (FlowEdge edge : edges) {
            if (edge.toId == null) continue;

            FlowNode from = findNodeById(nodes, edge.fromId);
            FlowNode to = findNodeById(nodes, edge.toId);
            if (from == null || to == null) continue;

            List<Line> segments = drawEdge(from, to, edge.label);
            edgeLines.put(edge.key(), segments);
            contentGroup.getChildren().addAll(segments);

            if (edge.label != null && !edge.label.isBlank()) {
                contentGroup.getChildren().add(createEdgeLabel(from, to, edge.label));
            }
        }

        for (FlowNode node : nodes) {
            if (node.type == NodeType.JOIN) continue;

//            Shape shape = createShape(node);
//            shape.setFill(Color.WHITE);
//            shape.setStroke(Color.BLACK);
//            shape.setStrokeWidth(2);
//
//            contentGroup.getChildren().add(shape);
//            nodeShapes.put(node.id, shape);

            StackPane nodeContainer = new StackPane();
            nodeContainer.setLayoutX(node.x);
            nodeContainer.setLayoutY(node.y);
            nodeContainer.setPrefSize(node.width, node.height);

            // Shape
            Shape shape = createShape(node);
            shape.setFill(Color.WHITE);
            shape.setStroke(Color.BLACK);
            shape.setStrokeWidth(2);

            // Text (Label handles wrapping + alignment better than Text)
            Label label = new Label(node.label);
            label.setWrapText(true);
            label.setMaxWidth(node.width - 20);
            label.setAlignment(Pos.CENTER);
            label.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

            // Center everything
            nodeContainer.setAlignment(Pos.CENTER);

            // Add children
            nodeContainer.getChildren().addAll(shape, label);

            // Add to scene
            contentGroup.getChildren().add(nodeContainer);

            // IMPORTANT: store shape for highlighting
            nodeShapes.put(node.id, shape);
        }

        Platform.runLater(this::fitToScreen);
    }

    public void clearHighlights() {
        nodeShapes.values().forEach(shape -> {
            shape.setFill(Color.WHITE);
            shape.setStroke(Color.BLACK);
            shape.setStrokeWidth(2);
        });

        edgeLines.values().forEach(lines ->
                lines.forEach(line -> {
                    line.setStroke(Color.BLACK);
                    line.setStrokeWidth(2);
                })
        );
    }

    public void highlightNode(String nodeId) {
        Shape shape = nodeShapes.get(nodeId);
        if (shape != null) {
            shape.setFill(Color.YELLOW);
        }
    }

    public void highlightEdge(String fromId, String toId) {
        List<Line> lines = edgeLines.get(fromId + "->" + toId);
        if (lines != null) {
            lines.forEach(line -> {
                line.setStroke(Color.RED);
                line.setStrokeWidth(4);
            });
        }
    }

    public void resetView() {
        scale = 1.0;
        contentGroup.setScaleX(scale);
        contentGroup.setScaleY(scale);
        contentGroup.setTranslateX(0);
        contentGroup.setTranslateY(0);
    }

    public void fitToScreen() {
        if (contentGroup.getChildren().isEmpty()) {
            resetView();
            return;
        }

        double viewportWidth = root.getWidth() > 0 ? root.getWidth() : root.getPrefWidth();
        double viewportHeight = root.getHeight() > 0 ? root.getHeight() : root.getPrefHeight();

        if (viewportWidth <= 0 || viewportHeight <= 0) {
            return;
        }

        // Reset transforms first
        contentGroup.setScaleX(1.0);
        contentGroup.setScaleY(1.0);
        contentGroup.setTranslateX(0);
        contentGroup.setTranslateY(0);

        Bounds layoutBounds = contentGroup.getLayoutBounds();
        if (layoutBounds.getWidth() <= 0 || layoutBounds.getHeight() <= 0) {
            return;
        }

        double availableWidth = Math.max(1, viewportWidth - (FIT_PADDING * 2));
        double availableHeight = Math.max(1, viewportHeight - (FIT_PADDING * 2));

        double scaleX = availableWidth / layoutBounds.getWidth();
        double scaleY = availableHeight / layoutBounds.getHeight();

        scale = clamp(Math.min(scaleX, scaleY), MIN_SCALE, MAX_SCALE);

        // Apply scale first
        contentGroup.setScaleX(scale);
        contentGroup.setScaleY(scale);

        // Now center based on the ACTUAL rendered bounds after scaling
        Bounds scaledBounds = contentGroup.getBoundsInParent();

        double dx = (viewportWidth - scaledBounds.getWidth()) / 2 - scaledBounds.getMinX();
        double dy = (viewportHeight - scaledBounds.getHeight()) / 2 - scaledBounds.getMinY();

        contentGroup.setTranslateX(contentGroup.getTranslateX() + dx);
        contentGroup.setTranslateY(contentGroup.getTranslateY() + dy);
    }

    private void setupZoom() {
        root.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() == 0) {
                return;
            }

            double oldScale = scale;
            double zoomMultiplier = event.getDeltaY() > 0 ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;
            scale = clamp(scale * zoomMultiplier, MIN_SCALE, MAX_SCALE);

            if (Math.abs(scale - oldScale) < 0.0001) {
                return;
            }

            double mouseX = event.getX();
            double mouseY = event.getY();

            Bounds bounds = contentGroup.getBoundsInParent();
            double dx = mouseX - bounds.getMinX();
            double dy = mouseY - bounds.getMinY();
            double f = scale / oldScale - 1;

            contentGroup.setScaleX(scale);
            contentGroup.setScaleY(scale);

            contentGroup.setTranslateX(contentGroup.getTranslateX() - f * dx);
            contentGroup.setTranslateY(contentGroup.getTranslateY() - f * dy);

            event.consume();
        });
    }

    private void setupPan() {
        root.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }

            lastPanX = event.getSceneX();
            lastPanY = event.getSceneY();
        });

        root.setOnMouseDragged(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }

            double deltaX = event.getSceneX() - lastPanX;
            double deltaY = event.getSceneY() - lastPanY;

            contentGroup.setTranslateX(contentGroup.getTranslateX() + deltaX);
            contentGroup.setTranslateY(contentGroup.getTranslateY() + deltaY);

            lastPanX = event.getSceneX();
            lastPanY = event.getSceneY();
        });
    }

    private List<Line> drawEdge(FlowNode from, FlowNode to, String label) {
        List<Line> lines = new ArrayList<>();

        if (from.type == NodeType.DECISION) {
            double startY = from.y + (from.height / 2);

            if ("True".equalsIgnoreCase(label) || "Yes".equalsIgnoreCase(label)) {
                double startX = from.x;
                double endX = to.x + (to.width / 2);
                double endY = to.y;

                lines.add(createLine(startX, startY, endX, startY));
                lines.add(createLine(endX, startY, endX, endY));
                return lines;
            }

            if ("False".equalsIgnoreCase(label) || "No".equalsIgnoreCase(label)) {
                double startX = from.x + from.width;
                double endX = to.x + (to.width / 2);
                double endY = to.y;

                lines.add(createLine(startX, startY, endX, startY));
                lines.add(createLine(endX, startY, endX, endY));
                return lines;
            }
        }

        if (to.type == NodeType.JOIN) {
            double startX = from.x + (from.width / 2);
            double startY = from.y + from.height;
            double endX = to.x + (to.width / 2);
            double endY = to.y;

            lines.add(createLine(startX, startY, startX, endY));
            lines.add(createLine(startX, endY, endX, endY));
            return lines;
        }

        double fromX = from.x + (from.width / 2);
        double fromY = from.y + from.height;
        double toX = to.x + (to.width / 2);
        double toY = to.y;

        if (Math.abs(fromX - toX) < 0.5) {
            lines.add(createLine(fromX, fromY, toX, toY));
            return lines;
        }

        lines.add(createLine(fromX, fromY, fromX, toY));
        lines.add(createLine(fromX, toY, toX, toY));
        return lines;
    }

    private Line createLine(double x1, double y1, double x2, double y2) {
        Line line = new Line(x1, y1, x2, y2);
        line.setStroke(Color.BLACK);
        line.setStrokeWidth(2);
        return line;
    }

    private Text createEdgeLabel(FlowNode from, FlowNode to, String label) {
        Text text = new Text(label);

        double x;
        double y;

        if (from.type == NodeType.DECISION) {
            if ("True".equalsIgnoreCase(label) || "Yes".equalsIgnoreCase(label)) {
                x = from.x - 55;
                y = from.y + (from.height / 2) - 8;
            } else if ("False".equalsIgnoreCase(label) || "No".equalsIgnoreCase(label)) {
                x = from.x + from.width + 10;
                y = from.y + (from.height / 2) - 8;
            } else {
                x = (from.x + to.x) / 2;
                y = (from.y + to.y) / 2;
            }
        } else {
            x = ((from.x + from.width / 2) + (to.x + to.width / 2)) / 2 + 5;
            y = from.y + from.height + 15;
        }

        text.setX(x);
        text.setY(y);
        return text;
    }

    private Shape createShape(FlowNode node) {
        if (node.type == NodeType.DECISION) {
            return new Polygon(
                    node.x + (node.width / 2), node.y,
                    node.x + node.width, node.y + (node.height / 2),
                    node.x + (node.width / 2), node.y + node.height,
                    node.x, node.y + (node.height / 2)
            );
        }

        return new Rectangle(node.x, node.y, node.width, node.height);
    }

    public FlowNode findNodeById(List<FlowNode> nodes, String id) {
        return nodes.stream()
                .filter(n -> n.id.equals(id))
                .findFirst()
                .orElse(null);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}