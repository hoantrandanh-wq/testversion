package com.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import org.sqlite.SQLiteDataSource;

@Configuration
public class SQLiteConfig {

    @Bean
    public DataSource dataSource() {
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + System.getProperty("user.home") + "/.helloworld-app/data.db");
        return ds;
    }
}