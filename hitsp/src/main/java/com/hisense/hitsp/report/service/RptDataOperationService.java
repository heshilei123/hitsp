package com.hisense.hitsp.report.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisense.dustcore.util.Converter;
import com.hisense.hitsp.report.util.DateUtil;
import com.hisense.hitsp.report.util.RptDataUtil;
import com.mongodb.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 运营端服务
 *
 * @author yanglei
 *         date: 2017-08-03.
 */
@Service
public class RptDataOperationService {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String SUCCESS_RETURN = "OK";
    private static final String ERROR_RETURN = "NOTOK";

    @Autowired
    MongoTemplate mongoTemplate;

    /**
     * 获取租户统计数据
     */
    public Object getTenantTotalData() {
        JSONObject result = new JSONObject();
        JSONObject tenantData = new JSONObject();
        JSONObject shopData = new JSONObject();

        List<Map<String, Object>> tenantsList = getAllTenantInfo();

        int tenantTotal = tenantsList.size();
        if (tenantTotal == 0) {
            logger.warn("无租户信息");
            return null;
        }


        int tenantWeekNew = 0;

        int tenantActiveCount = 0;
        int shopWeekNew = 0;

        int shopActiveCount = 0;
        int shopTotal = 0;

        for (int n = 0; n < tenantTotal; n++) {
            boolean tenantActive = false;

            //近7日新增租户
            Map<String, Object> tenant = tenantsList.get(n);
            Date tenantGmtCreate = Converter.toDate(tenant.get("gmtCreate"));
            if (tenantGmtCreate.getTime() <= System.currentTimeMillis() && tenantGmtCreate.getTime() >= DateUtils.addDays(new Date(), -7).getTime()) {
                tenantWeekNew++;
            }

            List<Map<String, Object>> shopListPerTenant = new ArrayList<>();
            DBCursor dbCursor = mongoTemplate.getCollection("shop").find(new BasicDBObject("tenantId", tenant.get("tenantId")));
            while (dbCursor.hasNext()) {
                DBObject dbObject = dbCursor.next();
                Map<String, Object> e = new HashMap();
                for (String key : dbObject.keySet()) {
                    Object value = dbObject.get(key);
                    e.put(key, value);

                    //近7日新增门店
                    if (StringUtils.equals(key, "gmtCreate")) {
                        Date shopGmtCreate = Converter.toDate(value);
                        if (shopGmtCreate.getTime() <= System.currentTimeMillis() && shopGmtCreate.getTime() >= DateUtils.addDays(new Date(), -7).getTime()) {
                            shopWeekNew++;
                        }
                    }
                }

                shopListPerTenant.add(e);
            }

            shopTotal += shopListPerTenant.size();

            //活跃租户
            List<Map<String, Object>> orderListPerTenant = new ArrayList<>();
            DBCursor dbCursor2 = mongoTemplate.getCollection("order").find(new BasicDBObject("tenantId", tenant.get("tenantId")));
            while (dbCursor2.hasNext()) {
                DBObject dbObject2 = dbCursor2.next();
                Map<String, Object> e = new HashMap();
                for (String key : dbObject2.keySet()) {
                    Object value = dbObject2.get(key);
                    e.put(key, value);
                    //近7日新增门店
                    if (StringUtils.equals(key, "gmtCreate")) {
                        Date orderGmtCreate = Converter.toDate(value);
                        if (orderGmtCreate.getTime() <= System.currentTimeMillis() && orderGmtCreate.getTime() >= DateUtils.addDays(new Date(), -7).getTime()) {
                            tenantActive = true;
                        }
                    }
                }

                orderListPerTenant.add(e);
            }

            if (tenantActive == true) {
                tenantActiveCount++;
            }

            //活跃门店数
            Calendar oneDayStart = Calendar.getInstance();
            oneDayStart.setTime(new Date());
            oneDayStart.add(Calendar.DAY_OF_MONTH, -7);
            oneDayStart.set(Calendar.HOUR_OF_DAY, 0);
            oneDayStart.set(Calendar.MINUTE, 0);
            oneDayStart.set(Calendar.SECOND, 0);
            oneDayStart.set(Calendar.MILLISECOND, 0);
            Calendar oneDayEnd = Calendar.getInstance();
            oneDayEnd.setTime(new Date());
//            oneDayEnd.add(Calendar.DAY_OF_MONTH, -3);
            oneDayEnd.set(Calendar.HOUR_OF_DAY, 23);
            oneDayEnd.set(Calendar.MINUTE, 59);
            oneDayEnd.set(Calendar.SECOND, 59);
            oneDayEnd.set(Calendar.MILLISECOND, 999);

            //match
            BasicDBObject[] array = {
                    new BasicDBObject("gmtCreate", new BasicDBObject("$gte", oneDayStart.getTime())),
                    new BasicDBObject("gmtCreate", new BasicDBObject("$lte", oneDayEnd.getTime())),
                    new BasicDBObject("tenantId", tenant.get("tenantId"))};
            BasicDBObject cond = new BasicDBObject();
            cond.put("$and", array);
            DBObject match = new BasicDBObject("$match", cond);
            //group
            DBObject groupFields = new BasicDBObject("_id", "$shopId");
            groupFields.put("count", new BasicDBObject("$sum", 1));
            DBObject group = new BasicDBObject("$group", groupFields);

            AggregationOutput output = mongoTemplate.getCollection("order")
                    .aggregate(Arrays.asList(match, group));
            Iterable<DBObject> list = output.results();
            for (DBObject dbObject : list) {
                shopActiveCount++;
                logger.info("活跃门店: shopId={}, orderCount={}", dbObject.get("_id"), dbObject.get("count"));
            }
        }

        tenantData.put("total", tenantTotal);
        tenantData.put("weekNew", tenantWeekNew);
        tenantData.put("active", tenantActiveCount);

        shopData.put("total", shopTotal);
        shopData.put("weekNew", shopWeekNew);
        shopData.put("active", shopActiveCount);

        result.put("tenantData", tenantData);
        result.put("shopData", shopData);
        return result;
    }

    /**
     * 获取租户-门店结构数据接口
     */

    public Object getTenantShopInfo() {
        JSONObject result = new JSONObject();
        JSONArray tenantsArray = new JSONArray();

        List<Map<String, Object>> tenantsList = getAllTenantInfo();
        int tenantTotal = tenantsList.size();
        if (tenantTotal == 0) {
            logger.warn("无租户信息");
            result.put("tenants", null);
            return result;
        }

        tenantsList.forEach(tenant -> {
            JSONObject tenantObject = new JSONObject();
            JSONArray shopsArray = new JSONArray();

            ObjectMapper objectMapper = new ObjectMapper();

            DBObject projectFields = new BasicDBObject();
            projectFields.put("shopid", "$_id");
            projectFields.put("shopname", "$name");
            projectFields.put("_id", 0);
            projectFields.put("tenantId", "$tenantId");
            DBObject project = new BasicDBObject("$project", projectFields);
            DBObject match = new BasicDBObject("$match", new BasicDBObject("tenantId", Converter.toString(tenant.get("tenantId"))));

            List<DBObject> pipeline = new ArrayList<>();
            pipeline.add(project);
            pipeline.add(match);

            AggregationOutput output = mongoTemplate.getCollection("shop").aggregate(pipeline);
            Iterable<DBObject> outputList = output.results();
            for (DBObject dbObject : outputList) {
                try {
                    JSONObject shopObj = JSON.parseObject(objectMapper.writeValueAsString(dbObject));
                    shopObj.remove("tenantId");
                    shopsArray.add(shopObj);
                    logger.info("count by first name: {}", objectMapper.writeValueAsString(dbObject));
                    //TODO 待解析
                } catch (JsonProcessingException e) {
                    logger.error("获取租户门店数据时Json处理异常", e);
                }
                logger.info("活跃门店: shopId={}, orderCount={}", dbObject.get("_id"), dbObject.get("count"));
            }


            tenantObject.put("tenantname", Converter.toString(tenant.get("appAlias")));
            tenantObject.put("tenantid", Converter.toString(tenant.get("tenantId")));
            tenantObject.put("shops", shopsArray);
            tenantsArray.add(tenantObject);
        });

        result.put("tenants", tenantsArray);
        return result;
    }

    /**
     * 获取销售概览数据接口
     *
     * @param tenantId
     * @param shopId
     * @param dateRange
     */
    public Object getTenantSaleInfo(String tenantId, String shopId, String dateRange) {
        //参数合法性和有效性校验
        if (StringUtils.isEmpty(tenantId)) {
            logger.error("请求tenantId不能为空");
            return ERROR_RETURN;
        }

        if (StringUtils.isEmpty(dateRange)) {
            logger.error("请求参数dateRange不能为空");
            return ERROR_RETURN;
        }

        Criteria criteria = null;
        Date beginDate = null;
        Date endDate = null;
        BasicDBObject query = null;
        BasicDBObject beforeQuery = null;
        switch (dateRange) {
            case "today":
                criteria = Criteria.where("gmtCreate")
                        .gte(DateUtil.getOneDay(new Date(), 0, true));

                beginDate = DateUtil.getOneDay(new Date(), 0, true);
                endDate = beginDate;
                query = new BasicDBObject("gmtCreate", new BasicDBObject("$gte", endDate));
                beforeQuery = new BasicDBObject("gmtCreate", new BasicDBObject("$lt", beginDate));
                break;
            case "yesterday":
                criteria = Criteria.where("gmtCreate")
                        .gte(DateUtil.getOneDay(new Date(), -1, true))
                        .lte(DateUtil.getOneDay(new Date(), -1, false));
                beginDate = DateUtil.getOneDay(new Date(), -1, true);
                endDate = DateUtil.getOneDay(new Date(), 0, true);
                query = new BasicDBObject("gmtCreate", new BasicDBObject("$gte", beginDate).append("$lt", endDate));
                beforeQuery = new BasicDBObject("gmtCreate", new BasicDBObject("$lt", beginDate));
                break;
            case "thisweek":
                criteria = Criteria.where("gmtCreate")
                        .gte(DateUtil.getOneDay(new Date(), -6, true))
                        .lte(new Date());
                beginDate = DateUtil.getOneDay(new Date(), -6, true);
                endDate = DateUtil.getOneDay(new Date(), 0, true);
                query = new BasicDBObject("gmtCreate", new BasicDBObject("$gte", beginDate).append("$lt", endDate));
                beforeQuery = new BasicDBObject("gmtCreate", new BasicDBObject("$lt", beginDate));
                break;
            default:
                logger.error("请求参数dateRange无效");
                break;
        }

        if (!StringUtils.isEmpty(shopId)) {
            criteria.and("shopId").is(shopId);
            query.append("shopId", shopId);
            beforeQuery.append("shopId", shopId);
        }

        //数据查询及返回数据整合
        JSONObject result = new JSONObject();
        JSONObject saleData = new JSONObject();
        JSONObject billData = new JSONObject();
        JSONObject customerData = new JSONObject();

        List<JSONObject> tempList = new ArrayList<>();

        Aggregation aggregation1 = Aggregation.newAggregation(Arrays.asList(
                Aggregation.match(Criteria.where("status").is("4").and("tenantId").is(tenantId).andOperator(criteria)),
                Aggregation.group("orderFrom", "payType").sum("totalPrice").as("sum")
        ));


        AggregationResults<BasicDBObject> aggregationResults1 = mongoTemplate.aggregate(aggregation1, "order", BasicDBObject.class);
        if (!aggregationResults1.getMappedResults().isEmpty()) {
            tempList = RptDataUtil.mongoAggregationResults2List(aggregationResults1, "order");
        }
        saleData = RptDataUtil.calculateSaleDate(tempList);
        
        //订单数据
        tempList.clear();
        Aggregation aggregation2 = Aggregation.newAggregation(Arrays.asList(
                new MatchOperation(Criteria.where("tenantId").is(tenantId).andOperator(criteria)),
                Aggregation.group("orderFrom", "status").count().as("sum")
        ));

        AggregationResults<BasicDBObject> aggregationResults2 = mongoTemplate.aggregate(aggregation2, "order", BasicDBObject.class);
        if (!aggregationResults2.getMappedResults().isEmpty()) {
            tempList = RptDataUtil.mongoAggregationResults2List(aggregationResults2, "order");
        }

        billData = RptDataUtil.calculateBillDate(tempList);

        //顾客统计数据
        List<String> customers = mongoTemplate.getCollection("order").distinct("recipientPhone",
                query.append("tenantId", tenantId));
        List<String> beforeCustomers = mongoTemplate.getCollection("order").distinct("recipientPhone",
                beforeQuery.append("tenantId", tenantId));
        JSONObject customerTotal = new JSONObject();
        JSONObject customerMeituan = new JSONObject();
        JSONObject customerEleme = new JSONObject();

        int customersCount = customers.size();
        int customerOld = 0;
        int customerNew = 0;
        for (int i = 0; i < customers.size(); i++) {
            if (beforeCustomers != null && beforeCustomers.size() != 0) {
                if (beforeCustomers.contains(customers.get(i))) {
                    customerOld++;
                }
            }
        }

        customerNew = customersCount - customerOld;
        customerTotal.put("sum", customersCount);
        customerTotal.put("new", customerNew);
        customerTotal.put("old", customerOld);
        customerData.put("total", customerTotal);

        List<String> meituanCustomers = mongoTemplate.getCollection("order").distinct("recipientPhone",
                query.append("tenantId", tenantId).append("orderFrom", "12"));
        List<String> meituanBeforeCustomers = mongoTemplate.getCollection("order").distinct("recipientPhone",
                beforeQuery.append("tenantId", tenantId).append("orderFrom", "12"));

        int meituanCustomersCount = meituanCustomers.size();
        int meituanCustomerOld = 0;
        int meituanCustomerNew = 0;
        for (int j = 0; j < meituanCustomers.size(); j++) {
            if (meituanBeforeCustomers != null && meituanBeforeCustomers.size() != 0) {
                if (meituanBeforeCustomers.contains(meituanCustomers.get(j))) {
                    meituanCustomerOld++;
                }
            }
        }
        meituanCustomerNew = meituanCustomersCount - meituanCustomerOld;
        customerMeituan.put("sum", meituanCustomersCount);
        customerMeituan.put("new", meituanCustomerNew);
        customerMeituan.put("old", meituanCustomerOld);

        customerEleme.put("sum", customersCount - meituanCustomersCount);
        customerEleme.put("new", customerNew - meituanCustomerNew);
        customerEleme.put("old", customerOld - meituanCustomerOld);

        customerData.put("total", customerTotal);
        customerData.put("meituan", customerMeituan);
        customerData.put("eleme", customerEleme);

        result.put("saleData", saleData);
        result.put("billData", billData);
        result.put("customerData", customerData);
        return result;
    }

    private List getAllTenantInfo() {
        List<Map<String, Object>> tenantsList = new ArrayList<>();
        DBCursor dbCursor = mongoTemplate.getCollection("appConfig").find();
        while (dbCursor.hasNext()) {
            DBObject dbObject = dbCursor.next();
            Map<String, Object> e = new HashMap();
            boolean isDb = false;
            for (String key : dbObject.keySet()) {
                Object value = dbObject.get(key);
                if (StringUtils.equals(key, "dbName") && value != null && String.valueOf(value).indexOf("hitsp") == 0) {
                    isDb = true;
                    break;
                }
                e.put(key, value);
            }

            if (isDb) {
                tenantsList.add(e);
            }

            logger.info("dbName: {}", Converter.toString(e.get("dbName")));
        }
        return tenantsList;
    }

}
