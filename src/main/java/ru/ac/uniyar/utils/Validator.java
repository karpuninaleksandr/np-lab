package ru.ac.uniyar.utils;

import ru.ac.uniyar.model.results.LCMSTResult;
import ru.ac.uniyar.model.Task;
import ru.ac.uniyar.model.Edge;

import java.util.HashMap;
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
}
