package ru.ac.uniyar.service;

import ru.ac.uniyar.model.Task;
import ru.ac.uniyar.model.results.VRPResult;
import ru.ac.uniyar.utils.Utils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class VRPResolver {

    private static final int MAX_OPT_ITER = 100;
    private static final int NUM_TRIALS = 100;

    public static VRPResult getAnswer(Task task) {
        int n = task.getSize();
        int m = (int) (Math.log(n) / Math.log(2));

        int[][] dist = new int[n + 1][n + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                dist[i][j] = Utils.getDistance(task.getVertexes().get(i), task.getVertexes().get(j));
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<CompletableFuture<VRPResult>> futures = new ArrayList<>();

        for (int depot = 1; depot <= n; ++depot) {
            final int currentDepot = depot;
            futures.add(CompletableFuture.supplyAsync(() -> {
                VRPResult best = null;
                for (int t = 0; t < NUM_TRIALS; ++t) {
                    VRPResult result = computeResultForDepot(task, currentDepot, m, dist, t);
                    if (best == null || result.getMaxCycleWeight() < best.getMaxCycleWeight() ||
                            (result.getMaxCycleWeight() == best.getMaxCycleWeight() && result.getTotalWeight() < best.getTotalWeight())) {
                        best = result;
                    }
                }
                return best;
            }, executor));
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

    private static VRPResult computeResultForDepot(Task task, int depot, int m, int[][] dist, int seed) {
        Instant start = Instant.now();
        List<Integer> otherVertices = new ArrayList<>();
        for (int i = 1; i <= task.getSize(); ++i) {
            if (i != depot) otherVertices.add(i);
        }

        List<List<Integer>> clusters = clusterWithNoise(otherVertices, m, depot, dist, seed);
        balanceClusters(clusters, dist, depot);

        Map<Integer, List<Integer>> ways = new HashMap<>();
        int maxWayLength = 0;
        int totalLength = 0;
        int vehicleId = 1;

        for (List<Integer> cluster : clusters) {
            List<Integer> route = improveRoute(buildRoute(cluster, depot, dist), dist);
            ways.put(vehicleId++, route);

            int length = calculateRouteLength(route, dist);
            maxWayLength = Math.max(maxWayLength, length);
            totalLength += length;
        }

        // обмен клиентами между маршрутами для улучшения макс длины
        boolean improved = true;
        int iter = 0;
        while (improved && iter++ < MAX_OPT_ITER) {
            improved = false;
            List<Integer> keys = new ArrayList<>(ways.keySet());

            for (int i = 0; i < keys.size(); i++) {
                for (int j = i + 1; j < keys.size(); j++) {
                    List<Integer> routeA = new ArrayList<>(ways.get(keys.get(i)));
                    List<Integer> routeB = new ArrayList<>(ways.get(keys.get(j)));

                    int currentMax = Math.max(calculateRouteLength(routeA, dist), calculateRouteLength(routeB, dist));

                    outer:
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
                                break outer;
                            } else {
                                routeA.set(a, tempA);
                                routeB.set(b, tempB);
                            }
                        }
                    }
                }
            }
        }

        // смещение узлов из длинных маршрутов в короткие
        boolean improvedShift = true;
        iter = 0;
        while (improvedShift && iter++ < MAX_OPT_ITER) {
            improvedShift = false;
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
                        improvedShift = true;
                        break;
                    }
                }
                if (improvedShift) break;
            }
        }

        VRPResult result = new VRPResult();
        result.setDepot(depot);
        result.setWays(ways);
        result.setMaxCycleWeight(ways.values().stream().mapToInt(r -> calculateRouteLength(r, dist)).max().orElse(0));
        result.setTotalWeight(ways.values().stream().mapToInt(r -> calculateRouteLength(r, dist)).sum());

        System.out.println(depot + " " + result.getMaxCycleWeight() + " " + result.getTotalWeight() + " " + Duration.between(start, Instant.now()).toMillis());
        return result;
    }

    private static List<List<Integer>> clusterWithNoise(List<Integer> vertices, int m, int depot, int[][] dist, int seed) {
        List<List<Integer>> clusters = new ArrayList<>();
        for (int i = 0; i < m; ++i) clusters.add(new ArrayList<>());

        Random random = new Random(seed);
        Map<Integer, Integer> noise = new HashMap<>();
        for (int v : vertices) {
            noise.put(v, random.nextInt(5));
        }
        vertices.sort(Comparator.comparingInt(v -> dist[depot][v] + noise.get(v)));

        List<Integer> centers = new ArrayList<>(vertices.subList(0, m));
        Set<Integer> assigned = new HashSet<>(centers);

        for (int i = 0; i < m; ++i) clusters.get(i).add(centers.get(i));

        for (int v : vertices) {
            if (assigned.contains(v)) continue;
            int bestCluster = -1, minDist = Integer.MAX_VALUE;
            for (int i = 0; i < m; ++i) {
                for (int c : clusters.get(i)) {
                    int d = dist[v][c];
                    if (d < minDist) {
                        minDist = d;
                        bestCluster = i;
                    }
                }
            }
            clusters.get(bestCluster).add(v);
            assigned.add(v);
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
            for (int i = 0; i < largest.size(); ++i) {
                int node = largest.get(i);
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

    private static List<Integer> relocate(List<Integer> route, int[][] dist) {
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 1; i < route.size() - 1; i++) {
                int node = route.get(i);
                for (int j = 1; j < route.size(); j++) {
                    if (j == i || j == i + 1) continue;
                    List<Integer> newRoute = new ArrayList<>(route);
                    newRoute.remove(i);
                    newRoute.add(j < i ? j : j - 1, node);

                    if (calculateRouteLength(newRoute, dist) < calculateRouteLength(route, dist)) {
                        route = newRoute;
                        improved = true;
                        break;
                    }
                }
                if (improved) break;
            }
        }
        return route;
    }

    private static List<Integer> improveRoute(List<Integer> route, int[][] dist) {
        int iter = 0;
        boolean improved = true;
        while (improved && iter++ < MAX_OPT_ITER) {
            improved = false;
            int currentLength = calculateRouteLength(route, dist);

            List<Integer> twoOptRoute = twoOpt(route, dist);
            int twoOptLen = calculateRouteLength(twoOptRoute, dist);
            if (twoOptLen < currentLength) {
                route = twoOptRoute;
                currentLength = twoOptLen;
                improved = true;
            }

            List<Integer> relocatedRoute = relocate(route, dist);
            int relocatedLen = calculateRouteLength(relocatedRoute, dist);
            if (relocatedLen < currentLength) {
                route = relocatedRoute;
                improved = true;
            }
        }
        return route;
    }

    private static int calculateRouteLength(List<Integer> route, int[][] dist) {
        int length = 0;
        for (int i = 0; i < route.size() - 1; ++i) {
            length += dist[route.get(i)][route.get(i + 1)];
        }
        return length;
    }
}
