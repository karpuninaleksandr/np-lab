package ru.ac.uniyar.model.results;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.ac.uniyar.model.Edge;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LCMSTResult {
    private int weight;
    private int leaves;
    private List<Edge> edges;
}
