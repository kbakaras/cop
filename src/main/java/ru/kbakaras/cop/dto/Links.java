package ru.kbakaras.cop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Links {

    private String webui;
    private String download;
    private String thumbnail;
    private String self;

}
