package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

/**
 * Checks if an array is used in arithmetic operations.
 */
public class ArrayArithmeticCheck extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.BINARY_OP, this::visitBinaryExpr);
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        String operator = binaryExpr.get("op");

        // Check if the operator is an arithmetic operation
        if (isArithmeticOperator(operator)) {
            // Check if any operand is an array
            JmmNode leftOperand = binaryExpr.getChildren().get(0);
            JmmNode rightOperand = binaryExpr.getChildren().get(1);

            if (isArrayType(leftOperand) || isArrayType(rightOperand) || isBooleanType(leftOperand) || isBooleanType(rightOperand)) {
                // Create error report
                String message = "Arrays cannot be used in arithmetic operations.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        message,
                        null)
                );
            }
        }

        return null;
    }

    private boolean isArithmeticOperator(String operator) {
        return operator.equals("+") || operator.equals("-") ||
                operator.equals("*") || operator.equals("/") ||
                operator.equals("%");
    }

    private boolean isArrayType(JmmNode operand) {
        String typeName = operand.get("type");
        return typeName.endsWith("[]");
    }

    private boolean isBooleanType(JmmNode operand) {
        String typeName = operand.get("type");
        return typeName.endsWith("[]");
    }

}
