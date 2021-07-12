package ru.kbakaras.cop.model;

import org.apache.commons.codec.digest.DigestUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyHtmlSerializer;
import org.htmlcleaner.TagNode;

import java.util.List;

public class PageSource {

    public final String title;
    public final String content;
    private final String sha1;

    public final List<ImageSource> imageSourceList;


    public PageSource(String title, TagNode content, List<ImageSource> imageSourceList) {
        this.title = title;
        this.content = serializeContent(content);
        this.sha1 = DigestUtils.sha1Hex(this.content);
        this.imageSourceList = imageSourceList;
    }

    public boolean differentContent(String htmlContent) {
        return !sha1.equals(DigestUtils.sha1Hex(serializeContent(cleanContent(htmlContent))));
    }


    public static TagNode cleanContent(String htmlContent) {

        HtmlCleaner cleaner = new HtmlCleaner();

        CleanerProperties props = cleaner.getProperties();
        props.setOmitHtmlEnvelope(true);
        props.setOmitXmlDeclaration(true);
        props.setDeserializeEntities(true);

        return cleaner.clean(htmlContent);
    }

    public static String serializeContent(TagNode node) {

        HtmlCleaner cleaner = new HtmlCleaner();

        CleanerProperties props = cleaner.getProperties();
        props.setOmitHtmlEnvelope(true);
        props.setOmitXmlDeclaration(true);

        return new PrettyHtmlSerializer(cleaner.getProperties()).getAsString(node);
    }

}
