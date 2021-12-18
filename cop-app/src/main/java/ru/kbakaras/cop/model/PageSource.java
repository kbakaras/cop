package ru.kbakaras.cop.model;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleXmlSerializer;
import org.htmlcleaner.TagNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PageSource {

    public final String title;

    private final TagNode content;

    public final List<AttachmentSource> attachmentSourceList;


    public PageSource(String title, TagNode content, Collection<AttachmentSource> attachmentSourceList) {
        this.title = title;
        this.content = content;
        this.attachmentSourceList = new ArrayList<>(attachmentSourceList);
    }

    public String getContent() {
        return serializeContent(content);
    }

    public boolean differentContent(String htmlContent) {
        return !DigestUtils.sha1Hex(getContent())
                .equals(DigestUtils.sha1Hex(serializeContent(cleanContent(htmlContent))));
    }


    public static TagNode cleanContent(String htmlContent) {

        HtmlCleaner cleaner = new HtmlCleaner();
        setProperties(cleaner)
                .setDeserializeEntities(true);

        return cleaner.clean(htmlContent);
    }

    public static String serializeContent(TagNode node) {

        HtmlCleaner cleaner = new HtmlCleaner();
        setProperties(cleaner);


        return StringUtils.chomp(
                new SimpleXmlSerializer(cleaner.getProperties()).getAsString(node)
        );
    }

    public static CleanerProperties setProperties(HtmlCleaner cleaner) {
        CleanerProperties props = cleaner.getProperties();
        props.setOmitHtmlEnvelope(true);
        props.setOmitXmlDeclaration(true);
        props.setCopCdata(true);
        props.setUseCdataFor("ac:plain-text-body,ac:plain-text-link-body");
        return props;
    }

}
