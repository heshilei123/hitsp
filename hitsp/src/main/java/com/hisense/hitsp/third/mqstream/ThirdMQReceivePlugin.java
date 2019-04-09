package com.hisense.hitsp.third.mqstream;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.hisense.dustcore.util.Converter;
import com.hisense.dustdb.DbAdapterManager;
import com.hisense.dustdb.TenantAdapterManager;
import com.hisense.dustdb.sql.DataTable;
import com.hisense.dustdb.sql.ISqlAdapter;
import com.hisense.dustdb.sql.SqlCommand;
import com.hisense.dustms.stream.IPlugin;
import com.hisense.hitsp.common.CommonOrderStatusUpdateService;
import com.hisense.hitsp.common.CommonUtil;
import com.hisense.hitsp.common.HitspException;
import com.hisense.hitsp.config.PushYmlConfig;
import com.hisense.hitsp.model.Order;
import com.hisense.hitsp.model.OrderDetail;
import com.hisense.hitsp.service.OrderSqlConst;
import com.hisense.hitsp.service.OrderViewService;
import com.hisense.hitsp.service.PushService;
import com.hisense.hitsp.third.IPlatform;
import com.hisense.hitsp.third.PlatFormFactory;
import com.hisense.hitsp.third.common.ThirdPlatformCommonUtil;
import com.hisense.hitsp.third.eb.EbSqlConst;
import com.hisense.hitsp.third.eleme.service.ElemeSqlConst;
import com.hisense.hitsp.third.jingdong.JingDongImpl;
import com.hisense.hitsp.third.jingdong.service.JingdongSqlConst;
import eleme.openapi.sdk.config.Config;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * 处理来源于第三方平台的消息
 *
 * @author yanglei
 *         date: 2017-04-20.
 */
@Component
public class ThirdMQReceivePlugin implements IPlugin {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    DbAdapterManager dbAdapterManager;
    @Autowired
    TenantAdapterManager tenantAdapterManager;

    @Autowired
    CommonOrderStatusUpdateService commonOrderStatusUpdateService;

    @Autowired
    OrderViewService orderViewService;
    @Autowired
    PushService pushService;
    @Autowired
    PushYmlConfig pushYmlConfig;
    private String xiaoweiUrl;

    @PostConstruct
    private void init() {
        xiaoweiUrl = pushYmlConfig.getXiaoweiUrl();
    }

    @Autowired
    @Qualifier("taskExecutor")
    Executor executor;

    @Override
    public void run(Object data) {
        logger.info(data.toString());

        executor.execute(new Runnable() {
            @Override
            public void run() {
                logger.info("进入MQ执行类");
                ISqlAdapter adapter = dbAdapterManager.getAdapter("tenantdb");
                if (adapter == null) {
                    logger.error("数据库连接不存在");
                    return;
                }
                String dbName = ((JSONObject) data).getString("dbName");
                try {
                    adapter.useDbName(dbName);
                } catch (SQLException e) {
                    logger.error(dbName + "数据库不存在", e);
                    return;
                }
                try {
                    String operateType = ((JSONObject) data).getString("do");
                    String orderId = ((JSONObject) data).getString("orderId");
                    String o2oOrderId = ((JSONObject) data).getString("o2oOrderId");
                    String orderFrom = ((JSONObject) data).getString("orderFrom");
                    String source = ((JSONObject) data).getString("source");
                    String status = orderViewService.getOrderStatusByOrderId(adapter, orderId);
                    //判断当前状态是否可变
                    if (StringUtils.isNotEmpty(orderFrom)) {
                        switch (orderFrom) {
                            case "11":
                                //TODO 对接百度平台状态更新
                                break;
                            case "12":
                                commonOrderStatusUpdateService.updateOrderMtStatus(adapter, orderId,
                                        operateType, OrderSqlConst.SQL_STATUS_UPDATE_MT_THIRD);
                                break;
                            case "15":
                                if (StringUtils.equals("save", operateType)) {
                                    long startTime = System.currentTimeMillis();
                                    logger.info("o2o保存饿了么订单startTime={}", startTime);
                                    Order order = null;
                                    try {
                                        order = convertOrderElemeToOrderObj04(adapter, orderId);
                                    } catch (HitspException e) {
                                        logger.error("保存饿了么订单时转化饿了么订单为o2o订单时异常 ", e);
                                    }
                                    logger.info("order = {}", order);
                                    if (order == null) {
                                        logger.info("order = {}", order);
                                        return;
                                    }
                                    ThirdPlatformCommonUtil.saveOrderMaster04(adapter, order);
                                    ThirdPlatformCommonUtil.saveOrderDetail(adapter, order);

                                    Order orderMaster = null;
                                    try {
                                        orderMaster = orderViewService.getOrderDetailById(orderId, adapter);
                                    } catch (HitspException e) {
                                        logger.error("保存饿了么订单时查询订单明细异常", e);
                                    }
                                    logger.info("orderMaster = {}", orderMaster);
                                    if (orderMaster == null) {
                                        logger.info("orderMaster = {}", orderMaster);
                                        return;
                                    }

//                            order = ThirdPlatformCommonUtil.orderToErpOrder(adapter, orderMaster);
//                            if (StringUtils.equals(CommonUtil.DB_NAME+"2", dbName)) {
//                                //TODO push判断是否是小微的订单
//                                Map<String, Object> params = new HashMap<>();
//                                params.put("order", orderMaster);
//                                pushService.circlePush(xiaoweiUrl, params);
//                            }
                                    long endTime = System.currentTimeMillis();
                                    logger.info("o2o保存饿了么订单endTime={}, 处理时间time={}", endTime, endTime - startTime);
                                } else if (StringUtils.equals("shipped", operateType)) {
                                    //判断该订单状态不为已完成、已取消，则会更新状态为已送达
                                    if (StringUtils.isNotEmpty(status) && !StringUtils.equals(status, "4") && !StringUtils.equals(status, "5")) {
                                        commonOrderStatusUpdateService.updateOrderElemeStatus(adapter, orderId,
                                                operateType, OrderSqlConst.SQL_STATUS_UPDATE_ELM_THIRD);
                                        commonOrderStatusUpdateService.updateOrderMasterStatus(adapter, orderId, operateType,
                                                OrderSqlConst.SQL_STATUS_UPDATE_MASTER_THIRD);
                                    }
                                } else {
                                    commonOrderStatusUpdateService.updateOrderElemeStatus(adapter, orderId,
                                            operateType, OrderSqlConst.SQL_STATUS_UPDATE_ELM_THIRD);
                                }

                                break;
                            case "16":
                                logger.info("进入京东拉单");
                                if (StringUtils.equals("save", operateType)) {
                                    getJdOrder(dbName, orderId, o2oOrderId, source);
                                }
                                break;
                            case "17":
                                logger.info("进入EB拉单");
                                if (StringUtils.equals("save", operateType)) {
                                    getEbOrder(dbName, orderId, o2oOrderId, source);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    if (!StringUtils.equals("shipped", operateType) && !StringUtils.equals(orderFrom, "17") && !StringUtils.equals(orderFrom, "16")) {
                        commonOrderStatusUpdateService.updateOrderMasterStatus(adapter, orderId, operateType,
                                OrderSqlConst.SQL_STATUS_UPDATE_MASTER_THIRD);
                    }

                    try {
                        adapter.commit();
                    } catch (SQLException e) {
                        logger.error("推送订单状态消息时，提交更新订单状态失败", e);
                        try {
                            adapter.rollback();
                        } catch (SQLException ex) {
                            logger.error("推送订单状态消息时，回滚更新订单状态失败", ex);
                            return;
                        }
                    } finally {
                        adapter.closeQuiet();
                    }
                    logger.info("外卖平台MQ消息接收处理结束");
                } finally {
                    adapter.closeQuiet();
                }
            }
        });
    }

    private Order convertOrderElemeToOrderObj04(ISqlAdapter adapter, String orderId) throws HitspException {
//        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        DataTable dtOrderEleme = getOrderElemeInfo(adapter, orderId);
        if (dtOrderEleme == null || dtOrderEleme.size() == 0) {
            return null;
        }

        Order order = new Order();
        if (!Converter.toBoolean(dtOrderEleme.get(0, "book"))) {
            //若该订单非预订单
            order.setDeliverTime(null);
        } else {
            //若该订单为预订单
            formatter.setTimeZone(TimeZone.getTimeZone("GMT+8"));
//            Date b = formatter.parse(a);
//            order.setDeliverTime(Converter.toDate(dtOrderEleme.get(0, "deliverTime")));
            try {
                order.setDeliverTime(formatter.parse(dtOrderEleme.get(0, "deliverTime")));
            } catch (ParseException e) {
                logger.error("预计送达时间处理错误");
            }
        }
        order.setOrderId(dtOrderEleme.get(0, "orderId"));
        order.setId(Converter.toLong(orderId));

        String udate = dtOrderEleme.get(0, "activeAt");
        String cdate = dtOrderEleme.get(0, "createdAt");
        try {
            Date uDate = df.parse(udate);
            Date cDate = df.parse(cdate);
            order.setGmtCreate(cDate);
            order.setGmtModify(uDate);
        } catch (ParseException e) {
            logger.error("下单时间处理错误");
        }

        order.setOrderFrom("15");
        order.setTotalPrice(Converter.toBigDecimal(dtOrderEleme.get(0, "totalPrice")));
        order.setOriginalPrice(Converter.toBigDecimal(dtOrderEleme.get(0, "originalPrice")));
        order.setStatus("0");
        String elemeShopId = dtOrderEleme.get(0, "shopId");
        DataTable dtShop = getShopInfoByElemeShopId(adapter, elemeShopId);
        if (dtShop == null || dtShop.size() == 0) {
            logger.error("饿了么({})门店不存在", elemeShopId);
            return null;
        }

        order.setShopId(dtShop.get(0, "id"));
        order.setShopName(dtShop.get(0, "name"));
//        order.setShopAddress(dtOrderEleme.get(0, "address"));
        order.setShopAddress("");//饿了么推送订单中无门店地址，暂置为空
        order.setShopPhone(null);
        order.setRecipientName(dtOrderEleme.get(0, "consignee"));
        order.setRecipientAddress(dtOrderEleme.get(0, "deliveryPoiAddress"));

        StringBuilder phones = new StringBuilder();
        JSONArray phoneList = JSON.parseArray(dtOrderEleme.get(0, "phoneList"));
        if (phoneList != null) {
            for (int i = 0; i < phoneList.size(); i++) {
                String phone = phoneList.getString(i);
                if (i != 0) {
                    phones.append(";");
                }
                phones.append(phone);
            }
        }
        order.setRecipientPhone(phones.toString());

        order.setRecipientLongitude(Converter.toBigDecimal(dtOrderEleme.get(0, "deliveryGeo").split(",")[0]));
        order.setRecipientLatitude(Converter.toBigDecimal(dtOrderEleme.get(0, "deliveryGeo").split(",")[1]));
        order.setShippingType("10");
        order.setShippingFee(dtOrderEleme.get(0, "deliverFee") == null ? BigDecimal.valueOf(0) : Converter.toBigDecimal(dtOrderEleme.get(0, "deliverFee")));
        order.setShipperName(null);
        order.setShipperPhone(null);
        order.setHasInvoiced(Converter.toBoolean(dtOrderEleme.get(0, "invoiced")));
        order.setInvoiceTitle(dtOrderEleme.get(0, "invoice"));
        order.setPackageFee(Converter.toBigDecimal(dtOrderEleme.get(0, "packageFee")));
        order.setPayType(Converter.toBoolean(dtOrderEleme.get(0, "onlinePaid")) == false ? "1" : "2");
        order.setCaution(dtOrderEleme.get(0, "description"));
        order.setRemark("");
        BigDecimal shopPart = Converter.toBigDecimal(dtOrderEleme.get(0, "shopPart"));
        if (shopPart.signum() == -1) {
            shopPart = shopPart.abs();
        }
        order.setShopPart(shopPart);
        order.setShopIncome(Converter.toBigDecimal(dtOrderEleme.get(0, "income")));
        BigDecimal serviceFee = Converter.toBigDecimal(dtOrderEleme.get(0, "serviceFee"));
        if (serviceFee.signum() == -1) {
            serviceFee = serviceFee.abs();
        }
        order.setServiceFee(serviceFee);
//        List<OrderDetail> detailList = convertOrderElemeToOrderDetailList(adapter, dtOrderEleme);
        List<OrderDetail> detailList = convertOrderElemeToOrderDetailList04(adapter, dtOrderEleme, dtShop.get(0, "id"));
        order.setDetail(detailList);
        return order;
    }

    private List<OrderDetail> convertOrderElemeToOrderDetailList04(ISqlAdapter adapter, DataTable dtOrderEleme, String shopId) throws HitspException {
        logger.info("该订单所属门店ID：" + shopId);
        List<OrderDetail> detailList = Lists.newArrayList();
        JSONArray jsonArray = JSON.parseArray(dtOrderEleme.get(0, "groups"));
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject group = (JSONObject) jsonArray.get(i);
            JSONArray itemsArray = group.getJSONArray("items");
            for (int j = 0; j < itemsArray.size(); j++) {
                JSONObject item = (JSONObject) itemsArray.get(j);
                OrderDetail orderDetail = new OrderDetail();

                String elemeProductId = item.getString("id");
                if (!StringUtils.equals(elemeProductId, "-70000")) {//若不为餐盒
                    String extendCode = item.getString("extendCode");
                    logger.info("该订单extendCode为：" + extendCode);
                    DataTable dtProductInfo = null;
                    //extendCode字段为商家版添加商品时所带erp商品id，若不为空，直接根据该erpid查询商品信息
                    if (StringUtils.isNotEmpty(extendCode)) {
                        dtProductInfo = getProductInfoByErpId(adapter, extendCode, shopId);
                        if (dtProductInfo == null || dtProductInfo.size() == 0) {
                            logger.error("ERP菜品({})不存在", extendCode);
                            throw new HitspException("ERP菜品不存在", adapter);
                        }
                    } else {
                        dtProductInfo = getProductInfoByElemeProductId(adapter, elemeProductId);
                        if (dtProductInfo == null || dtProductInfo.size() == 0) {
                            logger.error("饿了么菜品({})不存在", elemeProductId);
                            throw new HitspException("饿了么菜品不存在", adapter);
                        }
                    }

                    orderDetail.setOrderId(dtOrderEleme.get(0, "id"));
                    orderDetail.setCode(dtProductInfo.get(0, "id"));
                    orderDetail.setName(dtProductInfo.get(0, "name"));
                    orderDetail.setCategoryId(dtProductInfo.get(0, "category_id"));
//                orderDetail.setCode(item.getString("skuId"));
//                orderDetail.setName(item.getString("name"));
//                orderDetail.setCategoryId(item.getString("categoryId"));
                    orderDetail.setPrice(item.getBigDecimal("price"));
                    orderDetail.setQuantity(item.getBigDecimal("quantity"));
                    orderDetail.setTotal(item.getBigDecimal("total"));
                    orderDetail.setUnit(dtProductInfo.get(0, "spec"));
//                    orderDetail.setUnit("");//规格不是单位，暂时置为空 by hsl 2017年9月14日09:55:52

                    StringBuilder remark = new StringBuilder();
                    JSONArray newSpecs = item.getJSONArray("newSpecs");
                    JSONArray attributes = item.getJSONArray("attributes");
                    if (newSpecs != null && newSpecs.size() != 0) {
                        getOrderDetailRemark(remark, newSpecs);
                    }

                    if (attributes != null && attributes.size() != 0) {
                        if (remark.length() != 0) {
                            remark.append(",");
                        }

                        getOrderDetailRemark(remark, attributes);
                    }
                    orderDetail.setRemark(remark.toString());

                    detailList.add(orderDetail);
                }
//        else {//若为餐盒
//            //TODO 校验饿了么是否在主单中推送了餐盒费，若未推送，则需要在此处统计餐盒费总计
//
//        }
            }
        }

        return detailList;
    }


    private DataTable getOrderElemeInfo(ISqlAdapter adapter, String orderId) throws HitspException {
        DataTable dt = null;
        try {
            SqlCommand cmd = new SqlCommand(ElemeSqlConst.SQL_SELECT_ELM);
            cmd.setParameter("id", orderId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("获取饿了么订单明细时抛出异常", e);
            throw new HitspException("获取饿了么订单明细时数据库抛出异常", e, adapter);

        }

        return dt;
    }

    private DataTable getShopInfoByElemeShopId(ISqlAdapter adapter, String shopId) {

        DataTable dt = null;
        try {
            SqlCommand cmd = new SqlCommand(ElemeSqlConst.SQL_SELECT_SHOP_BY_ELEME_SHOPID);
            cmd.setParameter("shopId", shopId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("根据饿了么门店Id获取门店信息时数据库抛出异常", e);
            return null;
        }

        return dt;
    }

    private DataTable getProductInfoByElemeProductId(ISqlAdapter adapter, String elemeProductId) {
        DataTable dt = null;
        try {
            SqlCommand cmd = new SqlCommand(ElemeSqlConst.SQL_SELECT_PRODUCT_BY_ELEME_ID);
            cmd.setParameter("elemeProductId", elemeProductId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("根据饿了么菜品Id获菜品信息时数据库抛出异常", e);
            return null;
        }

        return dt;
    }

    private DataTable getProductInfoByErpId(ISqlAdapter adapter, String erpId, String shopId) {
        DataTable dt = null;
        try {
            SqlCommand cmd = new SqlCommand(ElemeSqlConst.SQL_SELECT_PRODUCT_BY_ERP_ID);
            cmd.setParameter("shopId", shopId);
            cmd.setParameter("erpId", erpId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("根据饿了么菜品Id获菜品信息时数据库抛出异常", e);
            return null;
        }

        return dt;
    }

    private void getOrderDetailRemark(StringBuilder remark, JSONArray tasteArray) {
        for (int m = 0; m < tasteArray.size(); m++) {
            JSONObject attribute = tasteArray.getJSONObject(m);
            remark.append(attribute.getString("name") + ":" + attribute.getString("value"));
            if (m != tasteArray.size() - 1) {
                remark.append(",");
            }
        }
    }

    private void getJdOrder(String dbName, String orderId, String o2oOrderId, String appKey) {
        logger.info("拉单开始dbName{},orderId{},o2oOrderId{}", dbName, orderId, o2oOrderId);
        try {
            //拉取京东订单并保存
            JSONObject orderJson = getJdOrderAndSave(dbName, orderId, o2oOrderId, appKey);
            JSONObject orderTotalSplitJson = getJdOrderTotalSplit(dbName, orderId, appKey);
            if (orderJson != null) {
                //保存O2O订单主表及明细表
                saveJdOrderToO2oOrder(dbName, orderId, o2oOrderId, orderJson, orderTotalSplitJson);
            }
        } catch (Exception ex) {
            logger.error("拉取京东订单并保存出现异常", ex);
        }
    }

    private void getEbOrder(String dbName, String orderId, String o2oOrderId, String source) {
        logger.info("拉单开始dbName{},orderId{},o2oOrderId{}", dbName, orderId, o2oOrderId);
        try {
            //拉取饿百订单并保存
            JSONObject orderJson = getEbOrderAndSave(dbName, orderId, o2oOrderId, source);
            if (orderJson != null) {
                //保存O2O订单主表及明细表
                saveEbOrderO2oOrder(dbName, orderId, o2oOrderId, orderJson);
            }
        } catch (Exception ex) {
            logger.error("拉取饿百订单并保存出现异常", ex);
        }
    }

    private void saveEbOrderO2oOrder(String dbName, String orderId, String o2oOrderId, JSONObject orderJson) throws Exception {
        ISqlAdapter adapter = dbAdapterManager.getAdapter("tenantdb");
        if (adapter == null) {
            logger.error("数据库连接不存在");
            return;
        }
        try {
            adapter.useDbName(dbName);
        } catch (SQLException e) {
            logger.error(dbName + "数据库不存在", e);
            return;
        }
        try {
            //保存O2O订单
            saveEbOrderToO2oOrder(adapter, o2oOrderId, orderJson);
            adapter.commit();
        } catch (Exception ex) {
            adapter.rollbackQuiet();
            throw ex;
        } finally {
            adapter.closeQuiet();
        }
    }

    private void saveEbOrderToO2oOrder(ISqlAdapter adapter, String o2oOrderId, JSONObject ebOrderJson) throws Exception {
        //转换为O2O order 对象
        Order order = ebCoverToO2oOrder(adapter, o2oOrderId, ebOrderJson);
        //保存O2O order 主表
        saveO2oOrderMaster(adapter, order);
        //保存O2O order 明细表
        saveO2oOrderDetail(adapter, order);
    }

    public static void main(String[] args) throws ParseException {
        Date createTime = CommonUtil.timestampToDate((Long.valueOf("1544679459")) * 1000L);
        System.out.println(createTime);
    }

    private Order ebCoverToO2oOrder(ISqlAdapter adapter, String o2oOrderId, JSONObject ebOrderJson) throws Exception {
        logger.info("饿百订单转换为O2O订单开始");
        JSONObject data = ebOrderJson.getJSONObject("data");
        JSONObject ebOrder = data.getJSONObject("order");
        JSONObject shop = data.getJSONObject("shop");
        JSONObject user = data.getJSONObject("user");
        logger.info("饿百订单order{}", ebOrder);
        logger.info("饿百订单shop{}", shop);
        logger.info("饿百订单user{}", user);
        Order order = new Order();
        if (StringUtils.equals(ebOrder.getString("send_immediately"), "2")) {
            Long lo = (Long.valueOf(ebOrder.getString("send_time"))) * 1000L;
            order.setDeliverTime(CommonUtil.timestampToDate(lo));
        } else {
            order.setDeliverTime(null);
        }

        order.setOrderId(ebOrder.getString("order_id"));
        order.setId(Converter.toLong(o2oOrderId));
        Date createTime = CommonUtil.timestampToDate((Long.valueOf(ebOrder.getString("create_time"))) * 1000L);
        order.setGmtModify(createTime);
        order.setGmtCreate(createTime);
        order.setOrderFrom("17");
        Integer totalPrice = ebOrder.getInteger("total_fee") - ebOrder.getInteger("discount_fee");
        order.setTotalPrice(new BigDecimal(totalPrice).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP));
        order.setOriginalPrice(new BigDecimal(ebOrder.getInteger("total_fee")).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP));
        order.setStatus("0");
        order.setShopId(shop.getString("id"));
        order.setShopName(shop.getString("name"));
        order.setShopAddress("");//饿百订单中无门店地址，暂置为空
        order.setShopPhone(null);
        order.setRecipientName(user.getString("name"));
        order.setRecipientAddress(user.getString("address"));
        order.setRecipientPhone(user.getString("phone"));
        JSONObject coord = user.getJSONObject("coord");
        order.setRecipientLongitude(Converter.toBigDecimal(coord.getString("longitude")));
        order.setRecipientLatitude(Converter.toBigDecimal(coord.getString("latitude")));
        String shipType = "00";
        if (!StringUtils.equals(ebOrder.getString("delivery_party"), "2") && !StringUtils.equals(ebOrder.getString("delivery_party"), "7")) {
            switch (ebOrder.getString("delivery_party")) {
                case "1":
                    shipType = "10";//蜂鸟专送
                    break;
                case "3":
                    shipType = "11";//蜂鸟众包
                    break;
                case "4":
                    shipType = "12";//饿了么众包
                    break;
                case "5":
                    shipType = "10";//蜂鸟专送
                    break;
                case "7":
                    shipType = "13";//全城送
                    break;
                case "8":
                    shipType = "14";//快递配送
                    break;
                default:
                    break;
            }
        }
        order.setShippingType(shipType);
        order.setShippingFee(ebOrder.getInteger("send_fee") == 0 ? new BigDecimal(0) : new BigDecimal(ebOrder.getInteger("send_fee")).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP));
        order.setShipperName(null);
        order.setShipperPhone(ebOrder.getString("delivery_phone"));
        order.setHasInvoiced(StringUtils.equals(ebOrder.getString("need_invoice"), "1") ? true : false);
        order.setInvoiceTitle(ebOrder.getString("invoice_title"));
        order.setPackageFee(ebOrder.getInteger("package_fee") == 0 ? new BigDecimal(0) : new BigDecimal(ebOrder.getInteger("package_fee")).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP));
        order.setPayType(ebOrder.getString("pay_type"));
        JSONObject ext = ebOrder.getJSONObject("ext");
        order.setCaution(ext.containsKey("greeting") ? ext.getString("greeting") : "");
        order.setRemark("");
        Integer shop_rate = 0;
        JSONArray discount = data.getJSONArray("discount");
        for (int i = 0; i < discount.size(); i++) {
            JSONObject oneDis = discount.getJSONObject(i);
            shop_rate = oneDis.getInteger("shop_rate") + shop_rate;
        }
        order.setShopPart(shop_rate == 0 ? new BigDecimal(0) : new BigDecimal(shop_rate).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP));
        //店铺实际收入计算，用户实付-服务费-配送费
        Integer shopPart = ebOrder.getInteger("user_fee") - ebOrder.getInteger("send_fee") - ebOrder.getInteger("commission");
        order.setShopIncome(shopPart == 0 ? new BigDecimal(0) : new BigDecimal(shopPart).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP));
        //饿百平台服务费
        order.setServiceFee(ebOrder.getInteger("commission") == 0 ? new BigDecimal(0) : new BigDecimal(ebOrder.getInteger("commission")).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP));
        JSONArray products = data.getJSONArray("products");
        List<OrderDetail> detailList = convertOrderEbToOrderDetailList(adapter, o2oOrderId, ebOrder.getString("order_id"), products, shop.getString("id"));
        order.setDetail(detailList);
        return order;
    }

    private List<OrderDetail> convertOrderEbToOrderDetailList(ISqlAdapter adapter, String o2oOrderId, String ebOrderId, JSONArray products, String shopId) throws Exception {
        List<OrderDetail> detailList = Lists.newArrayList();
        //TODO 暂不支持分袋
        for (int i = 0; i < products.size(); i++) {
            JSONArray productAry = products.getJSONArray(i);
            for (int k = 0; k < productAry.size(); k++) {
                JSONObject item = (JSONObject) productAry.get(k);
                OrderDetail orderDetail = new OrderDetail();
                String o2oProductId = item.getString("custom_sku_id");
                DataTable productInfo = null;
                if (StringUtils.isNotEmpty(o2oProductId)) {
                    //查询O2O商品表
                    productInfo = getProductByProductId(adapter, o2oProductId);
                    if (productInfo == null || productInfo.size() == 0) {
                        logger.error("ERP菜品({})不存在", o2oProductId);
                        throw new HitspException("ERP菜品不存在");
                    }
                } else {
                    String upc = item.getString("upc");
                    //查询ERP商品表
                    productInfo = getProductInfoByErpId(adapter, upc, shopId);
                    if (productInfo == null || productInfo.size() == 0) {
                        logger.error("ERP菜品({})不存在", upc);
                        throw new HitspException("ERP菜品不存在");
                    }
                }

                orderDetail.setOrderId(ebOrderId);
                orderDetail.setCode(productInfo.get(0, "id"));
                orderDetail.setName(productInfo.get(0, "name"));
                orderDetail.setCategoryId(productInfo.get(0, "category_id"));
                orderDetail.setPrice(item.getInteger("product_price") == 0 ? new BigDecimal(0) : new BigDecimal(item.getInteger("product_price")).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP));
                orderDetail.setQuantity(new BigDecimal(item.getString("product_amount")));
                orderDetail.setTotal(item.getInteger("total_fee") == 0 ? new BigDecimal(0) : new BigDecimal(item.getInteger("total_fee")).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP));
                orderDetail.setUnit(productInfo.get(0, "spec"));
                orderDetail.setRemark(null);
                detailList.add(orderDetail);
            }
        }
        return detailList;
    }

    private JSONObject getEbOrderAndSave(String dbName, String orderId, String o2oOrderId, String source) throws Exception {
        ISqlAdapter adapter = dbAdapterManager.getAdapter("tenantdb");
        if (adapter == null) {
            logger.error("数据库连接不存在");
            return null;
        }
        try {
            adapter.useDbName(dbName);
        } catch (SQLException e) {
            logger.error(dbName + "数据库不存在", e);
            return null;
        }
        JSONObject orderJson = null;
        try {
            //获取饿百订单详情
            IPlatform platform = PlatFormFactory.getPlatform("17");
            orderJson = (JSONObject) platform.getOrderInfo(orderId, dbName, source);
            logger.info("拉取到饿百订单：{}", orderJson);
            //保存饿百订单
            saveEbOrder(adapter, o2oOrderId, orderJson);
            adapter.commit();
        } catch (Exception ex) {
            adapter.rollbackQuiet();
            throw ex;
        } finally {
            adapter.closeQuiet();
        }
        return orderJson;
    }

    private void saveEbOrder(ISqlAdapter adapter, String o2oOrderId, JSONObject ebOrderJson) throws Exception {
        JSONObject data = ebOrderJson.getJSONObject("data");
        JSONObject reOrder = data.getJSONObject("order");
        SqlCommand cmd = new SqlCommand(EbSqlConst.SQL_INSERT_EB);
        cmd.setParameter("o2oOrderId", o2oOrderId);
        cmd.setParameter("ebOrderId", reOrder.getString("order_id"));
        cmd.setParameter("elemeOrderId", reOrder.getString("eleme_order_id"));
        cmd.setParameter("orderFrom", reOrder.getString("order_from"));
        cmd.setParameter("sendImmediately", reOrder.getString("send_immediately"));
        cmd.setParameter("orderIndex", reOrder.getString("order_index"));
        cmd.setParameter("isColdBoxOrder", reOrder.getString("is_cold_box_order"));
        cmd.setParameter("isPrivate", reOrder.getString("is_private"));
        cmd.setParameter("downFlag", reOrder.getString("down_flag"));
        cmd.setParameter("status", reOrder.getString("status"));
        cmd.setParameter("orderFlag", reOrder.getString("order_flag"));
        cmd.setParameter("expectTimeMode", reOrder.getString("expect_time_mode"));
        cmd.setParameter("sendTime", reOrder.getString("send_time"));
        cmd.setParameter("pickupTime", reOrder.getString("pickup_time"));
        cmd.setParameter("atshopTime", reOrder.getString("atshop_time"));
        cmd.setParameter("deliveryTime", reOrder.getString("delivery_time"));
        cmd.setParameter("deliveryPhone", reOrder.getString("delivery_phone"));
        cmd.setParameter("finishedTime", reOrder.getString("finished_time"));
        cmd.setParameter("confirmTime", reOrder.getString("confirm_time"));
        cmd.setParameter("cancelTime", reOrder.getString("cancel_time"));
        cmd.setParameter("sendFee", reOrder.getString("send_fee"));
        cmd.setParameter("packageFee", reOrder.getString("package_fee"));
        cmd.setParameter("discountFee", reOrder.getString("discount_fee"));
        cmd.setParameter("shopFee", reOrder.getString("shop_fee"));
        cmd.setParameter("totalFee", reOrder.getString("total_fee"));
        cmd.setParameter("userFee", reOrder.getString("user_fee"));
        cmd.setParameter("coldBoxFee", reOrder.getString("cold_box_fee"));
        cmd.setParameter("payType", reOrder.getString("pay_type"));
        cmd.setParameter("payStatus", reOrder.getString("pay_status"));
        cmd.setParameter("needInvoice", reOrder.getString("need_invoice"));
        cmd.setParameter("invoiceTitle", reOrder.getString("invoice_title"));
        cmd.setParameter("taxerId", reOrder.getString("taxer_id"));
        cmd.setParameter("remark", reOrder.getString("remark"));
        cmd.setParameter("deliveryParty", reOrder.getString("delivery_party"));
        cmd.setParameter("createTime", reOrder.getString("create_time"));
        cmd.setParameter("mealNum", reOrder.getString("meal_num"));
        cmd.setParameter("responsibleParty", reOrder.getString("responsible_party"));
        cmd.setParameter("commission", reOrder.getString("commission"));
        cmd.setParameter("ext", reOrder.get("ext").toString());
        cmd.setParameter("user", data.get("user").toString());
        cmd.setParameter("shop", data.get("shop").toString());
        cmd.setParameter("products", data.getJSONArray("products").toJSONString());
        cmd.setParameter("discount", data.getJSONArray("discount").toJSONString());
        adapter.update(cmd);
    }

    private void saveJdOrderToO2oOrder(String dbName, String orderId, String o2oOrderId, JSONObject orderJson, JSONObject orderTotalSplitJson) throws Exception {
        ISqlAdapter adapter = dbAdapterManager.getAdapter("tenantdb");
        if (adapter == null) {
            logger.error("数据库连接不存在");
            return;
        }
        try {
            adapter.useDbName(dbName);
        } catch (SQLException e) {
            logger.error(dbName + "数据库不存在", e);
            return;
        }
        try {
            //保存O2O订单
            saveJdOrderToO2oOrder(adapter, o2oOrderId, orderJson, orderTotalSplitJson);
            adapter.commit();
        } catch (Exception ex) {
            adapter.rollbackQuiet();
            throw ex;
        } finally {
            adapter.closeQuiet();
        }
    }

    private void saveJdOrderToO2oOrder(ISqlAdapter adapter, String o2oOrderId, JSONObject jdOrderJson, JSONObject orderTotalSplitJson) throws Exception {
        //转换为O2O order 对象
        Order order = jdCoverToO2oOrder(adapter, o2oOrderId, jdOrderJson, orderTotalSplitJson);
        //保存O2O order 主表
        saveO2oOrderMaster(adapter, order);
        //保存O2O order 明细表
        saveO2oOrderDetail(adapter, order);
    }

    private void saveO2oOrderDetail(ISqlAdapter adapter, Order order) throws Exception {
        logger.info("保存订单明细开始");
        SqlCommand sqlDetailCommand = new SqlCommand(OrderSqlConst.SQL_INSERT_DETAIL);
        List<OrderDetail> orderDetailList = order.getDetail();
        if (orderDetailList == null || orderDetailList.size() == 0) {
            return;
        }

        for (int i = 0; i < orderDetailList.size(); i++) {
            if (i != 0) {
                sqlDetailCommand.next();
            }
            OrderDetail orderDetail = orderDetailList.get(i);
            Field[] fields = orderDetail.getClass().getDeclaredFields();
            for (int j = 0; j < fields.length; j++) {
                String name = fields[j].getName();
                if (StringUtils.equals("orderId", name)) {
                    sqlDetailCommand.setParameter(name, order.getId());
                } else {
                    if (!StringUtils.equals("id", name) && !StringUtils.equals("gmtCreate", name)
                            && !StringUtils.equals("gmtModify", name)) {
                        fields[j].setAccessible(true);
                        sqlDetailCommand.setParameter(name, fields[j].get(orderDetail));
                    }
                }

            }
        }
        adapter.update(sqlDetailCommand);
    }

    private void saveO2oOrderMaster(ISqlAdapter adapter, Order order) throws Exception {
        SqlCommand cmd = new SqlCommand(OrderSqlConst.SQL_INSERT_MASTER_04);
        cmd.setParameter("id", order.getId());
        cmd.setParameter("gmtCreate", CommonUtil.getStr2Date(order.getGmtCreate()));
        cmd.setParameter("gmtModify", CommonUtil.getStr2Date(order.getGmtModify()));
        cmd.setParameter("deliverTime", order.getDeliverTime() == null ? order.getDeliverTime() : CommonUtil.getStr2Date(order.getDeliverTime()));
        cmd.setParameter("orderId", order.getOrderId());
        cmd.setParameter("orderFrom", order.getOrderFrom());
        cmd.setParameter("totalPrice", order.getTotalPrice());
        cmd.setParameter("originalPrice", order.getOriginalPrice());
        cmd.setParameter("status", order.getStatus());
        cmd.setParameter("shopId", order.getShopId());
        cmd.setParameter("shopName", order.getShopName());
        cmd.setParameter("shopAddress", order.getShopAddress());
        cmd.setParameter("shopPhone", order.getShopPhone());
        cmd.setParameter("recipientName", order.getRecipientName());
        cmd.setParameter("recipientAddress", order.getRecipientAddress());
        cmd.setParameter("recipientPhone", order.getRecipientPhone());
        cmd.setParameter("recipientLongitude", order.getRecipientLongitude());
        cmd.setParameter("recipientLatitude", order.getRecipientLatitude());
        cmd.setParameter("shippingType", order.getShippingType());
        cmd.setParameter("shippingFee", order.getShippingFee());
        cmd.setParameter("shipperName", order.getShipperName());
        cmd.setParameter("shipperPhone", order.getShipperPhone());
        cmd.setParameter("hasInvoiced", order.getHasInvoiced());
        //TODO 测试布尔值
        cmd.setParameter("invoiceTitle", order.getInvoiceTitle());
        cmd.setParameter("packageFee", order.getPackageFee());
        cmd.setParameter("payType", order.getPayType());
        cmd.setParameter("caution", order.getCaution());
        cmd.setParameter("remark", order.getRemark());
        cmd.setParameter("shopPart", order.getShopPart());
        cmd.setParameter("serviceFee", order.getServiceFee());
        cmd.setParameter("shopIncome", order.getShopIncome());
        adapter.update(cmd);
    }

    private Order jdCoverToO2oOrder(ISqlAdapter adapter, String o2oOrderId, JSONObject jdOrderJson, JSONObject orderTotalSplitJson) throws Exception {
        logger.info("京东订单转换为O2O订单开始，京东order信息：{}", jdOrderJson);
        JSONObject orderInvoice = jdOrderJson.getJSONObject("orderInvoice");
        Order order = new Order();
        order.setDeliverTime(CommonUtil.getDateTOStr(jdOrderJson.getString("orderPreEndDeliveryTime")));
        order.setOrderId(jdOrderJson.getString("orderId"));
        order.setId(Converter.toLong(o2oOrderId));
        Date createTime = CommonUtil.getDateTOStr(jdOrderJson.getString("orderPurchaseTime"));
        order.setGmtModify(createTime);
        order.setGmtCreate(createTime);
        order.setOrderFrom("16");
        Integer totalPrice = jdOrderJson.getInteger("orderBuyerPayableMoney");
        order.setTotalPrice(new BigDecimal(totalPrice).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP));
        Integer originalPrice = totalPrice + jdOrderJson.getInteger("orderDiscountMoney") + jdOrderJson.getInteger("platformPointsDeductionMoney");
        order.setOriginalPrice(new BigDecimal(originalPrice).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP));
        order.setStatus("0");
        order.setShopId(jdOrderJson.getString("deliveryStationNoIsv"));
        order.setShopName(jdOrderJson.getString("deliveryStationName"));
        order.setShopAddress("");//京东订单中无门店地址，暂置为空
        order.setShopPhone(null);
        order.setRecipientName(jdOrderJson.getString("buyerFullName"));
        order.setRecipientAddress(jdOrderJson.getString("buyerFullAddress"));
        order.setRecipientPhone(StringUtils.isEmpty(jdOrderJson.getString("buyerMobile")) ? jdOrderJson.getString("buyerTelephone") : jdOrderJson.getString("buyerMobile"));
        order.setRecipientLongitude(Converter.toBigDecimal(jdOrderJson.getDouble("buyerLng")));
        order.setRecipientLatitude(Converter.toBigDecimal(jdOrderJson.getDouble("buyerLat")));
        String shipType = "00";
        //获取京东费率（扣点）
        BigDecimal rate = getJdRate(adapter, jdOrderJson.getString("deliveryStationNoIsv"));
        // 运费佣金
        BigDecimal freightCommission = new BigDecimal(0);
        if (!StringUtils.equals(jdOrderJson.getString("deliveryCarrierNo"), "2938")) {
            switch (jdOrderJson.getString("deliveryCarrierNo")) {
                case "9966":
                    shipType = "15";//京东众包
                    break;
                case "1130":
                    shipType = "16";//达达同城送
                    break;
                case "9999":
                    shipType = "17";//到店自提
                    break;
                default:
                    break;
            }
        } else {
            freightCommission = new BigDecimal(jdOrderJson.getInteger("orderFreightMoney")).multiply(rate);
        }
        order.setShippingType(shipType);

        order.setShippingFee(jdOrderJson.getInteger("orderFreightMoney") == 0 ? new BigDecimal(0) : new BigDecimal(jdOrderJson.getInteger("orderFreightMoney")).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP));
        order.setShipperName(null);
        order.setShipperPhone(null);
        order.setHasInvoiced(orderInvoice == null ? false : true);
        order.setInvoiceTitle(orderInvoice == null ? "" : orderInvoice.getString("invoiceTitle"));
        order.setPackageFee(jdOrderJson.getInteger("packagingMoney") == 0 ? new BigDecimal(0) : new BigDecimal(jdOrderJson.getInteger("packagingMoney")).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP));
        String payType = null;
        if (jdOrderJson.getInteger("orderPayType") == 1) {
            payType = "1";
        } else if (jdOrderJson.getInteger("orderPayType") == 4) {
            payType = "2";
        }
        order.setPayType(payType);
        order.setCaution(jdOrderJson.getString("orderBuyerRemark"));
        order.setRemark("");
        Integer shop_rate = 0;
        JSONArray discount = jdOrderJson.getJSONArray("discount");
        if (discount != null) {
            for (int i = 0; i < discount.size(); i++) {
                JSONObject oneDis = discount.getJSONObject(i);
                shop_rate = oneDis.getInteger("venderPayMoney") + shop_rate;
            }
        }
        order.setShopPart(shop_rate == 0 ? new BigDecimal(0) : new BigDecimal(shop_rate).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP));
        //店铺实际收入
        order.setShopIncome(new BigDecimal(jdOrderJson.getInteger("orderBuyerPayableMoney") - jdOrderJson.getInteger("orderFreightMoney")).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP));
        //计算货款佣金
        BigDecimal productCommission = countProductCommission(orderTotalSplitJson, rate);
        //计算餐盒费佣金
        JSONArray products = jdOrderJson.getJSONArray("product");
        BigDecimal canteenTotal = countCanteenTotal(products, rate);
        //保底佣金
        BigDecimal jdMgCommission = getJdMgCommission(adapter, jdOrderJson.getString("deliveryStationNoIsv"));
        //每单保底佣金补差
        BigDecimal mgDiff = jdMgCommission.subtract((productCommission.add(canteenTotal).add(freightCommission)));
        //京东平台服务费
        BigDecimal jdCoverCharge = productCommission.add(canteenTotal).add(freightCommission);
        if (mgDiff.compareTo(BigDecimal.ZERO) == 1) {
            jdCoverCharge = jdMgCommission;
        }
        order.setServiceFee(jdCoverCharge);
        List<OrderDetail> detailList = convertOrderJdToOrderDetailList(adapter, o2oOrderId, products, jdOrderJson.getString("deliveryStationNoIsv"));
        order.setDetail(detailList);
        return order;
    }

    public BigDecimal countCanteenTotal(JSONArray productList, BigDecimal rate) {
        BigDecimal canteenSum = new BigDecimal(0);
        for (int i = 0; i < productList.size(); i++) {
            JSONObject product = productList.getJSONObject(i);
            canteenSum = canteenSum.add(new BigDecimal(product.getInteger("canteenMoney")));
        }
        BigDecimal canteenTotal = canteenSum.multiply(rate);
        return canteenTotal;
    }

    public BigDecimal countProductCommission(JSONObject orderTotalSplitJson, BigDecimal rate) {
        JSONArray oassBussinessSkusNew = orderTotalSplitJson.getJSONArray("data");
        //用户实际支付SKU金额
        BigDecimal merPayTotal = new BigDecimal(0);
        //到家平台承担优惠补贴金额
        BigDecimal platformSdTotal = new BigDecimal(0);
        //订单级别优惠补贴分摊金额
        BigDecimal shareTotal = new BigDecimal(0);
        for (int i = 0; i < oassBussinessSkusNew.size(); i++) {
            JSONObject oneSkuItem = oassBussinessSkusNew.getJSONObject(i);
            merPayTotal = merPayTotal.add(new BigDecimal(oneSkuItem.getInteger("skuPayMoney")));
            platformSdTotal = platformSdTotal.add(new BigDecimal(oneSkuItem.getInteger("costMoney")));
            JSONArray discountArray = oneSkuItem.getJSONArray("discountlist");
            for (int j = 0; j < discountArray.size(); j++) {
                JSONObject orderBussiDiscountMoney = discountArray.getJSONObject(j);
                shareTotal = shareTotal.add(new BigDecimal(orderBussiDiscountMoney.getInteger("costMoney")));
            }
        }
        BigDecimal result = (merPayTotal.add(platformSdTotal).add(shareTotal)).multiply(rate);
        return result;
    }

    public BigDecimal getJdRate(ISqlAdapter adapter, String shopId) throws SQLException {
        SqlCommand cmd = new SqlCommand(JingdongSqlConst.SQL_SELECT_JD_CONFIG_BY_SHOP_ID);
        cmd.setParameter("shopId", shopId);
        DataTable dt = adapter.query(cmd);
        BigDecimal rate = new BigDecimal(0.12);
        if (dt != null && dt.size() > 0) {
            String jdRate = dt.get(0, "jd_rate");
            rate = new BigDecimal(jdRate).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP);
        }
        return rate;
    }

    public BigDecimal getJdMgCommission(ISqlAdapter adapter, String shopId) throws SQLException {
        SqlCommand cmd = new SqlCommand(JingdongSqlConst.SQL_SELECT_JD_CONFIG_BY_SHOP_ID);
        cmd.setParameter("shopId", shopId);
        DataTable dt = adapter.query(cmd);
        BigDecimal jdMgCommission = new BigDecimal(0.12);
        if (dt != null && dt.size() > 0) {
            String jdCommission = dt.get(0, "jd_mg_commission");
            jdMgCommission = new BigDecimal(jdCommission);
        }
        return jdMgCommission;
    }

    private List<OrderDetail> convertOrderJdToOrderDetailList(ISqlAdapter adapter, String o2oOrderId, JSONArray products, String shopId) throws Exception {
        List<OrderDetail> detailList = Lists.newArrayList();
        for (int i = 0; i < products.size(); i++) {
            JSONObject item = products.getJSONObject(i);
            OrderDetail orderDetail = new OrderDetail();
            String o2oProductId = item.getString("skuIdIsv");
            DataTable productInfo = null;
            if (StringUtils.isNotEmpty(o2oProductId)) {
                //查询O2O商品表
                productInfo = getProductByPublicProductIdAndShopId(adapter, o2oProductId, shopId);
                if (productInfo == null || productInfo.size() == 0) {
                    logger.error("ERP菜品({})不存在", o2oProductId);
                    throw new HitspException("ERP菜品不存在");
                }
            }
            orderDetail.setOrderId(o2oOrderId);
            orderDetail.setCode(productInfo.get(0, "id"));
            orderDetail.setName(productInfo.get(0, "name"));
            orderDetail.setCategoryId(productInfo.get(0, "category_id"));
            orderDetail.setPrice(item.getInteger("skuJdPrice") == 0 ? new BigDecimal(0) : new BigDecimal(item.getInteger("skuJdPrice")).divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP));
            orderDetail.setQuantity(new BigDecimal(item.getInteger("skuCount")));
            orderDetail.setTotal(orderDetail.getPrice().multiply(orderDetail.getQuantity()).setScale(2, BigDecimal.ROUND_HALF_UP));
            orderDetail.setUnit(productInfo.get(0, "spec"));
            orderDetail.setRemark(null);
            detailList.add(orderDetail);
        }
        return detailList;
    }

    private DataTable getProductByProductId(ISqlAdapter adapter, String productId) {
        DataTable dt = null;
        try {
            SqlCommand cmd = new SqlCommand(JingdongSqlConst.SQL_SELECT_PRODUCT_BY_PRODUCT_ID);
            cmd.setParameter("productId", productId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("根据菜品Id获菜品信息时数据库抛出异常", e);
            return null;
        }

        if (dt == null || dt.size() == 0) {
            return null;
        }

        return dt;
    }

    private DataTable getProductByPublicProductIdAndShopId(ISqlAdapter adapter, String publicProductId, String shopId) {
        DataTable dt = null;
        try {
            SqlCommand cmd = new SqlCommand(JingdongSqlConst.SQL_SELECT_PRODUCT_BY_PUBLIC_PRODUCT_ID_AND_SHOP_ID);
            cmd.setParameter("publicProductId", publicProductId);
            cmd.setParameter("shopId", shopId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("根据菜品Id获菜品信息时数据库抛出异常", e);
            return null;
        }

        if (dt == null || dt.size() == 0) {
            return null;
        }

        return dt;
    }

    private JSONObject getJdOrderAndSave(String dbName, String orderId, String o2oOrderId, String appKey) throws Exception {
        ISqlAdapter adapter = dbAdapterManager.getAdapter("tenantdb");
        if (adapter == null) {
            logger.error("数据库连接不存在");
            return null;
        }
        try {
            adapter.useDbName(dbName);
        } catch (SQLException e) {
            logger.error(dbName + "数据库不存在", e);
            return null;
        }
        JSONObject orderJson = null;
        try {
            //获取京东订单详情
            IPlatform platform = PlatFormFactory.getPlatform("16");
            orderJson = (JSONObject) platform.getOrderInfo(orderId, dbName, appKey);
            logger.info("拉取到京东订单：{}", orderJson);
            //保存京东订单
            saveJdOrder(adapter, o2oOrderId, orderJson);
            adapter.commit();
        } catch (Exception ex) {
            adapter.rollbackQuiet();
            throw ex;
        } finally {
            adapter.closeQuiet();
        }
        return orderJson;
    }

    private JSONObject getJdOrderTotalSplit(String dbName, String orderId, String appKey) throws Exception {
        JSONObject orderJson = null;
        //获取京东订单详情
        IPlatform platform = PlatFormFactory.getPlatform("16");
        JingDongImpl jingDong = new JingDongImpl();
        orderJson = (JSONObject) jingDong.getOrderTotalSplit(orderId, dbName, appKey);
        return orderJson;
    }

    private void saveJdOrder(ISqlAdapter adapter, String o2oOrderId, JSONObject jdOrderJson) throws Exception {
        SqlCommand cmd = new SqlCommand(JingdongSqlConst.SQL_INSERT_JD_ORDER);
        cmd.setParameter("o2oOrderId", o2oOrderId);
        cmd.setParameter("jdOrderId", jdOrderJson.getLong("orderId"));
        cmd.setParameter("srcInnerType", jdOrderJson.getInteger("srcInnerType"));
        cmd.setParameter("orderType", jdOrderJson.getInteger("orderType"));
        cmd.setParameter("orderStatus", jdOrderJson.getInteger("orderStatus"));
        cmd.setParameter("orderStatusTime", jdOrderJson.getString("orderStatusTime"));
        cmd.setParameter("orderStartTime", jdOrderJson.getString("orderStartTime"));
        cmd.setParameter("orderPurchaseTime", jdOrderJson.getString("orderPurchaseTime"));
        cmd.setParameter("orderAgingType", jdOrderJson.getInteger("orderAgingType"));
        cmd.setParameter("orderPreStartDeliveryTime", jdOrderJson.getString("orderPreStartDeliveryTime"));
        cmd.setParameter("orderPreEndDeliveryTime", jdOrderJson.getString("orderPreEndDeliveryTime"));
        cmd.setParameter("orderCancelTime", jdOrderJson.getString("orderCancelTime"));
        cmd.setParameter("orderCancelRemark", jdOrderJson.getString("orderCancelRemark"));
        cmd.setParameter("orgCode", jdOrderJson.getString("orgCode"));
        cmd.setParameter("buyerFullName", jdOrderJson.getString("buyerFullName"));
        cmd.setParameter("buyerFullAddress", jdOrderJson.getString("buyerFullAddress"));
        cmd.setParameter("buyerTelephone", jdOrderJson.getString("buyerTelephone"));
        cmd.setParameter("buyerMobile", jdOrderJson.getString("buyerMobile"));
        cmd.setParameter("lastFourDigitsOfBuyerMobile", jdOrderJson.getString("lastFourDigitsOfBuyerMobile"));
        cmd.setParameter("deliveryStationNo", jdOrderJson.getString("deliveryStationNo"));
        cmd.setParameter("deliveryStationNoIsv", jdOrderJson.getString("deliveryStationNoIsv"));
        cmd.setParameter("deliveryStationName", jdOrderJson.getString("deliveryStationName"));
        cmd.setParameter("deliveryCarrierNo", jdOrderJson.getString("deliveryCarrierNo"));
        cmd.setParameter("deliveryCarrierName", jdOrderJson.getString("deliveryCarrierName"));
        cmd.setParameter("deliveryBillNo", jdOrderJson.getString("deliveryBillNo"));
        cmd.setParameter("deliveryPackageWeight", jdOrderJson.getDouble("deliveryPackageWeight"));
        cmd.setParameter("deliveryConfirmTime", jdOrderJson.getString("deliveryConfirmTime"));
        cmd.setParameter("orderPayType", jdOrderJson.getInteger("orderPayType"));
        cmd.setParameter("payChannel", jdOrderJson.getInteger("payChannel"));
        cmd.setParameter("orderTotalMoney", jdOrderJson.getLong("orderTotalMoney"));
        cmd.setParameter("orderDiscountMoney", jdOrderJson.getLong("orderDiscountMoney"));
        cmd.setParameter("orderFreightMoney", jdOrderJson.getLong("orderFreightMoney"));
        cmd.setParameter("localDeliveryMoney", jdOrderJson.getInteger("localDeliveryMoney"));
        cmd.setParameter("merchantPaymentDistanceFreightMoney", jdOrderJson.getLong("merchantPaymentDistanceFreightMoney"));
        cmd.setParameter("orderReceivableFreight", jdOrderJson.getLong("orderReceivableFreight"));
        cmd.setParameter("platformPointsDeductionMoney", jdOrderJson.getLong("platformPointsDeductionMoney"));
        cmd.setParameter("orderBuyerPayableMoney", jdOrderJson.getLong("orderBuyerPayableMoney"));
        cmd.setParameter("packagingMoney", jdOrderJson.getLong("packagingMoney"));
        cmd.setParameter("tips", jdOrderJson.getLong("tips"));
        cmd.setParameter("adjustIsExists", jdOrderJson.getBoolean("adjustIsExists"));
        cmd.setParameter("adjustId", jdOrderJson.getLong("adjustId"));
        cmd.setParameter("isGroupon", jdOrderJson.getBoolean("isGroupon"));
        cmd.setParameter("buyerCoordType", jdOrderJson.getInteger("buyerCoordType"));
        cmd.setParameter("buyerLng", jdOrderJson.getDouble("buyerLng"));
        cmd.setParameter("buyerLat", jdOrderJson.getDouble("buyerLat"));
        cmd.setParameter("buyerCity", jdOrderJson.getString("buyerCity"));
        cmd.setParameter("buyerCityName", jdOrderJson.getString("buyerCityName"));
        cmd.setParameter("buyerCountry", jdOrderJson.getString("buyerCountry"));
        cmd.setParameter("buyerCountryName", jdOrderJson.getString("buyerCountryName"));
        cmd.setParameter("orderBuyerRemark", jdOrderJson.getString("orderBuyerRemark"));
        cmd.setParameter("businessTag", jdOrderJson.getString("businessTag"));
        cmd.setParameter("equipmentId", jdOrderJson.getString("equipmentId"));
        cmd.setParameter("buyerPoi", jdOrderJson.getString("buyerPoi"));
        cmd.setParameter("ordererName", jdOrderJson.getString("ordererName"));
        cmd.setParameter("ordererMobile", jdOrderJson.getString("ordererMobile"));
        cmd.setParameter("orderNum", jdOrderJson.getInteger("orderNum"));
        cmd.setParameter("userTip", jdOrderJson.getLong("userTip"));
        cmd.setParameter("middleNumBindingTime", jdOrderJson.getString("middleNumBindingTime"));
        cmd.setParameter("deliverInputTime", jdOrderJson.getString("deliverInputTime"));
        cmd.setParameter("businessType", jdOrderJson.getInteger("businessType"));
        cmd.setParameter("venderVipCardId", jdOrderJson.getString("venderVipCardId"));
        cmd.setParameter("orderInvoice", jdOrderJson.getJSONObject("orderInvoice") == null ? "" : jdOrderJson.getJSONObject("orderInvoice").toJSONString());
        cmd.setParameter("product", jdOrderJson.getJSONArray("product").toJSONString());
        cmd.setParameter("discount", jdOrderJson.getJSONArray("discount") == null ? "" : jdOrderJson.getJSONArray("discount").toJSONString());
        cmd.setParameter("prescriptionDTO", jdOrderJson.getJSONObject("prescriptionDTO") == null ? "" : jdOrderJson.getJSONObject("prescriptionDTO").toJSONString());
        adapter.update(cmd);
    }
}
