package ru.kbakaras.cop.confluence.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Space {

    private String key;


    public Space(String key) {
        this.key = key;
    }

}
