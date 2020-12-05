package ru.kbakaras.cop.adoc.model;

import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;

public class ImageSource {

    public final String name;
    public final byte[] data;
    public final String sha1;

    @SneakyThrows
    public ImageSource(File imageFile) {
        name = imageFile.getName();
        data = FileUtils.readFileToByteArray(imageFile);
        sha1 = DigestUtils.sha1Hex(data);
    }

}
