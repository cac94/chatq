package kr.chatq.server.chatq_server.dto;

import java.util.List;

public class QueryRequest {
    private String prompt;
    private String conversationId;
    private String detailYn;
    private String lastQuery;
    private String tableQuery;
    private String tableName;
    private List<String> headerColumns;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getDetailYn() {
        return detailYn;
    }

    public void setDetailYn(String detailYn) {
        this.detailYn = detailYn;
    }

    public String getLastQuery() {
        return lastQuery;
    }

    public void setLastQuery(String lastQuery) {
        this.lastQuery = lastQuery;
    }

    public String getTableQuery() {
        return tableQuery;
    }

    public void setTableQuery(String tableQuery) {
        this.tableQuery = tableQuery;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<String> getHeaderColumns() {
        return headerColumns;
    }

    public void setHeaderColumns(List<String> headerColumns) {
        this.headerColumns = headerColumns;
    }
}