package ru.ac.uniyar;

import ru.ac.uniyar.model.BenchmarkResult;
import ru.ac.uniyar.model.BenchmarkTask;
import ru.ac.uniyar.service.BenchmarkResultValidator;
import ru.ac.uniyar.service.SpanningTreeFinder;
import ru.ac.uniyar.utils.BenchmarkResultWriter;
import ru.ac.uniyar.utils.BenchmarkReader;

public class Main {
    public static void main(String[] args) {
        BenchmarkReader reader = new BenchmarkReader();
        BenchmarkResultWriter writer = new BenchmarkResultWriter();
        BenchmarkResultValidator validator = new BenchmarkResultValidator();
        SpanningTreeFinder spanningTreeFinder = new SpanningTreeFinder();
        int taskSize = 4096, tryNumber = 3;

        BenchmarkTask task = reader.readTask(String.format("src/main/resources/benchmark/Taxicab_%s.txt", taskSize));
//        BenchmarkResult result = spanningTreeFinder.findMinSpanningTree(task);
//        System.out.println("size: " + taskSize + ", weight: " + result.getWeight() + ", leaves: " + result.getLeaves());
//        writer.writeResult(result, String.format("src/main/resources/result/task_1/try_%s/Karpunin_%s_%s.txt", tryNumber, taskSize, tryNumber));
        BenchmarkResult result = reader.readResult(String.format("src/main/resources/result/task_1/try_3/Karpunin_%s_%s_229184.txt", taskSize, tryNumber));
        validator.validate(task, result);
    }
}