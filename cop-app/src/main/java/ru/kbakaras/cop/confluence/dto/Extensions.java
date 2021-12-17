package ru.kbakaras.cop.confluence.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Extensions {

    private String mediaType;
    private int fileSize;
    private String comment;

}
