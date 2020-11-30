package ru.kbakaras.confluence.publisher.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Content {

    private String id;
    private String type;
    private String status;
    private String title;

    private ContentVersion version;

    private ContentBody body;

}
