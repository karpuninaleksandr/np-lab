package ru.ac.uniyar.service;

import ru.ac.uniyar.model.Task;
import ru.ac.uniyar.model.results.VRPResult;
import ru.ac.uniyar.utils.Utils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class VRPResolver {

    private static final int MAX_OPT_ITER = 500;
    private static final int NUM_TRIALS = 100;

    public static VRPResult getAnswer(Task task) {
        int n = task.getSize();
        int m = (int) (Math.log(n) / Math.log(2));
        int k = Math.min(30, n);

        int[][] dist = new int[n + 1][n + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                dist[i][j] = Utils.getDistance(task.getVertexes().get(i), task.getVertexes().get(j));
            }
        }

        List<Integer> candidates = new ArrayList<>();
        for (int i = 1; i <= n; i++) candidates.add(i);

        candidates.sort(Comparator.comparingInt(i ->
                Arrays.stream(dist[i]).sum()));

        List<Integer> bestDepots = candidates.subList(0, k);

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<CompletableFuture<VRPResult>> futures = new ArrayList<>();

        for (int depot : bestDepots) {
            final int currentDepot = depot;
            futures.add(CompletableFuture.supplyAsync(() -> computeBestForDepot(task, currentDepot, m, dist), executor));
        }

        List<VRPResult> results = futures.stream().map(CompletableFuture::join).toList();
        executor.shutdown();

        return results.stream()
                .min(Comparator.comparingInt(VRPResult::getMaxCycleWeight).thenComparingInt(VRPResult::getTotalWeight))
                .orElse(null);
    }

    private static VRPResult computeBestForDepot(Task task, int depot, int m, int[][] dist) {
        VRPResult best = null;
        for (int t = 0; t < NUM_TRIALS; ++t) {
            VRPResult result = computeResultForDepot(task, depot, m, dist);
            if (best == null || result.getMaxCycleWeight() < best.getMaxCycleWeight() ||
                    (result.getMaxCycleWeight() == best.getMaxCycleWeight() && result.getTotalWeight() < best.getTotalWeight())) {
                best = result;
            }
        }
        return best;
    }

    private static VRPResult computeResultForDepot(Task task, int depot, int m, int[][] dist) {
        Instant start = Instant.now();
        List<Integer> otherVertices = new ArrayList<>();
        for (int i = 1; i <= task.getSize(); ++i) if (i != depot) otherVertices.add(i);

        List<List<Integer>> clusters = clusterSmart(otherVertices, m, depot, dist);
        balanceClusters(clusters, dist, depot);

        Map<Integer, List<Integer>> ways = new ConcurrentHashMap<>();
        ExecutorService clusterExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<Map.Entry<Integer, List<Integer>>>> futures = new ArrayList<>();

        int vehicleId = 1;
        for (List<Integer> cluster : clusters) {
            final int vid = vehicleId++;
            futures.add(clusterExecutor.submit(() -> {
                List<Integer> initial = buildRoute(cluster, depot, dist);
                List<Integer> improved = improveRoute(initial, dist);
                return Map.entry(vid, improved);
            }));
        }

        for (Future<Map.Entry<Integer, List<Integer>>> f : futures) {
            try {
                Map.Entry<Integer, List<Integer>> entry = f.get();
                ways.put(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        clusterExecutor.shutdown();

        // Improve inter-route
        for (int iter = 0; iter < MAX_OPT_ITER; iter++) {
            boolean improved = false;
            List<Integer> keys = new ArrayList<>(ways.keySet());

            for (int i = 0; i < keys.size(); i++) {
                for (int j = i + 1; j < keys.size(); j++) {
                    List<Integer> routeA = new ArrayList<>(ways.get(keys.get(i)));
                    List<Integer> routeB = new ArrayList<>(ways.get(keys.get(j)));

                    int currentMax = Math.max(calculateRouteLength(routeA, dist), calculateRouteLength(routeB, dist));

                    for (int a = 1; a < routeA.size() - 1; a++) {
                        for (int b = 1; b < routeB.size() - 1; b++) {
                            int tempA = routeA.get(a);
                            int tempB = routeB.get(b);
                            routeA.set(a, tempB);
                            routeB.set(b, tempA);

                            int newMax = Math.max(calculateRouteLength(routeA, dist), calculateRouteLength(routeB, dist));
                            if (newMax < currentMax) {
                                ways.put(keys.get(i), improveRoute(routeA, dist));
                                ways.put(keys.get(j), improveRoute(routeB, dist));
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
                if (improved) break;
            }
            if (!improved) break;
        }

        // Shift from longest to shortest
        for (int iter = 0; iter < MAX_OPT_ITER; iter++) {
            boolean improved = false;
            List<Map.Entry<Integer, Integer>> sorted = ways.entrySet().stream()
                    .map(e -> Map.entry(e.getKey(), calculateRouteLength(e.getValue(), dist)))
                    .sorted(Map.Entry.comparingByValue())
                    .toList();

            int longest = sorted.get(sorted.size() - 1).getKey();
            int shortest = sorted.get(0).getKey();

            List<Integer> longRoute = new ArrayList<>(ways.get(longest));
            List<Integer> shortRoute = new ArrayList<>(ways.get(shortest));

            for (int i = 1; i < longRoute.size() - 1; i++) {
                int city = longRoute.get(i);
                List<Integer> newLong = new ArrayList<>(longRoute);
                newLong.remove(i);

                for (int j = 1; j < shortRoute.size(); j++) {
                    List<Integer> newShort = new ArrayList<>(shortRoute);
                    newShort.add(j, city);
                    int newMax = Math.max(calculateRouteLength(newLong, dist), calculateRouteLength(newShort, dist));
                    if (newMax < calculateRouteLength(longRoute, dist)) {
                        ways.put(longest, improveRoute(newLong, dist));
                        ways.put(shortest, improveRoute(newShort, dist));
                        improved = true;
                        break;
                    }
                }
                if (improved) break;
            }
            if (!improved) break;
        }

        VRPResult result = new VRPResult();
        result.setDepot(depot);
        result.setWays(ways);
        result.setMaxCycleWeight(ways.values().stream().mapToInt(r -> calculateRouteLength(r, dist)).max().orElse(0));
        result.setTotalWeight(ways.values().stream().mapToInt(r -> calculateRouteLength(r, dist)).sum());

        System.out.println(depot + " " + result.getMaxCycleWeight() + " " + result.getTotalWeight() + " " + Duration.between(start, Instant.now()).toMillis());
        return result;
    }

    private static List<List<Integer>> clusterSmart(List<Integer> vertices, int m, int depot, int[][] dist) {
        List<List<Integer>> clusters = new ArrayList<>();
        for (int i = 0; i < m; i++) clusters.add(new ArrayList<>());

        List<Integer> centers = new ArrayList<>();
        centers.add(vertices.stream().min(Comparator.comparingInt(v -> dist[depot][v])).orElseThrow());

        while (centers.size() < m) {
            int nextCenter = vertices.stream()
                    .filter(v -> !centers.contains(v))
                    .max(Comparator.comparingInt(v -> centers.stream().mapToInt(c -> dist[v][c]).min().orElse(Integer.MAX_VALUE)))
                    .orElseThrow();
            centers.add(nextCenter);
        }

        for (int i = 0; i < m; i++) clusters.get(i).add(centers.get(i));

        for (int v : vertices) {
            if (centers.contains(v)) continue;
            int bestCluster = -1, minDist = Integer.MAX_VALUE;
            for (int i = 0; i < m; i++) {
                int d = dist[v][centers.get(i)];
                if (d < minDist) {
                    minDist = d;
                    bestCluster = i;
                }
            }
            clusters.get(bestCluster).add(v);
        }

        return clusters;
    }

    private static void balanceClusters(List<List<Integer>> clusters, int[][] dist, int depot) {
        boolean changed = true;
        while (changed) {
            changed = false;
            clusters.sort(Comparator.comparingInt(List::size));
            List<Integer> largest = clusters.get(clusters.size() - 1);
            List<Integer> smallest = clusters.get(0);

            if (largest.size() - smallest.size() <= 1) break;

            int bestNode = -1, bestGain = Integer.MAX_VALUE;
            for (int node : largest) {
                int gain = dist[depot][node];
                if (gain < bestGain) {
                    bestGain = gain;
                    bestNode = node;
                }
            }

            if (bestNode != -1) {
                largest.remove((Integer) bestNode);
                smallest.add(bestNode);
                changed = true;
            }
        }
    }

    private static List<Integer> buildRoute(List<Integer> cluster, int depot, int[][] dist) {
        List<Integer> route = new ArrayList<>();
        route.add(depot);
        Set<Integer> unvisited = new HashSet<>(cluster);
        int current = depot;

        while (!unvisited.isEmpty()) {
            int finalCurrent = current;
            int next = unvisited.stream().min(Comparator.comparingInt(v -> dist[finalCurrent][v])).orElseThrow();
            route.add(next);
            unvisited.remove(next);
            current = next;
        }

        route.add(depot);
        return route;
    }

    private static List<Integer> improveRoute(List<Integer> route, int[][] dist) {
        RouteWithLength rwl = new RouteWithLength(route, dist);
        for (int iter = 0; iter < MAX_OPT_ITER; iter++) {
            RouteWithLength twoOpted = twoOpt(rwl);
            RouteWithLength relocated = relocate(twoOpted);
            if (relocated.length >= rwl.length) break;
            rwl = relocated;
        }
        return rwl.route;
    }

    private static RouteWithLength twoOpt(RouteWithLength rwl) {
        boolean improvement = true;
        while (improvement) {
            improvement = false;
            for (int i = 1; i < rwl.route.size() - 2; i++) {
                for (int j = i + 1; j < rwl.route.size() - 1; j++) {
                    int delta = rwl.dist[rwl.route.get(i - 1)][rwl.route.get(j)] +
                            rwl.dist[rwl.route.get(i)][rwl.route.get(j + 1)] -
                            rwl.dist[rwl.route.get(i - 1)][rwl.route.get(i)] -
                            rwl.dist[rwl.route.get(j)][rwl.route.get(j + 1)];

                    if (delta < 0) {
                        Collections.reverse(rwl.route.subList(i, j + 1));
                        rwl.length += delta;
                        improvement = true;
                    }
                }
            }
        }
        return rwl;
    }

    private static RouteWithLength relocate(RouteWithLength rwl) {
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 1; i < rwl.route.size() - 1; i++) {
                int node = rwl.route.get(i);
                for (int j = 1; j < rwl.route.size(); j++) {
                    if (j == i || j == i + 1) continue;
                    List<Integer> newRoute = new ArrayList<>(rwl.route);
                    newRoute.remove(i);
                    newRoute.add(j < i ? j : j - 1, node);
                    int newLen = calculateRouteLength(newRoute, rwl.dist);
                    if (newLen < rwl.length) {
                        rwl.updateRoute(newRoute);
                        improved = true;
                        break;
                    }
                }
                if (improved) break;
            }
        }
        return rwl;
    }

    private static int calculateRouteLength(List<Integer> route, int[][] dist) {
        int length = 0;
        for (int i = 0; i < route.size() - 1; ++i) {
            length += dist[route.get(i)][route.get(i + 1)];
        }
        return length;
    }

    private static class RouteWithLength {
        List<Integer> route;
        int length;
        int[][] dist;

        RouteWithLength(List<Integer> route, int[][] dist) {
            this.route = route;
            this.dist = dist;
            this.length = calculateRouteLength(route, dist);
        }

        void updateRoute(List<Integer> newRoute) {
            this.route = newRoute;
            this.length = calculateRouteLength(newRoute, dist);
        }
    }
}
