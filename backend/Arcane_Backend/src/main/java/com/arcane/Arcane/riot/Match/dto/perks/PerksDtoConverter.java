package com.arcane.Arcane.riot.Match.dto.perks;

// PerksDtoConverter.java
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;

@Converter
public class PerksDtoConverter implements AttributeConverter<PerksDto, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // PerksDto 객체를 DB에 저장하기 위해 JSON 문자열로 변환
    @Override
    public String convertToDatabaseColumn(PerksDto attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 변환 중 에러가 발생했습니다.", e);
        }
    }

    // DB의 JSON 문자열을 PerksDto 객체로 변환
    @Override
    public PerksDto convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, PerksDto.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("객체 변환 중 에러가 발생했습니다.", e);
        }
    }
}
