package ru.kbakaras.cop;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import ru.kbakaras.cop.confluence.ConfluenceApi;
import ru.kbakaras.cop.confluence.dto.Content;
import ru.kbakaras.cop.confluence.dto.ContentList;
import ru.kbakaras.cop.model.PageSource;

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


    @Override
    public Integer call() throws Exception {

        // Конвертация исходной страницы в формат хранения Confluence
        PageSource pageSource = parent.convertPageSource();

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
            Content content = new Content();
            parent.setContentValue(content, pageSource);

            Content newContent = api.createContent(content);
            if (pageSource.differentContent(newContent.getBody().getStorage().getValue())) {
                log.warn("SHA1 of published content differs from converted, check converter");
            }
            // endregion


            // region Загрузка изображений
            pageSource
                    .imageSourceList
                    .forEach(imageSource -> {
                        try {
                            api.createAttachment(newContent.getId(), imageSource.name, imageSource.data);
                        } catch (URISyntaxException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            // endregion

        }

        return 0;
    }

}
