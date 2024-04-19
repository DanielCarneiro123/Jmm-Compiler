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
public class WrongFields extends AnalysisVisitor {
    private String method;

    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_DECL, this::visitFields);
    }

    private Void visitMethodDecl(JmmNode currMethod, SymbolTable table) {
        method = currMethod.get("name");
        return null;
    }

    private Void visitFields(JmmNode varDecl, SymbolTable table) {
        var varType = getExprType(varDecl, table, method);
        if (varType.getName().equals("int") || varType.getName().equals("boolean") || varType.getName().equals("String") || varType.getName().equals(table.getClassName())){
            return null;
        }
        else {
            for (var imp: table.getImports()){
                if (imp.equals(varType.getName())){
                    return null;
                }
            }
        }
        String message = "Variable Type is not valid";
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
