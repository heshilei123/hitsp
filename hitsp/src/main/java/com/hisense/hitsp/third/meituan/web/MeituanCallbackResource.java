package com.hisense.hitsp.third.meituan.web;

import com.hisense.dustms.annotation.DustMapping;
import com.hisense.hitsp.common.HitspException;
import com.hisense.hitsp.third.meituan.service.MeituanCallbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

/**
 * 美团平台回调类接口
 *
 * @author yanglei
 *         date: 2017-03-24.
 */
@RestController
@RequestMapping("/o2o/mt")
public class MeituanCallbackResource {

    @Autowired
    MeituanCallbackService meituanCallbackService;

    /**
     * 设置每个开发者门店生成认证token的回调URL
     *
     * @param #ePoiId
     * @param #appAuthToken
     * @param #businessId
     * @param timestamp
     * @return
     */
    @DustMapping(consumes = {}, value = "/callurl")
    public Object setCallbackURL(String ePoiId, String appAuthToken, String businessId, String timestamp) throws HitspException {
        return meituanCallbackService.setMtCallUrl(ePoiId, appAuthToken, businessId, timestamp);
    }

    @DustMapping(consumes = {}, value = "/unbind")
    public Object unBind(String ePoiId, String businessId,String timestamp) throws HitspException {
        return meituanCallbackService.unBind(ePoiId, businessId, timestamp);
    }

    /**
     * 接收订单数据接口
     *
     * @param #developerId
     * @param #ePoiId
     * @param #sign
     * @param #order
     * @return
     * @patimestamp
     */
    @DustMapping(consumes = {}, value = "/pushorder")
    public Object pushOrder(String developerId, String ePoiId, String sign, String order) throws HitspException {

        return meituanCallbackService.pushOrder(developerId, ePoiId, sign, order);
    }

    /**
     * 接收订单完成消息接口
     *
     * @param #developerId
     * @param #ePoiId
     * @param #sign
     * @param #order       json格式串	, 订单详情数据
     * @return
     * @patimestamp
     */
    @DustMapping(consumes = {}, value = "/ordercompleted")
    public Object getOrderCompleted(String developerId, String ePoiId, String sign, String order) throws HitspException {
        return meituanCallbackService.orderCompleted(developerId, ePoiId, sign, order);
    }

    /**
     * ERP厂商接收订单取消消息接口
     *
     * @param #developerId
     * @param #ePoiId
     * @param #sign
     * @param #orderCancel
     * @return
     */
    @DustMapping(consumes = {}, value = "/cancelorder")
    public Object cancelOrder(String developerId, String ePoiId, String sign, String orderCancel) throws HitspException {
        return meituanCallbackService.cancelOrder(developerId, ePoiId, sign, orderCancel);
    }

    /**
     * 接收订单配送状态接口
     *
     * @param #developerId
     * @param #ePoiId
     * @param #sign
     * @param #shippingStatus
     * @return
     */
    @DustMapping(consumes = {}, value = "/shippingstatus")
    public Object shippingStatus(String developerId, String ePoiId, String sign, String shippingStatus) throws HitspException {
        return meituanCallbackService.orderShippingStatus(developerId, ePoiId, sign, shippingStatus);
    }

    /**
     * 接收门店状态变更接口
     *
     * @param #developerId
     * @param #sign
     * @param #poiStatus   门店状态信息
     * @return
     */
    @DustMapping(consumes = {}, value = "/shopstatus")
    public Object shopStatus(String developerId, String sign, String poiStatus) throws HitspException {
        return meituanCallbackService.shopStatus(developerId, sign, poiStatus);
    }

    /**
     * 接收订单退款类消息
     * 用户发起退款，确认或驳回退款，退款申诉等涉及退款的操作都会推送消息
     *
     * @param #developerId
     * @param #sign
     * @param #ePoiId
     * @param #orderRefund
     * @return
     * @throws Exception
     */
    @DustMapping(consumes = {}, value = "/refund")
    public Object refund(String developerId, String sign, String ePoiId, String orderRefund) throws HitspException, SQLException {
        return meituanCallbackService.refund(developerId, sign, ePoiId, orderRefund);
    }

    /**
     * 美团推送确认订单消息
     *
     * @param developerId
     * @param ePoiId
     * @param sign
     * @param order
     * @return
     * @throws Exception
     */
    @DustMapping(consumes = {}, value = "/confirmOrder")
    public Object comfirmOrder(String developerId, String ePoiId, String sign, String order)throws HitspException{
        return meituanCallbackService.comfirmOrder(developerId, ePoiId, sign, order);
    }

    @DustMapping(consumes = {}, value = "/privacynumdow")
    public Object privacyNumDowngrade(String developerId, String timestamp,String sign)throws HitspException{
        return meituanCallbackService.privacyNumDowngrade(developerId, timestamp, sign);
    }

}
