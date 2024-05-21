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

import java.util.List;

import static pt.up.fe.comp2024.ast.TypeUtils.getExprType;

public class IncompatibleArguments extends AnalysisVisitor {
    private String method;
    private boolean tem_imports;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.FUNCTION_CALL, this::visitIncompatibleArguments);
    }

    private Void visitMethodDecl(JmmNode currMethod, SymbolTable table) {
        method = currMethod.get("name");
        return null;
    }

    private Void visitIncompatibleArguments(JmmNode functionCall, SymbolTable table) {
        var caller = functionCall.getChildren().get(0);
        var callerType = getExprType(caller, table, method);
        Boolean isVararg = callerType.getName().equals("Varargs");

        for (int i = 1; i < functionCall.getChildren().size(); i++) {
            JmmNode child = functionCall.getChildren().get(i);
            Type typeChild = getExprType(child, table, method);
            if (isVararg) {
                if (!typeChild.getName().equals("int")) {
                    String message = "Incompatible Argument";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(functionCall),
                            NodeUtils.getColumn(functionCall),
                            message,
                            null)
                    );
                    return null;
                }
            }
        }
        if (isVararg) {
            return null;
        }


        for (var imp : table.getImports()) {
            if (imp.equals(callerType.getName())) {
                return null;
            }
        }
        if (callerType.getName().equals(table.getClassName())) {
            var functionCallName = functionCall.get("value");

            var existe_nesta_class = false;
            for (var method : table.getMethods()) {
                if (method.equals(functionCallName)) {
                    existe_nesta_class = true;
                }
            }
            if (!existe_nesta_class) {
                return null;
            }

            var functionCallParams = table.getParameters(functionCallName);

            if (functionCallParams.size() != functionCall.getChildren().size() - 1) {
                String message = "Incompatible Arguments";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(functionCall),
                        NodeUtils.getColumn(functionCall),
                        message,
                        null)
                );
                return null;
            }
            for (int i = 0; i < functionCallParams.size(); i++) {
                var param = functionCall.getChildren().get(i + 1);
                var paramType = getExprType(param, table, method);
                if (!paramType.equals(functionCallParams.get(i).getType())) {
                    String message = "Incompatible Arguments";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(functionCall),
                            NodeUtils.getColumn(functionCall),
                            message,
                            null)
                    );
                    return null;
                }
            }


            var funcName = functionCall.get("value");
            List<Symbol> realParam = null;
            for (var args : table.getMethods()) {
                if (args.equals(funcName)) {
                    realParam = table.getParameters(funcName);

                }

            }
            if (realParam == null) {
                String message = "Incompatible Arguments";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(functionCall),
                        NodeUtils.getColumn(functionCall),
                        message,
                        null)
                );
                return null;
            }


            List<JmmNode> paramsPassed = functionCall.getChildren(Kind.EXPR).subList(1, functionCall.getChildren(Kind.EXPR).size());
            for (int i = 0; i < realParam.size(); i++) {
                if (realParam.get(i).getType().hasAttribute("varArg")) {
                    return null;
                }
            }
            if (realParam.size() < paramsPassed.size()) {
                String message = "Too Many Arguments Added";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(functionCall),
                        NodeUtils.getColumn(functionCall),
                        message,
                        null)
                );
                return null;
            }
            if (realParam.size() > paramsPassed.size()) {
                String message = "Few Arguments Added";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(functionCall),
                        NodeUtils.getColumn(functionCall),
                        message,
                        null)
                );
                return null;
            }

            for (int i = 0; i < paramsPassed.size() - 1; i++) {
                var paramPassedType = getExprType(paramsPassed.get(i), table, method);
                if (!paramPassedType.equals(realParam.get(i).getType())) {
                    String message = "Invalid Arguments";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(functionCall),
                            NodeUtils.getColumn(functionCall),
                            message,
                            null)
                    );
                    return null;
                }
            }

            var functionCallValue = functionCall.get("value");

            for (int i = 0; i < functionCall.getChildren().size(); i++) {
                JmmNode functionCallChild = functionCall.getChildren().get(i);
                var functionCallChildType = getExprType(functionCallChild, table, method);
                var functionCallChildTypeName = getExprType(functionCallChild, table, method).getName();

                if (functionCallChildTypeName.equals("Varargs")) {
                    return null;
                }

                if (functionCallChild.getKind().equals("BinaryOp")) {
                    String message = "Incompatible Argument";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(functionCall),
                            NodeUtils.getColumn(functionCall),
                            message,
                            null)
                    );
                    return null;
                }

                for (var imp : table.getImports()) {
                    if (imp.equals(functionCallChildTypeName)) {
                        return null;
                    }
                    if (table.getClassName().equals(functionCallChildTypeName)) {
                        var funcParam = table.getParameters(functionCallValue);
                        if (funcParam.size() != functionCall.getChildren().size() - 1) {
                            String message = "Incompatible Argument";
                            addReport(Report.newError(
                                    Stage.SEMANTIC,
                                    NodeUtils.getLine(functionCall),
                                    NodeUtils.getColumn(functionCall),
                                    message,
                                    null)
                            );
                            return null;
                        } else if (funcParam.size() > 0) {
                            for (int j = 0; j < functionCall.getChildren().size(); j++) {
                                JmmNode paramChamar = functionCall.getChildren().get(j + 1);
                                Type paramChamarType = getExprType(paramChamar, table, method);
                                Symbol paramFunc = funcParam.get(j);
                                Type paramFuncType = paramFunc.getType();
                                if (!paramChamarType.equals(paramFuncType)) {
                                    String message = "Incompatible Argument";
                                    addReport(Report.newError(
                                            Stage.SEMANTIC,
                                            NodeUtils.getLine(functionCall),
                                            NodeUtils.getColumn(functionCall),
                                            message,
                                            null)
                                    );
                                    return null;
                                }
                                return null;
                            }
                        }
                        return null;
                    }
                }
            }

            var sizeParamChamada = functionCall.getChildren().size();
            for (int i = 1; i < functionCall.getChildren().size(); i++) {
                JmmNode child = functionCall.getChildren().get(i);
                Type typeChild = getExprType(child, table, method);
                var sizeParamFunc = table.getParameters(functionCallValue).size();

                if (sizeParamChamada != sizeParamFunc) {
                    if (!table.getParameters(functionCallValue).get(i - 1).getType().equals(typeChild)) {
                        String message = "Incompatible Argument";
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(functionCall),
                                NodeUtils.getColumn(functionCall),
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