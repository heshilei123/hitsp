package com.hisense.hitsp.report.service;

/**
 * 执行sql
 * @author yanglei.
 */
public class RptSqlConsts {
    public static final String SQL_SELECT_ORDER_TOTAL_BY_USER_AND_STATUS_AND_TIME = "" +
            "SELECT ifnull(sum(total_price), 0) as sum" +
            " FROM order_master" +
            " WHERE shop_id in (SELECT id FROM shop_user WHERE user_id=:userId)" +
            " AND status<>:status AND gmt_create>=:gmtCreate";
    public static final String SQL_SELECT_ORDER_TOTAL_BY_USER_AND_STATUS_AND_TIME_AND_PAYTYPE = "" +
            "SELECT ifnull(sum(total_price), 0) as sum" +
            " FROM order_master" +
            " WHERE shop_id in (SELECT id FROM shop_user WHERE user_id=:userId)" +
            " AND status<>:status AND gmt_create>=:gmtCreate AND pay_type=:payType";
    public static final String SQL_SELECT_ORDER_TOTAL_BY_USER_AND_STATUS_AND_TIME_AND_PAYTYPE_AND_ORDERFROM = "" +
            "SELECT ifnull(sum(total_price), 0) as sum" +
            " FROM order_master" +
            " WHERE shop_id in (SELECT id FROM shop_user WHERE user_id=:userId)" +
            " AND status<>:status AND gmt_create>=:gmtCreate AND pay_type=:payType AND order_from=:orderFrom";
    public static final String SQL_SELECT_ORDER_TOTAL_BY_USER_AND_STATUS_AND_TIME_AND_ORDERFROM = "" +
            "SELECT ifnull(sum(total_price), 0) as sum" +
            " FROM order_master" +
            " WHERE shop_id in (SELECT id FROM shop_user WHERE user_id=:userId)" +
            " AND status<>:status AND gmt_create>=:gmtCreate AND order_from=:orderFrom";
    public static final String SQL_SELECT_ORDER_COUNT_BY_USER_AND_TIME = "" +
            "SELECT count(1) as count" +
            " FROM order_master" +
            " WHERE shop_id in (SELECT id FROM shop_user WHERE user_id=:userId)" +
            " AND gmt_create>=:gmtCreate";
    public static final String SQL_SELECT_ORDER_COUNT_BY_USER_AND_TIME_AND_STATUS = "" +
            "SELECT count(1) as count" +
            " FROM order_master" +
            " WHERE shop_id in (SELECT id FROM shop_user WHERE user_id=:userId)" +
            " AND gmt_create>=:gmtCreate AND status=:status";
    public static final String SQL_SELECT_ORDER_COUNT_BY_USER_AND_TIME_AND_STATUS_AND_ORDERFROM = "" +
            "SELECT count(1) as count" +
            " FROM order_master" +
            " WHERE shop_id in (SELECT id FROM shop_user WHERE user_id=:userId)" +
            " AND gmt_create>=:gmtCreate AND status<>:status AND order_from=:orderFrom";
    public static final String SQL_SELECT_ORDER_COUNT_BY_USER_AND_TIME_AND_ORDERFROM = "" +
            "SELECT count(1) as count" +
            " FROM order_master" +
            " WHERE shop_id in (SELECT id FROM shop_user WHERE user_id=:userId)" +
            " AND gmt_create>=:gmtCreate AND order_from=:orderFrom";
    public static final String SQL_SELECT_ORDER_CUSTERMER_BY_USER_AND_TIME = "" +
            "SELECT distinct recipient_phone as recipientPhone" +
            " FROM order_master" +
            " WHERE shop_id in (SELECT id FROM shop_user WHERE user_id=:userId)" +
            " AND gmt_create>=:gmtCreate";
    public static final String SQL_SELECT_ORDER_CUSTERMER_BY_USER_AND_STARTTIME_AND_ENDTIME = "" +
            "SELECT distinct recipient_phone as recipientPhone" +
            " FROM order_master" +
            " WHERE shop_id in (SELECT id FROM shop_user WHERE user_id=:userId)" +
            " AND gmt_create>=:beginDate AND gmt_create<:endDate";
    public static final String SQL_SELECT_ORDER_PRODUCT_BY_USER = "" +
            "SELECT name as pluname," +
            " ifnull(sum(quantity), 0) as salcount," +
            " ifnull(sum(total),0) as saltotal," +
            " (SELECT shop_name FROM order_master m WHERE m.id=d.order_id) as shopname" +
            " FROM order_detail d" +
            " WHERE gmt_create >= DATE_FORMAT(CURDATE(),'%Y%m%d')" +
            " AND EXISTS(SELECT 1 FROM order_master m WHERE m.id=d.order_id AND m.status<>'5')" +
            " AND order_id in(SELECT id FROM order_master WHERE shop_id in (SELECT id FROM shop_user WHERE user_id=:userId))" +
            " GROUP BY code, pluname, shopname";
    public static final String SQL_SELECT_ORDER_PRODUCT_WITH_SHOPID = "" +
            "SELECT name as pluname," +
            " ifnull(sum(quantity),0) as salcount," +
            " ifnull(sum(total),0) as saltotal," +
            " (SELECT shop_name FROM order_master m WHERE m.id=d.order_id) as shopname" +
            " FROM order_detail d" +
            " WHERE gmt_create >= DATE_FORMAT(CURDATE(),'%Y%m%d')" +
            " AND EXISTS(SELECT 1 FROM order_master m WHERE m.id=d.order_id AND m.status<>'5' AND m.shop_id=:shopId)" +
            " GROUP BY code, pluname, shopname";
    public static final String SQL_SELECT_ORDER_PRODUCT_WITH_ORDEFROM_BY_USER = "" +
            "SELECT name as pluname," +
            " ifnull(sum(quantity),0) as salcount," +
            " ifnull(sum(total),0) as saltotal," +
            " (SELECT shop_name FROM order_master m WHERE m.id=d.order_id) as shopname" +
            " FROM order_detail d" +
            " WHERE gmt_create >= DATE_FORMAT(CURDATE(),'%Y%m%d')" +
            " AND EXISTS(SELECT 1 FROM order_master m WHERE m.id=d.order_id AND m.status<>'5' AND m.order_from=:orderFrom)" +
            " AND order_id in(SELECT id FROM order_master WHERE shop_id in (SELECT id FROM shop_user WHERE user_id=:userId))" +
            " GROUP BY code, pluname, shopname";
    public static final String SQL_SELECT_ORDER_PRODUCT_WITH_ORDEFROM_AND_SHOPID  = "" +
            "SELECT name as pluname," +
            " ifnull(sum(quantity),0) as salcount," +
            " ifnull(sum(total),0) as saltotal," +
            " (SELECT shop_name FROM order_master m WHERE m.id=d.order_id) as shopname" +
            " FROM order_detail d" +
            " WHERE gmt_create >= DATE_FORMAT(CURDATE(),'%Y%m%d')" +
            " AND EXISTS(SELECT 1 FROM order_master m WHERE m.id=d.order_id AND m.status<>'5' AND m.shop_id=:shopId AND m.order_from=:orderFrom)" +
            " GROUP BY code, pluname, shopname";
    public static final String SQL_SELECT_SHOP_BY_USER = "" +
            "SELECT id, gmt_create, gmt_modify, name," +
            " is_open, begin_time, end_time" +
            " phone, deliver_amount, is_invoice, settled_platform, remark" +
            " FROM shop" +
            " WHERE id in (SELECT id FROM shop_user WHERE user_id=:userId)";
}
