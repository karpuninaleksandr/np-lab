package ru.ac.uniyar.service;

import ru.ac.uniyar.model.Task;
import ru.ac.uniyar.model.results.VRPResult;
import ru.ac.uniyar.utils.Utils;
import ru.ac.uniyar.utils.Validator;
import ru.ac.uniyar.utils.Writer;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class VRPResolver {

    private static final int MAX_OPT_ITER = 500;
    private static final int NUM_TRIALS = 50;

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

        candidates.sort(Comparator.comparingInt(i -> Arrays.stream(dist[i]).sum()));

        List<Integer> bestDepots = candidates.subList(0, k);

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<CompletableFuture<VRPResult>> futures = new ArrayList<>();

        for (int depot : bestDepots) {
            final int currentDepot = depot;
            for (int t = 0; t < NUM_TRIALS; ++t) {
                int finalT = t;
                futures.add(CompletableFuture.supplyAsync(() -> computeResultForDepot(task, currentDepot, m, dist, finalT), executor));
            }
//            List<VRPResult> cur = futures.stream()
//                    .map(CompletableFuture::join)
//                    .toList();
//            VRPResult bestCurResult = cur.stream().min(Comparator
//                            .comparingInt(VRPResult::getMaxCycleWeight)
//                            .thenComparingInt(VRPResult::getTotalWeight))
//                    .orElse(null);
//            Validator.validateVRPResult(task, bestCurResult);
//            Writer.writeVRPResult(bestCurResult, "src/main/resources/result/vrp/try_2/%s.txt".formatted(depot), task);
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
//        return null;
    }

    private static VRPResult computeResultForDepot(Task task, int depot, int m, int[][] dist, int seed) {
        Instant start = Instant.now();
        List<Integer> otherVertices = new ArrayList<>();
        for (int i = 1; i <= task.getSize(); ++i) {
            if (i != depot) otherVertices.add(i);
        }

        List<List<Integer>> clusters = clusterWithMedoids(otherVertices, m, depot, dist, seed);
        balanceClusters(clusters, dist, depot);

        Map<Integer, List<Integer>> ways = new ConcurrentHashMap<>();

        int vehicleId = 1;
        for (List<Integer> cluster : clusters) {
            List<Integer> route = improveRoute(buildRoute(cluster, depot, dist), dist);
            ways.put(vehicleId++, route);
        }

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
        }

        VRPResult result = new VRPResult();
        result.setDepot(depot);
        result.setWays(ways);
        result.setMaxCycleWeight(ways.values().stream().mapToInt(r -> calculateRouteLength(r, dist)).max().orElse(0));
        result.setTotalWeight(ways.values().stream().mapToInt(r -> calculateRouteLength(r, dist)).sum());

        System.out.println(depot + " " + result.getMaxCycleWeight() + " " + result.getTotalWeight() + " " + Duration.between(start, Instant.now()).toMillis());
        return result;
    }

    private static List<List<Integer>> clusterWithMedoids(List<Integer> vertices, int m, int depot, int[][] dist, int seed) {
        List<List<Integer>> clusters = new ArrayList<>();
        for (int i = 0; i < m; ++i) clusters.add(new ArrayList<>());

        Random random = new Random(seed);
        List<Integer> medoids = new ArrayList<>();
        medoids.add(vertices.get(random.nextInt(vertices.size())));

        while (medoids.size() < m) {
            Map<Integer, Integer> minDistances = new HashMap<>();
            for (int v : vertices) {
                int minDist = medoids.stream().mapToInt(medoid -> dist[v][medoid]).min().orElse(Integer.MAX_VALUE);
                minDistances.put(v, minDist);
            }
            int total = minDistances.values().stream().mapToInt(Integer::intValue).sum();
            int r = random.nextInt(total);
            int cumulative = 0;
            for (Map.Entry<Integer, Integer> entry : minDistances.entrySet()) {
                cumulative += entry.getValue();
                if (cumulative >= r) {
                    medoids.add(entry.getKey());
                    break;
                }
            }
        }

        boolean changed;
        do {
            changed = false;
            for (List<Integer> cluster : clusters) cluster.clear();
            for (int v : vertices) {
                int best = -1, minDist = Integer.MAX_VALUE;
                for (int i = 0; i < medoids.size(); ++i) {
                    int d = dist[v][medoids.get(i)];
                    if (d < minDist) {
                        minDist = d;
                        best = i;
                    }
                }
                clusters.get(best).add(v);
            }

            List<Integer> newMedoids = new ArrayList<>();
            for (List<Integer> cluster : clusters) {
                int best = -1, minTotal = Integer.MAX_VALUE;
                for (int candidate : cluster) {
                    int total = cluster.stream().mapToInt(v -> dist[v][candidate]).sum();
                    if (total < minTotal) {
                        minTotal = total;
                        best = candidate;
                    }
                }
                newMedoids.add(best);
            }

            if (!new HashSet<>(newMedoids).equals(new HashSet<>(medoids))) {
                medoids = newMedoids;
                changed = true;
            }
        } while (changed);

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

            int transferCount = (largest.size() - smallest.size()) / 2;
            List<Integer> toMove = largest.stream()
                    .sorted(Comparator.comparingInt(node -> dist[depot][node]))
                    .limit(transferCount)
                    .toList();

            largest.removeAll(toMove);
            smallest.addAll(toMove);
            changed = true;
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
        int iter = 0;
        boolean improved = true;
        RouteWithLength rwl = new RouteWithLength(route, dist);

        while (improved && iter++ < MAX_OPT_ITER) {
            improved = false;
            int currentLength = rwl.length;

            RouteWithLength afterTwoOpt = twoOpt(rwl, dist);
            if (afterTwoOpt.length < currentLength) {
                rwl = afterTwoOpt;
                currentLength = rwl.length;
                improved = true;
            }

            RouteWithLength afterRelocate = relocate(rwl, dist);
            if (afterRelocate.length < currentLength) {
                rwl = afterRelocate;
                improved = true;
            }
        }
        return rwl.route;
    }

    private static int calculateRouteLength(List<Integer> route, int[][] dist) {
        int length = 0;
        for (int i = 0; i < route.size() - 1; ++i) {
            length += dist[route.get(i)][route.get(i + 1)];
        }
        return length;
    }

    private static RouteWithLength twoOpt(RouteWithLength rwl, int[][] dist) {
        boolean improvement = true;
        int size = rwl.route.size();

        while (improvement) {
            improvement = false;
            for (int i = 1; i < size - 2; i++) {
                for (int j = i + 1; j < size - 1; j++) {
                    int delta = dist[rwl.route.get(i - 1)][rwl.route.get(j)] +
                            dist[rwl.route.get(i)][rwl.route.get(j + 1)] -
                            dist[rwl.route.get(i - 1)][rwl.route.get(i)] -
                            dist[rwl.route.get(j)][rwl.route.get(j + 1)];

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

    private static RouteWithLength relocate(RouteWithLength rwl, int[][] dist) {
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

                    int newLen = calculateRouteLength(newRoute, dist);
                    if (newLen < rwl.length) {
                        rwl.updateRoute(newRoute, dist);
                        improved = true;
                        break;
                    }
                }
                if (improved) break;
            }
        }
        return rwl;
    }

    private static class RouteWithLength {
        List<Integer> route;
        int length;

        RouteWithLength(List<Integer> route, int[][] dist) {
            this.route = route;
            this.length = calculateRouteLength(route, dist);
        }

        void updateRoute(List<Integer> newRoute, int[][] dist) {
            this.route = newRoute;
            this.length = calculateRouteLength(newRoute, dist);
        }
    }
}