package com.hisense.hitsp.mqstream;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hisense.dustms.stream.SendBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 发送MQ消息模块
 *
 * @author yanglei
 *         date: 2017-04-21.
 */
@Component
public class MQSendService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    SendBean sendBean;

    public void syncProductToShopProduct(String publicProductId, String publicProduct, String type, String recordId, String tenantId, String appId) {
        JSONObject params = new JSONObject();
        params.put("publicProductId", publicProductId);
        params.put("publicProduct", publicProduct);
        params.put("type", type);
        params.put("recordId", recordId);
        params.put("tenantId", tenantId);
        params.put("appId", appId);

        params.put("do", "syncProductToShopProduct");
        JSONObject json = new JSONObject();
        json.put("plugin", "MQReceiverPlugin");
        json.put("data", params);
        sendBean.sendMessage(json.toJSONString());
        logger.info("MQ发送异步同步门店商品属性");
    }

    public void syncCategoryToShopCategory(String publicCategoryId, String publicCategory, String type, String recordId, String tenantId, String appId) {
        JSONObject params = new JSONObject();
        params.put("publicCategoryId", publicCategoryId);
        params.put("publicCategory", publicCategory);
        params.put("type", type);
        params.put("recordId", recordId);
        params.put("tenantId", tenantId);
        params.put("appId", appId);

        params.put("do", "syncCategoryToShopCategory");
        JSONObject json = new JSONObject();
        json.put("plugin", "MQReceiverPlugin");
        json.put("data", params);
        sendBean.sendMessage(json.toJSONString());
        logger.info("MQ发送异步同步门店品类属性");
    }

    public void batchUploadProductCategory(String recordId, String shopId, JSONArray categoryList, String tenantId, String appId) {
        JSONObject params = new JSONObject();
        params.put("shopId", shopId);
        params.put("categoryList", categoryList);
        params.put("recordId", recordId);
        params.put("tenantId", tenantId);
        params.put("appId", appId);

        params.put("do", "batchUploadProductCategory");
        JSONObject json = new JSONObject();
        json.put("plugin", "MQReceiverPlugin");
        json.put("data", params);
        sendBean.sendMessage(json.toJSONString());
        logger.info("MQ发送单店批量上架菜品分类");
    }

    public void batchUploadPublicProductCategory(String userId, String recordId, JSONArray publicCategoryList, String tenantId, String appId) {
        JSONObject params = new JSONObject();
        params.put("publicCategoryList", publicCategoryList);
        params.put("userId", userId);
        params.put("recordId", recordId);
        params.put("tenantId", tenantId);
        params.put("appId", appId);

        params.put("do", "batchUploadPublicProductCategory");
        JSONObject json = new JSONObject();
        json.put("plugin", "MQReceiverPlugin");
        json.put("data", params);
        sendBean.sendMessage(json.toJSONString());
        logger.info("MQ发送上架公共菜品分类批量");
    }

    public void batchUploadProduct(String recordId, String shopId, JSONArray productList, String tenantId, String appId) {
        JSONObject params = new JSONObject();
        params.put("shopId", shopId);
        params.put("productList", productList);
        params.put("recordId", recordId);
        params.put("tenantId", tenantId);
        params.put("appId", appId);

        params.put("do", "batchUploadProduct");
        JSONObject json = new JSONObject();
        json.put("plugin", "MQReceiverPlugin");
        json.put("data", params);
        sendBean.sendMessage(json.toJSONString());
        logger.info("MQ发送单店批量上架菜品");
    }

    public void batchUploadPublicProduct(String recordId, String userId, JSONArray publicProductList, String tenantId, String appId) {
        JSONObject params = new JSONObject();
        params.put("publicProductList", publicProductList);
        params.put("recordId", recordId);
        params.put("userId", userId);
        params.put("tenantId", tenantId);
        params.put("appId", appId);

        params.put("do", "batchUploadPublicProduct");
        JSONObject json = new JSONObject();
        json.put("plugin", "MQReceiverPlugin");
        json.put("data", params);
        sendBean.sendMessage(json.toJSONString());
        logger.info("MQ发送上架公共菜品批量");
    }

    public void batchExtractPublicCategory(String recordId, String userId, String shopId, JSONArray productCategoryIdList, String tenantId, String appId) {
        JSONObject params = new JSONObject();
        params.put("productCategoryIdList", productCategoryIdList);
        params.put("recordId", recordId);
        params.put("userId", userId);
        params.put("shopId", shopId);
        params.put("tenantId", tenantId);
        params.put("appId", appId);

        params.put("do", "batchExtractPublicCategory");
        JSONObject json = new JSONObject();
        json.put("plugin", "MQReceiverPlugin");
        json.put("data", params);
        sendBean.sendMessage(json.toJSONString());
        logger.info("MQ发送批量提取公共品类消息");
    }
}
