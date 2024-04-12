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
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitImport_Extend(JmmNode classDecl, SymbolTable table) {
        String extendedName = classDecl.getOptional("extendedClass").orElse("");
        for (int i = 0; i < classDecl.getParent().getChildren().size() - 1; i++) {
            JmmNode child = classDecl.getParent().getChildren().get(i);
            String childName = child.get("ID");
            //if (childName.equals(extendedName)) {
            tem_imports = true;
            return null;
            //}
        }
        tem_imports = false;
        return null;
    }

    private Void visitClassImport(JmmNode varRefExpr, SymbolTable table) {

        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var varRefName = varRefExpr.get("value");

        JmmNode varRefExprChild = varRefExpr.getJmmChild(0);
        Type childType = getExprType(varRefExprChild, table, currentMethod);


        if (!table.getMethods().stream()
                .anyMatch(param -> param.equals(varRefName)) && !table.getImports().stream().anyMatch(imp -> imp.equals(childType.getName())) && !(childType.getName().equals(table.getClassName()) && !table.getSuper().equals(""))) {
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

        return null;
    }
}
