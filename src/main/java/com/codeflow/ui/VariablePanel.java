package com.codeflow.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class VariablePanel {

    private final VBox root = new VBox();
    private final GridPane grid = new GridPane();
    private final Label emptyLabel = new Label("No variables found.");

    // Keeps the panel's current edited values
    private final Map<String, Object> currentVariables = new LinkedHashMap<>();

    // Optional callback so MainView/engine can be notified later
    private BiConsumer<String, Object> variableChangeListener;

    public VariablePanel() {
        root.setPrefHeight(120);
        root.setStyle("-fx-background-color: #eeeeee; -fx-padding: 10; -fx-spacing: 8;");

        grid.setHgap(10);
        grid.setVgap(6);
        grid.setPadding(new Insets(0));

        root.getChildren().add(emptyLabel);
    }

    public VBox getView() {
        return root;
    }

    public void setVariableChangeListener(BiConsumer<String, Object> listener) {
        this.variableChangeListener = listener;
    }

    public void updateVariables(Map<String, Object> vars) {
        currentVariables.clear();
        root.getChildren().clear();

        if (vars == null || vars.isEmpty()) {
            root.getChildren().add(emptyLabel);
            return;
        }

        currentVariables.putAll(vars);
        rebuildGrid();
    }

    public Map<String, Object> getCurrentVariables() {
        return Collections.unmodifiableMap(currentVariables);
    }

    public Object getVariableValue(String name) {
        return currentVariables.get(name);
    }

    public void clear() {
        currentVariables.clear();
        root.getChildren().clear();
    }

    private void rebuildGrid() {
        grid.getChildren().clear();

        int row = 0;
        for (Map.Entry<String, Object> entry : currentVariables.entrySet()) {
            String varName = entry.getKey();
            Object value = entry.getValue();

            Label nameLabel = new Label(varName + " =");
            TextField valueField = new TextField(value == null ? "null" : String.valueOf(value));
            Button applyBtn = new Button("Apply");

            HBox.setHgrow(valueField, Priority.ALWAYS);

            Runnable applyChange = () -> {
                Object oldValue = currentVariables.get(varName);

                try {
                    Object parsedValue = parseValue(valueField.getText(), oldValue);

                    // First notify engine / owner
                    if (variableChangeListener != null) {
                        variableChangeListener.accept(varName, parsedValue);
                    }

                    // Only update panel state if that succeeded
                    currentVariables.put(varName, parsedValue);
                    valueField.setText(parsedValue == null ? "null" : String.valueOf(parsedValue));

                    valueField.setStyle("");
                    valueField.setTooltip(null);

                } catch (IllegalArgumentException ex) {
                    valueField.setStyle("-fx-border-color: red;");
                    valueField.setTooltip(new Tooltip(ex.getMessage()));
                    valueField.setText(oldValue == null ? "null" : String.valueOf(oldValue));
                }
            };

            applyBtn.setOnAction(e -> applyChange.run());
            valueField.setOnAction(e -> applyChange.run());

            grid.add(nameLabel, 0, row);
            grid.add(valueField, 1, row);
            grid.add(applyBtn, 2, row);

            GridPane.setHgrow(valueField, Priority.ALWAYS);
            row++;
        }

        root.getChildren().add(grid);
    }

    private Object parseValue(String text, Object originalValue) {
        String trimmed = text == null ? "" : text.trim();

        if ("null".equalsIgnoreCase(trimmed)) {
            return null;
        }

        if (originalValue == null) {
            return inferBestType(trimmed);
        }

        if (originalValue instanceof Integer) {
            try {
                return Integer.parseInt(trimmed);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Expected an integer.");
            }
        }

        if (originalValue instanceof Long) {
            try {
                return Long.parseLong(trimmed);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Expected a long.");
            }
        }

        if (originalValue instanceof Double) {
            try {
                return Double.parseDouble(trimmed);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Expected a double.");
            }
        }

        if (originalValue instanceof Float) {
            try {
                return Float.parseFloat(trimmed);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Expected a float.");
            }
        }

        if (originalValue instanceof Boolean) {
            if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
                return Boolean.parseBoolean(trimmed);
            }
            throw new IllegalArgumentException("Expected true or false.");
        }

        if (originalValue instanceof Short) {
            try {
                return Short.parseShort(trimmed);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Expected a short.");
            }
        }

        if (originalValue instanceof Byte) {
            try {
                return Byte.parseByte(trimmed);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Expected a byte.");
            }
        }

        if (originalValue instanceof Character) {
            if (trimmed.length() == 1) {
                return trimmed.charAt(0);
            }
            throw new IllegalArgumentException("Expected a single character.");
        }

        // Default: treat as string
        return trimmed;
    }

    private Object inferBestType(String value) {
        if (value.isEmpty()) {
            return "";
        }

        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
        }

        if (value.length() == 1) {
            return value.charAt(0);
        }

        return value;
    }
}