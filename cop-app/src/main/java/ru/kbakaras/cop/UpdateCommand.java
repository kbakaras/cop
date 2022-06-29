package ru.kbakaras.cop;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.mutable.MutableBoolean;
import picocli.CommandLine;
import ru.kbakaras.cop.confluence.ConfluenceApi;
import ru.kbakaras.cop.confluence.dto.Attachment;
import ru.kbakaras.cop.confluence.dto.Content;
import ru.kbakaras.cop.model.AttachmentDestination;
import ru.kbakaras.cop.model.AttachmentSource;
import ru.kbakaras.cop.model.PageSource;
import ru.kbakaras.sugar.utils.CollectionUpdater;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

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


    @Override
    public Integer call() throws Exception {

        parent.checkListFileOrFileIsSupplied(listFile, file, log);

        UpdateTarget[] targets = listFile != null
                ? UpdateTarget.readTargets(listFile)
                : UpdateTarget.updateTarget(file, pageId);

        MutableBoolean stop = new MutableBoolean(false);

        Map<String, PageSource> newPages = parent.convertTargets(targets, null, log, stop);

        try (ConfluenceApi api = parent.confluenceApi()) {

            Map<String, Content> oldPages = new HashMap<>();

            // region Получение из Confluence страниц с текущим содержимым
            for (UpdateTarget target : targets) {
                try {
                    log.info("Getting old content of '{}' for pageId={}", target.file, target.pageId);
                    oldPages.put(target.pageId, api.getContentById(target.pageId));

                } catch (Exception e) {
                    log.error("Unable to fetch old content from Confluence by pageId=" + target.pageId, e);
                    stop.setTrue();
                }
            }
            // endregion

            if (stop.booleanValue()) {
                throw new IllegalArgumentException();
            }

            for (UpdateTarget target : targets) {

                Content oldContent = oldPages.get(target.pageId);
                PageSource pageSource = newPages.get(target.pageId);
                log.info("Updating publication of page '{}' for pageId={}", pageSource.title, target.pageId);

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
                    parent.setContentValue(content, pageSource, null);

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

}
