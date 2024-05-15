package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";

    private static final String BOOLEAN_TYPE_NAME = "boolean";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    public static String getBooleanTypeName() {
        return BOOLEAN_TYPE_NAME;
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */

    public static Type getExprType(JmmNode expr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case IDENTIFIER -> getVarExprType(expr, table);
            case INTEGER -> new Type(INT_TYPE_NAME, false);
            case BOOLEAN -> new Type("boolean", false);
            case BRACKETS -> getExprType(expr.getChild(0), table);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    public static Type getExprType(JmmNode expr, SymbolTable table, String currMethod) {
        // TODO: Simple implementation that needs to be expanded

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case IDENTIFIER, VAR_DECL, INT, ID -> getVarExprType(expr, table, currMethod);
            case ARRAYDEFINITION, ARRAY_DECLARATION -> new Type(INT_TYPE_NAME, true);
            case INTEGER, ARRAY_SUBSCRIPT -> new Type(INT_TYPE_NAME, false);
            case IFEXPR, ELSEEXPR -> new Type(BOOLEAN_TYPE_NAME, false);
            case BINARY_EXPR -> getBinExprType(expr);
            case BINARY_OP -> getBinExprType(expr);
            case FUNCTION_CALL -> getFunctionType(expr, table);
            case NEW_CLASS -> new Type(expr.get("classname"), false);
            case OBJECT -> new Type(table.getClassName(), false);
            case TRUE, FALSE -> new Type(BOOLEAN_TYPE_NAME, false);
            case BRACKETS -> getExprType(expr.getChild(0), table, currMethod);
            case LENGTH -> new Type(INT_TYPE_NAME, false);
            case ASSIGNMENT -> getVarExprTypeForAssigment(expr, table, currMethod);
            case PARENTESIS -> getExprType(expr.getChild(0), table, currMethod);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getFunctionType(JmmNode expr, SymbolTable table) {

        String methodName = expr.get("value");
        JmmNode exprChild = expr.getChild(0);

        /*var childName = exprChild.get("value");
        for (var imp : table.getImports()) {
            if (imp.equals(childName)) {
                return new Type(childName, false);
            }
        }*/

        for (var method : table.getMethods()) {
            if (method.equals(methodName)) {
                //return table.getReturnType(methodName);
            }
        }
        //return getExprType(exprChild, table, methodName);
        return new Type(BOOLEAN_TYPE_NAME, false);

    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        // TODO: Simple implementation that needs to be expanded

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*", "-", "/", "*=", "+=", "-=" -> new Type(INT_TYPE_NAME, false);
            case "==", "&&", "||", "<=", ">=", "<", ">", "!=", "/=" -> new Type("boolean", false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }


    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table, String currMethod) {
        String varName = varRefExpr.getOptional("value").orElse("");
        if (varName.equals("")) {
            varName = varRefExpr.getOptional("name").orElse("");
        }
        var locals = table.getLocalVariables(currMethod);
        for (var local : locals) {
            if (local.getName().equals(varName)) {
                return local.getType();
            }
        }
        var params = table.getParameters(currMethod);
        for (var param : params) {
            if (param.getName().equals(varName)) {
                return param.getType();
            }
        }
        var fields = table.getFields();
        for (var field : fields) {
            if (field.getName().equals(varName)) {
                return field.getType();
            }
        }
        var imports = table.getImports();
        for (var imp : imports) {
            if (imp.equals(varName)) {
                return new Type(imp, false);
            }
        }
        if (varName.equals("true") || varName.equals("false")) {
            return new Type("boolean", false);
        }
        if (table.getImports().contains(varName)) {
            return new Type(varName, false);
        }

        return new Type(INT_TYPE_NAME, false);
    }

    private static Type getVarExprTypeForAssigment(JmmNode varRefExpr, SymbolTable table, String currMethod) {
        String varName = varRefExpr.get("var");
        var locals = table.getLocalVariables(currMethod);
        for (var local : locals) {
            if (local.getName().equals(varName)) {
                return local.getType();
            }
        }
        var fields = table.getFields();
        for (var field : fields) {
            if (field.getName().equals(varName)) {
                return field.getType();
            }
        }
        if (varName.equals("true") || varName.equals("false")) {
            return new Type("boolean", false);
        }
        if (table.getImports().contains(varName)) {
            return new Type(varName, false);
        }

        return new Type(INT_TYPE_NAME, false);
    }

    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded
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
