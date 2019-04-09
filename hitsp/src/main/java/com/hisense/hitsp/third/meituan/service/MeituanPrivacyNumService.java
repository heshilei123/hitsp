package com.hisense.hitsp.third.meituan.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hisense.dustdb.DbAdapterManager;
import com.hisense.dustdb.sql.DataTable;
import com.hisense.dustdb.sql.ISqlAdapter;
import com.hisense.dustdb.sql.SqlCommand;
import com.hisense.hitsp.common.CommonUtil;
import com.hisense.hitsp.common.DataBaseCount;
import com.hisense.hitsp.common.HitspException;
import com.hisense.hitsp.common.HttpUtil;
import com.hisense.hitsp.service.OrderSqlConst;
import com.hisense.hitsp.service.PushService;
import com.hisense.hitsp.service.ShopSqlConst;
import com.hisense.hitsp.third.meituan.MeituanInfoConst;
import com.sankuai.sjst.platform.developer.utils.SignUtils;
import com.sankuai.sjst.platform.developer.utils.WebUtils;
import io.swagger.models.auth.In;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * Created by user on 2018/6/14.
 */
@Service
public class MeituanPrivacyNumService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    DbAdapterManager dbAdapterManager;

    public void getDowngradePhone() {
        Integer orderCount = 1000;
        Integer degradOffset = 0;
        while (orderCount >= 1000) {
            //构建参数
            Map<String, String> params = createParams(degradOffset);
            //post请求
            try {
                String restr = WebUtils.post(MeituanInfoConst.PULL_PHONE_NUMBER_URL, params);
                logger.info("美团返回：" + restr);
                if (StringUtils.isEmpty(restr)) {
                    orderCount = 0;
                } else {
                    JSONObject reJson = JSON.parseObject(restr);
                    if (reJson.containsKey("error")) {
                        orderCount = 0;
                        String msg = StringUtils.isEmpty((reJson.getJSONObject("error").getString("message"))) ? (reJson.getJSONObject("error").getString("msg")) : (reJson.getJSONObject("error").getString("message"));
                        logger.error("美团隐私号-拉取真实手机号返回错误信息：" + msg);
                    } else if (reJson.containsKey("data")) {
                        JSONArray dataList = reJson.getJSONArray("data");
                        //保存真实号码
                        saveRealPhone(dataList);
                    }
                }
            } catch (Exception e) {
                logger.error("请求美团“隐私号-批量拉取手机号”接口出错", e);
            }
        }
    }

    public Map<String, String> createParams(Integer degradOffset) {
        Map<String, String> result = new HashMap<>();
        result.put("charset", "UTF-8");
        result.put("timestamp", getSecondTimestamp());
        result.put("degradOffset", String.valueOf(degradOffset));
        result.put("degradLimit", String.valueOf(1000));
        result.put("developerId", MeituanInfoConst.SIGN_KEY);
        String sign = SignUtils.createSign(MeituanInfoConst.SIGN_KEY, result);
        result.put("sign", sign);
        return result;
    }

    public String getSecondTimestamp() {
        String timestamp = null;
        String time = String.valueOf(new Date().getTime());
        int length = time.length();
        if (length > 3) {
            timestamp = time.substring(0, length - 3);
        }
        return timestamp;
    }


    public void saveRealPhone(JSONArray list) {
        ISqlAdapter adapter = dbAdapterManager.getAdapter("tenantdb");
        try {
            for (int i = 0; i < list.size(); i++) {
                JSONObject json = list.getJSONObject(i);
                adapter = getDb(adapter, json.getString("ePoiId"));
                if (adapter != null) {
                    checkAndSave(adapter,json);
                }
            }
            adapter.commit();
        } catch (SQLException ex) {
            logger.error("执行数据库操作异常",ex);
        } finally {
            adapter.closeQuiet();
        }

    }

    public ISqlAdapter getDb(ISqlAdapter adapter, String ePoiId)throws SQLException{
        String dbName = null;
        for (int i = 1; i <= DataBaseCount.databaseCount; i++) {
            try {
                adapter.useDbName(CommonUtil.DB_NAME + i);

            } catch (SQLException e) {
                logger.error(CommonUtil.DB_NAME + "数据库不存在", e);
            }

            if (checkShopId(adapter, ePoiId)) {
                dbName = CommonUtil.DB_NAME + i;
                break;
            }
        }

        if (StringUtils.isEmpty(dbName)) {
            logger.error("根据ePoiId:" + ePoiId + "未查询到数据库");
            return null;
        }

        return adapter;
    }

    private boolean checkShopId(ISqlAdapter adapter, String ePoiId) throws SQLException {
        DataTable dt = null;
        try {
            SqlCommand cmd = new SqlCommand(MeituanSqlConst.SQL_SELECT_SHOP_BY_EPOIID);
            cmd.setParameter("ePoiId", ePoiId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("根据门店查找数据库抛出异常", e);
            throw e;
        }

        if (dt == null || dt.size() == 0) {
            return false;
        }

        return true;
    }

    public void  checkAndSave(ISqlAdapter adapter,JSONObject json)throws SQLException{
        SqlCommand selectCmd = new SqlCommand(OrderSqlConst.SQL_SELECT_MT_ORDER_REAL_PHONE);
        selectCmd.setParameter("orderId", json.getString("orderId"));
        DataTable dt = null;
        dt = adapter.query(selectCmd);
        if (dt == null || dt.size() == 0) {
            SqlCommand cmd = new SqlCommand(OrderSqlConst.SQL_INSERT_MT_ORDER_REAL_PHONE);
            cmd.setParameter("id", json.getString("id"));
            cmd.setParameter("orderId", json.getString("orderId"));
            cmd.setParameter("orderIdView", json.getString("orderIdView"));
            cmd.setParameter("daySeq", json.getString("daySeq"));
            cmd.setParameter("realPhoneNumber", json.getString("realPhoneNumber"));
            cmd.setParameter("ePoiId", json.getString("ePoiId"));
            adapter.update(cmd);
        }
    }
}
