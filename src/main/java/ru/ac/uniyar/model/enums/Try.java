package ru.ac.uniyar.model.enums;

import lombok.Getter;

@Getter
public enum Try {
    FIRST(1),
    SECOND(2),
    THIRD(3);

    private final int num;

    Try(int num) {
        this.num = num;
    }
}
