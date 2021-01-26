package ru.kbakaras.cop.confluence.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Content {

    public static final String TYPE_Page      = "page";
    public static final String STATUS_Current = "current";

    private String id;
    private String type;
    private String status;
    private String title;

    private Space space;

    private ContentVersion version;

    private Ancestor[] ancestors;

    private ContentBody body;


    public String sha1() {

        HtmlCleaner cleaner = new HtmlCleaner();

        CleanerProperties props = cleaner.getProperties();
        props.setOmitHtmlEnvelope(true);
        props.setOmitXmlDeclaration(true);

        TagNode node = cleaner.clean(body.getStorage().getValue());

        return DigestUtils.sha1Hex(new PrettyXmlSerializer(cleaner.getProperties()).getAsString(node));
    }

}
