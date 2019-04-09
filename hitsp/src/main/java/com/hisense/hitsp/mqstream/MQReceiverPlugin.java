package com.hisense.hitsp.mqstream;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.hisense.dustdb.TenantAdapterManager;
import com.hisense.dustdb.sql.ISqlAdapter;
import com.hisense.dustms.stream.IPlugin;
import com.hisense.hitsp.common.*;
import com.hisense.hitsp.model.Order;
import com.hisense.hitsp.model.Shop;
import com.hisense.hitsp.service.OrderViewService;
import com.hisense.hitsp.service.ShopViewService;
import com.hisense.hitsp.service.pruduct.ProductStatusService;
import com.hisense.hitsp.third.IPlatform;
import com.hisense.hitsp.third.PlatFormFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * MQ消息处理plugin
 *
 * @author yanglei
 *         date: 2017-04-14.
 */
@Component
public class MQReceiverPlugin implements IPlugin {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    TenantAdapterManager tenantAdapterManager;
    @Autowired
    OrderViewService orderViewService;
    @Autowired
    ShopViewService shopViewService;
    @Autowired
    ProductStatusService productStatusService;

    @Autowired
    @Qualifier("taskExecutor")
    Executor executor;

    public MQReceiverPlugin() {
    }

    @Override
    public void run(Object data) {
        logger.info("MQ接收消息处理开始：data={}", data.toString());

        executor.execute(new Runnable() {
            @Override
            public void run() {
                String operationType = ((JSONObject) data).getString("do");
                String orderId = null;
                Order order = null;
                String tenantId = ((JSONObject) data).getString("tenantId");
                String appId = ((JSONObject) data).getString("appId");

                if (((JSONObject) data).containsKey("orderId")) {
                    orderId = ((JSONObject) data).getString("orderId");
                    try {
                        order = (Order) orderViewService.getOrderDetailById(orderId, tenantId, appId);
                    } catch (HitspException e) {
                        logger.error("查询订单时异常", e);
                        return;
                    }
                }
                String recordId = null;
                if (((JSONObject) data).containsKey("recordId")) {
                    recordId = ((JSONObject) data).getString("recordId");
                }

                String categoryId = null;
                if (((JSONObject) data).containsKey("categoryId")) {
                    categoryId = ((JSONObject) data).getString("categoryId");
                }

                String shopId = null;
                if (((JSONObject) data).containsKey("shopId")) {
                    shopId = ((JSONObject) data).getString("shopId");
                }

                String publicProductId = null;
                if (((JSONObject) data).containsKey("publicProductId")) {
                    publicProductId = ((JSONObject) data).getString("publicProductId");
                }

                String publicProduct = null;
                if (((JSONObject) data).containsKey("publicProduct")) {
                    publicProduct = ((JSONObject) data).getString("publicProduct");
                }

                String type = null;
                if (((JSONObject) data).containsKey("type")) {
                    type = ((JSONObject) data).getString("type");
                }

                String publicCategoryId = null;
                if (((JSONObject) data).containsKey("publicCategoryId")) {
                    publicCategoryId = ((JSONObject) data).getString("publicCategoryId");
                }

                String publicCategory = null;
                if (((JSONObject) data).containsKey("publicCategory")) {
                    publicCategory = ((JSONObject) data).getString("publicCategory");
                }

                JSONArray categoryList = null;
                if (((JSONObject) data).containsKey("categoryList")) {
                    categoryList = ((JSONObject) data).getJSONArray("categoryList");
                }

                JSONArray publicCategoryList = null;
                if (((JSONObject) data).containsKey("publicCategoryList")) {
                    publicCategoryList = ((JSONObject) data).getJSONArray("publicCategoryList");
                }

                JSONArray productList = null;
                if (((JSONObject) data).containsKey("productList")) {
                    productList = ((JSONObject) data).getJSONArray("productList");
                }

                JSONArray publicProductList = null;
                if (((JSONObject) data).containsKey("publicProductList")) {
                    publicProductList = ((JSONObject) data).getJSONArray("publicProductList");
                }

                JSONArray productCategoryIdList = null;
                if (((JSONObject) data).containsKey("productCategoryIdList")) {
                    productCategoryIdList = ((JSONObject) data).getJSONArray("productCategoryIdList");
                }

                JSONArray productIdList = null;
                if (((JSONObject) data).containsKey("productIdList")) {
                    productIdList = ((JSONObject) data).getJSONArray("productIdList");
                }

                String userId = null;
                if (((JSONObject) data).containsKey("userId")) {
                    userId = ((JSONObject) data).getString("userId");
                }

                ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);
                if (adapter == null) {
                    logger.error("该数据库连接不存在");
                    return;
                }
                try {
                    switch (operationType) {
                        case "confirm":
                            logger.info("MQ plugin订单确认完成");
                            break;
                        case "cancel":
                            logger.info("MQ plugin订单取消完成");
                            break;
                        case "ship":
                            logger.info("MQ plugin发送订单配送状态完成");
                            break;
                        case "shipComplete":
                            logger.info("MQ plugin发送订单完成配送状态完成");
                            break;
                        case "open":
                            logger.info("MQ plugin门店置营业完成");
                            break;
                        case "close":
                            logger.info("MQ plugin门店置歇业完成");
                            break;
                        case "sellTime":
                            logger.info("MQ plugin门店更新营业时间完成");
                            break;
                        case "updateProduct":
                            break;
                        case "removeProduct":
                            break;
                        case "updateCategory":
                            break;
                        case "removeCategory":
                            break;
                        case "updateProductStock":
                            logger.info("MQ plugin更新菜品库存开始");
                            logger.info("MQ plugin更新菜品库存完成");
                            break;
                        case "agreeRefund":
                            logger.info("MQ plugin商家同意退款完成");
                            break;
                        case "rejectRefund":
                            logger.info("MQ plugin商家拒绝退款完成");
                            break;
                        case "syncProductToShopProduct":
                            productStatusService.syncProductToShopProduct(publicProductId, publicProduct, type, recordId, tenantId, appId);
                            break;
                        case "syncCategoryToShopCategory":
                            productStatusService.syncCategoryToShopCategory(publicCategoryId, publicCategory, type, recordId, tenantId, appId);
                            break;
                        case "batchUploadProductCategory":
                            productStatusService.syncBatchUploadProductCategoryService(recordId, shopId, categoryList, tenantId, appId);
                            break;
                        case "batchUploadPublicProductCategory":
                            productStatusService.syncBatchUploadPublicProductCategory(recordId, userId, publicCategoryList, tenantId, appId);
                            break;
                        case "batchUploadProduct":
                            productStatusService.syncBatchUploadProductProductService(recordId, shopId, productList, tenantId, appId);
                            break;
                        case "batchUploadPublicProduct":
                            productStatusService.syncBatchUploadPublicProduct(recordId, userId, publicProductList, tenantId, appId);
                            break;
                        case "batchExtractPublicCategory":
                            productStatusService.syncBatchExtractPublicCategory(recordId, userId, shopId, productCategoryIdList, tenantId, appId);
                            break;
                        default:
                            break;
                    }

                } catch (Exception e) {
                    logger.error("MQ处理异常", e);
                    adapter.closeQuiet();
                    return;
                }

                logger.info("adapterCommitted");
                CommonUtil.adapterCommitAndClose(adapter);
            }
        });
    }
}
