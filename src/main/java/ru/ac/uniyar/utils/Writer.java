package ru.ac.uniyar.utils;

import ru.ac.uniyar.model.Task;
import ru.ac.uniyar.model.results.LCMSTResult;
import ru.ac.uniyar.model.Edge;
import ru.ac.uniyar.model.results.VRPResult;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;

public class Writer {
    public static void writeLCMSTResult(LCMSTResult result, String path) {
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

    public static void writeVRPResult(VRPResult result, String path, Task task) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            int amountOfEdges = 0;
            for (List<Integer> way : result.getWays().values()) {
                amountOfEdges += way.size() - 1;
            }
            writer.write(String.format("c\tДепо\t=\t%s,\tсамый\tдлинный\tцикл\t=\t%s,\tсуммарная\tдлина\t=\t%s\n", result.getDepot(), result.getMaxCycleWeight(), result.getTotalWeight()));
            writer.write(String.format("p\tedge\t%s\t%s\n", task.getSize(), amountOfEdges));
            for (List<Integer> way: result.getWays().values()) {
                for (int i = 0; i < way.size() - 1; ++i) {
                    int u = way.get(i);
                    int v = way.get(i + 1);
                    writer.write(String.format("e\t%s\t%s\n", u, v));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
