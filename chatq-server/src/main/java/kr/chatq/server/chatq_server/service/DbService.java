package kr.chatq.server.chatq_server.service;

import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DbService {

    private JdbcTemplate jdbcTemplate;

    public DbService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // chatqauth 테이블에서 auth 칼럼이 auth인 것을 조회
    public List<Map<String, Object>> getAuthTables(String auth) {
        String sql = "SELECT * FROM chatqauth WHERE auth = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                row.put(columnName, rs.getObject(columnName));
            }
            return row;
        }, auth);
    }

    // chatqtable 테이블에서 table 정보 조회
    public List<Map<String, Object>> getTables(String auth, String metaYn) {
        if (auth == null || auth.isEmpty()) {
            return List.of();
        }
        
        String sql = "SELECT a.* FROM chatqtable a INNER JOIN chatqauth b ON (a.db_nm = b.db_nm AND a.table_nm = b.table_nm) WHERE b.auth = ? and a.meta_yn = ?";
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                row.put(columnName, rs.getObject(columnName));
            }
            return row;
        }, auth, metaYn);
    }

    // chatqauth 테이블에서 table_nm 칼럼이 tableNm인 것을 조회
    public List<Map<String, Object>> getColumns(String dbNm, String tableNm) {
        String sql = "SELECT * FROM chatqcolumn WHERE db_nm = ? AND table_nm = ? order by column_order";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                row.put(columnName, rs.getObject(columnName));
            }
            return row;
        }, dbNm, tableNm);
    }
}
