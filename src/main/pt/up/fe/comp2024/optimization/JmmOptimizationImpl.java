package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        List methods = ollirResult.getOllirClass().getMethods();

        for (Object method : methods) {
            performRegisterAllocation((org.specs.comp.ollir.Method) method);
        }
        return ollirResult;
    }

    private void performRegisterAllocation(org.specs.comp.ollir.Method method) {
        var livenessAnalysis = new LivenessAnalysis(method);
        var liveRanges = livenessAnalysis.computeLiveRanges();

        var interferenceGraph = new InterferenceGraph(liveRanges);

        var registerAllocator = new RegisterAllocator(interferenceGraph);
        var mapaDosRegistos = registerAllocator.allocateRegisters();

        replaceVariablesWithRegisters(method, mapaDosRegistos);
    }


    private void replaceVariablesWithRegisters(org.specs.comp.ollir.Method method, Map<String, Integer> mapaDosRegistos) {
        var varTable = method.getVarTable();

        for (Map.Entry<String, Integer> entry : mapaDosRegistos.entrySet()) {
            String varName = entry.getKey();
            Integer register = entry.getValue();
            var registerNum = register;
            var numParam = method.getParams().size();
            var thisRegister = 1;

            if (varTable.containsKey(varName)) {
                varTable.get(varName).setVirtualReg(registerNum + numParam + thisRegister);
            }
        }
    }
}
