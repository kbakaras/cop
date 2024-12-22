package ru.kbakaras.cop.confluence;

import org.apache.http.HttpEntity;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import ru.kbakaras.cop.confluence.dto.Attachment;
import ru.kbakaras.cop.confluence.dto.AttachmentList;
import ru.kbakaras.cop.confluence.dto.Content;
import ru.kbakaras.cop.confluence.dto.ContentList;
import ru.kbakaras.cop.confluence.dto.ContentProperty;
import ru.kbakaras.sugar.restclient.SugarRestClient;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class ConfluenceApi implements Closeable {

    private static final String EXPAND_CONTENT = "body.storage,version,container,metadata.properties.content_appearance_published,metadata.properties.content_appearance_draft";

    private final String baseUrl;
    private final SugarRestClient client;


    public ConfluenceApi(String baseUrl, SugarRestClient client) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.client = client;
    }


    public ContentList findContentByTitle(String spaceKey, String pageTitle) throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder(baseUrl + "/rest/api/content")
                .addParameter("spaceKey", spaceKey)
                .addParameter("title", pageTitle)
                .addParameter("expand", "space,body.view,body.storage,version,container");

        SugarRestClient.Response response = client.get(uriBuilder.toString());

        response.assertStatusCode(200);
        return response.getEntity(ContentList.class);
    }

    /**
     * Получить страницу по идентификатору. Если страница по указанному идентификатору отсутствует, возвращает null.
     */
    public Content getContentById(String contentId) throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder(baseUrl + "/rest/api/content/" + contentId)
                .addParameter("expand", EXPAND_CONTENT);

        SugarRestClient.Response response = client.get(uriBuilder.toString());

        if (response.httpResponse.getStatusLine().getStatusCode() == 404) {
            return null;
        }

        response.assertStatusCode(200);
        return response.getEntity(Content.class);
    }

    public AttachmentList findAttachmentByContentId(String contentId) throws URISyntaxException, IOException {

        URIBuilder uriBuilder =
                new URIBuilder(String.format(baseUrl + "/rest/api/content/%s/child/attachment", contentId))
                        .addParameter("expand", "version");

        SugarRestClient.Response response = client.get(uriBuilder.toString());

        response.assertStatusCode(200);
        return response.getEntity(AttachmentList.class);
    }

    public byte[] getAttachmentData(Attachment attachment) throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder(baseUrl + attachment.getLinks().getDownload());

        SugarRestClient.Response response = client.get(uriBuilder.toString());

        response.assertStatusCode(200);
        return response.getEntityData();
    }

    public void updateAttachmentData(String contentId, Attachment attachment, byte[] data) throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder(String.format(
                baseUrl + "/rest/api/content/%s/child/attachment/%s/data",
                contentId, attachment.getId()));
        HttpEntity entity = MultipartEntityBuilder
                .create()
                .addBinaryBody("file", data)
                .build();

        SugarRestClient.Response response = client.post(uriBuilder.toString(), entity, "X-Atlassian-Token: nocheck");

        response.assertStatusCode(200);
    }

    public void createAttachment(String contentId, String fileName, String fileMime, byte[] data) throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder(String.format(
                baseUrl + "/rest/api/content/%s/child/attachment", contentId));
        HttpEntity entity = MultipartEntityBuilder
                .create()
                .addBinaryBody("file", data, ContentType.parse(fileMime), fileName)
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .setCharset(StandardCharsets.UTF_8)
                .build();

        SugarRestClient.Response response = client.post(uriBuilder.toString(), entity, "X-Atlassian-Token: nocheck");

        response.assertStatusCode(200);
    }

    public Content updateContent(String contentId, Content content) throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder(baseUrl + "/rest/api/content/" + contentId)
                .addParameter("expand", EXPAND_CONTENT);

        SugarRestClient.Response response = client.put(uriBuilder.toString(), content);

        response.assertStatusCode(200);
        return response.getEntity(Content.class);
    }

    public Content createContent(Content content) throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder(baseUrl + "/rest/api/content")
                .addParameter("expand", EXPAND_CONTENT);

        SugarRestClient.Response response = client.post(uriBuilder.toString(), content);

        response.assertStatusCode(200);
        return response.getEntity(Content.class);
    }

    public void updateProperty(String contentId, ContentProperty property) throws URISyntaxException, IOException {

        URIBuilder uriBuilder = new URIBuilder(baseUrl + "/rest/api/content/" + contentId + "/property/" + property.getKey());

        SugarRestClient.Response response = client.put(uriBuilder.toString(), property);

        response.assertStatusCode(200);
    }

    /**
     * Обновление параметра, задающего режим отображения.
     */
    public void setDefaultAppearance(Content content) throws URISyntaxException, IOException {

        ContentProperty contentAppearance = content
                .getMetadata().getProperties().get(ContentProperty.CONTENT_APPEARANCE_PUBLISHED);

        if (contentAppearance != null
                && !ContentProperty.CONTENT_APPEARANCE_VALUE_DEFAULT.equals(contentAppearance.getValue())) {

            updateProperty(content.getId(),
                    contentAppearance.getUpdatedProperty(ContentProperty.CONTENT_APPEARANCE_VALUE_DEFAULT));
        }
    }


    public void trashContentById(String contentId) throws URISyntaxException, IOException {

        URIBuilder uriBuilder = new URIBuilder(baseUrl + "/rest/api/content/" + contentId);

        SugarRestClient.Response response = client.delete(uriBuilder.toString());

        response.assertStatusCode(204);
    }

    public void purgeContentById(String contentId) throws URISyntaxException, IOException {

        URIBuilder uriBuilder = new URIBuilder(baseUrl + "/rest/api/content/" + contentId)
                .addParameter("status", "trashed");

        SugarRestClient.Response response = client.delete(uriBuilder.toString());

        response.assertStatusCode(204);
    }

    private static String normalizeBaseUrl(String url) {
        return url.endsWith("/")
                ? url.substring(0, url.length() - 1)
                : url;
    }


    @Override
    public void close() {
        client.close();
    }

}
