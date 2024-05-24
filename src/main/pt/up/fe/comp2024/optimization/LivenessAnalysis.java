package pt.up.fe.comp2024.optimization;

import org.antlr.v4.runtime.misc.Pair;
import org.specs.comp.ollir.*;

import java.util.*;

public class LivenessAnalysis {
    private final Method method;
    private final Map<Object, Set<String>> liveIn;
    private final Map<Object, Set<String>> liveOut;

    public LivenessAnalysis(Method method) {
        this.method = method;
        this.liveIn = new HashMap<>();
        this.liveOut = new HashMap<>();
        computeLiveness();
    }

    private void computeLiveness() {
        initializeLivenessSets();
        boolean changesMade;

        do {
            changesMade = false;
            if (method.getInstructions().size() > 1) {
                for (int i = method.getInstructions().size() - 1; i >= 0; i--) {
                    Instruction inst = method.getInstructions().get(i);
                    Set<String> oldLiveOut = new HashSet<>(liveOut.get(inst));
                    Set<String> oldLiveIn = new HashSet<>(liveIn.get(inst));

                    Set<String> newLiveOut = computeLiveOut(inst);
                    Set<String> newLiveIn = computeLiveIn(inst, newLiveOut);

                    liveOut.put(inst, newLiveOut);
                    liveIn.put(inst, newLiveIn);

                    if (!oldLiveIn.equals(newLiveIn) || !oldLiveOut.equals(newLiveOut)) {
                        changesMade = true;
                    }
                }
            }
        } while (changesMade);
    }

    private void initializeLivenessSets() {
        for (Instruction inst : method.getInstructions()) {
            liveIn.put(inst, new HashSet<>());
            liveOut.put(inst, new HashSet<>());
        }
    }

    private Set<String> computeLiveOut(Instruction inst) {
        Set<String> newLiveOut = new HashSet<>();
        for (Node succ : inst.getSuccessors()) {
            newLiveOut.addAll(liveIn.get(succ));
        }
        return newLiveOut;
    }

    private Set<String> computeLiveIn(Instruction inst, Set<String> liveOut) {
        Set<String> newLiveIn = new HashSet<>(liveOut);
        newLiveIn.removeAll(getDef(inst));
        newLiveIn.addAll(getUse(inst));
        return newLiveIn;
    }


    public Map<Object, Set<String>> getLiveIn() {
        return liveIn;
    }

    public Map<Object, Set<String>> getLiveOut() {
        return liveOut;
    }

    private Set<String> getUse(Instruction inst) {

        var instType = inst.getInstType().name();

        Set<String> uses = new HashSet<>();

        switch (instType) {
            case "ASSIGN" -> {
                var useAssignInst = getUse(((AssignInstruction) inst).getRhs());
                uses.addAll(useAssignInst);
            }
            case "CALL" -> {
                CallInstruction callInst = (CallInstruction) inst;
                var callType = callInst.getInvocationType();
                var callInstFirstArg = callInst.getArguments().get(0);
                if (callType != CallType.invokestatic && callInstFirstArg instanceof Operand) {
                    var callInstFirstArgName = ((Operand) callInstFirstArg).getName();
                    uses.add(callInstFirstArgName);
                }
                List<Element> operandsList = callInst.getOperands();
                for (Element operand : operandsList) {
                    if (operand instanceof Operand) {
                        var opName = ((Operand) operand).getName();
                        uses.add(opName);
                    }
                }
            }
            case "BINARY_OP" -> {
                BinaryOpInstruction binOpInst = (BinaryOpInstruction) inst;
                var binOpInstLeftOperand = binOpInst.getLeftOperand();
                if (binOpInstLeftOperand instanceof Operand) {
                    var binOpInstLeftOperandName = ((Operand) binOpInstLeftOperand).getName();
                    uses.add(binOpInstLeftOperandName);
                }
                var binOpInstRightOperand = binOpInst.getRightOperand();
                if (binOpInstRightOperand instanceof Operand) {
                    var binOpInstRightOperandName = ((Operand) binOpInstRightOperand).getName();
                    uses.add(binOpInstRightOperandName);
                }
            }
            case "OP_COND" -> {
                var operandsList = ((OpCondInstruction) inst).getOperands();
                for (Element elem : operandsList)
                    if (elem instanceof Operand) {
                        var elemName = ((Operand) elem).getName();
                        uses.add(elemName);
                    }
            }
            case "PUT_FIELD" -> {
                PutFieldInstruction put = (PutFieldInstruction) inst;
                var putFirstOperand = put.getOperands().get(0);
                var putThirdOperand = put.getOperands().get(2);
                if (putFirstOperand instanceof Operand) {
                    var putFirstOperandName = ((Operand) putFirstOperand).getName();
                    uses.add(putFirstOperandName);
                }
                if (putThirdOperand instanceof Operand) {
                    var putThirdOperandName = ((Operand) putThirdOperand).getName();
                    uses.add(putThirdOperandName);
                }
            }
            case "NOPER" -> {
                var singleOp = ((SingleOpInstruction) inst).getSingleOperand();
                if (singleOp instanceof Operand) {
                    var singleOpName = ((Operand) singleOp).getName();
                    uses.add(singleOpName);
                }
            }
            case "RETURN" -> {
                var retInstOP = ((ReturnInstruction) inst).getOperand();
                if (retInstOP instanceof Operand) {
                    var retInstOPName = ((Operand) retInstOP).getName();
                    uses.add(retInstOPName);
                }
            }
            case "BRANCH" -> {
                var branchInstOP = ((CondBranchInstruction) inst).getOperands();
                for (Element elem : branchInstOP)
                    if (elem instanceof Operand) {
                        var elemName = ((Operand) elem).getName();
                        uses.add(elemName);
                    }
            }

            case "UNARYOPER" -> {
                var unaryOpInst = ((UnaryOpInstruction) inst).getOperand();
                if (unaryOpInst instanceof Operand) {
                    var unaryOpInstName = ((Operand) unaryOpInst).getName();
                    uses.add(unaryOpInstName);
                }
            }

            case "BINARYOPER" -> {
                var BinaryOpInstructionOps = ((BinaryOpInstruction) inst).getOperands();
                if (BinaryOpInstructionOps != null) {
                    for (Element elem : BinaryOpInstructionOps) {
                        if (elem instanceof Operand) {
                            var elemName = ((Operand) elem).getName();
                            uses.add(elemName);
                        }
                    }
                }
            }

            case "GETFIELD" -> {
                var firstOp = ((GetFieldInstruction) inst).getOperands().get(0);
                if (firstOp instanceof Operand) {
                    var firstOpName = ((Operand) firstOp).getName();
                    uses.add(firstOpName);
                }
                var secondOp = ((GetFieldInstruction) inst).getOperands().get(1);
                if (secondOp instanceof Operand) {
                    var secondOpName = ((Operand) secondOp).getName();
                    uses.add(secondOpName);
                }

            }

            default -> {
                throw new IllegalArgumentException("Unexpected instruction type: " + instType);
            }
        }
        return uses;
    }


    private Set<String> getDef(Instruction inst) {
        Set<String> defs = new HashSet<>();
        if (inst instanceof AssignInstruction) {
            var instDest = ((AssignInstruction) inst).getDest();
            if (instDest instanceof Operand) {
                var instDestName = ((Operand) instDest).getName();
                defs.add(instDestName);
            }
        }
        return defs;
    }


    public Map<String, Pair<Integer, Integer>> computeLiveRanges() {
        Map<String, Pair<Integer, Integer>> liveRanges = new HashMap<>();

        Map<String, Set<String>> interferenceGraph = new HashMap<>();

        for (int i = 0; i < method.getInstructions().size(); i++) {
            Instruction inst = method.getInstructions().get(i);

            Set<String> defs = getDef(inst);
            Set<String> outs = getLiveOut().get(inst);
            Set<String> ins = getLiveIn().get(inst);

            // Union of defs and outs
            Set<String> defOutUnion = new HashSet<>(defs);
            defOutUnion.addAll(outs);

            if (defOutUnion.size() >= 2) {
                for (String var : defOutUnion) {
                    interferenceGraph.putIfAbsent(var, new HashSet<>());
                    interferenceGraph.get(var).addAll(defOutUnion);
                    interferenceGraph.get(var).remove(var);
                }
            }

            if (ins.size() >= 2) {
                for (String var : ins) {
                    interferenceGraph.putIfAbsent(var, new HashSet<>());
                    interferenceGraph.get(var).addAll(ins);
                    interferenceGraph.get(var).remove(var);
                }
            }

            for (String var : defs) {
                if (!liveRanges.containsKey(var)) {
                    liveRanges.put(var, new Pair<>(i, i));
                } else {
                    Pair<Integer, Integer> range = liveRanges.get(var);
                    liveRanges.put(var, new Pair<>(range.a, i));
                }
            }
        }

        System.out.println("Interference Graph:");
        for (Map.Entry<String, Set<String>> entry : interferenceGraph.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }

        return liveRanges;
    }


}
