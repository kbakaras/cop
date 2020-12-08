package ru.kbakaras.cop;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import ru.kbakaras.cop.confluence.ConfluenceApi;
import ru.kbakaras.cop.confluence.dto.Attachment;
import ru.kbakaras.cop.confluence.dto.Content;
import ru.kbakaras.cop.confluence.dto.ContentList;
import ru.kbakaras.cop.model.ImageDestination;
import ru.kbakaras.cop.model.ImageSource;
import ru.kbakaras.cop.model.PageSource;
import ru.kbakaras.sugar.utils.CollectionUpdater;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "update",
        mixinStandardHelpOptions = true,
        header = "Operation to update previously published page"
)
@Slf4j
public class UpdateCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private ConfluencePublisher parent;

    @CommandLine.Option(names = {"-i", "--page-id"}, description = "Confluence's page id")
    private String pageId;


    @Override
    public Integer call() throws Exception {

        // Конвертация исходной страницы в формат хранения Confluence
        PageSource pageSource = parent.convertPageSource();

        try (ConfluenceApi api = parent.confluenceApi()) {

            // region Получение страницы Confluence
            Content oldContent;

            if (pageId != null) {
                oldContent = api.getContentById(pageId);

                if (oldContent == null) {
                    throw new IllegalArgumentException(String.format(
                            "Confluence page not found by provided id '%s'", pageId));
                }

            } else {
                ContentList contentList = api.findContentByTitle(pageSource.title);

                if (contentList.getSize() == 0) {
                    throw new IllegalArgumentException(String.format(
                            "Confluence page not found by title '%s'", pageSource.title));
                }

                oldContent = contentList.getResults()[0];
            }
            // endregion


            // region Обновление основного содержимого страницы
            if (!pageSource.sha1.equals(oldContent.sha1())) {
                Content content = new Content();
                content.setVersion(oldContent.getVersion());
                content.getVersion().setNumber(content.getVersion().getNumber() + 1);
                parent.setContentValue(content, pageSource);

                content = api.updateContent(oldContent.getId(), content);
                if (!pageSource.sha1.equals(content.sha1())) {
                    log.warn("SHA1 of updated content differs from converted, check converter");
                }
            }
            // endregion


            // region Обновление изображений (вложений)
            List<ImageDestination> destinationImages = new ArrayList<>();
            for (Attachment attachment : api.findAttachmentByContentId(oldContent.getId()).getResults()) {
                destinationImages.add(new ImageDestination(attachment, api.getAttachmentData(attachment)));
            }

            new CollectionUpdater<ImageDestination, ImageSource, String>(id -> id.name, is -> is.name)

                    .check4Changes((id, is) -> !id.sha1.equals(is.sha1))

                    .createElement(is -> {
                        try {
                            api.createAttachment(oldContent.getId(), is.name, is.data);
                        } catch (URISyntaxException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    })

                    .updateElement((id, is) -> {
                        try {
                            api.updateAttachmentData(oldContent.getId(), id.attachment, is.data);
                        } catch (URISyntaxException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    })

                    .collection(destinationImages, pageSource.imageSourceList);
            // endregion

        }

        return 0;
    }

}
