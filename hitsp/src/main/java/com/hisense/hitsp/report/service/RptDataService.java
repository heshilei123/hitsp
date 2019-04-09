package com.hisense.hitsp.report.service;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.hisense.dustcore.util.ClassBuildUtils;
import com.hisense.dustcore.util.Converter;
import com.hisense.dustdb.TenantAdapterManager;
import com.hisense.dustdb.sql.DataRow;
import com.hisense.dustdb.sql.DataTable;
import com.hisense.dustdb.sql.ISqlAdapter;
import com.hisense.dustdb.sql.SqlCommand;
import com.hisense.dustms.common.DustMsException;
import com.hisense.hitsp.model.Shop;
import com.hisense.hitsp.report.util.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 外卖租户端服务
 *
 * @author yanglei
 *         date: 2017-08-02.
 */
@Service
public class RptDataService {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String SUCCESS_RETURN = "OK";
    private static final String ERROR_RETURN = "NOTOK";

    @Autowired
    TenantAdapterManager tenantAdapterManager;

    /**
     * 获取聚合数据接口
     *
     * @param tenantId
     */
    public Object getTotalData(String tenantId, String userId) throws DustMsException {
        if (StringUtils.isEmpty(tenantId) || StringUtils.isEmpty(userId)) {
            logger.error("租户Id和用户Id均不能为空");
            return ERROR_RETURN;
        }

        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, "001");
        if (adapter == null) {
            throw new DustMsException("数据库无法连接");
        }

        JSONObject result = new JSONObject();
        JSONObject saleData = new JSONObject();
        JSONObject billData = new JSONObject();
        JSONObject customerData = new JSONObject();

        //销售数据
        BigDecimal totalSum = new BigDecimal(0);
        BigDecimal totalOnline = new BigDecimal(0);
        BigDecimal totalOffline = new BigDecimal(0);
        BigDecimal mtSum = new BigDecimal(0);
        BigDecimal mtOnline = new BigDecimal(0);
        BigDecimal mtOffline = new BigDecimal(0);
        BigDecimal eSum = new BigDecimal(0);
        BigDecimal eOnline = new BigDecimal(0);
        BigDecimal eOffline = new BigDecimal(0);
        BigDecimal jdSum = new BigDecimal(0);
        BigDecimal jdOnline = new BigDecimal(0);
        BigDecimal jdOffline = new BigDecimal(0);
        BigDecimal ebSum = new BigDecimal(0);
        BigDecimal ebOnline = new BigDecimal(0);
        BigDecimal ebOffline = new BigDecimal(0);

        try {
            totalSum = getSaleTotal06(adapter, userId);
            totalOnline = getSaleTotalOnline06(adapter, userId);
            totalOffline = totalSum.subtract(totalOnline);

            mtSum = getSaleMtSum06(adapter, userId);
            mtOnline = getSaleMtOnline06(adapter, userId);
            mtOffline = mtSum.subtract(mtOnline);

            eSum = getSaleESum06(adapter, userId);
            eOnline = getSaleEOnline06(adapter, userId);
            eOffline = eSum.subtract(eOnline);

            jdSum = getSaleJdSum06(adapter, userId);
            jdOnline = getSaleJdOnline06(adapter, userId);
            jdOffline = jdSum.subtract(jdOnline);

            ebSum = ((totalSum.subtract(mtSum)).subtract(eSum)).subtract(jdSum);
            ebOnline = ((totalOnline.subtract(mtOnline)).subtract(eOnline)).subtract(jdOnline);
            ebOffline = ebSum.subtract(ebOnline);
        } catch (SQLException e) {
            logger.error("数据库异常", e);
            adapter.rollbackQuiet();
            adapter.closeQuiet();
            return ERROR_RETURN;
        }

        JSONObject saleTotal = new JSONObject();
        JSONObject saleMeituan = new JSONObject();
        JSONObject saleEleme = new JSONObject();
        JSONObject saleJdleme = new JSONObject();
        JSONObject saleEb = new JSONObject();

        saleTotal.put("sum", totalSum);
        saleTotal.put("online", totalOnline);
        saleTotal.put("offline", totalOffline);

        saleMeituan.put("sum", mtSum);
        saleMeituan.put("online", mtOnline);
        saleMeituan.put("offline", mtOffline);

        saleEleme.put("sum", eSum);
        saleEleme.put("online", eOnline);
        saleEleme.put("offline", eOffline);

        saleJdleme.put("sum", jdSum);
        saleJdleme.put("online", jdOnline);
        saleJdleme.put("offline", jdOffline);

        saleEb.put("sum", ebSum);
        saleEb.put("online", ebOnline);
        saleEb.put("offline", ebOffline);

        saleData.put("total", saleTotal);
        saleData.put("meituan", saleMeituan);
        saleData.put("eleme", saleEleme);
        saleData.put("jd", saleJdleme);
        saleData.put("eb", saleEb);


        //订单数据
        int billTotalSum = 0;
        int totalEffect = 0;
        int totalInvalid = 0;
        int billMtSum = 0;
        int mtEffect = 0;
        int mtInvalid = 0;
        int billESum = 0;
        int eEffect = 0;
        int eInvalid = 0;
        int billJdSum = 0;
        int jdEffect = 0;
        int jdInvalid = 0;
        int billEbSum = 0;
        int ebEffect = 0;
        int ebInvalid = 0;

        try {
            billTotalSum = getBillTotal06(adapter, userId);
            totalInvalid = getBillTotalInvaild06(adapter, userId);
            totalEffect = billTotalSum - totalInvalid;

            billMtSum = getBillMtTotal06(adapter, userId);
            mtEffect = getBillMtVaild06(adapter, userId);
            mtInvalid = billMtSum - mtEffect;

            billESum = getBillETotal06(adapter, userId);
            eEffect = getBillEVaild06(adapter, userId);
            eInvalid = billESum - eEffect;

            billJdSum = getBillJdTotal06(adapter, userId);
            jdEffect = getBillJdVaild06(adapter, userId);
            jdInvalid = billJdSum - jdEffect;

            billEbSum = billTotalSum - billMtSum - billESum - billJdSum;
            ebEffect = totalEffect - mtEffect - eEffect - jdEffect;
            ebInvalid = totalInvalid - mtInvalid - eInvalid - jdInvalid;
        } catch (SQLException e) {
            logger.error("数据库异常", e);
            adapter.rollbackQuiet();
            adapter.closeQuiet();
            return ERROR_RETURN;
        }

        JSONObject billTotal = new JSONObject();
        JSONObject billMeituan = new JSONObject();
        JSONObject billEleme = new JSONObject();
        JSONObject billJd = new JSONObject();
        JSONObject billEb = new JSONObject();

        billTotal.put("sum", billTotalSum);
        billTotal.put("effect", totalEffect);
        billTotal.put("invalid", totalInvalid);

        billMeituan.put("sum", billMtSum);
        billMeituan.put("effect", mtEffect);
        billMeituan.put("invalid", mtInvalid);

        billEleme.put("sum", billESum);
        billEleme.put("effect", eEffect);
        billEleme.put("invalid", eInvalid);

        billJd.put("sum", billJdSum);
        billJd.put("effect", jdEffect);
        billJd.put("invalid", jdInvalid);

        billEb.put("sum", billEbSum);
        billEb.put("effect", ebEffect);
        billEb.put("invalid", ebInvalid);

        billData.put("total", billTotal);
        billData.put("meituan", billMeituan);
        billData.put("eleme", billEleme);
        billData.put("jd", billJd);
        billData.put("eb", billEb);

        //顾客统计数据
        //TODO limit
        List<String> todayCustomers = Lists.newArrayList();
        List<String> yesterdayCustomers = Lists.newArrayList();
        List<String> todayBeforeCustomers = Lists.newArrayList();
        List<String> yesterdayBeforeCustomers = Lists.newArrayList();

        try {
            todayCustomers = getTodayCustomers06(adapter, userId);
            yesterdayCustomers = getYesterdayCustomers06(adapter, userId);
            todayBeforeCustomers = getOneTodayBeforeCustomers06(adapter, 0, userId);
            yesterdayBeforeCustomers = getOneTodayBeforeCustomers06(adapter, -1, userId);
        } catch (SQLException e) {
            logger.error("数据库异常", e);
            adapter.rollbackQuiet();
            adapter.closeQuiet();
            return ERROR_RETURN;
        }
        //total
        JSONObject customerTotal = new JSONObject();

        int todayCustomersCount = todayCustomers.size();
        int yesterdayCustomersCount = yesterdayCustomers.size();
        if (yesterdayCustomersCount == 0) {
            logger.warn("昨日没有顾客!");
            customerTotal.put("trend", 1);
        } else {
            customerTotal.put("trend", (double) (todayCustomersCount - yesterdayCustomersCount) / yesterdayCustomersCount);
        }

        customerTotal.put("count", todayCustomersCount);
        customerData.put("total", customerTotal);

        JSONObject customerNew = new JSONObject();
        JSONObject customerOld = new JSONObject();
        int todayOldCount = 0;
        int yesterdayOldCount = 0;
        for (int i = 0; i < todayCustomers.size(); i++) {
            if (todayBeforeCustomers.size() != 0) {
                if (todayBeforeCustomers.contains(todayCustomers.get(i))) {
                    todayOldCount++;
                }
            }
        }

        for (int j = 0; j < yesterdayCustomers.size(); j++) {
            if (yesterdayBeforeCustomers.size() != 0) {
                if (yesterdayBeforeCustomers.contains(yesterdayCustomers.get(j))) {
                    yesterdayOldCount++;
                }
            }
        }

        customerNew.put("count", todayCustomersCount - todayOldCount);
        if (yesterdayCustomersCount - yesterdayOldCount == 0) {
            logger.warn("昨日未有新顾客!昨日顾客总数为: {}人", yesterdayCustomersCount);
            customerNew.put("trend", 1);
        } else {
            customerNew.put("trend", (double) ((todayCustomersCount - todayOldCount) - (yesterdayCustomersCount - yesterdayOldCount)) / (yesterdayCustomersCount - yesterdayOldCount));
        }

        customerOld.put("count", todayOldCount);
        if (yesterdayOldCount == 0) {
            logger.warn("昨日未有老顾客!昨日顾客总数为: {}人", yesterdayCustomersCount);
            customerOld.put("trend", 1);
        } else {
            customerOld.put("trend", (double) (todayOldCount - yesterdayOldCount) / yesterdayOldCount);
        }

        customerData.put("new", customerNew);
        customerData.put("old", customerOld);

        //复购率
        JSONObject rebuyRateObj = new JSONObject();
        BigDecimal rebuyRate = new BigDecimal(0);
        BigDecimal yesterdayRebuyRate = new BigDecimal(0);
        if (todayCustomersCount == 0) {
            logger.warn("今日没有顾客！");
        } else {
            rebuyRate = new BigDecimal(todayOldCount).divide(new BigDecimal(todayCustomersCount), 2, BigDecimal.ROUND_HALF_UP);
        }

        if (yesterdayCustomersCount == 0) {
            logger.warn("昨日没有顾客！");
        } else {
            yesterdayRebuyRate = new BigDecimal(yesterdayOldCount).divide(new BigDecimal(yesterdayCustomersCount), 2, BigDecimal.ROUND_HALF_UP);
        }

        rebuyRateObj.put("count", rebuyRate);
        if (yesterdayRebuyRate.compareTo(new BigDecimal(0)) != 0) {
            rebuyRateObj.put("trend", rebuyRate.divide(yesterdayRebuyRate, 2, BigDecimal.ROUND_HALF_UP));
        } else {
            rebuyRateObj.put("trend", 1);
        }

        customerData.put("rebuyRate", rebuyRateObj);
        result.put("saleData", saleData);
        result.put("billData", billData);
        result.put("customerData", customerData);

        try {
            List shops = getCurrentTenantShopList(adapter, userId);
            result.put("shops", shops);
        } catch (SQLException e) {
            logger.error("数据库异常", e);
            adapter.rollbackQuiet();
            logger.error("获取租户聚合数据时数据库错误");
            return ERROR_RETURN;
        } finally {
            adapter.closeQuiet();
        }

        return result;
    }

    private BigDecimal getSaleTotal06(ISqlAdapter adapter, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_TOTAL_BY_USER_AND_STATUS_AND_TIME);
        cmd.setParameter("status", "5");
        cmd.setParameter("gmtCreate", DateUtil.getOneDay(new Date(), 0, true));
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        if (dt.size() == 1) {
            return Converter.toBigDecimal(dt.get(0, "sum"));
        }

        return new BigDecimal(0);
    }

    private BigDecimal getSaleTotalOnline06(ISqlAdapter adapter, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_TOTAL_BY_USER_AND_STATUS_AND_TIME_AND_PAYTYPE);
        cmd.setParameter("status", "5");
        cmd.setParameter("gmtCreate", DateUtil.getOneDay(new Date(), 0, true));
        cmd.setParameter("payType", "2");
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        if (dt.size() == 1) {
            return Converter.toBigDecimal(dt.get(0, "sum"));
        }

        return new BigDecimal(0);
    }

    private BigDecimal getSaleMtSum06(ISqlAdapter adapter, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_TOTAL_BY_USER_AND_STATUS_AND_TIME_AND_ORDERFROM);
        cmd.setParameter("status", "5");
        cmd.setParameter("gmtCreate", DateUtil.getOneDay(new Date(), 0, true));
        cmd.setParameter("orderFrom", "12");
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        if (dt.size() == 1) {
            return Converter.toBigDecimal(dt.get(0, "sum"));
        }

        return new BigDecimal(0);
    }

    private BigDecimal getSaleESum06(ISqlAdapter adapter, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_TOTAL_BY_USER_AND_STATUS_AND_TIME_AND_ORDERFROM);
        cmd.setParameter("status", "5");
        cmd.setParameter("gmtCreate", DateUtil.getOneDay(new Date(), 0, true));
        cmd.setParameter("orderFrom", "15");
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        if (dt.size() == 1) {
            return Converter.toBigDecimal(dt.get(0, "sum"));
        }

        return new BigDecimal(0);
    }

    private BigDecimal getSaleJdSum06(ISqlAdapter adapter, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_TOTAL_BY_USER_AND_STATUS_AND_TIME_AND_ORDERFROM);
        cmd.setParameter("status", "5");
        cmd.setParameter("gmtCreate", DateUtil.getOneDay(new Date(), 0, true));
        cmd.setParameter("orderFrom", "16");
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        if (dt.size() == 1) {
            return Converter.toBigDecimal(dt.get(0, "sum"));
        }

        return new BigDecimal(0);
    }

    private BigDecimal getSaleMtOnline06(ISqlAdapter adapter, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_TOTAL_BY_USER_AND_STATUS_AND_TIME_AND_PAYTYPE_AND_ORDERFROM);
        cmd.setParameter("status", "5");
        cmd.setParameter("gmtCreate", DateUtil.getOneDay(new Date(), 0, true));
        cmd.setParameter("orderFrom", "12");
        cmd.setParameter("payType", "2");
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        if (dt.size() == 1) {
            return Converter.toBigDecimal(dt.get(0, "sum"));
        }

        return new BigDecimal(0);
    }

    private BigDecimal getSaleEOnline06(ISqlAdapter adapter, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_TOTAL_BY_USER_AND_STATUS_AND_TIME_AND_PAYTYPE_AND_ORDERFROM);
        cmd.setParameter("status", "5");
        cmd.setParameter("gmtCreate", DateUtil.getOneDay(new Date(), 0, true));
        cmd.setParameter("orderFrom", "15");
        cmd.setParameter("payType", "2");
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        if (dt.size() == 1) {
            return Converter.toBigDecimal(dt.get(0, "sum"));
        }

        return new BigDecimal(0);
    }

    private BigDecimal getSaleJdOnline06(ISqlAdapter adapter, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_TOTAL_BY_USER_AND_STATUS_AND_TIME_AND_PAYTYPE_AND_ORDERFROM);
        cmd.setParameter("status", "5");
        cmd.setParameter("gmtCreate", DateUtil.getOneDay(new Date(), 0, true));
        cmd.setParameter("orderFrom", "16");
        cmd.setParameter("payType", "2");
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        if (dt.size() == 1) {
            return Converter.toBigDecimal(dt.get(0, "sum"));
        }

        return new BigDecimal(0);
    }

    private Integer getBillTotal06(ISqlAdapter adapter, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_COUNT_BY_USER_AND_TIME);
        cmd.setParameter("gmtCreate", DateUtil.getOneDay(new Date(), 0, true));
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        if (dt.size() == 1) {
            return Converter.toInteger(dt.get(0, "count"));
        }

        return 0;
    }

    private Integer getBillTotalInvaild06(ISqlAdapter adapter, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_COUNT_BY_USER_AND_TIME_AND_STATUS);
        cmd.setParameter("gmtCreate", DateUtil.getOneDay(new Date(), 0, true));
        cmd.setParameter("status", "5");
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        if (dt.size() == 1) {
            return Converter.toInteger(dt.get(0, "count"));
        }

        return 0;
    }

    private Integer getBillMtVaild06(ISqlAdapter adapter, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_COUNT_BY_USER_AND_TIME_AND_STATUS_AND_ORDERFROM);
        cmd.setParameter("gmtCreate", DateUtil.getOneDay(new Date(), 0, true));
        cmd.setParameter("orderFrom", "12");
        cmd.setParameter("status", "5");
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        if (dt.size() == 1) {
            return Converter.toInteger(dt.get(0, "count"));
        }

        return 0;
    }

    private Integer getBillEVaild06(ISqlAdapter adapter, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_COUNT_BY_USER_AND_TIME_AND_STATUS_AND_ORDERFROM);
        cmd.setParameter("gmtCreate", DateUtil.getOneDay(new Date(), 0, true));
        cmd.setParameter("orderFrom", "15");
        cmd.setParameter("status", "5");
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        if (dt.size() == 1) {
            return Converter.toInteger(dt.get(0, "count"));
        }

        return 0;
    }

    private Integer getBillJdVaild06(ISqlAdapter adapter, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_COUNT_BY_USER_AND_TIME_AND_STATUS_AND_ORDERFROM);
        cmd.setParameter("gmtCreate", DateUtil.getOneDay(new Date(), 0, true));
        cmd.setParameter("orderFrom", "16");
        cmd.setParameter("status", "5");
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        if (dt.size() == 1) {
            return Converter.toInteger(dt.get(0, "count"));
        }

        return 0;
    }

    private Integer getBillMtTotal06(ISqlAdapter adapter, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_COUNT_BY_USER_AND_TIME_AND_ORDERFROM);
        cmd.setParameter("gmtCreate", DateUtil.getOneDay(new Date(), 0, true));
        cmd.setParameter("orderFrom", "12");
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        if (dt.size() == 1) {
            return Converter.toInteger(dt.get(0, "count"));
        }

        return 0;
    }

    private Integer getBillETotal06(ISqlAdapter adapter, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_COUNT_BY_USER_AND_TIME_AND_ORDERFROM);
        cmd.setParameter("gmtCreate", DateUtil.getOneDay(new Date(), 0, true));
        cmd.setParameter("orderFrom", "15");
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        if (dt.size() == 1) {
            return Converter.toInteger(dt.get(0, "count"));
        }

        return 0;
    }

    private Integer getBillJdTotal06(ISqlAdapter adapter, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_COUNT_BY_USER_AND_TIME_AND_ORDERFROM);
        cmd.setParameter("gmtCreate", DateUtil.getOneDay(new Date(), 0, true));
        cmd.setParameter("orderFrom", "16");
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        if (dt.size() == 1) {
            return Converter.toInteger(dt.get(0, "count"));
        }

        return 0;
    }

    private List<String> getTodayCustomers06(ISqlAdapter adapter, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_CUSTERMER_BY_USER_AND_TIME);
        cmd.setParameter("gmtCreate", DateUtil.getOneDay(new Date(), 0, true));
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        List<String> todayCustomers = Lists.newArrayList();
        for (DataRow dr : dt.getRows()) {
            todayCustomers.add(Converter.toString(dr.toMap().get("recipient_phone")));
        }
        return todayCustomers;
    }

    private List<String> getYesterdayCustomers06(ISqlAdapter adapter, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_CUSTERMER_BY_USER_AND_STARTTIME_AND_ENDTIME);
        cmd.setParameter("beginDate", DateUtil.getOneDay(new Date(), -1, true));
        cmd.setParameter("endDate", DateUtil.getOneDay(new Date(), 0, true));
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        List<String> customers = Lists.newArrayList();
        for (DataRow dr : dt.getRows()) {
            customers.add(Converter.toString(dr.toMap().get("recipient_phone")));
        }
        return customers;
    }

    private List<String> getOneTodayBeforeCustomers06(ISqlAdapter adapter, int addDay, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_CUSTERMER_BY_USER_AND_STARTTIME_AND_ENDTIME);
        cmd.setParameter("beginDate", DateUtil.getOneDay(new Date(), -6, true));
        cmd.setParameter("endDate", DateUtil.getOneDay(new Date(), addDay, true));
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        List<String> customers = Lists.newArrayList();
        for (DataRow dr : dt.getRows()) {
            customers.add(Converter.toString(dr.toMap().get("recipient_phone")));
        }
        return customers;
    }

    private List<Shop> getCurrentTenantShopList(ISqlAdapter adapter, String userId) throws SQLException {
        SqlCommand cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_SHOP_BY_USER);
        cmd.setParameter("userId", userId);

        DataTable dt = adapter.query(cmd);
        List<Shop> shops = Lists.newArrayList();
        for (DataRow dr : dt.getRows()) {
            shops.add(ClassBuildUtils.mapToObject(dr.toMap(), Shop.class));
        }
        return shops;
    }

    /**
     * 获取菜品分析数据接口
     *
     * @param tenantId
     */
    public Object getProductsData(String tenantId, String userId, String rankby, String platform, String shopId) throws DustMsException {
        //参数合法性和有效F性校验
        if (StringUtils.isEmpty(tenantId) || StringUtils.isEmpty(userId)) {
            logger.error("请求租户Id和用户Id均不能为空");
            return ERROR_RETURN;
        }

        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, "001");
        if (adapter == null) {
            throw new DustMsException("数据库无法连接");
        }

        //数据解析及返回结果
        JSONObject result = new JSONObject();

        String platformCode = null;
        if (StringUtils.isNotEmpty(platform)) {
            switch (platform) {
                case "all":
                    break;
                case "meituan":
                    platformCode = "12";
                    break;
                case "eleme":
                    platformCode = "15";
                    break;
                case "jd":
                    platformCode = "16";
                    break;
                case "eb":
                    platformCode = "17";
                    break;
                default:
                    break;
            }
        }
        String shopid = shopId;
        List<Map<String, Object>> rankData = null;
        try {
            if (StringUtils.equals(shopid, "all")) {
                shopid = null;
            }
            rankData = getProductRankData06(adapter, rankby, platformCode, shopid, userId);
        } catch (SQLException e) {
            logger.info("数据库异常", e);
            adapter.rollbackQuiet();
            adapter.closeQuiet();
            return ERROR_RETURN;
        }

        result.put("rankdata", rankData);

        if (StringUtils.isEmpty(shopid)) {
            try {
                List shops = getCurrentTenantShopList(adapter, userId);
                result.put("shops", shops);
            } catch (SQLException e) {
                adapter.rollbackQuiet();
                logger.error("获取菜品销售信息时数据库错误", e);
                return ERROR_RETURN;
            } finally {
                adapter.closeQuiet();
            }
        }

        return result;
    }

    private List<Map<String, Object>> getProductRankData06(ISqlAdapter adapter, String rankby, String platformCode, String shopId, String userId) throws SQLException {
        SqlCommand cmd = null;

        if (!StringUtils.isEmpty(platformCode) && !StringUtils.isEmpty(shopId)) {
            cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_PRODUCT_WITH_ORDEFROM_AND_SHOPID);
            cmd.setParameter("shopId", shopId);
            cmd.setParameter("orderFrom", platformCode);
        } else if (!StringUtils.isEmpty(platformCode)) {
            cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_PRODUCT_WITH_ORDEFROM_BY_USER);
            cmd.setParameter("orderFrom", platformCode);
            cmd.setParameter("userId", userId);
        } else if (!StringUtils.isEmpty(shopId)) {
            cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_PRODUCT_WITH_SHOPID);
            cmd.setParameter("shopId", shopId);
        } else {
            cmd = new SqlCommand(RptSqlConsts.SQL_SELECT_ORDER_PRODUCT_BY_USER);
            cmd.setParameter("userId", userId);
        }

        if (StringUtils.equals(rankby, "salcount")) {
            cmd.addOrder("salcount desc");
            cmd.addOrder("saltotal desc");
        } else {
            cmd.addOrder("saltotal desc");
            cmd.addOrder("salcount desc");
        }

        cmd.setPageIndex(0);
        cmd.setPageSize(10);

        DataTable dt = adapter.query(cmd);
        List<Map<String, Object>> products = Lists.newArrayList();
        for (DataRow dr : dt.getRows()) {
            products.add(dr.toMap());
        }
        return products;
    }

}
