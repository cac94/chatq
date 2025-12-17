package kr.chatq.server.chatq_server.service;

import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import kr.chatq.server.chatq_server.config.CompanyContext;

@Service
public class DbService {

    private static final Logger logger = LoggerFactory.getLogger(DbService.class);
    private JdbcTemplate jdbcTemplate;

    public DbService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // chatqauth 테이블에서 auth 칼럼이 auth인 것을 조회
    public List<Map<String, Object>> getAuthTables(String auth) {
        String company = CompanyContext.getCompany();
        String sql = "SELECT chatqauth.*,(SELECT max(table_alias) FROM chatqtable WHERE table_nm = chatqauth.table_nm AND company = ?) AS table_alias FROM chatqauth WHERE company = ? AND auth = ?";
        logger.info("Executing SQL: {} with params: [{}, {}, {}]", sql, company, company, auth);
        System.out.println(
                "[DbService] Executing SQL: " + sql + " with params: [" + company + ", " + company + ", " + auth + "]");
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                row.put(columnName, rs.getObject(columnName));
            }
            return row; // Fixed: was returning inside loop in original? No, original was fine.
        }, company, company, auth);
    }

    // chatqtable 테이블에서 table 정보 조회
    public List<Map<String, Object>> getTables(String auth, String metaYn) {
        if (auth == null || auth.isEmpty()) {
            return List.of();
        }

        String company = CompanyContext.getCompany();
        String sql = "SELECT a.* FROM chatqtable a INNER JOIN chatqauth b ON (a.table_nm = b.table_nm) WHERE a.company = ? AND b.company = ? AND b.auth = ? and a.meta_yn = ?";
        logger.info("Executing SQL: {} with params: [{}, {}, {}, {}]", sql, company, company, auth, metaYn);
        System.out.println("[DbService] Executing SQL: " + sql + " with params: [" + company + ", " + company + ", "
                + auth + ", " + metaYn + "]");

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                row.put(columnName, rs.getObject(columnName));
            }
            return row;
        }, company, company, auth, metaYn);
    }

    // chatqcolumn 테이블에서 table_nm 칼럼이 tableNm인 것을 조회
    public List<Map<String, Object>> getColumns(String tableNm, int level) {
        String company = CompanyContext.getCompany();
        String sql = "SELECT * FROM chatqcolumn WHERE company = ? AND table_nm = ? and level >= ? order by column_order";
        logger.info("Executing SQL: {} with params: [{}, {}, {}]", sql, company, tableNm, level);
        System.out.println("[DbService] Executing SQL: " + sql + " with params: [" + company + ", " + tableNm + ", "
                + level + "]");
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                row.put(columnName, rs.getObject(columnName));
            }
            return row;
        }, company, tableNm, level);
    }

    // chatquser 테이블에서 user, password 칼럼이 user, password인 것을 조회
    public List<Map<String, Object>> getUsers(String user) {
        String company = CompanyContext.getCompany();
        String sql = "SELECT * FROM chatquser WHERE company = ? AND user = ?";
        logger.info("Executing SQL: {} with params: [{}, {}]", sql, company, user);
        System.out.println("[DbService] Executing SQL: " + sql + " with params: [" + company + ", " + user + "]");
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                row.put(columnName, rs.getObject(columnName));
            }
            return row;
        }, company, user);
    }
}
