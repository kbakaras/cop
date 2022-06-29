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
                    "Supplied [list-file] '{0}' cannot be read", listFile));
        }

        return checkForClash(YAML_MAPPER.get().readValue(listFile, UpdateTarget[].class));
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
                    "Supplied [file] '{0}' cannot be read", file));
        }

        return file;
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
