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

public class WrongArrayAcess extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.CLASS_INSTANTIATION, this::visitWrongArray);
    }

    private Void visitWrongArray(JmmNode arrayDecl, SymbolTable table) {
        String method = arrayDecl.getJmmParent().getJmmParent().get("name");
        JmmNode leftOperand = arrayDecl.getChildren().get(0); //para dar erro este tem de começar por ser Arraydefinition
        String varNameToCheck = arrayDecl.get("className");

        if (leftOperand.getKind().equals("Arraydefinition")){
            for (var parameter : table.getParameters(method)){
                if (parameter.getType().getName().equals(varNameToCheck) && !parameter.getType().isArray()){
                    String message = "It is not an array";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(arrayDecl),
                            NodeUtils.getColumn(arrayDecl),
                            message,
                            null)
                    );
                    return null;                }
            }
            for (var localVariable : table.getLocalVariables(method)){
                if (localVariable.getName().equals(varNameToCheck) && !localVariable.getType().isArray()){
                    String message = "It is not an array";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(arrayDecl),
                            NodeUtils.getColumn(arrayDecl),
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
