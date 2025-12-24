package kr.chatq.server.chatq_server.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;

public class DynamicDataSource extends AbstractDataSource {

    private static final Logger logger = LoggerFactory.getLogger(DynamicDataSource.class);

    private final JdbcTemplate primaryJdbcTemplate;
    private final DataSource defaultDataSource;

    // Cache for company-specific DataSources
    private final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();

    public DynamicDataSource(JdbcTemplate primaryJdbcTemplate, DataSource defaultDataSource) {
        this.primaryJdbcTemplate = primaryJdbcTemplate;
        this.defaultDataSource = defaultDataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return determineTargetDataSource().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return determineTargetDataSource().getConnection(username, password);
    }

    private DataSource determineTargetDataSource() {
        String company = CompanyContext.getCompany();
        if (company == null || company.isEmpty()) {
            company = "chatq";
        }

        // Return default instantly if chatq (and assuming defaultDataSource IS the
        // chatq one)
        // However, usually we might want to ensure "chatq" also goes through the map or
        // uses default directly.
        // Let's rely on map caching.

        return dataSourceMap.computeIfAbsent(company, this::createDataSourceForCompany);
    }

    private DataSource createDataSourceForCompany(String company) {
        logger.info("Creating DataSource for company: {}", company);

        String jdbcUrl = null;
        String username = null;
        String password = null;
        String driverClassName = null;

        try {
            // Check DB for config
            Map<String, Object> config = primaryJdbcTemplate.queryForMap(
                    "SELECT jdbc_url, user_nm, password, driver_class_name FROM chatqcomp WHERE company = ?",
                    company);

            jdbcUrl = (String) config.get("jdbc_url");
            username = (String) config.get("user_nm");
            password = (String) config.get("password");
            driverClassName = (String) config.get("driver_class_name");

            logger.info("Loaded DataSource config from chatqcomp table for '{}'", company);
        } catch (Exception e) {
            // Failed to find or error
            logger.warn(
                    "No configuration found in chatqcomp table for '{}' or error occurred. Using default fallback.",
                    company);
            if (!"chatq".equals(company)) {
                // If not chatq and failed, maybe we should return defaultDataSource?
                // Or try to load 'chatq' config?
                // For now, return defaultDataSource (fallback)
                return defaultDataSource;
            }
        }

        if (jdbcUrl != null && username != null && password != null && driverClassName != null) {
            return DataSourceBuilder.create()
                    .url(jdbcUrl)
                    .username(username)
                    .password(password)
                    .driverClassName(driverClassName)
                    .build();
        }

        return defaultDataSource;
    }
}
