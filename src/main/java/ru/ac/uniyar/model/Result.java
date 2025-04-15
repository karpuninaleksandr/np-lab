package ru.ac.uniyar.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private int weight;
    private int leaves;
    private List<Edge> edges;
}
