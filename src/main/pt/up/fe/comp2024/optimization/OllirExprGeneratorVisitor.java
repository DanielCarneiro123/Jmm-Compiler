package pt.up.fe.comp2024.optimization;

import com.sun.jdi.BooleanValue;
import org.specs.comp.ollir.Ollir;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {

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
        addVisit(NEW_CLASS, this::visitNewClass);
        addVisit(PARENTESIS, this::visitParentesis);
        addVisit(TRUE, this::visitBoolLiteral);
        addVisit(FALSE, this::visitBoolLiteral);
        //addVisit(VAR_ARG, this::visitVarArg);
        addVisit(NEGATION, this::visitNegationExpr);
        addVisit(OBJECT, this::visitThisExpr);
        addVisit(ARRAYDEFINITION, this::visitArrayDef);
        addVisit(ARRAY_DECLARATION, this::visitArrayDecl);
        addVisit(ARRAY_SUBSCRIPT, this::visitArraySubscript);
        addVisit(LENGTH, this::visitLength);


        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitVarArg(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        code.append("SIGALE SIGALE");


        return new OllirExprResult(code.toString(),computation.toString());
    }

    private int arrayCounter = 0;

    private String getNextLabelArray() {
        return "__varargs_array_" + arrayCounter++;
    }

    private String getCurrentLabelArray(){
        var x = arrayCounter-1;
        return "__varargs_array_" + x;
    }
    private OllirExprResult visitArrayDef(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        //code.append(getNextLabelArray()).append(".array.i32");

        String tempInit = OptUtils.getTemp() + ".array.i32";
        code.append(tempInit);
        computation.append(tempInit).append(ASSIGN).append(".array.i32 new(array, ").append(node.getChildren().size()).append(".i32)").append(".array.i32").append(END_STMT);
        //computation.append(getCurrentLabelArray()).append(".array.i32").append(ASSIGN).append(".array.i32 ").append(tempInit).append(END_STMT);
        for(int i = 0; i < node.getChildren().size(); i++){
            computation.append(tempInit).append(".array.i32").append("[").append(i).append(".i32].i32").append(ASSIGN).append(".i32 ").append(node.getChild(i).get("value")).append(".i32").append(END_STMT);
        }


        return new OllirExprResult(code.toString(),computation.toString());
    }


    private OllirExprResult visitLength(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        if(node.getParent().getKind().equals("Assignment")){
            code.append("arraylength(").append(node.getChild(0).get("value")).append(".array.i32)");
            return new OllirExprResult(code.toString());
        }
        var temp = OptUtils.getTemp() + ".i32";
        computation.append(temp).append(ASSIGN).append(".i32").append(" arraylength(").append(node.getChild(0).get("value")).append(".array.i32)").append(".i32;\n");
        code.append(temp);


        return new OllirExprResult(code.toString(),computation.toString());
    }

    private OllirExprResult visitArraySubscript(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        var x = node.getAncestor("MethodDecl").get().get("name");






        var indexNode = visit(node.getJmmChild(1));
        computation.append(indexNode.getComputation());

        if(!node.getParent().getKind().equals("Assignment")){

            var temp = OptUtils.getTemp() + ".i32";
            computation.append(temp).append(" ").append(ASSIGN).append(".i32 ").append(node.getChild(0).get("value"));

            computation.append("[").append(indexNode.getCode()).append("].i32").append(END_STMT);

            code.append(temp);

        }

        else {
            code.append(node.getChild(0).get("value")).append("[").append(indexNode.getCode()).append("].i32");
        }
        return new OllirExprResult(code.toString(),computation.toString());
    }

    private OllirExprResult visitArrayDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        boolean isField = false;
        List<Symbol> fields = table.getFields();
        if(node.getParent().getKind().equals("Assignment")){
            var aux = node.getParent().get("var");
            for (Symbol field : fields) {
                if (field.getName().equals(aux)){
                    isField = true;
                }}
        }

        if(isField){
            var auxTemp = OptUtils.getTemp() + ".array.i32";

            var sizeNode = visit(node.getJmmChild(1));
            computation.append(sizeNode.getComputation());

            String ollirType = OptUtils.toOllirType(node.getChild(0));
            computation.append(auxTemp).append(ASSIGN).append(".array.i32 ");
            computation.append("new(array,").append(sizeNode.getCode()).append(").array").append(ollirType).append(END_STMT);

            code.append(auxTemp);
            return new OllirExprResult(code.toString(),computation.toString());
        }

        var sizeNode = visit(node.getJmmChild(1));
        computation.append(sizeNode.getComputation());

        String ollirType = OptUtils.toOllirType(node.getChild(0));
        code.append("new(array,").append(sizeNode.getCode()).append(").array").append(ollirType);



        return new OllirExprResult(code.toString(),computation.toString());
    }

    private OllirExprResult visitNegationExpr(JmmNode node, Void unused) {
        // Visit the child expression of the negation
        var b = node.getJmmChild(0);
        var exprNode = visit(node.getJmmChild(0));

        // Generate the OLLIR code for the negation operation
        StringBuilder code = new StringBuilder();
        code.append(exprNode.getComputation());

        // Append the OLLIR code for the negation operation
        String negatedVar = OptUtils.getTemp() + ".bool";
        code.append(negatedVar).append(" :=.bool !.bool ").append(exprNode.getCode()).append(END_STMT);

        return new OllirExprResult(negatedVar, code);
    }

    private OllirExprResult visitThisExpr(JmmNode node, Void unused) {
        // Visit the expression inside the parentheses
        var code = new StringBuilder();
        code.append("this.").append(table.getClassName());

        // Return the computation and code of the inner expression
        return new OllirExprResult(code.toString());
    }

    private OllirExprResult visitBoolLiteral(JmmNode node, Void unused) {
        String value = node.get("value");
        String code = value.equals("true") ? "1.bool" : "0.bool";
        return new OllirExprResult(code);
    }

    private OllirExprResult visitParentesis(JmmNode node, Void unused) {
        // Visit the expression inside the parentheses
        var innerExpr = visit(node.getJmmChild(0));

        // Return the computation and code of the inner expression
        return innerExpr;
    }

    private OllirExprResult visitNewClass(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        // Get the class name
        String className = node.get("classname");

        if(node.getParent().getKind().equals("Assignment")){
            code.append("new(").append(className).append(")").append(".").append(className).append(";").append("\n");

            code.append("invokespecial").append("(").append(node.getParent().get("var")).append(".").append(className).append(", \"\").V");

            return new OllirExprResult(code.toString());
        }

        // Generate code for creating a new instance of the class
        String instanceVar = OptUtils.getTemp();
        code.append(instanceVar).append(".").append(className).append(SPACE)
                .append(" :=").append(".").append(className).append(SPACE).append("new(").append(className).append(")").append(".").append(className).append(END_STMT);

        // Generate code for calling the constructor
        code.append("invokespecial").append("(").append(instanceVar).append(".").append(className).append(", \"\").V").append(END_STMT);

        // Append the instance variable to the result code
        String result = instanceVar + "." + className;
        return new OllirExprResult(result, code);
    }

    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private int thenCounter = 0;
    private int currentElseCounter1 = -1;
    private int currentElseCounter2 = -1;
    private String getNextLabelTrue() {
        currentElseCounter1++;
        currentElseCounter2++;
        return "true_" + thenCounter++;
    }

    private String getCurrentLabelTrue(){
        var x = thenCounter-1;
        return "true_" + x +":";
    }

    private String getNextLabelEnd() {
        var x = currentElseCounter1--;
        return "end_" + x;

    }

    private String getCurrentLabelEnd(){
        var x = currentElseCounter2--;
        return "end_" + x +":";

    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();
        String resOllirType = node.get("op").equals("+") || node.get("op").equals("-") || node.get("op").equals("*") || node.get("op").equals("/") ? ".i32" : ".bool";

        if(node.get("op").equals("&&")){
            StringBuilder code = new StringBuilder();


            code.append(lhs.getComputation());
            String tempStore = OptUtils.getTemp() + ".bool";
            String tempAux = OptUtils.getTemp() + ".bool";
            code.append("if (").append(lhs.getCode()).append(") goto ").append(getNextLabelTrue()).append(END_STMT);
            code.append(tempStore).append(ASSIGN).append(".bool 0.bool;\n");
            code.append("goto ").append(getNextLabelEnd()).append(END_STMT);
            code.append(getCurrentLabelTrue()).append("\n");
            code.append(rhs.getComputation());
            code.append(tempAux).append(ASSIGN).append(".bool ").append(rhs.getCode()).append(END_STMT);
            code.append(tempStore).append(ASSIGN).append(".bool ").append(tempAux).append(END_STMT);
            code.append(getCurrentLabelEnd()).append("\n");
            computation.append(tempStore);

            return new OllirExprResult(computation.toString(),code);
        }

        if(rhs.getComputation().isEmpty() && lhs.getComputation().isEmpty() && node.getParent().getKind().equals("Assignment")){
            StringBuilder code = new StringBuilder();
            code.append(lhs.getCode()).append(" ").append(node.get("op")).append(resOllirType).append(" ").append(rhs.getCode());
            return new OllirExprResult(code.toString(), computation);
        }

        // Generate code for the computation of the result
        String code = OptUtils.getTemp() + resOllirType;
        computation.append(lhs.getComputation());


        computation.append(rhs.getComputation());

        computation.append(code).append(" :=").append(resOllirType).append(" ").append(lhs.getCode()).append(" ").append(node.get("op")).append(resOllirType).append(" ").append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitFunctionCall(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();
        String code1 = "";
        var funcLhs =  visit(node.getJmmChild(0));
        Type argTypeImport = new Type("aux",false);
        boolean foundMatchImports = false;
        boolean parentIsAssign = false;
        boolean parentIsBinOp = false;


        if(node.getParent().getKind().equals("Assignment")){
            parentIsAssign = true;
        }

        if(node.getParent().getKind().equals("BinaryOp")){
            parentIsBinOp = true;
        }

        String methodSignature = node.get("value");


        var child = node;

        JmmNode methodDeclNode = node;
        while (methodDeclNode != null && !methodDeclNode.getKind().equals("MethodDecl")) {
            methodDeclNode = methodDeclNode.getParent();
        }
        if (methodDeclNode != null) {
            String methodName = methodDeclNode.get("name");


            // Get the local variables for the method
            List<Symbol> localVariables = table.getLocalVariables(methodName);

            List<String> imports = table.getImports();

// Get the name of the first child

            var leftChild = node.getJmmChild(0);
            while (leftChild.getKind().equals("Parentesis")) {
                leftChild = leftChild.getJmmChild(0);
            }

            String firstChildName = "";

            if (leftChild.getKind().equals("Identifier")) {
                firstChildName = leftChild.get("value");

                Type type = TypeUtils.getExprType(leftChild, table, methodName);


                String returnS = "";
                // instance from the current class
                if (type.getName().equals(table.getClassName())) {
                    Type returnType = table.getReturnType(methodSignature);
                    // returnS = OptUtils.toOllirType(returnType);
                }

                else {

                    // io.println
                    if (table.getImports().contains(firstChildName)) {
                        code.append("invokestatic(")
                                .append(firstChildName)
                                .append(", ")
                                .append("\"").append(methodSignature).append("\"");

                        // arguments
                        List<JmmNode> arguments = child.getChildren().subList(1, child.getNumChildren());

                        var allParams= table.getParameters(methodSignature);
                        int varArgParamNum = allParams.size()-1;
                        boolean varArgParamCurr = false;
                        boolean isVarArgLastParam = false;
                        if(allParams.size() != 0){
                            isVarArgLastParam = (!allParams.get(allParams.size()-1).getType().getAttributes().isEmpty());
                        }
                        var tempVarArgAux = "";
                        OllirExprResult x1 = null;
                        for (int i = 0; i < arguments.size(); i++) {
                            if(isVarArgLastParam && i== varArgParamNum){

                                if(node.getChild(i+1).getKind().equals("Arraydefinition")){

                                    x1 = visit(node.getChild(i+1));
                                    computation.append(x1.getComputation());
                                }
                                else{
                                tempVarArgAux = OptUtils.getTemp() + ".array.i32";
                                computation.append(tempVarArgAux).append(ASSIGN).append(" .array.i32").append(" new(array, ").append(arguments.size()-varArgParamNum).append(".i32).array.i32").append(END_STMT);
                                computation.append(getNextLabelArray()).append(".array.i32").append(ASSIGN).append(".array.i32 ").append(tempVarArgAux).append(END_STMT);
                                for(int j = 0; j < arguments.size()-varArgParamNum; j++){
                                    computation.append(getCurrentLabelArray()).append(".array.i32").append("[").append(j).append(".i32].i32").append(ASSIGN).append(".i32 ").append(node.getChild(1 + j+varArgParamNum).get("value")).append(".i32").append(END_STMT);
                                }}
                                break;
                            }
                            JmmNode argument = arguments.get(i);

                            // Visit the argument to get its value
                            OllirExprResult argumentResult = visit(argument);

                            // Extract the code representing the argument value
                            String argumentCode = argumentResult.getComputation();
                            computation.append(argumentCode);

                            String argCode = argumentResult.getCode();

                            if (argument.getKind().equals("FunctionCall")) {   // tmp0.i32 =.i32 invokevirtual(tmp0.Simple, "add", 1.i32).i32
                                String tmp = OptUtils.getTemp();
                                String lastType = "";
                                if (table.getMethods().contains(methodSignature)) {
                                    Type argType = table.getParameters(methodSignature).get(i).getType();
                                    lastType = OptUtils.toOllirType(argType);
                                }
                                else {
                                    lastType = argCode.substring(argCode.indexOf("."));
                                }
                                computation.append(tmp).append(lastType)
                                        .append(" :=").append(lastType).append(" ")
                                        .append(argCode).append(END_STMT);
                                argCode = tmp + lastType;

                            }

                            code.append(", ").append(argCode);

                        }
                        if(isVarArgLastParam){
                            if(x1 != null){
                                code.append(", ").append(x1.getCode());
                            }
                            else{
                            code.append(", ").append(getCurrentLabelArray()).append(".array.i32");}
                        }
                        if(node.getParent().getKind().equals("Assignment")){
                            code.append(")");
                            String variableName = node.getParent().get("var");
                            Optional<Symbol> matchingVariable = localVariables.stream()
                                    .filter(variable -> variable.getName().equals(variableName))
                                    .findFirst();

                            if (!matchingVariable.isPresent()) {
                                List<Symbol> fields = table.getFields();
                                matchingVariable = fields.stream()
                                        .filter(variable -> variable.getName().equals(variableName))
                                        .findFirst();
                            }

                            if (matchingVariable.isPresent()) {
                                Type parentType = matchingVariable.get().getType();
                                code.append(OptUtils.toOllirType(parentType));
                            }

                        }

                        else if(node.getParent().getKind().equals("BinaryOp")){
                            var aux = OptUtils.getTemp();
                            String resOllirType = node.getParent().get("op").equals("+") || node.getParent().get("op").equals("-") || node.getParent().get("op").equals("*") || node.getParent().get("op").equals("/") ? ".i32" : ".bool";
                            computation.append(aux).append(resOllirType).append(ASSIGN).append(" ").append(resOllirType).append(" ").append(code).append(")").append(resOllirType).append(END_STMT);
                            code = new StringBuilder(aux);
                            code.append(resOllirType);

                        }
                        else if (table.getMethods().contains(child.get("value"))) {
                            Type returnType = table.getReturnType(methodSignature);
                            String t = OptUtils.toOllirType(returnType);
                            code.append(")").append(t);
                        }

                        else{
                            code.append(").V");
                        }

                        return new OllirExprResult(code.toString(), computation.toString());
                    }
                    else {
                        code.append("invokevirtual(")
                                .append(firstChildName).append(".").append(type.getName())
                                .append(", ")
                                .append("\"").append(methodSignature).append("\"");

                        // arguments
                        List<JmmNode> arguments = child.getChildren().subList(1, child.getNumChildren());

                        var allParams= table.getParameters(methodSignature);
                        int varArgParamNum = allParams.size()-1;
                        boolean varArgParamCurr = false;
                        boolean isVarArgLastParam = false;
                        if(allParams.size() != 0){
                            isVarArgLastParam = (!allParams.get(allParams.size()-1).getType().getAttributes().isEmpty());
                        }
                        var tempVarArgAux = "";



                        OllirExprResult x1 = null;

                        for (int i = 0; i < arguments.size(); i++) {

                            if(isVarArgLastParam && i== varArgParamNum){

                                if(node.getChild(i+1).getKind().equals("Arraydefinition")){

                                    x1 = visit(node.getChild(i+1));
                                    computation.append(x1.getComputation());
                                }
                                else{
                                tempVarArgAux = OptUtils.getTemp() + ".array.i32";
                                computation.append(tempVarArgAux).append(ASSIGN).append(" .array.i32").append(" new(array, ").append(arguments.size()-varArgParamNum).append(".i32).array.i32").append(END_STMT);
                                computation.append(getNextLabelArray()).append(".array.i32").append(ASSIGN).append(".array.i32 ").append(tempVarArgAux).append(END_STMT);
                                for(int j = 0; j < arguments.size()-varArgParamNum; j++){
                                    computation.append(getCurrentLabelArray()).append(".array.i32").append("[").append(j).append(".i32].i32").append(ASSIGN).append(".i32 ").append(node.getChild(1 + j+varArgParamNum).get("value")).append(".i32").append(END_STMT);
                                }}

                                break;
                            }

                            JmmNode argument = arguments.get(i);

                            // Visit the argument to get its value
                            OllirExprResult argumentResult = visit(argument);

                            // Extract the code representing the argument value
                            String argumentCode = argumentResult.getComputation();
                            computation.append(argumentCode);

                            String argCode = argumentResult.getCode();

                            if (argument.getKind().equals("FunctionCall")) {   // tmp0.i32 =.i32 invokevirtual(tmp0.Simple, "add", 1.i32).i32
                                String tmp = OptUtils.getTemp();
                                String lastType = argCode.substring(argCode.indexOf("."));
                                computation.append(tmp).append(lastType)
                                        .append(" :=").append(lastType).append(" ")
                                        .append(argCode).append(END_STMT);
                                argCode = tmp + lastType;

                            }

                            code.append(", ").append(argCode);

                        }
                        if(isVarArgLastParam){
                            if(x1 != null){
                                code.append(", ").append(x1.getCode());
                            }{
                            code.append(", ").append(getCurrentLabelArray()).append(".array.i32");}
                        }

                        if(node.getParent().getKind().equals("Assignment")){
                            code.append(")");
                            String variableName = node.getParent().get("var");
                            Optional<Symbol> matchingVariable = localVariables.stream()
                                    .filter(variable -> variable.getName().equals(variableName))
                                    .findFirst();

                            if (!matchingVariable.isPresent()) {
                                List<Symbol> fields = table.getFields();
                                matchingVariable = fields.stream()
                                        .filter(variable -> variable.getName().equals(variableName))
                                        .findFirst();
                            }

                            if (matchingVariable.isPresent()) {
                                Type parentType = matchingVariable.get().getType();
                                code.append(OptUtils.toOllirType(parentType));
                            }

                        }

                        else if(node.getParent().getKind().equals("BinaryOp")){
                            var aux = OptUtils.getTemp();
                            String resOllirType = node.getParent().get("op").equals("+") || node.getParent().get("op").equals("-") || node.getParent().get("op").equals("*") || node.getParent().get("op").equals("/") ? ".i32" : ".bool";
                            computation.append(aux).append(resOllirType).append(ASSIGN).append(" ").append(resOllirType).append(" ").append(code).append(")").append(resOllirType).append(END_STMT);
                            code = new StringBuilder(aux);
                            code.append(resOllirType);

                        }
                        else if (table.getMethods().contains(child.get("value"))){
                            Type returnType = table.getReturnType(methodSignature);
                            String t = OptUtils.toOllirType(returnType);
                            code.append(")").append(t);
                        }


                        else{
                            code.append(").V");
                        }

                        return new OllirExprResult(code.toString(), computation.toString());
                    }

                    // io c; c.println
                }

            }

            else if (leftChild.getKind().equals("NewClass")) {
                firstChildName = leftChild.get("classname");

                // (new Simple()).add(1)

                // computation.append(new.Computation)
                computation.append(funcLhs.getComputation());

                // invokevirtual(new.Code, "add", 1.i32).i32;
                code.append("invokevirtual(")
                        .append(funcLhs.getCode())
                        .append(", \"")
                        .append(methodSignature)
                        .append("\"");

                // arguments
                List<JmmNode> arguments = child.getChildren().subList(1, child.getNumChildren());

                var allParams= table.getParameters(methodSignature);
                int varArgParamNum = allParams.size()-1;
                boolean varArgParamCurr = false;
                boolean isVarArgLastParam = false;
                if(allParams.size() != 0){
                    isVarArgLastParam = (!allParams.get(allParams.size()-1).getType().getAttributes().isEmpty());
                }
                var tempVarArgAux = "";
                OllirExprResult x1 = null;

                for (int i = 0; i < arguments.size(); i++) {
                    if(isVarArgLastParam && i== varArgParamNum){

                        if(node.getChild(i+1).getKind().equals("Arraydefinition")){

                            x1 = visit(node.getChild(i+1));
                            computation.append(x1.getComputation());
                        }
                        tempVarArgAux = OptUtils.getTemp() + ".array.i32";
                        computation.append(tempVarArgAux).append(ASSIGN).append(" .array.i32").append(" new(array, ").append(arguments.size()-varArgParamNum).append(".i32).array.i32").append(END_STMT);
                        computation.append(getNextLabelArray()).append(".array.i32").append(ASSIGN).append(".array.i32 ").append(tempVarArgAux).append(END_STMT);
                        for(int j = 0; j < arguments.size()-varArgParamNum; j++){
                            computation.append(getCurrentLabelArray()).append(".array.i32").append("[").append(j).append(".i32].i32").append(ASSIGN).append(".i32 ").append(node.getChild(1 + j+varArgParamNum).get("value")).append(".i32").append(END_STMT);
                        }

                        break;
                    }
                    JmmNode argument = arguments.get(i);

                    // Visit the argument to get its value
                    OllirExprResult argumentResult = visit(argument);

                    // Extract the code representing the argument value
                    String argumentCode = argumentResult.getComputation();
                    computation.append(argumentCode);

                    code.append(", ").append(argumentResult.getCode());

                }
                if(isVarArgLastParam){
                    if(x1 != null){
                        code.append(", ").append(x1.getCode());
                    }
                    else{
                    code.append(", ").append(getCurrentLabelArray()).append(".array.i32");}
                }
                code.append(")");

                // return type
                if(node.getParent().getKind().equals("Assignment")){

                    String variableName = node.getParent().get("var");
                    Optional<Symbol> matchingVariable = localVariables.stream()
                            .filter(variable -> variable.getName().equals(variableName))
                            .findFirst();

                    if (!matchingVariable.isPresent()) {
                        List<Symbol> fields = table.getFields();
                        matchingVariable = fields.stream()
                                .filter(variable -> variable.getName().equals(variableName))
                                .findFirst();
                    }

                    if (matchingVariable.isPresent()) {
                        Type parentType = matchingVariable.get().getType();
                        code.append(OptUtils.toOllirType(parentType));
                    }

                }

                else if(node.getParent().getKind().equals("BinaryOp")){
                    var aux = OptUtils.getTemp();
                    String resOllirType = node.getParent().get("op").equals("+") || node.getParent().get("op").equals("-") || node.getParent().get("op").equals("*") || node.getParent().get("op").equals("/") ? ".i32" : ".bool";
                    computation.append(aux).append(resOllirType).append(ASSIGN).append(" ").append(resOllirType).append(" ").append(code).append(resOllirType).append(END_STMT);
                    code = new StringBuilder(aux);
                    code.append(resOllirType);

                }

                else if (firstChildName.equals(table.getClassName())) {
                    Type returnType = table.getReturnType(methodSignature);
                    String t = OptUtils.toOllirType(returnType);
                    code.append(t);

                }


                else{
                    code.append(").V");
                }

                if(node.getParent().getKind().equals("FunctionCall")){
                    var aux = OptUtils.getTemp();
                    var lastType = code.substring(code.lastIndexOf("."));
                    computation.append(aux).append(lastType).append(ASSIGN).append(" ").append(lastType).append(" ").append(code).append(lastType).append(END_STMT);
                    code = new StringBuilder(aux);
                    code.append(lastType);

                }

                return new OllirExprResult(code.toString(), computation.toString());

            }


            else if (leftChild.getKind().equals("Object")) {

                // computation.append(new.Computation)
                computation.append(funcLhs.getComputation());


                code.append("invokevirtual(")
                        .append(funcLhs.getCode())
                        .append(", \"")
                        .append(methodSignature)
                        .append("\"");

                // arguments
                List<JmmNode> arguments = child.getChildren().subList(1, child.getNumChildren());

                var allParams= table.getParameters(methodSignature);
                int varArgParamNum = allParams.size()-1;
                boolean varArgParamCurr = false;
                OllirExprResult x1 = null;
                boolean isVarArgLastParam = false;

                if(allParams.size() != 0){
                    isVarArgLastParam = (!allParams.get(allParams.size()-1).getType().getAttributes().isEmpty());
                }
                var tempVarArgAux = "";


                for (int i = 0; i < arguments.size(); i++) {
                    if(isVarArgLastParam && i== varArgParamNum){
                        if(node.getChild(i+1).getKind().equals("Arraydefinition")){

                            x1 = visit(node.getChild(i+1));
                            computation.append(x1.getComputation());
                        }
                        else{
                        tempVarArgAux = OptUtils.getTemp() + ".array.i32";
                        computation.append(tempVarArgAux).append(ASSIGN).append(" .array.i32").append(" new(array, ").append(arguments.size()-varArgParamNum).append(".i32).array.i32").append(END_STMT);
                        computation.append(getNextLabelArray()).append(".array.i32").append(ASSIGN).append(".array.i32 ").append(tempVarArgAux).append(END_STMT);



                        for(int j = 0; j < arguments.size()-varArgParamNum; j++){
                            computation.append(getCurrentLabelArray()).append(".array.i32").append("[").append(j).append(".i32].i32").append(ASSIGN).append(".i32 ").append(node.getChild(1 + j+varArgParamNum).get("value")).append(".i32").append(END_STMT);
                        }}

                        break;
                    }
                    JmmNode argument = arguments.get(i);

                    // Visit the argument to get its value
                    OllirExprResult argumentResult = visit(argument);

                    // Extract the code representing the argument value
                    String argumentCode = argumentResult.getComputation();
                    computation.append(argumentCode);

                    String argCode = argumentResult.getCode();

                    if (argument.getKind().equals("FunctionCall")) {   // tmp0.i32 =.i32 invokevirtual(tmp0.Simple, "add", 1.i32).i32
                        String tmp = OptUtils.getTemp();
                        String lastType = "";
                        if (table.getMethods().contains(methodSignature)) {
                            Type argType = table.getParameters(methodSignature).get(i).getType();
                            lastType = OptUtils.toOllirType(argType);
                        }
                        else {
                            lastType = argCode.substring(argCode.lastIndexOf("."));
                        }
                        computation.append(tmp).append(lastType)
                                .append(" :=").append(lastType).append(" ")
                                .append(argCode).append(END_STMT);
                        argCode = tmp + lastType;

                    }

                    code.append(", ").append(argCode);

                }
                if(isVarArgLastParam){
                    if(x1 != null){
                        code.append(", ").append(x1.getCode());
                    }
                    else{
                    code.append(", ").append(getCurrentLabelArray()).append(".array.i32");}
                }
                code.append(")");


                // return type
                if(node.getParent().getKind().equals("Assignment")){

                    String variableName = node.getParent().get("var");
                    Optional<Symbol> matchingVariable = localVariables.stream()
                            .filter(variable -> variable.getName().equals(variableName))
                            .findFirst();

                    if (!matchingVariable.isPresent()) {
                        List<Symbol> fields = table.getFields();
                        matchingVariable = fields.stream()
                                .filter(variable -> variable.getName().equals(variableName))
                                .findFirst();
                    }

                    if (matchingVariable.isPresent()) {
                        Type parentType = matchingVariable.get().getType();
                        code.append(OptUtils.toOllirType(parentType));
                    }

                }

                else if(node.getParent().getKind().equals("BinaryOp")){
                    var aux = OptUtils.getTemp();
                    String resOllirType = node.getParent().get("op").equals("+") || node.getParent().get("op").equals("-") || node.getParent().get("op").equals("*") || node.getParent().get("op").equals("/") ? ".i32" : ".bool";
                    computation.append(aux).append(resOllirType).append(ASSIGN).append(" ").append(resOllirType).append(" ").append(code).append("").append(resOllirType).append(END_STMT);
                    code = new StringBuilder(aux);
                    code.append(resOllirType);

                }


                else{
                    Type returnType = table.getReturnType(methodSignature);
                    String t = OptUtils.toOllirType(returnType);
                    code.append(t);
                }






                return new OllirExprResult(code.toString(), computation.toString());

            }





            String fName = firstChildName;
            Optional<Symbol> matchingVariable1 = localVariables.stream()
                    .filter(variable -> variable.getName().equals(fName))
                    .findFirst();

            if (matchingVariable1.isPresent()) {
                argTypeImport = matchingVariable1.get().getType();
                String argTypeStr = OptUtils.toOllirType(argTypeImport).substring(1); // Remove the first character (the dot)
                foundMatchImports = imports.contains(argTypeStr);
            }



// Check if the type of the first child's name is in the list of imports



// Check if any local variable's name matches the first child's name
            boolean foundMatch1 = localVariables.stream()
                    .map(Symbol::getName)
                    .anyMatch(name -> name.equals(fName));

            if (foundMatchImports) {
                code.append("invokevirtual");
                code.append("(");
                code.append(child.getChildren().get(0).get("value"));
                code.append(OptUtils.toOllirType(argTypeImport));
                /*code1 = OptUtils.getTemp() + OptUtils.toOllirType(argTypeImport);
                computation.append(funcLhs.getComputation());
                computation.append(code1).append(" :=").append(OptUtils.toOllirType(argTypeImport)).append(" ");
                funcLhs = new OllirExprResult(code1); // Include the type here
                */




            } else if (foundMatch1) {
                code.append("invokevirtual");
                code.append("(");
                code.append(child.getChildren().get(0).get("value"));
                code.append(".").append(table.getClassName());
            }
            else {
                // If no match is found, continue with the regular logic for generating the function call code
                code.append(child.getChildren().get(0).get("value").equals("this") ? "invokevirtual" : "invokestatic");
                code.append("(");
                code.append(child.getChildren().get(0).get("value"));
                if(child.getChildren().get(0).get("value").equals("this")){
                    code.append(".").append(table.getClassName());
                }
            }
        }

        code.append(",");
        code.append("\"");
        code.append(child.get("value"));
        code.append("\"");




        JmmNode methodDeclNode1 = node;
        while (!methodDeclNode1.getKind().equals("MethodDecl")) {
            methodDeclNode1 = methodDeclNode1.getParent();
        }


        String methodName = methodDeclNode1.get("name");


        List<Symbol> localVariables = table.getLocalVariables(methodName);


// Get the local variables for the current method
        List<JmmNode> arguments = child.getChildren().subList(1, child.getNumChildren());

        var allParams= table.getParameters(methodSignature);
        int varArgParamNum = allParams.size()-1;
        boolean varArgParamCurr = false;
        boolean isVarArgLastParam = false;
        OllirExprResult x1 = null;
        if(allParams.size() != 0){
            isVarArgLastParam = (!allParams.get(allParams.size()-1).getType().getAttributes().isEmpty());
        }
        var tempVarArgAux = "";



        for (int i = 0; i < arguments.size(); i++) {
            if(isVarArgLastParam && i== varArgParamNum){

                if(node.getChild(i+1).getKind().equals("Arraydefinition")){

                    x1 = visit(node.getChild(i+1));
                    computation.append(x1.getComputation());
                }
                else {
                    tempVarArgAux = OptUtils.getTemp() + ".array.i32";
                    computation.append(tempVarArgAux).append(ASSIGN).append(" .array.i32").append(" new(array, ").append(arguments.size() - varArgParamNum).append(".i32).array.i32").append(END_STMT);
                    computation.append(getNextLabelArray()).append(".array.i32").append(ASSIGN).append(".array.i32 ").append(tempVarArgAux).append(END_STMT);
                    for (int j = 0; j < arguments.size() - varArgParamNum; j++) {
                        computation.append(getCurrentLabelArray()).append(".array.i32").append("[").append(j).append(".i32].i32").append(ASSIGN).append(".i32 ").append(node.getChild(1 + j + varArgParamNum).get("value")).append(".i32").append(END_STMT);
                    }
                }
                break;
            }
            code.append(",");
            JmmNode argument = arguments.get(i);

            // Visit the argument to get its value
            OllirExprResult argumentResult = visit(argument);

            // Extract the code representing the argument value
            String argumentCode = argumentResult.getComputation();
            computation.append(argumentCode);// Extract the code representing the argument value

            // Search for the matching local variable by name
            Optional<Symbol> matchingVariable = localVariables.stream()
                    .filter(variable -> variable.getName().equals(argumentCode))
                    .findFirst();

            // Use the value of the argument in the generated code
            if (matchingVariable.isPresent()) {
                Type argType = matchingVariable.get().getType();
                computation.append(argumentResult.getCode()); // Append the type
            }
            else{

                code.append(argumentResult.getCode());
                var argType = TypeUtils.getExprType(argument, table, methodName);


            }

        }
        if(isVarArgLastParam){
            if(x1 != null){
                code.append(", ").append(x1.getCode());
            }
            else{
            code.append(", ").append(getCurrentLabelArray()).append(".array.i32");}
        }


        code.append(")");

        JmmNode parent = child.getParent();

// Check if any ancestor is an "Assignment" node
        while (parent != null && !parent.getKind().equals("Assignment")) {
            parent = parent.getParent();
        }
        /*if(foundMatchImports) {

            //computation.append(funcLhs.getComputation());

            //computation.append("invokevirtual(").append(node.getChild(0).get("value")).append(OptUtils.toOllirType(argTypeImport)).append(code).append(OptUtils.toOllirType(argTypeImport)).append(END_STMT);
            code.append("tipo correto");
           // return new OllirExprResult(code1,computation);
        }
        else {*/
        Optional<Type> returnType = table.getReturnTypeTry(node.get("value"));
        if (returnType.isPresent()) {
            Type parentType = returnType.get();
            code.append(OptUtils.toOllirType(parentType));
        } else {
            // If an "Assignment" node is found, append the type
            if (parent != null && parent.getKind().equals("Assignment")) {
                String variableName = parent.get("var");
                Optional<Symbol> matchingVariable = localVariables.stream()
                        .filter(variable -> variable.getName().equals(variableName))
                        .findFirst();

                if (!matchingVariable.isPresent()) {
                    List<Symbol> fields = table.getFields();
                    matchingVariable = fields.stream()
                            .filter(variable -> variable.getName().equals(variableName))
                            .findFirst();
                }

                if (matchingVariable.isPresent()) {
                    Type parentType = matchingVariable.get().getType();
                    code.append(OptUtils.toOllirType(parentType));
                }
            } else {
                // If no "Assignment" node is found, append ".V"
                code.append(".V");
            }
        }
        // }



        var isBinaryOpNode = node.getAncestor("BinaryOp");

        if (isBinaryOpNode.isPresent()){
            var aux = OptUtils.getTemp();
            String resOllirType = isBinaryOpNode.get().get("op").equals("+") || isBinaryOpNode.get().get("op").equals("-") || isBinaryOpNode.get().get("op").equals("*") || isBinaryOpNode.get().get("op").equals("/") ? ".i32" : ".bool";
            computation.append(aux).append(resOllirType).append(ASSIGN).append(" ").append(resOllirType).append(" ").append(code).append(END_STMT);
            code = new StringBuilder(aux);
            code.append(resOllirType);
        }

        var isParamNode = node.getAncestor("FunctionCall");

        if (isParamNode.isPresent()){
            var aux = OptUtils.getTemp();
            returnType = table.getReturnTypeTry(node.get("value"));
            Type parentType = returnType.get();
            computation.append(aux).append(OptUtils.toOllirType(parentType)).append(ASSIGN).append(" ").append(OptUtils.toOllirType(parentType)).append(" ").append(code).append(END_STMT);
            code = new StringBuilder(aux);
            code.append(OptUtils.toOllirType(parentType));
        }


        return new OllirExprResult(code.toString(),computation);
    }



    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        var methodName = "";
        var id = node.get("value");
        var parentNode = node.getAncestor("MethodDecl");
        boolean isField = false;
        boolean isParam = false;
        List<Symbol> fields = table.getFields();

        String rhs ="";
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        if (parentNode.isPresent()){
            methodName = parentNode.get().get("name");
        }

        List<Symbol> params = table.getParameters(methodName);
        Type type = TypeUtils.getExprType(node, table,methodName);
        String ollirType = OptUtils.toOllirType(type);

        rhs = node.get("value");
        for (Symbol field : fields) {
            if (field.getName().equals(rhs)){
                isField = true;
            }
        }

        for (Symbol p : params) {
            if (p.getName().equals(rhs)){
                isParam = true;
            }
        }

        if(isParam){
            code.append(id).append(ollirType);

            return new OllirExprResult(code.toString());
        }

        if (isField){
            var aux = OptUtils.getTemp();
            computation.append(aux).append(ollirType).append(" :=").append(ollirType).append(" getfield(this, ").append(rhs).append(ollirType).append(")").append(ollirType).append(END_STMT);
            code.append(aux).append(ollirType);

            return new OllirExprResult(code.toString(),computation);
        }

        code.append(id).append(ollirType);
        return new OllirExprResult(code.toString());
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
