package com.arcane.Arcane.web.Statistics.domain;

import com.arcane.Arcane.common.Exception.Fail.PositionError;

import java.util.Arrays;

/**
 * 게임 내 포지션을 나타내는 Enum 클래스.
 */
public enum Position {
    TOP,
    JUG,
    MID,
    ADC,
    SUP;


    public static Position fromString(String positionString) {

        if (positionString == null || positionString.trim().isEmpty()) {
            throw new IllegalArgumentException("Position 문자열은 null이거나 비어있을 수 없습니다.");
        }

        // 모든 Enum 상수를 순회하며 일치하는 것을 찾습니다.
        for (Position pos : Position.values()) {
            // Enum 상수의 이름과 입력된 문자열을 대소문자 구분 없이 비교합니다.
            if (pos.name().equalsIgnoreCase(positionString)) {
                return pos;
            }
        }

        // 일치하는 상수를 찾지 못하면 예외를 발생시킵니다.
        throw new PositionError("'" + positionString + "'에 해당하는 포지션을 찾을 수 없습니다. 가능한 값: " + Arrays.toString(Position.values()));
    }
}