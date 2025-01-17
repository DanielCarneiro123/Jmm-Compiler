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

import static org.specs.comp.ollir.InstructionType.*;
import static org.specs.comp.ollir.OperationType.*;


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

    int stack_value;

    int curr_stack_value;

    int locals_value;

    int stackVariation;

    int array_length = 0;

    int label_control = 0;

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
        generators.put(CondBranchInstruction.class, this::generateBranch);
        generators.put(GotoInstruction.class, this::generateGoto);
        generators.put(ArrayOperand.class, this::generateLoadArray);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOp);
    }

    private String generateLoadArray(ArrayOperand arrayOperand) {
        var code = new StringBuilder();
        this.curr_stack_value++;
        maxStackValue();
        int reg = currentMethod.getVarTable().get(arrayOperand.getName()).getVirtualReg();
        code.append(NL).append("aload").append(reg < 4 ? '_' : ' ').append(reg).append(NL);
        code.append(generators.apply(arrayOperand.getIndexOperands().get(0))).append(NL).append("iaload").append(NL);
        this.curr_stack_value--;
        maxStackValue();
        return code.toString();
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

        this.curr_stack_value -= 2;
        maxStackValue();

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
        code.append(NL).append("getfield ")
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
        this.curr_stack_value = 0;
        this.stack_value = 0;

        this.locals_value = currentMethod.getVarTable().values().stream()
                .map(Descriptor::getVirtualReg)
                .collect(Collectors.toSet())
                .size();

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
            if (!method.getVarTable().containsKey("this")) {
                this.locals_value++;
            }
            code.append("\n.method ").append(modifier).append(methodName)
                    .append("(");
        }

        var parameterTypes = method.getParams();
        for (int i = 0; i < parameterTypes.size(); i++) {
            Type paramType = parameterTypes.get(i).getType();
            String paramJasminType = getFieldType(paramType);
            code.append(paramJasminType);
        }

        Type methodReturnType = method.getReturnType();
        String methodReturnJasminType = getFieldType(methodReturnType);
        code.append(")").append(methodReturnJasminType).append(NL);

        // aqui criei duas funções para calcular os limites

        var methodCode = methodPrint(method);

        code.append(TAB).append(".limit stack ").append(stack_value).append(NL);
        code.append(TAB).append(".limit locals ").append(locals_value).append(NL);


        code.append(methodCode).append(NL);


        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String methodPrint(Method method) {
        var code = new StringBuilder();
        for (var inst : method.getInstructions()) {
            method.getLabels().entrySet().stream()
                    .filter(label -> label.getValue().equals(inst))
                    .forEach(label -> code.append(NL).append(label.getKey()).append(":").append(NL));
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
            if (inst.getInstType() == CALL && ((CallInstruction) inst).getReturnType().getTypeOfElement() != ElementType.VOID) {
                code.append(TAB).append("pop").append(NL);
                curr_stack_value--;
                maxStackValue();
            }

            /*if (inst.getInstType() == ASSIGN && ((AssignInstruction) inst).getRhs().getInstType().equals(GETFIELD)){
                code.append(NL).append(TAB).append(generators.apply(((AssignInstruction) inst).getRhs().getChildren().get(1))).append(NL);
                curr_stack_value++;
                maxStackValue();
            }*/

        }
        return code.toString();

    }


    private String getJasminType(ElementType paramType) {
        switch (paramType) {
            case INT32, ARRAYREF:
                return "I";
            case BOOLEAN:
                return "Z";
            case VOID:
                return "V";
            case STRING:
                return "[L" + "java/lang/String" + ";";
            /*case ARRAYREF:
                return "L" + "java/lang/String" + ";";*/
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
        if (type.toString().equals("STRING[]")) {
            return "[" + "L" + "java/lang/String" + ";";
        }
        return "[" + this.getJasminType(type.getTypeOfElement());
    }

    private String getObjectType(Type type) {
        return "L" + this.getImportedClassName(((ClassType) type).getName()) + ";";
    }

    private String getImportedClassName(String basicClassName) {

        if (basicClassName.equals("this"))
            return this.ollirResult.getOllirClass().getClassName();

        String realClass = "." + basicClassName;

        if (ollirResult.getOllirClass().getImportedClasseNames().contains(basicClassName)) {
            for (var imp : ollirResult.getOllirClass().getImports()) {
                if (imp.endsWith(realClass)) {
                    return normalizeClassName(imp);
                }
            }
        }


        return basicClassName;
    }


    private String normalizeClassName(String className) {
        return className.replace('.', '/');
    }


    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();
        /*this.curr_stack_value++;
        maxStackValue();*/
        //having doubts about this

        var lhs = assign.getDest();
        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        //deal with array content
        if (lhs instanceof ArrayOperand arrayOperand) {
            if (reg >= 4) {
                code.append("aload ").append(reg).append(NL);
            } else {
                code.append("aload_").append(reg).append(NL);
            }
            curr_stack_value++;
            maxStackValue();
            for (var elem : arrayOperand.getIndexOperands()) {
                code.append(generators.apply(elem)).append(NL);
            }
        }

        if (assign.getRhs().getInstType().equals(BINARYOPER)) {
            var leftSide = assign.getDest();
            var firstRightSide = assign.getRhs().getChildren().get(0);
            var secondRightSide = assign.getRhs().getChildren().get(1);
            var bin = (BinaryOpInstruction) assign.getRhs();
            var op = bin.getOperation().getOpType();
            if (leftSide.getType().getTypeOfElement().equals(ElementType.INT32) && (op.equals(ADD) || op.equals(SUB))) {
                code.append(iincVar((Operand) assign.getDest(), (BinaryOpInstruction) assign.getRhs()));
            }
            if (!code.toString().isEmpty()) {
                curr_stack_value--;
                maxStackValue();
                return code.toString();
            }
        }


        code.append(generators.apply(assign.getRhs()));
        if (assign.getDest().getType().getTypeOfElement().equals(ElementType.BOOLEAN) && assign.getRhs() instanceof BinaryOpInstruction) {
            var firstVal = this.label_control;
            code.append("cmp_lt_").append(this.label_control).append("_true").append(NL);
            code.append("iconst_0").append(NL);
            this.curr_stack_value++;
            maxStackValue();
            code.append("goto ").append("cmp_lt_").append(this.label_control).append("_end").append(NL).append(NL);
            code.append("cmp_lt_").append(firstVal).append("_true").append(":").append(NL);
            code.append("iconst_m1").append(NL).append(NL);
            this.curr_stack_value++;
            maxStackValue();
            code.append("cmp_lt_").append(this.label_control).append("_end").append(":").append(NL);
            this.label_control++;

        }
        ElementType type = operand.getType().getTypeOfElement();
        switch (type) {
            case INT32, BOOLEAN:
                if (currentMethod.getVarTable().get(operand.getName()).getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                    code.append("iastore").append(NL);
                    curr_stack_value -= 3;
                    maxStackValue();
                    break;
                } else {
                    curr_stack_value--;
                    maxStackValue();
                    if (reg > 3) {
                        code.append("istore ").append(reg).append(NL);
                        break;

                    } else {
                        code.append("istore_").append(reg).append(NL);
                    }
                }
                break;
            case OBJECTREF, STRING, ARRAYREF, THIS:
                curr_stack_value--;
                maxStackValue();
                if (reg > 3) {
                    code.append("astore ").append(reg).append(NL);
                    break;
                } else {
                    code.append("astore_").append(reg).append(NL);
                }
                break;
            case VOID: {
            }
            default:
                throw new NotImplementedException("Type not supported: " + type.name());
        }

        return code.toString();
    }

    private String boolExprVar(Element dest, Instruction rhs) {
        var code = new StringBuilder();
        code.append(generators.apply(rhs));


        return "";
    }

    private String iincVar(Operand dest, BinaryOpInstruction rhs) {
        var code = new StringBuilder();
        var firstRight = rhs.getChildren().get(0);
        var secondRight = rhs.getChildren().get(1);
        if (firstRight instanceof Operand && secondRight instanceof LiteralElement) {
            if (((Operand) firstRight).getName().equals(dest.getName())) {
                if (rhs.getOperation().getOpType().equals(ADD)) {
                    int reg = currentMethod.getVarTable().get(dest.getName()).getVirtualReg();
                    code.append("iinc ").append(reg).append(" ").append(((LiteralElement) secondRight).getLiteral()).append(NL);
                } else if (rhs.getOperation().getOpType().equals(SUB)) {
                    int reg = currentMethod.getVarTable().get(dest.getName()).getVirtualReg();
                    code.append("iinc ").append(reg).append(" -").append(((LiteralElement) secondRight).getLiteral()).append(NL);
                }
            }

        } else if (firstRight instanceof LiteralElement && secondRight instanceof Operand) {
            if (((Operand) secondRight).getName().equals(dest.getName())) {
                if (rhs.getOperation().getOpType().equals(ADD)) {
                    int reg = currentMethod.getVarTable().get(dest.getName()).getVirtualReg();
                    code.append("iinc ").append(reg).append(" ").append(((LiteralElement) firstRight).getLiteral()).append(NL);
                } else if (rhs.getOperation().getOpType().equals(SUB)) {
                    int reg = currentMethod.getVarTable().get(dest.getName()).getVirtualReg();
                    code.append("iinc ").append(reg).append(" -").append(((LiteralElement) firstRight).getLiteral()).append(NL);
                }
            }
        }
        return code.toString();
    }

    private String generateCallInstruction(CallInstruction callInstruction) {
        var code = new StringBuilder();
        this.stackVariation = 0;
        switch (callInstruction.getInvocationType()) {
            case invokestatic:
                for (var op : callInstruction.getArguments()) {
                    this.stackVariation++;
                    code.append(generators.apply(op));
                }
                code.append("invokestatic ").append(getImportedClassName(generators.apply(callInstruction.getCaller()))).append("/").append(generators.apply(callInstruction.getMethodName()));
                //code.append("(");
                for (var arg : callInstruction.getArguments()) {
                    code.append(getFieldType(arg.getType()));
                }
                code.append(")");
                code.append(getFieldType(callInstruction.getReturnType())).append(NL);
                if (!callInstruction.getReturnType().getTypeOfElement().equals(ElementType.VOID)) {
                    this.stackVariation--;
                }
                break;
            case invokespecial:
                code.append(generators.apply(callInstruction.getOperands().get(0))).append(NL);
                var elemType = ((Operand) callInstruction.getCaller()).getType();
                code.append("invokespecial ");
                if (elemType.getTypeOfElement() == ElementType.THIS) {
                    code.append(ollirResult.getOllirClass().getSuperClass());
                } else {
                    code.append(getImportedClassName(((ClassType) elemType).getName()));
                }
                code.append("/<init>(");


                for (var elem : callInstruction.getArguments()) {
                    code.append(getFieldType(elem.getType()));
                }
                code.append(")");
                code.append(getFieldType(callInstruction.getReturnType())).append(NL);
                if (!callInstruction.getReturnType().getTypeOfElement().equals(ElementType.VOID)) {
                    this.stackVariation--;
                }
                code.append("pop");
                break;
            case NEW:
                this.stackVariation = -1;
                for (Element objetElement : callInstruction.getArguments()) {
                    this.stackVariation++;
                    code.append(generators.apply(objetElement));
                }
                if (!callInstruction.getReturnType().getTypeOfElement().equals(ElementType.ARRAYREF)) {
                    code.append(NL).append("new ").append(getImportedClassName(((Operand) callInstruction.getCaller()).getName())).append(NL).append("dup").append(NL);
                } else {
                    code.append("newarray int").append(NL);
                }
                curr_stack_value++;
                maxStackValue();
                break;
            case invokevirtual:
                code.append(generators.apply(callInstruction.getOperands().get(0))).append(NL);
                Operand firstVirtual = (Operand) callInstruction.getOperands().get(0);
                this.stackVariation = 1;
                for (var op : callInstruction.getArguments()) {
                    this.stackVariation++;
                    code.append(generators.apply(op));
                }
                code.append("invokevirtual ").append(getImportedClassName(((ClassType) firstVirtual.getType()).getName())).append("/").append(generators.apply(callInstruction.getMethodName()));
                for (var arg : callInstruction.getArguments()) {
                    code.append(getFieldType(arg.getType()));
                }
                code.append(")").append(getFieldType(callInstruction.getReturnType())).append(NL);

                if (!(callInstruction.getReturnType().getTypeOfElement().equals(ElementType.VOID)))
                    this.stackVariation--;
                break;
            case arraylength:
                code.append(generators.apply(callInstruction.getOperands().get(0)));
                code.append("arraylength").append(NL);
                this.array_length = 1;
                break;
            case invokeinterface:
                code.append(generators.apply(callInstruction.getOperands().get(0))).append(NL);
                Operand firstInterface = (Operand) callInstruction.getOperands().get(0);
                LiteralElement secondInterface = (LiteralElement) callInstruction.getOperands().get(1);
                for (var op : callInstruction.getArguments()) {
                    code.append(generators.apply(op));
                }

                //curr_stack_value -= callInstruction.getArguments().size();
                code.append("invokeinterface ").append(getImportedClassName(((ClassType) firstInterface.getType()).getName())).append("/").append(secondInterface.getLiteral().replace("\"", ""));
                code.append("(");
                for (var arg : callInstruction.getArguments()) {
                    code.append(getFieldType(arg.getType()));
                }
                code.append(")").append(getFieldType(callInstruction.getReturnType())).append(NL);
                break;
            default:
                throw new NotImplementedException("Invocation type not supported: " + callInstruction.getInvocationType());
        }
        this.curr_stack_value -= this.stackVariation;
        maxStackValue();
        return code.toString();
    }


    private String generateSingleOp(SingleOpInstruction singleOp) {

        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        StringBuilder code = new StringBuilder();
        String literalStr = literal.getLiteral();
        if (literal.getType().getTypeOfElement().name().equals("STRING")) {
            code.append(literalStr.replaceAll("\"", "")).append("(");
            return code.toString();
        }
        curr_stack_value++;
        maxStackValue();
        int value = Integer.parseInt(literalStr);
        if (value == -1) {
            return NL + "iconst_m1" + NL;
        } else if (value >= 0 && value <= 5) {
            return NL + "iconst_" + value + NL;
        } else if (value >= -128 && value <= 127) {
            return NL + "bipush " + value + NL;
        } else if (value >= -32768 && value <= 32767) {
            return NL + "sipush " + value + NL;
        } else {
            return NL + "ldc " + value + NL;
        }
    }

    private String generateOperand(Operand operand) {
        String name = operand.getName();
        // Verificar se o nome está na varTable
        curr_stack_value++;
        maxStackValue();
        if (currentMethod.getVarTable().containsKey(name)) {
            int reg = currentMethod.getVarTable().get(name).getVirtualReg();
            String type = operand.getType().getTypeOfElement().name();
            if (type.equals("THIS")) {
                return "aload_0" + NL;
            } else if (reg > -1) {
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

        }

        if (currentMethod.getOllirClass().getImports().contains(name)) {
            return name;
        }

        String realClass = "." + name;

        if (ollirResult.getOllirClass().getImportedClasseNames().contains(name)) {
            for (var imp : ollirResult.getOllirClass().getImports()) {
                if (imp.endsWith(realClass)) {
                    return name;
                }
            }
        }

        return "";

    }

    private String generateBinaryOp(BinaryOpInstruction binaryOpInstruction) {
        OperationType opType = binaryOpInstruction.getOperation().getOpType();

        if (isConditionalOperation(opType)) {
            return generateConditionalBinaryOp(binaryOpInstruction);
        } else if (isArithmeticOperation(opType)) {
            return generateAritmeticBinaryOp(binaryOpInstruction);
        }

        this.curr_stack_value--;
        maxStackValue();
        return "";
    }

    private boolean isConditionalOperation(OperationType opType) {
        return switch (opType) {
            case LTH, GTH, EQ, NEQ, LTE, GTE -> true;
            default -> false;
        };
    }

    private boolean isArithmeticOperation(OperationType opType) {
        return switch (opType) {
            case ADD, SUB, MUL, DIV, XOR, AND, OR, ANDB, ORB -> true;
            default -> false;
        };
    }

    //needs optimizations
    private String generateUnaryOp(UnaryOpInstruction unaryOpInstruction) {
        var code = new StringBuilder();
        code.append(generators.apply(unaryOpInstruction.getOperand()));
        if (unaryOpInstruction.getOperation().getOpType() == NOTB) {
            code.append(NL).append("iconst_1").append(NL);
            curr_stack_value++;
            //maxStackValue();
            code.append(NL).append("ixor").append(NL);
        } else if (unaryOpInstruction.getOperation().getOpType() == GTH) {
            code.append(NL).append("ifgt");
        } else if (unaryOpInstruction.getOperation().getOpType() == LTH) {
            code.append(NL).append("iflt");
        } else if (unaryOpInstruction.getOperation().getOpType() == NEQ) {
            code.append(NL).append("ifne");
        } else if (unaryOpInstruction.getOperation().getOpType() == EQ) {
            code.append(NL).append("ifeq");
        } else if (unaryOpInstruction.getOperation().getOpType() == GTE) {
            code.append(NL).append("ifge");
        } else if (unaryOpInstruction.getOperation().getOpType() == LTE) {
            code.append(NL).append("ifle");
        }
        curr_stack_value--;
        maxStackValue();
        return code.toString();
    }

    private int getOperandName(Operand operand) {
        String name = operand.getName();
        int reg = currentMethod.getVarTable().get(name).getVirtualReg();
        return reg;
    }


    private String generateAritmeticBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();
        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        switch (binaryOp.getOperation().getOpType()) {
            case ADD -> {
                code.append(NL).append("iadd").append(NL);
            }
            case MUL -> code.append(NL).append("imul").append(NL);
            case SUB -> code.append(NL).append("isub").append(NL);
            case DIV -> code.append(NL).append("idiv").append(NL);
            case XOR -> code.append(NL).append("ixor").append(NL);
            case AND, ANDB -> code.append(NL).append("iand").append(NL);
            case OR, ORB -> code.append(NL).append("ior").append(NL);
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        }
        curr_stack_value--;
        maxStackValue();

        return code.toString();
    }

    private String generateConditionalBinaryOp(BinaryOpInstruction binaryOpInstruction) {
        var code = new StringBuilder();

        code.append(generators.apply(binaryOpInstruction.getLeftOperand()));
        code.append(generators.apply(binaryOpInstruction.getRightOperand()));
        var leftOperand = binaryOpInstruction.getLeftOperand();
        var rightOperand = binaryOpInstruction.getRightOperand();
        switch (binaryOpInstruction.getOperation().getOpType()) {
            case GTE -> {
                if ((leftOperand instanceof LiteralElement) && (rightOperand instanceof LiteralElement)) {
                    int compareValue = Integer.parseInt(((LiteralElement) leftOperand).getLiteral()) - Integer.parseInt(((LiteralElement) rightOperand).getLiteral());
                    code.append("isub").append(NL);
                    if (compareValue < 0) {
                        code.append("iflt ");
                    } else {
                        code.append("ifge ");
                    }
                } else if (((leftOperand instanceof Operand) && (rightOperand instanceof Operand))) {
                    code.append("isub").append(NL);
                    code.append("ifge ");
                } else if ((rightOperand instanceof LiteralElement) && ((LiteralElement) rightOperand).getLiteral().equals("0")) {
                    code.append("iflt ");
                } else if ((leftOperand instanceof LiteralElement) && ((LiteralElement) leftOperand).getLiteral().equals("0")) {
                    code.append("ifge ");
                } else {
                    curr_stack_value--;
                    code.append("if_icmpge ");
                }
            }
            case LTH -> {
                var y = leftOperand.toElement();
                var x = rightOperand.toElement();
                if (((leftOperand instanceof LiteralElement) && (rightOperand instanceof LiteralElement))) {
                    int compareValue = Integer.parseInt(((LiteralElement) leftOperand).getLiteral()) - Integer.parseInt(((LiteralElement) rightOperand).getLiteral());
                    code.append("isub").append(NL);
                    if (compareValue < 0) {
                        code.append("iflt ");
                    } else {
                        code.append("ifge ");
                    }
                } else if (((leftOperand instanceof Operand) && (rightOperand instanceof Operand))) {
                    code.append("isub").append(NL);
                    code.append("iflt ");
                } else if ((rightOperand instanceof LiteralElement) && ((LiteralElement) rightOperand).getLiteral().equals("0")) {
                    code.append("iflt ");
                } else if ((leftOperand instanceof LiteralElement) && ((LiteralElement) leftOperand).getLiteral().equals("0")) {
                    code.append("ifge ");
                } else {
                    curr_stack_value--;
                    code.append("if_icmplt ");
                }
            }
            case GTH -> {
                if ((leftOperand instanceof LiteralElement) && (rightOperand instanceof LiteralElement)) {
                    int compareValue = Integer.parseInt(((LiteralElement) leftOperand).getLiteral()) - Integer.parseInt(((LiteralElement) rightOperand).getLiteral());
                    code.append("isub").append(NL);
                    if (compareValue < 0) {
                        code.append("iflt ");
                    } else {
                        code.append("ifge ");
                    }
                } else if ((rightOperand instanceof LiteralElement) && ((LiteralElement) rightOperand).getLiteral().equals("0")) {
                    code.append("iflt ");
                } else if ((leftOperand instanceof LiteralElement) && ((LiteralElement) leftOperand).getLiteral().equals("0")) {
                    code.append("ifge ");
                } else {
                    curr_stack_value--;
                    code.append("if_icmpgt ");
                }
            }
            case EQ -> {
                if ((leftOperand instanceof LiteralElement) && (rightOperand instanceof LiteralElement)) {
                    int compareValue = Integer.parseInt(((LiteralElement) leftOperand).getLiteral()) - Integer.parseInt(((LiteralElement) rightOperand).getLiteral());
                    code.append("isub").append(NL);

                    if (compareValue == 0) {
                        code.append("ifeq ");
                    } else {
                        code.append("ifne ");
                    }
                } else {
                    curr_stack_value--;
                    code.append("if_icmpeq ");
                }
            }
            case NEQ -> {
                if ((leftOperand instanceof LiteralElement) && (rightOperand instanceof LiteralElement)) {
                    int compareValue = Integer.parseInt(((LiteralElement) leftOperand).getLiteral()) - Integer.parseInt(((LiteralElement) rightOperand).getLiteral());
                    code.append("isub").append(NL);

                    if (compareValue == 0) {
                        code.append("ifeq ");
                    } else {
                        code.append("ifne ");
                    }
                } else {
                    curr_stack_value--;
                    code.append("if_icmpne ");
                }
            }
            case LTE -> {
                if ((leftOperand instanceof LiteralElement) && (rightOperand instanceof LiteralElement)) {
                    int compareValue = Integer.parseInt(((LiteralElement) leftOperand).getLiteral()) - Integer.parseInt(((LiteralElement) rightOperand).getLiteral());
                    code.append("isub").append(NL);
                    if (compareValue < 0) {
                        code.append("iflt ");
                    } else {
                        code.append("ifge ");
                    }
                } else {
                    curr_stack_value--;
                    code.append("if_icmple ");
                }
            }
            default -> {
                return "";
            }
        }
        curr_stack_value--;
        maxStackValue();
        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        // generate code for the return value
        if (returnInst.getOperand() != null) {
            code.append(NL).append(generators.apply(returnInst.getOperand())).append(NL);
        }
        ElementType returnType = returnInst.getReturnType().getTypeOfElement();
        switch (returnType) {
            case INT32:
                curr_stack_value--;
                maxStackValue();
                code.append(NL).append("ireturn").append(NL);
                break;
            case BOOLEAN:
                curr_stack_value--;
                maxStackValue();
                code.append(NL).append("ireturn").append(NL);
                break;
            case VOID:
                code.append(NL).append("return").append(NL);
                break;
            case OBJECTREF, STRING, ARRAYREF:
                curr_stack_value--;
                maxStackValue();
                code.append(NL).append("areturn").append(NL);
                break;
            default:
                throw new NotImplementedException("Return type not supported: " + returnType.name());
        }

        return code.toString();
    }

    private String generateBranch(CondBranchInstruction condBranchInstruction) {
        var code = new StringBuilder();

        if (condBranchInstruction.getCondition().getInstType().equals(UNARYOPER)) {
            var aritOp = (UnaryOpInstruction) condBranchInstruction.getCondition();
            var op = generateUnaryOp(aritOp);
            code.append(NL).append(op).append(condBranchInstruction.getLabel()).append(NL);
        } else if (condBranchInstruction.getCondition().getInstType().equals(BINARYOPER)) {
            var binOp = (BinaryOpInstruction) condBranchInstruction.getCondition();
            var op = generateBinaryOp(binOp);
            code.append(NL).append(op).append(" ").append(condBranchInstruction.getLabel()).append(NL);
        } else {
            code.append(generators.apply(condBranchInstruction.getCondition())).append(NL);
            code.append("ifne").append(" ").append(condBranchInstruction.getLabel()).append(NL);
        }
        return code.toString();
    }

    private String generateGoto(GotoInstruction gotoInstruction) {
        var code = new StringBuilder();
        code.append(NL).append("goto ").append(gotoInstruction.getLabel()).append(NL);
        return code.toString();
    }

    public void maxStackValue() {
        this.stack_value = Math.max(this.stack_value, this.curr_stack_value);
    }

}