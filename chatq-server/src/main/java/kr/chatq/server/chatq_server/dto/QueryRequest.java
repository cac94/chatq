package kr.chatq.server.chatq_server.dto;

import java.util.List;
import java.util.Map;

public class QueryRequest {
    private String prompt;
    private String conversationId;
    private String lastDetailYn;
    private String lastQuery;
    private String tableQuery;
    private String tableName;
    private String tableAlias;
    private List<String> headerColumns;
    private List<String> lastColumns;
    private Map<String, String> codeMaps;

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

    public String getLastDetailYn() {
        return lastDetailYn;
    }

    public void setLastDetailYn(String lastDetailYn) {
        this.lastDetailYn = lastDetailYn;
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

    public String getTableAlias() {
        return tableAlias;
    }

    public void setTableAlias(String tableAlias) {
        this.tableAlias = tableAlias;
    }

    public List<String> getHeaderColumns() {
        return headerColumns;
    }

    public void setHeaderColumns(List<String> headerColumns) {
        this.headerColumns = headerColumns;
    }

    public List<String> getLastColumns() {
        return lastColumns;
    }

    public void setLastColumns(List<String> lastColumns) {
        this.lastColumns = lastColumns;
    }

    public Map<String, String> getCodeMaps() {
        return codeMaps;
    }

    public void setCodeMaps(Map<String, String> codeMaps) {
        this.codeMaps = codeMaps;
    }
}