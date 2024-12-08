package ru.kbakaras.cop.confluence.adoc;

import org.apache.commons.io.IOUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.kbakaras.cop.adoc.ConfluenceConverter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

class ConfluenceConverterTest {

    @Test
    void paragraphsInTableCell() throws IOException {
        read("Абзацы и переводы строк в табличной ячейке");
    }

    @Test
    void paragraphsInList() throws IOException {
        read("Абзацы и переводы строк в элементах перечня");
    }


    @Test
    void sectionNumbers() throws IOException {
        read("Нумерация рубрик");
    }


    private void read(String sourceFileName) throws IOException {

        try (InputStream isSource = this.getClass().getResourceAsStream("/source/" + sourceFileName + ".adoc");
             InputStream isDest = this.getClass().getResourceAsStream("/dest/" + sourceFileName + ".xhtml")) {

            assert isSource != null;
            assert isDest != null;

            String source = IOUtils.toString(isSource, StandardCharsets.UTF_8);
            String expected = IOUtils.toString(isDest, StandardCharsets.UTF_8);

            try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
                asciidoctor.javaConverterRegistry().register(ConfluenceConverter.class);

                String destination = asciidoctor.convert(source, Options.builder()
                        .backend("confluence")
                        .toFile(false)
                        .safe(SafeMode.UNSAFE)
                        .build());

                if (!destination.endsWith("\n")) {
                    destination = destination + "\n";
                }

                asciidoctor.shutdown();

                Assertions.assertEquals(expected, destination);
            }
        }

    }

}
