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

public class WrongArrayAcess extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.CLASS_INSTANTIATION, this::visitWrongArray);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARRAY_ASSIGN, this::visitArrayAssign);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitWrongArray(JmmNode arrayDecl, SymbolTable table) {
        String varNameToCheck = arrayDecl.get("value");

        for (var localVariable : table.getLocalVariables(currentMethod)) {
            if (localVariable.getName().equals(varNameToCheck) && !localVariable.getType().isArray()) {
                String message = "It is not an array";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(arrayDecl),
                        NodeUtils.getColumn(arrayDecl),
                        message,
                        null)
                );
                return null;
            }
        }
        return null;
    }

    private Void visitArrayAssign(JmmNode arrayAssign, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");
        String varNameToCheck = arrayAssign.get("var");
        for (var localVariable : table.getLocalVariables(currentMethod)) {
            if (localVariable.getName().equals(varNameToCheck) && !localVariable.getType().isArray()) {
                String message = "It is not an array";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(arrayAssign),
                        NodeUtils.getColumn(arrayAssign),
                        message,
                        null)
                );
                return null;
            }
        }
        String typeName = "";
        if (arrayAssign.getChildren().size() > 0) {
            JmmNode childOperand = arrayAssign.getChildren().get(0);
            Type typeChildOperand = getExprType(childOperand, table, currentMethod);
            typeName = typeChildOperand.getName();

        }
        if (!typeName.equals("int")) {
            String message = "Array Index not int";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayAssign),
                    NodeUtils.getColumn(arrayAssign),
                    message,
                    null)
            );
            return null;
        }

        return null;
    }
}
