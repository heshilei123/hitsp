package com.hisense.hitsp.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.data.annotation.Transient;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Model类，该类fastjson化返回订单查询的全部信息
 *
 * @author yanglei
 * @date 2017-03-14.
 * @see #orderFrom 枚举值包括
 * 10-京东到家,11-百度外卖,12-美团外卖,13-大众点评,14-口碑外卖,15-饿了么,16-到家美食会
 * @see #status 枚举值包括
 * 0-已下单，1-已确认，2-配送中，3-已送达，4-已完成，5-无效，6-退单中
 * @see #shippingType 枚举值包括
 * 10-京东到家配送,11-百度外卖配送,12-美团外卖配送,13-大众点评配送,14-口碑外卖配送,15-饿了么配送,16-美食会配送,17-蜂鸟快递配送,18-达达快递配送,19-派乐趣配送,99-其他配送
 * @see #payType 枚举值包括
 * 1-货到付款,2-在线支付
 */
public class Order {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Date gmtCreate;
    @Transient
    private Date gmtModify;
    private String orderId;
    private String orderIdView;
    private String orderFrom;
    private Date deliverTime;
    private BigDecimal totalPrice;
    private BigDecimal originalPrice;
    private String status;
    private String shopId;
    private String shopName;
    private String shopAddress;
    private String shopPhone;
    private String recipientName;
    private String recipientAddress;
    private String recipientPhone;

    private BigDecimal recipientLongitude;
    private BigDecimal recipientLatitude;
    private String shippingType;
    private BigDecimal shippingFee;
    private String shipperName;
    private String shipperPhone;
    private Boolean hasInvoiced;
    private String invoiceTitle;
    private BigDecimal packageFee;
    private String payType;
    private String caution;
    private String remark;
    private BigDecimal shopPart;
    private BigDecimal serviceFee;
    private BigDecimal shopIncome;
    private String daySn;
    private List<OrderDetail> detail;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getGmtModify() {
        return gmtModify;
    }

    public void setGmtModify(Date gmtModify) {
        this.gmtModify = gmtModify;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderIdView() {
        return orderIdView;
    }

    public void setOrderIdView(String orderIdView) {
        this.orderIdView = orderIdView;
    }

    public String getOrderFrom() {
        return orderFrom;
    }

    public void setOrderFrom(String orderFrom) {
        this.orderFrom = orderFrom;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    public Date getDeliverTime() {
        return deliverTime;
    }

    public void setDeliverTime(Date deliverTime) {
        this.deliverTime = deliverTime;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getShopAddress() {
        return shopAddress;
    }

    public void setShopAddress(String shopAddress) {
        this.shopAddress = shopAddress;
    }

    public String getShopPhone() {
        return shopPhone;
    }

    public void setShopPhone(String shopPhone) {
        this.shopPhone = shopPhone;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientAddress() {
        return recipientAddress;
    }

    public void setRecipientAddress(String recipientAddress) {
        this.recipientAddress = recipientAddress;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public BigDecimal getRecipientLongitude() {
        return recipientLongitude;
    }

    public void setRecipientLongitude(BigDecimal recipientLongitude) {
        this.recipientLongitude = recipientLongitude;
    }

    public BigDecimal getRecipientLatitude() {
        return recipientLatitude;
    }

    public void setRecipientLatitude(BigDecimal recipientLatitude) {
        this.recipientLatitude = recipientLatitude;
    }

    public String getShippingType() {
        return shippingType;
    }

    public void setShippingType(String shippingType) {
        this.shippingType = shippingType;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(BigDecimal shippingFee) {
        this.shippingFee = shippingFee;
    }

    public String getShipperName() {
        return shipperName;
    }

    public void setShipperName(String shipperName) {
        this.shipperName = shipperName;
    }

    public Boolean getHasInvoiced() {
        return hasInvoiced;
    }

    public void setHasInvoiced(Boolean hasInvoiced) {
        this.hasInvoiced = hasInvoiced;
    }

    public String getShipperPhone() {
        return shipperPhone;
    }

    public void setShipperPhone(String shipperPhone) {
        this.shipperPhone = shipperPhone;
    }

    public String getInvoiceTitle() {
        return invoiceTitle;
    }

    public void setInvoiceTitle(String invoiceTitle) {
        this.invoiceTitle = invoiceTitle;
    }

    public BigDecimal getPackageFee() {
        return packageFee;
    }

    public void setPackageFee(BigDecimal packageFee) {
        this.packageFee = packageFee;
    }

    public String getPayType() {
        return payType;
    }

    public void setPayType(String payType) {
        this.payType = payType;
    }

    public String getCaution() {
        return caution;
    }

    public void setCaution(String caution) {
        this.caution = caution;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public BigDecimal getShopPart() {
        return shopPart;
    }

    public void setShopPart(BigDecimal shopPart) {
        this.shopPart = shopPart;
    }

    public BigDecimal getServiceFee() {
        return serviceFee;
    }

    public void setServiceFee(BigDecimal serviceFee) {
        this.serviceFee = serviceFee;
    }

    public BigDecimal getShopIncome() {
        return shopIncome;
    }

    public void setShopIncome(BigDecimal shopIncome) {
        this.shopIncome = shopIncome;
    }

    public String getDaySn() {
        return daySn;
    }

    public void setDaySn(String daySn) {
        this.daySn = daySn;
    }

    public List<OrderDetail> getDetail() {
        return detail;
    }

    public void setDetail(List<OrderDetail> detail) {
        this.detail = detail;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }

}
