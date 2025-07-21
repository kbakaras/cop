package ru.kbakaras.cop.confluence.adoc;

import org.apache.commons.io.IOUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.kbakaras.cop.adoc.ConfluenceConverter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

class ConfluenceConverterTest {

    private static Asciidoctor asciidoctor;


    @BeforeAll
    static void init() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaConverterRegistry().register(ConfluenceConverter.class);
    }

    @AfterAll
    static void done() {
        asciidoctor.shutdown();
        asciidoctor.close();
    }


    @Test
    void paragraphsInTableCell() throws IOException {
        read("Абзацы и переводы строк в табличной ячейке");
    }

    @Test
    void paragraphsInList() throws IOException {
        read("Абзацы и переводы строк в элементах перечня");
    }

    @Test
    void paragraphsInAdmonitions() throws IOException {
        read("Абзацы и переводы строк в блоке примечания");
    }

    @Test
    void sectionNumbers() throws IOException {
        read("Нумерация рубрик");
    }

    @Test
    void anchorsAndLinks() throws IOException {
        read("Локальные ссылки и якоря");
    }

    @Test
    void imagesdirIsSet() throws IOException {
        read("imagesdir is set");
    }

    @Test
    void imagesdirIsNotSet() throws IOException {
        read("imagesdir is not set");
    }

    @Test
    void callouts() throws IOException {
        read("Выноски (callouts)");
    }

    @Test
    void lists() throws IOException {
        read("Списки");
    }

    @Test
    void descriptionLis() throws IOException {
        read("Description list");
    }

    @Test
    void jira() throws IOException {
        read("Jira");
    }

    @Test
    void exampleExpandable() throws IOException {
        read("Example to expandable");
    }


    private void read(String sourceFileName) throws IOException {

        try (InputStream isSource = this.getClass().getResourceAsStream("/source/" + sourceFileName + ".adoc");
             InputStream isDest = this.getClass().getResourceAsStream("/dest/" + sourceFileName + ".xhtml")) {

            assert isSource != null;
            assert isDest != null;

            String source = IOUtils.toString(isSource, StandardCharsets.UTF_8);
            String expected = IOUtils.toString(isDest, StandardCharsets.UTF_8);


            String destination = asciidoctor.convert(source, Options.builder()
                    .backend("confluence")
                    .toFile(false)
                    .safe(SafeMode.UNSAFE)
                    .build());

            if (!destination.endsWith("\n")) {
                destination = destination + "\n";
            }

            Assertions.assertEquals(expected, destination);
        }

    }

}
