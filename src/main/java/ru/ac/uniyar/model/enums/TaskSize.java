package ru.ac.uniyar.model.enums;

import lombok.Getter;

@Getter
public enum TaskSize {
    TASK_64(64),
    TASK_128(128),
    TASK_512(512),
    TASK_2048(2048),
    TASK_4096(4096);

    private final int code;

    TaskSize(int code) {
        this.code = code;
    }
}
