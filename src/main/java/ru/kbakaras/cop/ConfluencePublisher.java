package ru.kbakaras.cop;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import ru.kbakaras.cop.adoc.ConfluenceConverter;
import ru.kbakaras.cop.adoc.model.ImageSource;
import ru.kbakaras.cop.adoc.model.PageSource;
import ru.kbakaras.cop.confluence.ConfluenceApi;
import ru.kbakaras.sugar.restclient.LoginPasswordDto;
import ru.kbakaras.sugar.restclient.SugarRestClient;
import ru.kbakaras.sugar.restclient.SugarRestIdentityBasic;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@CommandLine.Command(
        name = "Confluence Publisher",
        version = "0.0.1",
        mixinStandardHelpOptions = true,
        sortOptions = false,
        showDefaultValues = true,
        subcommands = {
                PublishCommand.class,
                UpdateCommand.class
        }
)
public class ConfluencePublisher implements Callable<Integer> {

    @Option(names = {"-b", "--base-url"}, description = "Base url for Confluence", required = true)
    private String baseUrl;

    @Option(names = {"-l", "--login"}, description = "Confluence user login", required = true)
    private String login;

    @Option(names = {"-p", "--password"}, description = "Confluence user password", required = true)
    private String password;

    @Option(names = {"-f", "--file"}, description = "Path to file with page to publish", required = true)
    private File file;

    @Option(names = {"-s", "--space"}, description = "Target space", required = true)
    private String spaceKey;

    @Option(names = {"-r", "--parent-id"}, description = "Confluence's parent page id")
    private String parentId;


    @SneakyThrows
    @Override
    public Integer call() throws Exception {
        throw new IllegalArgumentException("Operation not supported yet");
    }


    PageSource convertPageSource() throws IOException {

        if (!file.exists()) {
            throw new IllegalArgumentException(MessageFormat.format("File %s not found", file));
        }

        String pageContentSource = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        // region Конвертация документа в формат хранения Confluence
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaConverterRegistry().register(ConfluenceConverter.class);

        String pageTitle = asciidoctor.readDocumentHeader(pageContentSource).getDocumentTitle().getMain();
        String pageContent = asciidoctor.convert(pageContentSource, OptionsBuilder.options()
                .backend("confluence")
                .toFile(false)
                .safe(SafeMode.UNSAFE));

        asciidoctor.shutdown();
        // endregion

        return new PageSource(pageTitle, pageContent);

    }

    Stream<ImageSource> findSourceImages(String pageContent) {
        File imageDir = file.getParentFile();
        Document doc = Jsoup.parse(pageContent);
        Elements elements = doc.select("ac|image > ri|attachment");
        return elements
                .stream()
                .map(element -> element.attr("ri:filename"))
                .map(fileName -> new File(imageDir, fileName))
                .map(ImageSource::new);
    }

    ConfluenceApi confluenceApi() {
        return new ConfluenceApi(baseUrl, spaceKey,
                new SugarRestClient(new SugarRestIdentityBasic() {

                    @Override
                    public LoginPasswordDto getLoginAndPassword() {
                        return new LoginPasswordDto(login, password);
                    }

                })
        );
    }


    public static void main(String... args) {
        int exitCode = new CommandLine(new ConfluencePublisher()).execute(args);
        System.exit(exitCode);
    }

}
