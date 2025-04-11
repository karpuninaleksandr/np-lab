package ru.ac.uniyar.service;

import ru.ac.uniyar.model.BenchmarkResult;
import ru.ac.uniyar.model.BenchmarkTask;
import ru.ac.uniyar.model.Edge;
import ru.ac.uniyar.model.Vertex;
import ru.ac.uniyar.utils.Utils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class SpanningTreeFinder {
    public BenchmarkResult findMinSpanningTree(BenchmarkTask task) {
        List<BenchmarkResult> tries = new ArrayList<>();
        for (int k = 1; k <= task.getSize(); ++k) {
            Instant startTime = Instant.now();
            BenchmarkResult result = new BenchmarkResult();
            result.setEdges(new ArrayList<>());

            List<Integer> spanningTree = new ArrayList<>();
            Map<Integer, Integer> degrees = new HashMap<>();
            Map<Integer, Map<Integer, Integer>> weights = new HashMap<>();
            for (int i = 1; i <= task.getSize(); ++i) {
                degrees.put(i, 0);
                Map<Integer, Integer> currWeights = new HashMap<>();
                for (int it = 1; it <= task.getSize(); ++it) {
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
            int maxLeaves = task.getSize() / 16, leaves = 1;

            spanningTree.add(k);
            int minWeight = task.getSize();
            int vertexNumber1 = 0, vertexNumber2 = 0;
            Vertex checkVertex;

            while (spanningTree.size() < task.getSize()) {
                for (int number : spanningTree) {
                    checkVertex = task.getVertexes().get(number);
                    int currentVertexNumber = weights.get(checkVertex.getNumber()).keySet().stream().toList().get(0);
                    int currentWeight = weights.get(checkVertex.getNumber()).get(currentVertexNumber);
                    if (minWeight > currentWeight) {
                        if (degrees.get(checkVertex.getNumber()) == 1 || leaves < maxLeaves) {
                            minWeight = currentWeight;
                            vertexNumber1 = checkVertex.getNumber();
                            vertexNumber2 = currentVertexNumber;
                        }
                    }
                }
                spanningTree.add(vertexNumber2);
                result.getEdges().add(new Edge(vertexNumber1, vertexNumber2));
                if (degrees.get(vertexNumber1) != 1) {
                    ++leaves;
                }
                degrees.put(vertexNumber1, degrees.get(vertexNumber1) + 1);
                degrees.put(vertexNumber2, degrees.get(vertexNumber2) + 1);
                result.setWeight(result.getWeight() + minWeight);
                for (int number: spanningTree) {
                    weights.get(number).remove(vertexNumber2, weights.get(number).get(vertexNumber2));
                    weights.get(vertexNumber2).remove(number, weights.get(vertexNumber2).get(number));
                }
                minWeight = task.getSize();
//                System.out.println("spanningTree size: " + spanningTree.size());
            }

            result.setLeaves(leaves);
            System.out.println("leaves:" + leaves + " weight: "+ result.getWeight());
            tries.add(result);
            /*
            Duration duration = Duration.between(startTime, Instant.now());
            System.out.println("time (minutes): " + duration.toMinutes());
            System.out.println("time (seconds): " + duration.toSeconds());
            System.out.println("time (milliseconds): " + duration.toMillis());
             */
        }
        return tries.stream().min(Comparator.comparingInt(BenchmarkResult::getWeight)).orElse(null);
    }
}
