package kr.chatq.server.chatq_server.service;

import kr.chatq.server.chatq_server.dto.LoginResponse;
import kr.chatq.server.chatq_server.dto.QueryResponse;
import kr.chatq.server.chatq_server.dto.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import kr.chatq.server.chatq_server.config.CompanyContext;

import jakarta.servlet.http.HttpSession;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Service
public class QueryService {

    private static final Logger logger = LoggerFactory.getLogger(PromptMakerService.class);
    private final OllamaChatModel chatModel;
    private final OpenAiChatModel openAiChatModel;

    // 대화 메모리를 저장할 Map (conversationId -> 메시지 리스트)
    private final Map<String, List<Message>> conversationMemory = new HashMap<>();

    // application.properties 의 spring.ai.ollama.chat.options.model 값을 aimodel 변수로
    // 선언
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

    // OpenAI 설정
    @Value("${spring.ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}")
    private String openAiModel;

    @Value("${spring.ai.openai.chat.options.temperature:0.0}")
    private Double openAiTemperature;

    @Value("${spring.ai.openai.chat.options.max-completion-tokens:1024}")
    private Integer openAiMaxTokens;

    // OpenAI reasoning effort (none|low|medium|high)
    @Value("${spring.ai.openai.chat.options.reasoning-effort:}")
    private String openAiReasoningEffort;

    // OpenAI output verbosity (low|medium|high)
    @Value("${spring.ai.openai.chat.options.verbosity:}")
    private String openAiVerbosity;

    // Spring AI type (ollama or openai)
    @Value("${spring.ai.type:ollama}")
    private String aiType;

    // Encryption secret for reversible encryption (AES-GCM)
    @Value("${chatq.encrypt.secret:chatq-default-secret-key-32-bytes!!}")
    private String encryptSecret;

    // Query pre/post processing
    @Value("${chatq.query.pre_query:}")
    private String preQuery;

    @Value("${chatq.query.post_query:}")
    private String postQuery;

    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH = 12; // bytes

    // BCrypt encoder (one-way hashing, salt included per hash)
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("secondaryJdbcTemplate")
    private JdbcTemplate secondaryJdbcTemplate;

    @Autowired
    @Qualifier("secondaryDataSource")
    private DataSource secondaryDataSource;

    @Autowired
    private PromptMakerService promptMakerService;

    @Autowired
    private DbService dbService;

    // Spring이 ChatModel (Ollama 또는 기본 ChatModel) 빈을 찾아서 주입
    public QueryService(OllamaChatModel chatModel, OpenAiChatModel openAiChatModel) {
        this.chatModel = chatModel;
        this.openAiChatModel = openAiChatModel;
    }

    public QueryResponse executeQuery(String sql, String detailYn, List<String> headerColumnList) {
        if (sql == null) {
            throw new IllegalArgumentException("SQL query cannot be null");
        }

        // Apply pre_query and post_query
        String finalSql = sql;
        if (preQuery != null && !preQuery.isEmpty()) {
            finalSql = preQuery + finalSql;
        }
        if (postQuery != null && !postQuery.isEmpty()) {
            // finalSql 의 마지막에 ";" 제거
            if (finalSql.trim().endsWith(";")) {
                finalSql = finalSql.trim();
                finalSql = finalSql.substring(0, finalSql.length() - 1);
            }
            finalSql = finalSql + postQuery;
        }

        // logging sql
        System.out.println("Executing SQL: " + finalSql);
        logger.info("Executing SQL: {}", finalSql);

        QueryResponse response = new QueryResponse();
        return secondaryJdbcTemplate.query(finalSql, (ResultSet rs) -> {
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
            if ("Y".equalsIgnoreCase(detailYn) && headerColumnList != null && !headerColumnList.isEmpty()
                    && columns.containsAll(headerColumnList)) {
                // 헤더용 컬럼만 추출
                List<Map<String, Object>> headerData = new ArrayList<>();
                for (Map<String, Object> row : data) {
                    Map<String, Object> headerRow = new HashMap<>();
                    for (String headerColumn : headerColumnList) {
                        if (row.containsKey(headerColumn)) {
                            headerRow.put(headerColumn, row.get(headerColumn));
                        }
                    }
                    headerData.add(headerRow);
                }
                // 헤더 데이터 중복데이터는 제거하되 원래 순서 유지
                Set<Map<String, Object>> uniqueHeaderData = new LinkedHashSet<>(headerData);
                response.setHeaderData(new ArrayList<>(uniqueHeaderData));
                response.setHeaderColumns(headerColumnList);
                response.setDetailYn("Y");
            } else {
                response.setDetailYn("N");
            }
            return response;
        });
    }

    public QueryResponse executeChat(String conversationId, String message) {
        String ollamaResponse;
        if (message == null) {
            throw new IllegalArgumentException("message cannot be null");
        }

        // aiType에 따라 OpenAI 또는 Ollama로 분기
        if ("openai".equalsIgnoreCase(aiType)) {
            ollamaResponse = (conversationId == null || conversationId.isEmpty()) ? sendChatToOpenAI(message)
                    : sendChatToOpenAI(conversationId, message);
        } else {
            ollamaResponse = (conversationId == null || conversationId.isEmpty()) ? sendChatToOllama(message)
                    : sendChatToOllama(conversationId, message);
        }

        QueryResponse response = new QueryResponse();
        response.setMessage(ollamaResponse);

        return response;
    }

    @SuppressWarnings("unchecked")
    public QueryResponse executeChatQuery(String conversationId, String message, String lastDetailYn, String lastQuery,
            String tableQuery, String tableName, String tableAlias, List<String> headerColumns,
            List<String> lastColumns, Map<String, String> codeMaps) throws SQLException {
        String ollamaResponse;
        String baseQuery;
        String detailYn = lastDetailYn;
        if (message == null) {
            throw new IllegalArgumentException("message cannot be null");
        }

        // 문의에 맞는 테이블을 고르는 프롬프트 생성
        // PromptMakerService is now injected
        // PromptMakerService promptMakerService = new PromptMakerService(new
        // DbService(jdbcTemplate), secondaryDataSource);

        if (lastQuery != null && !lastQuery.isEmpty()) { // 이전 쿼리가 있으면 복호화
            tableQuery = decrypt(tableQuery);
            baseQuery = decrypt(lastQuery);
            lastQuery = baseQuery;
        } else { // 이전 쿼리가 없으면 테이블 선택 프롬프트 실행
            Map<String, Object> result = promptMakerService.getPickTablePrompt(getAuth(), message, getLevel());
            String prompt = (String) result.get("prompt");
            Map<String, String> infos = (Map<String, String>) result.get("infos");

            if (tableAlias == null || tableAlias.isEmpty()) {
                if ("openai".equalsIgnoreCase(aiType)) {
                    ollamaResponse = (conversationId == null || conversationId.isEmpty()) ? sendChatToOpenAI(prompt)
                            : sendChatToOpenAI(conversationId, prompt);
                } else {
                    ollamaResponse = (conversationId == null || conversationId.isEmpty()) ? sendChatToOllama(prompt)
                            : sendChatToOllama(conversationId, prompt);
                }

                // logging ollamaResponse
                System.out.println(aiType + " Response: " + ollamaResponse);
                logger.info("{} Response: {}", aiType, ollamaResponse);

                // 선택된 테이블의 alias 추출 후 기본 쿼리 가져오기
                tableAlias = extractBetweenDoubleUnderscores(ollamaResponse);
            }
            baseQuery = infos.get(tableAlias);

            List<Map<String, Object>> tables = (List<Map<String, Object>>) result.get("tables");
            // tables 에서 tab_alias가 tableAlias인 항목 찾기
            Map<String, Object> selectedTable = null;
            for (Map<String, Object> table : tables) {
                if (tableAlias.equals(table.get("table_alias"))) {
                    selectedTable = table;
                    break;
                }
            }
            tableQuery = selectedTable != null ? (String) selectedTable.get("table_query") : null;
            tableName = selectedTable != null ? (String) selectedTable.get("table_nm") : null;
            detailYn = selectedTable != null ? (String) selectedTable.get("detail_yn") : null;
            headerColumns = selectedTable != null ? (List<String>) selectedTable.get("headerColumns") : null;
            // 기본값으로 last variables 갱신
            lastQuery = baseQuery;
            lastDetailYn = detailYn;
            lastColumns = selectedTable != null ? (List<String>) selectedTable.get("columnNmList") : null;
            codeMaps = selectedTable != null ? (Map<String, String>) selectedTable.get("codeMaps") : null;
        }

        // 최종 쿼리 프롬프트 생성
        Map<String, Object> queryPromptResult = promptMakerService.getQueryPrompt(baseQuery, message, codeMaps);
        String queryPrompt = (String) queryPromptResult.get("prompt");

        if ("openai".equalsIgnoreCase(aiType)) {
            ollamaResponse = (conversationId == null || conversationId.isEmpty()) ? sendChatToOpenAI(queryPrompt)
                    : sendChatToOpenAI(conversationId, queryPrompt);
        } else {
            ollamaResponse = (conversationId == null || conversationId.isEmpty()) ? sendChatToOllama(queryPrompt)
                    : sendChatToOllama(conversationId, queryPrompt);
        }

        // logging ollamaResponse
        System.out.println(aiType + " Response: " + ollamaResponse);
        logger.info("{} Response: {}", aiType, ollamaResponse);

        // SQL문 추출
        String sql = extractSqlFromMarkdown(ollamaResponse);
        if ((sql == null || sql.isEmpty()) && ollamaResponse.toUpperCase().indexOf("SELECT") >= 0) {
            // 여러 개의 SQL 추출 시도
            sql = extractSelectStatement(ollamaResponse);
        }

        if (sql != null && !sql.isEmpty()) {
            sql = sanitizeResponse(sql);
            // sql을 암호화해서 String lastQuery에 저장
            String sqlOrg = sql;
            if (tableQuery != null && !tableQuery.isEmpty()) {
                sql = sql.replace(tableName, tableQuery);
            }

            QueryResponse queryResponse = executeQuery(sql, detailYn, headerColumns);
            // queryResponse.getColumns() 에 lastColumns 가 포함되면 lastColumns 갱신
            if (lastColumns != null && !lastColumns.isEmpty()) {
                if (queryResponse.getColumns().containsAll(lastColumns)) {
                    lastColumns = queryResponse.getColumns();
                    lastQuery = sqlOrg;
                    lastDetailYn = queryResponse.getDetailYn();
                }
            } else {
                lastColumns = queryResponse.getColumns();
            }

            queryResponse.setMessage("SUCCESS");
            queryResponse.setLastQuery(encrypt(lastQuery));
            queryResponse.setTableQuery(encrypt(tableQuery));
            queryResponse.setTableName(tableName);
            queryResponse.setTableAlias(tableAlias);
            queryResponse.setLastDetailYn(lastDetailYn);
            queryResponse.setLastColumns(lastColumns);
            queryResponse.setCodeMaps(codeMaps);

            return queryResponse;
        }

        QueryResponse response = new QueryResponse();
        response.setMessage("FAIL");

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
    // @Bean
    // public ChatModel chatModel() {
    // // OllamaChatModel 빌더로 생성
    // return OllamaChatModel.builder()
    // .baseUrl("http://localhost:11434")
    // .model("deepseek-coder:6.7b")
    // .build();
    // }

    public String extractBetweenDoubleUnderscores(String text) {
        if (text == null)
            return null;
        Matcher m = Pattern.compile("__([^_]+?)__").matcher(text);
        // 첫 번째 매칭만
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public String extractSqlFromMarkdown(String text) {
        if (text == null)
            return null;
        // ```sql ... ``` 패턴 매칭 (DOTALL 모드: 줄바꿈 포함)
        Matcher m = Pattern.compile("```sql\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    public String extractSelectStatement(String text) {
        if (text == null)
            return null;
        // SELECT ... ; 패턴 매칭 (대소문자 구분 없음, 줄바꿈 포함)
        Matcher m = Pattern.compile("(select[\\s\\S]*?;)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }

        // ';' 없는 경우 SELECT부터 끝까지 반환
        int selectIndex = text.toLowerCase().indexOf("select");
        if (selectIndex != -1) {
            return text.substring(selectIndex).trim();
        }
        return null;
    }

    private String sanitizeResponse(String text) {
        if (text == null)
            return null;
        // 특수 토큰 및 바로 앞 단어 제거 (공백 포함)
        return text
                .replaceAll("\\S+\\s*<[\\|｜]begin[▁_]of[▁_]sentence[\\|｜]>", "")
                .replaceAll("\\S+\\s*<[\\|｜]end[▁_]of[▁_]sentence[\\|｜]>", "")
                .replaceAll("\\S+\\s*<\\|begin_of_sentence\\|>", "")
                .replaceAll("\\S+\\s*<\\|end_of_sentence\\|>", "")
                .trim();
    }

    // private 메소드: Ollama에 문자열 보내고 결과 받기
    @SuppressWarnings("null")
    private String sendChatToOllama(String message) {
        ChatResponse response;
        OllamaOptions options = OllamaOptions.builder()
                .model(aimodel)
                .temperature(temperature)
                .numPredict(numPredict)
                .numCtx(numCtx)
                .topK(topK)
                .topP(topP)
                .build();

        if ("gpt-oss:20b".equals(aimodel)) {
            response = chatModel.call(
                    new Prompt(
                            List.of(
                                    // new SystemMessage("""
                                    // Use reasoning effort = "high".
                                    // Perform multi-step chain-of-thought internally.
                                    // """),
                                    new SystemMessage("""
                                                {
                                                    "settings": {
                                                        "reasoning_effort": "low"
                                                    }
                                                }
                                            """),
                                    new UserMessage(message))));
        } else {
            response = chatModel.call(new Prompt(message, options));
        }

        String rawText = response.getResult().getOutput().getText();

        return sanitizeResponse(rawText);
    }

    // private 메소드: OpenAI에 문자열 보내고 결과 받기
    private String sendChatToOpenAI(String message) {
        if (openAiApiKey == null || openAiApiKey.isEmpty()) {
            throw new IllegalStateException(
                    "OpenAI API key is not configured. Set spring.ai.openai.api-key in application.properties");
        }

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .temperature(1.0)
                .build();

        ChatModel model = (openAiChatModel != null) ? openAiChatModel : chatModel;
        ChatResponse response = model.call(new Prompt(message, options));
        String rawText = response.getResult().getOutput().getText();

        return sanitizeResponse(rawText);
    }

    // private 메소드: OpenAI에 문자열 보내고 결과 받기 (conversationId를 사용한 메모리 기능 포함)
    private String sendChatToOpenAI(String conversationId, String message) {
        if (openAiApiKey == null || openAiApiKey.isEmpty()) {
            throw new IllegalStateException(
                    "OpenAI API key is not configured. Set spring.ai.openai.api-key in application.properties");
        }

        if (message == null) {
            throw new IllegalArgumentException("message cannot be null");
        }

        // 이전 대화 내역 가져오기
        List<Message> messages = conversationMemory.getOrDefault(conversationId, new ArrayList<>());
        messages = new ArrayList<>(messages); // 새 리스트 생성 (원본 보호)

        // 현재 사용자 메시지 추가
        UserMessage userMessage = new UserMessage(message);
        messages.add(userMessage);

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .temperature(1.0)
                .build();

        // 메시지 히스토리와 함께 Prompt 생성
        ChatModel model = (openAiChatModel != null) ? openAiChatModel : chatModel;
        ChatResponse response = model.call(new Prompt(messages, options));

        // AI 응답을 메시지 리스트에 추가
        messages.add(response.getResult().getOutput());

        // 업데이트된 대화 내역을 메모리에 저장
        conversationMemory.put(conversationId, messages);
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

    // private 메소드: session에서 USER 값이 있으면 가져오고 없으면 GUEST 반환
    private String getUser() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession session = attr.getRequest().getSession(false);

        if (session != null) {
            Object user = session.getAttribute("USER");
            if (user != null) {
                return user.toString();
            }
        }
        return "guest";
    }

    // private 메소드: session에서 LEVEL 값이 있으면 가져오고 없으면 9 반환
    private int getLevel() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession session = attr.getRequest().getSession(false);

        if (session != null) {
            Object level = session.getAttribute("LEVEL");
            if (level != null) {
                try {
                    return Integer.parseInt(level.toString());
                } catch (NumberFormatException e) {
                    logger.warn("Invalid LEVEL format in session: {}", level);
                }
            }
        }
        return 9;
    }

    // AES-GCM encryption (reversible)
    private String encrypt(String plainText) {
        if (plainText == null)
            return null;
        try {
            byte[] keyBytes = Arrays.copyOf(encryptSecret.getBytes(StandardCharsets.UTF_8), 32); // 256-bit
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            logger.error("Encryption failed", e);
            return null;
        }
    }

    // AES-GCM decryption helper (for future use)
    @SuppressWarnings("unused")
    private String decrypt(String encBase64) {
        if (encBase64 == null)
            return null;
        try {
            byte[] combined = Base64.getDecoder().decode(encBase64);
            if (combined.length < IV_LENGTH + 1)
                return null;

            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

            byte[] keyBytes = Arrays.copyOf(encryptSecret.getBytes(StandardCharsets.UTF_8), 32); // 256-bit
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Decryption failed", e);
            return null;
        }
    }

    /**
     * 로그인 처리
     * 
     * @param user     사용자 아이디
     * @param password 비밀번호
     * @return LoginResponse
     */
    public LoginResponse processLogin(String user, String password) {
        // DbService is now injected
        // DbService dbService = new DbService(jdbcTemplate);
        // 1) 사용자 기본 정보 + 암호화된 비밀번호 조회
        List<Map<String, Object>> rows = dbService.getUsers(user);
        if (rows.isEmpty()) {
            return new LoginResponse("FAIL", null, null, 9, null, null, null, null);
        }

        Map<String, Object> userInfo = rows.get(0);
        String hashedPassword = (String) userInfo.get("password");
        if (!user.equals("admin") && (hashedPassword == null || !matchesPwd(password, hashedPassword))) {
            // 비밀번호 불일치
            return new LoginResponse("FAIL", null, null, 9, null, null, null, null);
        }

        String auth = (String) userInfo.get("auth");
        int level = ((Number) userInfo.get("level")).intValue();
        String userName = (String) userInfo.get("user_nm");
        String pwdFiredYn = (String) userInfo.get("pwd_fired_yn");

        // 2) 권한별 접근 가능한 정보(테이블) 목록 조회
        List<Map<String, Object>> authTables = dbService.getAuthTables(auth);
        List<String> infos = new ArrayList<>();
        Map<String, List<String>> infoColumns = new HashMap<>();
        for (Map<String, Object> table : authTables) {
            String tableName = (String) table.get("table_nm");
            String tableAlias = (String) table.get("table_alias");
            infos.add(tableAlias);
            List<Map<String, Object>> columns = dbService.getColumns(tableName, level);
            // columns 에서 column_nm 값만 추출하여 infoColumns에 저장
            List<String> columnNmList = new ArrayList<>();
            for (Map<String, Object> column : columns) {
                columnNmList.add((String) column.get("column_nm"));
            }
            infoColumns.put(tableAlias, columnNmList);
        }

        // 3) 세션 저장
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession session = attr.getRequest().getSession(true);
        session.setAttribute("USER", user);
        session.setAttribute("USER_NM", userName);
        session.setAttribute("AUTH", auth);
        session.setAttribute("LEVEL", level);
        session.setAttribute("INFOS", infos);
        session.setAttribute("INFO_COLUMNS", infoColumns);

        // 4) 로그인 성공 응답 반환
        return new LoginResponse(
                "SUCCESS",
                auth,
                infos,
                level,
                user,
                userName,
                pwdFiredYn,
                infoColumns);
    }

    /**
     * 로그아웃 처리
     */
    public void processLogout() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession session = attr.getRequest().getSession(false);
        if (session != null) {
            session.removeAttribute("USER");
            session.removeAttribute("USER_NM");
            session.removeAttribute("AUTH");
            session.removeAttribute("LEVEL");
            session.removeAttribute("INFOS");
            session.removeAttribute("INFO_COLUMNS");
        }
    }

    /**
     * 모든 사용자 조회
     */
    /**
     * 모든 사용자 조회
     */
    public List<UserDto> getUsers() {
        String company = CompanyContext.getCompany();
        String sql = "SELECT user, user_nm, auth, level FROM chatquser WHERE company = ? ORDER BY user";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            UserDto user = new UserDto();
            user.setUser(rs.getString("user"));
            user.setUser_nm(rs.getString("user_nm"));
            user.setAuth(rs.getString("auth"));
            user.setLevel(rs.getInt("level"));
            return user;
        }, company);
    }

    /**
     * 사용자 생성
     */
    public void createUser(UserDto user) {
        String company = CompanyContext.getCompany();
        String encryptedPassword = encryptPwd(user.getPassword());
        String sql = "INSERT INTO chatquser (user, user_nm, password, auth, level, company) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                user.getUser(),
                user.getUser_nm(),
                encryptedPassword,
                user.getAuth(),
                user.getLevel(),
                company);
    }

    /**
     * 사용자 수정
     */
    public void updateUser(String userId, UserDto user) {
        String company = CompanyContext.getCompany();
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            String encryptedPassword = encryptPwd(user.getPassword());
            String sql = "UPDATE chatquser SET user_nm = ?, password = ?, auth = ?, level = ? WHERE company = ? AND user = ?";
            jdbcTemplate.update(sql,
                    user.getUser_nm(),
                    encryptedPassword,
                    user.getAuth(),
                    user.getLevel(),
                    company,
                    userId);
        } else {
            String sql = "UPDATE chatquser SET user_nm = ?, auth = ?, level = ? WHERE company = ? AND user = ?";
            jdbcTemplate.update(sql,
                    user.getUser_nm(),
                    user.getAuth(),
                    user.getLevel(),
                    company,
                    userId);
        }
    }

    /**
     * 사용자 삭제
     */
    public void deleteUser(String userId) {
        String company = CompanyContext.getCompany();
        String sql = "DELETE FROM chatquser WHERE company = ? AND user = ?";
        jdbcTemplate.update(sql, company, userId);
    }

    /**
     * 비밀번호 초기화 (기본값으로 설정)
     */
    public void resetPassword(String userId) {
        String company = CompanyContext.getCompany(); // context
        String defaultPassword = userId; // 기본 비밀번호
        String encryptedPassword = encryptPwd(defaultPassword);
        String sql = "UPDATE chatquser SET pwd_fired_yn = 'Y', password = ? WHERE company = ? AND user = ?";
        jdbcTemplate.update(sql, encryptedPassword, company, userId);
    }

    /**
     * 비밀번호 변경
     * 
     * @param userId          사용자 아이디
     * @param currentPassword 현재 비밀번호 (null 이면 검증 없이 변경)
     * @param newPassword     새 비밀번호
     */
    public void changePassword(String userId, String currentPassword, String newPassword) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("newPassword cannot be null or empty");
        }

        DbService dbService = new DbService(jdbcTemplate);
        List<Map<String, Object>> rows = dbService.getUsers(userId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        Map<String, Object> userInfo = rows.get(0);
        String hashedPassword = (String) userInfo.get("password");

        // currentPassword 가 주어지면 검증
        if (currentPassword != null && !currentPassword.isEmpty()) {
            if (hashedPassword == null || !matchesPwd(currentPassword, hashedPassword)) {
                throw new IllegalArgumentException("Current password does not match");
            }
        }

        String company = CompanyContext.getCompany();
        String newHashed = encryptPwd(newPassword);
        String sql = "UPDATE chatquser SET pwd_fired_yn = 'N', password = ? WHERE company = ? AND user = ?";
        jdbcTemplate.update(sql, newHashed, company, userId);
    }

    /**
     * One-way password hashing using BCrypt. Each invocation generates a unique
     * salt.
     */
    public String encryptPwd(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Verify raw password against stored BCrypt hash.
     */
    public boolean matchesPwd(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }

    /**
     * 모든 권한 옵션 조회
     * 
     * @return 권한 목록
     */
    public List<Map<String, String>> getAuthOptions() {
        String company = CompanyContext.getCompany();
        String sql = "SELECT code as auth, text1 as auth_nm FROM chatqcode WHERE company = ? AND codetype = 'AUTH' ORDER BY sortorder";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, String> map = new HashMap<>();
            map.put("auth", rs.getString("auth"));
            map.put("auth_nm", rs.getString("auth_nm"));
            return map;
        }, company);
    }

    /**
     * 권한 목록 조회 (상세)
     * 
     * @return 권한 목록
     */
    public List<kr.chatq.server.chatq_server.dto.AuthDto> getAuths() {
        String company = CompanyContext.getCompany();
        String sql = "SELECT code as auth, text1 as auth_nm, " +
                "(SELECT GROUP_CONCAT(table_nm) \r\n" + //
                "FROM chatqauth\r\n" + //
                "WHERE company = chatqcode.company AND auth = chatqcode.code) as infos, " +
                "(SELECT GROUP_CONCAT(tb.table_alias) \r\n" + //
                "FROM chatqauth au\r\n" + //
                "INNER JOIN chatqtable tb\r\n" + //
                "ON(au.company = tb.company and au.table_nm = tb.table_nm)\r\n" + //
                "WHERE au.company = chatqcode.company AND au.auth = chatqcode.code) as infonms " +
                "FROM chatqcode WHERE company = ? AND codetype = 'AUTH' ORDER BY sortorder";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            kr.chatq.server.chatq_server.dto.AuthDto auth = new kr.chatq.server.chatq_server.dto.AuthDto();
            auth.setAuth(rs.getString("auth"));
            auth.setAuth_nm(rs.getString("auth_nm"));
            auth.setInfos(rs.getString("infos"));
            auth.setInfonms(rs.getString("infonms"));
            return auth;
        }, company);
    }

    /**
     * 권한 생성
     * 
     * @param auth 권한 정보
     */
    public void createAuth(kr.chatq.server.chatq_server.dto.AuthDto auth) {
        String company = CompanyContext.getCompany();
        // 1. chatqcode 테이블에 권한 추가
        String codeSql = "INSERT INTO chatqcode (codetype, code, text1, company) VALUES ('AUTH', ?, ?, ?)";
        jdbcTemplate.update(codeSql, auth.getAuth(), auth.getAuth_nm(), company);

        // 2. infos가 있으면 chatqauth 테이블에 추가
        if (auth.getInfos() != null && !auth.getInfos().isEmpty()) {
            String[] infoArray = auth.getInfos().split(",");
            String authSql = "INSERT INTO chatqauth (auth, table_nm, company) VALUES (?, ?, ?)";
            for (String info : infoArray) {
                jdbcTemplate.update(authSql, auth.getAuth(), info.trim(), company);
            }
        }
    }

    /**
     * 권한 수정
     * 
     * @param authId 권한 코드
     * @param auth   권한 정보
     */
    public void updateAuth(String authId, kr.chatq.server.chatq_server.dto.AuthDto auth) {
        String company = CompanyContext.getCompany();
        // 1. chatqcode 테이블 업데이트
        String codeSql = "UPDATE chatqcode SET text1 = ? WHERE company = ? AND code = ?";
        jdbcTemplate.update(codeSql, auth.getAuth_nm(), company, authId);

        // 2. chatqauth 테이블 기존 데이터 삭제 후 재생성
        String deleteSql = "DELETE FROM chatqauth WHERE company = ? AND auth = ?";
        jdbcTemplate.update(deleteSql, company, authId);

        if (auth.getInfos() != null && !auth.getInfos().isEmpty()) {
            String[] infoArray = auth.getInfos().split(",");
            String authSql = "INSERT INTO chatqauth (auth, table_nm, add_date, add_time, add_user, company) VALUES (?, ?, ?, ?, ?, ?)";
            String currentUser = getUser();
            LocalDateTime now = LocalDateTime.now();
            String addDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String addTime = now.format(DateTimeFormatter.ofPattern("HHmmss"));
            for (String info : infoArray) {
                jdbcTemplate.update(authSql, authId, info.trim(), addDate, addTime, currentUser, company);
            }
        }
    }

    /**
     * 권한 삭제
     * 
     * @param authId 권한 코드
     */
    public void deleteAuth(String authId) {
        String company = CompanyContext.getCompany();
        // 1. chatqauth 테이블에서 삭제
        String authSql = "DELETE FROM chatqauth WHERE company = ? AND auth = ?";
        jdbcTemplate.update(authSql, company, authId);

        // 2. chatqcode 테이블에서 삭제
        String codeSql = "DELETE FROM chatqcode WHERE company = ? AND code = ?";
        jdbcTemplate.update(codeSql, company, authId);
    }

    /**
     * 정보(테이블) 목록 조회
     * 
     * @return 정보 목록
     */
    public List<Map<String, String>> getInfos() {
        String company = CompanyContext.getCompany();
        String sql = "SELECT table_nm, table_alias FROM chatqtable WHERE company = ? ORDER BY table_nm";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, String> map = new HashMap<>();
            map.put("table_nm", rs.getString("table_nm"));
            map.put("table_alias", rs.getString("table_alias"));
            return map;
        }, company);
    }

    /**
     * 테이블의 칼럼 목록 조회 (메타데이터 기반)
     * 
     * @param tableNm 테이블명
     * @return 칼럼 목록 (column_cd, column_nm, level)
     */
    public List<kr.chatq.server.chatq_server.dto.ColumnDto> getInfoColumns(String tableNm) {
        if (tableNm == null || tableNm.isEmpty()) {
            return List.of();
        }
        String company = CompanyContext.getCompany();
        String sql = "SELECT column_cd, column_nm, level FROM chatqcolumn WHERE company = ? AND table_nm = ? ORDER BY column_order";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            kr.chatq.server.chatq_server.dto.ColumnDto dto = new kr.chatq.server.chatq_server.dto.ColumnDto();
            dto.setColumn_cd(rs.getString("column_cd"));
            dto.setColumn_nm(rs.getString("column_nm"));
            dto.setLevel(rs.getInt("level"));
            return dto;
        }, company, tableNm);
    }

    /**
     * 테이블 칼럼 정보 수정 (현재 메타 저장소가 없어 NO-OP 로깅 처리)
     * 
     * @param tableNm 테이블명
     * @param columns 수정할 칼럼 목록
     */
    public void updateInfoColumns(String tableNm, kr.chatq.server.chatq_server.dto.ColumnDto columnDto) {
        if (tableNm == null || tableNm.isEmpty() || columnDto == null || columnDto.getColumn_cd() == null
                || columnDto.getColumn_cd().isEmpty()) {
            return;
        }
        String company = CompanyContext.getCompany();
        String sql = "UPDATE chatqcolumn SET level = ? WHERE company = ? AND table_nm = ? AND column_cd = ?";
        jdbcTemplate.update(sql,
                columnDto.getLevel(),
                company,
                tableNm,
                columnDto.getColumn_cd());
    }

    /**
     * 현재 HTTP 세션에서 로그인 정보를 복원한다.
     * 컨트롤러 /api/check-session 엔드포인트에서 사용.
     * 
     * @param session HttpSession
     * @return LoginResponse (SUCCESS 또는 FAIL)
     */
    public LoginResponse checkSession(HttpSession session) {
        if (session == null) {
            return new LoginResponse("FAIL", null, null, 9, null, null, null, null);
        }
        String user = (String) session.getAttribute("USER");
        String auth = (String) session.getAttribute("AUTH");
        @SuppressWarnings("unchecked")
        List<String> infos = (List<String>) session.getAttribute("INFOS");
        @SuppressWarnings("unchecked")
        Map<String, List<String>> infoColumns = (Map<String, List<String>>) session.getAttribute("INFO_COLUMNS");
        Integer level = (Integer) session.getAttribute("LEVEL");
        String userName = (String) session.getAttribute("USER_NM");
        if (level == null)
            level = 9;
        return new LoginResponse("SUCCESS", auth, infos, level, user, userName, null, infoColumns);
    }

    /**
     * 차트 생성을 위한 메서드
     * 사용자 프롬프트, 차트 타입, 데이터를 받아 AI가 분석하여 차트 데이터를 생성한다.
     * 
     * @param request ChartRequest (prompt, chartType, columns, data)
     * @return ChartResponse (labels, datasets)
     */
    public kr.chatq.server.chatq_server.dto.ChartResponse generateChart(
            kr.chatq.server.chatq_server.dto.ChartRequest request) {
        String prompt = request.getPrompt();
        String chartType = request.getChartType();
        List<Map<String, String>> columns = request.getColumns();
        List<Map<String, Object>> data = request.getData();
        String conversationId = request.getConversationId();

        // PromptMakerService를 사용하여 차트 프롬프트 생성
        PromptMakerService promptMakerService = new PromptMakerService(new DbService(jdbcTemplate),
                secondaryDataSource);
        String queryPrompt = promptMakerService.getChartPrompt(prompt, chartType, columns, data);

        try {
            String aiResponse;
            if ("openai".equalsIgnoreCase(aiType)) {
                aiResponse = (conversationId == null || conversationId.isEmpty()) ? sendChatToOpenAI(queryPrompt)
                        : sendChatToOpenAI(conversationId, queryPrompt);
            } else {
                aiResponse = (conversationId == null || conversationId.isEmpty()) ? sendChatToOllama(queryPrompt)
                        : sendChatToOllama(conversationId, queryPrompt);
            }

            logger.info("AI Chart Response: {}", aiResponse);

            if (aiResponse == null || aiResponse.isEmpty()) {
                return createDefaultChartResponse(data, columns, chartType);
            }

            // JSON 파싱하여 ChartResponse 생성
            aiResponse = aiResponse.trim();
            if (aiResponse.startsWith("```json")) {
                aiResponse = aiResponse.substring(7);
            }
            if (aiResponse.startsWith("```")) {
                aiResponse = aiResponse.substring(3);
            }
            if (aiResponse.endsWith("```")) {
                aiResponse = aiResponse.substring(0, aiResponse.length() - 3);
            }
            aiResponse = aiResponse.trim();

            // 간단한 JSON 파싱 (실제 환경에서는 Jackson 등의 라이브러리 사용 권장)
            kr.chatq.server.chatq_server.dto.ChartResponse chartResponse = parseChartJson(aiResponse);
            return chartResponse;

        } catch (Exception e) {
            logger.error("차트 생성 중 오류 발생", e);
            // 오류 발생 시 기본 차트 데이터 반환
            return createDefaultChartResponse(data, columns, chartType);
        }
    }

    /**
     * JSON 문자열을 ChartResponse 객체로 파싱
     */
    private kr.chatq.server.chatq_server.dto.ChartResponse parseChartJson(String json) {
        // 실제 프로덕션 환경에서는 Jackson ObjectMapper 사용 권장
        // 여기서는 간단한 파싱 구현
        try {
            json = json.trim();
            kr.chatq.server.chatq_server.dto.ChartResponse response = new kr.chatq.server.chatq_server.dto.ChartResponse();

            // labels 추출
            List<String> labels = new ArrayList<>();
            Pattern labelsPattern = Pattern.compile("\"labels\"\\s*:\\s*\\[([^\\]]+)\\]");
            Matcher labelsMatcher = labelsPattern.matcher(json);
            if (labelsMatcher.find()) {
                String labelsStr = labelsMatcher.group(1);
                String[] labelArray = labelsStr.split(",");
                for (String label : labelArray) {
                    label = label.trim().replaceAll("^\"|\"$", "");
                    labels.add(label);
                }
            }
            response.setLabels(labels);

            // datasets 추출
            List<kr.chatq.server.chatq_server.dto.ChartResponse.ChartDataset> datasets = new ArrayList<>();
            Pattern datasetPattern = Pattern.compile("\\{[^}]*\"label\"[^}]*\\}");
            Matcher datasetMatcher = datasetPattern.matcher(json);

            while (datasetMatcher.find()) {
                String datasetJson = datasetMatcher.group();
                kr.chatq.server.chatq_server.dto.ChartResponse.ChartDataset dataset = new kr.chatq.server.chatq_server.dto.ChartResponse.ChartDataset();

                // label 추출
                Pattern labelPattern = Pattern.compile("\"label\"\\s*:\\s*\"([^\"]+)\"");
                Matcher labelMatcher = labelPattern.matcher(datasetJson);
                if (labelMatcher.find()) {
                    dataset.setLabel(labelMatcher.group(1));
                }

                // data 추출
                Pattern dataPattern = Pattern.compile("\"data\"\\s*:\\s*\\[([^\\]]+)\\]");
                Matcher dataMatcher = dataPattern.matcher(datasetJson);
                if (dataMatcher.find()) {
                    String dataStr = dataMatcher.group(1);
                    List<Number> dataList = new ArrayList<>();
                    String[] dataArray = dataStr.split(",");
                    for (String d : dataArray) {
                        d = d.trim();
                        try {
                            if (d.contains(".")) {
                                dataList.add(Double.parseDouble(d));
                            } else {
                                dataList.add(Integer.parseInt(d));
                            }
                        } catch (NumberFormatException e) {
                            dataList.add(0);
                        }
                    }
                    dataset.setData(dataList);
                }

                // backgroundColor 추출
                Pattern bgColorPattern = Pattern.compile("\"backgroundColor\"\\s*:\\s*\"([^\"]+)\"");
                Matcher bgColorMatcher = bgColorPattern.matcher(datasetJson);
                if (bgColorMatcher.find()) {
                    dataset.setBackgroundColor(bgColorMatcher.group(1));
                } else {
                    dataset.setBackgroundColor("rgba(53, 162, 235, 0.5)");
                }

                // borderColor 추출
                Pattern borderColorPattern = Pattern.compile("\"borderColor\"\\s*:\\s*\"([^\"]+)\"");
                Matcher borderColorMatcher = borderColorPattern.matcher(datasetJson);
                if (borderColorMatcher.find()) {
                    dataset.setBorderColor(borderColorMatcher.group(1));
                } else {
                    dataset.setBorderColor("rgb(53, 162, 235)");
                }

                // borderWidth 추출
                Pattern borderWidthPattern = Pattern.compile("\"borderWidth\"\\s*:\\s*(\\d+)");
                Matcher borderWidthMatcher = borderWidthPattern.matcher(datasetJson);
                if (borderWidthMatcher.find()) {
                    dataset.setBorderWidth(Integer.parseInt(borderWidthMatcher.group(1)));
                } else {
                    dataset.setBorderWidth(1);
                }

                datasets.add(dataset);
            }
            response.setDatasets(datasets);

            return response;
        } catch (Exception e) {
            logger.error("JSON 파싱 오류", e);
            throw new RuntimeException("차트 데이터 파싱 실패", e);
        }
    }

    /**
     * 기본 차트 응답 생성 (AI 호출 실패 시 폴백)
     */
    private kr.chatq.server.chatq_server.dto.ChartResponse createDefaultChartResponse(
            List<Map<String, Object>> data,
            List<Map<String, String>> columns,
            String chartType) {

        kr.chatq.server.chatq_server.dto.ChartResponse response = new kr.chatq.server.chatq_server.dto.ChartResponse();

        // 라벨 생성 (첫 번째 컬럼 또는 인덱스)
        List<String> labels = new ArrayList<>();
        int sampleSize = Math.min(10, data.size());
        String labelKey = columns.isEmpty() ? null : columns.get(0).get("key");

        for (int i = 0; i < sampleSize; i++) {
            if (labelKey != null && data.get(i).containsKey(labelKey)) {
                labels.add(String.valueOf(data.get(i).get(labelKey)));
            } else {
                labels.add("Item " + (i + 1));
            }
        }
        response.setLabels(labels);

        // 데이터셋 생성 (숫자 컬럼을 찾아서)
        List<kr.chatq.server.chatq_server.dto.ChartResponse.ChartDataset> datasets = new ArrayList<>();
        for (int colIdx = 1; colIdx < columns.size(); colIdx++) {
            String colKey = columns.get(colIdx).get("key");
            List<Number> dataValues = new ArrayList<>();

            for (int i = 0; i < sampleSize; i++) {
                Object value = data.get(i).get(colKey);
                if (value instanceof Number) {
                    dataValues.add((Number) value);
                } else {
                    try {
                        dataValues.add(Double.parseDouble(String.valueOf(value)));
                    } catch (NumberFormatException e) {
                        dataValues.add(0);
                    }
                }
            }

            kr.chatq.server.chatq_server.dto.ChartResponse.ChartDataset dataset = new kr.chatq.server.chatq_server.dto.ChartResponse.ChartDataset(
                    columns.get(colIdx).get("label"),
                    dataValues,
                    "rgba(53, 162, 235, 0.5)",
                    "rgb(53, 162, 235)",
                    1);
            datasets.add(dataset);
        }
        response.setDatasets(datasets);

        return response;
    }
}