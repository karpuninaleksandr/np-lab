package ru.ac.uniyar.utils;

import ru.ac.uniyar.model.Vertex;
import ru.ac.uniyar.model.results.C4C3FreeResult;
import ru.ac.uniyar.model.results.LCMSTResult;
import ru.ac.uniyar.model.Task;
import ru.ac.uniyar.model.Edge;
import ru.ac.uniyar.model.results.VRPResult;

import java.util.*;

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

    public static void validateC4C3FreeResult(Task task, C4C3FreeResult result) {
        Map<Integer, Vertex> vertexes = task.getVertexes();
        Map<Integer, Set<Integer>> adjacency = new HashMap<>();
        int computedWeight = 0;

        for (int i = 1; i <= task.getSize(); ++i) {
            adjacency.put(i, new HashSet<>());
        }

        for (Edge edge : result.getEdges()) {
            int u = edge.getVertex1();
            int v = edge.getVertex2();

            if (!vertexes.containsKey(u) || !vertexes.containsKey(v)) {
                System.out.println("NOT VALID (invalid vertex)");
                return;
            }

            if (adjacency.get(u).contains(v)) {
                System.out.println("NOT VALID (duplicate edge)");
                return;
            }

            adjacency.get(u).add(v);
            adjacency.get(v).add(u);

            computedWeight += Utils.getDistance(vertexes.get(u), vertexes.get(v));
        }

        for (int u : adjacency.keySet()) {
            for (int v : adjacency.get(u)) {
                if (v <= u) continue;
                for (int w : adjacency.get(v)) {
                    if (w <= v || w == u) continue;
                    if (adjacency.get(w).contains(u)) {
                        System.out.println("NOT VALID (triangle found)");
                        return;
                    }
                }
            }
        }

        for (int u : adjacency.keySet()) {
            for (int v : adjacency.get(u)) {
                if (v <= u) continue;
                for (int x : adjacency.get(u)) {
                    if (x == v || x <= u) continue;
                    for (int y : adjacency.get(v)) {
                        if (y == u || y == x || y <= v) continue;
                        if (adjacency.get(x).contains(y)) {
                            System.out.println("NOT VALID (square found)");
                            return;
                        }
                    }
                }
            }
        }

        if (computedWeight != result.getWeight()) {
            System.out.println("NOT VALID (weight mismatch)");
            return;
        }

        System.out.println("VALID");
    }
}
