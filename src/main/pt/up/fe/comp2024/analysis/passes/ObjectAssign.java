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
        String assigmentChildName = assigmentChildType.getName();
        String kindName = assigmentChild.getKind();

        if (extendedName.equals("")) {
            String assigmentName = assigment.get("var");
            for (Symbol local : table.getLocalVariables(method)) {
                if (local.getName().equals(assigmentName)) {
                    Type assigmentType = local.getType();
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

            /*
            if (!table.getImports().stream().anyMatch(param -> param.equals(assigmentType))) {
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
        return null;
    }
}