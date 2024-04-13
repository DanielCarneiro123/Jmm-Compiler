package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(PutFieldInstruction.class, this::generatePutFieldInstruction);
        generators.put(GetFieldInstruction.class, this::generateGetFieldInstruction);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(CallInstruction.class, this::generateCallInstruction);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }

    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL).append(NL);

        if (ollirResult.getOllirClass().getSuperClass() != null) { //ter de ver se na class ha algum construtor, se nao hovuer vai se ao extend
            var classExtended = ollirResult.getOllirClass().getSuperClass(); // se calhar vamos ter de ter um if par se não for extended
            code.append(".super ").append(classExtended).append(NL);
        }
        else {
            ollirResult.getOllirClass().setSuperClass("java/lang/Object");
            code.append(".super ").append(ollirResult.getOllirClass().getSuperClass()).append(NL);
            code.append(";default constructor").append(NL);
        }

        var classFields = ollirResult.getOllirClass().getFields();
        for (var field : classFields) {
            var fieldAcessModifier = field.getFieldAccessModifier().name();
            switch (field.getFieldAccessModifier().name()) {
                case "PUBLIC":
                    fieldAcessModifier = "public ";
                    break;
                case "PRIVATE":
                    fieldAcessModifier = "private ";
                    break;
                case "DEFAULT":
                    fieldAcessModifier = "";
                    break;
            }
            var fieldName = field.getFieldName();
            var fieldType = getJasminType(field.getFieldType().getTypeOfElement());
            code.append(".field ").append(fieldAcessModifier).append(fieldName).append(" ").append(fieldType).append(NL);
        }

        // generate a single constructor method

        code.append(".method public <init>()V").append(NL);
        code.append(TAB).append("aload_0").append(NL);
        code.append(TAB).append("invokespecial ");

        code.append(ollirResult.getOllirClass().getSuperClass()).append("/<init>()V").append(NL);

        code.append(TAB).append("return").append(NL);
        code.append(".end method").append(NL);

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));
        }

        return code.toString();
    }

    private String generatePutFieldInstruction(PutFieldInstruction putFieldInst) {
        StringBuilder code = new StringBuilder();

        var teste = putFieldInst.getOperands().get(1);
        // Emit the getfield instruction
        code.append("putfield ")
                .append("intField")
                .append(" ")
                .append("\n");

        // Store the value in the appropriate local variable
        // code.append(getStoreInstruction(getFieldInst.getDestination()));
        return code.toString();
    }

    private String generateGetFieldInstruction(GetFieldInstruction getFieldInst) {

        StringBuilder code = new StringBuilder();

        // Emit the getfield instruction
        code.append("getfield ")
                .append("intField")
                .append(" ")
                .append("\n");

        // Store the value in the appropriate local variable
        // code.append(getStoreInstruction(getFieldInst.getDestination()));
        return code.toString();
    }

    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var methodName = method.getMethodName();

        //ver se precisa de static
        if (method.isStaticMethod()){
            code.append("\n.method ").append(modifier).append("static ").append(methodName)
                    .append("(["); //temos de ver se isto do [ só acontece para os main static ou para todos os tatic
        }
        else{
            // nome do método, este comentário de merda não foi pelo chatgpt
            code.append("\n.method ").append(modifier).append(methodName)
                    .append("(");
        }

        // tipos dos parametros, este comentário de merda não foi pelo chatgpt
        var parameterTypes = method.getParams();
        for (int i = 0; i < parameterTypes.size(); i++) {
            ElementType paramType = parameterTypes.get(i).getType().getTypeOfElement();
            String paramJasminType = getJasminType(paramType);
            code.append(paramJasminType);
        }
        // tipo do retorno, este comentário de merda não foi pelo chatgpt
        ElementType methodReturnType = method.getReturnType().getTypeOfElement();
        String methodReturnJasminType = getJasminType(methodReturnType);
        code.append(")").append(methodReturnJasminType).append(NL);

        // aqui criei duas funções para calcular os limites

        int maxStackSize = calculateMaxStackSize(method);//não sei se devemos chamar
        int maxLocals = calculateMaxLocals(method);//não sei se devemos chamar
        code.append(TAB).append(".limit stack ").append("99").append(NL);
        code.append(TAB).append(".limit locals ").append("99").append(NL);


        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private int calculateMaxStackSize(Method method) {
        int maxStackSize = 0;
        int currentStackSize = 0;

        for (var inst : method.getInstructions()) {
            switch (inst.getInstType().name()) { //este switch case ta mal
                case "LOAD":
                case "CONST":
                    currentStackSize++;
                    break;
                case "ASSIGN":
                case "RETURN":
                    currentStackSize--;
                    break;
            }
            if (currentStackSize > maxStackSize) {
                maxStackSize = currentStackSize;
            }
        }

        return maxStackSize;
    }

    private int calculateMaxLocals(Method method) {
        int maxLocals = 0;

        maxLocals += method.getParams().size();

        for (var inst : method.getInstructions()) {
            if (inst.getInstType().name().equals("ASSIGN") || inst.getInstType().name().equals("RETURN")) {
                maxLocals++;
            }
        }

        return maxLocals;
    }

    private String getJasminType(ElementType paramType) {
        switch (paramType) {
            case INT32:
                return "I";
            case BOOLEAN:
                return "Z";
            case VOID:
                return "V";
            default:
                return "L" + "java/lang/String" + ";";
        }
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        ElementType type = operand.getType().getTypeOfElement();
        switch (type) {
            case INT32:
                code.append("istore_").append(reg).append(NL).append(NL);
                break;
            case BOOLEAN:
                code.append("istore_").append(reg).append(NL).append(NL);
                break;
            case CLASS:
                code.append("astore").append(reg).append(NL).append(NL);
            default:
                throw new NotImplementedException("Type not supported: " + type.name());
        }

        return code.toString();
    }

    private String generateCallInstruction(CallInstruction callInstruction) {
        var code = new StringBuilder();

        switch (callInstruction.getInvocationType()) {
            case invokestatic:
                code.append("invokestatic ");
                break;
            case invokespecial, NEW:
                code.append("invokespecial ");
                break;
            case invokevirtual:
                code.append("invokevirtual ");
                break;
            default:
                throw new NotImplementedException("Invocation type not supported: " + callInstruction.getInvocationType());
        }

        var x = callInstruction.getCaller().toString();

        var y = callInstruction.getMethodName().toString();

        code.append(callInstruction.getCaller().toString())
                .append("/")
                .append(callInstruction.getMethodName().toString())
                .append("(");



        return code.toString();
    }


    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return NL + "iconst_" + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        return "iload_" + reg + NL;
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case MUL -> "imul";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        // generate code for the return value
        if (returnInst.getOperand() != null) {
            code.append(generators.apply(returnInst.getOperand()));
        }


        ElementType returnType = returnInst.getReturnType().getTypeOfElement();
        switch (returnType) {
            case INT32:
                code.append(NL).append("ireturn").append(NL);
                break;
            case BOOLEAN:
                code.append(NL).append("ireturn").append(NL);
                break;
            case VOID:
                code.append(NL).append("return").append(NL);
                break;
            default:
                throw new NotImplementedException("Return type not supported: " + returnType.name());
        }

        return code.toString();
    }


}
