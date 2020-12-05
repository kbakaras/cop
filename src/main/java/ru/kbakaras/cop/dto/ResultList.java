package ru.kbakaras.cop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultList<R> {
    private R[] results;

    private int start;
    private int limit;
    private int size;
}
