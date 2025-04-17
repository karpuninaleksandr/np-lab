package ru.ac.uniyar;

import ru.ac.uniyar.model.Result;
import ru.ac.uniyar.model.Task;
import ru.ac.uniyar.service.BenchmarkResultValidator;
import ru.ac.uniyar.service.SpanningTreeFinder;
import ru.ac.uniyar.utils.Writer;
import ru.ac.uniyar.utils.Reader;

public class Main {
    public static void main(String[] args) {
        Reader reader = new Reader();
        Writer writer = new Writer();
        BenchmarkResultValidator validator = new BenchmarkResultValidator();
        SpanningTreeFinder spanningTreeFinder = new SpanningTreeFinder();
        int taskSize = 2048, tryNumber = 3;

        Task task = reader.readTask(String.format("src/main/resources/benchmark/Taxicab_%s.txt", taskSize));
        Result result = spanningTreeFinder.findMinSpanningTree(task);
        System.out.println("size: " + taskSize + ", weight: " + result.getWeight() + ", leaves: " + result.getLeaves());
        writer.writeResult(result, String.format("src/main/resources/result/task_1/try_%s/Karpunin_%s_%s.txt", tryNumber, taskSize, tryNumber));
//        Result result = reader.readResult("src/main/resources/result/task_1/try_3/9477_Karpunin_512_3.txt");
        validator.validate(task, result);
    }
}