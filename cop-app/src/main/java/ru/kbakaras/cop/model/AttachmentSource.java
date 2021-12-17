package ru.kbakaras.cop.model;

import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.htmlcleaner.TagNode;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class AttachmentSource {

    public final String name;
    public final String mime;
    public final byte[] data;
    public final String sha1;

    private final Set<TagNode> nodes = new HashSet<>();


    @SneakyThrows
    public AttachmentSource(File imageFile) {

        name = imageFile.getName();
        mime = new Tika().detect(imageFile);
        data = FileUtils.readFileToByteArray(imageFile);
        sha1 = DigestUtils.sha1Hex(data);
    }

    public AttachmentSource addNode(TagNode node) {

        this.nodes.add(node);
        return this;
    }

    public void setVersionAtSave(int number) {

        String versionAtSave = String.format("%d", number);
        nodes.forEach(node -> node.addAttribute("ri:version-at-save", versionAtSave));
    }

}
