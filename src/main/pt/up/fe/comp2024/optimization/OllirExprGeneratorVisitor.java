package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(IDENTIFIER, this::visitVarRef);
        addVisit(BINARY_OP, this::visitBinExpr);
        addVisit(INTEGER, this::visitInteger);
        addVisit(FUNCTION_CALL, this::visitFunctionCall);


        setDefaultVisit(this::defaultVisit);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        Type resType = TypeUtils.getExprType(node.getJmmChild(0), table);


        // code to compute self

        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);


        computation.append(node.get("op")).append(OptUtils.toOllirType(resType)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitFunctionCall(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        var child = node;
        code.append("invokestatic");
        code.append("(");
        code.append(child.getChildren().get(0).get("value"));
        code.append(",");
        code.append("\"");
        code.append(child.get("value"));
        code.append("\"");
        code.append(",");

        List<Symbol> localVariables = table.getLocalVariables(node.getParent().getParent().get("name"));

// Get the local variables for the current method
        List<JmmNode> arguments = child.getChildren().subList(1, child.getNumChildren());
        for (int i = 0; i < arguments.size(); i++) {
            JmmNode argument = arguments.get(i);
            String argumentName = argument.get("value");
            // Search for the matching local variable by name
            Optional<Symbol> matchingVariable = localVariables.stream()
                    .filter(variable -> variable.getName().equals(argumentName))
                    .findFirst();
            if (matchingVariable.isPresent()) {
                code.append(argumentName);
                Type argType = matchingVariable.get().getType();
                code.append(OptUtils.toOllirType(argType)); // Append the type
            }
            if (i < arguments.size() - 1) {
                code.append(", ");
            }
        }



        code.append(")");

        if (child.getParent().getKind().equals("Assignment")) {

            String variableName = child.getParent().get("var");


            Optional<Symbol> matchingVariable = localVariables.stream()
                    .filter(variable -> variable.getName().equals(variableName))
                    .findFirst();

            if (matchingVariable.isPresent()) {
                Type parentType = matchingVariable.get().getType();
                code.append(OptUtils.toOllirType(parentType));

        } }else {
            code.append(".V");
        }


        return new OllirExprResult(code.toString());
    }

    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("value");
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        String code = id + ollirType;

        return new OllirExprResult(code);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
