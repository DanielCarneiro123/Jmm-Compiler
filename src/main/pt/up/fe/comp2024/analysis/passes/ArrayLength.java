package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;

public class ArrayLength extends AnalysisVisitor {
    private String method;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.LENGTH, this::visitLength);
        //addVisit(Kind.EXPR, this::visitArrayAcess);
    }

    private Void visitMethodDecl(JmmNode currMethod, SymbolTable table) {
        method = currMethod.get("name");
        return null;
    }

    private Void visitLength(JmmNode currLenght, SymbolTable table) {
        var currLenghtTeste = currLenght.get("len");
        var currLenghtIsArray = getExprType(currLenght.getChildren().get(0), table, method).isArray();
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
        if (currLenghtTeste.equals("length") && !currLenghtIsArray) {
            String message = "its not an Array to see length";
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

    /*private Void visitArrayAcess(JmmNode expr, SymbolTable table) {
        if (expr.getKind().equals("ClassInstantiation")) {
            String exprName = expr.get("className");
            JmmNode exprChild = expr.getChild(0);
            JmmNode exprChildChild = exprChild.getChild(0);
            String indexAcess = exprChildChild.getOptional("value").orElse("99999");
            for (var local : table.getLocalVariables(method)) {
                if (local.getName().equals(exprName)) {
                    return null;
                }
            }

            return null;
        }
        return null;
    }*/
}
