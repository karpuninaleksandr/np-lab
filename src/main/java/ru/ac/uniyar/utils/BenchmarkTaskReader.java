package ru.ac.uniyar.utils;

import ru.ac.uniyar.model.BenchmarkTask;
import ru.ac.uniyar.model.Vertex;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

public class BenchmarkTaskReader {

    public BenchmarkTask readTask(String path) {
        BenchmarkTask benchmarkTask = new BenchmarkTask();
        benchmarkTask.setVertexes(new HashMap<>());
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            benchmarkTask.setSize(Integer.parseInt(line.substring(4)));
            line = reader.readLine();
            int numberCounter = 1;

            while (line != null) {
                String[] position = line.split("\t");
                Vertex vertex = new Vertex(Integer.parseInt(position[0]), Integer.parseInt(position[1]), numberCounter);
                benchmarkTask.getVertexes().put(numberCounter, vertex);
                line = reader.readLine();
                ++numberCounter;
            }

            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return benchmarkTask;
    }
}
