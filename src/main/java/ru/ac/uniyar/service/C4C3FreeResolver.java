package ru.ac.uniyar.service;

import ru.ac.uniyar.model.Edge;
import ru.ac.uniyar.model.Task;
import ru.ac.uniyar.model.Vertex;
import ru.ac.uniyar.model.results.C4C3FreeResult;
import ru.ac.uniyar.utils.Utils;
import ru.ac.uniyar.utils.Validator;
import ru.ac.uniyar.utils.Writer;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class C4C3FreeResolver {

    public static C4C3FreeResult resolve(Task task) throws InterruptedException, ExecutionException {
        final int NUM_STARTS = 200;

        Map<Integer, Vertex> vertexes = task.getVertexes();
        Map<Long, Integer> distanceCache = new HashMap<>();
        List<Edge> allEdges = generateAllEdges(vertexes, distanceCache);

        allEdges.sort(Comparator.comparingInt(Edge::getWeight).reversed());
        List<Edge> topEdges = new ArrayList<>(allEdges);

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<C4C3FreeResult>> futures = new ArrayList<>();

        for (int startIndex = 0; startIndex < NUM_STARTS; startIndex++) {
            final int seed = startIndex * 997;
            final List<Edge> edgeCopy = new ArrayList<>(topEdges);
            futures.add(executor.submit(() -> runGRASPAttempt(task, edgeCopy, seed)));
        }

        C4C3FreeResult bestOverall = null;
        int bestOverallWeight = -1;

        for (Future<C4C3FreeResult> future : futures) {
            C4C3FreeResult result = future.get();
            if (result.getWeight() > bestOverallWeight) {
                bestOverallWeight = result.getWeight();
                bestOverall = result;
            }
        }
        executor.shutdown();

        return bestOverall;
    }

    private static C4C3FreeResult runGRASPAttempt(Task task, List<Edge> allEdges, int seed) {
        Instant start = Instant.now();
        final int RCL_POOL_SIZE = 20;
        final int RCL_SIZE = 2;
        final int MAX_ITERATIONS = 500000;
        final int TABU_TENURE = 1000;
        final int MAX_NO_IMPROVEMENT = 50;

        Collections.shuffle(allEdges, new Random(seed));

        List<Edge> currentSolution = new ArrayList<>();
        Map<Integer, BitSet> adjacency = initializeAdjacency(task.getVertexes());

        List<Edge> candidateRCL = new ArrayList<>();
        for (Edge edge : allEdges) {
            if (canAddEdge(edge, adjacency)) {
                candidateRCL.add(edge);
                if (candidateRCL.size() >= RCL_POOL_SIZE) break;
            }
        }
        Collections.shuffle(candidateRCL, new Random(seed));
        for (int i = 0; i < RCL_SIZE && i < candidateRCL.size(); i++) {
            Edge chosen = candidateRCL.get(i);
            addEdge(chosen, adjacency);
            currentSolution.add(chosen);
        }

        List<Edge> bestLocalSolution = new ArrayList<>(currentSolution);
        int bestLocalWeight = getTotalWeight(currentSolution);
        Map<String, Integer> tabuList = new HashMap<>();
        int noImprovementCounter = 0;

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            List<Edge> candidateSolution = new ArrayList<>(currentSolution);
            Map<Integer, BitSet> candidateAdj = buildAdjacency(candidateSolution, task.getVertexes());

            candidateSolution.sort(Comparator.comparingInt(Edge::getWeight));
            int toRemove = Math.min(3, candidateSolution.size());
            for (int i = 0; i < toRemove; i++) {
                Edge e = candidateSolution.remove(0);
                removeEdge(e, candidateAdj);
            }

            int additions = 0;
            for (Edge edge : allEdges) {
                if (candidateSolution.contains(edge)) continue;
                String move = edge.getVertex1() + "-" + edge.getVertex2();
                if (tabuList.getOrDefault(move, 0) > iter) continue;
                if (canAddEdge(edge, candidateAdj)) {
                    addEdge(edge, candidateAdj);
                    candidateSolution.add(edge);
                    tabuList.put(move, iter + TABU_TENURE);
                    additions++;
                    if (additions >= 5) break;
                }
            }

            int candidateWeight = getTotalWeight(candidateSolution);
            if (candidateWeight > bestLocalWeight) {
                bestLocalWeight = candidateWeight;
                bestLocalSolution = new ArrayList<>(candidateSolution);
                noImprovementCounter = 0;
            } else {
                noImprovementCounter++;
                if (noImprovementCounter >= MAX_NO_IMPROVEMENT) {
                    break;
                }
            }

            currentSolution = candidateSolution;
            System.out.println("iter: " + iter + "/" + MAX_ITERATIONS + " : " + bestLocalWeight);
        }

        Map<Integer, BitSet> finalAdj = buildAdjacency(bestLocalSolution, task.getVertexes());
        for (Edge edge : allEdges) {
            if (bestLocalSolution.contains(edge)) continue;
            if (canAddEdge(edge, finalAdj)) {
                addEdge(edge, finalAdj);
                bestLocalSolution.add(edge);
                bestLocalWeight += edge.getWeight();
            }
        }

        C4C3FreeResult result = new C4C3FreeResult();
        result.setEdges(bestLocalSolution);
        result.setWeight(bestLocalWeight);
        System.out.println(bestLocalWeight + " " + Duration.between(start, Instant.now()).toMillis());
        Writer.writeBiggestSubGraphResult(result, "src/main/resources/result/biggestsubgraph/2048/4096_%s.txt".formatted(result.getWeight()), task);
        Validator.validateC4C3FreeResult(task, result);
        return result;
    }

    private static List<Edge> generateAllEdges(Map<Integer, Vertex> vertexes, Map<Long, Integer> cache) {
        List<Integer> keys = new ArrayList<>(vertexes.keySet());
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            for (int j = i + 1; j < keys.size(); j++) {
                Vertex v1 = vertexes.get(keys.get(i));
                Vertex v2 = vertexes.get(keys.get(j));
                int id1 = v1.getNumber(), id2 = v2.getNumber();
                long key = Math.min(id1, id2) * 1_000_000L + Math.max(id1, id2);
                int weight = cache.computeIfAbsent(key, k -> Utils.getDistance(v1, v2));
                edges.add(new Edge(id1, id2, weight));
            }
        }
        return edges;
    }

    private static Map<Integer, BitSet> initializeAdjacency(Map<Integer, Vertex> vertexes) {
        Map<Integer, BitSet> adjacency = new HashMap<>();
        for (Vertex v : vertexes.values()) {
            adjacency.put(v.getNumber(), new BitSet());
        }
        return adjacency;
    }

    private static boolean canAddEdge(Edge edge, Map<Integer, BitSet> adj) {
        int u = edge.getVertex1(), v = edge.getVertex2();
        BitSet nu = adj.get(u), nv = adj.get(v);

        BitSet intersection = (BitSet) nu.clone();
        intersection.and(nv);
        if (!intersection.isEmpty()) return false;

        for (int x = nu.nextSetBit(0); x >= 0; x = nu.nextSetBit(x + 1)) {
            BitSet xAdj = adj.get(x);
            if (xAdj == null) continue;
            BitSet temp = (BitSet) xAdj.clone();
            temp.and(nv);
            if (!temp.isEmpty()) return false;
        }

        return true;
    }

    private static void addEdge(Edge e, Map<Integer, BitSet> adj) {
        adj.get(e.getVertex1()).set(e.getVertex2());
        adj.get(e.getVertex2()).set(e.getVertex1());
    }

    private static void removeEdge(Edge e, Map<Integer, BitSet> adj) {
        adj.get(e.getVertex1()).clear(e.getVertex2());
        adj.get(e.getVertex2()).clear(e.getVertex1());
    }

    private static int getTotalWeight(List<Edge> edges) {
        return edges.stream().mapToInt(Edge::getWeight).sum();
    }

    private static Map<Integer, BitSet> buildAdjacency(List<Edge> edges, Map<Integer, Vertex> vertices) {
        Map<Integer, BitSet> adj = initializeAdjacency(vertices);
        for (Edge e : edges) {
            addEdge(e, adj);
        }
        return adj;
    }
}