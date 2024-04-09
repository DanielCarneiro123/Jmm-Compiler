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

public class WrongWhileCondition extends AnalysisVisitor {
    private String method;

    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.WHILE_STMT, this::visitWrongWhileCondition);
    }

    private Void visitMethodDecl(JmmNode currMethod, SymbolTable table) {
        method = currMethod.get("name");
        return null;
    }

    private Void visitWrongWhileCondition(JmmNode stmtDecl, SymbolTable table) {

        for (JmmNode operand : stmtDecl.getChildren()) {
            Type typeOperand = getExprType(operand, table, method);

            if (!typeOperand.getName().equals("boolean") || !typeOperand.getName().equals("int")) {
                String message = "Not Bool in While Condition";
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
        return null;
    }
}
