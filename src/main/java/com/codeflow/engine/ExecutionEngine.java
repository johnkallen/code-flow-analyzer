package com.codeflow.engine;

import com.codeflow.model.ExecutionContext;
import com.codeflow.model.FlowNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExecutionEngine {

    private final Map<String, FlowNode> nodesById = new HashMap<>();
    private final ExecutionContext context = new ExecutionContext();

    public ExecutionEngine(List<FlowNode> nodes, Map<String, Object> initialVariables) {
        for (FlowNode node : nodes) {
            nodesById.put(node.id, node);
        }

        context.variables.putAll(initialVariables);

        if (!nodes.isEmpty()) {
            context.currentNodeId = nodes.get(0).id;
        } else {
            context.finished = true;
        }
    }

    public FlowNode step() {
        if (context.finished || context.currentNodeId == null) {
            context.finished = true;
            return null;
        }

        FlowNode node = nodesById.get(context.currentNodeId);
        if (node == null) {
            context.finished = true;
            return null;
        }

        executeNode(node);

        return node;
    }

    public Map<String, Object> getVariables() {
        return context.variables;
    }

    private void executeNode(FlowNode node) {
        switch (node.type) {
            case DECISION -> {
                boolean result = evaluateCondition(node.condition);
                context.currentNodeId = result ? node.trueNextId : node.falseNextId;
            }
            case END -> {
                context.finished = true;
                context.currentNodeId = null;
            }
            default -> {
                executeProcess(node);
                context.currentNodeId = node.nextId;
                if (context.currentNodeId == null) {
                    context.finished = true;
                }
            }
        }
    }

    private void executeProcess(FlowNode node) {
        String expr = node.expression;
        if (expr == null || expr.isBlank()) {
            return;
        }

        expr = expr.trim();

        if (expr.endsWith(";")) {
            expr = expr.substring(0, expr.length() - 1).trim();
        }

        // int x = 5
        if (expr.matches("^(int|double|long|float|boolean|String)\\s+\\w+\\s*=.*$")) {
            String withoutType = expr.replaceFirst("^(int|double|long|float|boolean|String)\\s+", "");
            String[] parts = withoutType.split("=", 2);
            String varName = parts[0].trim();
            String valueExpr = parts[1].trim();
            Object value = evaluateExpression(valueExpr);
            context.variables.put(varName, value);
            return;
        }

        // x = 5
        if (expr.matches("^\\w+\\s*=.*$")) {
            String[] parts = expr.split("=", 2);
            String varName = parts[0].trim();
            String valueExpr = parts[1].trim();
            Object value = evaluateExpression(valueExpr);
            context.variables.put(varName, value);
            return;
        }

        // x++
        if (expr.matches("^\\w+\\+\\+$")) {
            String varName = expr.substring(0, expr.length() - 2).trim();
            Object current = context.variables.get(varName);
            int value = toInt(current);
            context.variables.put(varName, value + 1);
            return;
        }

        // x--
        if (expr.matches("^\\w+--$")) {
            String varName = expr.substring(0, expr.length() - 2).trim();
            Object current = context.variables.get(varName);
            int value = toInt(current);
            context.variables.put(varName, value - 1);
            return;
        }
    }

    private Object evaluateExpression(String expr) {
        expr = expr.trim();

        if (expr.matches("^-?\\d+$")) {
            return Integer.parseInt(expr);
        }

        if ("true".equals(expr)) {
            return true;
        }

        if ("false".equals(expr)) {
            return false;
        }

        if (expr.matches("^\\w+$")) {
            return context.variables.get(expr);
        }

        if (expr.contains("+")) {
            String[] parts = expr.split("\\+");
            int sum = 0;
            for (String part : parts) {
                sum += toInt(evaluateExpression(part.trim()));
            }
            return sum;
        }

        if (expr.contains("-")) {
            String[] parts = expr.split("-");
            int result = toInt(evaluateExpression(parts[0].trim()));
            for (int i = 1; i < parts.length; i++) {
                result -= toInt(evaluateExpression(parts[i].trim()));
            }
            return result;
        }

        return null;
    }

    private boolean evaluateCondition(String condition) {
        if (condition == null || condition.isBlank()) {
            return false;
        }

        String c = condition.trim();

        if (c.contains(">=")) {
            String[] parts = c.split(">=");
            return toInt(evaluateExpression(parts[0].trim())) >= toInt(evaluateExpression(parts[1].trim()));
        }

        if (c.contains("<=")) {
            String[] parts = c.split("<=");
            return toInt(evaluateExpression(parts[0].trim())) <= toInt(evaluateExpression(parts[1].trim()));
        }

        if (c.contains("==")) {
            String[] parts = c.split("==");
            Object left = evaluateExpression(parts[0].trim());
            Object right = evaluateExpression(parts[1].trim());
            return left == null ? right == null : left.equals(right);
        }

        if (c.contains("!=")) {
            String[] parts = c.split("!=");
            Object left = evaluateExpression(parts[0].trim());
            Object right = evaluateExpression(parts[1].trim());
            return !(left == null ? right == null : left.equals(right));
        }

        if (c.contains(">")) {
            String[] parts = c.split(">");
            return toInt(evaluateExpression(parts[0].trim())) > toInt(evaluateExpression(parts[1].trim()));
        }

        if (c.contains("<")) {
            String[] parts = c.split("<");
            return toInt(evaluateExpression(parts[0].trim())) < toInt(evaluateExpression(parts[1].trim()));
        }

        Object result = evaluateExpression(c);
        return result instanceof Boolean b && b;
    }

    private int toInt(Object value) {
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s && s.matches("^-?\\d+$")) {
            return Integer.parseInt(s);
        }
        return 0;
    }
}