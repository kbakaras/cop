package ru.kbakaras.cop;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.Options;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.htmlcleaner.ContentNode;
import org.htmlcleaner.TagNode;
import org.slf4j.Logger;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "Confluence Publisher",
        versionProvider = VersionProvider.class,
        mixinStandardHelpOptions = true,
        sortOptions = false,
        showDefaultValues = true,
        subcommands = {
                PublishCommand.class,
                UpdateCommand.class
        })
public class ConfluencePublisher implements Callable<Integer> {

    @Option(names = {"-b", "--base-url"}, description = "Base url for Confluence", required = true)
    private String baseUrl;

    @Option(names = {"-l", "--login"}, description = "Confluence user login", required = true)
    private String login;

    @Option(names = {"-p", "--password", "--token"},
            description = "Confluence user password or access token",
            required = true,
            arity = "0..1",
            interactive = true)
    private char[] password;

    @Option(names = {"-a", "--attribute"}, description = "Attribute overrides")
    Map<String, Object> attributes;


    @SneakyThrows
    @Override
    public Integer call() {
        throw new IllegalArgumentException("Operation not supported yet");
    }


    void setContentValue(Content content, PageSource pageSource, String spaceKey, String parentId) {
        content.setTitle(pageSource.title);
        content.setType(Content.TYPE_Page);

        content.setSpace(new Space(spaceKey));
        Optional.ofNullable(parentId)
                .map(id -> new Ancestor[]{new Ancestor(id)})
                .ifPresent(content::setAncestors);

        ContentBodyValue contentValue = new ContentBodyValue(pageSource.getContent(), ContentBodyValue.REPRESENTATION_Storage);
        ContentBody contentBody = new ContentBody();
        contentBody.setStorage(contentValue);

        content.setBody(contentBody);
    }

    void checkListFileOrFileIsSupplied(File listFile, File file, Logger log) {

        if (listFile == null && file == null) {
            throw new IllegalArgumentException("Either [file] or [list-file] parameter has to be supplied");
        }

        if (listFile != null && file != null) {
            log.warn("Parameter [file] is ignored if [list-file] was supplied");
        }
    }

    Map<String, PageSource> convertTargets(UpdateTarget[] targets, String titlePrefix, Logger log, MutableBoolean stop) {

        Map<String, PageSource> result = new HashMap<>();

        for (UpdateTarget target : targets) {
            try {
                log.info("Running asciidoctor conversion of '{}'", target.file);
                result.put(target.pageId, convertPageSource(target.file, titlePrefix));

            } catch (Exception e) {
                log.error("Asciidoctor conversion of '" + target.file + "' failed", e);
                stop.setTrue();
            }
        }

        return result;
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
    PageSource convertPageSource(File file, String titlePrefix) throws IOException {

        String pageContentSource = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        // region Конвертация документа в формат хранения Confluence
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaConverterRegistry().register(ConfluenceConverter.class);
        asciidoctor.requireLibrary("asciidoctor-diagram");


        String pageTitle = Optional.ofNullable(titlePrefix).orElse("") + asciidoctor
                .load(pageContentSource, Options.builder().parseHeaderOnly(true).build())
                .getDoctitle();

        OptionsBuilder options = Options.builder()
                .backend("confluence")
                .baseDir(file.getAbsoluteFile().getParentFile())
                .toFile(false)
                .safe(SafeMode.UNSAFE);
        Optional.ofNullable(attributes)
                .map(Attributes.builder()::attributes)
                .map(AttributesBuilder::build)
                .ifPresent(options::attributes);

        String pageContent = asciidoctor.convert(pageContentSource, options.build());

        asciidoctor.shutdown();
        // endregion

        TagNode node = PageSource.cleanContent(pageContent);

        File attachmentDir = file.getAbsoluteFile().getParentFile();
        Map<File, AttachmentSource> attachments = new HashMap<>();

        List<? extends TagNode> linkNodes =
                node.getElementList(tagNode -> tagNode.getName().equals("a"), true);

        for (TagNode linkNode : linkNodes) {

            String href = linkNode.getAttributeByName("href");
            if (!isAbsoluteUri(href) && !isFragmentUri(href)) {

                File attachmentFile = new File(attachmentDir, href);
                TagNode riAttachment = new TagNode("ri:attachment");

                AttachmentSource attachmentSource = attachments
                        .computeIfAbsent(attachmentFile, AttachmentSource::new)
                        .addNode(riAttachment);

                riAttachment.addAttribute("ri:filename", attachmentSource.name);

                TagNode acLinkBody = new TagNode("ac:link-body");
                acLinkBody.addChild(new ContentNode(linkNode.getText().toString()));

                /* Такой вариант указан в описании формата хранения, так тоже можно.
                Но в самом начале так не получилось из-за комментариев вокруг CDATA.
                TagNode acLinkBody = new TagNode("ac:plain-text-link-body");
                acLinkBody.addChild(new CData(linkNode.getText().toString()));*/

                TagNode acLink = new TagNode("ac:link");
                acLink.addChild(riAttachment);
                acLink.addChild(acLinkBody);

                linkNode.getParent().insertChildBefore(linkNode, acLink);
                linkNode.getParent().removeChild(linkNode);
            }
        }

        List<? extends TagNode> imageNodes = node.getElementList(
                tagNode -> tagNode.getName().equals("ri:attachment") && tagNode.getParent().getName().equals("ac:image"),
                true);

        for (TagNode imageNode: imageNodes) {

            File imageFile = new File(attachmentDir, imageNode.getAttributeByName("ri:filename"));

            AttachmentSource attachmentSource = attachments
                    .computeIfAbsent(imageFile, AttachmentSource::new)
                    .addNode(imageNode);

            imageNode.removeAttribute("ri:filename");
            imageNode.addAttribute("ri:filename", attachmentSource.name);
        }

        return new PageSource(pageTitle, node, attachments.values());
    }

    ConfluenceApi confluenceApi() {
        return new ConfluenceApi(baseUrl,
                new SugarRestClient(new SugarRestIdentityBasic() {

                    @Override
                    public LoginPasswordDto getLoginAndPassword() {
                        return new LoginPasswordDto(login, String.valueOf(password));
                    }

                })
        );
    }


    public static void main(String... args) {
        int exitCode = new CommandLine(new ConfluencePublisher())
                .setExecutionExceptionHandler(new ExceptionHandler())
                .execute(args);
        System.exit(exitCode);
    }


    private static boolean isAbsoluteUri(String href) {
        //noinspection HttpUrlsUsage
        return href.startsWith("http://") || href.startsWith("https://");
    }

    @SneakyThrows(URISyntaxException.class)
    private static boolean isFragmentUri(String href) {
        return StringUtils.isNotBlank(new URI(href).getFragment());
    }

    private static class ExceptionHandler implements CommandLine.IExecutionExceptionHandler {

        @Override
        public int handleExecutionException(Exception e, CommandLine commandLine, CommandLine.ParseResult parseResult) {

            commandLine.getErr().println(
                    e.getClass().getSimpleName() +
                            Optional.ofNullable(e.getMessage()).map(m -> ":\n" + m).orElse(": ^^^"));
            return 1;
        }
    }

}
