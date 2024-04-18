package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.HashSet;

public class DuplicatedParam extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.PROGRAM, this::visitDuplicatedParam);
    }
    private Void visitDuplicatedParam(JmmNode program1, SymbolTable table) {
        var methods = table.getMethods();
        var uniqueMethods = new HashSet<>();
        var duplicatedMethods = new HashSet<>();

        for (var method : methods) {
            for (var param : table.getParameters(method)) {
                if (!uniqueMethods.add(param)) {
                    duplicatedMethods.add(param);
                }
            }
        }

        if (!duplicatedMethods.isEmpty()) {
            String message = "Duplicated Param ";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(program1),
                    NodeUtils.getColumn(program1),
                    message,
                    null)
            );
            return null;
        } else {
            return null;
        }
    }
}
