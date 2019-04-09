package com.hisense.hitsp.report.service;

import com.hisense.dustcore.util.ClassBuildUtils;
import com.hisense.dustcore.util.Converter;
import com.hisense.dustdb.DbAdapterManager;
import com.hisense.dustdb.TenantAdapterManager;
import com.hisense.dustdb.sql.DataRow;
import com.hisense.dustdb.sql.DataTable;
import com.hisense.dustdb.sql.ISqlAdapter;
import com.hisense.dustdb.sql.SqlCommand;
import com.hisense.dustdb.tenant.pojo.AppConfig;
import com.hisense.hitsp.common.HitspException;
import com.hisense.hitsp.config.DatabaseYmlConfig;
import com.hisense.hitsp.model.Order;
import com.hisense.hitsp.model.OrderDetail;
import com.hisense.hitsp.model.Shop;
import com.hisense.hitsp.report.config.MongoProperties;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

/**
 * 定时任务：用于更新数据库数据到mongo
 *
 * @author yanglei
 *         date: 2017-08-04.
 */
@Service
public class RptDataUpdateService {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    private List<AppConfig> appConfigs = new ArrayList<>();

    @Autowired
    DbAdapterManager dbAdapterManager;
    @Autowired
    TenantAdapterManager tenantAdapterManager;
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    MongoProperties mongoProperties;
    @Autowired
    DatabaseYmlConfig databaseYmlConfig;

    /**
     * 定时任务，用于定时刷新报表数据
     * 每天凌晨3点更新报表数据
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void updateAllRptData() throws HitspException {
        updateDustdbData();
        updateRptData();
    }

    public void updateRptData() throws HitspException {
        ISqlAdapter adapter = dbAdapterManager.getAdapter("tenantdb");
        if (adapter == null) {
            return;
        }

        String dbName = null;

        for (int i = 1; i <= databaseYmlConfig.getDatabaseCount(); i++) {
            try {
                adapter.useDbName(RptOperationConsts.DB_NAME_PREFIX + i);
            } catch (SQLException e) {
                logger.error("数据库({})不存在", RptOperationConsts.DB_NAME_PREFIX, e);
                adapter.closeQuiet();
                return;
            }

            dbName = RptOperationConsts.DB_NAME_PREFIX + i;
            String tenantId = null;
            for (int j = 0; j < appConfigs.size(); j++) {
                AppConfig appConfig = appConfigs.get(j);
                if (StringUtils.equals(dbName, appConfig.getDbName())) {
                    tenantId = appConfig.getTenantId();
                    break;
                }
            }

            if (StringUtils.isEmpty(tenantId)) {
                logger.error("数据库({})没有对应租户, 请先检查租户信息", dbName);
                adapter.closeQuiet();
                return;
            }

            Date lastUpdateTime = getLastUpdateTime();
            DataTable dataTable = null;
            //获取报表新增订单数据
            try {
                dataTable = dbQuery(adapter, RptOperationConsts.SQL_SELECT_ORDER_MASTER_INSERT, lastUpdateTime);
            } catch (SQLException e) {
                logger.error("报表查询新增订单数据时，数据库操作异常", e);
                adapter.closeQuiet();
                return;
            }

            if (dataTable != null && dataTable.size() > 0) {
                List<Order> orderList = new ArrayList<>();

                List<DataRow> dataRows = dataTable.getRows();
                for (int j = 0; j < dataRows.size(); j++) {
                    Map orderMap = dataRows.get(j).toMap();
                    String orderId;
                    orderId = Converter.toString(orderMap.get("id"));

                    List<OrderDetail> orderDetailList = null;
                    try {
                        orderDetailList = queryOrderDetailList(adapter, orderId);
                    } catch (SQLException e) {
                        logger.error("数据库异常",e);
                        adapter.closeQuiet();
                        return;
                    }

                    //明细订单不保存
                    if (orderDetailList == null) {
                        logger.warn("外卖平台订单(orderId={})没有明细信息", orderId);
                        continue;
                    }

                    Order order = ClassBuildUtils.mapToObject(orderMap, Order.class);
                    order.setDetail(orderDetailList);
                    orderList.add(order);
                }

                batchSaveRptOrderData(tenantId, RptOperationConsts.ORDER_COLLECTION_NAME, orderList);
            }

            //获取报表更新订单数据(仅status & gmt_modify变化)
            List<String> updateOrderFieldNames = new ArrayList<>();
            updateOrderFieldNames.add("status");

            try {
                dataTable = dbQuery(adapter, RptOperationConsts.SQL_SELECT_ORDER_MASTER_UPDATE, lastUpdateTime);
            } catch (SQLException e) {
                logger.error("报表查询更新订单数据时，数据库操作异常", e);
                adapter.closeQuiet();
                return;
            }

            if (dataTable != null && dataTable.size() > 0) {
                List<Order> updateOrderList = new ArrayList<>();
                dataTable.getRows().forEach(dataRow -> {
                    Order order = ClassBuildUtils.mapToObject(dataRow.toMap(), Order.class);
                    updateOrderList.add(order);
                });

                batchUpdateRptData(RptOperationConsts.ORDER_COLLECTION_NAME, updateOrderList, updateOrderFieldNames);
            }

            //获取报表新增门店数据
            try {
                dataTable = dbQuery(adapter, RptOperationConsts.SQL_SELECT_SHOP_INSERT, lastUpdateTime);
            } catch (SQLException e) {
                logger.error("报表查询新增门店数据时，数据库操作异常", e);
                adapter.closeQuiet();
                return;
            }

            if (dataTable != null && dataTable.size() > 0) {
                List<Shop> shopList = new ArrayList<>();
                for (int l = 0; l < dataTable.size(); l++) {
                    Map<String, Object> map = dataTable.getRows().get(l).toMap();
                    Shop shop = ClassBuildUtils.mapToObject(map, Shop.class);
                    shopList.add(shop);
                }

                batchSaveRptOrderData(tenantId, RptOperationConsts.SHOP_COLLECTION_NAME, shopList);
            }

            //获取报表更新门店数据
            List<String> updateShopFieldNames = new ArrayList<>();
            updateShopFieldNames.add("isOpen");
            updateShopFieldNames.add("beginTime");
            updateShopFieldNames.add("endTime");
            updateShopFieldNames.add("settledPlatform");

            try {
                dataTable = dbQuery(adapter, RptOperationConsts.SQL_SELECT_SHOP_UPDATE, lastUpdateTime);
            } catch (SQLException e) {
                logger.error("报表查询更新门店数据时，数据库操作异常", e);
                adapter.closeQuiet();
                return;
            }

            if (dataTable != null && dataTable.size() > 0) {
                List<Shop> updateShopList = new ArrayList<>();
                dataTable.getRows().forEach(dataRow -> {
                    Shop shop = ClassBuildUtils.mapToObject(dataRow.toMap(), Shop.class);
                    updateShopList.add(shop);
                });

                batchUpdateRptData(RptOperationConsts.SHOP_COLLECTION_NAME, updateShopList, updateShopFieldNames);
            }
        }

        adapter.closeQuiet();
    }

    private DataTable dbQuery(ISqlAdapter adapter, String sql, Date date) throws SQLException {
        SqlCommand cmd = new SqlCommand(sql);
        cmd.setParameter("lastUpdateTime", date);
        return adapter.query(cmd);
    }

    private List<OrderDetail> queryOrderDetailList(ISqlAdapter adapter, String orderId) throws SQLException {
        DataTable dataTable = null;

        SqlCommand cmd = new SqlCommand(RptOperationConsts.SQL_SELECT_ORDER_DETAIL);
        cmd.setParameter("orderId", orderId);
        dataTable = adapter.query(cmd);
        if (dataTable.size() == 0) {
            return null;
        }

        List<OrderDetail> orderDetailList = new ArrayList<>();
        dataTable.getRows().forEach(dataRow -> {
            OrderDetail detail = ClassBuildUtils.mapToObject(dataRow.toMap(), OrderDetail.class);
            orderDetailList.add(detail);
        });

        return orderDetailList;
    }

    public void updateDustdbData() {
        ISqlAdapter adapter = dbAdapterManager.getAdapter("tenantdb");
        if (adapter == null) {
            return;
        }

        DataTable dataTable = null;
        try {
            adapter.useDbName(RptOperationConsts.TENANT_DB_NAME);
            SqlCommand command = new SqlCommand(RptOperationConsts.SQL_APP_DB);
            dataTable = adapter.query(command);
        } catch (SQLException e) {
            logger.error("数据库异常",e);
            adapter.closeQuiet();
            return;
        }

        if (dataTable.size() == 0) {
            logger.warn("未查到租户信息, 请检查数据库配置");
            adapter.closeQuiet();
            return;
        }

        List<AppConfig> appConfigList = new ArrayList<>();
        List<DataRow> dataRows = dataTable.getRows();
        dataRows.forEach(dataRow -> {

            AppConfig appConfig = ClassBuildUtils.mapToObject(dataRow.toMap(), AppConfig.class);
            appConfigList.add(appConfig);
        });

        mongoTemplate.dropCollection(AppConfig.class);
        mongoTemplate.insertAll(appConfigList);
        appConfigs = appConfigList;
        logger.info("mongo于{}更新租户信息完成", new Date());

        adapter.closeQuiet();
    }

    private void batchSaveRptOrderData(String tenantId, String collectionName, List<? extends Object> data) {
        if (data == null || data.size() == 0) {
            return;
        }

        List<DBObject> saveData = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            Object obj = data.get(i);

            DBObject newDoc = new BasicDBObject();
            Field[] fields = obj.getClass().getDeclaredFields();
            for (int j = 0; j < fields.length; j++) {
                Field field = fields[j];
                String key = field.getName();
                if (StringUtils.equals("id", key)) {
                    key = "_id";
                }

                Object value = null;
                field.setAccessible(true);
                try {
                    value = field.get(obj);
                } catch (IllegalAccessException e) {
                    logger.error("安全权限异常",e);
                    return;
                }

                if (value != null) {
                    if (value instanceof BigDecimal) {
                        newDoc.put(key, Decimal128.parse(Converter.toString(value)));
                    } else if (StringUtils.equals(key, "detail")) {
                        List<OrderDetail> details = (List<OrderDetail>) value;
                        List<Document> detailLst = null;
                        try {
                            detailLst = orderDetailConvert(details);
                        } catch (IllegalAccessException e) {
                            logger.error("安全权限异常",e);
                            return;
                        }

                        if (detailLst != null && detailLst.size() != 0) {
                            newDoc.put(key, detailLst);
                        }
                    } else {
                        newDoc.put(key, value);
                    }
                }
            }

            newDoc.put("tenantId", tenantId);
            saveData.add(newDoc);
        }

        if (saveData.size() == 0) {
            return;
        }

        mongoTemplate.getCollection(collectionName).insert(saveData);
    }

    private List<Document> orderDetailConvert(List<OrderDetail> data) throws IllegalAccessException {
        if (data == null || data.size() == 0) {
            return null;
        }

        List<Document> saveData = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            OrderDetail detail = data.get(i);
            Document newDoc = new Document();
            Field[] fields = detail.getClass().getDeclaredFields();
            for (int j = 0; j < fields.length; j++) {
                Field field = fields[j];
                field.setAccessible(true);

                String key = field.getName();
                Object value = null;
                try {
                    value = field.get(detail);
                } catch (IllegalAccessException e) {
                    logger.error("java实体类反射异常", e);
                }

                if (value != null) {
                    if (value instanceof BigDecimal) {
                        newDoc.append(key, Decimal128.parse(Converter.toString(value)));
                    } else {
                        newDoc.append(key, value);
                    }
                }
            }

            saveData.add(newDoc);
        }

        return saveData;
    }

    private void batchUpdateRptData(String collectionName, List<? extends Object> data, List<String> updateFiledName) {
        if (data == null || data.size() == 0) {
            return;
        }

        for (int i = 0; i < data.size(); i++) {
            DBObject query = new BasicDBObject();
            DBObject update = new BasicDBObject();

            Object obj = data.get(i);
            Field[] fields = obj.getClass().getDeclaredFields();
            for (int j = 0; j < fields.length; j++) {
                Field field = fields[j];
                String key = field.getName();
                Object value = null;
                field.setAccessible(true);
                try {
                    value = field.get(obj);
                } catch (IllegalAccessException e) {
                    logger.error("安全权限异常",e);
                    return;
                }


                if (StringUtils.equals("id", key)) {
                    query.put("_id", value);
                }

                for (int k = 0; k < updateFiledName.size(); k++) {
                    if (StringUtils.equals(key, updateFiledName.get(k))) {
                        update.put(key, value);
                    }
                }
            }

            mongoTemplate.getCollection(collectionName).update(query, new BasicDBObject("$set", update));
        }
    }

    /**
     * 设定每天凌晨3点更新报表数据
     * 获取距当前时间最近一次更新报表时间
     */
    private Date getLastUpdateTime() {
        Calendar calendar = Calendar.getInstance();
        if (calendar.get(Calendar.HOUR_OF_DAY) >= 3) {
            calendar.set(Calendar.HOUR_OF_DAY, 3);
        } else {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            calendar.set(Calendar.HOUR_OF_DAY, 3);
        }
//        calendar.add(Calendar.DAY_OF_MONTH, -300);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }
}
