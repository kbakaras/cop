package ru.kbakaras.cop.confluence.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Content {

    public static final String TYPE_Page      = "page";
    public static final String STATUS_Current = "current";

    private String id;
    private String type;
    private String status;
    private String title;

    private Space space;

    private ContentVersion version;

    private Ancestor[] ancestors;

    private ContentBody body;

}
