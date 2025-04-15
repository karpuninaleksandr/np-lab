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
        for (int k = 1; k <= task.getSize(); ++k) {
            Map<Integer, Boolean> spanningTree = new HashMap<>();
            Map<Integer, Integer> degrees = new HashMap<>();
            for (int i = 1; i <= n; ++i) {
                spanningTree.put(i, false);
                degrees.put(i, 0);
            }
            PriorityQueue<Edge> queue = new PriorityQueue<>(Comparator.comparingInt(Edge::getWeight));
            BenchmarkResult result = new BenchmarkResult();
            result.setEdges(new ArrayList<>());
            spanningTree.put(k, true);
            for (int i = 1; i <= n; ++i) {
                if (i == k) continue;
                queue.offer(new Edge(k, i, Utils.getDistance(vertexes.get(k), vertexes.get(i))));
            }

            while (result.getEdges().size() < n - 1 && !queue.isEmpty()) {
                Edge edge = queue.poll();
                int u = edge.getVertex1();
                int v = edge.getVertex2();
                int w = edge.getWeight();

                if (spanningTree.get(u) && spanningTree.get(v)) continue;

                Map<Integer, Integer> tempDegrees = (Map<Integer, Integer>) ((HashMap<Integer, Integer>) degrees).clone();
                tempDegrees.put(u, tempDegrees.get(u) + 1);
                tempDegrees.put(v, tempDegrees.get(v) + 1);
                int leafCount = (int) tempDegrees.values().stream().filter(it -> it == 1).count();

                if (leafCount <= maxLeaves) {
                    result.getEdges().add(new Edge(u, v, w));
                    result.setWeight(result.getWeight() + w);
                    degrees = tempDegrees;
                    if (spanningTree.get(u)) {
                        spanningTree.put(v, true);

                        for (int to = 1; to <= n; ++to) {
                            if (!spanningTree.get(to)) {
                                queue.offer(new Edge(v, to, Utils.getDistance(vertexes.get(v), vertexes.get(to))));
                            }
                        }
                    } else {
                        spanningTree.put(u, true);
                        for (int to = 1; to <= n; ++to) {
                            if (!spanningTree.get(to)) {
                                queue.offer(new Edge(u, to, Utils.getDistance(vertexes.get(u), vertexes.get(to))));
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
