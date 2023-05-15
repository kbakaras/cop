package ru.kbakaras.cop;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.mutable.MutableBoolean;
import picocli.CommandLine;
import ru.kbakaras.cop.confluence.ConfluenceApi;
import ru.kbakaras.cop.confluence.dto.Content;
import ru.kbakaras.cop.confluence.dto.ContentList;
import ru.kbakaras.cop.model.PageSource;
import ru.kbakaras.sugar.restclient.StatusAssertionFailed;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@CommandLine.Command(
        name = "publish",
        mixinStandardHelpOptions = true,
        header = "Operation for initial publication of page to Confluence"
)
@Slf4j
public class PublishCommand implements Callable<Integer> {

    private static final int ATTEMPT_COUNT = 3;
    private static final long ATTEMPT_SLEEP_SECONDS = 2;


    @CommandLine.ParentCommand
    private ConfluencePublisher parent;

    @CommandLine.Option(names = {"--list-file"}, description = "Path to file with list of files to publish")
    private File listFile;

    @CommandLine.Option(names = {"-f", "--file"}, description = "Path to file with page to publish")
    private File file;

    @CommandLine.Option(names = {"-r", "--parent-id"}, description = "Confluence's parent page id")
    private String parentId;

    @CommandLine.Option(names = {"-p", "--title-prefix"}, description = "Prefix to add to title of page being published")
    private String titlePrefix;

    @CommandLine.Option(names = {"--test-run"}, description = "Delete published page if --test-run flag is set", defaultValue = "false")
    private boolean testRun;


    @Override
    public Integer call() throws Exception {

        parent.checkListFileOrFileIsSupplied(listFile, file, log);

        UpdateTarget[] targets = listFile != null
                ? UpdateTarget.readTargets(listFile)
                : UpdateTarget.publishTarget(file);

        MutableBoolean stop = new MutableBoolean(false);

        Map<String, PageSource> newPages = parent.convertTargets(targets, titlePrefix, log, stop);

        try (ConfluenceApi api = parent.confluenceApi()) {

            // region Поиск в пространстве Confluence страниц с совпадающими заголовками
            for (UpdateTarget target : targets) {

                PageSource pageSource = newPages.get(target.pageId);

                ContentList contentList = api.findContentByTitle(pageSource.title);
                if (contentList.getSize() > 0) {
                    log.error("Page with same title as '" + pageSource.title + "' already exists in space " + parent.spaceKey);
                    stop.setTrue();
                }
            }
            // endregion

            if (stop.booleanValue()) {
                throw new IllegalArgumentException();
            }

            for (UpdateTarget target : targets) {

                PageSource pageSource = newPages.get(target.pageId);
                log.info("Publishing page '{}' from '{}'", pageSource.title, target.file);

                // region Создание страницы
                pageSource
                        .attachmentSourceList
                        .forEach(attachmentSource -> attachmentSource.setVersionAtSave(1));

                Content content = new Content();
                parent.setContentValue(content, pageSource, parentId);

                Content newContent = api.createContent(content);
                if (pageSource.differentContent(newContent.getBody().getStorage().getValue())) {
                    log.warn("  SHA1 of published content differs from converted, check converter");
                }
                // endregion

                // region Загрузка изображений
                pageSource
                        .attachmentSourceList
                        .forEach(is -> {
                            try {
                                log.info("  publishing new attachment '{}'", is.name);
                                api.createAttachment(newContent.getId(), is.name, is.mime, is.data);
                            } catch (URISyntaxException | IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                // endregion

                api.setDefaultAppearance(newContent);

                if (testRun) {
                    log.info("  TEST RUN: trashing published page '{}'", newContent.getTitle());
                    api.trashContentById(newContent.getId());

                    for (int attempt = 1; attempt <= ATTEMPT_COUNT; attempt++) {

                        // Иногда Confluence отказывает (возвращает ошибку со статусом 500) на шаге purge.
                        // Поэтому тут предпринимается несколько попыток выполнить purge. И даже если ни
                        // одна из них не увенчается успехом, это не критично для сборки и можно игнорировать.

                        try {
                            log.info("  TEST RUN:  purging published page '{}'", newContent.getTitle());
                            api.purgeContentById(newContent.getId());
                            break;

                        } catch (StatusAssertionFailed e) {
                            log.warn("  TEST RUN:  purging published page '{}' failed", newContent.getTitle(), e);

                            try {
                                TimeUnit.SECONDS.sleep(ATTEMPT_SLEEP_SECONDS);
                            } catch (InterruptedException ignore) {}
                        }
                    }
                }
            }
        }

        return 0;
    }

}
