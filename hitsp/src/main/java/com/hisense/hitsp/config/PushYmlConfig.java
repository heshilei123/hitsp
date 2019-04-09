package com.hisense.hitsp.config;

import com.hisense.dustdb.DustDbProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by yanglei on 2017-05-18.
 */
@Component
@ConfigurationProperties(prefix = "push")
public class PushYmlConfig {

    private String xiaoweiUrl;
    private String xiaoweiUrlStatus;

    @Autowired
    DustDbProperties dustDbProperties;

    @PostConstruct
    public void init() {
        dustDbProperties.getDbList().forEach(dataSourceContext -> System.out.println(dataSourceContext.toString()));
    }

    public String getXiaoweiUrl() {
        return xiaoweiUrl;
    }

    public void setXiaoweiUrl(String xiaoweiUrl) {
        this.xiaoweiUrl = xiaoweiUrl;
    }

    public String getXiaoweiUrlStatus() {
        return xiaoweiUrlStatus;
    }

    public void setXiaoweiUrlStatus(String xiaoweiUrlStatus) {
        this.xiaoweiUrlStatus = xiaoweiUrlStatus;
    }
}
