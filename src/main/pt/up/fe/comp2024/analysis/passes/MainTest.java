package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.HashSet;

public class MainTest extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.PROGRAM, this::visitDuplicatedMethods);
        addVisit(Kind.FUNCTION_CALL, this::visitFunctioCallinMain);
    }


    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        if (currentMethod.equals("main")) {

            var isStatic = method.get("isStatic").equals("true");
            if (!isStatic) {
                String message = "Method main must be static";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
                return null;
            }

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

    private Void visitFunctioCallinMain(JmmNode functionCall, SymbolTable table) {
        if (currentMethod.equals("main")) {
            var functionCallChild = functionCall.getChild(0);
            var isThis = functionCallChild.get("value").equals("this");
            if (isThis) {
                String message = "This cannot be used, main is static";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(functionCall),
                        NodeUtils.getColumn(functionCall),
                        message,
                        null)
                );
                return null;
            }
        }
        return null;
    }

    private Void visitDuplicatedMethods(JmmNode program, SymbolTable table) {
        var methods = table.getMethods();
        var uniqueMethods = new HashSet<>();
        var duplicatedMethods = new HashSet<>();

        for (String method : methods) {
            if (!uniqueMethods.add(method)) {
                duplicatedMethods.add(method);
            }
        }

        if (!duplicatedMethods.isEmpty()) {
            String message = "Duplicated Methods ";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(program),
                    NodeUtils.getColumn(program),
                    message,
                    null)
            );
            return null;
        } else {
            return null;
        }
    }
}
