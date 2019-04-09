package com.hisense.hitsp.report.web;

import com.hisense.dustms.annotation.DustMapping;
import com.hisense.dustms.common.DustMsException;
import com.hisense.hitsp.annotation.BizReturn;
import com.hisense.hitsp.report.service.RptDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by yanglei on 2017-08-10.
 */
@RestController
@RequestMapping("/o2o/rpt")
@CrossOrigin
public class RptDataResource {

    @Autowired
    RptDataService rptDataService;

    /**
     * 获取聚合数据接口
     *
     * @param tenantId
     */
    @BizReturn
    @DustMapping(value = "/totaldata")
    public Object getTotalData(String tenantId, String userId) throws DustMsException {
        return rptDataService.getTotalData(tenantId, userId);
    }

    /**
     * 获取菜品分析数据接口
     *
     * @param tenantId
     * @param rankby
     * @param platform
     * @param shopId
     */
    @BizReturn
    @DustMapping(value = "/products")
    public Object getProductsData(String tenantId, String userId, String rankby, String platform, String shopId) throws DustMsException {
        return rptDataService.getProductsData(tenantId, userId, rankby, platform, shopId);
    }
}
