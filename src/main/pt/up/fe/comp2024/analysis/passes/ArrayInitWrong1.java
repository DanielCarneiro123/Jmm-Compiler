package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;

public class ArrayInitWrong1 extends AnalysisVisitor {

    private String currentMethod;
    private Type varRefChildType;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGNMENT, this::arrayWrong);
        addVisit(Kind.ARRAYDEFINITION, this::arrayInitWrong);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void arrayWrong(JmmNode varRefExpr, SymbolTable table) {

        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        varRefChildType = getExprType(varRefExpr.getChildren().get(0), table, currentMethod);

        return null;
    }

    private Void arrayInitWrong(JmmNode varDecl, SymbolTable table) {

        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");


        for (JmmNode child : varDecl.getChildren()) {
            if (varRefChildType == null) {
                return null;
            }
            var childType = getExprType(child, table, currentMethod);
            var childTypeName = childType.getName();
            var varRefChildTypeName = varRefChildType.getName();

            if (!childTypeName.equals("int") || childType.isArray()) {
                String message = "Incorrect Array Type";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varDecl),
                        NodeUtils.getColumn(varDecl),
                        message,
                        null)
                );
                return null;
            }
        }
        return null;
    }
}