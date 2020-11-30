package ru.kbakaras.confluence.publisher;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.utils.URIBuilder;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import ru.kbakaras.confluence.publisher.adoc.ConfluenceConverter;
import ru.kbakaras.confluence.publisher.dto.Content;
import ru.kbakaras.confluence.publisher.dto.ContentBody;
import ru.kbakaras.confluence.publisher.dto.ContentBodyValue;
import ru.kbakaras.confluence.publisher.dto.ContentList;
import ru.kbakaras.sugar.restclient.LoginPasswordDto;
import ru.kbakaras.sugar.restclient.SugarRestClient;
import ru.kbakaras.sugar.restclient.SugarRestIdentityBasic;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.concurrent.Callable;

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

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show usage help")
    private boolean showUsage;

    @Option(names = { "-V", "--version" }, versionHelp = true, description = "Show version information")
    private boolean versionRequested;


    @SneakyThrows
    @Override
    public Integer call() throws Exception {

        normalizeBaseUrl();

        if (!file.exists()) {
            throw new IllegalArgumentException(MessageFormat.format("File %s not found", file));
        }
        String pageName = FilenameUtils.removeExtension(file.getName());


        SugarRestIdentityBasic identity = new SugarRestIdentityBasic() {

            @Override
            public LoginPasswordDto getLoginAndPassword() {
                return new LoginPasswordDto(login, password);
            }

        };

        String pageContentSource = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaConverterRegistry().register(ConfluenceConverter.class);

        String pageContent = asciidoctor.convert(pageContentSource, OptionsBuilder.options()
                .backend("confluence")
                .toFile(false)
                .safe(SafeMode.UNSAFE));

        asciidoctor.shutdown();


        try (SugarRestClient client = new SugarRestClient(identity)) {

            URIBuilder uriBuilder = new URIBuilder(baseUrl + "rest/api/content")
                    .addParameter("spaceKey", spaceKey)
                    .addParameter("title", pageName)
                    .addParameter("expand", "space,body.view,body.storage,version,container");

            SugarRestClient.Response response = client.get(uriBuilder.toString());
            response.assertStatusCode(200);

            Content oldContent = response.getEntity(ContentList.class).getResults()[0];

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

            uriBuilder = new URIBuilder(baseUrl + "rest/api/content/" + oldContent.getId());

            response = client.put(uriBuilder.toString(), content);
            response.assertStatusCode(200);

            return 0;
        }

    }

    private void normalizeBaseUrl() {
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
    }


    public static void main(String... args) {
        int exitCode = new CommandLine(new ConfluencePublisher()).execute(args);
        System.exit(exitCode);
    }

}
