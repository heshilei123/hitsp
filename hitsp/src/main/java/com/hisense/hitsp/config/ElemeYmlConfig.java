package com.hisense.hitsp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by yanglei on 2017-05-12.
 */
@Component
@ConfigurationProperties(prefix = "eleme")
public class ElemeYmlConfig {

    private Boolean isSandbox;
    private String key;
    private String secret;
    private String callbackUrl;

    public Boolean getIsSandbox() {
        return isSandbox;
    }

    public void setIsSandbox(Boolean sandbox) {
        isSandbox = sandbox;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }
}
