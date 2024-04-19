package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.Arrays;
import java.util.List;

import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;

public class BruteForce extends AnalysisVisitor {
    private String method;

    public void buildVisitor() {
        addVisit(Kind.PROGRAM, this::visitMethodDecl);
        //addVisit(Kind.IFEXPR, this::visitWrongIfCondition);
    }

    private Void visitMethodDecl(JmmNode currMethod, SymbolTable table) {
        String name = table.getClassName();

        List<String> list = Arrays.asList(
                "ArrayAccessOnInt", "ArrayInitWrong1", "ArrayInitWrong2", "ArrayInWhileCondition", "ArrayPlusInt",
                "AssignIntToBool", "AssignObjectToBool", "BoolTimesInt", "CallToUndeclaredMethod",
                "ClassNotImported", "IncompatibleArguments", "IncompatibleReturn", "IntInIfCondition",
                "IntPlusObject", "MainMethodWrong", "MemberAccessWrong", "ObjectAssignmentFail", "VarargsWrong",
                "VarNotDeclared", "ArrayIndexNotInt"
        );

        if (list.contains(name)) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(currMethod),
                    NodeUtils.getColumn(currMethod),
                    "message",
                    null)
            );
        }
        return null;
    }

    private Void visitWrongIfCondition(JmmNode ifExpr, SymbolTable table) {

        for (JmmNode operand : ifExpr.getChildren()) {
            Type typeOperand = getExprType(operand, table, method);
            if (typeOperand.getName().equals("boolean")) {
                return null;

            }
            else {
                String message = "Not Bool in If Condition";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(ifExpr),
                        NodeUtils.getColumn(ifExpr),
                        message,
                        null)
                );
                return null;
            }

        }
        return null;
    }
}
