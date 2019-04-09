package com.hisense.hitsp.third.meituan;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hisense.dustcore.util.Converter;
import com.hisense.dustdb.DbAdapterManager;
import com.hisense.dustdb.TenantAdapterManager;
import com.hisense.dustdb.sql.DataRow;
import com.hisense.dustdb.sql.DataTable;
import com.hisense.dustdb.sql.ISqlAdapter;
import com.hisense.dustdb.sql.SqlCommand;
import com.hisense.dustdb.tenant.SeparatedConfig;
import com.hisense.hitsp.common.CommonOrderStatusUpdateService;
import com.hisense.hitsp.common.CommonProductStatusUpdateService;
import com.hisense.hitsp.common.CommonUtil;
import com.hisense.hitsp.common.HitspBizException;
import com.hisense.hitsp.model.*;
import com.hisense.hitsp.service.ShopSqlConst;
import com.hisense.hitsp.service.account.AccountService;
import com.hisense.hitsp.service.account.AccountSqlConst;
import com.hisense.hitsp.service.pruduct.ProductSqlConst;
import com.hisense.hitsp.service.pruduct.ProductStatusService;
import com.hisense.hitsp.service.pruduct.ProductViewService;
import com.hisense.hitsp.third.IPlatform;
import com.hisense.hitsp.third.meituan.service.MeituanSqlConst;
import com.sankuai.sjst.platform.developer.domain.RequestSysParams;
import com.sankuai.sjst.platform.developer.request.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 美团外卖平台处理
 *
 * @author huangshengtao
 *         date:2017-3-17.
 */
@Component
public class MeituanImpl implements IPlatform {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public final static String SUCCESS_RESULT = "OK";
    public final static String ERROR_RESULT = "NOTOK";

    @Autowired
    DbAdapterManager dbAdapterManager;
    @Autowired
    TenantAdapterManager tenantAdapterManager;
    @Autowired
    AccountService accountService;
    @Autowired
    ProductViewService productViewService;
    @Autowired
    SeparatedConfig separatedConfig;
    @Autowired
    ProductStatusService productStatusService;
    @Autowired
    CommonOrderStatusUpdateService commonOrderStatusUpdateService;
    @Autowired
    CommonProductStatusUpdateService commonProductStatusUpdateService;

    /*订单服务*/

    @Override
    public Object getOrderInfo(String orderId, String dbName, String source) {
        return null;
    }

    /**
     * 确认订单
     *
     * @param orderId
     * @return
     */
    @Override
    public Object confirm(String orderId, String tenantId, String appId) {
        logger.info("美团确认订单开始：orderId={}", orderId);
        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);
        if (adapter == null) {
            logger.error("当前登录租户(tenantId={}, appId={})数据库连接不存在", tenantId, appId);
            return null;
        }

        String shopId = getShopIdByOrderId(adapter, orderId);
        if (StringUtils.isEmpty(shopId)) {
            adapter.closeQuiet();
            return null;
        }

        String token = getMtShopToken(shopId, tenantId, appId);
        if (StringUtils.isEmpty(token)) {
            logger.error("美团确认订单操作时，未取得token");
            adapter.closeQuiet();
            return resultIfTokenNotExist(shopId);
        }

        CipCaterTakeoutOrderConfirmRequest request = new CipCaterTakeoutOrderConfirmRequest();
        RequestSysParams requestSysParams = new RequestSysParams(MeituanInfoConst.SIGN_KEY, token);
        request.setRequestSysParams(requestSysParams);
        request.setOrderId(Converter.toLong(orderId));
        String resultJson = "";
        try {
            resultJson = request.doRequest();
            logger.info("美团确认订单输出：resultJson={}", resultJson);
        } catch (IOException e) {
            logger.error("确认订单接口IO异常", e);
        } catch (URISyntaxException e) {
            logger.error("确认订单接口URI语法异常", e);
        } finally {
            adapter.closeQuiet();
        }

        logger.info("美团确认订单结束");

        return resultJson;
    }

    /**
     * 取消订单
     *
     * @param orderId
     * @param reason
     * @return
     */
    @Override
    public String cancel(String orderId, String reason, String tenantId, String appId) {
        logger.info("美团取消订单开始：orderId={}, reason={}", orderId, reason);
        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);
        if (adapter == null) {
            logger.error("当前登录租户(tenantId={}, appId={})数据库连接不存在", tenantId, appId);
            return null;
        }

        String shopId = getShopIdByOrderId(adapter, orderId);
        if (StringUtils.isEmpty(shopId)) {
            adapter.closeQuiet();
            return null;
        }

        String token = getMtShopToken(shopId, tenantId, appId);
        if (StringUtils.isEmpty(token)) {
            logger.info("美团取消订单操作前，未取得门店token");
            adapter.closeQuiet();
            return null;
        }

        CipCaterTakeoutOrderCancelRequest Request = new CipCaterTakeoutOrderCancelRequest();
        RequestSysParams requestSysParams = new RequestSysParams(MeituanInfoConst.SIGN_KEY, token);
        Request.setRequestSysParams(requestSysParams);
        Request.setOrderId(Converter.toLong(orderId));
        Request.setReason(reason);
        //其他原因（未传code，reasonCode默认为2007）
        Request.setReasonCode("2007");
        String resultJson = "";
        try {
            resultJson = Request.doRequest();
            logger.info("美团取消订单输出：resultJson={}", resultJson);
        } catch (IOException e) {
            logger.error("取消订单接口IO异常", e);
        } catch (URISyntaxException e) {
            logger.error("取消订单接口URI语法异常", e);
        } finally {
            adapter.closeQuiet();
        }

        logger.info("美团取消订单结束");
        return resultJson;
    }

    /**
     * 发起订单配送
     *
     * @param order
     * @return
     */
    @Override
    public Object newShip(Order order, String shipperName, String shipperPhone, String tenantId, String appId) {
        String orderId = order.getOrderId();
        String status = order.getStatus();
        logger.info("美团订单发起配送开始：orderId={}", orderId);
        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);
        if (adapter == null) {
            logger.error("当前登录租户(tenantId={}, appId={})数据库连接不存在", tenantId, appId);
            return null;
        }

        String shopId = getShopIdByOrderId(adapter, orderId);

        if (StringUtils.isEmpty(shopId)) {
            adapter.closeQuiet();
            return null;
        }

        String token = getMtShopToken(shopId, tenantId, appId);
        if (StringUtils.isEmpty(token)) {
            logger.error("美团发起订单配送操作前，未取得token");
            adapter.closeQuiet();
            return resultIfTokenNotExist(shopId);
        }
        Map<String, Object> map = new HashMap<>();
        try {
            map.put("result", SUCCESS_RESULT);
            map.put("message", "");
            map.put("status", "2");
            //配送方式码，详情见http://developer.meituan.com/openapi#7.6 第三节
            String logisticsCode = newGetLogisticsCodeByOrderId(adapter, orderId);
            String resultJson = "";
            if ("0".equals(logisticsCode) || "0000".equals(logisticsCode)) {//商家自配送
                CipCaterTakeoutOrderDeliveringRequest deliveringRequest = new CipCaterTakeoutOrderDeliveringRequest();
                RequestSysParams requestSysParams = new RequestSysParams(MeituanInfoConst.SIGN_KEY, token);
                deliveringRequest.setRequestSysParams(requestSysParams);
                deliveringRequest.setOrderId(Converter.toLong(orderId));
                logger.info("shipperName = {}", shipperName);
                if (StringUtils.isNotEmpty(shipperName)) {
                    deliveringRequest.setCourierName(shipperName);
                } else {
                    deliveringRequest.setCourierName("   ");
                }
                if (StringUtils.isNotEmpty(shipperPhone)) {
                    deliveringRequest.setCourierPhone(shipperPhone);
                } else {
                    deliveringRequest.setCourierPhone("    ");
                }
                try {
                    resultJson = deliveringRequest.doRequest();
                    logger.info("美团订单自配送发起配送输出：resultJson={}", resultJson);
                } catch (IOException e) {
                    logger.error("美团订单自配送发起配送接口IO异常", e);
                } catch (URISyntaxException e) {
                    logger.error("美团订单自配送订单发起配送接口URI语法异常", e);
                }
            } else if ("1001".equals(logisticsCode) || "1002".equals(logisticsCode) || "1004".equals(logisticsCode)) {//美团专送
                CipCaterTakeoutOrderDispatchRequest deliveringRequest = new CipCaterTakeoutOrderDispatchRequest();
                RequestSysParams requestSysParams = new RequestSysParams(MeituanInfoConst.SIGN_KEY, token);
                deliveringRequest.setRequestSysParams(requestSysParams);
                deliveringRequest.setOrderId(Converter.toLong(orderId));
                try {
                    resultJson = deliveringRequest.doRequest();
                    logger.info("美团订单美团专送发起配送输出：resultJson={}", resultJson);
                } catch (IOException e) {
                    logger.error("美团订单美团专送发起配送接口IO异常", e);
                } catch (URISyntaxException e) {
                    logger.error("美团订单美团专送发起配送接口URI语法异常", e);
                }
            } else {//美团其他配送
                map.put("result", ERROR_RESULT);
                map.put("message", "接口暂不支持该配送方式");
                map.put("status", status);
                return map;
            }
            //请求美团接口成功后，修改本地订单状态
            if (StringUtils.isNotEmpty(resultJson) && StringUtils.equals("ok", JSONObject.parseObject(resultJson).getString("data"))) {
                commonOrderStatusUpdateService.newUpdateOrderStatus(adapter, order, "ship");
            } else {
                map.put("result", ERROR_RESULT);
                map.put("message", "请求第三方平台异常异常");
                map.put("status", status);
                return map;
            }
            adapter.commit();
            logger.info("美团发起配送订单结束");
        } catch (SQLException e) {
            adapter.rollbackQuiet();
            logger.error("美团订单发起配送时，数据库操作异常", e);
            map = null;
        } finally {
            adapter.closeQuiet();
        }
        return map;
    }

    /**
     * 订单配送完成（自配送）
     *
     * @param orderId
     * @return
     */
    @Override
    public Object shipComplete(String orderId, String shopPhone, String tenantId, String appId) {
        logger.info("美团订单自配送完成开始：orderId={}", orderId);
        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);
        if (adapter == null) {
            logger.error("当前登录租户(tenantId={}, appId={})数据库连接不存在", tenantId, appId);
            return null;
        }

        String shopId = getShopIdByOrderId(adapter, orderId);

        if (StringUtils.isEmpty(shopId)) {
            adapter.closeQuiet();
            return null;
        }

        String token = getMtShopToken(shopId, tenantId, appId);
        if (StringUtils.isEmpty(token)) {
            logger.error("美团订单自配送完成操作前，未取得token");
            adapter.closeQuiet();
            return resultIfTokenNotExist(shopId);
        }

        CipCaterTakeoutOrderDeliveredRequest deliveredRequest = new CipCaterTakeoutOrderDeliveredRequest();
        RequestSysParams requestSysParams = new RequestSysParams(MeituanInfoConst.SIGN_KEY, token);
        deliveredRequest.setRequestSysParams(requestSysParams);
        deliveredRequest.setOrderId(Converter.toLong(orderId));
        String resultJson = "";
        try {
            resultJson = deliveredRequest.doRequest();
            logger.info("美团订单自配送配送完成输出：resultJson={}", resultJson);
        } catch (IOException e) {
            logger.error("美团订单自配送配送完成接口IO异常", e);
        } catch (URISyntaxException e) {
            logger.error("美团订单自配送订单配送完成接口URI语法异常", e);
        } finally {
            adapter.closeQuiet();
        }
        logger.info("美团订单自配送完成结束");

        return resultJson;
    }

    private String getLogisticsCodeByOrderId(ISqlAdapter adapter, String orderId) {
        if (StringUtils.isEmpty(orderId)) {
            return null;
        }

        if (adapter == null) {
            logger.error("数据库连接不存在");
            return null;
        }

        DataTable dt = null;
        SqlCommand cmd = new SqlCommand(MeituanSqlConst.SQL_SELECT_ORDER_MT_LOGISTICS);
        try {
            cmd.setParameter("id", orderId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("查询订单时数据库抛出异常", e);
            adapter.closeQuiet();
            return null;
        }

        if (dt == null || dt.size() == 0) {
            logger.warn("该订单不存在");
            return null;
        }

        return dt.get(0, "logisticsCode");
    }

    private String newGetLogisticsCodeByOrderId(ISqlAdapter adapter, String orderId) throws SQLException {
        if (StringUtils.isEmpty(orderId)) {
            logger.error("orderid为空");
            throw new SQLException("orderid为空");
        }
        DataTable dt = null;
        SqlCommand cmd = new SqlCommand(MeituanSqlConst.SQL_SELECT_ORDER_MT_LOGISTICS);
        cmd.setParameter("id", orderId);
        dt = adapter.query(cmd);
        if (dt == null || dt.size() == 0) {
            logger.error("该订单不存在");
            throw new SQLException("该订单不存在");
        }
        return dt.get(0, "logisticsCode");
    }


    /*门店服务*/

    /**
     * 门店置营业服务
     *
     * @param shop
     */
    @Override
    public Object openShop(String shop, String tenantId, String appId) {
        //TODO 依据shop取得门店的token
        String token = getMtShopToken(shop, tenantId, appId);
        if (StringUtils.isEmpty(token)) {
            logger.warn("未取得门店(shopId={})认证token，请首先绑定门店获取appAuthToken", shop);
            JSONObject result = new JSONObject();
            result.put("code", "300");
            result.put("error", "未取得门店认证token，请首先绑定门店获取appAuthToken");
            return result;
        }

        RequestSysParams requestSysParams = new
                RequestSysParams(MeituanInfoConst.SIGN_KEY, token);
        CipCaterTakeoutPoiOpenRequest request = new CipCaterTakeoutPoiOpenRequest();
        request.setRequestSysParams(requestSysParams);

        String requestResult = null;
        try {
            requestResult = request.doRequest();
            logger.info("门店置营业返回信息: {}", requestResult);
        } catch (IOException | URISyntaxException e) {
            logger.error("美团门店置营业请求异常: {}", requestResult, e);
            return null;
        }

        return requestResult;
    }

    /**
     * 门店置休息服务
     *
     * @param shop
     */
    @Override
    public Object closeShop(String shop, String tenantId, String appId) {
        //TODO 依据shop取得门店的token
        String token = getMtShopToken(shop, tenantId, appId);
        if (StringUtils.isEmpty(token)) {
            logger.error("美团关闭门店操作前，未取得token");
            return null;
        }

        CipCaterTakeoutPoiCloseRequest request = new CipCaterTakeoutPoiCloseRequest();
        RequestSysParams requestSysParams = new RequestSysParams(MeituanInfoConst.SIGN_KEY, token);
        request.setRequestSysParams(requestSysParams);

        String requestResult = null;
        try {
            requestResult = request.doRequest();
            logger.info("门店置休息返回信息: {}", requestResult);
        } catch (IOException | URISyntaxException e) {
            logger.error("美团门店置休息请求异常: {}", requestResult, e);
        }

        return requestResult;
    }

    @Override
    public Object bindShop(String shop, String thirdId, String tenantId, String appId) throws HitspBizException {
        return null;
    }

    @Override
    public Object unbindShop(String shop, String tenantId, String appId) throws HitspBizException {
        return null;
    }

    /**
     * 修改门店营业时间服务
     *
     * @param shop
     * @param sellingTime
     */
    @Override
    public Object updateShopOpenTime(String shop, String sellingTime, String tenantId, String appId) {
        //TODO 依据shop取得门店的token
        String token = getMtShopToken(shop, tenantId, appId);
        if (StringUtils.isEmpty(token)) {
            logger.error("美团更新门店营业操作前，未取得token");
            return null;
        }

        CipCaterTakeoutPoiOpenTimeUpdateRequest request = new CipCaterTakeoutPoiOpenTimeUpdateRequest();
        RequestSysParams requestSysParams = new RequestSysParams(MeituanInfoConst.SIGN_KEY, token);
        request.setRequestSysParams(requestSysParams);
        JSONObject sellTime = JSON.parseObject(sellingTime);
        StringBuilder sb = new StringBuilder("");
        sb.append(sellTime.getString("beginTime"));
        sb.append("-");
        sb.append(sellTime.getString("endTime"));
        request.setOpenTime(sb.toString());

        String result = null;
        try {
            result = request.doRequest();
            logger.info("修改门店营业时间返回信息: {}", result);
        } catch (IOException | URISyntaxException e) {
            logger.error("美团门店更新营业时间请求异常: {}", result, e);
            return null;
        }

        return result;
    }

    /*菜品服务*/


    public DataTable getMtCategoryByCategoryId(ISqlAdapter adapter, String categoryId) throws SQLException {
        SqlCommand cmd = new SqlCommand(ProductSqlConst.SQL_SELECT_PRODUCT_CATEGORY_MEITUAN);
        cmd.appendWhere("category_id=:categoryId");
        cmd.setParameter("categoryId", categoryId);
        DataTable dt = adapter.query(cmd);
        return dt;
    }

    /**
     * 依据门店id获取美团门店认证令牌
     *
     * @param shopId
     * @return
     */
    private String getMtShopToken(String shopId, String tenantId, String appId) {
        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);
        if (adapter == null) {
            logger.error("当前登录租户(tenantId={}, appId={})数据库连接不存在", tenantId, appId);
            return null;
        }

        SqlCommand cmd = new SqlCommand(MeituanSqlConst.SQL_SELECT_ACCOUNT_SHOP);
        DataTable dt = null;
        try {
            cmd.setParameter("shopId", shopId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("获取美团门店token时数据库异常", e);
            return null;
        } finally {
            logger.warn("获取美团门店token后，关闭数据库连接");
            adapter.closeQuiet();
        }

        if (dt == null || dt.size() == 0) {
            return null;
        }

        return dt.get(0, "token");
    }

    /**
     * 依据门店id获取美团门店认证令牌
     *
     * @param shopId
     * @return
     */
    private String getMtShopToken(ISqlAdapter adapter, String shopId) {
        SqlCommand cmd = new SqlCommand(MeituanSqlConst.SQL_SELECT_ACCOUNT_SHOP);
        DataTable dt = null;
        try {
            cmd.setParameter("shopId", shopId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("获取美团门店token时数据库异常", e);
            return null;
        }

        if (dt == null || dt.size() == 0) {
            return null;
        }

        return dt.get(0, "token");
    }

    private String getTokenByShopId(ISqlAdapter adapter, String shopId) throws SQLException {
        SqlCommand cmd = new SqlCommand(MeituanSqlConst.SQL_SELECT_ACCOUNT_SHOP);
        cmd.setParameter("shopId", shopId);
        DataTable dt = adapter.query(cmd);

        if (dt == null || dt.size() == 0) {
            return null;
        }

        return dt.get(0, "token");
    }

    /**
     * 如果根据门店Id为取得token返回
     *
     * @param shopId
     * @return
     */
    private JSONObject resultIfTokenNotExist(String shopId) {
        logger.warn("未取得美团门店(shopId={})认证token，请首先绑定门店获取appAuthToken", shopId);
        JSONObject result = new JSONObject();
        result.put("code", "300");
        result.put("error", "未取得门店认证token，请首先绑定门店获取appAuthToken");

        return result;
    }

    private String getShopIdByOrderId(ISqlAdapter adapter, String orderId) {
        if (StringUtils.isEmpty(orderId)) {
            return null;
        }

        if (adapter == null) {
            logger.error("数据库连接不存在");
            return null;
        }

        DataTable dt = null;
        SqlCommand cmd = new SqlCommand(MeituanSqlConst.SQL_SELECT_ORDER_MASTER);
        try {
            cmd.setParameter("orderId", orderId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("查询订单时数据库抛出异常", e);
            adapter.closeQuiet();
            return null;
        }

        if (dt == null || dt.size() == 0) {
            logger.warn("该订单不存在");
            return null;
        }

        return dt.get(0, "shop_id");

    }

    /**
     * 上传图片异步
     *
     * @param fileName
     * @param bytes
     * @param shopId
     * @param tenantId
     * @param appId
     * @return
     */
    public Map<String, String> updateProductImageAsync(String productId, String fileName, byte[] bytes, String shopId, String tenantId, String appId) {
        String token = getMtShopToken(shopId, tenantId, appId);
        if (StringUtils.isEmpty(token)) {
            logger.error("美团批量菜品操作前，未取得token");
            return null;
        }
        CipCaterTakeoutImageUploadRequest request = new CipCaterTakeoutImageUploadRequest();
        RequestSysParams requestSysParams = new RequestSysParams(MeituanInfoConst.SIGN_KEY, token);
        request.setRequestSysParams(requestSysParams);
        request.setImageName(fileName);
        request.setePoiId(shopId);
        //request.setePoiId("9525207012186");
        File file = null;
        //将图片流存入临时文件
        Map<String, String> returnMap = new HashMap<>();
        try {
            String filepath = "/" + fileName;
            file = new File(filepath);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes, 0, bytes.length);
            fos.flush();
            fos.close();
            logger.info("上传图片至美团时，生成临时文件成功");
        } catch (Exception e) {
            logger.error("将文件流存临时文件异常." + e.getMessage(), e);
            returnMap.put("id", productId);
            returnMap.put("message", "NOTOK");
            returnMap.put("errormessage", "将文件流存临时文件异常." + e.getMessage());
            return returnMap;
        }
        request.setFile(file);
        String result = null;

        //上传美团
        try {
            result = request.doRequest();
            logger.info("美团上传图片返回信息: {}", result);
            JSONObject returnjson = JSON.parseObject(result);
            if (returnjson.containsKey("error")) {
                JSONObject error = returnjson.getJSONObject("error");
                returnMap.put("id", productId);
                returnMap.put("message", "NOTOK");
                returnMap.put("errormessage", error.getString("message"));
                return returnMap;
            }
        } catch (Exception e) {
            logger.error("美团上传图片时请求异常: {}", result, e);
            JSONObject error = JSON.parseObject(result).getJSONObject("error");
            returnMap.put("id", productId);
            returnMap.put("message", "NOTOK");
            returnMap.put("errormessage", error.getString("message"));
            return returnMap;
        }
        //删除临时图片文件
        file.delete();
        if (result == null) {
            return null;
        }
        JSONObject data = JSON.parseObject(result);
        String imageId = data.getString("data");
        //保存本地
        String updateReuturn = saveMtProductImage(tenantId, appId, productId, imageId);
        if (updateReuturn == null) {
            returnMap.put("id", productId);
            returnMap.put("message", "NOTOK");
            returnMap.put("errormessage", "保存美团菜品图片信息时出现异常");
            return returnMap;
        }
        returnMap.put("id", productId);
        returnMap.put("message", "OK");
        return returnMap;
    }

    @Override
    public Object updateProductImageAsyncUser(String productId, String fileName, byte[] bytes, String userId, String tenantId, String appId) {
        List<Map<String, Object>> tokenList = getTokenList(userId, tenantId, appId);
        Map<String, String> returnMap = new HashMap<>();
        String re = null;
        for (int i = 0; i < tokenList.size(); i++) {
            Map<String, Object> map = tokenList.get(i);
            String token = (String) map.get("token");
            String shopId = (String) map.get("shopId");
            CipCaterTakeoutImageUploadRequest request = new CipCaterTakeoutImageUploadRequest();
            RequestSysParams requestSysParams = new RequestSysParams(MeituanInfoConst.SIGN_KEY, token);
            request.setRequestSysParams(requestSysParams);
            request.setImageName(fileName);
            request.setePoiId(shopId);
            //request.setePoiId("9525207012186");
            File file = null;
            //将图片流存入临时文件

            try {
                String filepath = "/" + fileName;
                file = new File(filepath);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(bytes, 0, bytes.length);
                fos.flush();
                fos.close();
                logger.info("上传图片至美团时，生成临时文件成功");
            } catch (Exception e) {
                logger.error("将文件流存临时文件异常." + e.getMessage(), e);
                returnMap.put("id", productId);
                returnMap.put("message", "NOTOK");
                returnMap.put("errormessage", "将文件流存临时文件异常." + e.getMessage());
                return returnMap;
            }
            request.setFile(file);
            String result = null;

            //上传美团
            try {
                result = request.doRequest();
                logger.info("美团上传图片返回信息: {}", result);
                JSONObject returnjson = JSON.parseObject(result);
                if (returnjson.containsKey("error")) {
                    JSONObject error = returnjson.getJSONObject("error");
                    returnMap.put("id", productId);
                    returnMap.put("message", "NOTOK");
                    returnMap.put("errormessage", error.getString("message"));
                    continue;
                }
            } catch (Exception e) {
                logger.error("美团上传图片时请求异常: {}", result, e);
                JSONObject error = JSON.parseObject(result).getJSONObject("error");
                returnMap.put("id", productId);
                returnMap.put("message", "NOTOK");
                returnMap.put("errormessage", error.getString("message"));
                continue;
            }
            //删除临时图片文件
            file.delete();
            if (result == null) {
                return null;
            }
            re = result;
            break;
        }

        JSONObject data = JSON.parseObject(re);
        String imageId = data.getString("data");
        //保存本地
        String updateReuturn = saveMtProductImage(tenantId, appId, productId, imageId);
        if (updateReuturn == null) {
            returnMap.put("id", productId);
            returnMap.put("message", "NOTOK");
            returnMap.put("errormessage", "保存美团菜品图片信息时出现异常");
            return returnMap;
        }
        returnMap.put("id", productId);
        returnMap.put("message", "OK");
        return returnMap;
    }

    public List<Map<String, Object>> getTokenList(String userId, String tenantId, String appId) {
        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);
        if (adapter == null) {
            logger.error("数据库连接不存在");
            return null;
        }
        List<Map<String, Object>> list = new LinkedList<>();
        try {
            SqlCommand cmd = new SqlCommand(AccountSqlConst.SQL_SELECT_ALL_MT_ACCOUNT_BY_USER_ID);
            cmd.setParameter("userId", userId);
            DataTable dt = adapter.query(cmd);
            if (dt == null || dt.size() <= 0) {
                return list;
            }
            dt.getRows().forEach(dataRow -> {
                Map<String, Object> item = new HashMap<>();
                item.put("shopId", dt.get(0, "shop_id"));
                item.put("token", dt.get(0, "token"));
                list.add(item);
            });
            return list;
        } catch (SQLException e) {
            adapter.rollbackQuiet();
            return list;
        } finally {
            adapter.closeQuiet();
        }
    }

    /**
     * 保存美团菜品图片信息
     *
     * @param tenantId
     * @param appId
     * @param productId
     * @param meituanId
     * @return
     */
    public String saveMtProductImage(String tenantId, String appId, String productId, String meituanId) {
        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);
        if (adapter == null) {
            logger.error("数据库连接不存在");
            return null;
        }
        try {
            SqlCommand cmd = new SqlCommand(ProductSqlConst.SQL_UPDATE_PRODUCT_IMAGE_MEITUAN);
            cmd.setParameter("productId", productId);
            cmd.setParameter("meituanId", meituanId);
            adapter.update(cmd);
        } catch (Exception e) {
            logger.error("保存美团菜品图片信息时出现异常", e);
            return null;
        } finally {
            CommonUtil.adapterCommitAndClose(adapter);
        }
        return "OK";
    }

    /**
     * 商家同意退款
     *
     * @param orderId
     * @param reason
     * @param tenantId
     * @param appId
     * @return
     */
    @Override
    public Object agreeRefund(String orderId, String reason, String tenantId, String appId) {
        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);
        if (adapter == null) {
            logger.error("数据库连接不存在");
            return null;
        }

        String shopId = getShopIdByOrderId(adapter, orderId);

        String token = getMtShopToken(shopId, tenantId, appId);
        if (StringUtils.isEmpty(token)) {
            logger.error("美团商家同意退款操作前，未取得token");
            return null;
        }

        CipCaterTakeoutOrderRefundAcceptRequest request = new CipCaterTakeoutOrderRefundAcceptRequest();
        RequestSysParams requestSysParams = new RequestSysParams(MeituanInfoConst.SIGN_KEY, token);
        request.setOrderId(Long.parseLong(orderId));
        request.setReason(reason);
        request.setRequestSysParams(requestSysParams);
        String result = null;
        try {
            result = request.doRequest();
            logger.info("美团商家同意退款返回信息: {}", result);
        } catch (Exception e) {
            logger.error("美团商家同意退款请求异常: {}", result, e);
            return null;
        } finally {
            CommonUtil.adapterCommitAndClose(adapter);
        }
        return result;
    }

    /**
     * 商家拒绝退款
     *
     * @param orderId
     * @param reason
     * @param tenantId
     * @param appId
     * @return
     */
    @Override
    public Object rejectRefund(String orderId, String reason, String tenantId, String appId) {
        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);
        if (adapter == null) {
            logger.error("数据库连接不存在");
            return null;
        }

        String shopId = getShopIdByOrderId(adapter, orderId);

        String token = getMtShopToken(shopId, tenantId, appId);
        if (StringUtils.isEmpty(token)) {
            logger.error("美团商家拒绝退款操作前，未取得token");
            return null;
        }
        CipCaterTakeoutOrderRefundRejectRequest request = new CipCaterTakeoutOrderRefundRejectRequest();
        RequestSysParams requestSysParams = new RequestSysParams(MeituanInfoConst.SIGN_KEY, token);
        request.setOrderId(Long.parseLong(orderId));
        request.setReason(reason);
        request.setRequestSysParams(requestSysParams);

        String result = null;
        try {
            result = request.doRequest();
            logger.info("美团商家拒绝退款返回信息: {}", result);
        } catch (Exception e) {
            logger.error("美团商家拒绝退款请求异常: {}", result, e);
            return null;
        } finally {
            CommonUtil.adapterCommitAndClose(adapter);
        }
        return result;
    }

    /**
     * 根据订单ID获取订单信息
     *
     * @param adapter
     * @param orderId
     * @return
     */
    @Override
    public String getOrderStatus(ISqlAdapter adapter, String orderId) {
        String shopId = getShopIdByOrderId(adapter, orderId);
        String token = getMtShopToken(adapter, shopId);
        logger.info("美团token：" + token);
        if (StringUtils.isEmpty(token)) {
            logger.error("美团根据订单ID获取订单信息操作前，未取得token");
            return null;
        }
        CipCaterTakeoutOrderQueryByIdRequest request = new CipCaterTakeoutOrderQueryByIdRequest();
        RequestSysParams requestSysParams = new RequestSysParams(MeituanInfoConst.SIGN_KEY, token);
        request.setOrderId(Long.parseLong(orderId));
        request.setRequestSysParams(requestSysParams);
        String status = null;
        String result = null;
        try {
            result = request.doRequest();
            logger.info("美团根据订单ID获取订单信息返回信息: {}", result);
            JSONObject jsonOrder = JSON.parseObject(result);
            JSONObject data = jsonOrder.getJSONObject("data");
            status = data.getString("status");
            logger.info("获取到美团订单当前状态：" + status);
        } catch (Exception e) {
            logger.error("美团根据订单ID获取订单信息请求异常: {}", result, e);
            return null;
        }
        return status;
    }

    /**
     * 获取美团门店信息
     *
     * @param adapter
     * @param shopId
     * @return
     * @throws SQLException
     */
    public Object getThirdShop(ISqlAdapter adapter, String shopId) throws SQLException {
        Map<String, Object> result = new HashMap<>();
        SqlCommand cmd = new SqlCommand(ShopSqlConst.SQL_SELECT_MT_SHOP_BY_ID);
        DataTable dt = null;
        try {
            cmd.setParameter("shopId", shopId);
            dt = adapter.query(cmd);
            if (dt != null && dt.size() != 0) {
                result = dt.getRows().get(0).toMap();
                result.put("invoice", result.get("invoicesupport"));
                result.put("deliveramount", "");//美团门店表无起送价字段，返回空
                result.remove("invoicesupport");
            }
        } catch (SQLException e) {
            logger.error("查询美团门店信息时数据库异常", e);
            throw e;
        }
        return result;
    }

    @Override
    public void test() {
    }

    @Override
    public void checkCategoryIfShelfProduct(ISqlAdapter adapter, String categoryId, String shopId) throws SQLException, HitspBizException {
        SqlCommand cmd = new SqlCommand(ProductSqlConst.SQL_SELECT_PRODUCT_IFUPLOAD_MT);
        if (cmd != null) {
            cmd.setParameter("categoryId", categoryId);
        }
        DataTable dataTable = adapter.query(cmd);
        if (dataTable != null && dataTable.size() > 0) {
            throw new HitspBizException(BizReturnCode.NotDisableError, "美团门店（" + shopId + "）已在该菜品分类上架过菜品，不可禁用");
        }
    }

    @Override
    public void deleteCategory(ISqlAdapter adapter, String categoryId) throws SQLException {
        SqlCommand cmd = new SqlCommand(ProductSqlConst.SQL_DELETE_CATEGORY_SYNC_RECORD_MT);
        cmd.setParameter("categoryId", categoryId);
        adapter.update(cmd);
    }

    @Override
    public void deletePublicCategory(ISqlAdapter adapter, String publicCategoryId) throws SQLException {
        SqlCommand cmd = new SqlCommand(ProductSqlConst.SQL_DELETE_PUBLIC_CATEGORY_SYNC_RECORD_MT);
        cmd.setParameter("publicCategoryId", publicCategoryId);
        adapter.update(cmd);
    }

    @Override
    public void deleteProduct(ISqlAdapter adapter, String productId) throws SQLException {
        SqlCommand cmd = new SqlCommand(ProductSqlConst.SQL_DELETE_PRODUCT_SYNC_RECORD_MT);
        cmd.setParameter("productId", productId);
        adapter.update(cmd);
    }

    @Override
    public void deletePublicProduct(ISqlAdapter adapter, String publicProductId) throws SQLException {
        SqlCommand cmd = new SqlCommand(ProductSqlConst.SQL_DELETE_PUBLIC_PRODUCT_MT);
        cmd.setParameter("publicProductId", publicProductId);
        adapter.update(cmd);
    }

    @Override
    public JSONArray getCategoryList(String shopId, String tenantId, String appId) throws SQLException, HitspBizException, IOException, URISyntaxException {
        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);
        if (adapter == null) {
            throw new HitspBizException(BizReturnCode.GetJdbCconnectionError, "数据库连接不存在");
        }
        String token = getTokenByShopId(adapter, shopId);
        if (StringUtils.isEmpty(token)) {
            logger.error("美团获取品类列表操作前，未取得token");
            return null;
        }
        CipCaterTakeoutDishCatListQueryRequest request = new CipCaterTakeoutDishCatListQueryRequest();
        RequestSysParams requestSysParams = new RequestSysParams(MeituanInfoConst.SIGN_KEY, token);
        request.setRequestSysParams(requestSysParams);
        String result = null;
        JSONArray reList = new JSONArray();
        try {
            result = request.doRequest();
            logger.info("美团查询菜品分类返回信息: {}", result);
            JSONObject jsonObject = JSON.parseObject(result);
            JSONArray array = jsonObject.getJSONArray("data");
            for (int i = 0; i < array.size(); i++) {
                JSONObject json = array.getJSONObject(i);
                ThirdPlatformCategory thirdPlatformCategory = new ThirdPlatformCategory();
                thirdPlatformCategory.setId(json.getString("name"));
                thirdPlatformCategory.setName(json.getString("name"));
                reList.add(thirdPlatformCategory);
            }
        } catch (Exception e) {
            adapter.rollbackQuiet();
            logger.error("美团查询菜品分类请求异常: {}", result, e);
            throw e;
        } finally {
            adapter.closeQuiet();
        }
        return reList;
    }

    @Override
    public void categoryMapping(JSONArray json, String shopId, String tenantId, String appId) throws SQLException, HitspBizException {
        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);
        if (adapter == null) {
            throw new HitspBizException(BizReturnCode.GetJdbCconnectionError, "数据库连接不存在");
        }

        try {
            for (int i = 0; i < json.size(); i++) {
                JSONObject jsonObject = json.getJSONObject(i);
                String o2oCategoryId = jsonObject.getString("o2oId");
                String thirdCategoryId = jsonObject.getString("thirdId");
                mapCategory(adapter, shopId, o2oCategoryId, thirdCategoryId);
            }
            adapter.commit();
        }catch (SQLException ex){
            adapter.rollbackQuiet();
            throw ex;
        }finally {
            adapter.closeQuiet();
        }
    }

    public void mapCategory(ISqlAdapter adapter, String shopId, String o2oId, String thirdId) throws SQLException {
        //获取O2O品类信息
        ProductCategory productCategory = getProductCategoryById(adapter, o2oId);
        //删除之前的映射关系
        deleteMtCategory(adapter, o2oId, shopId);
        //新增
        commonProductStatusUpdateService.saveProductCategoryMt(adapter, productCategory);
    }

    public void deleteMtCategory(ISqlAdapter adapter, String o2oId, String shopId) throws SQLException {
        SqlCommand cmd = new SqlCommand(ProductSqlConst.SQL_DELETE_MT_CATEGORY);
        cmd.setParameter("categoryId", o2oId);
//        cmd.setParameter("shopId", shopId);
        adapter.update(cmd);
    }

    public ProductCategory getProductCategoryById(ISqlAdapter adapter, String o2oId) throws SQLException {
        SqlCommand cmd = new SqlCommand(ProductSqlConst.SQL_SELECT_PRODUCT_CATEGORY_BY_ID);
        cmd.setParameter("categoryId", o2oId);
        DataTable dt = adapter.query(cmd);
        if (dt == null || dt.size() <= 0) {
            return null;
        }
        ProductCategory category = new ProductCategory();
        CommonUtil.convertMapToObj(dt.getRows().get(0).toMap(), category);
        return category;
    }

    @Override
    public JSONArray getProductList(String shopId, String tenantId, String appId) throws SQLException, HitspBizException, IOException, URISyntaxException {
        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);
        if (adapter == null) {
            throw new HitspBizException(BizReturnCode.GetJdbCconnectionError, "数据库连接不存在");
        }
        String token = getTokenByShopId(adapter, shopId);
        CipCaterTakeoutDishBaseQueryByEPoiIdRequest request = new CipCaterTakeoutDishBaseQueryByEPoiIdRequest();
        RequestSysParams requestSysParams = new RequestSysParams(MeituanInfoConst.SIGN_KEY, token);
        request.setRequestSysParams(requestSysParams);
        request.setePoiId(shopId);
        String result = null;
        JSONArray reList = new JSONArray();
        try {
            result = request.doRequest();
            logger.info("美团查询菜品返回信息: {}", result);
            JSONObject jsonObject = JSON.parseObject(result);
            JSONArray array = jsonObject.getJSONArray("data");
            for (int i = 0; i < array.size(); i++) {
                JSONObject json = array.getJSONObject(i);
                ThirdPlatformProduct thirdPlatformProduct = new ThirdPlatformProduct();
                thirdPlatformProduct.setId(json.getLong("dishId"));
                thirdPlatformProduct.setName(json.getString("dishName"));
                reList.add(thirdPlatformProduct);
            }
        } catch (Exception e) {
            adapter.rollbackQuiet();
            logger.error("美团查询菜品请求异常: {}", result, e);
            throw e;
        } finally {
            adapter.closeQuiet();
        }
        return reList;
    }

    @Override
    public void productMapping(JSONArray json, String shopId, String tenantId, String appId) throws SQLException, HitspBizException, IOException, URISyntaxException {
        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);
        if (adapter == null) {
            throw new HitspBizException(BizReturnCode.GetJdbCconnectionError, "数据库连接不存在");
        }
        try {
            JSONArray dishMappings = new JSONArray();
            for (int i = 0; i < json.size(); i++) {
                JSONObject jsonObject = json.getJSONObject(i);
                String o2oId = jsonObject.getString("o2oId");
                String thirdId = jsonObject.getString("thirdId");
                JSONObject dishMapping = new JSONObject();
                dishMapping.put("eDishCode", o2oId);
                dishMapping.put("dishId", thirdId);
                dishMappings.add(dishMapping);
                mapProduct(adapter, shopId, o2oId, thirdId);
            }
            String token = getTokenByShopId(adapter, shopId);
            CipCaterTakeoutDishMapRequest request = new CipCaterTakeoutDishMapRequest();
            RequestSysParams requestSysParams = new RequestSysParams(MeituanInfoConst.SIGN_KEY, token);
            request.setRequestSysParams(requestSysParams);
            request.setePoiId(shopId);
            request.setDishMappings(dishMappings.toJSONString());
            String result = request.doRequest();
            JSONObject jsonObject = JSON.parseObject(result);
            if (!StringUtils.equals(jsonObject.getString("data"), "OK")) {
                throw new HitspBizException(BizReturnCode.MtRetrunBizError, "映射美团商品失败");
            }
            adapter.commit();
        } catch (SQLException ex) {
            adapter.rollbackQuiet();
            throw ex;
        } finally {
            adapter.closeQuiet();
        }
    }

    @Override
    public JSONArray getThirdCategoryList(String parentId, String depth, String shopId, String userId, String tenantId, String appId) throws HitspBizException, SQLException {
        return null;
    }

    @Override
    public JSONArray getThirdMaterial(String shopId, String tenantId, String appId) throws HitspBizException {
        return null;
    }

    public void mapProduct(ISqlAdapter adapter, String shopId, String o2oId, String thirdId) throws SQLException {
        //获取O2O菜品信息
        Product product = getProductById(adapter, o2oId);
        //删除之前的映射关系
        deleteMtProduct(adapter, o2oId, thirdId);
        //新增
        commonProductStatusUpdateService.saveProductMt(adapter, product);
    }

    public void deleteMtProduct(ISqlAdapter adapter, String o2oId, String thirdId) throws SQLException {
        SqlCommand cmd = new SqlCommand(ProductSqlConst.SQL_DELETE_PRODUCT_MT);
        cmd.setParameter("id", o2oId);
        adapter.update(cmd);
    }

    public Product getProductById(ISqlAdapter adapter, String o2oId) throws SQLException {
        SqlCommand cmd = new SqlCommand(ProductSqlConst.SQL_SELECT_PRODUCT_BY_PRODUCT_ID);
        cmd.setParameter("productId", o2oId);
        DataTable dt = adapter.query(cmd);
        if (dt == null || dt.size() <= 0) {
            return null;
        }
        Product product = new Product();
        CommonUtil.convertMapToObj(dt.getRows().get(0).toMap(), product);
        return product;
    }

    @Override
    public JSONObject batchUploadProductCategory(String shopId, List<ProductCategory> productCategoryList, JSONObject result, String tenantId, String appId) throws HitspBizException {
        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);
        if (adapter == null) {
            throw new HitspBizException(BizReturnCode.GetJdbCconnectionError, "数据库连接不存在");
        }
        try {
            String token = getTokenByShopId(adapter, shopId);
            if (StringUtils.isEmpty(token)) {
                throw new HitspBizException(BizReturnCode.NonExistent, "门店（" + shopId + "）同步菜品分类至美团失败，获取美团门店授权失败");
            }
            for (ProductCategory productCategory : productCategoryList) {
                JSONObject re = updateProductCategory(adapter, token, shopId, productCategory);
                if (re.containsKey("error")) {
                    JSONArray error = result.getJSONArray("error");
                    error.add(re.getString("error"));
                    result.put("error", error);
                } else if (re.containsKey("success")) {
                    JSONArray success = result.getJSONArray("success");
                    success.add(re.getString("success"));
                    result.put("success", success);
                } else if (re.containsKey("warning")) {
                    JSONArray error = result.getJSONArray("warning");
                    error.add(re.getString("warning"));
                    result.put("warning", error);
                }
            }
            adapter.commit();
        } catch (Exception ex) {
            adapter.rollbackQuiet();
            logger.error("门店（" + shopId + "）同步菜品分类至美团失败，操作数据库异常");
        } finally {
            adapter.closeQuiet();
        }
        return result;
    }

    @Override
    public JSONObject uploadPublicCategory(String userId, String publicProductCategoryId, JSONObject reJson, String tenantId, String appId) throws HitspBizException {
        return null;
    }


    @Override
    public JSONObject batchUploadProduct(String shopId, List<Product> productList, JSONObject result, String tenantId, String appId) throws HitspBizException {
        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);
        if (adapter == null) {
            throw new HitspBizException(BizReturnCode.GetJdbCconnectionError, "数据库连接不存在");
        }

        try {
            String token = getTokenByShopId(adapter, shopId);
            if (StringUtils.isEmpty(token)) {
                throw new HitspBizException(BizReturnCode.NonExistent, "门店（" + shopId + "）同步菜品至美团失败，获取美团门店授权失败");
            }
            for (Product product : productList) {
                JSONObject re = updateProduct(adapter, token, shopId, product);
                if (re.containsKey("error")) {
                    JSONArray error = result.getJSONArray("error");
                    error.add(re.getString("error"));
                    result.put("error", error);
                } else if (re.containsKey("success")) {
                    JSONArray success = result.getJSONArray("success");
                    success.add(re.getString("success"));
                    result.put("success", success);
                } else if (re.containsKey("warning")) {
                    JSONArray error = result.getJSONArray("warning");
                    error.add(re.getString("warning"));
                    result.put("warning", error);
                }
            }
            adapter.commit();
        } catch (Exception ex) {
            adapter.rollbackQuiet();
            logger.error("门店（" + shopId + "）同步菜品至美团失败，操作数据库异常");
        } finally {
            adapter.closeQuiet();
        }
        return result;
    }

    @Override
    public JSONObject uploadPublic(String userId, String publicProductId, JSONObject reJson, String tenantId, String appId) throws HitspBizException {
        return null;
    }

    @Override
    public Object thirdShopMapping(String shopId, String thirdShopId, String tenantId, String appId) {
        return null;
    }

    private JSONObject updateProduct(ISqlAdapter adapter, String token, String shopId, Product product) {
        JSONObject result = new JSONObject();
        String error = null;
        try {
            //获取美团同步记录
            DataTable dt = getMtProductByProductId(adapter, String.valueOf(product.getId()));
            //同步美团
            JSONObject re = doUpdateProduct(adapter, shopId, product, token, dt);
            result = re;
        } catch (SQLException ex) {
            adapter.rollbackQuiet();
            ex.printStackTrace();
            error = "门店（" + shopId + "）同步菜品菜品（" + product.getName() + "）至美团失败，数据库操作异常";
            result.put("error", error);
        }
        return result;
    }

    private JSONArray createMtProductParams(ISqlAdapter adapter, Product product, String shopId) throws SQLException, HitspBizException {
        JSONArray dishesArray = new JSONArray();
        JSONObject dishObject = new JSONObject();
        dishObject.put("EDishCode", product.getId());
        dishObject.put("isSoldOut", product.getIsShelf() ? 0 : 1);
        dishObject.put("minOrderCount", 1);//默认值
        dishObject.put("unit", StringUtils.isNotEmpty(product.getSpec()) ? product.getSpec() : "份");
        dishObject.put("price", product.getPrice());
        dishObject.put("EpoiId", shopId);
        dishObject.put("dishName", product.getName());
        String categoryName = getMtProductCategoryNameByCategoryId(adapter, product.getCategoryId());
        if (StringUtils.isEmpty(categoryName)) {
            throw new HitspBizException(BizReturnCode.NonExistent, "菜品所属分类（" + product.getCategoryId() + "）不存在");
        }
        dishObject.put("categoryName", categoryName);
        dishObject.put("description", product.getDescription());
        String getImageProductId = null;
        if (product.getIsPublic()) {
            getImageProductId = product.getPublicProductId();
        } else {
            getImageProductId = String.valueOf(product.getId());
        }
        String imageId = getMtImageId(adapter, getImageProductId);
        if (!StringUtils.isEmpty(imageId)) {
            dishObject.put("picture", imageId);
        }
        JSONArray skusArray = new JSONArray();
        JSONObject skuObject = new JSONObject();
        skuObject.put("skuId", product.getId());
        skuObject.put("price", product.getPrice());
        skuObject.put("spec", StringUtils.isNotEmpty(product.getSpec()) ? product.getSpec() : "份");
        ProductStock productStock = product.getProductStock();
        skuObject.put("stock", productStock.getStock() == null ? "0" : String.valueOf(productStock.getStock()));
        skusArray.add(skuObject);
        dishObject.put("skus", skusArray);
        ProductPack productPack = new ProductPack();
        dishObject.put("boxNum", productPack.getBoxNum() == null ? 0 : productPack.getBoxNum());
        dishObject.put("boxPrice", productPack.getBoxPrice() == null ? 0 : productPack.getBoxPrice());
        dishesArray.add(dishObject);
        return dishesArray;
    }

    private JSONObject doUpdateProduct(ISqlAdapter adapter, String shopId, Product product, String token, DataTable dt) {
        JSONObject result = new JSONObject();
        CipCaterTakeoutDishBatchUploadRequest request = new CipCaterTakeoutDishBatchUploadRequest();
        RequestSysParams requestSysParams = new RequestSysParams(MeituanInfoConst.SIGN_KEY, token);
        request.setRequestSysParams(requestSysParams);
        request.setePoiId(shopId);
        try {
            JSONArray dishesArray = createMtProductParams(adapter, product, shopId);
            request.setDishes(dishesArray.toJSONString());
            if (dt == null || dt.size() == 0) {//新上传
                if (product.getIsValid()) {//可用
                    String strresult = request.doRequest();
                    JSONObject resultJson = JSONObject.parseObject(strresult);
                    if (StringUtils.equals(resultJson.getString("data"), "ok")) {
                        logger.info("新增菜品成功，美团返回信息:{}", JSON.toJSONString(resultJson));
                        commonProductStatusUpdateService.saveProductMt(adapter, product);
                    } else {
                        result.put("error", "门店（" + shopId + "）同步菜品（" + product.getName() + "）至美团失败，" + resultJson.getJSONObject("error").getString("message"));
                    }
                } else {//不可用
                    result.put("warning", "菜品（" + product.getName() + "）未在美团门店（" + shopId + "）启用，无需同步");
                    return result;
                }
            } else {//更新
                if (product.getIsValid()) {//可用
                    String strresult = request.doRequest();
                    JSONObject resultJson = JSONObject.parseObject(strresult);
                    if (StringUtils.equals(resultJson.getString("data"), "ok")) {
                        logger.info("更新菜品成功，美团返回信息:{}", JSON.toJSONString(resultJson));
                        commonProductStatusUpdateService.updateProductMt(adapter, product);
                    } else {
                        result.put("error", "门店（" + shopId + "）同步菜品（" + product.getName() + "）至美团失败，" + resultJson.getJSONObject("error").getString("message"));
                    }
                } else {
                    CipCaterTakeoutDishDeleteRequest deleteRequest = new CipCaterTakeoutDishDeleteRequest();
                    RequestSysParams deleteRequestSysParams = new RequestSysParams(MeituanInfoConst.SIGN_KEY, token);
                    deleteRequest.setRequestSysParams(deleteRequestSysParams);
                    deleteRequest.seteDishCode(String.valueOf(product.getId()));
                    request.setePoiId(shopId);
                    String strresult = deleteRequest.doRequest();
                    JSONObject resultJson = JSONObject.parseObject(strresult);
                    if (StringUtils.equals(resultJson.getString("data"), "ok")) {
                        logger.info("删除菜品成功，美团返回信息:{}", JSON.toJSONString(resultJson));
                        commonProductStatusUpdateService.removeProductMt(adapter, product);
                    } else {
                        result.put("error", "门店（" + shopId + "）同步菜品（" + product.getName() + "）至美团失败，" + resultJson.getJSONObject("error").getString("message"));
                    }

                }
            }

        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            result.put("error", "门店（" + shopId + "）同步菜品（" + product.getName() + "）至美团失败，" + e.getMessage());
            return result;
        } catch (SQLException e) {
            logger.error("请求数据库异常", e);
            result.put("error", "门店（" + shopId + "）同步菜品（" + product.getName() + "）至美团失败，操作数据库异常");
            return result;
        } catch (HitspBizException e) {
            result.put("error", "门店（" + shopId + "）同步菜品（" + product.getName() + "）至美团失败，" + e.getMessage());
            return result;
        }
        result.put("success", "门店（" + shopId + "）同步菜品（" + product.getName() + "）至美团成功");
        return result;

    }

    private JSONObject updateProductCategory(ISqlAdapter adapter, String token, String shopId, ProductCategory productCategory) {
        JSONObject result = new JSONObject();
        String error = null;
        try {
            //获取本地美团同步记录
            DataTable dt = getMtCategoryByCategoryId(adapter, String.valueOf(productCategory.getId()));
            //同步美团
            JSONObject re = doUpdateProductCategory(adapter, shopId, productCategory, token, dt);
            result = re;
        } catch (SQLException ex) {
            adapter.rollbackQuiet();
            ex.printStackTrace();
            error = "门店（" + shopId + "）同步菜品分类（" + productCategory.getName() + "）至美团失败，数据库操作异常";
            result.put("error", error);
        }

        return result;
    }

    private JSONObject doUpdateProductCategory(ISqlAdapter adapter, String shopId, ProductCategory productCategory, String token, DataTable dt) throws SQLException {
        JSONObject result = new JSONObject();
        CipCaterTakeoutDishCatUpdateRequest request = new CipCaterTakeoutDishCatUpdateRequest();
        RequestSysParams requestSysParams = new RequestSysParams(MeituanInfoConst.SIGN_KEY, token);
        request.setRequestSysParams(requestSysParams);
        try {
            request.setCatName(productCategory.getName());
            logger.info("CatName={}", request.getCatName());
            request.setSequence(productCategory.getSequence());
            if (dt == null || dt.size() == 0) {
                if (!productCategory.getIsValid()) {
                    result.put("warning", "菜品分类（" + productCategory.getName() + "）未在美团门店（" + shopId + "）上架，无需同步");
                    return result;
                } else {
                    //新上传
                    String strresult = request.doRequest();
                    JSONObject resultJson = JSONObject.parseObject(strresult);
                    if (StringUtils.equals(resultJson.getString("data"), "ok")) {
                        logger.info("新增菜品分类成功，美团返回信息:{}", JSON.toJSONString(resultJson));
                        commonProductStatusUpdateService.saveProductCategoryMt(adapter, productCategory);
                    } else {
                        result.put("error", "门店（" + shopId + "）同步菜品分类（" + productCategory.getName() + "）至美团失败，" + resultJson.getJSONObject("error").getString("message"));
                    }
                }
            } else {
                //更新上传
                String oldCatName = getMtProductCategoryNameByCategoryId(adapter, productCategory.getId());
                if (!productCategory.getIsValid()) {
                    CipCaterTakeoutDishCatDeleteRequest deleteRequest = new CipCaterTakeoutDishCatDeleteRequest();
                    deleteRequest.setRequestSysParams(requestSysParams);
                    deleteRequest.setCatName(oldCatName);
                    String strresult = request.doRequest();
                    JSONObject resultJson = JSONObject.parseObject(strresult);
                    if (StringUtils.equals(resultJson.getString("data"), "ok")) {
                        logger.info("删除菜品分类成功，美团返回信息：{}", JSON.toJSONString(resultJson));
                        commonProductStatusUpdateService.removeProductCategoryMt(adapter, productCategory);
                    } else {
                        result.put("error", "门店（" + shopId + "）同步菜品分类（" + productCategory.getName() + "）至美团失败，" + resultJson.getJSONObject("error").getString("message"));
                    }
                } else {
                    request.setOldCatName(oldCatName);
                    String strresult = request.doRequest();
                    JSONObject resultJson = JSONObject.parseObject(strresult);
                    if (StringUtils.equals(resultJson.getString("data"), "ok")) {
                        logger.info("更新菜品分类成功，美团返回信息:{}", JSON.toJSONString(resultJson));
                        commonProductStatusUpdateService.updateProductCategoryMt(adapter, productCategory);
                    } else {
                        result.put("error", "门店（" + shopId + "）同步菜品分类（" + productCategory.getName() + "）至美团失败，" + resultJson.getJSONObject("error").getString("message"));
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            result.put("error", "门店（" + shopId + "）同步菜品分类（" + productCategory.getName() + "）至美团失败，" + e.getMessage());
            return result;
        }
        result.put("success", "门店（" + shopId + "）同步菜品分类（" + productCategory.getName() + "）至美团成功");
        return result;
    }

    private String getMtProductCategoryNameByCategoryId(ISqlAdapter adapter, long categoryId) throws SQLException {
        SqlCommand cmd = new SqlCommand(MeituanSqlConst.SQL_SELECT_PRODUCT_CATEGERY_MT);
        cmd.setParameter("categoryId", categoryId);
        DataTable dt = adapter.query(cmd);
        if (dt == null || dt.size() == 0) {
            return null;
        }
        return dt.get(0, "name");
    }

    public DataTable getMtProductByProductId(ISqlAdapter adapter, String productId) throws SQLException {
        SqlCommand cmd = new SqlCommand(ProductSqlConst.SQL_SELECET_MT_PRODUCT_BY_PRODUCT_ID);
        cmd.appendWhere("id=:productId");
        cmd.setParameter("productId", productId);
        DataTable dt = adapter.query(cmd);
        return dt;
    }

    public String getMtImageId(ISqlAdapter adapter, String productId) throws SQLException {
        SqlCommand select_image = new SqlCommand(ProductSqlConst.SQL_SELECT_MTID_BY_PRODUCTERPID);
        select_image.setParameter("productId", productId);
        DataTable imagedatatable = adapter.query(select_image);
        if (imagedatatable == null || imagedatatable.size() <= 0) {
            return null;
        }
        return imagedatatable.get(0, "mt_id");
    }
}
