package kr.chatq.server.chatq_server.service;

import kr.chatq.server.chatq_server.dto.QueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaOptions;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QueryService {

    private final ChatModel chatModel;

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
    public QueryResponse executeChatQuery(String message) {
        if (message == null) {
            throw new IllegalArgumentException("message cannot be null");
        }
        String ollamaResponse = sendChatToOllama(message);

        QueryResponse response = new QueryResponse();
        response.setMessage(ollamaResponse);

        return response;
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
            .model("deepseek-coder:6.7b")
            .temperature(0.3)
            .build();

        ChatResponse response = chatModel.call(new Prompt(message, options));
        return response.getResult().getOutput().getText();
   }    
}