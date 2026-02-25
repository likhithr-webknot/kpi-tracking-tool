package com.webknot.kpi.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * HikariCP DataSource Configuration
 * 
 * Optimizes database connection pooling to prevent connection exhaustion.
 * Pool size and timeouts can be configured via environment variables:
 * - DB_POOL_SIZE (default: 20)
 * - DB_POOL_MIN_IDLE (default: 5)
 * - DB_POOL_CONNECTION_TIMEOUT (default: 30000ms)
 * - DB_POOL_IDLE_TIMEOUT (default: 600000ms = 10 minutes)
 * - DB_POOL_MAX_LIFETIME (default: 1800000ms = 30 minutes)
 */
@Configuration
public class DataSourceConfig {

    private final Environment env;

    public DataSourceConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        // Basic connection properties
        config.setJdbcUrl(env.getProperty("spring.datasource.url"));
        config.setUsername(env.getProperty("spring.datasource.username"));
        config.setPassword(env.getProperty("spring.datasource.password"));
        config.setDriverClassName(env.getProperty("spring.datasource.driver-class-name", "org.postgresql.Driver"));
        
        // Connection pool sizing
        config.setMaximumPoolSize(env.getProperty("spring.datasource.hikari.maximum-pool-size", Integer.class, 20));
        config.setMinimumIdle(env.getProperty("spring.datasource.hikari.minimum-idle", Integer.class, 5));
        
        // Timeout configurations (in milliseconds)
        config.setConnectionTimeout(env.getProperty("spring.datasource.hikari.connection-timeout", Long.class, 30000L));
        config.setIdleTimeout(env.getProperty("spring.datasource.hikari.idle-timeout", Long.class, 600000L)); // 10 minutes
        config.setMaxLifetime(env.getProperty("spring.datasource.hikari.max-lifetime", Long.class, 1800000L)); // 30 minutes
        
        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setAutoCommit(true);
        
        // Connection initialization and leak detection
        config.setLeakDetectionThreshold(60000); // 60 seconds - logs warnings for connections held longer
        
        // Pool name for monitoring
        config.setPoolName("KpiApplicationPool");
        
        return new HikariDataSource(config);
    }
}
