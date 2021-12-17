package ru.kbakaras.cop.confluence.test;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;
import ru.kbakaras.cop.adoc.ConfluenceConverter;

public class Test_Conversion {

    @Test
    void formatWidth_CorrectIntValue_CorrectString() throws Exception {

        Assertions.assertEquals("width: 22.0%; ", formatWidth(22));
    }

    @Test
    void formatWidth_CorrectStringValue_CorrectString() throws Exception {

        Assertions.assertEquals("width: 23.0%; ", formatWidth("23"));
        Assertions.assertEquals("width: 24.0%; ", formatWidth("24%"));
    }


    @SneakyThrows
    private static String formatWidth(int width) {
        return Whitebox.invokeMethod(ConfluenceConverter.class, "formatWidth", width);
    }

    @SneakyThrows
    private static String formatWidth(Object width) {
        return Whitebox.invokeMethod(ConfluenceConverter.class, "formatWidth", width);
    }

}
