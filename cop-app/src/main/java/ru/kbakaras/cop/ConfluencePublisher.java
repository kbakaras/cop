package ru.kbakaras.cop;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.htmlcleaner.TagNode;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import ru.kbakaras.cop.adoc.ConfluenceConverter;
import ru.kbakaras.cop.confluence.ConfluenceApi;
import ru.kbakaras.cop.confluence.dto.Ancestor;
import ru.kbakaras.cop.confluence.dto.Content;
import ru.kbakaras.cop.confluence.dto.ContentBody;
import ru.kbakaras.cop.confluence.dto.ContentBodyValue;
import ru.kbakaras.cop.confluence.dto.Space;
import ru.kbakaras.cop.model.AttachmentSource;
import ru.kbakaras.cop.model.PageSource;
import ru.kbakaras.sugar.restclient.LoginPasswordDto;
import ru.kbakaras.sugar.restclient.SugarRestClient;
import ru.kbakaras.sugar.restclient.SugarRestIdentityBasic;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
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

    @Option(names = {"-p", "--password"},
            description = "Confluence user password",
            required = true,
            arity = "0..1",
            interactive = true)
    private char[] password;

    @Option(names = {"-f", "--file"}, description = "Path to file with page to publish", required = true)
    private File file;

    @Option(names = {"-s", "--space"}, description = "Target space", required = true)
    String spaceKey;

    @Option(names = {"-r", "--parent-id"}, description = "Confluence's parent page id")
    String parentId;


    @SneakyThrows
    @Override
    public Integer call() {
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

    /**
     * Метод вызывается из команд публикации/обновления страницы. Он исполняет последовательность действий:
     * <ol>
     * <li>Зачитывает содержимое файла с публикуемым документом.</li>
     * <li>Выполняет его конвертацию из формата asciidoctor в формат хранения Confluence.</li>
     * <li>Выполняет канонизацию полученного в результате конвертации html.</li>
     * <li>Определяет список изображений и размещает их в специальной структуре.</li>
     * </ol>
     * В итоге на выходе получается объект {@link PageSource}, содержащий структурированное содержимое
     * страницы, подготовленное к публикации через api Confluence.
     */
    PageSource convertPageSource() throws IOException {

        if (!file.exists()) {
            throw new IllegalArgumentException(MessageFormat.format("File %s not found", file));
        }

        String pageContentSource = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        // region Конвертация документа в формат хранения Confluence
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaConverterRegistry().register(ConfluenceConverter.class);


        String pageTitle = asciidoctor.load(pageContentSource, Options.builder().parseHeaderOnly(true).build())
                .getDoctitle();
        String pageContent = asciidoctor.convert(pageContentSource, Options.builder()
                .backend("confluence")
                .toFile(false)
                .safe(SafeMode.UNSAFE)
                .build());

        asciidoctor.shutdown();
        // endregion

        TagNode node = PageSource.cleanContent(pageContent);

        File attachmentDir = file.getParentFile();
        ArrayList<AttachmentSource> attachments = new ArrayList<>();

        List<? extends TagNode> attachmentNodes = node.getElementList(
                tagNode -> tagNode.getName().equals("ri:attachment")
                        && tagNode.getParent().getName().equals("ac:link"),
                true);

        for (TagNode attachmentNode : attachmentNodes) {

            File attachmentFile = new File(attachmentDir, attachmentNode.getAttributeByName("ri:filename"));

            AttachmentSource attachmentSource = new AttachmentSource(attachmentFile);
            attachments.add(attachmentSource);

            attachmentNode.removeAttribute("ri:filename");
            attachmentNode.addAttribute("ri:filename", attachmentSource.name);
        }

        List<? extends TagNode> imageNodes = node.getElementList(
                tagNode -> tagNode.getName().equals("ri:attachment") && tagNode.getParent().getName().equals("ac:image"),
                true);

        for (TagNode imageNode: imageNodes) {

            File imageFile = new File(attachmentDir, imageNode.getAttributeByName("ri:filename"));

            AttachmentSource attachmentSource = new AttachmentSource(imageFile);
            attachments.add(attachmentSource);

            imageNode.removeAttribute("ri:filename");
            imageNode.addAttribute("ri:filename", attachmentSource.name);
        }

        return new PageSource(pageTitle, node, attachments);
    }

    ConfluenceApi confluenceApi() {
        return new ConfluenceApi(baseUrl, spaceKey,
                new SugarRestClient(new SugarRestIdentityBasic() {

                    @Override
                    public LoginPasswordDto getLoginAndPassword() {
                        return new LoginPasswordDto(login, String.valueOf(password));
                    }

                })
        );
    }


    public static void main(String... args) {
        int exitCode = new CommandLine(new ConfluencePublisher()).execute(args);
        System.exit(exitCode);
    }

}