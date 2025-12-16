package kr.chatq.server.chatq_server.config;

import javax.sql.DataSource;

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
     * Secondary DataSource - 보조 데이터소스 (chatqcomp 테이블에서 조회)
     */
    @Bean(name = "secondaryDataSource")
    @DependsOn({ "primaryDataSource", "jdbcTemplate" })
    public DataSource secondaryDataSource(@Qualifier("jdbcTemplate") JdbcTemplate primaryJdbcTemplate) {

        // Default fallback config builder (using properties) is needed
        // IF we want the "default" to be based on application.properties values.
        // But DynamicDataSource logic uses these fields as fallback.

        DataSource defaultDataSource = DataSourceBuilder.create()
                .url(defaultJdbcUrl)
                .username(defaultUsername)
                .password(defaultPassword)
                .driverClassName(defaultDriverClassName)
                .build();

        return new DynamicDataSource(primaryJdbcTemplate, defaultDataSource);
    }

    /**
     * Secondary JdbcTemplate
     */
    @Bean(name = "secondaryJdbcTemplate")
    public JdbcTemplate secondaryJdbcTemplate(@Qualifier("secondaryDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
