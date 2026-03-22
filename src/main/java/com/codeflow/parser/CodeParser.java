package com.codeflow.parser;

import com.codeflow.enums.NodeType;
import com.codeflow.model.FlowNode;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.*;

import java.util.*;

public class CodeParser {
    public static class ParseResult {
        public Map<String, Object> variables = new HashMap<>();
        public List<FlowNode> flowNodes = new ArrayList<>();
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
        if (methodOpt.isPresent() && methodOpt.get().getBody().isPresent()) {
            BlockStmt body = methodOpt.get().getBody().get();
            for (Statement stmt : body.getStatements()) {
                addStatement(result.flowNodes, stmt);
            }
        }

        return result;
    }

    private void addStatement(List<FlowNode> flowNodes, Statement stmt) {
        if (stmt instanceof IfStmt ifStmt) {
            flowNodes.add(new FlowNode(
                    UUID.randomUUID().toString(),
                    ifStmt.getCondition().toString(),
                    NodeType.DECISION
            ));

            Statement thenStmt = ifStmt.getThenStmt();
            if (thenStmt instanceof BlockStmt thenBlock) {
                for (Statement child : thenBlock.getStatements()) {
                    addStatement(flowNodes, child);
                }
            } else {
                addStatement(flowNodes, thenStmt);
            }

            ifStmt.getElseStmt().ifPresent(elseStmt -> {
                if (elseStmt instanceof BlockStmt elseBlock) {
                    for (Statement child : elseBlock.getStatements()) {
                        addStatement(flowNodes, child);
                    }
                } else {
                    addStatement(flowNodes, elseStmt);
                }
            });

        } else if (stmt instanceof ExpressionStmt) {
            flowNodes.add(new FlowNode(
                    UUID.randomUUID().toString(),
                    stmt.toString(),
                    NodeType.PROCESS
            ));

        } else if (stmt instanceof ReturnStmt) {
            flowNodes.add(new FlowNode(
                    UUID.randomUUID().toString(),
                    stmt.toString(),
                    NodeType.END
            ));

        } else if (stmt instanceof ForStmt || stmt instanceof WhileStmt || stmt instanceof ForEachStmt || stmt instanceof DoStmt) {
            flowNodes.add(new FlowNode(
                    UUID.randomUUID().toString(),
                    stmt.toString(),
                    NodeType.DECISION
            ));

        } else {
            flowNodes.add(new FlowNode(
                    UUID.randomUUID().toString(),
                    stmt.toString(),
                    NodeType.PROCESS
            ));
        }
    }


}
