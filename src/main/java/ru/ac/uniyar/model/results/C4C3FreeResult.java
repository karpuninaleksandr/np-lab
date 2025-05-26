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
public class C4C3FreeResult {
    private int weight;
    private List<Edge> edges;
}
