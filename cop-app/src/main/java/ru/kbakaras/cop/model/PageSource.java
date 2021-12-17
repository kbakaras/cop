package ru.kbakaras.cop.model;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.SimpleXmlSerializer;
import org.htmlcleaner.TagNode;

import java.util.List;

public class PageSource {

    public final String title;
    public final String content;
    private final String sha1;

    public final List<AttachmentSource> attachmentSourceList;


    public PageSource(String title, TagNode content, List<AttachmentSource> attachmentSourceList) {
        this.title = title;
        this.content = serializeContent(content);
        this.sha1 = DigestUtils.sha1Hex(this.content);
        this.attachmentSourceList = attachmentSourceList;
    }

    public boolean differentContent(String htmlContent) {
        return !sha1.equals(DigestUtils.sha1Hex(serializeContent(cleanContent(htmlContent))));
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
        props.setUseCdataFor("ac:plain-text-body");
        return props;
    }

}
