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

        double centerX = 300;
        double currentY = 40;
        double width = 200;
        double height = 60;
        double verticalGap = 100;
        double branchOffset = 220;

        FlowNode previous = null;

        for (Statement stmt : statements) {
            if (stmt instanceof IfStmt ifStmt) {
                FlowNode decision = new FlowNode(
                        UUID.randomUUID().toString(),
                        ifStmt.getCondition().toString(),
                        NodeType.DECISION,
                        centerX,
                        currentY,
                        width,
                        height
                );
                decision.condition = ifStmt.getCondition().toString();
                result.flowNodes.add(decision);

                if (previous != null) {
                    previous.nextId = decision.id;
                    result.flowEdges.add(new FlowEdge(previous.id, decision.id, ""));
                }

                FlowNode thenNode = null;
                FlowNode elseNode = null;

                double branchY = currentY + verticalGap;

                Statement thenStmt = unwrapFirst(ifStmt.getThenStmt());
                if (thenStmt != null) {
                    thenNode = createProcessNode(thenStmt, centerX - branchOffset, branchY, width, height);
                    result.flowNodes.add(thenNode);
                    result.flowEdges.add(new FlowEdge(decision.id, thenNode.id, "Yes"));
                    decision.trueNextId = thenNode.id;
                }

                if (ifStmt.getElseStmt().isPresent()) {
                    Statement elseStmt = unwrapFirst(ifStmt.getElseStmt().get());
                    if (elseStmt != null) {
                        elseNode = createProcessNode(elseStmt, centerX + branchOffset, branchY, width, height);
                        result.flowNodes.add(elseNode);
                        result.flowEdges.add(new FlowEdge(decision.id, elseNode.id, "No"));
                        decision.falseNextId = elseNode.id;
                    }
                }

                FlowNode joinNode = new FlowNode(
                        UUID.randomUUID().toString(),
                        "Join",
                        NodeType.PROCESS,
                        centerX,
                        currentY + (verticalGap * 2),
                        width,
                        height
                );
                joinNode.expression = null;
                result.flowNodes.add(joinNode);

                if (thenNode != null) {
                    thenNode.nextId = joinNode.id;
                    result.flowEdges.add(new FlowEdge(thenNode.id, joinNode.id, ""));
                } else {
                    decision.trueNextId = joinNode.id;
                    result.flowEdges.add(new FlowEdge(decision.id, joinNode.id, "Yes"));
                }

                if (elseNode != null) {
                    elseNode.nextId = joinNode.id;
                    result.flowEdges.add(new FlowEdge(elseNode.id, joinNode.id, ""));
                } else {
                    decision.falseNextId = joinNode.id;
                    result.flowEdges.add(new FlowEdge(decision.id, joinNode.id, "No"));
                }

                previous = joinNode;
                currentY += verticalGap * 3;
            } else {
                FlowNode node = createProcessNode(stmt, centerX, currentY, width, height);
                result.flowNodes.add(node);

                if (previous != null) {
                    previous.nextId = node.id;
                    result.flowEdges.add(new FlowEdge(previous.id, node.id, ""));
                }

                previous = node;
                currentY += verticalGap;
            }
        }

        return result;
    }

    private Statement unwrapFirst(Statement stmt) {
        if (stmt instanceof BlockStmt block) {
            if (block.getStatements().isEmpty()) {
                return null;
            }
            return block.getStatement(0);
        }
        return stmt;
    }

    private FlowNode createProcessNode(Statement stmt, double x, double y, double width, double height) {
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
}