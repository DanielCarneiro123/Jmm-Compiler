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

public class ClassNotImported extends AnalysisVisitor {

    private String currentMethod;
    private boolean tem_imports;

    @Override
    public void buildVisitor() {
        addVisit(Kind.FUNCTION_CALL, this::visitClassImport);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.CLASS_DECLARATION, this::visitImport_Extend);
        addVisit(Kind.ARGUMENT, this::visitVarArguments);
        //addVisit(Kind.VAR_DECL, this::visitVarDeclFields);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitImport_Extend(JmmNode classDecl, SymbolTable table) {
        String extendedName = classDecl.getOptional("extendedClass").orElse("");
        if (!extendedName.equals("")) {
            for (var imp : table.getImports()) {
                if (imp.equals(extendedName))
                    return null;
            }
            var message = String.format("Class not imported", extendedName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(classDecl),
                    NodeUtils.getColumn(classDecl),
                    message,
                    null)
            );
            return null;
        }
        return null;
    }

    private Void visitClassImport(JmmNode varRefExpr, SymbolTable table) {

        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var varRefName = varRefExpr.get("value");

        JmmNode varRefExprChild = varRefExpr.getJmmChild(0);
        Type childType = getExprType(varRefExprChild, table, currentMethod);


        if (!table.getMethods().stream().anyMatch(param -> param.equals(varRefName)) &&
                !table.getImports().stream().anyMatch(imp -> imp.equals(childType.getName())) &&
                !(childType.getName().equals(table.getClassName()) && !table.getSuper().equals(""))) {

            for (var imp : table.getImports()) {
                if (imp.equals(table.getSuper())) {
                    return null;
                } else {

                    var message = String.format("Class not imported", varRefName);
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
        }
        return null;
    }

    private Void visitVarDeclTYPES(JmmNode varDecl, SymbolTable table) {
        var varDeclType = getExprType(varDecl, table, currentMethod);
        var varDeclTypeName = varDeclType.getName();
        var className = table.getClassName();
        if (!varDeclTypeName.equals("int") && !varDeclTypeName.equals("boolean") && !varDeclTypeName.equals(className)) {
            for (var imp : table.getImports()) {
                if (imp.equals(varDeclTypeName)) {
                    return null;
                }
            }
            var message = String.format("Class not imported", varDeclTypeName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(varDecl),
                    NodeUtils.getColumn(varDecl),
                    message,
                    null)
            );
            return null;
        }
        return null;
    }

    private Void visitVarArguments(JmmNode argDecl, SymbolTable table) {
        var argDeclName = argDecl.get("name");
        String argDeclTypeName = "";
        for (var param : table.getParameters(currentMethod)) {
            var pramTypeName = param.getName();
            if (pramTypeName.equals(argDeclName)) {
                argDeclTypeName = param.getType().getName();
            }
            //talvez tenha de meter algo se o argDeclName não tiver nos param
        }

        if (!argDeclTypeName.equals("String") && !argDeclTypeName.equals("int") && !argDeclTypeName.equals("boolean") && !argDeclTypeName.equals(table.getClassName())) {
            for (var imp : table.getImports()) {
                if (imp.equals(argDeclTypeName)) {
                    return null;
                }
            }
            var message = String.format("Class not imported in Arguments", argDeclTypeName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(argDecl),
                    NodeUtils.getColumn(argDecl),
                    message,
                    null)
            );
            return null;
        }

        return null;

    }

    /*
    private Void visitVarDeclFields(JmmNode varDecl, SymbolTable table) {

        var varDeclType = getExprType(varDecl, table, currentMethod);
        var varDeclTypeName = varDeclType.getName();

        for (var field : table.getFields()) {
            if (!field.getName().equals("int") && !field.getName().equals("boolean") && !field.getName().equals(table.getClassName())) {
                if (table.getImports().contains(varDeclTypeName)) {
                var fieldType = field.getType();
                var fieldTypeName = fieldType.getName();
                if (fieldTypeName.equals(varDeclTypeName)) {
                    for (var imp : table.getImports()) {
                        if (imp.equals(varDeclTypeName)) {
                            return null;
                        }
                    }
                    var message = String.format("Not imported in Fields", varDeclTypeName);
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
        }
        return null;
    }*/
}
