package kr.chatq.server.chatq_server.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.secondary.jdbc-url}")
    private String defaultJdbcUrl;

    @Value("${spring.datasource.secondary.username}")
    private String defaultUsername;

    @Value("${spring.datasource.secondary.password}")
    private String defaultPassword;

    @Value("${spring.datasource.secondary.driver-class-name}")
    private String defaultDriverClassName;

    /**
     * Primary DataSource - 기본 데이터소스 (mysrm 사용자)
     */
    @Bean(name = "primaryDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Primary JdbcTemplate
     */
    @Bean(name = "jdbcTemplate")
    @Primary
    public JdbcTemplate primaryJdbcTemplate(@Qualifier("primaryDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * Secondary DataSource - 보조 데이터소스 (chatcompany 테이블에서 조회)
     */
    @Bean(name = "secondaryDataSource")
    @DependsOn("primaryDataSource")
    public DataSource secondaryDataSource(@Qualifier("primaryDataSource") DataSource primaryDataSource) {
        String targetCompany = "chatq"; // Default company
        // TODO: 향후 Request Context 등에서 동적으로 company를 가져오려면 AbstractRoutingDataSource 도입
        // 필요

        logger.info("Initializing Secondary DataSource for company: {}", targetCompany);

        String jdbcUrl = null;
        String username = null;
        String password = null;
        String driverClassName = null;

        try (Connection conn = primaryDataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "SELECT jdbc_url, username, password, driver_class_name FROM chatcompany WHERE company = ?")) {

            pstmt.setString(1, targetCompany);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    jdbcUrl = rs.getString("jdbc_url");
                    username = rs.getString("username");
                    password = rs.getString("password");
                    driverClassName = rs.getString("driver_class_name");
                    logger.info("Loaded DataSource config from chatcompany table for '{}'", targetCompany);
                } else {
                    logger.warn("No configuration found in chatcompany table for '{}'. using fallback.", targetCompany);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load DataSource config from chatcompany table. Using fallback.", e);
        }

        if (jdbcUrl == null || username == null || password == null || driverClassName == null) {
            logger.info("Using fallback (application.properties) for Secondary DataSource");
            jdbcUrl = defaultJdbcUrl;
            username = defaultUsername;
            password = defaultPassword;
            driverClassName = defaultDriverClassName;
        }

        return DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .driverClassName(driverClassName)
                .build();
    }

    /**
     * Secondary JdbcTemplate
     */
    @Bean(name = "secondaryJdbcTemplate")
    public JdbcTemplate secondaryJdbcTemplate(@Qualifier("secondaryDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
