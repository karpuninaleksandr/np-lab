package ru.ac.uniyar.service;

import ru.ac.uniyar.model.Task;
import ru.ac.uniyar.model.results.VRPResult;
import ru.ac.uniyar.utils.Utils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VRPResolver {

    public static VRPResult getAnswer(Task task) {
        int n = task.getSize();
        int m = (int) (Math.log(n) / Math.log(2));

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<CompletableFuture<VRPResult>> futures = new ArrayList<>();

        for (int depot = 1; depot <= n; ++depot) {
            final int currentDepot = depot;
            futures.add(CompletableFuture.supplyAsync(() -> computeResultForDepot(task, currentDepot, m), executor));
        }

        List<VRPResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        executor.shutdown();

        return results.stream()
                .min(Comparator
                        .comparingInt(VRPResult::getMaxCycleWeight)
                        .thenComparingInt(VRPResult::getTotalWeight))
                .orElse(null);
    }

    private static VRPResult computeResultForDepot(Task task, int depot, int m) {
        int[][] dist = new int[task.getSize() + 1][task.getSize() + 1];
        for (int i = 1; i <= task.getSize(); i++) {
            for (int j = 1; j <= task.getSize(); j++) {
                dist[i][j] = Utils.getDistance(task.getVertexes().get(i), task.getVertexes().get(j));
            }
        }

        List<Integer> otherVertices = new ArrayList<>();
        for (int i = 1; i <= task.getSize(); ++i) {
            if (i != depot) {
                otherVertices.add(i);
            }
        }

        List<List<Integer>> clusters = cluster(otherVertices, m, task, depot);

        Map<Integer, List<Integer>> ways = new HashMap<>();
        int maxWayLength = 0;
        int totalLength = 0;
        int vehicleId = 1;
        for (List<Integer> cluster : clusters) {
            List<Integer> route = buildRoute(cluster, depot, dist);
            ways.put(vehicleId++, route);

            int length = calculateRouteLength(route, dist);
            maxWayLength = Math.max(maxWayLength, length);
            totalLength += length;
        }

        boolean improved = true;
        while (improved) {
            improved = false;
            List<Integer> keys = new ArrayList<>(ways.keySet());

            for (int i = 0; i < keys.size(); i++) {
                for (int j = i + 1; j < keys.size(); j++) {
                    List<Integer> routeA = new ArrayList<>(ways.get(keys.get(i)));
                    List<Integer> routeB = new ArrayList<>(ways.get(keys.get(j)));

                    for (int a = 1; a < routeA.size() - 1; a++) {
                        for (int b = 1; b < routeB.size() - 1; b++) {
                            int tempA = routeA.get(a);
                            int tempB = routeB.get(b);

                            routeA.set(a, tempB);
                            routeB.set(b, tempA);

                            Map<Integer, List<Integer>> tempWays = new HashMap<>(ways);
                            tempWays.put(keys.get(i), routeA);
                            tempWays.put(keys.get(j), routeB);

                            int newMax = 0;
                            for (List<Integer> r : tempWays.values()) {
                                int len = calculateRouteLength(r, dist);
                                newMax = Math.max(newMax, len);
                            }

                            if (newMax < maxWayLength) {
                                ways.put(keys.get(i), routeA);
                                ways.put(keys.get(j), routeB);

                                maxWayLength = 0;
                                totalLength = 0;
                                for (List<Integer> r : ways.values()) {
                                    int len = calculateRouteLength(r, dist);
                                    totalLength += len;
                                    maxWayLength = Math.max(maxWayLength, len);
                                }

                                improved = true;
                                break;
                            } else {
                                routeA.set(a, tempA);
                                routeB.set(b, tempB);
                            }
                        }
                        if (improved) break;
                    }
                    if (improved) break;
                }
            }
        }

        // Final 2-opt improvement after exchanges
        for (Map.Entry<Integer, List<Integer>> entry : ways.entrySet()) {
            entry.setValue(twoOpt(entry.getValue(), dist));
        }

        // Recompute lengths
        maxWayLength = 0;
        totalLength = 0;
        for (List<Integer> r : ways.values()) {
            int len = calculateRouteLength(r, dist);
            totalLength += len;
            maxWayLength = Math.max(maxWayLength, len);
        }

        VRPResult result = new VRPResult();
        result.setDepot(depot);
        result.setWays(ways);
        result.setMaxCycleWeight(maxWayLength);
        result.setTotalWeight(totalLength);

        System.out.println(depot + " " + maxWayLength + " " + totalLength);
        return result;
    }

    private static List<List<Integer>> cluster(List<Integer> vertices, int m, Task task, int depot) {
        List<List<Integer>> clusters = new ArrayList<>();
        for (int i = 0; i < m; ++i) {
            clusters.add(new ArrayList<>());
        }

        vertices.sort(Comparator.comparingInt(v -> Utils.getDistance(task.getVertexes().get(depot), task.getVertexes().get(v))));
        List<Integer> centers = new ArrayList<>(vertices.subList(0, m));
        Set<Integer> assigned = new HashSet<>(centers);

        for (int i = 0; i < m; ++i) {
            clusters.get(i).add(centers.get(i));
        }

        for (int v : vertices) {
            if (assigned.contains(v)) continue;

            int bestCluster = 0;
            int minDistance = Integer.MAX_VALUE;
            for (int i = 0; i < m; ++i) {
                for (int c : clusters.get(i)) {
                    int dist = Utils.getDistance(task.getVertexes().get(v), task.getVertexes().get(c));
                    if (dist < minDistance) {
                        minDistance = dist;
                        bestCluster = i;
                    }
                }
            }
            clusters.get(bestCluster).add(v);
            assigned.add(v);
        }

        return clusters;
    }

    private static List<Integer> buildRoute(List<Integer> cluster, int depot, int[][] dist) {
        List<Integer> route = new ArrayList<>();
        route.add(depot);

        Set<Integer> unvisited = new HashSet<>(cluster);
        int current = depot;

        while (!unvisited.isEmpty()) {
            int next = -1;
            int minDist = Integer.MAX_VALUE;
            for (int v : unvisited) {
                if (dist[current][v] < minDist) {
                    minDist = dist[current][v];
                    next = v;
                }
            }
            route.add(next);
            unvisited.remove(next);
            current = next;
        }

        route.add(depot);
        return route;
    }

    private static int calculateRouteLength(List<Integer> route, int[][] dist) {
        int length = 0;
        for (int i = 0; i < route.size() - 1; ++i) {
            length += dist[route.get(i)][route.get(i + 1)];
        }
        return length;
    }

    private static List<Integer> twoOpt(List<Integer> route, int[][] dist) {
        boolean improvement = true;
        int size = route.size();

        while (improvement) {
            improvement = false;
            for (int i = 1; i < size - 2; i++) {
                for (int j = i + 1; j < size - 1; j++) {
                    int delta = dist[route.get(i - 1)][route.get(j)] +
                            dist[route.get(i)][route.get(j + 1)] -
                            dist[route.get(i - 1)][route.get(i)] -
                            dist[route.get(j)][route.get(j + 1)];

                    if (delta < 0) {
                        Collections.reverse(route.subList(i, j + 1));
                        improvement = true;
                    }
                }
            }
        }
        return route;
    }
}
