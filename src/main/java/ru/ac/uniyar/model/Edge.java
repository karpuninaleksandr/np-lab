package ru.ac.uniyar.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Edge {
    private int vertex1;
    private int vertex2;
    private int weight;
}
