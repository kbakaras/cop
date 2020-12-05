package ru.kbakaras.cop.adoc.model;

import org.apache.commons.codec.digest.DigestUtils;
import ru.kbakaras.cop.confluence.dto.Attachment;

public class ImageDestination {

    public final String name;
    public final Attachment attachment;
    public final String sha1;


    public ImageDestination(Attachment attachment, byte[] imageData) {
        this.name = attachment.getTitle();
        this.attachment = attachment;
        this.sha1 = DigestUtils.sha1Hex(imageData);
    }

}
