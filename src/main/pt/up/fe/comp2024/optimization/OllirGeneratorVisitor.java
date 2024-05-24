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
        addVisit(IF_STMT, this::visitIfStatement);
        addVisit(WHILE_STMT, this::visitWhileStatement);
        addVisit(ARRAY_ASSIGN, this::visitArrayAssignStatement);


        addVisit(ASSIGNMENT, this::visitAssignStmt);

        setDefaultVisit(this::defaultVisit);
    }


    private String visitArrayAssignStatement(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        var indexNode = exprVisitor.visit(node.getChild(0));
        var valueNode = exprVisitor.visit(node.getChild(1));

        var indexType = indexNode.getCode().substring(indexNode.getCode().lastIndexOf("."));
        code.append(indexNode.getComputation());
        code.append(valueNode.getComputation());
        code.append(node.get("var")).append("[").append(indexNode.getCode()).append("]").append(indexType);
        code.append(ASSIGN).append(".i32 ").append(valueNode.getCode()).append(";").append(NL);

        return code.toString();
    }


    private int thenCounter = 0;
    private int currentElseCounter1 = -1;
    private int currentElseCounter2 = -1;

    private String getNextLabelThen() {
        currentElseCounter1++;
        currentElseCounter2++;
        return "if_then_" + thenCounter++;
    }

    private String getCurrentLabelThen(){
        var x = thenCounter-1;
        return "if_then_" + x +":";
    }

    private String getNextLabelEnd() {
        var x = currentElseCounter1--;
        return "if_end_" + x;

    }

    private String getCurrentLabelEnd(){
        var x = currentElseCounter2--;
        return "if_end_" + x +":";

    }


    private String visitIfStatement(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        var ifChild = node.getChild(0);
        var elseChild = node.getChild(1);
        var visitChild = exprVisitor.visit(ifChild.getChild(0));
        code.append(visitChild.getComputation());
        code.append("if(").append(visitChild.getCode()).append(") goto ");
        code.append(getNextLabelThen()).append(";").append(NL);
        //code.append("handle else").append(NL);
        var elseContent = elseChild.getChild(0);
        for (var child : elseContent.getChildren()){
            var aux = visit(child);

            code.append(aux);

        }

        code.append("goto ").append(getNextLabelEnd()).append(";").append(NL);
        code.append(getCurrentLabelThen()).append(NL);

        var ifContent = ifChild.getChild(1);
        for (var child : ifContent.getChildren()){
            var aux = visit(child);

            code.append(aux);

        }

        code.append(getCurrentLabelEnd()).append(NL);


        return code.toString();
    }

    private int whileCondCounter = 0;
    private int whileLoopCounter = 0;

    private int whileEndCounter = 0;

    private String getNextLabelWhileCond() {
        return "whileCond" + whileCondCounter++;
    }

    private String getCurrentLabelWhileCond(){
        var x = whileCondCounter-1;
        return "whileCond" + x + ";";
    }

    private String getNextLabelWhileLoop() {
        return "whileLoop" + whileLoopCounter++ + ";";
    }

    private String getCurrentLabelWhileLoop(){
        var x = whileLoopCounter-1;
        return "whileLoop" + x +":";
    }

    private String getNextLabelWhileEnd() {
        return "whileEnd" + whileEndCounter++ + ";";
    }

    private String getCurrentLabelWhileEnd(){
        var x = whileEndCounter-1;
        return "whileEnd" + x +":";
    }

    private String visitWhileStatement(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        var condChild = node.getChild(0);
        var visitChild = exprVisitor.visit(condChild);

        code.append(getNextLabelWhileCond()).append(":").append(NL);
        code.append(visitChild.getComputation());
        code.append("if (").append(visitChild.getCode()).append(") ").append("goto ").append(getNextLabelWhileLoop()).append(NL);
        code.append("goto ").append(getNextLabelWhileEnd()).append(NL);
        code.append(getCurrentLabelWhileLoop()).append(NL);
        var insideWhile = node.getChild(1);
        for (var child : insideWhile.getChildren()){
            var aux = visit(child);

            code.append(aux);

        }

        code.append("goto ").append(getCurrentLabelWhileCond()).append(NL);
        code.append(getCurrentLabelWhileEnd()).append(NL);

        return code.toString();
    }



    private String visitExpressionStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        var child = node.getChild(0);
        var visitChild = exprVisitor.visit(child);
        code.append(visitChild.getComputation());
        code.append(visitChild.getCode());
        code.append(END_STMT);

        return code.toString();
    }


/*
    private String visitBinaryOp(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        return code.toString();
    }
*/

    private String visitAssignStmt(JmmNode node, Void unused) {
        var lhs = node.get("var");

        boolean isField = false;
        var x = node.getJmmChild(0);
        var rhsNode = exprVisitor.visit(x);

        StringBuilder code = new StringBuilder();
        code.append(rhsNode.getComputation());

        List<Symbol> localVariables = table.getLocalVariables(node.getAncestor("MethodDecl").get().get("name"));
        List<Symbol> paramVariables = table.getParameters(node.getAncestor("MethodDecl").get().get("name"));
        Type thisType = null;

        // Search for the variable in local variables
        for (Symbol localVariable : localVariables) {
            if (localVariable.getName().equals(lhs)) {
                thisType = localVariable.getType();
                break;
            }
        }

        for (Symbol paramVariable : paramVariables) {
            if (paramVariable.getName().equals(lhs)) {
                thisType = paramVariable.getType();
                break;
            }
        }

        List<Symbol> fields = table.getFields();
        String rhs ="";
        if(node.getChildren().get(0).getKind().equals("Identifier")) {
            rhs = node.getChildren().get(0).get("value");
            for (Symbol field : fields) {
                if (field.getName().equals(rhs)){
                    isField = true;
                }}
        }


        // If variable not found in locals, search in fields
        if (thisType == null) {

            for (Symbol field : fields) {

                if (field.getName().equals(lhs)) {
                    thisType = field.getType();
                    // Generate OLLIR instructions for accessing/modifying field
                    String typeString = OptUtils.toOllirType(thisType);
                    code.append("putfield(this, ").append(lhs).append(typeString).append(", ").append(rhsNode.getCode()).append(").V;\n");

                    return code.toString();
                }
            }
            // If variable is neither local variable nor field, throw an exception or handle it accordingly
            throw new RuntimeException("Variable not found: " + lhs);
        }

        // If the variable is a local variable
        String typeString = OptUtils.toOllirType(thisType);




        code.append(lhs);

        code.append(typeString);
        code.append(SPACE);
        code.append(ASSIGN);

        code.append(typeString);
        code.append(SPACE);
        code.append(rhsNode.getCode());

        code.append(END_STMT);

        return code.toString();
    }





    private String visitVarDecl(JmmNode node, Void unused) {
        String typeCode = OptUtils.toOllirType(node.getJmmChild(0)); // Get the type code (e.g., i32, bool)
        String varName = node.get("name"); // Get the variable name


        var isArray = node.getChild(0).getObject("isArray");

        boolean isArrayBool = isArray.toString().equals("true");


        String code = ".field public " + varName;

        if(isArrayBool){
            code = code + ".array";
        }
        code = code + typeCode + ";" + NL;

        return code;
    }
    private String visitReturn(JmmNode node, Void unused) {

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        //code.append(exprVisitor.visit(firstChild).getCode());

        var lhs = exprVisitor.visit(node.getJmmChild(0));






        code.append(lhs.getComputation());
        code.append("ret");

        code.append(OptUtils.toOllirType(retType));
        code.append(" ");
        code.append(lhs.getCode()).append(END_STMT);

        return code.toString();
    }

    private String visitImportDeclaration(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        String importNames = node.get("value");


        importNames = importNames.replaceAll("^\\[|\\]$", "");


        String[] importNameArray = importNames.split(",");
        for (int i = 0; i < importNameArray.length; i++) {
            importNameArray[i] = importNameArray[i].trim();
        }


        String joinedImportNames = String.join(".", importNameArray);

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
        var allParams= table.getParameters(node.get("name"));
        int varArgParamNum = allParams.size()-1;

        boolean isVarArgLastParam = false;
        if(allParams.size() != 0){
            isVarArgLastParam = (!allParams.get(allParams.size()-1).getType().getAttributes().isEmpty());
        }
        if(isVarArgLastParam){
        code.append("varargs ");}



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
                "invokespecial(this, \"<init>\").V;\n" +
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
