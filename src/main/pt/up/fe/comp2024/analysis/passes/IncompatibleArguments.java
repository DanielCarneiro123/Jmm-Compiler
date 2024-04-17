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

public class IncompatibleArguments extends AnalysisVisitor {
    private String method;
    private boolean tem_imports;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.FUNCTION_CALL, this::visitIncompatibleArguments);
        addVisit(Kind.CLASS_DECLARATION, this::visitImport_Extend);
        addVisit(Kind.CLASS_INSTANTIATION, this::visitIncompatibleArguments2);
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
            //if (childName.equals(extendedName)) {
            tem_imports = true;
            return null;
            //}
        }
        tem_imports = false;
        return null;
    }

    private Void visitIncompatibleArguments(JmmNode functionCall, SymbolTable table) {
        var functionCallValue = functionCall.get("value");

        for (int i = 0; i < functionCall.getChildren().size(); i++) {
            JmmNode functionCallChild = functionCall.getChildren().get(i);
            var functionCallChildType = getExprType(functionCallChild, table, method);
            var functionCallChildTypeName = getExprType(functionCallChild, table, method).getName();

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

            if (!functionCallValue.equals("varargs") && sizeParamChamada != sizeParamFunc) {
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
            if (functionCallValue.equals("varargs")) {
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


        return null;
    }

    private Void visitIncompatibleArguments2(JmmNode classInst, SymbolTable table) {
        var classInstKind = classInst.getKind();
        if (classInstKind.equals("ClassInstantiation")) {
            var classInstChild = classInst.getChild(0);
            var classInstChildName = classInstChild.getKind();
            if (classInstChildName.equals("Parentesis")) {
                var classInstChildChild = classInstChild.getChild(0);
                var classInstChildChildKind = classInstChildChild.getKind();
                if (classInstKind.equals(classInstChildChildKind)) {
                    String message = "Incompatible Argument";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(classInst),
                            NodeUtils.getColumn(classInst),
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
