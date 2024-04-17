package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class MainTest extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        if (currentMethod.equals("main")) {
            var mainChild = method.getChild(1);
            if (mainChild.getKind().equals("ReturnStmt")) {
                String message = "Main must be void";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
                return null;
            }

            var mainChildKind = mainChild.getKind();
            var valueIsString = mainChild.get("value").equals("String");
            if (!mainChildKind.equals("Id") && !valueIsString) {
                String message = "Main arguments Wrong";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
                return null;
            }
            return null;

        } else {
            var isStatic = method.get("isStatic").equals("true");
            if (isStatic) {
                String message = "Method cannot be static";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
                return null;
            }
        }
        return null;
    }
}
