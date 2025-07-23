package ru.kbakaras.cop;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import ru.kbakaras.cop.confluence.ConfluenceApi;
import ru.kbakaras.cop.confluence.dto.Content;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "source",
        mixinStandardHelpOptions = true,
        header = "Operation to get page source in storage format")
@Slf4j
public class SourceCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private ConfluencePublisher parentCommand;

    @CommandLine.Option(names = {"-i", "--page-id"}, description = "Confluence's page id")
    private String pageId;


    @Override
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public Integer call() throws Exception {

        try (ConfluenceApi api = parentCommand.confluenceApi()) {

            Content content = api.getContentById(pageId);

            try (Writer out = new OutputStreamWriter(System.out, StandardCharsets.UTF_8)) {
                out.write(content.getBody().getStorage().getValue());
                out.flush();
            }
        }

        return 0;
    }

}
