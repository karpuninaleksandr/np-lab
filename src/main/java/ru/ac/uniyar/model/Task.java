package ru.ac.uniyar.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class Task {
    private int size;
    private Map<Integer, Vertex> vertexes;
}
