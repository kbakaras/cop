package ru.kbakaras.cop.confluence.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Attachment {

    private String id;
    private String type;
    private String status;
    private String title;

    private ContentVersion version;

    private Extensions extensions;

    @JsonProperty(value = "_links")
    private Links links;

}
