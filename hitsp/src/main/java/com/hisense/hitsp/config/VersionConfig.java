package com.hisense.hitsp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by hsl on 2018/10/11.
 */
@Component
@ConfigurationProperties(prefix = "version")
public class VersionConfig {

    private String versionId;

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }
}
