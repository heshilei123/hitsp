package com.hisense.hitsp.service;

import com.google.common.collect.Lists;
import com.hisense.dustcore.util.CamelNameUtils;
import com.hisense.dustdb.TenantAdapterManager;
import com.hisense.dustdb.sql.DataRow;
import com.hisense.dustdb.sql.DataTable;
import com.hisense.dustdb.sql.ISqlAdapter;
import com.hisense.dustdb.sql.SqlCommand;
import com.hisense.hitsp.common.CommonUtil;
import com.hisense.hitsp.common.HitspException;
import com.hisense.hitsp.model.Order;
import com.hisense.hitsp.model.OrderDetail;
import com.hisense.hitsp.third.common.ThirdPlatformCommonUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.*;

/**
 * 订单查询业务
 *
 * @author yanglei
 */
@Service
public class OrderViewService {
    private final Logger logger = LoggerFactory.getLogger(OrderViewService.class);

    @Autowired
    TenantAdapterManager tenantAdapterManager;

    /**
     * 查询订单详情服务
     *
     * @param orderId
     * @return 当orderId为空值或空字符串时，返回全部未确认订单; 否则，返回Order实体类
     */
    public Object getOrderDetailById(String orderId, String tenantId, String appId) throws HitspException {
        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);
        if (adapter == null) {
            logger.error("数据库连接不存在");
            return null;
        }

        Object order = getOrderDetailById(orderId, adapter);
        adapter.closeQuiet();

        return order;
    }

    public Order getOrderDetailById(String orderId, ISqlAdapter adapter) throws HitspException {

        DataTable dtMaster = null;
        DataTable dtDetail = null;
        try {
            dtMaster = queryOrderById(adapter, orderId, OrderSqlConst.SQL_SELECT_MASTER);
            dtDetail = queryOrderById(adapter, orderId, OrderSqlConst.SQL_SELECT_DETAIL);
        } catch (SQLException e) {
            logger.error("查询订单时数据库抛出异常", e);
            throw new HitspException("查询订单时数据库抛出异常", e, adapter);
        }

        if (dtMaster == null || dtMaster.size() == 0) {
            logger.warn("该订单不存在");
            return null;
        }

        DataRow rowMaster = dtMaster.getRows().get(0);
        Map<String, Object> mapMaster = rowMaster.toMap();
        List<DataRow> listRows = dtDetail.getRows();
        List<OrderDetail> listDetail = rows2OrderDetailList(listRows);
        Order order = new Order();
        CommonUtil.convertMapToObj(mapMaster, order);
        order.setDetail(listDetail);
        order = ThirdPlatformCommonUtil.orderToErpOrder(adapter, order);
        //获取流水号
        String order_id = order.getOrderId();
        String order_from = order.getOrderFrom();
        String daySn = getDaySnByOrderId(adapter, order_id, order_from);
        order.setDaySn(daySn);
        String orderIdView = getOrderIdViewByOrderId(adapter, order_id, order_from);
        order.setOrderIdView(orderIdView);
        return order;
    }

    public String getOrderIdViewByOrderId(ISqlAdapter adapter, String orderId, String orderFrom) throws HitspException {
        if (StringUtils.equals(orderFrom, "12")) {
            String sql = OrderSqlConst.SQL_SELECT_MT_ORDERIDVIEW_BY_ORDERID;
            SqlCommand cmd = new SqlCommand(sql);
            try {
                cmd.setParameter("orderId", orderId);
                DataTable dataTable = adapter.query(cmd);
                if (dataTable.size() <= 0) {
                    logger.warn("订单{}，不存在或无orderIdView", orderId);
                    return null;
                }
                String orderIdView = dataTable.get(0, "orderIdView");
                return orderIdView;
            } catch (SQLException e) {
                logger.error("查询订单orderIdView时，数据库异常");
                throw new HitspException("查询订单orderIdView时，数据库异常", e, adapter);
            }
        } else {
            return orderId;
        }
    }

    public String getDaySnByOrderId(ISqlAdapter adapter, String orderId, String orderFrom) throws HitspException {
        String sql = "";
        if (StringUtils.equals(orderFrom, "12")) {
            sql = OrderSqlConst.SQL_SELECT_MT_DAYSN_BY_ORDERID;
        } else if (StringUtils.equals(orderFrom, "15")) {
            sql = OrderSqlConst.SQL_SELECT_ELEME_DAYSN_BY_ORDERID;
        } else if (StringUtils.equals(orderFrom, "16")) {
            sql = OrderSqlConst.SQL_SELECT_JD_DAYSN_BY_ORDERID;
        } else if (StringUtils.equals(orderFrom, "17")) {
            sql = OrderSqlConst.SQL_SELECT_EB_DAYSN_BY_ORDERID;
        }
        SqlCommand cmd = new SqlCommand(sql);
        try {
            cmd.setParameter("orderId", orderId);
            DataTable dataTable = adapter.query(cmd);
            if (dataTable.size() <= 0) {
                logger.warn("订单{}，不存在或无流水号", orderId);
                return null;
            }
            String daySn = "";
            if (StringUtils.equals(orderFrom, "12")) {
                daySn = dataTable.get(0, "daySeq");
            } else if (StringUtils.equals(orderFrom, "15")) {
                daySn = dataTable.get(0, "daySn");
            } else if (StringUtils.equals(orderFrom, "16")) {
                daySn = dataTable.get(0, "orderNum");
            } else if (StringUtils.equals(orderFrom, "17")) {
                daySn = dataTable.get(0, "order_index");
            }
            return daySn;
        } catch (SQLException e) {
            logger.error("查询订单流水号时，数据库异常");
            throw new HitspException("查询订单流水号时，数据库异常", e, adapter);
        }
    }

    public String getDaySnById(String orderId, String orderFrom, String tenantId, String appId) {
        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);
        if (adapter == null) {
            logger.error("数据库连接不存在");
            return null;
        }
        String sql = "";
        if (StringUtils.equals(orderFrom, "12")) {
            sql = OrderSqlConst.SQL_SELECT_MT_DAYSN_BY_ID;
        } else if (StringUtils.equals(orderFrom, "15")) {
            sql = OrderSqlConst.SQL_SELECT_ELEME_DAYSN_BY_ID;
        } else if (StringUtils.equals(orderFrom, "16")) {
            sql = OrderSqlConst.SQL_SELECT_JD_DAYSN_BY_ID;
        } else if (StringUtils.equals(orderFrom, "17")) {
            sql = OrderSqlConst.SQL_SELECT_EB_DAYSN_BY_ID;
        }
        SqlCommand cmd = new SqlCommand(sql);
        String daySn = "";
        try {
            cmd.setParameter("orderId", orderId);
            DataTable dataTable = adapter.query(cmd);
            if (dataTable.size() <= 0) {
                logger.warn("订单{}，不存在或无流水号", orderId);
                return null;
            }
            if (StringUtils.equals(orderFrom, "12")) {
                daySn = dataTable.get(0, "daySeq");
            } else if (StringUtils.equals(orderFrom, "15")) {
                daySn = dataTable.get(0, "daySn");
            } else if (StringUtils.equals(orderFrom, "16")) {
                daySn = dataTable.get(0, "orderNum");
            } else if (StringUtils.equals(orderFrom, "17")) {
                daySn = dataTable.get(0, "order_index");
            }
        } catch (SQLException e) {
            logger.error("查询订单流水号时，数据库异常", e);
        } finally {
            adapter.closeQuiet();
        }
        return daySn;
    }

    /**
     * 根据erp门店Id，查询全部未被确认订单列表服务
     *
     * @return 返回Order实体类的list
     */
    public Object getNoConfirmOrderListByShopId(String shopId, String tenantId, String appId) throws HitspException {
        logger.info("拉取" + shopId + "门店未确认订单开始");
        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);
        if (adapter == null) {
            logger.error("数据库连接不存在");
            return null;
        }

        List<Map<String, Object>> orderList = Lists.newArrayList();
        DataTable dtMaster = null;

        SqlCommand cmd = new SqlCommand(OrderSqlConst.SQL_SELECT_MASTER_BY_SHOPID);
        try {
            cmd.setParameter("status", "0");
            cmd.setParameter("shopId", shopId);
            dtMaster = adapter.query(cmd);
        } catch (SQLException e) {
            logger.error("根据门店id查询未确认订单时数据库错误", e);
            return null;
        } finally {
            adapter.closeQuiet();
        }

        if (dtMaster == null || dtMaster.size() == 0) {
            logger.warn("当前erp门店Id无未确认订单");
            return null;
        }

        return orderListReturn(dtMaster, orderList, tenantId, appId);
    }

    /**
     * 遍历dtMaster的所有行，并将列名转化为驼峰格式，
     * 同时将列名id转化为orderId，存放到新的订单数组orderList
     */
    private List<Map<String, Object>> orderListReturn(DataTable dtMaster, List<Map<String, Object>> orderList, String tenantId, String appId) {
        dtMaster.getRows().forEach(row -> {
            Map<String, Object> map = new TreeMap();
            Map<String, Object> mapRow = row.toMap();
            mapRow.forEach((key, value) -> {
                if ("id".equals(key)) {
                    key = "orderId";
                }
                if (key.indexOf("_") > 0) {
                    key = CamelNameUtils.underscore2camel(key);
                }

                map.put(key, value);
            });
            String orderId = String.valueOf(map.get("orderId"));
            String order_from = String.valueOf(map.get("orderFrom"));
            String daySn = getDaySnById(orderId, order_from, tenantId, appId);
            map.put("daySn", daySn);
            orderList.add(map);
        });

        return orderList;
    }

    /**
     * 查询全部未被确认订单列表服务
     * 即订单状态status=0
     *
     * @return 返回Order实体类的list
     */
    public Object getAllNoConfirmOrderList(String tenantId, String appId) throws HitspException {
        ISqlAdapter adapter = tenantAdapterManager.getAdapter(tenantId, appId);

        if (adapter == null) {
            logger.error("数据库连接不存在");
            return null;
        }

        List<Map<String, Object>> orderList = Lists.newArrayList();
        DataTable dtMaster = null;


        try {
            dtMaster = queryOrderListByStatus(adapter, OrderSqlConst.SQL_SELECT_MASTER_NOID);
        } catch (SQLException e) {
            logger.error("输入参数设置不正确", e);
            return null;
        } finally {
            adapter.closeQuiet();
        }

        if (dtMaster == null || dtMaster.size() == 0) {
            logger.warn("无未确认订单");
            return null;
        }


        return orderListReturn(dtMaster, orderList, tenantId, appId);
    }

    /**
     * 根据订单Id，查询订单信息
     *
     * @param adapter
     * @param orderId
     * @param sql
     * @return 返回DataTable对象
     * @throws SQLException sql语句语法不正确
     */
    public DataTable queryOrderById(ISqlAdapter adapter, String orderId, String sql) throws SQLException {
        SqlCommand cmd = new SqlCommand(sql);
        cmd.setParameter("orderId", orderId);
        return adapter.query(cmd);
    }

    /**
     * 根据订单状态，查询订单列表信息
     *
     * @param adapter
     * @param sql
     * @return 返回DataTable对象
     * @throws SQLException sql语句语法不正确
     */
    public DataTable queryOrderListByStatus(ISqlAdapter adapter, String sql) throws SQLException {
        SqlCommand cmd = new SqlCommand(sql);
        cmd.setParameter("status", "0");
        return adapter.query(cmd);
    }

    /**
     * 将取得的OrderDetail数据库表数据装换为{@link OrderDetail}实体类的list
     *
     * @param listRows 取得的OrderDetail数据库表数据
     * @return OrderDetail类的list
     */
    private List<OrderDetail> rows2OrderDetailList(List<DataRow> listRows) {
        List<OrderDetail> listDetail = Lists.newArrayList();
        listRows.forEach(item -> {
            OrderDetail orderDetail = new OrderDetail();
            CommonUtil.convertMapToObj(item.toMap(), orderDetail);
            listDetail.add(orderDetail);
        });

        return listDetail;
    }

    public Map getOrderIdByThirdOrderId(ISqlAdapter adapter, String thirdOrderId, String sql) throws HitspException {
        Map map = new HashMap<>();
        try {
            SqlCommand cmd = new SqlCommand(sql);
            cmd.setParameter("orderId", thirdOrderId);
            DataTable dataTable = adapter.query(cmd);
            if (dataTable.size() <= 0) {
                return null;
            }
            DataRow dataRow = dataTable.getRows().get(0);
            String id = dataRow.get("id");
            String status = dataRow.get("status");
            map.put("id", id);
            map.put("status", status);
        } catch (SQLException ex) {
            logger.error("根据第三方外卖平台订单ID查询O2O订单ID时异常", ex);
            throw new HitspException("根据门店id查询未确认订单时数据库错误", ex, adapter);
        }
        return map;
    }

    public String getOrderStatusByOrderId(ISqlAdapter adapter, String orderId) {
        SqlCommand cmd = new SqlCommand(OrderSqlConst.SQL_SELECT_STATUS_BY_ORDERID);
        try {
            cmd.setParameter("orderId", orderId);
            DataTable dataTable = adapter.query(cmd);
            if (dataTable.size() <= 0) {
                logger.error("订单ID为{}的订单不存在", orderId);
                return null;
            }
            DataRow dataRow = dataTable.getRows().get(0);
            String status = dataRow.get("status");
            return status;
        } catch (SQLException e) {
            logger.error("根据orderId查询订单状态数据库异常", e);
            adapter.closeQuiet();
            return null;
        }
    }
}
