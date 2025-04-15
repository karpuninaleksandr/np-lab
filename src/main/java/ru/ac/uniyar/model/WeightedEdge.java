package ru.ac.uniyar.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WeightedEdge {
    private Edge edge;
    private int weight;
}
