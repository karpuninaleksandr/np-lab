package ru.ac.uniyar.utils;

import ru.ac.uniyar.model.results.LCMSTResult;
import ru.ac.uniyar.model.Task;
import ru.ac.uniyar.model.Edge;
import ru.ac.uniyar.model.Vertex;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

public class Reader {
    public static Task readTask(String path) {
        Task task = new Task();
        task.setVertexes(new HashMap<>());
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            task.setSize(Integer.parseInt(line.substring(4)));
            line = reader.readLine();
            int numberCounter = 1;

            while (line != null) {
                String[] position = line.split("\t");
                Vertex vertex = new Vertex(Integer.parseInt(position[0]), Integer.parseInt(position[1]), numberCounter);
                task.getVertexes().put(numberCounter, vertex);
                line = reader.readLine();
                ++numberCounter;
            }

            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return task;
    }

    public static LCMSTResult readLCMSTResult(String path) {
        LCMSTResult LCMSTResult = new LCMSTResult();
        LCMSTResult.setEdges(new ArrayList<>());
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            String[] position = line.split("\t");
            LCMSTResult.setWeight(Integer.parseInt(position[4].substring(0, position[4].length() - 1)));
            LCMSTResult.setLeaves(Integer.parseInt(position[8].substring(0, position[8].length() - 1)));
            line = reader.readLine();
            line = reader.readLine();
            while (line != null) {
                position = line.split("\t");
                LCMSTResult.getEdges().add(new Edge(Integer.parseInt(position[1]), Integer.parseInt(position[2]), 0));
                line = reader.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return LCMSTResult;
    }
}
