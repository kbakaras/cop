package ru.kbakaras.cop;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import ru.kbakaras.cop.adoc.ConfluenceConverter;
import ru.kbakaras.cop.confluence.ConfluenceApi;
import ru.kbakaras.cop.confluence.dto.Ancestor;
import ru.kbakaras.cop.confluence.dto.Content;
import ru.kbakaras.cop.confluence.dto.ContentBody;
import ru.kbakaras.cop.confluence.dto.ContentBodyValue;
import ru.kbakaras.cop.confluence.dto.Space;
import ru.kbakaras.cop.model.ImageSource;
import ru.kbakaras.cop.model.PageSource;
import ru.kbakaras.sugar.restclient.LoginPasswordDto;
import ru.kbakaras.sugar.restclient.SugarRestClient;
import ru.kbakaras.sugar.restclient.SugarRestIdentityBasic;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.Callable;

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
    String spaceKey;

    @Option(names = {"-r", "--parent-id"}, description = "Confluence's parent page id")
    String parentId;


    @SneakyThrows
    @Override
    public Integer call() throws Exception {
        throw new IllegalArgumentException("Operation not supported yet");
    }


    void setContentValue(Content content, PageSource pageSource) {
        content.setTitle(pageSource.title);
        content.setType(Content.TYPE_Page);

        content.setSpace(new Space(spaceKey));
        Optional.ofNullable(parentId)
                .map(id -> new Ancestor[]{new Ancestor(id)})
                .ifPresent(content::setAncestors);

        ContentBodyValue contentValue = new ContentBodyValue(pageSource.content, ContentBodyValue.REPRESENTATION_Storage);
        ContentBody contentBody = new ContentBody();
        contentBody.setStorage(contentValue);

        content.setBody(contentBody);
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


        File imageDir = file.getParentFile();
        Document doc = Jsoup.parseBodyFragment(pageContent);
        Elements elements = doc.select("ac|image > ri|attachment");

        ArrayList<ImageSource> images = new ArrayList<>();
        for (Element element : elements) {
            File imageFile = new File(imageDir, element.attr("ri:filename"));
            images.add(new ImageSource(imageFile));
            element.attr("ri:filename", imageFile.getName());
        }

        return new PageSource(pageTitle, doc.body().html(), images);

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
