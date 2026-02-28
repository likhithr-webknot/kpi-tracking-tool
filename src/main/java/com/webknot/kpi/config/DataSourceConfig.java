package com.webknot.kpi.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    private final Environment env;

    public DataSourceConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl(env.getProperty("spring.datasource.url"));
        config.setUsername(env.getProperty("spring.datasource.username"));
        config.setPassword(env.getProperty("spring.datasource.password"));
        config.setDriverClassName(env.getProperty("spring.datasource.driver-class-name", "org.postgresql.Driver"));
        
        config.setMaximumPoolSize(env.getProperty("spring.datasource.hikari.maximum-pool-size", Integer.class, 20));
        config.setMinimumIdle(env.getProperty("spring.datasource.hikari.minimum-idle", Integer.class, 5));
        
        config.setConnectionTimeout(env.getProperty("spring.datasource.hikari.connection-timeout", Long.class, 30000L));
        config.setIdleTimeout(env.getProperty("spring.datasource.hikari.idle-timeout", Long.class, 600000L));
        config.setMaxLifetime(env.getProperty("spring.datasource.hikari.max-lifetime", Long.class, 1800000L));
        
        config.setConnectionTestQuery("SELECT 1");
        config.setAutoCommit(true);
        
        long leakThresholdMs = env.getProperty("spring.datasource.hikari.leak-detection-threshold", Long.class, 0L);
        if (leakThresholdMs > 0) {
            config.setLeakDetectionThreshold(leakThresholdMs);
        }
        
        config.setPoolName("KpiApplicationPool");
        
        return new HikariDataSource(config);
    }
}
