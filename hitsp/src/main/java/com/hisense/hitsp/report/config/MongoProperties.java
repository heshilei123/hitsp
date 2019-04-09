package com.hisense.hitsp.report.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by yanglei on 2017-08-03.
 */
@ConfigurationProperties("spring.data.mongodb")
@Component
public class MongoProperties {
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String database;
//    private Integer dbcount;

    public MongoProperties() {
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

//    public Integer getDbcount() {
//        return dbcount;
//    }
//
//    public void setDbcount(Integer dbcount) {
//        this.dbcount = dbcount;
//    }
}
