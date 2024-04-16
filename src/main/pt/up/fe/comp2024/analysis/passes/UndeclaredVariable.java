package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class UndeclaredVariable extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IDENTIFIER, this::visitVarRefExpr);
        addVisit(Kind.VAR_DECL, this::visitVarDecls);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {

        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var varRefName = varRefExpr.get("value");

        // Var is a field, return
        if (table.getFields().stream()
                .anyMatch(param -> param.getName().equals(varRefName))) {
            return null;
        }

        // Var is a parameter, return
        if (table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(varRefName))) {
            return null;
        }

        // Var is a declared variable, return
        if (table.getLocalVariables(currentMethod).stream()
                .anyMatch(varDecl -> varDecl.getName().equals(varRefName))) {
            return null;
        }

        if (table.getImports().stream()
                .anyMatch(param -> param.equals(varRefName))) {
            return null;
        }

        // Create error report
        var message = String.format("Variable '%s' does not exist.", varRefName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(varRefExpr),
                NodeUtils.getColumn(varRefExpr),
                message,
                null)
        );

        return null;
    }

    private Void visitVarDecls(JmmNode varDecl, SymbolTable table) {

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var varDeclName = varDecl.getOptional("name").orElse("");

        boolean doubleField = false;
        for (var field : table.getFields()) {
            if (field.getName().equals(varDeclName)) {
                if (doubleField) {
                    var message = String.format("Variable '%s' is already defined in the scope.", varDeclName);
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(varDecl),
                            NodeUtils.getColumn(varDecl),
                            message,
                            null)
                    );
                    return null;
                }
                doubleField = true;
            }
        }

        boolean doubleParameter = false;
        for (var param : table.getParameters(currentMethod)) {
            if (param.getName().equals(varDeclName)) {
                if (doubleParameter) {
                    var message = String.format("Variable '%s' is already defined in the scope.", varDeclName);
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(varDecl),
                            NodeUtils.getColumn(varDecl),
                            message,
                            null)
                    );
                    return null;
                }
                doubleParameter = true;
            }
        }

        boolean doubleLocal = false;
        for (var local : table.getLocalVariables(currentMethod)) {
            if (local.getName().equals(varDeclName)) {
                if (doubleLocal) {
                    var message = String.format("Variable '%s' is already defined in the scope.", varDeclName);
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(varDecl),
                            NodeUtils.getColumn(varDecl),
                            message,
                            null)
                    );
                    return null;
                }
                doubleLocal = true;
            }
        }

        return null;
    }
}
