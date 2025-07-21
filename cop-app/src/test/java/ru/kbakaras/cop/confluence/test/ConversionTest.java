package ru.kbakaras.cop.confluence.test;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;
import ru.kbakaras.cop.adoc.ConfluenceConverter;

class ConversionTest {

    @Test
    void formatWidth_CorrectIntValue_CorrectString() {

        Assertions.assertEquals("width: 22.0px; ", formatWidth(22));
    }

    @Test
    void formatWidth_CorrectStringValue_CorrectString() {

        Assertions.assertEquals("width: 23.0px; ", formatWidth("23"));
        Assertions.assertEquals("width: 24.0px; ", formatWidth("24%"));
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
