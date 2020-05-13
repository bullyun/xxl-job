package com.xxl.job.admin.core.model;

import java.util.Date;

/**
 * @author bbb
 * @since 2020-05-12
 */
public class XxlJobWebhook {
    private int id;
    private String webhookUrl;		 // api地址
    private String webhookType;     // 类型
    private Date ctime;				 // 创建时间
    private Date mtime;	             //修改时间

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getWebhookType() {
        return webhookType;
    }

    public void setWebhookType(String webhookType) {
        this.webhookType = webhookType;
    }

    public Date getCtime() {
        return ctime;
    }

    public void setCtime(Date ctime) {
        this.ctime = ctime;
    }

    public Date getMtime() {
        return mtime;
    }

    public void setMtime(Date mtime) {
        this.mtime = mtime;
    }
}
