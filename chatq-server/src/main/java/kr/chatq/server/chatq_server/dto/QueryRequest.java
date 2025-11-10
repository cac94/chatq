package kr.chatq.server.chatq_server.dto;

public class QueryRequest {
    private String sql;
    private String conversationId;

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}