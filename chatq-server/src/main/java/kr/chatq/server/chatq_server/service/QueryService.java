package kr.chatq.server.chatq_server.service;

import kr.chatq.server.chatq_server.dto.QueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QueryService {

    private static final Logger logger = LoggerFactory.getLogger(PromptMakerService.class);
    private final ChatModel chatModel;

    // 대화 메모리를 저장할 Map (conversationId -> 메시지 리스트)
    private final Map<String, List<Message>> conversationMemory = new HashMap<>();

    // application.properties 의 spring.ai.ollama.chat.options.model 값을 aimodel 변수로 선언
    @Value("${spring.ai.ollama.chat.options.model}")
    private String aimodel;

    @Value("${spring.ai.ollama.chat.options.temperature}")
    private Double temperature;

    @Value("${spring.ai.ollama.chat.options.num-predict}")
    private Integer numPredict;

    @Value("${spring.ai.ollama.chat.options.num-ctx:8192}")
    private Integer numCtx;

    @Value("${spring.ai.ollama.chat.options.top-k}")
    private Integer topK;

    @Value("${spring.ai.ollama.chat.options.top-p}")
    private Double topP;

    // Spring이 ChatModel (OllamaChatModel 구현체) 빈을 찾아서 주입
    public QueryService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private javax.sql.DataSource dataSource;

    public QueryResponse executeQuery(String sql) {
        if (sql == null) {
            throw new IllegalArgumentException("SQL query cannot be null");
        }
        //logging sql
        System.out.println("Executing SQL: " + sql);
        logger.info("Executing SQL: {}", sql);

        QueryResponse response = new QueryResponse();
        return jdbcTemplate.query(sql, (ResultSet rs) -> {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // Get column names
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnLabel(i));
            }
            response.setColumns(columns);
            
            // Get data
            List<Map<String, Object>> data = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (String column : columns) {
                    row.put(column, rs.getObject(column));
                }
                data.add(row);
            }
            response.setData(data);
            
            return response;
        });
    }
    
    public QueryResponse executeChat(String conversationId,String message) {
        if (message == null) {
            throw new IllegalArgumentException("message cannot be null");
        }
        String ollamaResponse = (conversationId == null || conversationId.isEmpty()) ? sendChatToOllama(message) : sendChatToOllama(conversationId, message);

        QueryResponse response = new QueryResponse();
        response.setMessage(ollamaResponse);

        return response;
    }

    @SuppressWarnings("unchecked")
    public QueryResponse executeChatQuery(String conversationId, String message) throws SQLException {
        if (message == null) {
            throw new IllegalArgumentException("message cannot be null");
        }

        // 문의에 맞는 테이블을 고르는 프롬프트 생성
        PromptMakerService promptMakerService = new PromptMakerService(new DbService(jdbcTemplate), dataSource);

        Map<String, Object> result = promptMakerService.getPickTablePrompt(getAuth(), message);
        String prompt = (String) result.get("prompt");
        Map<String, String> infos = (Map<String, String>) result.get("infos");

        String ollamaResponse = (conversationId == null || conversationId.isEmpty()) ? sendChatToOllama(prompt) : sendChatToOllama(conversationId, prompt);

        //logging ollamaResponse
        System.out.println("Ollama Response: " + ollamaResponse);
        logger.info("Ollama Response: {}", ollamaResponse);

        // 선택된 테이블의 alias 추출 후 기본 쿼리 가져오기
        String tableAlias = extractBetweenDoubleUnderscores(ollamaResponse);
        String baseQuery = infos.get(tableAlias);

        List<Map<String, Object>> tables = (List<Map<String, Object>>) result.get("tables");
        // tables 에서 tab_alias가 tableAlias인 항목 찾기
        Map<String, Object> selectedTable = null;
        for (Map<String, Object> table : tables) {
            if (tableAlias.equals(table.get("table_alias"))) {
                selectedTable = table;
                break;
            }
        }
        String tableQuery = selectedTable != null ? (String) selectedTable.get("table_query") : null;
        String tableName = selectedTable != null ? (String) selectedTable.get("table_nm") : null;

        Map<String, Object> queryPromptResult = promptMakerService.getQueryPrompt(tableAlias, baseQuery, message);
        String queryPrompt = (String) queryPromptResult.get("prompt");
        ollamaResponse = (conversationId == null || conversationId.isEmpty()) ? sendChatToOllama(queryPrompt) : sendChatToOllama(conversationId, queryPrompt);

        //logging ollamaResponse
        System.out.println("Ollama Response: " + ollamaResponse);
        logger.info("Ollama Response: {}", ollamaResponse);

        // SQL문 추출
        String sql = extractSqlFromMarkdown(ollamaResponse);
        if((sql == null || sql.isEmpty()) && ollamaResponse.toUpperCase().indexOf("SELECT") >= 0) {
            // 여러 개의 SQL 추출 시도
            sql = extractSelectStatement(ollamaResponse);
        }
        if (sql != null && !sql.isEmpty()) {
            sql = sanitizeResponse(sql);
            if(tableQuery != null && !tableQuery.isEmpty()) {
                sql = sql.replace(tableName, tableQuery);
            }

            return executeQuery(sql);
        }

        QueryResponse response = new QueryResponse();
        response.setMessage(ollamaResponse);

        return response;
    }
    
    public QueryResponse executeNewChat() {
        String auth = getAuth();
        String conversationId = auth + "_" + System.currentTimeMillis();
        
        QueryResponse response = new QueryResponse();
        response.setMessage("New chat session started: " + conversationId);
        response.setConversationId(conversationId);

        return response;
    }
    
    // 새 채팅을 시작하는 함수
    public String newChat() {
        String auth = getAuth();
        String conversationId = auth + "_" + System.currentTimeMillis();
        
        // 새로운 대화 세션을 위한 고유 ID 생성
        return conversationId;
    }
    //  @Bean
    // public ChatModel chatModel() {
    //     // OllamaChatModel 빌더로 생성
    //     return OllamaChatModel.builder()
    //             .baseUrl("http://localhost:11434")
    //             .model("deepseek-coder:6.7b")
    //             .build();
    // }   
    
    public String extractBetweenDoubleUnderscores(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("__([^_]+?)__").matcher(text);
        // 첫 번째 매칭만
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
    
    public String extractSqlFromMarkdown(String text) {
        if (text == null) return null;
        // ```sql ... ``` 패턴 매칭 (DOTALL 모드: 줄바꿈 포함)
        Matcher m = Pattern.compile("```sql\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }
    
    public String extractSelectStatement(String text) {
        if (text == null) return null;
        // SELECT ... ; 패턴 매칭 (대소문자 구분 없음, 줄바꿈 포함)
        Matcher m = Pattern.compile("(select[\\s\\S]*?;)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }
    
    private String sanitizeResponse(String text) {
        if (text == null) return null;
        // 특수 토큰 및 바로 앞 단어 제거 (공백 포함)
        return text
            .replaceAll("\\S+\\s*<[\\|｜]begin[▁_]of[▁_]sentence[\\|｜]>", "")
            .replaceAll("\\S+\\s*<[\\|｜]end[▁_]of[▁_]sentence[\\|｜]>", "")
            .replaceAll("\\S+\\s*<\\|begin_of_sentence\\|>", "")
            .replaceAll("\\S+\\s*<\\|end_of_sentence\\|>", "")
            .trim();
    }
    
    // private 메소드: Ollama에 문자열 보내고 결과 받기
    private String sendChatToOllama(String message) {
        OllamaOptions options = OllamaOptions.builder()
            .model(aimodel)
            .temperature(temperature)
            .numPredict(numPredict)
            .numCtx(numCtx)
            .topK(topK)
            .topP(topP)
            .build();

        ChatResponse response = chatModel.call(new Prompt(message, options));
        String rawText = response.getResult().getOutput().getText();
        
        return sanitizeResponse(rawText);
   }

   // private 메소드: Ollama에 문자열 보내고 결과 받기 (conversationId를 사용한 메모리 기능 포함)
    private String sendChatToOllama(String conversationId, String message) {
        if (message == null) {
            throw new IllegalArgumentException("message cannot be null");
        }
        // 이전 대화 내역 가져오기
        List<Message> messages = conversationMemory.getOrDefault(conversationId, new ArrayList<>());
        messages = new ArrayList<>(messages); // 새 리스트 생성 (원본 보호)
        
        // 현재 사용자 메시지 추가
        UserMessage userMessage = new UserMessage(message);
        messages.add(userMessage);
        
        OllamaOptions options = OllamaOptions.builder()
            .model(aimodel)
            .temperature(temperature)
            .numPredict(numPredict)
            .numCtx(numCtx)
            .topK(topK)
            .topP(topP)
            .build();

        // 메시지 히스토리와 함께 Prompt 생성
        ChatResponse response = chatModel.call(new Prompt(messages, options));
        
        // AI 응답을 메시지 리스트에 추가
        messages.add(response.getResult().getOutput());
        
        // 업데이트된 대화 내역을 메모리에 저장
        conversationMemory.put(conversationId, messages);
        String rawText = response.getResult().getOutput().getText();
        
        return sanitizeResponse(rawText);
   }

    // private 메소드: session에서 AUTH 값이 있으면 가져오고 없으면 GUEST 반환
    private String getAuth() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession session = attr.getRequest().getSession(false);
        
        if (session != null) {
            Object auth = session.getAttribute("AUTH");
            if (auth != null) {
                return auth.toString();
            }
        }
        return "GUEST";
    }
}