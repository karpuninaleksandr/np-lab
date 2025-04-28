package ru.ac.uniyar.model.results;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VRPResult {
    private int maxCycleWeight;
    private int totalWeight;
    private Map<Integer, List<Integer>> ways;
    private int depot;
}
