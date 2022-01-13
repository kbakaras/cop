package ru.kbakaras.cop.confluence.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentProperty {

    public static final String CONTENT_APPEARANCE_PUBLISHED = "content-appearance-published";
    public static final String CONTENT_APPEARANCE_DRAFT = "content-appearance-draft";

    public static final String CONTENT_APPEARANCE_VALUE_DEFAULT = "default";

    private String id;
    private String key;
    private String value;

    private ContentVersion version;

    @JsonIgnore
    public ContentProperty getUpdatedProperty(String newValue) {
        return new ContentProperty(id, key, newValue, version.getNextVersion());
    }

}
