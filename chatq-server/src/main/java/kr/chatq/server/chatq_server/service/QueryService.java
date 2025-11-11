package kr.chatq.server.chatq_server.service;

import kr.chatq.server.chatq_server.dto.QueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QueryService {

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

    public QueryResponse executeQuery(String sql) {
        if (sql == null) {
            throw new IllegalArgumentException("SQL query cannot be null");
        }
        
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

    public QueryResponse executeChatQuery(String conversationId, String message) {
        if (message == null) {
            throw new IllegalArgumentException("message cannot be null");
        }
        String auth = getAuth();
        List<Map<String, Object>> authTables = getAuthTables(auth);
        String ollamaResponse = (conversationId == null || conversationId.isEmpty()) ? sendChatToOllama(message) : sendChatToOllama(conversationId, message);

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

    // private 메소드: Ollama에 문자열 보내고 결과 받기
    private String sendChatToOllama(String message) {
        OllamaOptions options = OllamaOptions.builder()
            .model(aimodel)
            .temperature(temperature)
            .numPredict(numPredict)
            .topK(topK)
            .topP(topP)
            .build();

        ChatResponse response = chatModel.call(new Prompt(message, options));
        
        return response.getResult().getOutput().getText();
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
            .topK(topK)
            .topP(topP)
            .build();

        // 메시지 히스토리와 함께 Prompt 생성
        ChatResponse response = chatModel.call(new Prompt(messages, options));
        
        // AI 응답을 메시지 리스트에 추가
        messages.add(response.getResult().getOutput());
        
        // 업데이트된 대화 내역을 메모리에 저장
        conversationMemory.put(conversationId, messages);
        
        return response.getResult().getOutput().getText();
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
}