package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;

public class ArrayInitWrong extends AnalysisVisitor{

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGNMENT, this::arrayWrong);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void arrayWrong(JmmNode varRefExpr, SymbolTable table) {

        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var varRefName = varRefExpr.get("var");

        var varRefType = getExprType(varRefExpr.getChildren().get(0), table, currentMethod);

        for (var parameter : table.getParameters(currentMethod)) {
            if (parameter.getType().getName().equals(varRefName) && !parameter.getType().equals(varRefType)) {
                String message = "It is not an array";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varRefExpr),
                        NodeUtils.getColumn(varRefExpr),
                        message,
                        null)
                );
                return null;
            }
        }
        for (var localVariable : table.getLocalVariables(currentMethod)) {
            if (localVariable.getName().equals(varRefName) && !localVariable.getType().equals(varRefType)) {
                String message = "It is not an array";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varRefExpr),
                        NodeUtils.getColumn(varRefExpr),
                        message,
                        null)
                );
                return null;
            }
        }
        return null;
    }


}
