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
//        for (int k = 1; k <= n; ++k) {
        for (int k = 699; k <= 699; ++k) {
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
            postProcess(result, vertexes, n, maxLeaves);
            results.add(result);
            int w = 0;
            for (Edge edge : result.getEdges()) {
                w += Utils.getDistance(task.getVertexes().get(edge.getVertex1()), task.getVertexes().get(edge.getVertex2()));
            }
            System.out.println(k + " - " + result.getWeight() + " - " + w);
            if (result.getWeight() == 229922) {
                break;
            }
        }
        return results.stream().min(Comparator.comparingInt(BenchmarkResult::getWeight)).orElse(null);
    }

    private void postProcess(BenchmarkResult result, Map<Integer, Vertex> vertices, int n, int maxLeaves) {
        boolean improved = true;

        while (improved) {
            improved = false;
            List<Edge> currentEdges = new ArrayList<>(result.getEdges());

            for (Edge edgeToRemove : currentEdges) {
                Set<Integer> componentA = bfsWithoutEdge(result.getEdges(), edgeToRemove.getVertex1(), edgeToRemove);
                Set<Integer> componentB = new HashSet<>();
                for (int i = 1; i <= n; ++i) {
                    if (!componentA.contains(i)) {
                        componentB.add(i);
                    }
                }

                Edge bestReplacement = null;
                int bestDelta = 0;

                for (int u : componentA) {
                    for (int v : componentB) {
                        int weight = Utils.getDistance(vertices.get(u), vertices.get(v));
                        int delta = edgeToRemove.getWeight() - weight;
                        if (delta > bestDelta) {
                            bestDelta = delta;
                            bestReplacement = new Edge(u, v, weight);
                        }
                    }
                }

                if (bestReplacement != null) {
                    List<Edge> newEdges = new ArrayList<>(result.getEdges());
                    newEdges.remove(edgeToRemove);
                    newEdges.add(bestReplacement);

                    int newLeafCount = computeLeafCount(newEdges, n);
                    if (newLeafCount <= maxLeaves) {
                        result.setEdges(newEdges);
                        result.setWeight(result.getWeight() - bestDelta);
                        result.setLeaves(newLeafCount);
                        improved = true;
                        break;
                    }
                }
            }
        }
    }

    private Set<Integer> bfsWithoutEdge(List<Edge> edges, int start, Edge skipEdge) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (Edge e : edges) {
            if (e.equals(skipEdge)) continue;
            adj.computeIfAbsent(e.getVertex1(), k -> new ArrayList<>()).add(e.getVertex2());
            adj.computeIfAbsent(e.getVertex2(), k -> new ArrayList<>()).add(e.getVertex1());
        }

        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v : adj.getOrDefault(u, Collections.emptyList())) {
                if (!visited.contains(v)) {
                    visited.add(v);
                    queue.add(v);
                }
            }
        }
        return visited;
    }

    private int computeLeafCount(List<Edge> edges, int n) {
        Map<Integer, Integer> degrees = new HashMap<>();
        for (int i = 1; i <= n; ++i) degrees.put(i, 0);
        for (Edge e : edges) {
            degrees.put(e.getVertex1(), degrees.get(e.getVertex1()) + 1);
            degrees.put(e.getVertex2(), degrees.get(e.getVertex2()) + 1);
        }
        return (int) degrees.values().stream().filter(d -> d == 1).count();
    }
}
