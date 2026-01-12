package kr.chatq.server.chatq_server.service;

import kr.chatq.server.chatq_server.config.CompanyContext;
import kr.chatq.server.chatq_server.entity.QueryTopic;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpSession;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatqLogService {
    private static final Logger logger = LoggerFactory.getLogger(ChatqLogService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;

    public ChatqLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * chatqlog 테이블에 로그를 저장 (request, response를 JSON으로 저장)
     * @return 생성된 로그의 auto increment ID
     */
    public int saveChatqLog(Long topicId, Object request, Object response) {
        try {
            String company = CompanyContext.getCompany();
            String user = getCurrentUser();
            LocalDateTime now = LocalDateTime.now();
            String addDate = now.format(DATE_FORMATTER);
            String addTime = now.format(TIME_FORMATTER);

            // request를 JSON으로 변환
            String requestJson = objectMapper.writeValueAsString(request);

            // response를 JSON으로 변환
            String responseJson = null;
            if (response != null) {
                responseJson = objectMapper.writeValueAsString(response);
            }

            String sql = "INSERT INTO chatqlog (company, user, topic_id, add_date, add_time, add_user, request, response) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            
            KeyHolder keyHolder = new GeneratedKeyHolder();
            final String finalRequestJson = requestJson;
            final String finalResponseJson = responseJson;
            
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, company);
                ps.setString(2, user);
                ps.setLong(3, topicId);
                ps.setString(4, addDate);
                ps.setString(5, addTime);
                ps.setString(6, user);
                ps.setString(7, finalRequestJson);
                ps.setString(8, finalResponseJson);
                return ps;
            }, keyHolder);
            
            int generatedId = keyHolder.getKey().intValue();
            logger.info("Saved chatq log - company: {}, user: {}, topic_id: {}, id: {}", company, user, topicId, generatedId);
            return generatedId;
        } catch (JsonProcessingException e) {
            logger.error("Error converting to JSON: {}", e.getMessage(), e);
            return -1;
        } catch (Exception e) {
            logger.error("Error saving chatq log: {}", e.getMessage(), e);
            return -1;
        }
    }

    /**
     * response만 업데이트 (id로 특정 로그의 response 업데이트)
     */
    public void updateResponse(int id, Object response) {
        try {
            // response를 JSON으로 변환
            String responseJson = objectMapper.writeValueAsString(response);

            String sql = "UPDATE chatqlog SET response = ? WHERE id = ?";
            
            jdbcTemplate.update(sql, responseJson, id);
            
            logger.info("Updated chatq log response - id: {}", id);
        } catch (JsonProcessingException e) {
            logger.error("Error converting response to JSON: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error updating chatq log response: {}", e.getMessage(), e);
        }
    }

    public Map<String, Object> getChatqLog(int id) {
        String sql = "SELECT * FROM chatqlog WHERE id = ?";
        try {
            Map<String, Object> logEntry = jdbcTemplate.queryForMap(sql, id);
            return logEntry;
        } catch (Exception e) {
            logger.error("Error retrieving chatq log with id {}: {}", id, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * topic_id로 chatqlog 조회
     */
    public java.util.List<Map<String, Object>> getChatqLogsByTopicId(long topicId) {
        String sql = "SELECT * FROM chatqlog WHERE topic_id = ? ORDER BY id";
        try {
            java.util.List<Map<String, Object>> logs = jdbcTemplate.queryForList(sql, topicId);
            return logs;
        } catch (Exception e) {
            logger.error("Error retrieving chatq logs with topic_id {}: {}", topicId, e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * chatqtopic 테이블에 토픽 정보 저장
     */
    public void saveChatqTopic(long topicId, String firstQuery, String tableAlias) {
        try {
            String company = CompanyContext.getCompany();
            String user = getCurrentUser();
            LocalDateTime now = LocalDateTime.now();
            String addDate = now.format(DATE_FORMATTER);
            String addTime = now.format(TIME_FORMATTER);

            String sql = "INSERT INTO chatqtopic (company, user, topic_id, first_query, table_alias, started_at, add_date, add_time) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE first_query = VALUES(first_query), table_alias = VALUES(table_alias), started_at = VALUES(started_at)";
            
            jdbcTemplate.update(sql, company, user, topicId, firstQuery, tableAlias, now, addDate, addTime);
            
            logger.info("Saved chatq topic - company: {}, user: {}, topic_id: {}", company, user, topicId);
        } catch (Exception e) {
            logger.error("Error saving chatq topic: {}", e.getMessage(), e);
        }
    }

    /**
     * chatqtopic 목록 조회 (페이징 지원)
     * @param lastTopicId 마지막으로 조회한 topic_id (null이면 처음부터 조회)
     * @param limit 조회할 개수
     */
    public List<QueryTopic> getChatqTopics(Long lastTopicId, Integer limit) {
        try {
            String company = CompanyContext.getCompany();
            String user = getCurrentUser();
            
            String sql;
            Object[] params;
            
            if (lastTopicId != null) {
                sql = "SELECT * FROM chatqtopic WHERE company = ? AND user = ? AND topic_id < ? " +
                      "ORDER BY topic_id DESC LIMIT ?";
                params = new Object[]{company, user, lastTopicId, limit};
            } else {
                sql = "SELECT * FROM chatqtopic WHERE company = ? AND user = ? " +
                      "ORDER BY topic_id DESC LIMIT ?";
                params = new Object[]{company, user, limit};
            }
            
            return jdbcTemplate.query(sql, new RowMapper<QueryTopic>() {
                @Override
                public QueryTopic mapRow(ResultSet rs, int rowNum) throws SQLException {
                    QueryTopic topic = new QueryTopic();
                    topic.setCompany(rs.getString("company"));
                    topic.setUser(rs.getString("user"));
                    topic.setTopicId(rs.getLong("topic_id"));
                    topic.setFirstQuery(rs.getString("first_query"));
                    topic.setTableAlias(rs.getString("table_alias"));
                    
                    Timestamp timestamp = rs.getTimestamp("started_at");
                    if (timestamp != null) {
                        topic.setStartedAt(timestamp.toLocalDateTime());
                    }
                    
                    topic.setAddDate(rs.getString("add_date"));
                    topic.setAddTime(rs.getString("add_time"));
                    return topic;
                }
            }, params);
        } catch (Exception e) {
            logger.error("Error retrieving chatq topics: {}", e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * 세션에서 현재 사용자 정보 가져오기
     */
    private String getCurrentUser() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpSession session = attributes.getRequest().getSession(false);
                if (session != null) {
                    String userId = (String) session.getAttribute("USER");
                    if (userId != null) {
                        return userId;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not get current user from session: {}", e.getMessage());
        }
        return "system";
    }
}
