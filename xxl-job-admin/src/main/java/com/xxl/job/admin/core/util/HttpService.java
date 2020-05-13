package com.xxl.job.admin.core.util;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.stereotype.Component;

/**
 * @author admin
 * @date 2017/7/20
 */
@Component
public class HttpService {
    private static final CloseableHttpClient HTTPCLIENT = createHttpClient();

    private HttpService() {
    }

    private static CloseableHttpClient createHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(50000).setConnectionRequestTimeout(10000)
                //设置超时
                .setSocketTimeout(50000).build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);
        cm.setDefaultMaxPerRoute(100);
        return HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .build();

    }

    public static CloseableHttpClient getHttpClient() {
        return HTTPCLIENT;
    }

}
