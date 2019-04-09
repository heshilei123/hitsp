package com.hisense.hitsp.web;

import com.hisense.dustms.annotation.DustMapping;
import com.hisense.hitsp.common.HitspException;
import com.hisense.hitsp.service.OrderViewService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单查询接口
 *
 * @author yanglei
 */
@RestController
@RequestMapping("/o2o/order")
@CrossOrigin
public class OrderViewResource {
    private final Logger logger = LoggerFactory.getLogger(OrderViewResource.class);

    @Autowired
    OrderViewService orderViewService;

    /**
     * 查询订单接口
     * 若订单号存在，获取订单详情
     * 若订单号为空，门店Id存在，返回当前erp门店的全部未确认订单列表
     * 若订单号为空，且门店Id为空，返回全部未确认订单列表
     *
     * @param orderId
     * @param shopId
     * @return success 返回Order实体类, 即Order对应的json串
     */
    @DustMapping(consumes = {}, value = "/view")
    public Object getOrderDetail(String orderId, String shopId, String tenantId, String appId, String sign, Long timestamp) throws HitspException {
        logger.info("拉取订单信息，请求orderId为:" + orderId + "，shopId为：" + shopId + "，tenantId为：" + tenantId);
        if (StringUtils.isNotEmpty(orderId)) {
            return orderViewService.getOrderDetailById(orderId, tenantId, appId);
        }

        if (StringUtils.isNotEmpty(shopId)) {
            return orderViewService.getNoConfirmOrderListByShopId(shopId, tenantId, appId);
        }

        return orderViewService.getAllNoConfirmOrderList(tenantId, appId);


    }
}
