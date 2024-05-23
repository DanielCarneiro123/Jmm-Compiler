package pt.up.fe.comp2024.optimization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RegisterAllocator {
    private final InterferenceGraph interferenceGraph;

    public RegisterAllocator(InterferenceGraph interferenceGraph) {
        this.interferenceGraph = interferenceGraph;
    }

    public Map<String, Integer> allocateRegisters() {
        Map<String, Set<String>> graph = interferenceGraph.getGraph();

        Map<String, Integer> mapaCores = graficoColorido(graph);

        Map<String, Integer> mapaDosRegistos = new HashMap<>();
        for (Map.Entry<String, Integer> entry : mapaCores.entrySet()) {
            mapaDosRegistos.put(entry.getKey(), entry.getValue());
        }

        return mapaDosRegistos;
    }

    private Map<String, Integer> graficoColorido(Map<String, Set<String>> graph) {
        Set<String> nosColoridos = new HashSet<>();


        Map<String, Integer> mapaCores = new HashMap<>();
        for (String no : graph.keySet()) {
            if (!nosColoridos.contains(no)) {
                Set<Integer> coresVizinhos = new HashSet<>();
                for (String vizinho : graph.get(no)) {
                    if (mapaCores.containsKey(vizinho)) {
                        coresVizinhos.add(mapaCores.get(vizinho));
                    }
                }
                int cor = 0;
                while (coresVizinhos.contains(cor)) {
                    cor++;
                }
                mapaCores.put(no, cor);
                nosColoridos.add(no);
            }
        }
        return mapaCores;
    }


}
