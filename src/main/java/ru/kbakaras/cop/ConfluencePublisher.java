package ru.kbakaras.cop;

import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import ru.kbakaras.cop.adoc.ConfluenceConverter;
import ru.kbakaras.cop.dto.Attachment;
import ru.kbakaras.cop.dto.AttachmentList;
import ru.kbakaras.cop.dto.Content;
import ru.kbakaras.cop.dto.ContentBody;
import ru.kbakaras.cop.dto.ContentBodyValue;
import ru.kbakaras.cop.dto.ContentList;
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

        pageName = asciidoctor.readDocumentHeader(pageContentSource).getDocumentTitle().getMain();
        String pageContent = asciidoctor.convert(pageContentSource, OptionsBuilder.options()
                .backend("confluence")
                .toFile(false)
                .safe(SafeMode.UNSAFE));

        asciidoctor.shutdown();

        Document doc = Jsoup.parse(pageContent);
        Elements elements = doc.select("ac|image > ri|attachment");
        //.attr("ri:filename")

        try (SugarRestClient client = new SugarRestClient(identity)) {

            URIBuilder uriBuilder = new URIBuilder(baseUrl + "/rest/api/content")
                    .addParameter("spaceKey", spaceKey)
                    .addParameter("title", pageName)
                    .addParameter("expand", "space,body.view,body.storage,version,container");

            SugarRestClient.Response response = client.get(uriBuilder.toString());
            response.assertStatusCode(200);

            Content oldContent = response.getEntity(ContentList.class).getResults()[0];


            uriBuilder = new URIBuilder(baseUrl + "/rest/api/content/" + oldContent.getId() + "/child/attachment");
            response = client.get(uriBuilder.toString());
            response.assertStatusCode(200);
            AttachmentList attachmentList = response.getEntity(AttachmentList.class);

            for (Attachment attachment: attachmentList.getResults()) {
                uriBuilder = new URIBuilder(baseUrl + attachment.getLinks().getDownload());
                response = client.get(uriBuilder.toString());
                response.assertStatusCode(200);
                response.getEntityData();
                DigestUtils.sha1Hex(response.getEntityData());



                uriBuilder = new URIBuilder(String.format(
                        baseUrl + "/rest/api/content/%s/child/attachment/%s/data",
                        oldContent.getId(), attachment.getId()));
                HttpEntity entity = MultipartEntityBuilder
                        .create()
                        .addBinaryBody("file", response.getEntityData())
                        .build();
                response = client.post(uriBuilder.toString(), entity, "X-Atlassian-Token: nocheck");
                response.assertStatusCode(200);
            }

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

            uriBuilder = new URIBuilder(baseUrl + "/rest/api/content/" + oldContent.getId());

            response = client.put(uriBuilder.toString(), content);
            response.assertStatusCode(200);

            return 0;
        }

    }

    private void normalizeBaseUrl() {
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
    }


    public static void main(String... args) {
        int exitCode = new CommandLine(new ConfluencePublisher()).execute(args);
        System.exit(exitCode);
    }

}
