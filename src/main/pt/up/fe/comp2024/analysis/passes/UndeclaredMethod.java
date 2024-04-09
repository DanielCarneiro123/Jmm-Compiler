package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class UndeclaredMethod extends AnalysisVisitor {
    private String method;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.NEW_CLASS, this::visitUndeclaredMethod);
    }

    private Void visitMethodDecl(JmmNode currMethod, SymbolTable table) {
        method = currMethod.get("name");
        return null;
    }

    private Void visitUndeclaredMethod(JmmNode newClass, SymbolTable table) {
        String newClassKind = newClass.getKind();
        String className = newClass.get("classname");

        if (newClassKind.equals("NewClass")) {
            if (!table.getMethods().stream()
                    .anyMatch(param -> param.equals(className))) {
                String message = "Undeclared Method";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(newClass),
                        NodeUtils.getColumn(newClass),
                        message,
                        null)
                );
                return null;
            }
        }

        return null;
    }
}
