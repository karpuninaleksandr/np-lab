package ru.ac.uniyar;

import ru.ac.uniyar.model.BenchmarkResult;
import ru.ac.uniyar.model.BenchmarkTask;
import ru.ac.uniyar.service.SpanningTreeFinder;
import ru.ac.uniyar.utils.BenchmarkResultWriter;
import ru.ac.uniyar.utils.BenchmarkTaskReader;

public class Main {
    public static void main(String[] args) {
        BenchmarkTaskReader reader = new BenchmarkTaskReader();
        BenchmarkResultWriter writer = new BenchmarkResultWriter();
        SpanningTreeFinder spanningTreeFinder = new SpanningTreeFinder();
        int taskSize = 512, tryNumber = 2;

        BenchmarkTask task = reader.readTask(String.format("src/main/resources/benchmark/Taxicab_%s.txt", taskSize));
        BenchmarkResult result = spanningTreeFinder.findMinSpanningTree(task);
        System.out.println("size: " + taskSize + ", weight: " + result.getWeight() + ", leaves: " + result.getLeaves());
        writer.writeResult(result, String.format("src/main/resources/result/Karpunin_%s_%s.txt", taskSize, tryNumber));
    }
}