package com.codeflow.parser;

import com.codeflow.model.FlowEdge;
import com.codeflow.model.FlowNode;
import com.codeflow.enums.NodeType;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CodeParser {

    public static class ParseResult {
        public Map<String, Object> variables = new LinkedHashMap<>();
        public List<FlowNode> flowNodes = new ArrayList<>();
        public List<FlowEdge> flowEdges = new ArrayList<>();
    }

    public ParseResult parse(String code) {
        ParseResult result = new ParseResult();

        if (!code.contains("class")) {
            code = "class Temp { void temp() { " + code + " } }";
        }

        CompilationUnit cu = StaticJavaParser.parse(code);

        cu.findAll(VariableDeclarator.class).forEach(v ->
                result.variables.put(v.getNameAsString(), null)
        );

        Optional<MethodDeclaration> methodOpt = cu.findFirst(MethodDeclaration.class);
        if (methodOpt.isEmpty() || methodOpt.get().getBody().isEmpty()) {
            return result;
        }

        List<Statement> statements = methodOpt.get().getBody().get().getStatements();

        Layout layout = new Layout();
        FlowNode previous = null;

        for (Statement stmt : statements) {
            if (stmt instanceof IfStmt ifStmt) {
                previous = addIf(result, ifStmt, previous, layout);
            } else {
                // Default if parser cannot find a specific type
                FlowNode node = createProcessNode(
                        stmt,
                        layout.centerX,
                        layout.currentY,
                        layout.nodeWidth,
                        layout.nodeHeight
                );
                result.flowNodes.add(node);

                if (previous != null) {
                    previous.nextId = node.id;
                    result.flowEdges.add(new FlowEdge(previous.id, node.id, ""));
                }

                previous = node;
                layout.currentY += layout.verticalGap;
            }
        }

        return result;
    }

    private FlowNode addIf(ParseResult result, IfStmt ifStmt, FlowNode previous, Layout layout) {
        double decisionX = layout.centerX;
        double decisionY = layout.currentY;

        FlowNode decision = new FlowNode(
                UUID.randomUUID().toString(),
                ifStmt.getCondition().toString(),
                NodeType.DECISION,
                decisionX,
                decisionY,
                layout.nodeWidth,
                layout.nodeHeight
        );
        decision.condition = ifStmt.getCondition().toString();
        result.flowNodes.add(decision);

        if (previous != null) {
            previous.nextId = decision.id;
            result.flowEdges.add(new FlowEdge(previous.id, decision.id, ""));
        }

        List<Statement> thenStatements = unwrapStatements(ifStmt.getThenStmt());
        List<Statement> elseStatements = ifStmt.getElseStmt()
                .map(this::unwrapStatements)
                .orElseGet(ArrayList::new);

        double trueX = layout.centerX - layout.branchOffset;
        double falseX = layout.centerX + layout.branchOffset;
        double branchStartY = layout.currentY + layout.verticalGap;

        List<FlowNode> trueNodes = createVerticalBranch(result, thenStatements, trueX, branchStartY, layout);
        List<FlowNode> falseNodes = createVerticalBranch(result, elseStatements, falseX, branchStartY, layout);

        FlowNode trueFirst = trueNodes.isEmpty() ? null : trueNodes.get(0);
        FlowNode falseFirst = falseNodes.isEmpty() ? null : falseNodes.get(0);

        FlowNode trueLast = trueNodes.isEmpty() ? null : trueNodes.get(trueNodes.size() - 1);
        FlowNode falseLast = falseNodes.isEmpty() ? null : falseNodes.get(falseNodes.size() - 1);

        int trueDepth = Math.max(1, trueNodes.size());
        int falseDepth = Math.max(1, falseNodes.size());
        int branchDepth = Math.max(trueDepth, falseDepth);

        double joinY = branchStartY + ((branchDepth - 1) * layout.verticalGap) + layout.verticalGap;

        // Invisible join node: kept in model for routing/execution, not intended for drawing
        FlowNode join = new FlowNode(
                UUID.randomUUID().toString(),
                "",
                NodeType.JOIN,
                layout.centerX,
                joinY,
                0,
                0
        );
        join.expression = null;
        result.flowNodes.add(join);

        // Decision outgoing edges
        if (trueFirst != null) {
            decision.trueNextId = trueFirst.id;
            result.flowEdges.add(new FlowEdge(decision.id, trueFirst.id, "True"));
        } else {
            decision.trueNextId = join.id;
            result.flowEdges.add(new FlowEdge(decision.id, join.id, "True"));
        }

        if (falseFirst != null) {
            decision.falseNextId = falseFirst.id;
            result.flowEdges.add(new FlowEdge(decision.id, falseFirst.id, "False"));
        } else {
            decision.falseNextId = join.id;
            result.flowEdges.add(new FlowEdge(decision.id, join.id, "False"));
        }

        // Branches reconnect to join
        if (trueLast != null) {
            trueLast.nextId = join.id;
            result.flowEdges.add(new FlowEdge(trueLast.id, join.id, ""));
        }

        if (falseLast != null) {
            falseLast.nextId = join.id;
            result.flowEdges.add(new FlowEdge(falseLast.id, join.id, ""));
        }

        layout.currentY = joinY + layout.verticalGap;
        return join;
    }

    private List<FlowNode> createVerticalBranch(ParseResult result,
                                                List<Statement> statements,
                                                double x,
                                                double startY,
                                                Layout layout) {
        List<FlowNode> nodes = new ArrayList<>();
        FlowNode previous = null;
        double y = startY;

        for (Statement stmt : statements) {
            FlowNode node = createProcessNode(stmt, x, y, layout.nodeWidth, layout.nodeHeight);
            result.flowNodes.add(node);
            nodes.add(node);

            if (previous != null) {
                previous.nextId = node.id;
                result.flowEdges.add(new FlowEdge(previous.id, node.id, ""));
            }

            previous = node;
            y += layout.verticalGap;
        }

        return nodes;
    }

    private List<Statement> unwrapStatements(Statement stmt) {
        if (stmt instanceof BlockStmt block) {
            return new ArrayList<>(block.getStatements());
        }

        List<Statement> list = new ArrayList<>();
        list.add(stmt);
        return list;
    }

    private FlowNode createProcessNode(Statement stmt,
                                       double x,
                                       double y,
                                       double width,
                                       double height) {
        NodeType type = stmt instanceof ReturnStmt ? NodeType.END : NodeType.PROCESS;

        FlowNode node = new FlowNode(
                UUID.randomUUID().toString(),
                stmt.toString().trim(),
                type,
                x,
                y,
                width,
                height
        );

        if (type == NodeType.PROCESS) {
            node.expression = stmt.toString().trim();
        }

        return node;
    }

    private static class Layout {
        double centerX = 320;
        double currentY = 40;
        double nodeWidth = 200;
        double nodeHeight = 60;
        double verticalGap = 110;
        double branchOffset = 260;
    }
}
