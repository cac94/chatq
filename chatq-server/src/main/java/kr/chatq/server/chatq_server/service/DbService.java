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
    @SuppressWarnings("null")
    public List<Map<String, Object>> getTables(List<Map<String, Object>> authTables) {
        if (authTables == null || authTables.isEmpty()) {
            return List.of();
        }
        
        // IN 절용 플레이스홀더 생성 (db_nm = ? AND table_nm = ?) OR (db_nm = ? AND table_nm = ?) ...
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM chatqtable WHERE ");
        List<Object> params = new java.util.ArrayList<>();
        
        for (int i = 0; i < authTables.size(); i++) {
            if (i > 0) {
                sqlBuilder.append(" OR ");
            }
            sqlBuilder.append("(db_nm = ? AND table_nm = ?)");
            
            Map<String, Object> authTable = authTables.get(i);
            params.add(authTable.get("db_nm"));
            params.add(authTable.get("table_nm"));
        }
        
        String sql = sqlBuilder.toString();
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                row.put(columnName, rs.getObject(columnName));
            }
            return row;
        }, params.toArray());
    }

    // chatqauth 테이블에서 table_nm 칼럼이 tableNm인 것을 조회
    public List<Map<String, Object>> getColumns(String dbNm, String tableNm) {
        String sql = "SELECT * FROM chatqcolumn WHERE db_nm = ? AND table_nm = ?";
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
