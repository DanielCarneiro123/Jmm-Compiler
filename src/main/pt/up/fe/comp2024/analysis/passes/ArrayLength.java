package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class ArrayLength extends AnalysisVisitor {
    private String method;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.LENGTH, this::visitLength);
    }

    private Void visitMethodDecl(JmmNode currMethod, SymbolTable table) {
        method = currMethod.get("name");
        return null;
    }

    private Void visitLength(JmmNode currLenght, SymbolTable table) {
        var currLenghtTeste = currLenght.get("len");
        if (!currLenghtTeste.equals("length")) {
            String message = "Must be .length";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(currLenght),
                    NodeUtils.getColumn(currLenght),
                    message,
                    null)
            );
            return null;
        }
        return null;
    }
}
