package ru.ac.uniyar.service;

import ru.ac.uniyar.model.*;
import ru.ac.uniyar.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.*;

public class SpanningTreeFinder {
    public Result findMinSpanningTree(Task task) {
        int n = task.getSize();
        int maxLeaves = n / 16;
        Map<Integer, Map<Integer, Integer>> weights = new HashMap<>();

        for (int i = 1; i <= n; ++i) {
            Map<Integer, Integer> currWeights = new HashMap<>();
            for (int it = 1; it <= n; ++it) {
                if (i == it) continue;
                currWeights.put(it, Utils.getDistance(task.getVertexes().get(i), task.getVertexes().get(it)));
            }
            Map<Integer, Integer> sortedWeights = currWeights.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue,
                            LinkedHashMap::new));
            sortedWeights.putAll(currWeights);
            weights.put(i, sortedWeights);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Callable<Result>> tasks = new ArrayList<>();

        for (int k = 1; k <= n; ++k) {
            int finalK = k;
            tasks.add(() -> checkVertex(finalK, n, maxLeaves, weights));
        }

        List<Result> results = new ArrayList<>();
        try {
            for (Future<Result> future : executorService.invokeAll(tasks)) {
                results.add(future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }

        return results.stream()
                .min(Comparator.comparingInt(Result::getWeight))
                .orElse(null);
    }

    private Result checkVertex(int k, int n, int maxLeaves, Map<Integer, Map<Integer, Integer>> weights) {
        Map<Integer, Boolean> spanningTree = new HashMap<>();
        Map<Integer, Integer> degrees = new HashMap<>();
        for (int i = 1; i <= n; ++i) {
            spanningTree.put(i, false);
            degrees.put(i, 0);
        }

        PriorityQueue<Edge> queue = new PriorityQueue<>(Comparator.comparingInt(Edge::getWeight));
        Result result = new Result();
        result.setEdges(new ArrayList<>());

        spanningTree.put(k, true);
        for (int i = 1; i <= n; ++i) {
            if (i != k) {
                queue.offer(new Edge(k, i, weights.get(k).get(i)));
            }
        }

        while (result.getEdges().size() < n - 1 && !queue.isEmpty()) {
            Edge edge = queue.poll();
            int u = edge.getVertex1();
            int v = edge.getVertex2();

            if (spanningTree.get(u) && spanningTree.get(v)) continue;

            Map<Integer, Integer> tempDegrees = new HashMap<>(degrees);
            tempDegrees.put(u, tempDegrees.get(u) + 1);
            tempDegrees.put(v, tempDegrees.get(v) + 1);

            int leafCount = (int) tempDegrees.values().stream().filter(d -> d == 1).count();

            if (leafCount <= maxLeaves) {
                result.getEdges().add(edge);
                degrees = tempDegrees;

                int newNode = spanningTree.get(u) ? v : u;
                spanningTree.put(newNode, true);

                for (int i = 1; i <= n; ++i) {
                    if (!spanningTree.get(i)) {
                        queue.offer(new Edge(newNode, i, weights.get(newNode).get(i)));
                    }
                }
            }
        }

        result.setWeight(result.getEdges().stream().mapToInt(Edge::getWeight).sum());
        result.setLeaves((int) degrees.values().stream().filter(it -> it == 1).count());
        localEdgeReplacementOptimization(result, n, maxLeaves, weights);
        System.out.println(k + " - " + result.getWeight());

        return result;
    }

    private void localEdgeReplacementOptimization(Result result, int n, int maxLeaves, Map<Integer, Map<Integer, Integer>> weights) {
        boolean improved = true;
        int count = 0;
        while (improved) {
            improved = false;
            ++count;
            System.out.println(count + " : " + result.getWeight());
            List<Edge> edges = new ArrayList<>(result.getEdges());
            for (Edge toRemove : edges) {
                Set<Integer> componentA = bfsWithoutEdge(edges, toRemove.getVertex1(), toRemove);
                Set<Integer> componentB = new HashSet<>();
                for (int i = 1; i <= n; ++i) {
                    if (!componentA.contains(i)) {
                        componentB.add(i);
                    }
                }

                for (int u : componentA) {
                    for (int v : componentB) {
                        if (u == v) continue;
                        int weight = weights.get(u).get(v);
                        if (weight >= toRemove.getWeight()) continue;

                        List<Edge> newEdges = new ArrayList<>(edges);
                        newEdges.remove(toRemove);
                        newEdges.add(new Edge(u, v, weight));
                        int newLeafCount = computeLeafCount(newEdges, n);
                        if (newLeafCount <= maxLeaves) {
                            result.setEdges(newEdges);
                            result.setWeight(newEdges.stream().mapToInt(Edge::getWeight).sum());
                            result.setLeaves(newLeafCount);
                            improved = true;
                            break;
                        }
                    }
                    if (improved) break;
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
