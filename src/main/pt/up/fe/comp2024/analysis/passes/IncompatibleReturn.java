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

public class IncompatibleReturn extends AnalysisVisitor {
    private String method;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.RETURN_STMT, this::visitIncompatibleReturn);
    }

    private Void visitMethodDecl(JmmNode currMethod, SymbolTable table) {
        method = currMethod.get("name");
        return null;
    }

    private Void visitIncompatibleReturn(JmmNode expr, SymbolTable table) {
        JmmNode parentExpr = expr.getJmmParent();
        Type typeMethod = table.getReturnType(parentExpr.get("name"));
        JmmNode childExpr = expr.getJmmChild(0);
        Type typeExpr = getExprType(childExpr, table, method);

        if (!typeExpr.equals(typeMethod)) {
            String message = "Incompatible Return";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(expr),
                    NodeUtils.getColumn(expr),
                    message,
                    null)
            );
            return null;
        }

        return null;
    }

}