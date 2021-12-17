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
import ru.kbakaras.sugar.restclient.SugarRestClient;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class ConfluenceApi implements Closeable {

    private final String baseUrl;
    private final String spaceKey;
    private final SugarRestClient client;


    public ConfluenceApi(String baseUrl, String spaceKey, SugarRestClient client) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.spaceKey = spaceKey;
        this.client = client;
    }


    public ContentList findContentByTitle(String pageTitle) throws URISyntaxException, IOException {
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
                .addParameter("expand", "body.storage,version,container");

        SugarRestClient.Response response = client.get(uriBuilder.toString());

        if (response.httpResponse.getStatusLine().getStatusCode() == 404) {
            return null;
        }

        response.assertStatusCode(200);
        return response.getEntity(Content.class);
    }

    public AttachmentList findAttachmentByContentId(String contentId) throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder(String.format(
                baseUrl + "/rest/api/content/%s/child/attachment",
                contentId));

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
        URIBuilder uriBuilder = new URIBuilder(baseUrl + "/rest/api/content/" + contentId);

        SugarRestClient.Response response = client.put(uriBuilder.toString(), content);

        response.assertStatusCode(200);
        return response.getEntity(Content.class);
    }

    public Content createContent(Content content) throws URISyntaxException, IOException {
        URIBuilder uriBuilder = new URIBuilder(baseUrl + "/rest/api/content");

        SugarRestClient.Response response = client.post(uriBuilder.toString(), content);

        response.assertStatusCode(200);
        return response.getEntity(Content.class);
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
