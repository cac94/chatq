package kr.chatq.server.chatq_server.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@Service
public class PromptMakerService {
    private static final Logger logger = LoggerFactory.getLogger(PromptMakerService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private DbService dbService;
    private javax.sql.DataSource dataSource;

    @Value("${chatq.query.date_format}")
    private String dateFormat;

    // 생성자
    public PromptMakerService(DbService dbService,
            @org.springframework.beans.factory.annotation.Qualifier("secondaryDataSource") javax.sql.DataSource dataSource) {
        this.dbService = dbService;
        this.dataSource = dataSource;
    }

    // 쿼리문을 생성할 테이블 선택 프롬프트 생성
    public Map<String, Object> getPickTablePrompt(String auth, String message, int level) throws SQLException {
        Map<String, Object> result = new java.util.HashMap<>();
        Map<String, String> infos = new java.util.HashMap<>();

        String metaYn = (message.indexOf("[[meta]]") < 0) ? "N" : "Y";
        List<Map<String, Object>> tables = dbService.getTables(auth, metaYn);
        List<String> tableAliasList = new ArrayList<>();

        // Promt 생성을 위한 StringBuilder
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("다음은 정보종류별 조회sql문 json이다. {");
        for (Map<String, Object> table : tables) {
            StringBuilder queryBuilder = new StringBuilder();
            // table 정보로 getColumns 호출
            String tableName = table.get("table_nm").toString();
            String tableAlias = table.get("table_alias").toString();
            String tailQuery = table.get("tail_query").toString();
            tableAliasList.add("`__" + tableAlias + "__`");

            promptBuilder.append("\"__").append(tableAlias).append("__\": \"select ");
            queryBuilder.append("select ");

            List<Map<String, Object>> columns = dbService.getColumns(tableName, level);
            // 문자열을 담을 배열 변수 columnList 선언
            List<String> columnList = new ArrayList<>();
            List<String> columnNmList = new ArrayList<>();
            List<String> headerColumnList = new ArrayList<>();
            Map<String, String> codeMaps = new HashMap<>();

            for (int i = 0; i < columns.size(); i++) {
                Map<String, Object> column = columns.get(i);
                String columnCd = column.get("column_cd") != null ? column.get("column_cd").toString() : "";
                String columnName = column.get("column_nm") != null ? column.get("column_nm").toString() : "";
                String columnDesc = column.get("column_desc") != null ? column.get("column_desc").toString() : "";
                String subqueryYn = column.get("subquery_yn") != null ? column.get("subquery_yn").toString() : "N";
                String codeMap = column.get("code_map") != null ? column.get("code_map").toString() : "";
                if (!codeMap.isEmpty()) {
                    codeMaps.put(columnName, codeMap);
                }
                String headerColumnYn = column.get("header_column_yn") != null
                        ? column.get("header_column_yn").toString()
                        : "N";

                StringBuilder columnBuilder = new StringBuilder();
                if (subqueryYn.equals("Y")) {
                    columnBuilder.append("(").append(columnDesc).append(") as `").append(columnName).append("`");
                } else {
                    columnBuilder.append(columnCd).append(" as `").append(columnName).append("`");
                }

                columnList.add(columnBuilder.toString());

                columnNmList.add(columnName);
                if (headerColumnYn.equals("Y")) {
                    headerColumnList.add(columnName);
                }
            }
            promptBuilder.append(String.join(", ", columnList));
            queryBuilder.append(String.join(", ", columnList));

            promptBuilder.append(" from ").append(tableName).append(" ").append(tailQuery).append("\",\n ");
            queryBuilder.append(" from ").append(tableName).append(" ").append(tailQuery);
            infos.put(tableAlias, queryBuilder.toString());
            table.put("headerColumns", headerColumnList);
            table.put("columnNmList", columnNmList);
            table.put("codeMaps", codeMaps);
        }
        promptBuilder.append("}\\n 이 json 내용을 참고하여 [[").append(message).append("]]  문의에 가장 가까운 정보종류를 ")
                .append(String.join(", ", tableAliasList)).append(" 중에서 어느 것인지 한 개만 골라줘.");

        String prompt = promptBuilder.toString();

        // 프롬프트 로그 남기기
        logger.info("===  PickTablePrompt Generated Prompt ===");
        logger.info("Auth: {}", auth);
        logger.info("Message: {}", message);
        logger.info("Prompt:\n{}", prompt);
        logger.info("========================");

        // 콘솔 출력
        System.out.println("PickTablePrompt Generated Prompt: " + prompt);
        result.put("prompt", prompt);
        result.put("infos", infos);
        result.put("tables", tables);

        return result;
    }

    public Map<String, Object> getQueryPrompt(String baseQuery, String message, Map<String, String> codeMaps)
            throws SQLException {
        Map<String, Object> result = new java.util.HashMap<>();

        String codeMapsJson = "";
        try {
            codeMapsJson = objectMapper.writeValueAsString(codeMaps);
        } catch (JsonProcessingException e) {
            logger.error("Error converting codeMaps to JSON", e);
        }

        StringBuilder promptBuilder = new StringBuilder();
        if (!codeMapsJson.equals("{}")) {
            promptBuilder.append("다음은 칼럼별 허용 값 정보이다. 쿼리 작성시 참고해라: ").append(codeMapsJson).append("\\n\\n");
        }
        promptBuilder.append("```sql\\n").append(baseQuery).append(";\\n```\\n 앞의 sql문을 수정하여 [[")
                // .append(message).append("]] 문의에 답변하기 위한 쿼리문을 작성해줘. 어떤 토큰 문자열(<|...|>)도 출력하지
                // 말고 작성한 쿼리문만 답해줘.");
                .append(message).append("]] 문의에 답변하기 위한 ").append(dbProductName())
                .append(" 쿼리문을 작성해줘.")
                .append(" 날짜형식은 '" + dateFormat + "'이고 column alias는 유지해줘. 작성한 쿼리문만 출력해줘.");

        String prompt = promptBuilder.toString();

        // 프롬프트 로그 남기기
        logger.info("===  QueryPrompt Generated Prompt ===");
        logger.info("BaseQuery: {}", baseQuery);
        logger.info("Message: {}", message);
        logger.info("Prompt:\n{}", prompt);
        logger.info("========================");

        // 콘솔 출력
        System.out.println("QueryPrompt Generated Prompt: " + prompt);
        result.put("prompt", prompt);

        return result;
    }

    private String dbProductName() throws SQLException {
        var conn = dataSource.getConnection();
        String productName = conn.getMetaData().getDatabaseProductName();
        conn.close();
        return productName; // 예: "MariaDB"
    }

    /**
     * 차트 생성을 위한 프롬프트 생성
     * 
     * @param prompt    사용자 프롬프트
     * @param chartType 차트 타입
     * @param columns   컬럼 정보
     * @param data      데이터 샘플
     * @return 생성된 프롬프트
     */
    public String getChartPrompt(String prompt, String chartType, List<Map<String, String>> columns,
            List<Map<String, Object>> data) {
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("당신은 데이터 분석 전문가입니다. 사용자가 제공한 데이터를 분석하여 차트에 적합한 형태로 변환해야 합니다.\n\n");
        systemPrompt.append("차트 타입: ").append(chartType).append("\n");
        systemPrompt.append("컬럼 정보: ").append(columns.toString()).append("\n\n");
        systemPrompt.append("데이터 샘플 (최대 10개):\n");

        int sampleSize = Math.min(10, data.size());
        for (int i = 0; i < sampleSize; i++) {
            systemPrompt.append(data.get(i).toString()).append("\n");
        }

        systemPrompt.append("\n사용자 요청: ").append(prompt).append("\n\n");
        systemPrompt.append("다음 형식의 JSON으로 응답하세요:\n");
        systemPrompt.append("{\n");
        systemPrompt.append("  \"labels\": [\"라벨1\", \"라벨2\", ...],\n");
        systemPrompt.append("  \"datasets\": [\n");
        systemPrompt.append("    {\n");
        systemPrompt.append("      \"label\": \"데이터셋 이름\",\n");
        systemPrompt.append("      \"data\": [숫자1, 숫자2, ...],\n");
        systemPrompt.append("      \"backgroundColor\": \"rgba(53, 162, 235, 0.5)\",\n");
        systemPrompt.append("      \"borderColor\": \"rgb(53, 162, 235)\",\n");
        systemPrompt.append("      \"borderWidth\": 1\n");
        systemPrompt.append("    }\n");
        systemPrompt.append("  ]\n");
        systemPrompt.append("}\n\n");
        systemPrompt.append("JSON 외에는 아무것도 출력하지 마세요. 코드 블록 마커(```)도 사용하지 마세요.");

        String generatedPrompt = systemPrompt.toString();

        // 프롬프트 로그 남기기
        logger.info("===  ChartPrompt Generated Prompt ===");
        logger.info("ChartType: {}", chartType);
        logger.info("UserPrompt: {}", prompt);
        logger.info("Columns: {}", columns);
        logger.info("DataSize: {}", data.size());
        logger.info("Prompt:\n{}", generatedPrompt);
        logger.info("========================");

        System.out.println("ChartPrompt Generated Prompt: " + generatedPrompt);

        return generatedPrompt;
    }
}
