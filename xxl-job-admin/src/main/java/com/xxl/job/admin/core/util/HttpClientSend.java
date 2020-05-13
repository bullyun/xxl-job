package com.xxl.job.admin.core.util;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by admin on 2017/7/12.
 */
public class HttpClientSend {

    private static Logger logger = LoggerFactory.getLogger(HttpClientSend.class);

    public static String postsend(String url, String content) {
        HttpPost httpPost = new HttpPost(url);
        Map<String, String> mp = new HashMap<>();
        Header[] headerss = buildHeader(mp);
        httpPost.setHeaders(headerss);
        CloseableHttpClient client = HttpService.getHttpClient();
        StringEntity entity = new StringEntity(content, "UTF-8");
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        String result = "";
        try (CloseableHttpResponse response = client.execute(httpPost)) {
            result = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            logger.warn("httpClient", e);
        }
        return result;
    }

    private static Header[] buildHeader(Map<String, String> params) {
        Header[] headers = null;
        if (params != null && params.size() > 0) {
            int i = 0;
            headers = new BasicHeader[params.size()];
            for (Map.Entry<String, String> entry : params.entrySet()) {
                headers[i] = new BasicHeader(entry.getKey(), entry.getValue());
                i++;
            }
        }
        return headers;
    }

}
