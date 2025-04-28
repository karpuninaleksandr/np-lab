package ru.ac.uniyar.utils;

import ru.ac.uniyar.model.results.LCMSTResult;
import ru.ac.uniyar.model.Task;
import ru.ac.uniyar.model.Edge;
import ru.ac.uniyar.model.results.VRPResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Validator {
    public static void validateLCMSTResult(Task task, LCMSTResult result) {
        int weight = 0, leaves;
        Map<Integer, Integer> degrees = new HashMap<>();
        for (int i = 1; i <= task.getSize(); ++i) {
            degrees.put(i, 0);
        }
        for (Edge edge : result.getEdges()) {
            weight += Utils.getDistance(task.getVertexes().get(edge.getVertex1()), task.getVertexes().get(edge.getVertex2()));
            degrees.put(edge.getVertex1(), degrees.get(edge.getVertex1()) + 1);
            degrees.put(edge.getVertex2(), degrees.get(edge.getVertex2()) + 1);
        }
        leaves = (int) degrees.values().stream().filter(it -> it == 1).count();
        System.out.println(weight == result.getWeight() && leaves == result.getLeaves() ? "VALID" : "NOT VALID");
    }

    public static void validateVRPResult(Task task, VRPResult result) {
        int totalWeight = 0, maxCycleWeight = 0;
        int n = task.getSize();
        int m = (int) (Math.log(n) / Math.log(2));
        for (List<Integer> way: result.getWays().values()) {
            int currWeight = 0;
            for (int i = 0; i < way.size() - 1; ++i) {
                int u = way.get(i);
                int v = way.get(i + 1);
                currWeight += Utils.getDistance(task.getVertexes().get(u), task.getVertexes().get(v));
            }
            if (currWeight > maxCycleWeight) {
                maxCycleWeight = currWeight;
            }
            totalWeight += currWeight;
        }
        System.out.println(totalWeight == result.getTotalWeight() && m == result.getWays().size() && maxCycleWeight == result.getMaxCycleWeight() ? "VALID" : "NOT VALID");
    }
}
