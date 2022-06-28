package ru.kbakaras.cop;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import ru.kbakaras.cop.confluence.ConfluenceApi;
import ru.kbakaras.cop.confluence.dto.Content;
import ru.kbakaras.cop.confluence.dto.ContentList;
import ru.kbakaras.cop.model.PageSource;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "publish",
        mixinStandardHelpOptions = true,
        header = "Operation for initial publication of page to Confluence"
)
@Slf4j
public class PublishCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private ConfluencePublisher parent;

    @CommandLine.Option(names = {"-f", "--file"}, description = "Path to file with page to publish", required = true)
    private File file;

    @CommandLine.Option(names = {"-r", "--parent-id"}, description = "Confluence's parent page id", required = true)
    String parentId;


    @Override
    public Integer call() throws Exception {

        // Конвертация исходной страницы в формат хранения Confluence
        PageSource pageSource = parent.convertPageSource(file);

        try (ConfluenceApi api = parent.confluenceApi()) {

            // region Поиск в пространстве Confluence страницы с таким же заголовком
            ContentList contentList = api.findContentByTitle(pageSource.title);
            if (contentList.getSize() > 0) {
                throw new IllegalArgumentException(String.format(
                        "Page with same title ('%s') already exists in space '%s'",
                        pageSource.title, parent.spaceKey));
            }
            // endregion


            // region Создание страницы
            pageSource
                    .attachmentSourceList
                    .forEach(attachmentSource -> attachmentSource.setVersionAtSave(1));

            Content content = new Content();
            parent.setContentValue(content, pageSource, parentId);

            Content newContent = api.createContent(content);
            if (pageSource.differentContent(newContent.getBody().getStorage().getValue())) {
                log.warn("SHA1 of published content differs from converted, check converter");
            }

            api.setDefaultAppearance(newContent);
            // endregion


            // region Загрузка изображений
            pageSource
                    .attachmentSourceList
                    .forEach(imageSource -> {
                        try {
                            api.createAttachment(newContent.getId(), imageSource.name, imageSource.mime, imageSource.data);
                        } catch (URISyntaxException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            // endregion

        }

        return 0;
    }

}
