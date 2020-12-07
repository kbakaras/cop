package ru.kbakaras.cop.model;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.List;

public class PageSource {

    public final String title;
    public final String content;
    public final String sha1;

    public final List<ImageSource> imageSourceList;


    public PageSource(String title, String content, List<ImageSource> imageSourceList) {
        this.title = title;
        this.content = content;
        this.sha1 = DigestUtils.sha1Hex(content);
        this.imageSourceList = imageSourceList;
    }

}
