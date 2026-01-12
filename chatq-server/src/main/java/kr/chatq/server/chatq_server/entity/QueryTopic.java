package kr.chatq.server.chatq_server.entity;

import java.time.LocalDateTime;

public class QueryTopic {
    private String company;
    private String user;
    private Long topicId;
    private String firstQuery;
    private String tableAlias;
    private LocalDateTime startedAt;
    private String addDate;
    private String addTime;

    public QueryTopic() {
    }

    public QueryTopic(String company, String user, Long topicId, String firstQuery, String tableAlias, 
                     LocalDateTime startedAt, String addDate, String addTime) {
        this.company = company;
        this.user = user;
        this.topicId = topicId;
        this.firstQuery = firstQuery;
        this.tableAlias = tableAlias;
        this.startedAt = startedAt;
        this.addDate = addDate;
        this.addTime = addTime;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public String getFirstQuery() {
        return firstQuery;
    }

    public void setFirstQuery(String firstQuery) {
        this.firstQuery = firstQuery;
    }

    public String getTableAlias() {
        return tableAlias;
    }

    public void setTableAlias(String tableAlias) {
        this.tableAlias = tableAlias;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public String getAddDate() {
        return addDate;
    }

    public void setAddDate(String addDate) {
        this.addDate = addDate;
    }

    public String getAddTime() {
        return addTime;
    }

    public void setAddTime(String addTime) {
        this.addTime = addTime;
    }
}
