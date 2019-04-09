package com.hisense.hitsp.third.common;

import com.hisense.dustdb.sql.DataTable;
import com.hisense.dustdb.sql.ISqlAdapter;
import com.hisense.dustdb.sql.SqlCommand;
import com.hisense.hitsp.model.Order;
import com.hisense.hitsp.model.OrderDetail;
import com.hisense.hitsp.service.OrderSqlConst;
import com.hisense.hitsp.third.meituan.service.MeituanSqlConst;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.List;

/**
 * 第三方平台公共业务处理
 *
 * @author yanglei
 *         date: 2017-04-20.
 */
public class ThirdPlatformCommonUtil {
    private static final Logger logger = LoggerFactory.getLogger(ThirdPlatformCommonUtil.class);

    public static void saveOrderMaster04(ISqlAdapter adapter, Order order) {
        SqlCommand cmd = new SqlCommand(OrderSqlConst.SQL_INSERT_MASTER_04);
        try {
            cmd.setParameter("id", order.getId());
            cmd.setParameter("gmtCreate", order.getGmtCreate());
            cmd.setParameter("gmtModify", order.getGmtModify());
            cmd.setParameter("deliverTime", order.getDeliverTime());
            cmd.setParameter("orderId", order.getOrderId());
            cmd.setParameter("orderFrom", order.getOrderFrom());
            cmd.setParameter("totalPrice", order.getTotalPrice());
            cmd.setParameter("originalPrice", order.getOriginalPrice());
            cmd.setParameter("status", order.getStatus());
            cmd.setParameter("shopId", order.getShopId());
            cmd.setParameter("shopName", order.getShopName());
            cmd.setParameter("shopAddress", order.getShopAddress());
            cmd.setParameter("shopPhone", order.getShopPhone());
            cmd.setParameter("recipientName", order.getRecipientName());
            cmd.setParameter("recipientAddress", order.getRecipientAddress());
            cmd.setParameter("recipientPhone", order.getRecipientPhone());
            cmd.setParameter("recipientLongitude", order.getRecipientLongitude());
            cmd.setParameter("recipientLatitude", order.getRecipientLatitude());
            cmd.setParameter("shippingType", order.getShippingType());
            cmd.setParameter("shippingFee", order.getShippingFee());
            cmd.setParameter("shipperName", order.getShipperName());
            cmd.setParameter("shipperPhone", order.getShipperPhone());
            cmd.setParameter("hasInvoiced", order.getHasInvoiced());
            //TODO 测试布尔值
            cmd.setParameter("invoiceTitle", order.getInvoiceTitle());
            cmd.setParameter("packageFee", order.getPackageFee());
            cmd.setParameter("payType", order.getPayType());
            cmd.setParameter("caution", order.getCaution());
            cmd.setParameter("remark", order.getRemark());
            cmd.setParameter("shopPart", order.getShopPart());
            cmd.setParameter("serviceFee", order.getServiceFee());
            cmd.setParameter("shopIncome", order.getShopIncome());
        } catch (SQLException e) {
            logger.error("保存订单时参数类型不匹配", e);
            adapter.closeQuiet();
            return;
        }

        try {
            adapter.update(cmd);
        } catch (SQLException e) {
            logger.error("保存订单主表数据时异常", e);
            adapter.closeQuiet();
            return;
        }
    }

    /**
     * 将order数据保存到订单明细表中
     *
     * @param order
     */
    public static void saveOrderDetail(ISqlAdapter adapter, Order order) {
        SqlCommand sqlDetailCommand = new SqlCommand(OrderSqlConst.SQL_INSERT_DETAIL);
        List<OrderDetail> orderDetailList = order.getDetail();
        if (orderDetailList == null || orderDetailList.size() == 0) {
            return;
        }

        for (int i = 0; i < orderDetailList.size(); i++) {
            if (i != 0) {
                sqlDetailCommand.next();
            }
            OrderDetail orderDetail = orderDetailList.get(i);
            Field[] fields = orderDetail.getClass().getDeclaredFields();
            for (int j = 0; j < fields.length; j++) {
                String name = fields[j].getName();
                if (StringUtils.equals("orderId", name)) {
                    try {
                        sqlDetailCommand.setParameter(name, order.getId());
                    } catch (SQLException e) {
                        logger.error("保存订单明细表时参数类型设置错误", e);
                        adapter.closeQuiet();
                        return;
                    }
                } else {
                    if (!StringUtils.equals("id", name) && !StringUtils.equals("gmtCreate", name)
                            && !StringUtils.equals("gmtModify", name)) {
                        try {
                            try {
                                fields[j].setAccessible(true);
                                sqlDetailCommand.setParameter(name, fields[j].get(orderDetail));
                            } catch (SQLException e) {
                                logger.error("保存订单明细表时参数类型设置错误", e);
                                adapter.closeQuiet();
                                return;
                            }
                        } catch (IllegalAccessException e) {
                            logger.error("反射异常", e);
                            return;
                        }
                    }
                }

            }
        }
        try {
            adapter.update(sqlDetailCommand);
        } catch (SQLException e) {
            logger.error("保存订单明细表时更新数据异常", e);
            adapter.closeQuiet();
        }
    }

    public static Order orderToErpOrder(ISqlAdapter adapter, Order order) {
        logger.info("查询erp门店是否存在");
        DataTable dtErpShop = getErpShopInfoByShopId(adapter, order.getShopId());
        if (dtErpShop == null || dtErpShop.size() == 0) {
            logger.error("门店({})的erp门店信息不存在！", order.getShopId());
            return null;
        }

        order.setShopId(dtErpShop.get(0, "erp_id"));
        order.setShopName(dtErpShop.get(0, "name"));

        List<OrderDetail> orderDetailList = order.getDetail();
        if (orderDetailList != null) {
            for (int i = 0; i < orderDetailList.size(); i++) {
                OrderDetail orderDetail = orderDetailList.get(i);

                String productId = orderDetail.getCode();
                DataTable dtErpProduct = getErpProductInfoByProductId(adapter, productId);
                if (dtErpProduct != null && dtErpProduct.size() != 0) {
                    orderDetail.setCode(dtErpProduct.get(0, "erp_id"));
                    orderDetail.setName(dtErpProduct.get(0, "name"));
                    orderDetail.setCategoryId(dtErpProduct.get(0, "categoryId"));
                    orderDetail.setUnitWeight(dtErpProduct.get(0, "unitWeight"));
                } else {
                    logger.error("菜品({})的erp菜品信息不存在！", productId);
                    return null;
                }

            }
        }

        order.setDetail(orderDetailList);
        return order;
    }


    private static DataTable getErpShopInfoByShopId(ISqlAdapter adapter, String shopId) {

        DataTable dt = null;
        try {
            SqlCommand cmd = new SqlCommand(MeituanSqlConst.SQL_SELECT_SHOP_ERP_BY_SHOPID);
            cmd.setParameter("shopId", shopId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("根据门店Id获取erp门店信息时数据库抛出异常", e);
            adapter.closeQuiet();
            return null;
        }

        return dt;
    }

    private static DataTable getErpProductInfoByProductId(ISqlAdapter adapter, String productId) {
        DataTable dt = null;
        try {
            SqlCommand cmd = new SqlCommand(MeituanSqlConst.SQL_SELECT_PRODUCT_ERP_BY_ID);
            cmd.setParameter("productId", productId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("根据菜品Id获取erp菜品信息时数据库抛出异常", e);
            return null;
        }

        return dt;
    }

    public static DataTable getErpProductSpecInfoByProductId(ISqlAdapter adapter, String productId, String erpShopId) {
        DataTable dt = null;
        try {
            SqlCommand cmd = new SqlCommand(MeituanSqlConst.SQL_SELECT_PRODUCT_ERP_SPEC_BY_ID);
            cmd.setParameter("productId", productId);
            cmd.setParameter("erpShopId", erpShopId);
            dt = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("根据菜品Id获取erp菜品信息时数据库抛出异常", e);
            return null;
        }

        return dt;
    }

}
