package ru.ac.uniyar;

import ru.ac.uniyar.model.enums.TaskSize;
import ru.ac.uniyar.model.enums.TaskType;
import ru.ac.uniyar.model.enums.Try;
import ru.ac.uniyar.model.results.C4C3FreeResult;
import ru.ac.uniyar.model.results.LCMSTResult;
import ru.ac.uniyar.model.Task;
import ru.ac.uniyar.model.results.VRPResult;
import ru.ac.uniyar.service.C4C3FreeResolver;
import ru.ac.uniyar.service.VRPResolver;
import ru.ac.uniyar.utils.Validator;
import ru.ac.uniyar.service.LCMSTResolver;
import ru.ac.uniyar.utils.Writer;
import ru.ac.uniyar.utils.Reader;

import java.util.concurrent.ExecutionException;

public class Main {
    private static final TaskSize size = TaskSize.TASK_512;
    private static final TaskType type = TaskType.C4C3FREE;
    private static final Try tryNum = Try.FIRST;

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Task task = Reader.readTask(String.format("src/main/resources/benchmark/Taxicab_%s.txt", size.getCode()));

        switch (type) {
            case LCMST -> {
                LCMSTResult result = LCMSTResolver.getAnswer(task);
                System.out.println("weight: " + result.getWeight() + ", leaves: " + result.getLeaves());
                Validator.validateLCMSTResult(task, result);
                Writer.writeLCMSTResult(result, "src/main/resources/result/lcmst/try_%s/Karpunin_%s_%s.txt"
                        .formatted(tryNum.getNum(), size.getCode(), tryNum.getNum()));
            }
            case VRP -> {
                VRPResult result = VRPResolver.getAnswer(task);
                System.out.println("maxCycleWeight: " + result.getMaxCycleWeight() + ", totalWeight: " +
                        result.getTotalWeight() + ", depot: " + result.getDepot());
                Validator.validateVRPResult(task, result);
                Writer.writeVRPResult(result, "src/main/resources/result/vrp/try_%s/Karpunin_%s_%s.txt"
                        .formatted(tryNum.getNum(), size.getCode(), tryNum.getNum()), task);
            }
            case C4C3FREE -> {
                C4C3FreeResult result = C4C3FreeResolver.resolve(task);
                System.out.println("weight: " + result.getWeight() + ", edges: " + result.getEdges().size());
                Validator.validateC4C3FreeResult(task, result);
                Writer.writeBiggestSubGraphResult(result, "src/main/resources/result/biggestsubgraph/try_%s/Karpunin_%s_%s_%s.txt"
                        .formatted(tryNum.getNum(), size.getCode(), result.getWeight(), tryNum.getNum()), task);
            }
        }
    }
}