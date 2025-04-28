package ru.ac.uniyar.utils;

import ru.ac.uniyar.model.results.LCMSTResult;
import ru.ac.uniyar.model.Edge;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class Writer {
    public static void writeLCMSTResult(LCMSTResult LCMSTResult, String path) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(String.format("c\tВес\tдерева\t=\t%s,\tчисло\tлистьев\t=\t%s,\n", LCMSTResult.getWeight(), LCMSTResult.getLeaves()));
            writer.write(String.format("p\tedge\t%s\t%s\n", LCMSTResult.getEdges().size() + 1, LCMSTResult.getEdges().size()));
            for (Edge edge : LCMSTResult.getEdges()) {
                writer.write(String.format("e\t%s\t%s\n", edge.getVertex1(), edge.getVertex2()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
