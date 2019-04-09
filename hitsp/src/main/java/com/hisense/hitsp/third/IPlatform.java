package com.hisense.hitsp.third;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hisense.dustdb.sql.ISqlAdapter;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;

import com.hisense.hitsp.common.HitspBizException;
import com.hisense.hitsp.model.*;
import eleme.openapi.sdk.api.exception.ServiceException;

/**
 * 外卖平台业务接口
 *
 * @author huangshengtao
 *         date:2017-3-17.
 */
public interface IPlatform {

    /**
     * 门店绑定
     *
     * @param shop
     * @return
     */
    Object bindShop(String shop, String thirdId, String tenantId, String appId) throws HitspBizException, SQLException;

    /**
     * 门店解绑
     *
     * @param shop
     * @return
     */
    Object unbindShop(String shop, String tenantId, String appId) throws HitspBizException, SQLException;

    /**
     * 门店营业
     *
     * @param shop 门店Id
     * @return
     */
    Object openShop(String shop, String tenantId, String appId) throws HitspBizException, SQLException;

    /**
     * 门店歇业
     *
     * @param shop
     * @return
     */
    Object closeShop(String shop, String tenantId, String appId) throws HitspBizException, SQLException;

    /**
     * 门店营业时间
     *
     * @param shop
     * @param sellingTime json串
     * @return
     */
    Object updateShopOpenTime(String shop, String sellingTime, String tenantId, String appId) throws HitspBizException, SQLException;

    /**
     * 第三方门店映射
     *
     * @param shopId
     * @param thirdShopId
     * @param tenantId
     * @param appId
     * @return
     */
    Object thirdShopMapping(String shopId, String thirdShopId, String tenantId, String appId);

    /**
     * 获取第三方门店信息
     *
     * @param adapter
     * @param shopId
     * @return
     * @throws SQLException
     */
    Object getThirdShop(ISqlAdapter adapter, String shopId) throws SQLException;

    /**
     * 上传商品图片
     *
     * @param erpProductId
     * @param fileName
     * @param bytes
     * @param shopId
     * @param tenantId
     * @param appId
     * @return
     */
    Object updateProductImageAsync(String erpProductId, String fileName, byte[] bytes, String shopId, String tenantId, String appId);

    /**
     * 上传商品图片
     *
     * @param erpProductId
     * @param fileName
     * @param bytes
     * @param userId
     * @param tenantId
     * @param appId
     * @return
     */
    Object updateProductImageAsyncUser(String erpProductId, String fileName, byte[] bytes, String userId, String tenantId, String appId);

    /**
     * 批量菜品分类同步（包括新上传、更新上传、删除）（单店）
     *
     * @param shopId
     * @param productCategoryList
     * @param result
     * @param tenantId
     * @param appId
     * @return
     * @throws HitspBizException
     */
    JSONObject batchUploadProductCategory(String shopId, List<ProductCategory> productCategoryList, JSONObject result, String tenantId, String appId) throws HitspBizException;

    /**
     * 批量菜品同步（包括新上传、更新、删除）（单店）
     *
     * @param shopId
     * @param productList
     * @param result
     * @param tenantId
     * @param appId
     * @return
     * @throws HitspBizException
     */
    JSONObject batchUploadProduct(String shopId, List<Product> productList, JSONObject result, String tenantId, String appId) throws HitspBizException;

    /**
     * 强制公共管理的平台上传菜品分类（公共）
     *
     * @param userId
     * @param publicProductCategoryId
     * @param reJson
     * @param tenantId
     * @param appId
     * @return
     * @throws HitspBizException
     */
    JSONObject uploadPublicCategory(String userId, String publicProductCategoryId, JSONObject reJson, String tenantId, String appId) throws HitspBizException;

    /**
     * 强制公共管理的平台上传菜品（公共）
     *
     * @param userId
     * @param publicProductId
     * @param reJson
     * @param tenantId
     * @param appId
     * @return
     * @throws HitspBizException
     */
    JSONObject uploadPublic(String userId, String publicProductId, JSONObject reJson, String tenantId, String appId) throws HitspBizException;

    /**
     * 删除单店品类
     *
     * @param adapter
     * @param categoryId
     * @throws SQLException
     */
    void deleteCategory(ISqlAdapter adapter, String categoryId) throws SQLException;

    /**
     * 删除公共品类
     *
     * @param adapter
     * @param publicCategoryId
     * @throws SQLException
     */
    void deletePublicCategory(ISqlAdapter adapter, String publicCategoryId) throws SQLException;

    /**
     * 删除单店菜品
     *
     * @param adapter
     * @param productId
     * @throws SQLException
     */
    void deleteProduct(ISqlAdapter adapter, String productId) throws SQLException;

    /**
     * 删除公共菜品
     *
     * @param adapter
     * @param publicProductId
     * @throws SQLException
     */
    void deletePublicProduct(ISqlAdapter adapter, String publicProductId) throws SQLException;

    /**
     * 获取已有菜品分类列表
     *
     * @param shopId
     * @return
     * @throws SQLException
     */
    JSONArray getCategoryList(String shopId, String tenantId, String appId) throws SQLException, HitspBizException, IOException, URISyntaxException;

    /**
     * 菜品分类映射
     *
     * @param json
     * @param shopId
     * @throws SQLException
     */
    void categoryMapping(JSONArray json, String shopId, String tenantId, String appId) throws SQLException, HitspBizException;

    /**
     * 获取已有菜品列表
     *
     * @param shopId
     * @return
     * @throws SQLException
     */
    JSONArray getProductList(String shopId, String tenantId, String appId) throws SQLException, HitspBizException, IOException, URISyntaxException;

    /**
     * 菜品映射
     *
     * @param json
     * @param shopId
     * @throws SQLException
     */
    void productMapping(JSONArray json, String shopId, String tenantId, String appId) throws SQLException, HitspBizException, IOException, URISyntaxException;

    /**
     * 获取类目
     * @param parentId
     * @param depth
     * @param shopId
     * @param userId
     * @param tenantId
     * @param appId
     * @return
     * @throws HitspBizException
     * @throws SQLException
     */
    JSONArray getThirdCategoryList(String parentId, String depth, String shopId, String userId, String tenantId, String appId) throws HitspBizException, SQLException;

    JSONArray getThirdMaterial(String shopId,String tenantId,String appId) throws HitspBizException, SQLException;

    /**
     * 获取订单信息
     *
     * @param orderId
     * @return
     */
    Object getOrderInfo(String orderId, String dbName, String source) throws HitspBizException, SQLException;

    //TODO 未知方法
    void checkCategoryIfShelfProduct(ISqlAdapter adapter, String categoryId, String shopId) throws SQLException, HitspBizException;

    /**
     * 确认订单
     *
     * @param orderId
     * @return
     */
    Object confirm(String orderId, String tenantId, String appId) throws HitspBizException, SQLException;

    /**
     * 取消订单
     *
     * @param orderId
     * @param reason
     * @return
     */
    Object cancel(String orderId, String reason, String tenantId, String appId) throws HitspBizException, SQLException;

    /**
     * 新配送订单
     *
     * @param order
     * @param shipperName
     * @param shipperPhone
     * @param tenantId
     * @param appId
     * @return
     */
    Object newShip(Order order, String shipperName, String shipperPhone, String tenantId, String appId) throws HitspBizException, SQLException;

    /**
     * 订单配送完成
     *
     * @param orderId
     * @return
     */
    Object shipComplete(String orderId, String shopPhone, String tenantId, String appId) throws HitspBizException, SQLException;

    /**
     * 获取订单状态
     *
     * @param adapter
     * @param orderId
     * @return
     */
    Object getOrderStatus(ISqlAdapter adapter, String orderId);

    /**
     * 商家同意退款
     *
     * @param orderId
     * @param reason
     * @param tenantId
     * @param appId
     * @return
     */
    Object agreeRefund(String orderId, String reason, String tenantId, String appId) throws HitspBizException, SQLException;

    /**
     * 商家拒绝退款
     *
     * @param orderId
     * @param reason
     * @param tenantId
     * @param appId
     * @return
     */
    Object rejectRefund(String orderId, String reason, String tenantId, String appId) throws HitspBizException, SQLException;

    void test();


}
