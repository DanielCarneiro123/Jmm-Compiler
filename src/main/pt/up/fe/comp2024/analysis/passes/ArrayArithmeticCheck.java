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
        String method = binaryExpr.getJmmParent().getJmmParent().getOptional("name").orElse("");

        if (operator.equals("==") || operator.equals("/=")) {
            return null;
        } else if (isArithmeticOperator(operator)) {
            JmmNode leftOperand = binaryExpr.getChildren().get(0);
            JmmNode rightOperand = binaryExpr.getChildren().get(1);
            Type type1 = getExprType(leftOperand, table, method);
            Type type2 = getExprType(rightOperand, table, method);

            if (type1.isArray() || type2.isArray()) {
                String message = "Arrays cannot be used in arithmetic operations.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        message,
                        null)
                );
                return null;
            }

            if (type1.getName().equals("boolean") || type2.getName().equals("boolean")) {
                String message = "Booleans cannot be used in arithmetic operations.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        message,
                        null)
                );
                return null;
            }

            if (!type1.getName().equals("int") || !type2.getName().equals("int")) {
                String message = "Objects cannot be used in arithmetic operations.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        message,
                        null)
                );
                return null;
            }

        } else if (isBooleanOperator(operator)) {
            JmmNode leftOperand = binaryExpr.getChildren().get(0);
            JmmNode rightOperand = binaryExpr.getChildren().get(1);
            Type type1 = getExprType(leftOperand, table, method);
            Type type2 = getExprType(rightOperand, table, method);
            /*if (type1.isArray() || type2.isArray()) {
                String message = "Arrays cannot be used in boolean operations.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        message,
                        null)
                );
                return null;
            }*/
            if (type1.getName().equals("int") || type2.getName().equals("int")) {
                String message = "Int cannot be used in boolean operations.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        message,
                        null)
                );
                return null;
            }
            if (!type1.getName().equals("boolean") || !type2.getName().equals("boolean")) {
                String message = "Objects cannot be used in boolean operations.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryExpr),
                        NodeUtils.getColumn(binaryExpr),
                        message,
                        null)
                );
                return null;
            }
        }
        return null;
    }

    private boolean isArithmeticOperator(String operator) {
        return operator.equals("+") || operator.equals("-") ||
                operator.equals("*") || operator.equals("/") ||
                operator.equals("-=") || operator.equals("+=")
                || operator.equals("*=") || operator.equals("<=") || operator.equals(">=")
                || operator.equals("<") || operator.equals(">");
    }

    private boolean isBooleanOperator(String operator) {
        return operator.equals("&&") || operator.equals("||");
    }
}
