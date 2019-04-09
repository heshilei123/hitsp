package com.hisense.hitsp.service;

/**
 * 订单操作的执行SQL
 *
 * @author yanglei
 * @date 2017-03-14.
 */
public class OrderSqlConst {

    public static final String SQL_SELECT_MASTER = "" +
            "SELECT id, gmt_create, gmt_modify, order_id, order_from, deliver_time," +
            " total_price, original_price, status, shop_id, shop_name, shop_address, shop_phone," +
            " recipient_name, recipient_address, recipient_phone, recipient_longitude, " +
            " recipient_latitude, shipping_type, shipping_fee, shipper_name, shipper_phone," +
            " invoice_title, has_invoiced, package_fee, pay_type, caution, remark,shop_part,service_fee," +
            " shop_income" +
            " FROM order_master" +
            " WHERE id=:orderId";
    public static final String SQL_SELECT_MASTER_NOID = "" +
            "SELECT id, order_from, pay_type" +
            " FROM order_master" +
            " WHERE status=:status";
    public static final String SQL_SELECT_MASTER_BY_SHOPID = "" +
            "SELECT id, order_from, pay_type" +
            " FROM order_master" +
            " WHERE status=:status AND shop_id=(SELECT id from shop_erp WHERE erp_id=:shopId)";
    public static final String SQL_SELECT_DETAIL = "" +
            "SELECT id, gmt_create, gmt_modify, order_id, code, name," +
            " category_id, price, quantity, total, unit, remark" +
            " FROM order_detail" +
            " WHERE order_id=:orderId";
    public static final String SQL_INSERT_MASTER_04 = "" +
            "INSERT INTO order_master(id, gmt_create, gmt_modify, order_id, order_from, deliver_time," +
            " total_price, original_price, status, shop_id, shop_name, shop_address, shop_phone," +
            " recipient_name, recipient_address, recipient_phone, recipient_longitude," +
            " recipient_latitude, shipping_type, shipping_fee, shipper_name, shipper_phone," +
            " has_invoiced, invoice_title, package_fee, pay_type, caution, remark,shop_part,service_fee,shop_income)" +
            " VALUES (:id, :gmtCreate, :gmtModify, :orderId, :orderFrom, :deliverTime," +
            " :totalPrice, :originalPrice, :status, :shopId, :shopName, :shopAddress, :shopPhone," +
            " :recipientName, :recipientAddress, :recipientPhone, :recipientLongitude," +
            " :recipientLatitude, :shippingType, :shippingFee, :shipperName, :shipperPhone," +
            " :hasInvoiced, :invoiceTitle, :packageFee, :payType, :caution, :remark,:shopPart,:serviceFee,:shopIncome)";
    public static final String SQL_INSERT_DETAIL = "" +
            "INSERT INTO order_detail(order_id, code, name," +
            " category_id, price, quantity, total, unit, remark)" +
            " VALUES (:orderId, :code, :name," +
            " :categoryId, :price, :quantity, :total, :unit, :remark)";
    public static final String SQL_STATUS_UPDATE_MT = "" +
            "UPDATE order_mt" +
            " SET status=:status" +
            " WHERE orderId=(SELECT order_id from order_master WHERE id=:orderId)";
    public static final String SQL_STATUS_UPDATE_MASTER = "" +
            "UPDATE order_master" +
            " SET status=:status" +
            " WHERE id=:orderId";
    public static final String SQL_SELECT_ORDER_STATUS = "" +
            "SELECT id, status, shipper_name, shipper_phone" +
            " FROM order_master" +
            " WHERE id=:orderId";
    public static final String SQL_STATUS_UPDATE_ELM = "" +
            "UPDATE order_elm" +
            " SET status=:status" +
            " WHERE orderId=(SELECT order_id from order_master WHERE id=:orderId)";
    public static final String SQL_STATUS_UPDATE_MT_THIRD = "" +
            "UPDATE order_mt" +
            " SET status=:status" +
            " WHERE orderId=:orderId";
    public static final String SQL_STATUS_UPDATE_MASTER_THIRD = "" +
            "UPDATE order_master" +
            " SET status=:status" +
            " WHERE order_id=:orderId";
    public static final String SQL_STATUS_BEFORE_REFUND_UPDATE_MASTER = "" +
            "UPDATE order_master" +
            " SET statusbeforerefund=:statusbeforerefund" +
            " WHERE order_id=:orderId";
    public static final String SQL_STATUS_BEFORE_REFUND_SELECT_MASTER = "" +
            "SELECT statusbeforerefund" +
            " FROM order_master" +
            " WHERE order_id=:orderId";
    public static final String SQL_STATUS_UPDATE_ELM_THIRD = "" +
            "UPDATE order_elm" +
            " SET status=:status" +
            " WHERE orderId=:orderId";
    public static final String SQL_STATUS_REFUNDSTATUS_UPDATE_ELM_THIRD = "" +
            "UPDATE order_elm" +
            " SET status=:status,refundStatus=:refundStatus" +
            " WHERE orderId=:orderId";
    public static final String SQL_SELECT_ID_STATUS_BY_ORDERID = "" +
            "SELECT id,status" +
            " FROM order_master" +
            " WHERE order_id=:orderId";
    public static final String SQL_SELECT_REFUND_ORDER_STATUS = "" +
            "SELECT status" +
            " FROM order_master" +
            " WHERE id=:orderId";
    public static final String SQL_SELECT_STATUS_BY_ORDERID = "" +
            "SELECT status" +
            " FROM order_master" +
            " WHERE order_id=:orderId";
    public static final String SQL_SELECT_MT_DAYSN_BY_ORDERID = "" +
            "SELECT daySeq" +
            " FROM order_mt" +
            " WHERE orderId=:orderId";
    public static final String SQL_SELECT_ELEME_DAYSN_BY_ORDERID = "" +
            "SELECT daySn" +
            " FROM order_elm" +
            " WHERE orderId = :orderId";
    public static final String SQL_SELECT_EB_DAYSN_BY_ORDERID = "" +
            "SELECT order_index" +
            " FROM order_eb" +
            " WHERE order_id =:orderId";
    public static final String SQL_SELECT_JD_DAYSN_BY_ORDERID = "" +
            "SELECT orderNum" +
            " FROM order_jd" +
            " WHERE orderId =:orderId";
    public static final String SQL_SELECT_MT_DAYSN_BY_ID = "" +
            "SELECT daySeq" +
            " FROM order_mt" +
            " WHERE orderId=(SELECT order_id FROM order_master WHERE id=:orderId)";
    public static final String SQL_SELECT_ELEME_DAYSN_BY_ID = "" +
            "SELECT daySn" +
            " FROM order_elm" +
            " WHERE orderId =(SELECT order_id FROM order_master WHERE id=:orderId)";
    public static final String SQL_SELECT_EB_DAYSN_BY_ID = "" +
            "SELECT order_index" +
            " FROM order_eb" +
            " WHERE order_id =(SELECT order_id FROM order_master WHERE id=:orderId)";
    public static final String SQL_SELECT_JD_DAYSN_BY_ID = "" +
            "SELECT orderNum" +
            " FROM order_jd" +
            " WHERE orderId =(SELECT order_id FROM order_master WHERE id=:orderId)";
    public static final String SQL_SELECT_MT_ORDERIDVIEW_BY_ORDERID = "" +
            "SELECT orderIdView" +
            " FROM order_mt" +
            " WHERE orderId=:orderId";
    public static final String SQL_INSERT_MT_ORDER_REAL_PHONE = "" +
            "INSERT INTO orderrealphone(id,orderId,orderIdView,daySeq,realPhoneNumber,ePoiId)" +
            " VALUES (:id,:orderId,:orderIdView,:daySeq,:realPhoneNumber,:ePoiId)";
    public static final String SQL_SELECT_MT_ORDER_REAL_PHONE = "" +
            "SELECT id" +
            " FROM orderrealphone" +
            " WHERE orderId=:orderId";
    public static final String SQL_SELECT_ORDER_REAL_PHONE_BY_ORDERID = "" +
            "SELECT orderId,orderIdView,daySeq,realPhoneNumber" +
            " FROM orderrealphone" +
            " WHERE orderId=:orderId";
    public static final String SQL_SELECT_ORDER_REAL_PHONE_BY_TIME = "" +
            "SELECT orderId,orderIdView,daySeq,realPhoneNumber" +
            " FROM orderrealphone" +
            " WHERE createtime>:createTime";
    public static final String SQL_SELECT_ORDER_REAL_PHONE_BY_RANGE_TIME = "" +
            "SELECT orderId,orderIdView,daySeq,realPhoneNumber" +
            " FROM orderrealphone" +
            " WHERE createtime>:startTime AND createtime<:endTime AND ePoiId=:ePoiId";
    public static final String SQL_STATUS_UPDATE_JD_THIRD = "" +
            "UPDATE order_jd" +
            " SET orderStatus=:status" +
            " WHERE id=:o2oOrderId";
    public static final String SQL_STATUS_UPDATE_MASTER_THIRD_BY_O2O_ORDER_ID = "" +
            "UPDATE order_master" +
            " SET status=:status" +
            " WHERE id=:o2oOrderId";
    public static final String SQL_STATUS_BEFORE_REFUND_UPDATE_MASTER_BY_O2O_ORDER_ID = "" +
            "UPDATE order_master" +
            " SET statusbeforerefund=:statusbeforerefund" +
            " WHERE id=:o2oOrderId";
    public static final String SQL_STATUS_UPDATE_EB_THIRD = "" +
            "UPDATE order_eb" +
            " SET status=:status" +
            " WHERE id=:o2oOrderId";
}
