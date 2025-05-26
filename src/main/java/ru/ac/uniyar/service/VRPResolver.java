package ru.ac.uniyar.service;

import lombok.Getter;
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

    private static final int MAX_OPT_ITER = 100;
    private static final int NUM_TRIALS = 10;

    public static VRPResult getAnswer(Task task) {
        int n = task.getSize();
        int m = (int) (Math.log(n) / Math.log(2));

        int[][] dist = new int[n + 1][n + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                dist[i][j] = Utils.getDistance(task.getVertexes().get(i), task.getVertexes().get(j));
            }
        }

        List<Integer> candidates = new ArrayList<>();
        for (int i = 1; i <= n; i++) candidates.add(i);

        candidates.sort(Comparator.comparingInt(i -> Arrays.stream(dist[i]).sum()));

        List<Integer> bestDepots = candidates.subList(0, 10);

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<CompletableFuture<VRPResult>> futures = new ArrayList<>();

        for (int depot : bestDepots) {
            final int currentDepot = depot;
            for (int t = 0; t < NUM_TRIALS; ++t) {
                int finalT = t;
                futures.add(CompletableFuture.supplyAsync(() -> computeResultForDepot(task, currentDepot, m, dist, finalT), executor));
            }
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

        List<List<Integer>> clusters = clusterWithMedoids(otherVertices, m, dist, seed);
        adaptiveBalanceClusters(clusters, dist, depot);

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

        for (int iter = 0; iter < MAX_OPT_ITER; iter++) {
            if (!moveBlockBetweenRoutes(ways, dist, 3)) break;
        }

        tabuSearch(ways, dist, MAX_OPT_ITER, 15);

        VRPResult result = new VRPResult();
        result.setDepot(depot);
        result.setWays(ways);
        result.setMaxCycleWeight(ways.values().stream().mapToInt(r -> calculateRouteLength(r, dist)).max().orElse(0));
        result.setTotalWeight(ways.values().stream().mapToInt(r -> calculateRouteLength(r, dist)).sum());

        System.out.println(depot + " " + result.getMaxCycleWeight() + " " + result.getTotalWeight() + " " + Duration.between(start, Instant.now()).toMillis());
//        Validator.validateVRPResult(task, result);
//        Writer.writeVRPResult(result, "src/main/resources/result/vrp/4096/%s_%s_%s.txt".formatted(depot, result.getMaxCycleWeight(), result.getTotalWeight()), task);
        return result;
    }

    private static List<List<Integer>> clusterWithMedoids(List<Integer> vertices, int m, int[][] dist, int seed) {
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

    private static void adaptiveBalanceClusters(List<List<Integer>> clusters, int[][] dist, int depot) {
        boolean changed = true;
        while (changed) {
            changed = false;

            List<Pair<Integer, Integer>> clusterWeights = new ArrayList<>();
            for (int i = 0; i < clusters.size(); ++i) {
                List<Integer> route = buildRoute(clusters.get(i), depot, dist);
                int length = calculateRouteLength(route, dist);
                clusterWeights.add(new Pair<>(i, length));
            }

            clusterWeights.sort(Comparator.comparingInt(Pair::getValue));
            int lightestId = clusterWeights.get(0).getKey();
            int heaviestId = clusterWeights.get(clusterWeights.size() - 1).getKey();

            if (lightestId == heaviestId) break;

            List<Integer> heaviest = clusters.get(heaviestId);
            List<Integer> lightest = clusters.get(lightestId);

            int bestGain = 0;
            int bestNode = -1;
            int oldHeavyLen = calculateRouteLength(buildRoute(heaviest, depot, dist), dist);
            int oldLightLen = calculateRouteLength(buildRoute(lightest, depot, dist), dist);

            for (int node : heaviest) {
                if (node == depot) continue;
                List<Integer> newHeavy = new ArrayList<>(heaviest);
                newHeavy.remove((Integer) node);
                List<Integer> newLight = new ArrayList<>(lightest);
                newLight.add(node);

                int newHeavyLen = calculateRouteLength(buildRoute(newHeavy, depot, dist), dist);
                int newLightLen = calculateRouteLength(buildRoute(newLight, depot, dist), dist);

                int newMax = Math.max(newHeavyLen, newLightLen);
                int oldMax = Math.max(oldHeavyLen, oldLightLen);
                int gain = oldMax - newMax;

                if (gain > bestGain) {
                    bestGain = gain;
                    bestNode = node;
                }
            }

            if (bestGain > 0) {
                heaviest.remove((Integer) bestNode);
                lightest.add(bestNode);
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


    private static void tabuSearch(Map<Integer, List<Integer>> ways, int[][] dist, int maxIters, int tabuTenure) {
        Map<String, Integer> tabuList = new HashMap<>();
        int iteration = 0;

        int bestMax = ways.values().stream().mapToInt(r -> calculateRouteLength(r, dist)).max().orElse(Integer.MAX_VALUE);
        Map<Integer, List<Integer>> bestWays = new HashMap<>(ways);

        while (iteration < maxIters) {
            boolean moved = false;
            int bestDelta = Integer.MAX_VALUE;
            Move bestMove = null;

            List<Integer> keys = new ArrayList<>(ways.keySet());

            for (int i = 0; i < keys.size(); ++i) {
                for (int j = 0; j < keys.size(); ++j) {
                    if (i == j) continue;
                    List<Integer> from = ways.get(keys.get(i));
                    List<Integer> to = ways.get(keys.get(j));

                    for (int pos = 1; pos < from.size() - 1; pos++) {
                        int node = from.get(pos);
                        for (int insert = 1; insert < to.size(); insert++) {
                            List<Integer> newFrom = new ArrayList<>(from);
                            List<Integer> newTo = new ArrayList<>(to);
                            newFrom.remove(pos);
                            newTo.add(insert, node);

                            int newMax = Math.max(calculateRouteLength(newFrom, dist), calculateRouteLength(newTo, dist));
                            int currentMax = Math.max(calculateRouteLength(from, dist), calculateRouteLength(to, dist));
                            int delta = newMax - currentMax;

                            String moveKey = keys.get(i) + "->" + keys.get(j) + ":" + node;

                            if ((delta < bestDelta && (!tabuList.containsKey(moveKey) || newMax < bestMax))) {
                                bestDelta = delta;
                                bestMove = new Move(keys.get(i), keys.get(j), node, pos, insert, moveKey);
                            }
                        }
                    }
                }
            }

            if (bestMove != null) {
                List<Integer> from = new ArrayList<>(ways.get(bestMove.fromId));
                List<Integer> to = new ArrayList<>(ways.get(bestMove.toId));

                from.remove(bestMove.fromPos);
                to.add(bestMove.toPos, bestMove.node);

                ways.put(bestMove.fromId, improveRoute(from, dist));
                ways.put(bestMove.toId, improveRoute(to, dist));

                int newMax = ways.values().stream().mapToInt(r -> calculateRouteLength(r, dist)).max().orElse(Integer.MAX_VALUE);
                if (newMax < bestMax) {
                    bestMax = newMax;
                    bestWays = new HashMap<>(ways);
                }

                tabuList.put(bestMove.key, iteration + tabuTenure);
                moved = true;
            }

            int finalIteration = iteration;
            tabuList.entrySet().removeIf(e -> e.getValue() <= finalIteration);
            if (!moved) break;
            iteration++;
        }

        ways.clear();
        ways.putAll(bestWays);
    }

    private record Move(int fromId, int toId, int node, int fromPos, int toPos, String key) {}


    private static List<Integer> improveRoute(List<Integer> route, int[][] dist) {
        RouteWithLength rwl = new RouteWithLength(route, dist);
        int iter = 0;

        while (iter++ < MAX_OPT_ITER) {
            boolean improved = false;

            RouteWithLength afterRelocate = relocate(rwl, dist);
            if (afterRelocate.length < rwl.length) {
                rwl = afterRelocate;
                improved = true;
            }

            RouteWithLength afterTwoOpt = twoOpt(rwl, dist);
            if (afterTwoOpt.length < rwl.length) {
                rwl = afterTwoOpt;
                improved = true;
            }

            if (!improved) break;
        }

        return rwl.route;
    }

    private static boolean moveBlockBetweenRoutes(Map<Integer, List<Integer>> ways, int[][] dist, int maxBlockSize) {
        List<Integer> keys = new ArrayList<>(ways.keySet());

        for (int i = 0; i < keys.size(); i++) {
            for (int j = 0; j < keys.size(); j++) {
                if (i == j) continue;
                List<Integer> from = new ArrayList<>(ways.get(keys.get(i)));
                List<Integer> to = new ArrayList<>(ways.get(keys.get(j)));

                for (int size = 2; size <= maxBlockSize; size++) {
                    for (int start = 1; start < from.size() - 1 - (size - 1); start++) {
                        List<Integer> block = from.subList(start, start + size);
                        for (int insert = 1; insert < to.size(); insert++) {
                            List<Integer> newFrom = new ArrayList<>(from);
                            List<Integer> newTo = new ArrayList<>(to);

                            newFrom.subList(start, start + size).clear();
                            newTo.addAll(insert, new ArrayList<>(block));

                            int maxBefore = Math.max(calculateRouteLength(from, dist), calculateRouteLength(to, dist));
                            int maxAfter = Math.max(calculateRouteLength(newFrom, dist), calculateRouteLength(newTo, dist));

                            if (maxAfter < maxBefore) {
                                ways.put(keys.get(i), improveRoute(newFrom, dist));
                                ways.put(keys.get(j), improveRoute(newTo, dist));
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }


    private static int calculateRouteLength(List<Integer> route, int[][] dist) {
        int sum = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            sum += dist[route.get(i)][route.get(i + 1)];
        }
        return sum;
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

    @Getter
    public static class Pair<K, V> {
        public final K key;
        public final V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }
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