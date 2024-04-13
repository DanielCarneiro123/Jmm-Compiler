package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.*;
import java.util.stream.Collectors;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;



    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(IMPORT_DECLARATION, this::visitImportDeclaration);
        addVisit(CLASS_DECLARATION, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(VAR_DECL, this::visitVarDecl);
        addVisit(EXPR_STMT, this::visitExpressionStmt);




        addVisit(ASSIGNMENT, this::visitAssignStmt);

        setDefaultVisit(this::defaultVisit);
    }


    private String visitExpressionStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        var child = node.getChild(0);

        code.append(exprVisitor.visit(child).getCode());
        code.append(END_STMT);

        return code.toString();
    }



    private String visitBinaryOp(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        code.append("bananaOp");
        return code.toString();
    }


    private String visitAssignStmt(JmmNode node, Void unused) {
        var lhs = node.get("var");
        var rhsNode = node.getJmmChild(0);


        StringBuilder code = new StringBuilder();

        List<Symbol> localVariables = table.getLocalVariables(node.getParent().get("name"));

        Type thisType = null;

        for (int i = 0; i < localVariables.size(); i++) {
            Symbol localVariable = localVariables.get(i);
            if (localVariable.getName().equals(lhs)) {
                thisType = localVariable.getType();
                break;
            }
        }

        // Compute the OLLIR code for the right-hand side (rhs)
        String rhsCode;

        rhsCode = exprVisitor.visit(rhsNode).getCode();




        String typeString = OptUtils.toOllirType(thisType);




        code.append(lhs);
        code.append(typeString);
        code.append(SPACE);
        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);
        code.append(rhsCode);
        code.append(END_STMT);

        return code.toString();
    }




    private String visitVarDecl(JmmNode node, Void unused) {
        String typeCode = OptUtils.toOllirType(node.getJmmChild(0)); // Get the type code (e.g., i32, bool)
        String varName = node.get("name"); // Get the variable name


        // Construct the field declaration code
        String code = ".field public " + varName + typeCode + ";" + NL;

        return code;
    }
    private String visitReturn(JmmNode node, Void unused) {

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();


        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);
        JmmNode firstChild = node.getChildren().get(0);



        code.append(exprVisitor.visit(firstChild).getCode());
        code.append(OptUtils.toOllirType(retType));

        code.append(END_STMT);

        return code.toString();
    }

    private String visitImportDeclaration(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        String importNames = node.get("value");

        // Remove the brackets '[' and ']' from the import names
        importNames = importNames.substring(1, importNames.length() - 1);


        String[] importNameArray = importNames.split(",");


        String joinedImportNames = Arrays.stream(importNameArray)
                .map(String::trim)
                .collect(Collectors.joining("."));


        code.append("import ").append(joinedImportNames).append(";").append(NL);

        return code.toString();
    }






    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        String code = id + typeCode;

        return code;
    }


    private String visitMethodDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder(".method ");

        // Check if the method is public
        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");
        if (isPublic) {
            code.append("public ");
        }
        boolean isStatic = NodeUtils.getBooleanAttribute(node, "isStatic", "false");
        if (isStatic) {
            code.append("static ");
        }

        // Get the method name
        String methodName = node.get("name");
        code.append(methodName);

        // Append the parameter list
        if (methodName.equals("main")) {
            String argName = node.get("argName");
            code.append("(").append(argName).append(".array.String)");
        } else {
            var paramSymbols = table.getParameters(methodName);
            code.append("(");
            if (!paramSymbols.isEmpty()) {
                for (int i = 0; i < paramSymbols.size(); i++) {
                    var paramSymbol = paramSymbols.get(i);
                    String paramType = OptUtils.toOllirType(paramSymbol.getType());
                    String paramName = paramSymbol.getName();
                    code.append(paramName).append(paramType);
                    if (i < paramSymbols.size() - 1) {
                        code.append(", ");
                    }
                }
            }
            code.append(")");
        }

        // Append the return type
        Type retType = table.getReturnType(methodName);
        String retTypeCode = OptUtils.toOllirType(retType);
        code.append(retTypeCode);

        // Append the method body
        code.append(L_BRACKET);

        // Check if the method has any children (i.e., method body)
        if (node.getNumChildren() > 1) {
            for (int i = 1; i < node.getNumChildren(); i++) {
                var child = node.getJmmChild(i);
                if (child.getKind().equals("VarDecl")) {
                    continue;

                }
                var childCode = visit(child);
                code.append(childCode);
            }
        }

        // Add return statement if the method is void and there's no explicit return statement
        if (retType != null && retType.getName().equals("void") ) {
            code.append("ret.V;").append(NL);
        }

        code.append(R_BRACKET).append(NL);

        return code.toString();
    }





    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());


        String superClass = table.getSuper();
        if (!superClass.isEmpty()) {
            code.append(" extends ");
            code.append(superClass);
        }
        else {
            code.append(" extends Object");
        }

        code.append(L_BRACKET);


        code.append(NL);
        var needNl = true;

        for (var child : node.getChildren()) {
            var result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }


    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
