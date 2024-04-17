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
        addVisit(NEW_CLASS, this::visitNewClass);
        addVisit(PARENTESIS, this::visitParentesis);
        addVisit(TRUE, this::visitBoolLiteral);
        addVisit(FALSE, this::visitBoolLiteral);
        addVisit(NEGATION, this::visitNegationExpr);


        setDefaultVisit(this::defaultVisit);
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

        // Get the class name
        String className = node.get("classname");

        // Generate code for creating a new instance of the class
        String instanceVar = OptUtils.getTemp();
        code.append(instanceVar).append(".").append(className).append(SPACE)
                .append(":=").append(".").append(className).append(SPACE).append("new(").append(className).append(")").append(".").append(className).append(END_STMT);

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


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();
        String resOllirType = node.get("op").equals("+") || node.get("op").equals("-") || node.get("op").equals("*") || node.get("op").equals("/") ? ".i32" : ".bool";

        // Check if either lhs or rhs is a function call, and if so, store their result in temporary variables
        if (lhs.getCode().contains("invoke") || rhs.getCode().contains("invoke")) {
            // Compute the value of lhs if it's a function call
            if (lhs.getCode().contains("invoke")) {
                String tempLhs = OptUtils.getTemp();
                computation.append(lhs.getComputation());
                computation.append(tempLhs).append(resOllirType).append(" := ").append(resOllirType).append(" ").append(lhs.getCode()).append(END_STMT);
                lhs = new OllirExprResult(tempLhs + resOllirType); // Include the type here
            }
            // Compute the value of rhs if it's a function call
            if (rhs.getCode().contains("invoke")) {
                String tempRhs = OptUtils.getTemp();
                computation.append(rhs.getComputation());
                computation.append(tempRhs).append(resOllirType).append(" := ").append(resOllirType).append(" ").append(rhs.getCode()).append(END_STMT);
                rhs = new OllirExprResult(tempRhs + resOllirType); // Include the type here
            }
        }

        // Generate code for the computation of the result
        String code = OptUtils.getTemp() + resOllirType;
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());
        computation.append(code).append(" := ").append(resOllirType).append(" ").append(lhs.getCode()).append(" ").append(node.get("op")).append(resOllirType).append(" ").append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitFunctionCall(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();
        String code1 = "";
        var funcLhs =  visit(node.getJmmChild(0));
        Type argTypeImport = new Type("aux",false);
        boolean foundMatchImports = false;

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

            String firstChildName = node.getChildren().get(0).get("value");

            Optional<Symbol> matchingVariable1 = localVariables.stream()
                    .filter(variable -> variable.getName().equals(firstChildName))
                    .findFirst();

            if (matchingVariable1.isPresent()) {
                argTypeImport = matchingVariable1.get().getType();
                String argTypeStr = OptUtils.toOllirType(argTypeImport).substring(1); // Remove the first character (the dot)
                foundMatchImports = imports.contains(argTypeStr);
            }

            code1 = OptUtils.getTemp() + OptUtils.toOllirType(argTypeImport);

// Check if the type of the first child's name is in the list of imports



// Check if any local variable's name matches the first child's name
            boolean foundMatch1 = localVariables.stream()
                    .map(Symbol::getName)
                    .anyMatch(name -> name.equals(firstChildName));

            if (foundMatchImports) {

                computation.append(funcLhs.getComputation());
                computation.append(code1).append(" :=").append(OptUtils.toOllirType(argTypeImport)).append(" ");
                funcLhs = new OllirExprResult(code1); // Include the type here

                /*

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
        for (int i = 0; i < arguments.size(); i++) {
            code.append(",");
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

        }



        code.append(")");

        JmmNode parent = child.getParent();

// Check if any ancestor is an "Assignment" node
        while (parent != null && !parent.getKind().equals("Assignment")) {
            parent = parent.getParent();
        }
        if(foundMatchImports) {

            computation.append(funcLhs.getComputation());

            computation.append("invokevirtual(").append(node.getChild(0).get("value")).append(OptUtils.toOllirType(argTypeImport)).append(code).append(OptUtils.toOllirType(argTypeImport)).append(END_STMT);
            return new OllirExprResult(code1,computation);
        }
        else
// If an "Assignment" node is found, append the type
            if (parent != null && parent.getKind().equals("Assignment")) {
                String variableName = parent.get("var");
                Optional<Symbol> matchingVariable = localVariables.stream()
                        .filter(variable -> variable.getName().equals(variableName))
                        .findFirst();
                if (matchingVariable.isPresent()) {
                    Type parentType = matchingVariable.get().getType();
                    code.append(OptUtils.toOllirType(parentType));
                }
            } else {
                // If no "Assignment" node is found, append ".V"
                code.append(".V");
            }


        return new OllirExprResult(code.toString());
    }



    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        var methodName = "";
        var id = node.get("value");
        var parentNode = node.getAncestor("MethodDecl");

        if (parentNode.isPresent()){
             methodName = parentNode.get().get("name");
        }
        Type type = TypeUtils.getExprType(node, table,methodName);
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
