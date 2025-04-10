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

//        BenchmarkTask task64 = reader.readTask("src/main/resources/benchmark/Taxicab_64.txt");
//        BenchmarkResult result64 = spanningTreeFinder.findMinSpanningTree(task64);
//        System.out.println("result64: " + result64.getWeight());
//        writer.writeResult(result64, "src/main/resources/result/Karpunin_64_1.txt");

//        BenchmarkTask task128 = reader.readTask("src/main/resources/benchmark/Taxicab_128.txt");
//        BenchmarkResult result128 = spanningTreeFinder.findMinSpanningTree(task128);
//        System.out.println("result128: " + result128.getWeight() + " : " + result128.getLeaves());
//        writer.writeResult(result128, "src/main/resources/result/Karpunin_128_1.txt");

//        BenchmarkTask task512 = reader.readTask("src/main/resources/benchmark/Taxicab_512.txt");
//        BenchmarkResult result512 = spanningTreeFinder.findMinSpanningTree(task512);
//        System.out.println("result512: " + result512.getWeight() + " : " + result512.getLeaves());
//        writer.writeResult(result512, "src/main/resources/result/Karpunin_512_1.txt");

//        BenchmarkTask task2048 = reader.readTask("src/main/resources/benchmark/Taxicab_2048.txt");
//        BenchmarkResult result2048 = spanningTreeFinder.findMinSpanningTree(task2048);
//        System.out.println("result2048: " + result2048.getWeight() + " : " + result2048.getLeaves());
//        writer.writeResult(result2048, "src/main/resources/result/Karpunin_2048_1.txt");

        BenchmarkTask task4096 = reader.readTask("src/main/resources/benchmark/Taxicab_4096.txt");
        BenchmarkResult result4096 = spanningTreeFinder.findMinSpanningTree(task4096);
        System.out.println("result4096: " + result4096.getWeight() + " : " + result4096.getLeaves());
        writer.writeResult(result4096, "src/main/resources/result/Karpunin_4096_1.txt");
    }
}