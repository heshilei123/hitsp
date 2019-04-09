package com.hisense.hitsp.third.meituan;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hisense.dustcore.util.Converter;
import com.sankuai.sjst.platform.developer.domain.RequestSysParams;
import com.sankuai.sjst.platform.developer.request.CipCaterTakeoutOrderQueryByIdRequest;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by huangshengtao on 2017-3-17.
 */
public class MeituanSdkSample {

    RequestSysParams requestSysParams = new RequestSysParams(MeituanInfoConst.SIGN_KEY,
                                                                MeituanInfoConst.AUTH_TOKEN);

    public void queryOrderById(String id) throws IOException, URISyntaxException {
        CipCaterTakeoutOrderQueryByIdRequest request = new CipCaterTakeoutOrderQueryByIdRequest();
        request.setRequestSysParams(requestSysParams);
        request.setOrderId(Converter.toLong(id));
        String jsonResult = request.doRequest();
        JSONObject json = JSON.parseObject(jsonResult);
        //TODO 处理返回值
    }

}
