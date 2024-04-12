package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class UndeclaredMethod extends AnalysisVisitor {
    private String method;
    private boolean tem_imports;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.NEW_CLASS, this::visitUndeclaredMethod);
        addVisit(Kind.CLASS_DECLARATION, this::visitImport_Extend);
    }

    private Void visitMethodDecl(JmmNode currMethod, SymbolTable table) {
        method = currMethod.get("name");
        return null;
    }

    private Void visitImport_Extend(JmmNode classDecl, SymbolTable table) {
        //String extendedName = classDecl.getOptional("extendedClass").orElse("");
        for (int i = 0; i < classDecl.getParent().getChildren().size() - 1; i++) {
            JmmNode child = classDecl.getParent().getChildren().get(i);
            //String childName = child.get("ID");
            //if (childName.equals(extendedName)) {
            tem_imports = true;
            return null;
            //}
        }
        tem_imports = false;
        return null;
    }

    private Void visitUndeclaredMethod(JmmNode newClass, SymbolTable table) {
        String newClassKind = newClass.getKind();
        String className = newClass.get("classname");

        if (!tem_imports) {
            if (newClassKind.equals("NewClass")) {
                if (!table.getMethods().stream()
                        .anyMatch(param -> param.equals(className))) {
                    String message = "Undeclared Method";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(newClass),
                            NodeUtils.getColumn(newClass),
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
