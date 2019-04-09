package com.hisense.hitsp.report.config;

import com.mongodb.Mongo;
import com.mongodb.MongoCredential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoClientFactoryBean;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Created by yanglei on 2017-08-03.
 */
@Configuration
public class MongoDBConfig {

    @Autowired
    MongoProperties mongoProperties;

    @Bean
    public MongoClientFactoryBean mongoDbFactory(){
        MongoClientFactoryBean clientFactoryBean = new MongoClientFactoryBean();
        clientFactoryBean.setHost(mongoProperties.getHost());
        clientFactoryBean.setPort(mongoProperties.getPort());
        MongoCredential credential = MongoCredential.createScramSha1Credential(mongoProperties.getUsername(),
                mongoProperties.getDatabase(), mongoProperties.getPassword().toCharArray());
        clientFactoryBean.setCredentials(new MongoCredential[]{credential});
        return clientFactoryBean;
    }

    @Bean
    public MongoTemplate mongoTemplate(Mongo mongo){
        MongoTemplate mongoTemplate = new MongoTemplate(mongo, mongoProperties.getDatabase());
        return mongoTemplate;
    }
}
