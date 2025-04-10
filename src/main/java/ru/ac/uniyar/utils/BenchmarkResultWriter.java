package ru.ac.uniyar.utils;

import ru.ac.uniyar.model.BenchmarkResult;
import ru.ac.uniyar.model.Edge;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class BenchmarkResultWriter {
    public void writeResult(BenchmarkResult result, String path) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(String.format("c\tВес\tдерева\t=\t%s,\tчисло\tлистьев\t=\t%s,\n", result.getWeight(), result.getLeaves()));
            writer.write(String.format("p\tedge\t%s\t%s\n", result.getEdges().size() + 1, result.getEdges().size()));
            for (Edge edge : result.getEdges()) {
                writer.write(String.format("e\t%s\t%s\n", edge.getVertex1(), edge.getVertex2()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
