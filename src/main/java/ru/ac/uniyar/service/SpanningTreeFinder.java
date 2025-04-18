package ru.ac.uniyar.service;

import ru.ac.uniyar.model.*;
import ru.ac.uniyar.utils.Utils;

import java.util.*;
import java.util.concurrent.*;

public class SpanningTreeFinder {
    public Result findMinSpanningTree(Task task) {
        int n = task.getSize();
        int maxLeaves = n / 16;
        int[][] weights = new int[n + 1][n + 1];

        for (int i = 1; i <= n; ++i) {
            for (int it = 1; it <= n; ++it) {
                if (i == it) continue;
                weights[i][it] = Utils.getDistance(task.getVertexes().get(i), task.getVertexes().get(it));
            }
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

    private Result checkVertex(int k, int n, int maxLeaves, int[][] weights) {
        boolean[] spanningTree = new boolean[n + 1];
        int[] degrees = new int[n + 1];

        PriorityQueue<Edge> queue = new PriorityQueue<>(Comparator.comparingInt(Edge::getWeight));
        Result result = new Result();
        result.setEdges(new ArrayList<>());

        spanningTree[k] = true;
        for (int i = 1; i <= n; ++i) {
            if (i != k) {
                queue.offer(new Edge(k, i, weights[k][i]));
            }
        }

        while (result.getEdges().size() < n - 1 && !queue.isEmpty()) {
            Edge edge = queue.poll();
            int u = edge.getVertex1();
            int v = edge.getVertex2();

            if (spanningTree[u] && spanningTree[v]) continue;

            int[] tempDegrees = Arrays.copyOf(degrees, degrees.length);
            tempDegrees[u]++;
            tempDegrees[v]++;

            int leafCount = 0;
            for (int deg : tempDegrees) {
                if (deg == 1) ++leafCount;
            }

            if (leafCount <= maxLeaves) {
                result.getEdges().add(edge);
                degrees = tempDegrees;

                int newNode = spanningTree[u] ? v : u;
                spanningTree[newNode] = true;

                for (int i = 1; i <= n; ++i) {
                    if (!spanningTree[i]) {
                        queue.offer(new Edge(newNode, i, weights[newNode][i]));
                    }
                }
            }
        }

        int finalLeafCount = 0;
        for (int d : degrees) if (d == 1) ++finalLeafCount;
        result.setWeight(result.getEdges().stream().mapToInt(Edge::getWeight).sum());
        result.setLeaves(finalLeafCount);
        postprocess(result, n, maxLeaves, weights);
        System.out.println("=================== " + k + " - " + result.getWeight());

        return result;
    }

    private void postprocess(Result result, int n, int maxLeaves, int[][] weights) {
        boolean improved = true;
        int count = 0;

        while (improved) {
            improved = false;
            ++count;
            System.out.println(count + " : " + result.getWeight());
            List<Edge> edges = result.getEdges();
            int[] degrees = new int[n + 1];
            for (Edge e : edges) {
                degrees[e.getVertex1()]++;
                degrees[e.getVertex2()]++;
            }

            outer:
            for (int i = 0; i < edges.size(); ++i) {
                Edge toRemove = edges.get(i);

                DSU dsu = new DSU(n);
                for (int j = 0; j < edges.size(); ++j) {
                    if (i != j) {
                        Edge e = edges.get(j);
                        dsu.union(e.getVertex1(), e.getVertex2());
                    }
                }

                for (int u = 1; u <= n; ++u) {
                    for (int v = u + 1; v <= n; ++v) {
                        if (dsu.connected(u, v)) continue;

                        int weight = weights[u][v];
                        if (weight >= toRemove.getWeight()) continue;

                        Edge replacement = new Edge(u, v, weight);

                        degrees[toRemove.getVertex1()]--;
                        degrees[toRemove.getVertex2()]--;
                        degrees[u]++;
                        degrees[v]++;

                        int newLeafCount = 0;
                        for (int d : degrees) {
                            if (d == 1) ++newLeafCount;
                        }

                        if (newLeafCount <= maxLeaves) {
                            edges.set(i, replacement);
                            result.setWeight(edges.stream().mapToInt(Edge::getWeight).sum());
                            result.setLeaves(newLeafCount);
                            improved = true;
                            break outer;
                        } else {
                            degrees[toRemove.getVertex1()]++;
                            degrees[toRemove.getVertex2()]++;
                            degrees[u]--;
                            degrees[v]--;
                        }
                    }
                }
            }
        }
    }

    private static class DSU {
        int[] parent;

        public DSU(int n) {
            parent = new int[n + 1];
            for (int i = 1; i <= n; ++i) {
                parent[i] = i;
            }
        }

        public int find(int x) {
            if (parent[x] != x)
                parent[x] = find(parent[x]);
            return parent[x];
        }

        public void union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);
            if (rootX != rootY)
                parent[rootY] = rootX;
        }

        public boolean connected(int x, int y) {
            return find(x) == find(y);
        }
    }
}
