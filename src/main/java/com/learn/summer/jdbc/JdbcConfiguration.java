package com.learn.summer.jdbc;

import com.learn.summer.annotation.Autowired;
import com.learn.summer.annotation.Bean;
import com.learn.summer.annotation.Configuration;
import com.learn.summer.annotation.Value;
import com.learn.summer.jdbc.tx.DataSourceTransactionManager;
import com.learn.summer.jdbc.tx.PlatformTransactionManager;
import com.learn.summer.jdbc.tx.TransactionBeanPostProcessor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

@Configuration
public class JdbcConfiguration {
    @Bean(destroyMethod = "close")
    DataSource dataSource(
            @Value("${summer.datasource.url}") String url,
            @Value("${summer.datasource.username}") String username,
            @Value("${summer.datasource.password}") String password,
            @Value("${summer.datasource.driver-class-name:}") String driver,
            @Value("${summer.datasource.maximum-pool-size:20}") int maximumPoolSize,
            @Value("${summer.datasource.minimum-pool-size:1}") int minimumPoolSize,
            @Value("${summer.datasource.connection-timeout:30000}") int connTimeout
    ) {
        var config = new HikariConfig();
        config.setAutoCommit(false);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        if(driver != null)
            config.setDriverClassName(driver);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumPoolSize);
        config.setConnectionTimeout(connTimeout);
        return new HikariDataSource(config);
    }

    @Bean
    JdbcTemplate jdbcTemplate(@Autowired DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    @Bean
    TransactionBeanPostProcessor transactionBeanPostProcessor() {
        return new TransactionBeanPostProcessor();
    }

    @Bean
    PlatformTransactionManager platformTransactionManager(@Autowired DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
