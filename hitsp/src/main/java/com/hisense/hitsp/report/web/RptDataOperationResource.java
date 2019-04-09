package com.hisense.hitsp.report.web;

import com.hisense.dustms.annotation.DustMapping;
import com.hisense.hitsp.annotation.BizReturn;
import com.hisense.hitsp.report.service.RptDataOperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 聚合报表数据接口
 *
 * @author yanglei
 * date: 2017-08-02.
 */
@RestController
@RequestMapping("/o2o/rpt/tenant")
@CrossOrigin
public class RptDataOperationResource {
    @Autowired
    RptDataOperationService rptDataOperationService;

    /**
     * 获取租户统计数据接口
     */
    @BizReturn
    @DustMapping(value = "/totaldata")
    public Object getTenantTotalData() {
        return rptDataOperationService.getTenantTotalData();
    }

    /**
     * 获取租户-门店结构数据接口
     */
    @BizReturn
    @DustMapping(value = "/shop")
    public Object getTenantShopInfo() {
        return rptDataOperationService.getTenantShopInfo();
    }

    /**
     * 获取销售概览数据接口
     */
    @BizReturn
    @DustMapping(value = "/sale")
    public Object getTenantSaleInfo(String tenantId, String shopId, String dateRange) {
        return rptDataOperationService.getTenantSaleInfo(tenantId, shopId, dateRange);
    }

}
