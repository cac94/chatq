package kr.chatq.server.chatq_server.service;

import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;

import kr.chatq.server.chatq_server.config.CompanyContext;

@Service
public class DbService {
    private static final String ENC_SEED = "chatq!2015";

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
        String company = CompanyContext.getCompany();
        String sql;
        Object[] params;

        StringBuilder sb = new StringBuilder();
        if (auth == null || auth.isEmpty()) {
            // auth가 비어있으면 모든 auth에 대해 조회
            sb.append("SELECT company, table_nm, table_alias, detail_yn, meta_yn, tail_query, change_date, change_time, change_user, keywords, ");
            sb.append("CONVERT(AES_DECRYPT(table_metab, '" + ENC_SEED + "') USING utf8mb4) AS table_query ");
            sb.append(" FROM chatqtable WHERE company = ? and meta_yn = ?");
            sql = sb.toString();
            params = new Object[]{company, metaYn};
            logger.info("Executing SQL: {} with params: [{}, {}, {}]", sql, company, company, metaYn);
            System.out.println("[DbService] Executing SQL: " + sql + " with params: [" + company + ", " + company + ", "
                    + metaYn + "]");
        } else {
            // auth가 있으면 특정 auth만 조회
            sb.append("SELECT a.company, a.table_nm, a.table_alias, a.detail_yn, a.meta_yn, a.tail_query, a.change_date, a.change_time, a.change_user, a.keywords, ");
            sb.append("CONVERT(AES_DECRYPT(a.table_metab, '" + ENC_SEED + "') USING utf8mb4) AS table_query ");
            sb.append("FROM chatqtable a INNER JOIN chatqauth b ON (a.table_nm = b.table_nm) WHERE a.company = ? AND b.company = ? AND b.auth = ? and a.meta_yn = ?");
            sql = sb.toString();
            params = new Object[]{company, company, auth, metaYn};
            logger.info("Executing SQL: {} with params: [{}, {}, {}, {}]", sql, company, company, auth, metaYn);
            System.out.println("[DbService] Executing SQL: " + sql + " with params: [" + company + ", " + company + ", "
                    + auth + ", " + metaYn + "]");
        }

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                row.put(columnName, rs.getObject(columnName));
            }
            return row;
        }, params);
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
