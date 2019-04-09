package com.hisense.hitsp.report.util;

import com.alibaba.fastjson.JSONObject;
import com.hisense.hitsp.report.service.RptOperationConsts;
import com.mongodb.BasicDBObject;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 报表数据一些公用处理方法
 *
 * @author yanglei
 * date: 2017-08-08.
 */
public class RptDataUtil {

    public static List mongoAggregationResults2List(AggregationResults<BasicDBObject> aggregationResults, String collectionName) {
        List<JSONObject> list = new ArrayList<>();

        for (BasicDBObject obj : aggregationResults.getMappedResults()) {
            JSONObject jsonObject = new JSONObject();
            for (Map.Entry<String, Object> entry : obj.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value != null && value instanceof Decimal128) {
                    value = ((Decimal128) value).bigDecimalValue();
                }

                if (StringUtils.equals(collectionName, RptOperationConsts.ORDER_COLLECTION_NAME)) {
                    if (!StringUtils.equals(key, "code")) {
                        if (StringUtils.equals(key, "shopName")) {
                            key = "shopname";
                        }

                        if (StringUtils.equals(key, "name")) {
                            key = "pluname";
                        }

                        jsonObject.put(key, value);
                    }
                } else if (StringUtils.equals(collectionName, RptOperationConsts.SHOP_COLLECTION_NAME)) {
                    if (StringUtils.equals(key, "_id")) {
                        key = "id";
                    }

                    if (StringUtils.equals(key, "id") || StringUtils.equals(key, "name")) {
                        jsonObject.put(key, value);
                    }
                }
            }

            list.add(jsonObject);
        }

        return list;
    }

    public static JSONObject calculateBillDate(List<JSONObject> list) {
        JSONObject billData = new JSONObject();
        JSONObject billTotal = new JSONObject();
        JSONObject billMeituan = new JSONObject();
        JSONObject billEleme = new JSONObject();


        int totalSum2 = 0;
        int totalEffect = 0;
        int totalInvalid = 0;
        int mtSum2 = 0;
        int mtEffect = 0;
        int mtInvalid = 0;
        int eSum2 = 0;
        int eEffect = 0;
        int eInvalid = 0;

        for (int m = 0; m < list.size(); m++) {
            JSONObject obj = list.get(m);
            int sum = obj.getInteger("sum");
            totalSum2 += sum;
            if (StringUtils.equals(obj.getString("status"), "5")) { //invalid
                totalInvalid += sum;
                if (StringUtils.equals(obj.getString("orderFrom"), "12")) {
                    mtInvalid += sum;
                } else {
                    eInvalid += sum;
                }
            } else {
                totalEffect += sum;
                if (StringUtils.equals(obj.getString("orderFrom"), "12")) {
                    mtEffect += sum;
                } else {
                    eEffect += sum;
                }
            }
        }

        mtSum2 = mtEffect + mtInvalid;
        eSum2 = eEffect + eInvalid;

        billTotal.put("sum", totalSum2);
        billTotal.put("effect", totalEffect);
        billTotal.put("invalid", totalInvalid);
        billMeituan.put("sum", mtSum2);
        billMeituan.put("effect", mtEffect);
        billMeituan.put("invalid", mtInvalid);
        billEleme.put("sum", eSum2);
        billEleme.put("effect", eEffect);
        billEleme.put("invalid", eInvalid);

        billData.put("total", billTotal);
        billData.put("meituan", billMeituan);
        billData.put("eleme", billEleme);

        return billData;
    }

    public static JSONObject calculateSaleDate(List<JSONObject> list) {
        JSONObject saleData = new JSONObject();

        JSONObject saleTotal = new JSONObject();
        JSONObject saleMeituan = new JSONObject();
        JSONObject saleEleme = new JSONObject();

        BigDecimal totalSum = new BigDecimal(0);
        BigDecimal totalOnline = new BigDecimal(0);
        BigDecimal totalOffline = new BigDecimal(0);
        BigDecimal mtSum = new BigDecimal(0);
        BigDecimal mtOnline = new BigDecimal(0);
        BigDecimal mtOffline = new BigDecimal(0);
        BigDecimal eSum = new BigDecimal(0);
        BigDecimal eOnline = new BigDecimal(0);
        BigDecimal eOffline = new BigDecimal(0);

        for (int k = 0; k < list.size(); k++) {
            JSONObject obj = list.get(k);
            BigDecimal sum = obj.getBigDecimal("sum");
            totalSum = totalSum.add(sum);
            if (StringUtils.equals(obj.getString("payType"), "2")) { //online
                totalOnline = totalOnline.add(sum);
                if (StringUtils.equals(obj.getString("orderFrom"), "12")) {
                    mtOnline = mtOnline.add(sum);
                } else {
                    eOnline = eOnline.add(sum);
                }
            } else {
                totalOffline = totalOffline.add(sum);
                if (StringUtils.equals(obj.getString("orderFrom"), "12")) {
                    mtOffline = mtOffline.add(sum);
                } else {
                    eOffline = eOffline.add(sum);
                }
            }
        }

        mtSum = mtOnline.add(mtOffline);
        eSum = eOnline.add(eOffline);

        saleTotal.put("sum", totalSum);
        saleTotal.put("online", totalOnline);
        saleTotal.put("offline", totalOffline);
        saleMeituan.put("sum", mtSum);
        saleMeituan.put("online", mtOnline);
        saleMeituan.put("offline", mtOffline);
        saleEleme.put("sum", eSum);
        saleEleme.put("online", eOnline);
        saleEleme.put("offline", eOffline);

        saleData.put("total", saleTotal);
        saleData.put("meituan", saleMeituan);
        saleData.put("eleme", saleEleme);

        return saleData;
    }

}
