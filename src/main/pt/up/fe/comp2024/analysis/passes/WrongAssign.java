package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;

public class WrongAssign extends AnalysisVisitor {
    private String method;

    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.STMT, this::visitWrongAssign);
    }

    private Void visitMethodDecl(JmmNode currMethod, SymbolTable table) {
        method = currMethod.get("name");
        return null;
    }

    private Void visitWrongAssign(JmmNode stmtDecl, SymbolTable table) {

        for (JmmNode operand : stmtDecl.getChildren()) {
            JmmNode parentOperand = operand.getJmmParent();
            Type typeOperand = getExprType(operand, table, method);


            for (var parameter : table.getLocalVariables(method)) {
                if (parentOperand.get("var").equals(parameter.getName()) && !typeOperand.getName().equals(parameter.getType().getName()) && !typeOperand.getName().equals("object")) {
                    String message = "Wrong Assing Types";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(stmtDecl),
                            NodeUtils.getColumn(stmtDecl),
                            message,
                            null)
                    );
                    return null;
                }
            }
            for (var parameter : table.getParameters(method)) {
                if (parentOperand.get("var").equals(parameter.getName()) && !typeOperand.getName().equals(parameter.getType().getName()) && !typeOperand.getName().equals("object")) {
                    String message = "Wrong Assing Types";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(stmtDecl),
                            NodeUtils.getColumn(stmtDecl),
                            message,
                            null)
                    );
                    return null;
                }
            }
            for (var parameter : table.getFields()) {
                if (parentOperand.get("var").equals(parameter.getName()) && !typeOperand.getName().equals(parameter.getType().getName()) && !typeOperand.getName().equals("object")) {
                    String message = "Wrong Assing Types";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(stmtDecl),
                            NodeUtils.getColumn(stmtDecl),
                            message,
                            null)
                    );
                    return null;
                }
            }

        }
        return null;
    }
}
