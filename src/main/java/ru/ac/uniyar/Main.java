package ru.ac.uniyar;

import ru.ac.uniyar.model.enums.TaskSize;
import ru.ac.uniyar.model.enums.TaskType;
import ru.ac.uniyar.model.enums.Try;
import ru.ac.uniyar.model.results.LCMSTResult;
import ru.ac.uniyar.model.Task;
import ru.ac.uniyar.utils.Validator;
import ru.ac.uniyar.service.LCMSTResolver;
import ru.ac.uniyar.utils.Writer;
import ru.ac.uniyar.utils.Reader;

public class Main {
    private static final TaskSize size = TaskSize.TASK_64;
    private static final TaskType type = TaskType.LCMST;
    private static final Try tryNum = Try.FIRST;

    public static void main(String[] args) {
        Task task = Reader.readTask(String.format("src/main/resources/benchmark/Taxicab_%s.txt", size.getCode()));

        switch (type) {
            case LCMST -> {
                LCMSTResult result = LCMSTResolver.getAnswer(task);
                System.out.println("weight: " + result.getWeight() + ", leaves: " + result.getLeaves());
                Validator.validateLCMSTResult(task, result);
                Writer.writeLCMSTResult(result, String.format("src/main/resources/result/lcmst/try_%s/Karpunin_%s_%s.txt",
                        tryNum.getNum(), size.getCode(), tryNum.getNum()));
            }
            case VRP -> {

            }
        }
    }
}