package ru.ac.uniyar.service;

import ru.ac.uniyar.model.*;
import ru.ac.uniyar.utils.Utils;

import java.util.*;

public class SpanningTreeFinder {
    public BenchmarkResult findMinSpanningTree(BenchmarkTask task) {
        int n = task.getSize();
        int maxLeaves = n / 16;
        Map<Integer, Vertex> vertexes = task.getVertexes();
        List<BenchmarkResult> results = new ArrayList<>();
        for (int k = 1201; k <= task.getSize(); ++k) {
            Map<Integer, Boolean> spanningTree = new HashMap<>();
            Map<Integer, Integer> degrees = new HashMap<>();
            for (int i = 1; i <= n; ++i) {
                spanningTree.put(i, false);
                degrees.put(i, 0);
            }
            PriorityQueue<WeightedEdge> queue = new PriorityQueue<>(Comparator.comparingInt(WeightedEdge::getWeight));
            BenchmarkResult result = new BenchmarkResult();
            result.setEdges(new ArrayList<>());
            spanningTree.put(k, true);
            for (int i = 1; i <= n; ++i) {
                if (i == k) continue;
                int dist = Utils.getDistance(vertexes.get(k), vertexes.get(i));
                queue.offer(new WeightedEdge(new Edge(k, i), dist));
            }

            while (result.getEdges().size() < n - 1 && !queue.isEmpty()) {
                WeightedEdge entry = queue.poll();
                int u = entry.getEdge().getVertex1();
                int v = entry.getEdge().getVertex2();
                int w = entry.getWeight();

                if (spanningTree.get(u) && spanningTree.get(v)) continue;

                Map<Integer, Integer> tempDegrees = (Map<Integer, Integer>) ((HashMap<Integer, Integer>) degrees).clone();
                tempDegrees.put(u, tempDegrees.get(u) + 1);
                tempDegrees.put(v, tempDegrees.get(v) + 1);
                int leafCount = (int) tempDegrees.values().stream().filter(it -> it == 1).count();

                if (leafCount <= maxLeaves) {
                    result.getEdges().add(new Edge(u, v));
                    result.setWeight(result.getWeight() + w);
                    degrees = tempDegrees;
                    if (spanningTree.get(u)) {
                        spanningTree.put(v, true);

                        for (int to = 1; to <= n; ++to) {
                            if (!spanningTree.get(to)) {
                                int dist = Utils.getDistance(vertexes.get(v), vertexes.get(to));
                                queue.offer(new WeightedEdge(new Edge(v, to), dist));
                            }
                        }
                    } else {
                        spanningTree.put(u, true);

                        for (int to = 1; to <= n; ++to) {
                            if (!spanningTree.get(to)) {
                                int dist = Utils.getDistance(vertexes.get(u), vertexes.get(to));
                                queue.offer(new WeightedEdge(new Edge(u, to), dist));
                            }
                        }
                    }

                }
            }

            result.setLeaves((int) degrees.values().stream().filter(it -> it == 1).count());
            results.add(result);
        }

        return results.stream().min(Comparator.comparingInt(BenchmarkResult::getWeight)).orElse(null);
    }
}
