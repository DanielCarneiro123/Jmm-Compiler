package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

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
        var imports = ollirResult.getOllirClass().getImports();
        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL).append(NL);
        var superClass = ollirResult.getOllirClass().getSuperClass();

        if (superClass == null || superClass.equals("Object")) {
            ollirResult.getOllirClass().setSuperClass("java/lang/Object");
            code.append(".super ").append(ollirResult.getOllirClass().getSuperClass()).append(NL);
            code.append(";default constructor").append(NL);//ter de ver se na class ha algum construtor, se nao hovuer vai se ao extend

        } else {
            String classExtended = ollirResult.getOllirClass().getSuperClass(); // se calhar vamos ter de ter um if par se não for extended
            code.append(".super ").append(classExtended).append(NL);
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
            var fieldType = getFieldType(field.getFieldType());
            code.append(".field ").append(fieldAcessModifier);
            if (field.isFinalField()) code.append("final ");
            if (field.isStaticField()) code.append("static ");
            code.append(fieldName).append(" ").append(fieldType).append(NL);
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
        // Load the object reference onto the stack
        code.append(generators.apply(putFieldInst.getObject()));

        // Load the value of the field onto the stack
        code.append(generators.apply(putFieldInst.getValue()));

        String callObjName = getImportedClassName(putFieldInst.getObject().getName());
        String fieldName = putFieldInst.getField().getName();
        String fieldType = getFieldType(putFieldInst.getField().getType());
        // Emit the getfield instruction
        code.append("putfield ")
                .append(callObjName)
                .append("/")
                .append(fieldName)
                .append(" ")
                .append(fieldType)
                .append("\n");

        // Store the value in the appropriate local variable
        // code.append(getStoreInstruction(getFieldInst.getDestination()));
        return code.toString();
    }


    private String generateGetFieldInstruction(GetFieldInstruction getFieldInst) {
        StringBuilder code = new StringBuilder();

        // Load the object reference onto the stack
        code.append(generators.apply(getFieldInst.getObject()));

        String callObjName = getImportedClassName(getFieldInst.getObject().getName());
        String fieldName = getFieldInst.getField().getName();
        String fieldType = getFieldType(getFieldInst.getFieldType());
        // Emit the getfield instruction
        code.append("getfield ")
                .append(callObjName)
                .append("/")
                .append(fieldName)
                .append(" ")
                .append(fieldType)
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
        if (method.isStaticMethod()) {
            code.append("\n.method ").append(modifier).append("static ").append(methodName)
                    .append("("); //temos de ver se isto do [ só acontece para os main static ou para todos os tatic
        } else {
            // nome do método, este comentário de merda não foi pelo chatgpt
            code.append("\n.method ").append(modifier).append(methodName)
                    .append("(");
        }

        // tipos dos parametros, este comentário de merda não foi pelo chatgpt
        var parameterTypes = method.getParams();
        for (int i = 0; i < parameterTypes.size(); i++) {
            Type paramType = parameterTypes.get(i).getType();
            String paramJasminType = getFieldType(paramType);
            code.append(paramJasminType);
        }
        // tipo do retorno, este comentário de merda não foi pelo chatgpt
        Type methodReturnType = method.getReturnType();
        String methodReturnJasminType = getFieldType(methodReturnType);
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
            if (inst.getInstType() == InstructionType.CALL && ((CallInstruction) inst).getReturnType().getTypeOfElement() != ElementType.VOID) {
                code.append("pop");
            }
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
            case STRING:
                return "[L" + "java/lang/String" + ";";
            case ARRAYREF:
                return "L" + "java/lang/String" + ";";

            default:
                return null;
        }
    }

    private String getFieldType(Type type) {
        return switch (type.getTypeOfElement()) {
            case ARRAYREF -> this.getArrayType(type);
            case OBJECTREF -> this.getObjectType(type);
            default -> this.getJasminType(type.getTypeOfElement());
        };
    }

    private String getArrayType(Type type) {
        return "[" + this.getJasminType(type.getTypeOfElement());
    }

    private String getObjectType(Type type) {
        return "L" + this.getImportedClassName(((ClassType) type).getName()) + ";";
    }

    private String getImportedClassName(String basicClassName) {

        if (basicClassName.equals("this"))
            return this.ollirResult.getOllirClass().getClassName();

        String realClass = "." + basicClassName;

        if (ollirResult.getOllirClass().getImportedClasseNames().contains(basicClassName)){
            for (var imp: ollirResult.getOllirClass().getImports()) {
                if (imp.endsWith(realClass)) {
                    return normalizeClassName(imp);
                }
            }
        }


        return basicClassName;
    }

    private String normalizeClassName2(String className) {
        // Converte para minúsculas e remove espaços em branco extras
        return className.toLowerCase().trim();
    }


    private String normalizeClassName(String className) {
        return className.replace('.', '/');
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
            case INT32, BOOLEAN:
                if (reg > 3) {
                    code.append("istore ").append(reg).append(NL);
                    break;

                } else {
                    code.append("istore_").append(reg).append(NL);
                }
                break;
            case CLASS, OBJECTREF, STRING:
                if (reg > 3) {
                    code.append("astore ").append(reg).append(NL);
                    break;
                } else {
                    code.append("astore_").append(reg).append(NL);
                }
                break;
            case VOID:
                {}
            default:
                throw new NotImplementedException("Type not supported: " + type.name());
        }

        return code.toString();
    }

    private String generateCallInstruction(CallInstruction callInstruction) {
        var code = new StringBuilder();
        var v = callInstruction.getOperands();

        switch (callInstruction.getInvocationType()) {
            case invokestatic:
                Operand first = (Operand) callInstruction.getOperands().get(0);
                LiteralElement second = (LiteralElement) callInstruction.getOperands().get(1);
                for (var op : callInstruction.getArguments()) {
                    code.append(generators.apply(op));
                }
                code.append("invokestatic ").append(getImportedClassName(generators.apply(callInstruction.getCaller()))).append("/").append(generators.apply(callInstruction.getMethodName()));
                //code.append("(");
                for (var arg : callInstruction.getArguments()) {
                    code.append(getFieldType(arg.getType()));
                }
                code.append(")");
                code.append(getFieldType(callInstruction.getReturnType())).append(NL);
                break;
            case invokespecial:
                code.append(generators.apply(callInstruction.getOperands().get(0))).append(NL);
                var elemType = ((Operand) callInstruction.getCaller()).getType();
                code.append("invokespecial ");
                if (elemType.getTypeOfElement() == ElementType.THIS){
                    code.append(ollirResult.getOllirClass().getSuperClass());
                }
                else {
                    code.append(getImportedClassName(((ClassType) elemType).getName()));
                }
                code.append("/<init>(");


                for (var elem: callInstruction.getArguments()){
                    code.append(getFieldType(elem.getType()));
                }
                code.append(")");
                code.append(getFieldType(callInstruction.getReturnType())).append(NL);
                code.append("pop");
                break;
            case NEW:
                for (Element objetElement : callInstruction.getArguments())
                    code.append(generators.apply(objetElement));
                code.append(NL).append("new ").append(getImportedClassName(((Operand) callInstruction.getCaller()).getName())).append(NL).append("dup").append(NL);
                //code.append("dup").append(NL);
                break;
            case invokevirtual:
                code.append(generators.apply(callInstruction.getOperands().get(0))).append(NL);
                Operand firstVirtual = (Operand) callInstruction.getOperands().get(0);
                LiteralElement secondVirtual = (LiteralElement) callInstruction.getOperands().get(1);
                for (var op : callInstruction.getArguments()) {
                    code.append(generators.apply(op));
                }
                code.append("invokevirtual ").append(getImportedClassName(((ClassType) firstVirtual.getType()).getName())).append("/").append(generators.apply(callInstruction.getMethodName()));
                //code.append("(");
                for (var arg : callInstruction.getArguments()) {
                    code.append(getFieldType(arg.getType()));
                }
                code.append(")");
                code.append(getFieldType(callInstruction.getReturnType())).append(NL);
                break;
            case arraylength:
                code.append(generators.apply(callInstruction.getOperands().get(0)));
                code.append(" arraylength").append(NL);
                break;
            case invokeinterface:
                code.append(generators.apply(callInstruction.getOperands().get(0))).append(NL);
                Operand firstInterface = (Operand) callInstruction.getOperands().get(0);
                LiteralElement secondInterface = (LiteralElement) callInstruction.getOperands().get(1);
                for (var op : callInstruction.getArguments()) {
                    code.append(generators.apply(op));
                }
                code.append("invokeinterface ").append(getImportedClassName(((ClassType) firstInterface.getType()).getName())).append("/").append(secondInterface.getLiteral().replace("\"", ""));
                code.append("(");
                for (var arg : callInstruction.getArguments()) {
                    code.append(getFieldType(arg.getType()));
                }
                code.append(")");
                code.append(getFieldType(callInstruction.getReturnType())).append(NL);
                break;
            default:
                throw new NotImplementedException("Invocation type not supported: " + callInstruction.getInvocationType());
        }

        return code.toString();
    }


    private String generateSingleOp(SingleOpInstruction singleOp) {

        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        StringBuilder code = new StringBuilder();
        ;
        String literalStr = literal.getLiteral();
        if (literal.getType().getTypeOfElement().name().equals("STRING")) {
            code.append(literalStr.replaceAll("\"", "")).append("(");
            return code.toString();
        }
        int value = Integer.parseInt(literalStr);
        if (value >= -1 && value <= 5) {
            return "iconst_" + value + NL;
        } else if (value >= -128 && value <= 127) {
            return "bipush " + value + NL;
        } else if (value >= -32768 && value <= 32767) {
            return "sipush " + value + NL;
        } else {
            return "ldc " + value + NL;
        }
    }

    private String generateOperand(Operand operand) {
        String name = operand.getName();

        // Verificar se o nome está na varTable
        if (currentMethod.getVarTable().containsKey(name)) {
            int reg = currentMethod.getVarTable().get(name).getVirtualReg();
            String type = operand.getType().getTypeOfElement().name();
            if (type.equals("INT32") || type.equals("BOOLEAN")) {
                if (reg > 3) {
                    return "iload " + reg + NL;
                }
                return "iload_" + reg + NL;
            } else {
                if (reg > 3) {
                    return "aload " + reg + NL;
                }
                return "aload_" + reg + NL;
            }
        }

        if (currentMethod.getOllirClass().getImports().contains(name)) {
            return name;
        }

        String realClass = "." + name;

        if (ollirResult.getOllirClass().getImportedClasseNames().contains(name)){
            for (var imp: ollirResult.getOllirClass().getImports()) {
                if (imp.endsWith(realClass)) {
                    return name;
                }
            }
        }

        return null;

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
            case SUB -> "isub";
            case DIV -> "idiv";
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
            case OBJECTREF, STRING:
                code.append(NL).append("areturn").append(NL);
                break;
            default:
                throw new NotImplementedException("Return type not supported: " + returnType.name());
        }

        return code.toString();
    }
}
