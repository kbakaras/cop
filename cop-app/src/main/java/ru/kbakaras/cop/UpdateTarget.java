package ru.kbakaras.cop;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import ru.kbakaras.sugar.lazy.Lazy;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class UpdateTarget {

    private static final Lazy<ObjectMapper> YAML_MAPPER = Lazy
            .of(() -> new ObjectMapper(new YAMLFactory()).findAndRegisterModules());


    public final File file;
    public final String pageId;

    @JsonCreator
    private UpdateTarget(
            @JsonProperty("file")
            File file,
            @JsonProperty("pageId")
            String pageId) {

        this.file = file;
        this.pageId = pageId;
    }


    public static UpdateTarget[] readTargets(File listFile) throws IOException {

        if (!listFile.isFile()) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "Supplied [list-file] ''{0}'' cannot be read", listFile));
        }

        UpdateTarget[] targets = YAML_MAPPER.get().readValue(listFile, UpdateTarget[].class);

        File baseDir = listFile.getAbsoluteFile().getParentFile();
        for (int i = 0; i < targets.length; i++) {
            if (!targets[i].file.isAbsolute()) {
                targets[i] = new UpdateTarget(
                        new File(baseDir, targets[i].file.getPath()).getCanonicalFile(),
                        targets[i].pageId);
            }
        }

        return checkForClash(checkFiles(targets));
    }

    public static UpdateTarget[] publishTarget(File file) {
        return new UpdateTarget[]{new UpdateTarget(checkFile(file), null)};
    }

    public static UpdateTarget[] updateTarget(File file, String pageId) {

        if (pageId.isBlank()) {
            throw new IllegalArgumentException("The [pageId] parameter has to be supplied");
        }

        return new UpdateTarget[]{new UpdateTarget(checkFile(file), pageId)};
    }


    private static File checkFile(File file) {

        if (!file.isFile()) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "Page file ''{0}'' cannot be read", file));
        }

        return file;
    }

    private static UpdateTarget[] checkFiles(UpdateTarget[] targets) {

        String message = Arrays
                .stream(targets)
                .map(target -> target.file)
                .filter(file -> !file.isFile())
                .map(file -> MessageFormat.format("  File ''{0}'' cannot be read", file))
                .collect(Collectors.joining("\n"));

        if (!message.isBlank()) {
            throw new IllegalArgumentException("Some page files cannot be read:\n" + message);
        }

        return targets;
    }

    private static UpdateTarget[] checkForClash(UpdateTarget[] targets) {

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
            String message = clash.stream()
                    .map(id -> "Same pageId=" + id + " is configured for this files:\n" + Arrays
                            .stream(targets)
                            .filter(target -> id.equals(target.pageId))
                            .map(target -> "  " + target.file.getPath())
                            .collect(Collectors.joining("\n")))
                    .collect(Collectors.joining("\n"));

            throw new IllegalArgumentException(message);
        }

        return targets;
    }

}
