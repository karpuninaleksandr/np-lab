package ru.ac.uniyar.service;

import ru.ac.uniyar.model.Task;
import ru.ac.uniyar.model.results.VRPResult;
import ru.ac.uniyar.utils.Utils;

import java.util.*;

public class VRPResolver {

    public static VRPResult getAnswer(Task task) {
        List<VRPResult> results = new ArrayList<>();
        int n = task.getSize();
        int m = (int) (Math.log(n) / Math.log(2));

        for (int depot = 1; depot <= n; ++depot) {
            System.out.println(depot);
            List<Integer> otherVertices = new ArrayList<>();
            for (int i = 1; i <= n; ++i) {
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
                List<Integer> route = buildRoute(cluster, depot, task);
                ways.put(vehicleId++, route);

                int length = calculateRouteLength(route, task);
                maxWayLength = Math.max(maxWayLength, length);
                totalLength += length;
            }

            VRPResult result = new VRPResult();
            result.setDepot(depot);
            result.setWays(ways);
            result.setMaxCycleWeight(maxWayLength);
            result.setTotalWeight(totalLength);

            results.add(result);
        }

        return results.stream()
                .min(Comparator
                        .comparingInt(VRPResult::getMaxCycleWeight)
                        .thenComparingInt(VRPResult::getTotalWeight))
                .orElse(null);
    }

    private static List<List<Integer>> cluster(List<Integer> vertices, int m, Task task, int depot) {
        List<List<Integer>> clusters = new ArrayList<>();
        for (int i = 0; i < m; ++i) {
            clusters.add(new ArrayList<>());
        }

        vertices.sort(Comparator.comparingInt(v -> Utils.getDistance(task.getVertexes().get(depot), task.getVertexes().get(v))));

        int clusterSize = vertices.size() / m;
        int extra = vertices.size() % m; // сколько кластеров получат +1 вершину

        int index = 0;
        for (int i = 0; i < m; ++i) {
            int size = clusterSize + (i < extra ? 1 : 0);
            for (int j = 0; j < size; ++j) {
                clusters.get(i).add(vertices.get(index++));
            }
        }

        return clusters;
    }

    private static List<Integer> buildRoute(List<Integer> cluster, int depot, Task task) {
        List<Integer> route = new ArrayList<>();
        route.add(depot);

        Set<Integer> unvisited = new HashSet<>(cluster);
        int current = depot;

        while (!unvisited.isEmpty()) {
            int finalCurrent = current;
            int next = unvisited.stream()
                    .min(Comparator.comparingInt(v -> Utils.getDistance(task.getVertexes().get(finalCurrent), task.getVertexes().get(v))))
                    .orElseThrow();
            route.add(next);
            unvisited.remove(next);
            current = next;
        }

        route.add(depot); // возвращаемся в депо
        return route;
    }

    private static int calculateRouteLength(List<Integer> route, Task task) {
        int length = 0;
        for (int i = 0; i < route.size() - 1; ++i) {
            length += Utils.getDistance(task.getVertexes().get(route.get(i)), task.getVertexes().get(route.get(i + 1)));
        }
        return length;
    }
}
