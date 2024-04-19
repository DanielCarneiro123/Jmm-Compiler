package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;

public class ObjectAssign extends AnalysisVisitor {
    private String method;
    private boolean tem_imports;
    private String classParentName;
    private String extendedName;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.CLASS_DECLARATION, this::visitImport_Extend);
        addVisit(Kind.ASSIGNMENT, this::visitObjectAssign);
    }

    private Void visitMethodDecl(JmmNode currMethod, SymbolTable table) {
        method = currMethod.get("name");
        return null;
    }

    private Void visitImport_Extend(JmmNode classDecl, SymbolTable table) {
        classParentName = classDecl.get("className");
        extendedName = classDecl.getOptional("extendedClass").orElse("");
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

    private Void visitObjectAssign(JmmNode assigment, SymbolTable table) {
        JmmNode assigmentChild = assigment.getChild(0);
        Type assigmentChildType = getExprType(assigmentChild, table, method);
        var assigmentChildName = assigmentChildType.getName();
        String kindName = assigmentChild.getKind();
        if (assigmentChildName.equals("int") || assigmentChildName.equals("boolean") || assigmentChildName.equals("String") || assigmentChildName.equals(table.getClassName())) {
            return null;
        }
        String assigmentName = assigment.get("var");
        for (Symbol field : table.getFields()) {
            Boolean isStatic = Boolean.parseBoolean(assigment.getParent().get("isStatic"));
            if (field.getName().equals(assigmentName)) {
                if (isStatic) {
                    String message = "Non-static field cannot be referenced from a static context";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(assigment),
                            NodeUtils.getColumn(assigment),
                            message,
                            null)
                    );
                    return null;
                } else {
                    return null;
                }
            }
        }
        if (extendedName.equals("")) {
            for (Symbol local : table.getLocalVariables(method)) {
                if (local.getName().equals(assigmentName)) {
                    Type assigmentType = local.getType();
                    String assigmentTypeName = assigmentType.getName();

                    var teste0 = table.getImports().stream().anyMatch(param1 -> param1.equals(assigmentTypeName));
                    var teste1 = extendedName.equals(assigmentTypeName);
                    var teste2 = assigmentChildName.equals(classParentName);

                    var teste3 = table.getImports().stream().anyMatch(param1 -> param1.equals(assigmentChildName));
                    var teste4 = extendedName.equals(assigmentChildName);
                    var teste5 = assigmentTypeName.equals(classParentName);


                    /*if ((!assigmentChildName.equals(assigmentTypeName) && !(table.getImports().stream().anyMatch(param1 -> param1.equals(assigmentChildName) && table.getImports().stream().anyMatch(param2 -> param2.equals(assigmentTypeName))))) &&
                            (!((table.getImports().stream().anyMatch(param3 -> param3.equals(assigmentChildName) && (assigmentTypeName.equals("int") || assigmentTypeName.equals("boolean")))) || ((table.getImports().stream().anyMatch(param4 -> param4.equals(assigmentTypeName)) && (assigmentChildName.equals("int") || assigmentChildName.equals("boolean"))))) ||
                                    ((teste0 && teste1 && teste2) &&
                                            (teste3 && teste4 && teste5)))) {
                        String message = "Object is not imported";
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(assigment),
                                NodeUtils.getColumn(assigment),
                                message,
                                null)
                        );
                        return null;
                    }*/
                }
            }

            for (Symbol field : table.getFields()) {
                if (field.getName().equals(assigmentName)) {
                    Type assigmentType = field.getType();
                    String assigmentTypeName = assigmentType.getName();
                    if (!table.getImports().stream().anyMatch(param -> param.equals(assigmentTypeName))) {
                        String message = "Object is not imported";
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
            for (Symbol param : table.getParameters(method)) {
                if (param.getName().equals(assigmentName)) {
                    Type assigmentType = param.getType();
                    String assigmentTypeName = assigmentType.getName();
                    if (!table.getImports().stream().anyMatch(param1 -> param1.equals(assigmentTypeName))) {
                        String message = "Object is not imported";
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
            if (!kindName.equals("NewClass") && !extendedName.equals("")) {
                if (!(assigmentChildName.equals(classParentName) && tem_imports)) {
                    String message = "Object does not extend import";
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
        return null;
    }
}