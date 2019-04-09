package com.hisense.hitsp.report.service;

/**
 * 报表常量及执行SQL
 * @author yanglei
 */
public class RptOperationConsts {
    public static final String DB_NAME_PREFIX = "hitsp";
//    public static final int CURRENT_DATABASE_COUNT = 2;
    public static final int RPT_DATA_UPDATE_INTERVAL = 12;//小时
    public static final String ORDER_COLLECTION_NAME = "order";
    public static final String SHOP_COLLECTION_NAME = "shop";
    public static final String TENANT_DB_NAME = "dustdb";
    public static final String TENANT_COLLECTION_NAME = "appconfig";
    public static final String SQL_SELECT_ORDER_MASTER_INSERT = "" +
            "SELECT id, gmt_create, gmt_modify, order_id, order_from as orderFrom," +
            " total_price as totalPrice," +
            " status, shop_id as shopId, shop_name as shopname," +
            " recipient_phone as recipientPhone," +
            " pay_type as payType" +
            " FROM order_master" +
            " WHERE gmt_create >= :lastUpdateTime";
    public static final String SQL_SELECT_ORDER_MASTER_UPDATE = "" +
            "SELECT id, gmt_create, gmt_modify, order_id, order_from as orderFrom," +
            " total_price as totalPrice," +
            " status, shop_id as shopId, shop_name as shopname," +
            " recipient_phone as recipientPhone," +
            " pay_type as payType" +
            " FROM order_master" +
            " WHERE gmt_create < :lastUpdateTime" +
            " AND gmt_modify >= :lastUpdateTime";
    public static final String SQL_SELECT_ORDER_DETAIL = "" +
            "SELECT id, gmt_create, gmt_modify, order_id," +
            " code as pluId, name as pluname," +
            " category_id, price, quantity, total, unit, remark" +
            " FROM order_detail" +
            " WHERE order_id=:orderId";
    public static final String SQL_SELECT_SHOP_INSERT = "" +
            "SELECT id, gmt_create, gmt_modify, name as shopname," +
            " is_open, begin_time, end_time" +
            " phone, deliver_amount, is_invoice, settled_platform, remark" +
            " FROM shop" +
            " WHERE gmt_create >= :lastUpdateTime";
    public static final String SQL_SELECT_SHOP_UPDATE = "" +
            "SELECT id, gmt_create, gmt_modify, name as shopname," +
            " is_open, begin_time, end_time" +
            " phone, deliver_amount, is_invoice, settled_platform, remark" +
            " FROM shop" +
            " WHERE gmt_create < :lastUpdateTime" +
            " AND gmt_modify >= :lastUpdateTime";
    public static final String SQL_APP_DB = "SELECT id, gmt_create," +
            " tenant_id as tenantId, app_alias as tenantName, db_name as dbName" +
            " FROM dust_app_config ";
}
