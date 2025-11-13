package kr.chatq.server.chatq_server.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PromptMakerService {
    private static final Logger logger = LoggerFactory.getLogger(PromptMakerService.class);
 
    private DbService dbService;
    private javax.sql.DataSource dataSource;

    //생성자
    public PromptMakerService(DbService dbService, javax.sql.DataSource dataSource) {
        this.dbService = dbService;
        this.dataSource = dataSource;
    }

    //쿼리문을 생성할 테이블 선택 프롬프트 생성
    public Map<String, Object> getPickTablePrompt(String auth, String message) {
        Map<String, Object> result = new java.util.HashMap<>();
        Map<String, String> infos = new java.util.HashMap<>();

        String metaYn = (message.indexOf("[[meta]]") < 0) ? "N" : "Y";
        List<Map<String, Object>> tables = dbService.getTables(auth, metaYn);

        //Promt 생성을 위한 StringBuilder
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("다음은 정보종류별 조회sql문 json이다. {");
        for( Map<String, Object> table : tables ) {
             StringBuilder queryBuilder = new StringBuilder();
            //table 정보로 getColumns 호출
            String dbName = table.get("db_nm").toString();
            String tableName = table.get("table_nm").toString();
            String tableAlias = table.get("table_alias").toString();
            String tailQuery = table.get("tail_query").toString();
            String headerYn = table.get("header_yn").toString();

            //headerYn 이 "Y" 면 continue
            if(headerYn.equals("Y")) {
                continue;
            }

            promptBuilder.append("\"__").append(tableAlias).append("__\": \"select ");
            queryBuilder.append("select ");

            List<Map<String, Object>> columns = dbService.getColumns(dbName, tableName);
            //문자열을 담을 배열 변수 columnsList 선언
            List<String> columnsList = new ArrayList<>();
            
            for( int i = 0; i < columns.size(); i++ ) {
                Map<String, Object> column = columns.get(i);
                String columnCd = column.get("column_cd") != null ? column.get("column_cd").toString() : "";
                String columnName = column.get("column_nm") != null ? column.get("column_nm").toString() : "";
                String columnDesc = column.get("column_desc") != null ? column.get("column_desc").toString() : "";
                String subqueryYn = column.get("subquery_yn") != null ? column.get("subquery_yn").toString() : "N";

                StringBuilder columnBuilder = new StringBuilder();
                if( subqueryYn.equals("Y") ) {
                    columnBuilder.append("(").append(columnDesc).append(") as '").append(columnName).append("'");
                } else {
                    columnBuilder.append(columnCd).append(" as '").append(columnName).append("'");
                }

                columnsList.add(columnBuilder.toString());
            }
            promptBuilder.append(String.join(", ", columnsList));
            queryBuilder.append(String.join(", ", columnsList));

            promptBuilder.append(" from ").append(tableName).append(" ").append(tailQuery).append("\",\n ");
            queryBuilder.append(" from ").append(tableName).append(" ").append(tailQuery);
            infos.put(tableAlias, queryBuilder.toString());
        }   
        promptBuilder.append("}\n [[").append(message).append("]]  문의에 가장 가까운 정보종류는?");

        String prompt = promptBuilder.toString();
        
        // 프롬프트 로그 남기기
        logger.info("===  PickTablePrompt Generated Prompt ===");
        logger.info("Auth: {}", auth);
        logger.info("Message: {}", message);
        logger.info("Prompt:\n{}", prompt);
        logger.info("========================");

        //콘솔 출력 
        System.out.println("PickTablePrompt Generated Prompt: " + prompt);
        result.put("prompt", prompt);
        result.put("infos", infos);
        result.put("tables", tables);

        return result;
    }

    public Map<String, Object> getQueryPrompt(String tableAlias, String baseQuery, String message) {
        Map<String, Object> result = new java.util.HashMap<>();

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("다음은 정보종류별 조회sql문 이다. {");
        promptBuilder.append("\"__").append(tableAlias).append("__\": \"").append(baseQuery).append("\"");
        try {
            promptBuilder.append("}\n [[").append(message).append("]]  문의에 맞게 " + dbProductName() + " sql 문을 만들어 쿼리문만 표시해줘"); //.append(tableAlias).append("__ 만 사용해줘.");
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String prompt = promptBuilder.toString();

        // 프롬프트 로그 남기기
        logger.info("===  QueryPrompt Generated Prompt ===");
        logger.info("TableAlias: {}", tableAlias);
        logger.info("BaseQuery: {}", baseQuery);
        logger.info("Message: {}", message);
        logger.info("Prompt:\n{}", prompt);
        logger.info("========================");

        //콘솔 출력 
        System.out.println("QueryPrompt Generated Prompt: " + prompt);
        result.put("prompt", prompt);

        return result;
    }

    private String dbProductName() throws SQLException {
        try (var conn = dataSource.getConnection()) {
            return conn.getMetaData().getDatabaseProductName(); // 예: "MariaDB"
        }
    }
}
