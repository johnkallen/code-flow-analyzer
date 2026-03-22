package com.codeflow.parser;

import com.codeflow.enums.NodeType;
import com.codeflow.model.FlowEdge;
import com.codeflow.model.FlowNode;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.*;

import java.util.*;

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

        BlockStmt body = methodOpt.get().getBody().get();
        List<Statement> statements = body.getStatements();

        double centerX = 300;
        double currentY = 40;
        double width = 200;
        double height = 60;
        double verticalGap = 100;
        double branchOffset = 220;

        FlowNode previous = null;

        for (int i = 0; i < statements.size(); i++) {
            Statement stmt = statements.get(i);

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
                result.flowNodes.add(decision);

                if (previous != null) {
                    result.flowEdges.add(new FlowEdge(previous.id, decision.id, ""));
                }

                FlowNode thenNode = null;
                FlowNode elseNode = null;

                double branchY = currentY + verticalGap;

                Statement thenStmt = ifStmt.getThenStmt();
                if (thenStmt instanceof BlockStmt thenBlock && !thenBlock.getStatements().isEmpty()) {
                    Statement firstThen = thenBlock.getStatement(0);
                    thenNode = createSimpleNode(firstThen, centerX - branchOffset, branchY, width, height);
                    result.flowNodes.add(thenNode);
                    result.flowEdges.add(new FlowEdge(decision.id, thenNode.id, "Yes"));
                } else if (!(thenStmt instanceof BlockStmt)) {
                    thenNode = createSimpleNode(thenStmt, centerX - branchOffset, branchY, width, height);
                    result.flowNodes.add(thenNode);
                    result.flowEdges.add(new FlowEdge(decision.id, thenNode.id, "Yes"));
                }

                if (ifStmt.getElseStmt().isPresent()) {
                    Statement elseStmt = ifStmt.getElseStmt().get();
                    if (elseStmt instanceof BlockStmt elseBlock && !elseBlock.getStatements().isEmpty()) {
                        Statement firstElse = elseBlock.getStatement(0);
                        elseNode = createSimpleNode(firstElse, centerX + branchOffset, branchY, width, height);
                        result.flowNodes.add(elseNode);
                        result.flowEdges.add(new FlowEdge(decision.id, elseNode.id, "No"));
                    } else if (!(elseStmt instanceof BlockStmt)) {
                        elseNode = createSimpleNode(elseStmt, centerX + branchOffset, branchY, width, height);
                        result.flowNodes.add(elseNode);
                        result.flowEdges.add(new FlowEdge(decision.id, elseNode.id, "No"));
                    }
                } else {
                    result.flowEdges.add(new FlowEdge(decision.id, null, "No"));
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
                result.flowNodes.add(joinNode);

                if (thenNode != null) {
                    result.flowEdges.add(new FlowEdge(thenNode.id, joinNode.id, ""));
                } else {
                    result.flowEdges.add(new FlowEdge(decision.id, joinNode.id, "Yes"));
                }

                if (elseNode != null) {
                    result.flowEdges.add(new FlowEdge(elseNode.id, joinNode.id, ""));
                } else {
                    result.flowEdges.add(new FlowEdge(decision.id, joinNode.id, "No"));
                }

                previous = joinNode;
                currentY += verticalGap * 3;
            } else {
                FlowNode node = createSimpleNode(stmt, centerX, currentY, width, height);
                result.flowNodes.add(node);

                if (previous != null) {
                    result.flowEdges.add(new FlowEdge(previous.id, node.id, ""));
                }

                previous = node;
                currentY += verticalGap;
            }
        }

        return result;
    }

    private FlowNode createSimpleNode(Statement stmt, double x, double y, double width, double height) {
        NodeType type = NodeType.PROCESS;

        if (stmt instanceof ReturnStmt) {
            type = NodeType.END;
        }

        String label = stmt.toString().trim();
        return new FlowNode(
                UUID.randomUUID().toString(),
                label,
                type,
                x,
                y,
                width,
                height
        );
    }
}