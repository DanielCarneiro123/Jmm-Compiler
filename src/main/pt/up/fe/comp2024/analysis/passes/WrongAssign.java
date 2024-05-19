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

public class WrongAssign extends AnalysisVisitor {
    private String method;
    private boolean tem_imports;

    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        //addVisit(Kind.STMT, this::visitWrongAssign);
        addVisit(Kind.CLASS_DECLARATION, this::visitImport_Extend);
        addVisit(Kind.ASSIGNMENT, this::visitWrongAssign2);
        addVisit(Kind.ARRAY_ASSIGN, this::visitWrongAssign3);
    }

    private Void visitMethodDecl(JmmNode currMethod, SymbolTable table) {
        method = currMethod.get("name");
        return null;
    }

    private Void visitImport_Extend(JmmNode classDecl, SymbolTable table) {
        String extendedName = classDecl.getOptional("extendedClass").orElse("");
        for (int i = 0; i < classDecl.getParent().getChildren().size() - 1; i++) {
            JmmNode child = classDecl.getParent().getChildren().get(i);
            String childName = child.get("ID");
            if (childName.equals(extendedName)) {
                tem_imports = true;
                return null;
            }
        }
        tem_imports = false;
        return null;
    }

    private Void visitWrongAssign2(JmmNode assigment, SymbolTable table) {
        var assigmentChil = assigment.getChildren();
        var assigmentType = getExprType(assigment, table, method);
        var assigmentTypeName = assigmentType.getName();
        var assigmentChilType = getExprType(assigmentChil.get(0), table, method);

        var isThisTest = assigmentChil.get(0).getOptional("value").orElse("");
        var isThis = isThisTest.equals("this");
        if (isThis) return null;

        if (assigmentChilType.equals(assigmentType)) {
            return null;
        } else {
            for (var imp : table.getImports()) {
                if (imp.equals(assigmentTypeName) || imp.equals(assigmentChilType.getName())) {
                    if (imp.equals(assigmentTypeName)) {
                        for (var imp2 : table.getImports()) {
                            if (imp2.equals(assigmentChilType.getName())) {
                                return null;
                            }
                        }
                    }
                    if (imp.equals(assigmentChilType.getName())) {
                        for (var imp2 : table.getImports()) {
                            if (imp2.equals(assigmentTypeName)) {
                                return null;
                            }
                            if (imp2.equals(imp)) {
                                return null;
                            }
                        }
                    }
                    if (assigmentChilType.getName().equals(table.getClassName()) && imp.equals(assigmentTypeName) && imp.equals(table.getSuper())) {
                        return null;
                    } else if (assigmentTypeName.equals(table.getClassName()) && imp.equals(assigmentChilType.getName()) && imp.equals(table.getSuper())) {
                        return null;
                    } else {
                        var assigmentChilChilValue = "";
                        if (assigmentChil.get(0).getChildren().size() > 0) {
                            assigmentChilChilValue = assigmentChil.get(0).getChildren().get(0).getOptional("value").orElse("");
                        }
                        for (var imp2 : table.getImports()) {
                            if (imp2.equals(assigmentChilChilValue) && !assigmentTypeName.equals(table.getSuper()) && !assigmentChilChilValue.equals(table.getSuper())) {
                                return null;
                            }
                        }
                        for (var imp3 : table.getImports()) {
                            if (imp3.equals(assigmentType.getName()) && !assigmentType.getName().equals(table.getSuper()) && !assigmentChilChilValue.equals(table.getSuper())) {
                                return null;
                            }
                        }
                        String message = "Wrong Assign Types";
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(assigment),
                                NodeUtils.getColumn(assigment),
                                message,
                                null)
                        );
                        return null;
                    }
                }
            }
            String message = "Wrong Assign Types";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assigment),
                    NodeUtils.getColumn(assigment),
                    message,
                    null)
            );
            return null;
        }

    }

    private Void visitWrongAssign3(JmmNode assigment, SymbolTable table) {
        var assigmentChil = assigment.getChildren();
        var assigmentType = getExprType(assigment, table, method);
        var assigmentTypeName = assigmentType.getName();
        var assigmentChilType = getExprType(assigmentChil.get(0), table, method);

        var isThisTest = assigmentChil.get(0).getOptional("value").orElse("");
        var isThis = isThisTest.equals("this");
        if (isThis) return null;

        if (assigmentChilType.equals(assigmentType)) {
            return null;
        } else {
            for (var imp : table.getImports()) {
                if (imp.equals(assigmentTypeName) || imp.equals(assigmentChilType.getName())) {
                    if (imp.equals(assigmentTypeName)) {
                        for (var imp2 : table.getImports()) {
                            if (imp2.equals(assigmentChilType.getName())) {
                                return null;
                            }
                        }
                    }
                    if (imp.equals(assigmentChilType.getName())) {
                        for (var imp2 : table.getImports()) {
                            if (imp2.equals(assigmentTypeName)) {
                                return null;
                            }
                            if (imp2.equals(imp)) {
                                return null;
                            }
                        }
                    }
                    if (assigmentChilType.getName().equals(table.getClassName()) && imp.equals(assigmentTypeName) && imp.equals(table.getSuper())) {
                        return null;
                    } else if (assigmentTypeName.equals(table.getClassName()) && imp.equals(assigmentChilType.getName()) && imp.equals(table.getSuper())) {
                        return null;
                    } else {
                        var assigmentChilChilValue = "";
                        if (assigmentChil.get(0).getChildren().size() > 0) {
                            assigmentChilChilValue = assigmentChil.get(0).getChildren().get(0).getOptional("value").orElse("");
                        }
                        for (var imp2 : table.getImports()) {
                            if (imp2.equals(assigmentChilChilValue) && !assigmentTypeName.equals(table.getSuper()) && !assigmentChilChilValue.equals(table.getSuper())) {
                                return null;
                            }
                        }
                        for (var imp3 : table.getImports()) {
                            if (imp3.equals(assigmentType.getName()) && !assigmentType.getName().equals(table.getSuper()) && !assigmentChilChilValue.equals(table.getSuper())) {
                                return null;
                            }
                        }
                        String message = "Wrong Assign Types";
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(assigment),
                                NodeUtils.getColumn(assigment),
                                message,
                                null)
                        );
                        return null;
                    }
                }
            }
            String message = "Wrong Assign Types";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assigment),
                    NodeUtils.getColumn(assigment),
                    message,
                    null)
            );
            return null;
        }

    }


    private Void visitWrongAssign(JmmNode stmtDecl, SymbolTable table) {
        for (JmmNode operand : stmtDecl.getChildren()) {
            JmmNode parentOperand = operand.getJmmParent();
            Type rightOperandType = getExprType(operand, table, method);


            for (var parameter : table.getLocalVariables(method)) {
                if (parentOperand.getOptional("var").orElse("").equals(parameter.getName()) && !rightOperandType.getName().equals(parameter.getType().getName()) && !rightOperandType.getName().equals("object") && !table.getImports().stream().anyMatch(param -> param.equals(parentOperand.getOptional("var").orElse(""))) && !table.getImports().stream().anyMatch(param -> param.equals(parameter.getType().getName()))) {
                    String message = "Wrong Assign Types";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(stmtDecl),
                            NodeUtils.getColumn(stmtDecl),
                            message,
                            null)
                    );
                    return null;
                }
            }
            for (var parameter : table.getParameters(method)) {
                if (parentOperand.getOptional("var").orElse("").equals(parameter.getName()) && !rightOperandType.getName().equals(parameter.getType().getName()) && !rightOperandType.getName().equals("object") && tem_imports) {
                    String message = "Wrong Assign Types";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(stmtDecl),
                            NodeUtils.getColumn(stmtDecl),
                            message,
                            null)
                    );
                    return null;
                }
            }
            for (var parameter : table.getFields()) {
                String parameterName = parameter.getName();
                if (parameterName.equals(parameter.getName()) && !rightOperandType.getName().equals(rightOperandType) && !rightOperandType.getName().equals("object") && tem_imports) {
                    String message = "Wrong Assing Types";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(stmtDecl),
                            NodeUtils.getColumn(stmtDecl),
                            message,
                            null)
                    );
                    return null;
                }
                String stmtDeclName = stmtDecl.getOptional("name").orElse("");
                if (stmtDeclName.equals("")) {
                    stmtDeclName = stmtDecl.getOptional("var").orElse("");
                }
                Type stmtDeclType = getExprType(stmtDecl, table, method);
                if (parameterName.equals(stmtDeclName)) {
                    //então o tipo do parameter é igual ao tipo do stmtdecl
                    //e vamos ver o tipo da direita:
                    if (!stmtDeclType.equals(rightOperandType)) {
                        String message = "Wrong Assing Types";
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(stmtDecl),
                                NodeUtils.getColumn(stmtDecl),
                                message,
                                null)
                        );
                        return null;
                    }
                }
            }

        }
        return null;
    }
}