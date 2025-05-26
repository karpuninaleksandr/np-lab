package ru.ac.uniyar.service;

import ru.ac.uniyar.model.Edge;
import ru.ac.uniyar.model.Task;
import ru.ac.uniyar.model.Vertex;
import ru.ac.uniyar.model.results.C4C3FreeResult;
import ru.ac.uniyar.utils.Utils;
import ru.ac.uniyar.utils.Writer;

import java.util.*;
import java.util.concurrent.*;

public class C4C3FreeResolver {

    public static C4C3FreeResult resolve(Task task) throws InterruptedException, ExecutionException {
        final int NUM_STARTS = 10;

        Map<Integer, Vertex> vertexes = task.getVertexes();
        List<Edge> allEdges = generateAllEdges(vertexes);

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<C4C3FreeResult>> futures = new ArrayList<>();

        for (int startIndex = 0; startIndex < NUM_STARTS; startIndex++) {
            final int seed = startIndex * 997;
            futures.add(executor.submit(() -> runGRASPAttempt(task, allEdges, seed)));
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
        final int RCL_SIZE = 2;
        final int MAX_ITERATIONS = 1000;
        final int TABU_TENURE = 1000;

        List<Edge> shuffledEdges = new ArrayList<>(allEdges);
        Collections.shuffle(shuffledEdges, new Random(seed));

        List<Edge> currentSolution = new ArrayList<>();
        Map<Integer, Set<Integer>> adjacency = initializeAdjacency(task.getVertexes());

        Random random = new Random(seed);
        while (true) {
            List<Edge> rcl = new ArrayList<>();
            for (Edge edge : shuffledEdges) {
                if (canAddEdge(edge, adjacency)) {
                    rcl.add(edge);
                    if (rcl.size() >= RCL_SIZE) break;
                }
            }
            if (rcl.isEmpty()) break;
            Edge chosen = rcl.get(random.nextInt(rcl.size()));
            addEdge(chosen, adjacency);
            currentSolution.add(chosen);
        }

        List<Edge> bestLocalSolution = new ArrayList<>(currentSolution);
        int bestLocalWeight = getTotalWeight(currentSolution);

        Map<String, Integer> tabuList = new HashMap<>();

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            List<Edge> candidateSolution = new ArrayList<>(currentSolution);
            Map<Integer, Set<Integer>> candidateAdj = buildAdjacency(candidateSolution, task.getVertexes());

            Edge worstEdge = getLightestEdge(candidateSolution);
            if (worstEdge != null) {
                candidateSolution.remove(worstEdge);
                removeEdge(worstEdge, candidateAdj);
            }

            Edge bestCandidate = null;
            for (Edge edge : shuffledEdges) {
                if (candidateSolution.contains(edge)) continue;
                String move = edge.getVertex1() + "-" + edge.getVertex2();
                if (tabuList.getOrDefault(move, 0) > iter) continue;
                if (canAddEdge(edge, candidateAdj)) {
                    bestCandidate = edge;
                    break;
                }
            }

            if (bestCandidate != null) {
                addEdge(bestCandidate, candidateAdj);
                candidateSolution.add(bestCandidate);
                tabuList.put(bestCandidate.getVertex1() + "-" + bestCandidate.getVertex2(), iter + TABU_TENURE);
            }

            int candidateWeight = getTotalWeight(candidateSolution);
            if (candidateWeight > bestLocalWeight) {
                bestLocalWeight = candidateWeight;
                bestLocalSolution = new ArrayList<>(candidateSolution);
            }

            currentSolution = candidateSolution;
            System.out.println(bestLocalWeight);
        }

        C4C3FreeResult result = new C4C3FreeResult();
        result.setEdges(bestLocalSolution);
        result.setWeight(bestLocalWeight);
        System.out.println(bestLocalWeight);
//        Writer.writeBiggestSubGraphResult(result, "src/main/resources/result/biggestsubgraph/try_%s/Karpunin_%s_%s_%s.txt"
//                .formatted(1, 4096, result.getWeight(), 1), task);
        return result;
    }

    private static List<Edge> generateAllEdges(Map<Integer, Vertex> vertexes) {
        List<Integer> keys = new ArrayList<>(vertexes.keySet());
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            for (int j = i + 1; j < keys.size(); j++) {
                Vertex v1 = vertexes.get(keys.get(i));
                Vertex v2 = vertexes.get(keys.get(j));
                int weight = Utils.getDistance(v1, v2);
                edges.add(new Edge(v1.getNumber(), v2.getNumber(), weight));
            }
        }
        return edges;
    }

    private static Map<Integer, Set<Integer>> initializeAdjacency(Map<Integer, Vertex> vertexes) {
        Map<Integer, Set<Integer>> adjacency = new HashMap<>();
        for (Vertex v : vertexes.values()) {
            adjacency.put(v.getNumber(), new HashSet<>());
        }
        return adjacency;
    }

    private static boolean canAddEdge(Edge edge, Map<Integer, Set<Integer>> adj) {
        int u = edge.getVertex1(), v = edge.getVertex2();
        Set<Integer> nu = adj.get(u), nv = adj.get(v);
        Set<Integer> intersection = new HashSet<>(nu);
        intersection.retainAll(nv);
        if (!intersection.isEmpty()) return false;
        for (int x : nu) {
            for (int y : nv) {
                if (adj.get(x).contains(y)) return false;
            }
        }
        return true;
    }

    private static void addEdge(Edge e, Map<Integer, Set<Integer>> adj) {
        adj.get(e.getVertex1()).add(e.getVertex2());
        adj.get(e.getVertex2()).add(e.getVertex1());
    }

    private static void removeEdge(Edge e, Map<Integer, Set<Integer>> adj) {
        adj.get(e.getVertex1()).remove(e.getVertex2());
        adj.get(e.getVertex2()).remove(e.getVertex1());
    }

    private static int getTotalWeight(List<Edge> edges) {
        return edges.stream().mapToInt(Edge::getWeight).sum();
    }

    private static Edge getLightestEdge(List<Edge> edges) {
        return edges.stream().min(Comparator.comparingInt(Edge::getWeight)).orElse(null);
    }

    private static Map<Integer, Set<Integer>> buildAdjacency(List<Edge> edges, Map<Integer, Vertex> vertices) {
        Map<Integer, Set<Integer>> adj = initializeAdjacency(vertices);
        for (Edge e : edges) {
            addEdge(e, adj);
        }
        return adj;
    }
}