package com.hisense.hitsp.config;

import com.hisense.dustdb.DustDbProperties;
import com.hisense.hitsp.common.HitspException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.sql.*;

/**
 * Created by hsl on 2017/8/11.
 */
@Component
@ConfigurationProperties(prefix = "database")
public class DatabaseYmlConfig {

    private Integer databaseCount;

    @Autowired
    DustDbProperties dustDbProperties;

    @Value("${druid.url}")
    private String url;

    @Value("${druid.username}")
    private String username;

    @Value("${druid.password}")
    private String password;

    @PostConstruct
    public void init() {
        dustDbProperties.getDbList().forEach(dataSourceContext -> System.out.println(dataSourceContext.toString()));
    }

    public Integer getDatabaseCount() throws HitspException {
        try {
            Connection connection = DriverManager.getConnection(url, username, password);
            Statement stat = connection.createStatement();
            ResultSet rs = stat.executeQuery("SELECT COUNT(*) FROM information_schema.SCHEMATA where schema_name like 'hitsp%'");
            int rowCount = 0;
            if(rs.next())
            {
                rowCount = rs.getInt(1);
            }
            connection.close();

            return rowCount;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new HitspException("获取DatabaseCount出错");
        }
    }

    public void setDatabaseCount(Integer databaseCount) {
        this.databaseCount = databaseCount;
    }

}
