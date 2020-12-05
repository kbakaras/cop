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
import ru.kbakaras.cop.adoc.model.ImageDestination;
import ru.kbakaras.cop.adoc.model.ImageSource;
import ru.kbakaras.cop.confluence.ConfluenceApi;
import ru.kbakaras.cop.confluence.dto.Attachment;
import ru.kbakaras.cop.confluence.dto.Content;
import ru.kbakaras.cop.confluence.dto.ContentBody;
import ru.kbakaras.cop.confluence.dto.ContentBodyValue;
import ru.kbakaras.cop.confluence.dto.ContentList;
import ru.kbakaras.sugar.restclient.LoginPasswordDto;
import ru.kbakaras.sugar.restclient.SugarRestClient;
import ru.kbakaras.sugar.restclient.SugarRestIdentityBasic;
import ru.kbakaras.sugar.utils.CollectionUpdater;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@CommandLine.Command(
        name = "Confluence Publisher",
        version = "0.0.1",
        sortOptions = false,
        showDefaultValues = true)
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

    @SuppressWarnings("unused")
    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show usage help")
    private boolean showUsage;

    @SuppressWarnings("unused")
    @Option(names = { "-V", "--version" }, versionHelp = true, description = "Show version information")
    private boolean versionRequested;


    @SneakyThrows
    @Override
    public Integer call() throws Exception {

        String pageContentSource = readPageContentSource();

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

        // region Stream с исходными (публикуемыми) изображениями
        File imageDir = file.getParentFile();
        Document doc = Jsoup.parse(pageContent);
        Elements elements = doc.select("ac|image > ri|attachment");
        Stream<ImageSource> sourceImages = elements
                .stream()
                .map(element -> element.attr("ri:filename"))
                .map(fileName -> new File(imageDir, fileName))
                .map(ImageSource::new);
        // endregion


        try (SugarRestClient client = clientWithIdentity()) {
            ConfluenceApi api = new ConfluenceApi(baseUrl, spaceKey, client);

            ContentList contentList = api.findContentByTitle(pageTitle);

            // TODO Где-то должна быть логика, решающая обновление или создание
            if (contentList.getSize() != 1) {
                throw new RuntimeException("Обновление не возможно");
            }

            Content oldContent = contentList.getResults()[0];

            List<ImageDestination> destinationImages = new ArrayList<>();
            for (Attachment attachment : api.findAttachmentByContentId(oldContent.getId()).getResults()) {
                destinationImages.add(new ImageDestination(attachment, api.getAttachmentData(attachment)));
            }

            new CollectionUpdater<ImageDestination, ImageSource, String>(id -> id.name, is -> is.name)

                    .check4Changes((id, is) -> !id.sha1.equals(is.sha1))

                    .updateElement((id, is) -> {
                        try {
                            api.updateAttachmentData(oldContent.getId(), id.attachment, is.data);
                        } catch (URISyntaxException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    })

                    .collection(destinationImages, sourceImages);

            if (true) return 0;

            /*String oldStorageContent = oldContent.getBody().getStorage().getValue();



            String storageContent = IOUtils.toString(
                    ClassLoader.getSystemResourceAsStream("content.html"),
                    StandardCharsets.UTF_8);

            if (oldStorageContent.equals(storageContent)) {
                return;
            }*/

            Content content = new Content();
            content.setVersion(oldContent.getVersion());
            content.getVersion().setNumber(content.getVersion().getNumber() + 1);

            content.setTitle(oldContent.getTitle());
            content.setType("page");

            ContentBodyValue contentValue = new ContentBodyValue(pageContent, "storage");
            ContentBody contentBody = new ContentBody();
            contentBody.setStorage(contentValue);

            content.setBody(contentBody);

            api.updateContent(oldContent.getId(), content);

            return 0;
        }

    }

    private String readPageContentSource() throws IOException {

        if (!file.exists()) {
            throw new IllegalArgumentException(MessageFormat.format("File %s not found", file));
        }

        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    private SugarRestClient clientWithIdentity() {
        return new SugarRestClient(new SugarRestIdentityBasic() {

            @Override
            public LoginPasswordDto getLoginAndPassword() {
                return new LoginPasswordDto(login, password);
            }

        });
    }


    public static void main(String... args) {
        int exitCode = new CommandLine(new ConfluencePublisher()).execute(args);
        System.exit(exitCode);
    }

}
