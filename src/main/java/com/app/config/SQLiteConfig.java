package com.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;

@Configuration
public class SQLiteConfig {

    static {
        try {
            Class.forName("org.sqlite.JDBC"); // 🔥 load driver tại đây
            System.out.println("✅ SQLite Driver loaded");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("❌ Cannot load SQLite JDBC Driver", e);
        }
    }

    @Bean
    public DataSource dataSource() {
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + System.getProperty("user.home") + "/.helloworld-app/data.db");
        return ds;
    }
}
