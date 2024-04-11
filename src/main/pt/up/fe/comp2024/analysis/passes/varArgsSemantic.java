package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class varArgsSemantic extends AnalysisVisitor {
    private String method;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.METHOD_DECL, this::visitVarArguments);
    }

    private Void visitMethodDecl(JmmNode currMethod, SymbolTable table) {
        method = currMethod.get("name");
        return null;
    }

    private Void visitVarArguments(JmmNode methodDecl, SymbolTable table) {
        var argsList = methodDecl.getChildren(Kind.ARGUMENT);
        for (int i = 0; i < argsList.size(); i++) {
            String x = argsList.get(i).getChildren().get(0).getKind();
            if (x.equals("VarArg") && (i != argsList.size() - 1)) {
                String message = "VarArguments Badly Placed";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(methodDecl),
                        NodeUtils.getColumn(methodDecl),
                        message,
                        null)
                );
                return null;
            }
        }
        return null;
    }

}