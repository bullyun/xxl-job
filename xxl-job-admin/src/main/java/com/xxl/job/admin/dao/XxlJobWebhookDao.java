package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.XxlJobWebhook;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author no_f
 * @since 2020-05-12
 */
@Mapper
public interface XxlJobWebhookDao {

    public XxlJobWebhook findByWebhookType(@Param("webhookType") String webhookType);

}
