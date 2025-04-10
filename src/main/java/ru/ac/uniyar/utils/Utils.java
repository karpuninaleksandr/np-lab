package ru.ac.uniyar.utils;

import ru.ac.uniyar.model.Vertex;

public class Utils {
    public static int getDistance(Vertex vertex1, Vertex vertex2) {
        return Math.abs(vertex1.getX() - vertex2.getX()) + Math.abs(vertex1.getY() - vertex2.getY());
    }
}
