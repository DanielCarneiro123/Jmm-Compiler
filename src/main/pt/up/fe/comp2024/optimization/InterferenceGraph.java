package pt.up.fe.comp2024.optimization;

import org.antlr.v4.runtime.misc.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InterferenceGraph {
    private final Map<String, Set<String>> graph;

    public InterferenceGraph(Map<String, Pair<Integer, Integer>> liveRanges) {
        this.graph = new HashMap<>();
        buildGraph(liveRanges);
    }

    private void buildGraph(Map<String, Pair<Integer, Integer>> liveRanges) {
        for (String variab : liveRanges.keySet()) {
            graph.put(variab, new HashSet<>());
        }

        for (Map.Entry<String, Pair<Integer, Integer>> entry : liveRanges.entrySet()) {
            String variab = entry.getKey();
            Pair<Integer, Integer> range = entry.getValue();

            for (Map.Entry<String, Pair<Integer, Integer>> outraEntry : liveRanges.entrySet()) {
                String outraVariab = outraEntry.getKey();
                Pair<Integer, Integer> outraRange = outraEntry.getValue();

                if (variab.equals(outraVariab)) continue;
                if (range.b >= outraRange.a && range.a <= outraRange.b) {
                    graph.get(variab).add(outraVariab);
                    graph.get(outraVariab).add(variab);
                }
            }
        }
    }

    public Map<String, Set<String>> getGraph() {
        return graph;
    }
}


