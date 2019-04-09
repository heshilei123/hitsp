package com.hisense.hitsp.third.meituan.service;

/**
 * 美团业务SQL语句
 *
 * @author yanglei
 *         date: 2017-04-20.
 */
public class MeituanSqlConst {

    public static final String SQL_INSERT_MT = "" +
            "INSERT INTO order_mt(id, orderId, orderIdView, caution," +
            " cityId, ctime, utime, daySeq, deliveryTime, detail, ePoiId, extras," +
            " hasInvoiced, invoiceTitle, isFavorites, isPoiFirstOrder, isThirdShipping," +
            " latitude, longitude, logisticsCode, originalPrice, payType, poiAddress," +
            " poiName, poiPhone, poiReceiveDetail, recipientAddress, recipientName," +
            " recipientPhone, shipperPhone, shippingFee, status, total)" +
            " VALUES (:id, :orderId, :orderIdView, :caution," +
            " :cityId, :ctime, :utime, :daySeq, :deliveryTime, :detail, :ePoiId, :extras," +
            " :hasInvoiced, :invoiceTitle, :isFavorites, :isPoiFirstOrder, :isThirdShipping," +
            " :latitude, :longitude, :logisticsCode, :originalPrice, :payType, :poiAddress," +
            " :poiName, :poiPhone, :poiReceiveDetail, :recipientAddress, :recipientName," +
            " :recipientPhone, :shipperPhone, :shippingFee, :status, :total)";
    public static final String SQL_UPDATE_MASTER_SHIP_STATUS = "" +
            "UPDATE order_master" +
            " SET status=:status, shipping_type=:shippingType," +
            " shipper_name=:shipperName, shipper_phone=:shipperPhone" +
            " WHERE order_id=:orderId";
    public static final String SQL_UPDATE_MT_SHIPPING_STATUS_SEND = "" +
            "UPDATE order_mt" +
            " SET logisticsStatus=:logisticsStatus, shipperPhone=:shipperPhone," +
            " logisticsDispatcherName=:dispatcherName, logisticsDispatcherMobile=:dispatcherMobile," +
            " logisticsSendTime=:time" +
            " WHERE orderId=:orderId";
    public static final String SQL_UPDATE_MT_SHIPPING_STATUS_CONFIRM = "" +
            "UPDATE order_mt" +
            " SET logisticsStatus=:logisticsStatus, shipperPhone=:shipperPhone," +
            " logisticsDispatcherName=:dispatcherName, logisticsDispatcherMobile=:dispatcherMobile," +
            " logisticsConfirmTime=:time" +
            " WHERE orderId=:orderId";
    public static final String SQL_UPDATE_MT_SHIPPING_STATUS_FECTH = "" +
            "UPDATE order_mt" +
            " SET logisticsStatus=:logisticsStatus, shipperPhone=:shipperPhone," +
            " logisticsDispatcherName=:dispatcherName, logisticsDispatcherMobile=:dispatcherMobile," +
            " logisticsFetchTime=:time" +
            " WHERE orderId=:orderId";
    public static final String SQL_UPDATE_MT_SHIPPING_STATUS_COMPLETED = "" +
            "UPDATE order_mt" +
            " SET logisticsStatus=:logisticsStatus, shipperPhone=:shipperPhone," +
            " logisticsDispatcherName=:dispatcherName, logisticsDispatcherMobile=:dispatcherMobile," +
            " logisticsCompletedTime=:time" +
            " WHERE orderId=:orderId";
    public static final String SQL_UPDATE_MT_SHIPPING_STATUS_CANCEL = "" +
            "UPDATE order_mt" +
            " SET logisticsStatus=:logisticsStatus, shipperPhone=:shipperPhone," +
            " logisticsDispatcherName=:dispatcherName, logisticsDispatcherMobile=:dispatcherMobile," +
            " orderCancelTime=:time" +
            " WHERE orderId=:orderId";
    public static final String SQL_UPDATE_ACCOUNT = "" +
            "UPDATE account" +
            " SET platform=:platform, token=:token" +
            " WHERE id=(SELECT id FROM account_shop WHERE shop_id=:shopId)";
    public static final String SQL_UPDATE_ACCOUNT_SHOP = "" +
            "UPDATE account_shop" +
            " SET token=:token" +
            " WHERE shop_id=:shopId";
    public static final String SQL_INSERT_ACCOUNT = "" +
            "INSERT INTO account(id, platform)" +
            " VALUES (:id, :platform)";
    public static final String SQL_INSERT_ACCOUNT_SHOP = "" +
            "INSERT INTO account_shop(id, shop_id, token)" +
            " VALUES (:id, :shopId, :token)";
    public static final String SQL_SELECT_ACCOUNT_SHOP = "" +
            "SELECT id, shop_id, gmt_create, gmt_modify, token" +
            " FROM account_shop" +
            " WHERE shop_id=:shopId";
    public static final String SQL_SELECT_PRODUCT_CATEGERY_MT = "" +
            "SELECT ePoiId, category_id, name, sequence, remark" +
            " FROM product_category_meituan" +
            " WHERE category_id=:categoryId";
    public static final String SQL_SELECT_ORDER_MASTER = "" +
            "SELECT id, shop_id" +
            " FROM order_master" +
            " WHERE order_id=:orderId";
    public static final String SQL_SELECT_ORDER_MASTER_BY_ORDERID = "" +
            "SELECT id" +
            " FROM order_master" +
            " WHERE order_id=:orderId";
    public static final String SQL_SELECT_ORDER_MT_LOGISTICS = "" +
            "Select logisticsCode " +
            " from order_mt" +
            " where orderId = :id";
    public static final String SQL_SELECT_PRODUCT_MEITUAN = "" +
            "SELECT id, ePoiId, dishId, extend, remark" +
            " FROM product_meituan" +
            " WHERE id=:productId";
    public static final String SQL_SELECT_PRODUCT_ELEME = "" +
            "SELECT id,shopId, itemId, specId, extend, remark" +
            " FROM product_eleme" +
            " WHERE id=:productId";
    public static final String SQL_SELECT_SHOP_MT_BY_SHOPID = "" +
            "SELECT id, mt_id, remark" +
            " FROM shop_meituan" +
            " WHERE id=:shopId";
    public static final String SQL_SELECT_SHOP_MT_BY_EPOIID = "" +
            "SELECT id, mt_id, remark" +
            " FROM shop_meituan" +
            " WHERE id=:ePoiId";
    public static final String SQL_SELECT_SHOP_BY_EPOIID = "" +
            "SELECT id, remark" +
            " FROM shop" +
            " WHERE id=:ePoiId";
    public static final String SQL_SELECT_SHOP_BY_ID = "" +
            "SELECT id, gmt_create, gmt_modify, name, is_open, begin_time, end_time, open_time_bitmap," +
            " book_time_bitmap, phone, deliver_amount, is_invoice, settled_platform, remark" +
            " FROM shop" +
            " WHERE id=:shopId";
    public static final String SQL_SELECT_PRODUCT_BY_MTID = "" +
            "SELECT id, shop_id," +
            " name, category_id" +
            " FROM product" +
            " WHERE id=(SELECT id FROM product_meituan WHERE dishId=:productId)";
    public static final String SQL_SELECT_SHOP_ERP_BY_SHOPID = "" +
            "SELECT id, name, erp_id, remark" +
            " FROM shop_erp" +
            " WHERE id=:shopId";
    public static final String SQL_SELECT_PRODUCT_ERP_BY_ID = "" +
            "SELECT id, shop_id, erp_id, name, categoryId, description, spec_id, spec, price, remark,unitWeight" +
            " FROM product_erp" +
            " WHERE id=:productId";
    public static final String SQL_SELECT_PRODUCT_ERP_SPEC_BY_ID = "" +
            "SELECT id, erp_id, specId, specName, specPrice, remark,unitWeight," +
            " (SELECT name FROM product_erp WHERE erp_id=p.erp_id AND shop_id=:erpShopId) as name," +
            " (SELECT categoryId FROM product_erp WHERE erp_id=p.erp_id AND shop_id=:erpShopId) as categoryId" +
            " FROM product_erp_spec p" +
            " WHERE id=:productId";
    public static final String SQL_SELECT_ORDER_MT = "" +
            "SELECT id" +
            " FROM order_mt" +
            " WHERE orderId=:orderId";
}
