package ru.kbakaras.cop.model;

import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;

import java.io.File;

public class AttachmentSource {

    public final String name;
    public final String mime;
    public final byte[] data;
    public final String sha1;

    @SneakyThrows
    public AttachmentSource(File imageFile) {

        name = imageFile.getName();
        mime = new Tika().detect(imageFile);
        data = FileUtils.readFileToByteArray(imageFile);
        sha1 = DigestUtils.sha1Hex(data);
    }

}
