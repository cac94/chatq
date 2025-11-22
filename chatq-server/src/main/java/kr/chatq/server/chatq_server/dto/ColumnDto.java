package kr.chatq.server.chatq_server.dto;

/**
 * Column meta DTO for updateInfoColumns endpoint.
 * Fields match JSON property names: column_cd, column_nm, level.
 */
public class ColumnDto {
    private String column_cd; // column identifier
    private String column_nm; // column display name / alias
    private int level;        // access level

    public String getColumn_cd() {
        return column_cd;
    }
    public void setColumn_cd(String column_cd) {
        this.column_cd = column_cd;
    }
    public String getColumn_nm() {
        return column_nm;
    }
    public void setColumn_nm(String column_nm) {
        this.column_nm = column_nm;
    }
    public int getLevel() {
        return level;
    }
    public void setLevel(int level) {
        this.level = level;
    }
}