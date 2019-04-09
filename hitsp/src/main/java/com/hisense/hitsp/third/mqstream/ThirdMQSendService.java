package com.hisense.hitsp.third.mqstream;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hisense.dustdb.sql.ISqlAdapter;
import com.hisense.dustms.stream.SendBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 外卖平台MQ发送模块
 * Created by yanglei on 2017-04-24.
 */
@Component
public class ThirdMQSendService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    SendBean sendBean;

    public void saveOrderMQ(String orderId, String orderFrom, String dbName) {
        JSONObject params = new JSONObject();
        params.put("orderId", orderId);
        params.put("orderFrom", orderFrom);
        params.put("do", "save");
        params.put("dbName", dbName);

        JSONObject json = new JSONObject();
        json.put("plugin", "ThirdMQReceivePlugin");
        json.put("data", params);
        sendBean.sendMessage(JSON.toJSONString(json));
        logger.info("发送保存饿了么平台推送订单MQ");
    }

    public void cancelMQ(String orderId, String orderFrom, String dbName) {
        JSONObject params = new JSONObject();
        params.put("orderId", orderId);
        params.put("orderFrom", orderFrom);
        params.put("do", "cancel");
        params.put("dbName", dbName);

        JSONObject json = new JSONObject();
        json.put("plugin", "ThirdMQReceivePlugin");
        json.put("data", params);
        sendBean.sendMessage(JSON.toJSONString(json));
        logger.info("cancel send");
    }

    public void completedMQ(String orderId, String orderFrom, String dbName) {
        JSONObject paramsJson = new JSONObject();
        paramsJson.put("orderId", orderId);
        paramsJson.put("orderFrom", orderFrom);
        paramsJson.put("do", "complete");
        paramsJson.put("dbName", dbName);

        JSONObject json = new JSONObject();
        json.put("plugin", "ThirdMQReceivePlugin");
        json.put("data", paramsJson);
        sendBean.sendMessage(json.toJSONString());
    }

    public void shippingMQ(String orderId, String dbName) {
        JSONObject params = new JSONObject();
        params.put("orderId", orderId);
        params.put("orderFrom", "15");
        params.put("do", "ship");
        params.put("dbName", dbName);

        JSONObject json = new JSONObject();
        json.put("plugin", "ThirdMQReceivePlugin");
        json.put("data", params);
        sendBean.sendMessage(json.toJSONString());
    }

    public void shippedMQ(String orderId, String dbName) {
        JSONObject params = new JSONObject();
        params.put("orderId", orderId);
        params.put("orderFrom", "15");
        params.put("do", "shipped");
        params.put("dbName", dbName);

        JSONObject json = new JSONObject();
        json.put("plugin", "ThirdMQReceivePlugin");
        json.put("data", params);
        sendBean.sendMessage(json.toJSONString());
    }

    public void saveOrderMQ(String orderId, String o2oOrderId, String orderFrom, String dbName, String source) {
        JSONObject params = new JSONObject();
        params.put("orderId", orderId);
        params.put("o2oOrderId", o2oOrderId);
        params.put("orderFrom", orderFrom);
        params.put("source", source);
        params.put("do", "save");
        params.put("dbName", dbName);

        JSONObject json = new JSONObject();
        json.put("plugin", "ThirdMQReceivePlugin");
        json.put("data", params);
        sendBean.sendMessage(JSON.toJSONString(json));
        logger.info("发送拉取并保存订单MQ");
    }
}
