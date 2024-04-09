package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table, String currMethod) {
        // TODO: Simple implementation that needs to be expanded

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case IDENTIFIER -> getVarExprType(expr, table, currMethod);
            case ARRAYDEFINITION -> new Type(INT_TYPE_NAME, true);
            case INTEGER -> new Type(INT_TYPE_NAME, false);
            case BINARY_EXPR -> getBinExprType(expr);
            case BINARY_OP -> getBinExprType(expr);
            case FUNCTION_CALL -> getFunctionType(expr, table);
            //case NEW_CLASS -> new Type("object", false);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getFunctionType(JmmNode expr, SymbolTable table) {
        String methodName = expr.get("value");
        Type type = table.getReturnType(methodName);

        return type; //aqui devia dar erro porque é um tipo que não existe (?)
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        // TODO: Simple implementation that needs to be expanded

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*" -> new Type(INT_TYPE_NAME, false);
            case "==" -> new Type("boolean", false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }


    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table, String currMethod) {
        String varName = varRefExpr.get("value");
        var locals = table.getLocalVariables(currMethod);
        for (var local : locals) {
            if (local.getName().equals(varName)) {
                return local.getType();
            }
        }
        if (varName.equals("true") || varName.equals("false")) {
            return new Type("boolean", false);
        }
        return new Type(INT_TYPE_NAME, false);
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: Simple implementation that needs to be expanded
        return sourceType.getName().equals(destinationType.getName());
    }
}
