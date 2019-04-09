package com.hisense.hitsp.aop;

import com.alibaba.fastjson.JSONObject;
import com.hisense.dustms.aop.web.WebAspect;
import com.hisense.hitsp.common.AppContext;
import com.hisense.hitsp.common.HitspBizException;
import com.hisense.hitsp.config.VersionConfig;
import com.hisense.hitsp.model.BizReturnCode;
import com.hisense.hitsp.model.WebApiResultData;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Created by user on 2018/2/1.
 */
@Component
@Aspect
@Order(4)
public class O2oAspect {
    final static Logger logger = LoggerFactory.getLogger(O2oAspect.class);

    @Pointcut("@annotation(com.hisense.hitsp.annotation.BizReturn) && within(com.hisense..*)")
    public void o2oPointcut() {
    }

    @Around("o2oPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            Object result = joinPoint.proceed();
            WebApiResultData webApiResultData = new WebApiResultData();
            if (result == null) {
                webApiResultData.defaultNonDataSuccess();
            } else {
                webApiResultData.defaultSuccess(result);
            }
            AppContext aContext = new AppContext();
            VersionConfig versionConfig = (VersionConfig) aContext.getBean("versionConfig");
            webApiResultData.setVersion(versionConfig.getVersionId());
            return webApiResultData;
        } catch (SQLException ex) {
            ex.printStackTrace();
            WebApiResultData webApiResultData = new WebApiResultData();
            webApiResultData.setBizIsSucceed(false);
            webApiResultData.setBizReturnCode(BizReturnCode.DbError);
            webApiResultData.setBizReturnMsg("数据库操作异常");
            AppContext aContext = new AppContext();
            VersionConfig versionConfig = (VersionConfig) aContext.getBean("versionConfig");
            webApiResultData.setVersion(versionConfig.getVersionId());
            return webApiResultData;
        } catch (HitspBizException ex) {
            WebApiResultData webApiResultData = new WebApiResultData();
            webApiResultData.setBizIsSucceed(false);
            webApiResultData.setBizReturnCode(ex.getErrorCode());
            webApiResultData.setBizReturnMsg(ex.getMessage());
            webApiResultData.setBizReturnData(ex.getReturnData());
            AppContext aContext = new AppContext();
            VersionConfig versionConfig = (VersionConfig) aContext.getBean("versionConfig");
            webApiResultData.setVersion(versionConfig.getVersionId());
            return webApiResultData;
        }
    }
}
