package ru.kbakaras.cop;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import ru.kbakaras.cop.confluence.ConfluenceApi;
import ru.kbakaras.cop.confluence.dto.Attachment;
import ru.kbakaras.cop.confluence.dto.Content;
import ru.kbakaras.cop.model.AttachmentDestination;
import ru.kbakaras.cop.model.AttachmentSource;
import ru.kbakaras.cop.model.PageSource;
import ru.kbakaras.sugar.lazy.Lazy;
import ru.kbakaras.sugar.utils.CollectionUpdater;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(
        name = "update",
        mixinStandardHelpOptions = true,
        header = "Operation to update previously published page")
@Slf4j
public class UpdateCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private ConfluencePublisher parent;

    @CommandLine.Option(names = {"--list-file"}, description = "Path to file with list of files to publish")
    private File listFile;

    @CommandLine.Option(names = {"-f", "--file"}, description = "Path to file with page to publish")
    private File file;

    @CommandLine.Option(names = {"-i", "--page-id"}, description = "Confluence's page id")
    private String pageId;

    @CommandLine.Option(names = {"-r", "--parent-id"}, description = "Confluence's parent page id")
    String parentId;

    private final Lazy<ObjectMapper> yamlMapper = Lazy
            .of(() -> new ObjectMapper(new YAMLFactory()).findAndRegisterModules());


    @Override
    public Integer call() throws Exception {

        if (listFile == null && file == null) {
            throw new IllegalArgumentException("Either [file] or [list-file] parameter has to be supplied");
        }

        if (listFile != null && file != null) {
            log.warn("Parameter [file] is ignored if [list-file] was supplied");
        }

        UpdateTarget[] targets;

        // region Формирование массива файлов для обновления страниц
        if (listFile != null) {
            if (!listFile.isFile()) {
                throw new IllegalArgumentException(MessageFormat.format(
                        "Supplied [list-file] '{0}' can not be read", listFile));
            }

            targets = yamlMapper.get().readValue(listFile, UpdateTarget[].class);
            checkForClash(targets).ifPresent(message -> {
                throw new IllegalArgumentException(message);
            });

        } else {
            if (pageId.isBlank()) {
                throw new IllegalArgumentException("The [pageId] parameter has to be supplied");
            }

            targets = new UpdateTarget[]{new UpdateTarget(file, pageId)};
        }
        // endregion

        boolean stop = false;

        Map<String, PageSource> newPages = new HashMap<>();

        // region конвертация страниц в формат хранения Confluence
        for (UpdateTarget target : targets) {
            try {
                log.info("Running asciidoctor conversion of '{}' for pageId={}", target.file, target.pageId);
                newPages.put(target.pageId, parent.convertPageSource(target.file));

            } catch (Exception e) {
                log.error("Asciidoctor conversion of '" + target.file + "' failed", e);
                stop = true;
            }
        }
        // endregion

        try (ConfluenceApi api = parent.confluenceApi()) {

            Map<String, Content> oldPages = new HashMap<>();

            // region Получение из Confluence страниц с текущим содержимым
            for (UpdateTarget target : targets) {
                try {
                    log.info("Getting old content of '{}' for pageId={}", target.file, target.pageId);
                    oldPages.put(target.pageId, api.getContentById(target.pageId));

                } catch (Exception e) {
                    log.error("Unable to fetch old content from Confluence by pageId=" + target.pageId, e);
                    stop = true;
                }
            }
            // endregion

            if (stop) {
                throw new IllegalArgumentException();
            }

            for (UpdateTarget target : targets) {

                Content oldContent = oldPages.get(target.pageId);
                PageSource pageSource = newPages.get(target.pageId);

                log.info("Updating publication of '{}' for pageId={}", target.file, target.pageId);

                // region Обновление изображений (вложений)
                List<AttachmentDestination> destinationImages = new ArrayList<>();
                for (Attachment attachment : api.findAttachmentByContentId(oldContent.getId()).getResults()) {
                    destinationImages.add(new AttachmentDestination(attachment, api.getAttachmentData(attachment)));
                }

                new CollectionUpdater<AttachmentDestination, AttachmentSource, String>(id -> id.name, is -> is.name)

                        .check4Changes((id, is) -> {
                            if (id.sha1.equals(is.sha1)) {
                                is.setVersionAtSave(id.attachment.getVersion().getNumber());
                                return false;
                            }
                            return true;
                        })

                        .createElement(is -> {
                            try {
                                log.info("  publishing new attachment '{}'", is.name);
                                api.createAttachment(oldContent.getId(), is.name, is.mime, is.data);
                                is.setVersionAtSave(1);
                            } catch (URISyntaxException | IOException e) {
                                throw new RuntimeException(e);
                            }
                        })

                        .updateElement((id, is) -> {
                            try {
                                log.info("  updating attachment '{}'", is.name);
                                api.updateAttachmentData(oldContent.getId(), id.attachment, is.data);
                                is.setVersionAtSave(id.attachment.getVersion().getNumber() + 1);
                            } catch (URISyntaxException | IOException e) {
                                throw new RuntimeException(e);
                            }
                        })

                        .collection(destinationImages, pageSource.attachmentSourceList);
                // endregion

                // region Обновление основного содержимого страницы
                if (!pageSource.title.equals(oldContent.getTitle())
                        || pageSource.differentContent(oldContent.getBody().getStorage().getValue())) {
                    log.info("  updating page content");
                    Content content = new Content();
                    content.setVersion(oldContent.getVersion());
                    content.getVersion().setNumber(content.getVersion().getNumber() + 1);
                    parent.setContentValue(content, pageSource, parentId);

                    content = api.updateContent(oldContent.getId(), content);

                    if (pageSource.differentContent(content.getBody().getStorage().getValue())) {
                        log.warn("  SHA1 of updated content differs from converted, check converter");
                    }

                    if (!pageSource.title.equals(oldContent.getTitle())) {
                        log.info("  RENAMED: {}", oldContent.getTitle());
                        log.info("  -------> {}", content.getTitle());
                    }
                }
                // endregion

                api.setDefaultAppearance(oldContent);
            }

        }

        return 0;
    }

    private Optional<String> checkForClash(UpdateTarget[] targets) {

        Set<String> pages = new HashSet<>();
        Set<String> clash = new HashSet<>();
        for (UpdateTarget target : targets) {
            if (pages.contains(target.pageId)) {
                clash.add(target.pageId);
            } else {
                pages.add(target.pageId);
            }
        }

        if (!clash.isEmpty()) {
            return Optional.of(clash.stream()
                    .map(id -> "Same pageId=" + id + " is configured for this files:\n" + Arrays
                            .stream(targets)
                            .filter(target -> id.equals(target.pageId))
                            .map(target -> "  " + target.file.getPath())
                            .collect(Collectors.joining("\n")))
                    .collect(Collectors.joining("\n")));
        }

        return Optional.empty();
    }


    public static class UpdateTarget {
        public final File file;
        public final String pageId;

        @JsonCreator
        UpdateTarget(
                @JsonProperty("file")
                File file,
                @JsonProperty("pageId")
                String pageId) {

            this.file = file;
            this.pageId = pageId;
        }

    }

}
