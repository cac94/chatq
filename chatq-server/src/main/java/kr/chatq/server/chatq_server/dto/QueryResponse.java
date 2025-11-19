package kr.chatq.server.chatq_server.dto;

import java.util.List;
import java.util.Map;


public class QueryResponse {
    private List<String> columns;
    private List<Map<String, Object>> data;
    private List<String> headerColumns;
    private List<Map<String, Object>> headerData;
    private List<String> lastColumns;
    private String message;
    private String conversationId;
    private String detailYn;
    private String lastQuery;
    private String tableQuery;
    private String tableName;
    private String lastDetailYn;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<Map<String, Object>> getData() {
        return data;
    }

    public void setData(List<Map<String, Object>> data) {
        this.data = data;
    }

    public List<String> getLastColumns() {
        return lastColumns;
    }

    public void setLastColumns(List<String> lastColumns) {
        this.lastColumns = lastColumns;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public List<String> getHeaderColumns() {
        return headerColumns;
    }

    public void setHeaderColumns(List<String> headerColumns) {
        this.headerColumns = headerColumns;
    }

    public List<Map<String, Object>> getHeaderData() {
        return headerData;
    }

    public void setHeaderData(List<Map<String, Object>> headerData) {
        this.headerData = headerData;
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

    public String getLastDetailYn() {
        return lastDetailYn;
    }

    public void setLastDetailYn(String lastDetailYn) {
        this.lastDetailYn = lastDetailYn;
    }
}