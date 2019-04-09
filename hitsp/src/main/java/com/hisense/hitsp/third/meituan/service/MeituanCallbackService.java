package com.hisense.hitsp.third.meituan.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hisense.dustcore.util.Converter;
import com.hisense.dustdb.DbAdapterManager;
import com.hisense.dustdb.TenantAdapterManager;
import com.hisense.dustdb.sql.DataRow;
import com.hisense.dustdb.sql.DataTable;
import com.hisense.dustdb.sql.ISqlAdapter;
import com.hisense.dustdb.sql.SqlCommand;
import com.hisense.dustdb.tenant.SeparatedConfig;
import com.hisense.hitsp.common.CommonOrderStatusUpdateService;
import com.hisense.hitsp.common.CommonUtil;
import com.hisense.hitsp.common.DataBaseCount;
import com.hisense.hitsp.common.HitspException;
import com.hisense.hitsp.config.DatabaseYmlConfig;
import com.hisense.hitsp.config.PushYmlConfig;
import com.hisense.hitsp.model.Order;
import com.hisense.hitsp.model.OrderDetail;
import com.hisense.hitsp.model.Product;
import com.hisense.hitsp.model.Shop;
import com.hisense.hitsp.service.*;
import com.hisense.hitsp.third.IPlatform;
import com.hisense.hitsp.third.PlatFormFactory;
import com.hisense.hitsp.third.mqstream.PrivacyNumMQSendService;
import com.hisense.hitsp.third.mqstream.ThirdMQSendService;
import com.hisense.hitsp.third.common.ThirdPlatformCommonUtil;
import com.hisense.hitsp.third.meituan.MeituanInfoConst;
import com.sankuai.sjst.platform.developer.utils.SignUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 美团的对接服务
 *
 * @author yanglei
 *         date: 2017-03-24.
 */
@Service
public class MeituanCallbackService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    DbAdapterManager dbAdapterManager;
    @Autowired
    TenantAdapterManager tenantAdapterManager;
    @Autowired
    ThirdMQSendService thirdMQSendService;
    @Autowired
    PushYmlConfig pushYmlConfig;
    @Autowired
    DatabaseYmlConfig databaseYmlConfig;
    @Autowired
    OrderViewService orderViewService;
    @Autowired
    PushService pushService;
    @Autowired
    CommonOrderStatusUpdateService commonOrderStatusUpdateService;
    @Autowired
    ShopStatusService shopStatusService;
    @Autowired
    PrivacyNumMQSendService privacyNumMQSendService;

    private static Map<String, String> beforeRefundStatus = new HashMap<>();
    private String xiaoweiUrl;
    private String xiaoweiUrlStatus;
//    private Integer databaseCount;

    @PostConstruct
    private void init() throws HitspException {
        xiaoweiUrl = pushYmlConfig.getXiaoweiUrl();
        xiaoweiUrlStatus = pushYmlConfig.getXiaoweiUrlStatus();
//        databaseCount = databaseYmlConfig.getDatabaseCount();
    }

    /**
     * 设置callbackUrl服务, 接收回调URL的数据，返回门店token
     *
     * @param #ePoiId
     * @param #appAuthToken
     * @param #businessId
     * @param #timestamp
     * @return 接收appAuthToken成功后返回{data:"success"}字符串
     */
    public Object setMtCallUrl(String ePoiId, String appAuthToken, String businessId,
                               String timestamp) throws HitspException {

        if (StringUtils.isNotEmpty(ePoiId) && StringUtils.isNotEmpty(appAuthToken)) {
            logger.info("\r\n回调返回信息, ePoiId: {}, appAuthToken: {}", ePoiId, appAuthToken);

            ISqlAdapter adapter = dbAdapterManager.getAdapter("tenantdb");
            if (adapter == null) {
                logger.error("数据库连接不存在");
                return null;
            }

            for (int i = 1; i <= DataBaseCount.databaseCount; i++) {
                try {
                    adapter.useDbName(CommonUtil.DB_NAME + i);
                } catch (SQLException e) {
                    logger.error(CommonUtil.DB_NAME + "数据库不存在", e);
                    throw new HitspException(CommonUtil.DB_NAME + "数据库不存在", e, adapter);
                }

                if (checkShopId(adapter, ePoiId)) {
                    saveAccountToken(adapter, ePoiId, appAuthToken);
                    try {
                        adapter.commit();
                    } catch (SQLException e) {
                        logger.error("事务提交异常", e);
                        try {
                            adapter.rollback();
                        } catch (SQLException ex) {
                            logger.error("事务回滚异常", ex);
                            adapter.closeQuiet();
                            return null;
                        }
                        adapter.closeQuiet();
                        return null;
                    }

                    break;
                }

            }

            adapter.closeQuiet();
            return "success";
        }

        logger.info("\r\n回调返回信息不正确");
        return null;
    }

    public Object unBind(String ePoiId, String businessId, String timestamp) throws HitspException {

        if (StringUtils.isNotEmpty(ePoiId)) {
            logger.info("\r\n解绑回调返回信息, ePoiId: {}", ePoiId);
            unBindMtShop(ePoiId);
            return "success";
        }

        logger.info("\r\n回调返回信息不正确");
        return null;
    }

    @Async
    public void unBindMtShop(String ePoiId) throws HitspException {
        for (int i = 1; i <= DataBaseCount.databaseCount; i++) {
            ISqlAdapter adapter = dbAdapterManager.getAdapter("tenantdb");
            if (adapter == null) {
                logger.error("美团门店解绑失败，数据库连接不存在");
                throw new HitspException("美团门店解绑失败，数据库连接不存在");
            }
            try {
                try {
                    adapter.useDbName(CommonUtil.DB_NAME + i);
                } catch (SQLException e) {
                    logger.error(CommonUtil.DB_NAME + "数据库不存在", e);
                    throw e;
                }
                shopStatusService.unBindMtShop06(adapter, ePoiId);
                adapter.commit();
            } catch (SQLException e) {
                adapter.rollbackQuiet();
                logger.error("美团门店解绑失败，数据库异常", e);
            } finally {
                adapter.closeQuiet();
            }
        }
    }

    /**
     * 美团平台：用户下单后，平台会给erp厂商推送新订单数据服务<br/>
     * 首先校验数字签名sign是否正确
     *
     * @param #developerId
     * @param #ePoiId
     * @param #sign        数字签名
     * @param #order
     * @return 接收成功后返回{data:"OK"}.
     */
    public Object pushOrder(String developerId, String ePoiId, String sign, String order) throws HitspException {
        logger.info("developerId={}\nePoiId={}\nsign={}\norder={}", developerId, ePoiId, sign, order);
        Map<String, String> params = getParamsMap(developerId, ePoiId, sign);
        params.put("order", order);
        logger.info("推送订单，sign校验通过！");

        ISqlAdapter adapter = dbAdapterManager.getAdapter("tenantdb");
        if (adapter == null) {
            return null;
        }

        String dbName = null;
        for (int i = 1; i <= DataBaseCount.databaseCount; i++) {
            try {
                adapter.useDbName(CommonUtil.DB_NAME + i);
            } catch (SQLException e) {
                logger.error(CommonUtil.DB_NAME + "数据库不存在", e);
                adapter.closeQuiet();
                return null;
            }

            if (checkShopId(adapter, ePoiId)) {
                dbName = CommonUtil.DB_NAME + i;
                break;
            }
        }

        if (StringUtils.isEmpty(dbName)) {
            logger.error("数据库不存在");
            adapter.closeQuiet();
            return null;
        }

        JSONObject orderObject = JSON.parseObject(order);
        boolean mtOrderIsExist = checkMtOrderIsExist(adapter, orderObject.getString("orderId"));
        if (mtOrderIsExist) {
            logger.info("美团推单({})已保存", orderObject.getString("orderId"));
            adapter.closeQuiet();
            return null;
        }

        String orderId = CommonUtil.generateId();

        try {
            saveMtOrder(adapter, ePoiId, orderObject, orderId);
            try {
                adapter.commit();
            } catch (SQLException e) {
                logger.error("提交保存美团订单失败", e);
                try {
                    adapter.rollback();
                } catch (SQLException ex) {
                    logger.error("回滚保存美团订单失败", ex);
                    adapter.closeQuiet();
                    return null;
                }

                adapter.closeQuiet();
                return null;
            }
        } catch (SQLException e) {
            logger.error("美团推送新订单时，保存美团订单表失败", e);
            adapter.closeQuiet();
            return null;
        }

        logger.info("美团推订单时，保存美团订单成功");


        Order orderMaster = convertMtOrderToOrderObj(adapter, orderObject, orderId);
        ThirdPlatformCommonUtil.saveOrderMaster04(adapter, orderMaster);
        ThirdPlatformCommonUtil.saveOrderDetail(adapter, orderMaster);
        try {
            adapter.commit();
        } catch (SQLException e) {
            logger.error("提交保存订单详情失败", e);
            try {
                adapter.rollback();
            } catch (SQLException ex) {
                logger.error("回滚保存订单详情失败", ex);
                adapter.closeQuiet();
                return null;
            }
            adapter.closeQuiet();
            return null;

        }
        adapter.closeQuiet();
        logger.info("美团推订单时，保存订单明细成功");
        return CommonUtil.SUCCESS_RETURN;

    }

    /**
     * 美团确认订单推送消息处理
     *
     * @param developerId
     * @param ePoiId
     * @param sign
     * @param order
     * @return
     */
    public Object comfirmOrder(String developerId, String ePoiId, String sign, String order) throws HitspException {
        logger.info("美团确认订单推送消息处理开始");
        logger.info("developerId={}\nePoiId={}\nsign={}\norder={}", developerId, ePoiId, sign, order);
        Map<String, String> params = getParamsMap(developerId, ePoiId, sign);
        params.put("order", order);
        logger.info("推送订单，sign校验通过！");

        ISqlAdapter adapter = dbAdapterManager.getAdapter("tenantdb");
        if (adapter == null) {
            return null;
        }
        try {
            String dbName = null;
            for (int i = 1; i <= DataBaseCount.databaseCount; i++) {
                try {
                    adapter.useDbName(CommonUtil.DB_NAME + i);
                } catch (SQLException e) {
                    logger.error(CommonUtil.DB_NAME + "数据库不存在", e);
                    throw new HitspException(CommonUtil.DB_NAME + "数据库不存在", e, adapter);
                }

                if (checkShopId(adapter, ePoiId)) {
                    dbName = CommonUtil.DB_NAME + i;
                    break;
                }
            }

            if (StringUtils.isEmpty(dbName)) {
                logger.error("数据库不存在");
                adapter.closeQuiet();
                return null;
            }

            JSONObject orderObject = JSON.parseObject(order);
            String orderId = orderObject.getString("orderId");
            String status = orderObject.getString("status");
            if (StringUtils.equals("4", status)) {
                if (checkMtOrderIsExist(adapter, orderId)) {
                    logger.info("美团确认订单推送消息，订单存在，修改订单状态开始", orderId);
                    String orderMasterId = getOrderIdByErpOrderId(adapter, orderId);
                    Order orderMaster = new Order();
                    orderMaster.setId(Long.parseLong(orderMasterId));
                    orderMaster.setOrderFrom("12");
                    commonOrderStatusUpdateService.updateOrderStatus(adapter, orderMaster, "confirm");
                    if (StringUtils.equals(CommonUtil.DB_NAME + "2", dbName)) {
                        if (StringUtils.isNotEmpty(orderMasterId)) {
                            Map<String, Object> paramMap = new HashMap<>();
                            paramMap.put("orderId", orderMasterId);
                            paramMap.put("status", "1");
                            pushService.circlePush(xiaoweiUrlStatus, paramMap);
                        }
                    }
                }
            }

            CommonUtil.adapterCommitAndClose(adapter);
            return CommonUtil.SUCCESS_RETURN;
        } finally {
            adapter.closeQuiet();
        }
    }


    /**
     * 美团平台：订单被取消时，推送订单取消消息服务
     *
     * @param orderCancel json格式串,订单取消信息
     * @return
     */
    public Object cancelOrder(String developerId, String ePoiId, String sign,
                              String orderCancel) throws HitspException {
        logger.info("取消订单: developerId={}, ePoiId={}, sign={},orderCancel={}",
                developerId, ePoiId, sign, orderCancel);
        Map<String, String> params = getParamsMap(developerId, ePoiId, sign);
        params.put("orderCancel", orderCancel);
        if (!checkSign(params)) {
            logger.info("美团平台传送数字签名sign不正确");
            return null;
        }
        logger.info("推送取消订单，sign校验通过！");

        JSONObject jsonObject = JSON.parseObject(orderCancel);
        String reasonCode = jsonObject.getString("reasonCode");

        String erpOrderId = Converter.toString(jsonObject.getLong("orderId"));

        ISqlAdapter adapter = dbAdapterManager.getAdapter("tenantdb");
        String dbName = null;
        try {
            for (int i = 1; i <= DataBaseCount.databaseCount; i++) {
                try {
                    adapter.useDbName(CommonUtil.DB_NAME + i);

                } catch (SQLException e) {
                    logger.error(CommonUtil.DB_NAME + "数据库不存在", e);
                    throw new HitspException(CommonUtil.DB_NAME + "数据库不存在", e, adapter);
                }

                if (checkShopId(adapter, ePoiId)) {
                    dbName = CommonUtil.DB_NAME + i;
                    break;
                }
            }

            if (StringUtils.isEmpty(dbName)) {
                logger.error("数据库不存在");
                adapter.closeQuiet();
                return null;
            }

            thirdMQSendService.cancelMQ(erpOrderId, "12", dbName);
            logger.info("cancel success!");

            if (StringUtils.equals(CommonUtil.DB_NAME + "2", dbName)) {
                String orderId = getOrderIdByErpOrderId(adapter, erpOrderId);
                if (StringUtils.isNotEmpty(orderId)) {
                    Map<String, Object> paramMap = new HashMap<>();
                    paramMap.put("orderId", orderId);
                    paramMap.put("status", "5");
                    paramMap.put("shipperName", null);
                    paramMap.put("shipperPhone", null);
                    pushService.circlePush(xiaoweiUrlStatus, paramMap);
                }
            }
            return CommonUtil.SUCCESS_RETURN;
        } finally {
            adapter.closeQuiet();
        }
    }

    /**
     * 美团平台：推送已完成状态的订单给ERP方服务
     *
     * @param #developerId
     * @param #ePoiId
     * @param #sign        数字签名
     * @param #order
     * @return 接收成功后返回{data:"OK"}.
     */
    public Object orderCompleted(String developerId, String ePoiId, String sign, String order) throws HitspException {
        Map<String, String> params = getParamsMap(developerId, ePoiId, sign);
        params.put("order", order);
        if (!checkSign(params)) {
            logger.info("美团数字签名sign不正确");
            return null;
        }
        logger.info("推送完成订单，sign校验通过！");


        ISqlAdapter adapter = dbAdapterManager.getAdapter("tenantdb");
        String dbName = null;
        try {
            for (int i = 1; i <= DataBaseCount.databaseCount; i++) {
                try {
                    adapter.useDbName(CommonUtil.DB_NAME + i);
                } catch (SQLException e) {
                    logger.error(CommonUtil.DB_NAME + "数据库不存在", e);
                    throw new HitspException(CommonUtil.DB_NAME + "数据库不存在", e, adapter);
                }

                if (checkShopId(adapter, ePoiId)) {
                    dbName = CommonUtil.DB_NAME + i;
                    break;
                }
            }
            if (StringUtils.isEmpty(dbName)) {
                logger.error("数据库不存在");
                return null;
            }

            JSONObject jsonObject = JSON.parseObject(order);
            String erpOrderId = jsonObject.getString("orderId");
            thirdMQSendService.completedMQ(erpOrderId, "12", dbName);
            logger.info("completed success!");

            if (StringUtils.equals(CommonUtil.DB_NAME + "2", dbName)) {
                String orderId = getOrderIdByErpOrderId(adapter, erpOrderId);
                if (StringUtils.isNotEmpty(orderId)) {
                    Map<String, Object> paramMap = new HashMap<>();
                    paramMap.put("orderId", orderId);
                    paramMap.put("status", "4");
                    paramMap.put("shipperName", null);
                    paramMap.put("shipperPhone", null);
                    pushService.circlePush(xiaoweiUrlStatus, paramMap);
                }
            }
            return CommonUtil.SUCCESS_RETURN;
        } finally {
            adapter.closeQuiet();
        }
    }

    /**
     * 美团外卖平台：推送配送状态变更消息服务
     *
     * @param #developerId
     * @param #ePoiId         erp方门店id
     * @param #sign           数字签名
     * @param #shippingStatus json格式串,订单配送信息
     * @return 接收成功返回{"data": "OK"}
     */
    public Object orderShippingStatus(String developerId, String ePoiId, String sign, String shippingStatus) throws HitspException {
        Map<String, String> params = getParamsMap(developerId, ePoiId, sign);
        params.put("shippingStatus", shippingStatus);
        if (!checkSign(params)) {
            logger.info("美团数字签名sign不正确");
            return null;
        }

        JSONObject jsonObject = JSON.parseObject(shippingStatus);
        String erpOrderId = jsonObject.getString("orderId");
        if (StringUtils.isEmpty(erpOrderId)) {
            return null;
        }

        ISqlAdapter adapter = dbAdapterManager.getAdapter("tenantdb");
        String dbName = null;
        try {
            for (int i = 1; i <= DataBaseCount.databaseCount; i++) {
                try {
                    adapter.useDbName(CommonUtil.DB_NAME + i);
                } catch (SQLException e) {
                    logger.error(CommonUtil.DB_NAME + "数据库不存在", e);
                    throw new HitspException(CommonUtil.DB_NAME + "数据库不存在", e, adapter);
                }

                if (checkShopId(adapter, ePoiId)) {
                    dbName = CommonUtil.DB_NAME + i;
                    break;
                }
            }

            if (StringUtils.isEmpty(dbName)) {
                logger.error("数据库不存在");
                adapter.closeQuiet();
                return null;
            }

            String orderId = getOrderIdByErpOrderId(adapter, erpOrderId);

            updateOrderMtShippingStatus(adapter, jsonObject);
            updateOrderMasterShippingStatus(adapter, jsonObject);
            try {
                adapter.commit();
            } catch (SQLException e) {
                logger.error("美团平台推送配送状态变更消息时，提交更新订单状态失败", e);
                try {
                    adapter.rollback();
                } catch (SQLException ex) {
                    logger.error("美团平台推送配送状态变更消息时，回滚更新订单状态失败", ex);
                    throw new HitspException("美团平台推送配送状态变更消息时，回滚更新订单状态失败", e, adapter);
                }

                throw new HitspException("美团平台推送配送状态变更消息时，提交更新订单状态失败", e, adapter);
            } finally {
                adapter.closeQuiet();
            }


            if (StringUtils.equals(CommonUtil.DB_NAME + "2", dbName)) {
                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("orderId", orderId);
                paramMap.put("status", "2");
                paramMap.put("shipperName", jsonObject.getString("dispatcherName"));
                paramMap.put("shipperPhone", jsonObject.getString("dispatcherMobile"));
                pushService.circlePush(xiaoweiUrlStatus, paramMap);
            }


            return CommonUtil.SUCCESS_RETURN;
        } finally {
            adapter.closeQuiet();
        }
    }

    /**
     * 保存美团订单数据
     *
     * @param ePoiId     门店Id
     * @param jsonObject 美团订单信息
     * @throws SQLException
     */
    private void saveMtOrder(ISqlAdapter adapter, String ePoiId, JSONObject jsonObject, String orderId) throws SQLException {
        SqlCommand cmd = new SqlCommand(MeituanSqlConst.SQL_INSERT_MT);
        cmd.setParameter("id", Converter.toLong(orderId));
        cmd.setParameter("orderId", jsonObject.getBigInteger("orderId"));
        cmd.setParameter("orderIdView", jsonObject.getLong("orderIdView"));
        cmd.setParameter("caution", jsonObject.getString("caution"));
        cmd.setParameter("cityId", jsonObject.getLong("cityId"));
        cmd.setParameter("ctime", jsonObject.getLong("ctime"));
        cmd.setParameter("utime", jsonObject.getLong("utime"));
        cmd.setParameter("daySeq", jsonObject.getString("daySeq"));
        cmd.setParameter("deliveryTime", jsonObject.getLong("deliveryTime"));
        cmd.setParameter("detail", jsonObject.getString("detail"));
        cmd.setParameter("ePoiId", ePoiId);
        cmd.setParameter("extras", jsonObject.getString("extras"));
        cmd.setParameter("hasInvoiced", jsonObject.getInteger("hasInvoiced"));
        cmd.setParameter("invoiceTitle", jsonObject.getString("invoiceTitle"));
        cmd.setParameter("isFavorites", jsonObject.containsKey("isFavorites") ? jsonObject.getBoolean("isFavorites") : 0);
        cmd.setParameter("isPoiFirstOrder", jsonObject.getBoolean("isPoiFirstOrder"));
        cmd.setParameter("isThirdShipping", jsonObject.getInteger("isThirdShipping"));
        cmd.setParameter("latitude", jsonObject.getDouble("latitude"));
        cmd.setParameter("longitude", jsonObject.getDouble("longitude"));
        cmd.setParameter("logisticsCode", jsonObject.getInteger("logisticsCode"));
        cmd.setParameter("originalPrice", jsonObject.getDouble("originalPrice"));
        cmd.setParameter("payType", jsonObject.getInteger("payType"));
        cmd.setParameter("poiAddress", jsonObject.getString("poiAddress"));
        cmd.setParameter("poiId", jsonObject.getString("poiId"));
        cmd.setParameter("poiName", jsonObject.getString("poiName"));
        cmd.setParameter("poiPhone", jsonObject.getString("poiPhone"));
        cmd.setParameter("poiReceiveDetail", jsonObject.getString("poiReceiveDetail"));
        cmd.setParameter("recipientAddress", jsonObject.getString("recipientAddress"));
        cmd.setParameter("recipientName", jsonObject.getString("recipientName"));
        cmd.setParameter("recipientPhone", jsonObject.getString("recipientPhone"));
        cmd.setParameter("shipperPhone", jsonObject.getString("shipperPhone"));
        cmd.setParameter("shippingFee", jsonObject.getDouble("shippingFee"));
        cmd.setParameter("status", jsonObject.getInteger("status"));
        cmd.setParameter("total", jsonObject.getDouble("total"));
        adapter.update(cmd);
    }

    /**
     * 将美团order的json串转化为{@link Order}对象
     *
     * @param orderJson order的json串
     */
    private Order convertMtOrderToOrderObj(ISqlAdapter adapter, JSONObject orderJson, String orderId) throws HitspException {
        Order order = new Order();
        order.setId(Converter.toLong(orderId));

        //TODO 与饿了么保持一致，立即送达配送时间为空
        if (orderJson.getInteger("deliveryTime") == 0) {
            order.setDeliverTime(null);
        } else {
            Long lo = (orderJson.getLong("deliveryTime")) * 1000;
            order.setDeliverTime(new Date(lo));
        }
        try {
            Date cTime = CommonUtil.timestampToDate(orderJson.getLong("ctime") * 1000);
            Date uTime = CommonUtil.timestampToDate(orderJson.getLong("utime") * 1000);
            order.setGmtCreate(cTime);
            order.setGmtModify(uTime);
        } catch (ParseException e) {
            logger.error("下单时间处理错误", e);
        }
        order.setOrderId(orderJson.getString("orderId"));
        order.setOrderFrom("12");
        order.setTotalPrice(orderJson.getBigDecimal("total"));
        BigDecimal originalPrice = orderJson.getBigDecimal("originalPrice");
        order.setOriginalPrice(originalPrice);
        order.setStatus("0");

        //TODO 下单查看ePoiId值
        String shopId = getShopIdByEPoiId(adapter, orderJson.getString("ePoiId"));
        DataTable dtShop = getShopInfoByShopId(adapter, shopId);
        if (dtShop != null && dtShop.size() != 0) {
            Shop shop = new Shop();
            CommonUtil.convertMapToObj(dtShop.getRows().get(0).toMap(), shop);
            order.setShopId(shopId);
            order.setShopName(shop.getName());
        }

        order.setShopAddress(orderJson.getString("poiAddress"));
        order.setShopPhone(orderJson.getString("poiPhone"));
        order.setRecipientName(orderJson.getString("recipientName"));
        order.setRecipientAddress(orderJson.getString("recipientAddress"));
        order.setRecipientPhone(orderJson.getString("recipientPhone"));
        order.setRecipientLongitude(orderJson.getBigDecimal("longitude"));
        order.setRecipientLatitude(orderJson.getBigDecimal("latitude"));
        String logisticsCode = orderJson.getString("logisticsCode");
        String shippingType = "00";
        switch (logisticsCode) {
            case "0000":
                shippingType = "00";
                break;
            case "0002":
                shippingType = "01";
                break;
            case "0016":
                shippingType = "02";
                break;
            case "0033":
                shippingType = "03";
                break;
            case "1001":
                shippingType = "04";
                break;
            case "1002":
                shippingType = "05";
                break;
            case "1003":
                shippingType = "06";
                break;
            case "1004":
                shippingType = "07";
                break;
            case "2001":
                shippingType = "08";
                break;
            case "2002":
                shippingType = "09";
                break;
            case "3001":
                shippingType = "18";
                break;
            default:
                shippingType = "00";
                break;
        }
        order.setShippingType(shippingType);
        order.setShippingFee(orderJson.getBigDecimal("shippingFee"));
        order.setShipperName(orderJson.getString("logisticsDispatcherName"));
        order.setShipperPhone(orderJson.getString("shipperPhone"));
        order.setHasInvoiced(orderJson.getBoolean("hasInvoiced"));
        order.setInvoiceTitle(orderJson.getString("invoiceTitle"));
        order.setPackageFee(getPackageFee(orderJson));
        order.setPayType(orderJson.getString("payType"));

        String caution = orderJson.getString("caution");

        if (caution.length() == 0) {
            order.setCaution("");
        } else {
            String[] cautionArray = caution.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < cautionArray.length; i++) {
                String ss = cautionArray[i];
                if (!StringUtils.equals("", ss)) {
                    sb.append(ss);
                    sb.append(",");
                }
            }
            sb.deleteCharAt(sb.length() - 1);
            order.setCaution(sb.toString());
        }

        order.setRemark("");
        //店铺实收、店铺承担活动费、服务费
        JSONObject poiReceiveDetail = orderJson.getJSONObject("poiReceiveDetail");
        JSONArray actOrderChargeByPoi = poiReceiveDetail.getJSONArray("actOrderChargeByPoi");
        BigDecimal moneyCentTotal = new BigDecimal(0);
        for (int i = 0; i < actOrderChargeByPoi.size(); i++) {
            JSONObject moneyCentJson = actOrderChargeByPoi.getJSONObject(i);
            BigDecimal moneyCent = moneyCentJson.getBigDecimal("moneyCent");
            moneyCentTotal = moneyCentTotal.add(moneyCent);
        }
        moneyCentTotal = moneyCentTotal.divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);//店铺承担活动费
        BigDecimal foodShareFeeChargeByPoi = poiReceiveDetail.getBigDecimal("foodShareFeeChargeByPoi").divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);//服务费
        BigDecimal shopIncome = poiReceiveDetail.getBigDecimal("wmPoiReceiveCent").divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);//店铺实收

        order.setShopPart(moneyCentTotal);
        order.setServiceFee(foodShareFeeChargeByPoi);
        order.setShopIncome(shopIncome);
        List<OrderDetail> detailList = convertOrderMtToOrderDetailList(orderJson, adapter);
        order.setDetail(detailList);
        return order;
    }

    private DataTable getShopInfoByShopId(ISqlAdapter adapter, String shopId) throws HitspException {

        SqlCommand cmd = new SqlCommand(MeituanSqlConst.SQL_SELECT_SHOP_BY_ID);

        DataTable dt = null;
        try {
            cmd.setParameter("shopId", shopId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("美团推订单时，查询门店信息时异常", e);
            throw new HitspException("美团推订单时，查询门店信息时异常", e, adapter);

        }

        return dt;
    }


    /**
     * 将美团order的json数据，转化为{@link OrderDetail}对象的List
     *
     * @param orderJson order的json数据
     * @return
     */
    private List<OrderDetail> convertOrderMtToOrderDetailList(JSONObject orderJson, ISqlAdapter adapter) throws HitspException {
        List<OrderDetail> detailList = Lists.newArrayList();
        JSONArray jsonArray = JSON.parseArray(orderJson.getString("detail"));
        for (int i = 0; i < jsonArray.size(); i++) {
            Object object = jsonArray.get(i);
            JSONObject obj = JSON.parseObject(object.toString());
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderJson.getString("orderId"));

            String mtProductId = obj.getString("app_food_code");
            DataTable dtProduct = getProductInfoByMtProductId(adapter, mtProductId);
            if (dtProduct != null && dtProduct.size() != 0) {
                Product product = (Product) CommonUtil.convertMapToObj(dtProduct.getRows().get(0).toMap(), new Product());
                orderDetail.setCode(Converter.toString(product.getId()));
                orderDetail.setName(product.getName());
                orderDetail.setCategoryId(Converter.toString(product.getCategoryId()));
            } else {
                logger.error("美团菜品({})不存在", mtProductId);
            }

            orderDetail.setPrice(obj.getBigDecimal("price"));
            orderDetail.setQuantity(obj.getBigDecimal("quantity"));
            //不包含餐盒费的单条商品总计
            BigDecimal total = obj.getBigDecimal("price").multiply(obj.getBigDecimal("quantity"));
//                    .add(obj.getBigDecimal("box_price").multiply(obj.getBigDecimal("box_num")));
            orderDetail.setTotal(total);
            orderDetail.setUnit(obj.getString("unit"));
            orderDetail.setRemark(obj.getString("food_property"));
            detailList.add(orderDetail);
        }

        return detailList;
    }

    /**
     * 取得订单主表的packageFee
     *
     * @param orderJson
     * @return Double
     */
    private BigDecimal getPackageFee(JSONObject orderJson) {
        BigDecimal totalPackageFee = new BigDecimal("0");
        JSONArray jsonArray = JSON.parseArray(orderJson.getString("detail"));
        for (int i = 0; i < jsonArray.size(); i++) {
            Object object = jsonArray.get(i);
            JSONObject obj = JSON.parseObject(object.toString());
            BigDecimal packageFee = obj.getBigDecimal("box_price").multiply(obj.getBigDecimal("box_num"));
            totalPackageFee = totalPackageFee.add(packageFee);
        }

        return totalPackageFee;
    }

    /**
     * 美团平台：回调服务返回参数Map
     */
    public Map<String, String> getParamsMap(String developerId, String ePoiId, String sign) {
        Map<String, String> params = Maps.newHashMap();
        params.put("developerId", developerId);
        params.put("ePoiId", ePoiId);
        params.put("sign", sign);

        return params;
    }

    /**
     * 校验美团传过来的参数sign是否正确
     *
     * @param params 参数Map
     * @return 布尔值
     */
    public boolean checkSign(Map<String, String> params) {
        return SignUtils.checkSign(MeituanInfoConst.SIGN_KEY, params);
    }

    /**
     * 修改美团订单表的配送状态
     *
     * @param #jsonObject 订单取消信息
     * @throws SQLException
     */
    private void updateOrderMtShippingStatus(ISqlAdapter adapter, JSONObject jsonObject) throws HitspException {
        String orderId = jsonObject.getString("orderId");
        Integer shippingStatus = jsonObject.getInteger("shippingStatus");
        Long time = jsonObject.getLong("time");
        String dispatcherName = jsonObject.getString("dispatcherName");
        String dispatcherMobile = jsonObject.getString("dispatcherMobile");
        SqlCommand mtCmd = null;
        switch (shippingStatus) {
            case 0:
                mtCmd = new SqlCommand(MeituanSqlConst.SQL_UPDATE_MT_SHIPPING_STATUS_SEND);
                break;
            case 10:
                mtCmd = new SqlCommand(MeituanSqlConst.SQL_UPDATE_MT_SHIPPING_STATUS_CONFIRM);
                break;
            case 20:
                mtCmd = new SqlCommand(MeituanSqlConst.SQL_UPDATE_MT_SHIPPING_STATUS_FECTH);
                break;
            case 40:
                mtCmd = new SqlCommand(MeituanSqlConst.SQL_UPDATE_MT_SHIPPING_STATUS_COMPLETED);
                break;
            case 100:
                mtCmd = new SqlCommand(MeituanSqlConst.SQL_UPDATE_MT_SHIPPING_STATUS_CANCEL);
                break;
            default:
                break;
        }
        if (mtCmd == null) {
            throw new HitspException("无该配送类型", adapter);
        }
        try {
            mtCmd.setParameter("logisticsStatus", shippingStatus);
            mtCmd.setParameter("dispatcherName", dispatcherName);
            mtCmd.setParameter("dispatcherMobile", dispatcherMobile);
            mtCmd.setParameter("shipperPhone", dispatcherMobile);
            mtCmd.setParameter("time", time);
            mtCmd.setParameter("orderId", orderId);
        } catch (SQLException e) {
            logger.error("修改美团订单表配送状态时，sql语句参数类型错误", e);
            throw new HitspException("修改美团订单表配送状态时，sql语句参数类型错误", e, adapter);
        }
        try {
            adapter.update(mtCmd);
        } catch (SQLException e) {
            logger.error("修改美团订单表配送状态时，sql语句参数类型错误", e);
            throw new HitspException("修改美团订单表配送状态时，sql语句参数类型错误", e, adapter);
        }
    }

    /**
     * 修改订单主表的配送状态
     *
     * @param #jsonObject 订单取消信息
     * @throws SQLException
     */
    private void updateOrderMasterShippingStatus(ISqlAdapter adapter, JSONObject jsonObject) throws HitspException {
        String orderId = jsonObject.getString("orderId");
        Integer shippingStatus = jsonObject.getInteger("shippingStatus");
        String dispatcherName = jsonObject.getString("dispatcherName");
        String dispatcherMobile = jsonObject.getString("dispatcherMobile");
        String status = "";
        SqlCommand mtCmd = null;
        switch (shippingStatus) {
            case 0:
            case 10:
            case 20:
                status = "2";
                break;
            case 40:
                status = "3";
                break;
            case 100:
                status = "1";
                break;
            default:
                status = "2";
                break;
        }

        SqlCommand masterCmd = new SqlCommand(MeituanSqlConst.SQL_UPDATE_MASTER_SHIP_STATUS);
        try {
            masterCmd.setParameter("status", status);
            masterCmd.setParameter("shippingType", "12");
            masterCmd.setParameter("shipperName", dispatcherName);
            masterCmd.setParameter("shipperPhone", dispatcherMobile);
            masterCmd.setParameter("orderId", orderId);
        } catch (SQLException e) {
            logger.error("修改订单主表配送状态时 ,数据库异常", e);
            throw new HitspException("修改订单主表配送状态时 ,数据库异常", e, adapter);
        }

        try {
            adapter.update(masterCmd);
        } catch (SQLException e) {
            logger.error("修改订单主表配送状态时 ,数据库异常", e);
            throw new HitspException("修改订单主表配送状态时 ,数据库异常", e, adapter);

        }
    }

    /**
     * 接收门店状态变更服务
     *
     * @param #developerId
     * @param #sign
     * @param #poiStatus   门店状态信息
     * @return
     */
    public Object shopStatus(String developerId, String sign, String poiStatus) throws HitspException {
        logger.info("接收门店状态变更: poiStatus={}", poiStatus);
        //TODO 校验sign
        JSONObject object = JSON.parseObject(poiStatus);
        String ePoiId = object.getString("ePoiId");
        int status = object.getInteger("poiStatus");

        ISqlAdapter adapter = dbAdapterManager.getAdapter("tenantdb");
        try {
            for (int i = 1; i <= DataBaseCount.databaseCount; i++) {
                try {
                    adapter.useDbName(CommonUtil.DB_NAME + i);
                } catch (SQLException e) {
                    logger.error(CommonUtil.DB_NAME + "数据库不存在", e);
                    throw new HitspException(CommonUtil.DB_NAME + "数据库不存在", e, adapter);
                }

                if (checkShopId(adapter, ePoiId)) {
                    break;
                }
            }


            String shopId = getShopIdByEPoiId(adapter, ePoiId);
            //TODO 更新数据表门店状态
            switch (status) {
                case 121:
                    updateShop(adapter, shopId, "1");
                    break;
                case 120:
                    updateShop(adapter, shopId, "0");
                    break;
                default:
                    break;
            }

            return CommonUtil.SUCCESS_RETURN;
        } finally {
            adapter.closeQuiet();
        }
    }

    /**
     * 接收订单退款类消息
     * 用户发起退款，确认或驳回退款，退款申诉等涉及退款的操作都会推送消息
     *
     * @param developerId
     * @param sign
     * @param ePoiId
     * @param orderRefund
     * @return
     */
    public Object refund(String developerId, String sign, String ePoiId, String orderRefund) throws HitspException, SQLException {
        logger.info("接收订单退款类消息: orderRefund={}", orderRefund);
        //TODO 校验sign
        Map<String, String> params = getParamsMap(developerId, ePoiId, sign);
        params.put("orderRefund", orderRefund);
        if (!checkSign(params)) {
            logger.info("美团平台传送数字签名sign不正确");
            return null;
        }


        JSONObject refundJson = JSON.parseObject(orderRefund);

        ISqlAdapter adapter = dbAdapterManager.getAdapter("tenantdb");
        try {
            String dbName = null;
            for (int i = 1; i <= DataBaseCount.databaseCount; i++) {
                try {
                    adapter.useDbName(CommonUtil.DB_NAME + i);

                } catch (SQLException e) {
                    logger.error(CommonUtil.DB_NAME + "数据库不存在", e);
                    throw new HitspException(CommonUtil.DB_NAME + "数据库不存在", e, adapter);
                }

                if (checkShopId(adapter, ePoiId)) {
                    dbName = CommonUtil.DB_NAME + i;
                    break;
                }
            }

            if (StringUtils.isEmpty(dbName)) {
                logger.error("数据库不存在");
                adapter.closeQuiet();
                return null;
            }
            String notifyType = refundJson.getString("notifyType");
            String mtstatus = "";
            String oldstatus = "";
            String o2ostatus = "";
            IPlatform iplatform = PlatFormFactory.getPlatform("12");
            switch (notifyType) {
                case "apply"://发起退款
                    logger.info("美团平台用户申请取消");
                    Map beforeStatusMap = orderViewService.getOrderIdByThirdOrderId(adapter, refundJson.getString("orderId"),
                            OrderSqlConst.SQL_SELECT_ID_STATUS_BY_ORDERID);
                    String status = (String) beforeStatusMap.get("status");
                    //保存退款前状态
                    commonOrderStatusUpdateService.updateOrderMasterStatusBeforeRefund(adapter, refundJson.getString("orderId"),
                            status, OrderSqlConst.SQL_STATUS_BEFORE_REFUND_UPDATE_MASTER);
                    //更新订单主表状态（由于美团无退单状态，所以暂不修改美团订单状态）
                    commonOrderStatusUpdateService.updateOrderMasterStatus(adapter, refundJson.getString("orderId"),
                            "cancelByUser", OrderSqlConst.SQL_STATUS_UPDATE_MASTER_THIRD);
                    logger.info("美团平台用户申请取消操作保存成功");
                    //给小微推送退款申请
                    commonOrderStatusUpdateService.commonPutStatusToXw(adapter, dbName, refundJson, xiaoweiUrlStatus);
                    break;
                case "agree"://确认退款
                    logger.info("美团平台确认退款");
                    //更新美团订单表、订单主表状态为取消
                    mtstatus = "9";//已取消
                    commonOrderStatusUpdateService.refundUpdateOrderMtStatus(adapter, refundJson.getString("orderId"),
                            mtstatus, OrderSqlConst.SQL_STATUS_UPDATE_MT_THIRD);
                    commonOrderStatusUpdateService.updateOrderMasterStatus(adapter, refundJson.getString("orderId"),
                            "agreeRefund", OrderSqlConst.SQL_STATUS_UPDATE_MASTER_THIRD);
                    //给小微推送美团已确认退款
                    commonOrderStatusUpdateService.commonPutStatusToXw(adapter, dbName, refundJson, xiaoweiUrlStatus);
                    break;
                case "reject"://驳回退款
                    logger.info("美团平台驳回退款");
                    //获取美团当前订单状态
                    Object rejectstatus = iplatform.getOrderStatus(adapter, refundJson.getString("orderId"));
                    if (rejectstatus == null) {
                        throw new HitspException("获取美团当前订单状态异常", adapter);
                    }
                    mtstatus = String.valueOf(rejectstatus);//美团订单表状态
                    //更新美团订单表、订单主表状态
                    commonOrderStatusUpdateService.refundUpdateOrderMtStatus(adapter, refundJson.getString("orderId"),
                            mtstatus, OrderSqlConst.SQL_STATUS_UPDATE_MT_THIRD);
                    //获取退款前状态
                    oldstatus = commonOrderStatusUpdateService.getOrderMasterStatusBeforeRefund(adapter, refundJson.getString("orderId"),
                            OrderSqlConst.SQL_STATUS_BEFORE_REFUND_SELECT_MASTER);
                    commonOrderStatusUpdateService.refundUpdateOrderMasterStatus(adapter, refundJson.getString("orderId"),
                            oldstatus, OrderSqlConst.SQL_STATUS_UPDATE_MASTER_THIRD);
                    //给小微推送已经驳回退款
                    commonOrderStatusUpdateService.commonPutStatusToXw(adapter, dbName, refundJson, xiaoweiUrlStatus);
                    break;
                case "cancelRefund"://用户取消退款申请
                    logger.info("美团平台用户取消退款申请");
                    //获取美团当前订单状态
                    Object cancelstatus = iplatform.getOrderStatus(adapter, refundJson.getString("orderId"));
                    if (cancelstatus == null) {
                        throw new HitspException("获取美团当前订单状态异常", adapter);
                    }
                    mtstatus = String.valueOf(cancelstatus);//美团订单表状态
                    //更新美团订单表、订单主表状态
                    commonOrderStatusUpdateService.refundUpdateOrderMtStatus(adapter, refundJson.getString("orderId"),
                            mtstatus, OrderSqlConst.SQL_STATUS_UPDATE_MT_THIRD);
                    //获取退款前状态
                    oldstatus = commonOrderStatusUpdateService.getOrderMasterStatusBeforeRefund(adapter, refundJson.getString("orderId"),
                            OrderSqlConst.SQL_STATUS_BEFORE_REFUND_SELECT_MASTER);
                    commonOrderStatusUpdateService.refundUpdateOrderMasterStatus(adapter, refundJson.getString("orderId"),
                            oldstatus, OrderSqlConst.SQL_STATUS_UPDATE_MASTER_THIRD);
                    //给小微推送用户已经取消退款
                    commonOrderStatusUpdateService.commonPutStatusToXw(adapter, dbName, refundJson, xiaoweiUrlStatus);
                    break;
//            case "cancelRefundComplaint"://取消退款申诉
//                break;
                default:
                    break;
            }
            CommonUtil.adapterCommitAndClose(adapter);
            return CommonUtil.SUCCESS_RETURN;
        } finally {
            adapter.closeQuiet();
        }


    }

    private void updateShop(ISqlAdapter adapter, String shopId, String isOpen) throws HitspException {

        SqlCommand cmd = new SqlCommand(ShopSqlConst.SQL_UPDATE_SHOP);
        SqlCommand cmdMt = new SqlCommand(ShopSqlConst.SQL_UPDATE_MT_SHOP);
        try {
            cmd.setParameter("isOpen", isOpen);
            cmd.setParameter("shopId", shopId);
            adapter.update(cmd);

            cmdMt.setParameter("isOpen", isOpen);
            cmdMt.setParameter("shopId", shopId);
            adapter.update(cmdMt);
        } catch (SQLException e) {
            logger.error("美团回调时，更新门店营业状态时异常", e);
            throw new HitspException("美团回调时，更新门店营业状态时异常", e, adapter);
        }

        CommonUtil.adapterCommitAndClose(adapter);
    }


    /**
     * 保存美团平台的门店认证token
     * 若门店账户信息存在，更新账户信息; 否则，新增一条门店账户信息
     *
     * @param ePoiId
     * @param token
     */
    private void saveAccountToken(ISqlAdapter adapter, String ePoiId, String token) throws HitspException {
        if (checkAccountIsExist(adapter, ePoiId)) {
            updateAccount(adapter, ePoiId, token);
        } else {
            saveAccount(adapter, ePoiId, token);
        }

        //TODO 打印日志供调试时使用
        logger.info("保存美团账户的门店token成功");
    }

    private void saveAccount(ISqlAdapter adapter, String ePoiId, String token) throws HitspException {

        SqlCommand accountCmd = new SqlCommand(MeituanSqlConst.SQL_INSERT_ACCOUNT);
        SqlCommand accountShopCmd = new SqlCommand(MeituanSqlConst.SQL_INSERT_ACCOUNT_SHOP);

        String accountId = CommonUtil.generateId();
        try {
            accountCmd.setParameter("id", Converter.toLong(accountId));
            accountCmd.setParameter("platform", "12");
            accountShopCmd.setParameter("id", accountId);
            accountShopCmd.setParameter("shopId", ePoiId);
            accountShopCmd.setParameter("token", token);
            adapter.update(accountCmd);
            adapter.update(accountShopCmd);
        } catch (SQLException e) {
            logger.error("保存美团账户的门店token时数据库异常", e);
            throw new HitspException("保存美团账户的门店token时数据库异常", e, adapter);

        }
    }

    private String getShopIdByEPoiId(ISqlAdapter adapter, String ePoiId) throws HitspException {
        if (StringUtils.isEmpty(ePoiId)) {
            return null;
        }

        DataTable dt = null;
        try {
            SqlCommand cmd = new SqlCommand(MeituanSqlConst.SQL_SELECT_SHOP_MT_BY_EPOIID);
            cmd.setParameter("ePoiId", ePoiId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("校验账户是否存在时数据库抛出异常", e);
            throw new HitspException("校验账户是否存在时数据库抛出异常", e, adapter);
        }

        if (dt == null || dt.size() == 0) {
            return null;
        }

        return dt.get(0, "id");
    }

    private void updateAccount(ISqlAdapter adapter, String ePoiId, String token) throws HitspException {

        SqlCommand accountCmd = new SqlCommand(MeituanSqlConst.SQL_UPDATE_ACCOUNT);
        SqlCommand accountShopCmd = new SqlCommand(MeituanSqlConst.SQL_UPDATE_ACCOUNT_SHOP);

        try {
            accountCmd.setParameter("platform", "12");
            accountCmd.setParameter("token", null);
            String shopId = getShopIdByEPoiId(adapter, ePoiId);
            accountCmd.setParameter("shopId", shopId);
            accountShopCmd.setParameter("shopId", shopId);
            accountShopCmd.setParameter("token", token);
            adapter.update(accountCmd);
            adapter.update(accountShopCmd);
        } catch (SQLException e) {
            logger.error("更新美团账户的门店token时数据库异常", e);
            throw new HitspException("更新美团账户的门店token时数据库异常", e, adapter);
        }
    }


    /**
     * 根据门店Id，校验账户是否存在
     *
     * @param shopId
     */
    private boolean checkAccountIsExist(ISqlAdapter adapter, String shopId) throws HitspException {
        if (StringUtils.isEmpty(shopId)) {
            return false;
        }

        DataTable dt = null;
        try {
            SqlCommand cmd = new SqlCommand(MeituanSqlConst.SQL_SELECT_ACCOUNT_SHOP);
            cmd.setParameter("shopId", shopId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("校验账户是否存在时数据库抛出异常", e);
            throw new HitspException("校验账户是否存在时数据库抛出异常", e, adapter);
        }

        if (dt == null || dt.size() == 0) {
            return false;
        }

        return true;
    }

    private DataTable getProductInfoByMtProductId(ISqlAdapter adapter, String mtProductId) throws HitspException {

        if (adapter == null) {
            logger.error("数据库连接不存在");
            return null;
        }

        DataTable dt = null;
        try {
            SqlCommand cmd = new SqlCommand(MeituanSqlConst.SQL_SELECT_PRODUCT_BY_MTID);
            cmd.setParameter("productId", mtProductId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("根据美团菜品编码获取菜品信息时数据库抛出异常", e);
            throw new HitspException("根据美团菜品编码获取菜品信息时数据库抛出异常", e, adapter);
        }

        return dt;
    }

    private String getOrderIdByErpOrderId(ISqlAdapter adapter, String erpOrderId) throws HitspException {
        DataTable dt = null;
        try {
            SqlCommand cmd = new SqlCommand(MeituanSqlConst.SQL_SELECT_ORDER_MASTER_BY_ORDERID);
            cmd.setParameter("orderId", erpOrderId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("根据美团订单Id查询订单时数据库抛出异常", e);
            throw new HitspException("根据美团订单Id查询订单时数据库抛出异常", e, adapter);
        }

        if (dt == null || dt.size() == 0) {
            return null;
        }

        return dt.get(0, "id");
    }

    private boolean checkShopId(ISqlAdapter adapter, String ePoiId) throws HitspException {
        DataTable dt = null;
        try {
            SqlCommand cmd = new SqlCommand(MeituanSqlConst.SQL_SELECT_SHOP_BY_EPOIID);
            cmd.setParameter("ePoiId", ePoiId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("根据门店查找数据库抛出异常", e);
            throw new HitspException("根据门店查找数据库抛出异常", e, adapter);
        }

        if (dt == null || dt.size() == 0) {
            return false;
        }

        return true;
    }

    private boolean checkMtOrderIsExist(ISqlAdapter adapter, String orderId) throws HitspException {
        DataTable dt = null;
        try {
            SqlCommand cmd = new SqlCommand(MeituanSqlConst.SQL_SELECT_ORDER_MT);
            cmd.setParameter("orderId", orderId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("校验美团订单是否已存在时数据库抛出异常", e);
            throw new HitspException("校验美团订单是否已存在时数据库抛出异常", e, adapter);

        }

        if (dt == null || dt.size() == 0) {
            return false;
        }

        return true;

    }

    public Object privacyNumDowngrade(String developerId, String timestamp, String sign) throws HitspException {
        //调用异步拉取隐私号真实号方法
        privacyNumMQSendService.getDowngradePhone("12");
        return CommonUtil.SUCCESS_RETURN;
    }
}