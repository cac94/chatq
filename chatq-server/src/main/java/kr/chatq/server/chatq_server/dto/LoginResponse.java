package kr.chatq.server.chatq_server.dto;

import java.util.List;
import java.util.Map;

public class LoginResponse {
    private String message;
    private String auth;
    private List<String> infos;
    private int level;
    private String user;      // 사용자 아이디
    private String user_nm;   // 사용자 이름
    private String pwdFiredYn;
    private Map<String, List<String>> infoColumns;
    private int autoLogoutSec;

    public LoginResponse() {
    }

    public LoginResponse(String message, String auth, List<String> infos, int level, String user, String user_nm, String pwdFiredYn, Map<String, List<String>> infoColumns, int autoLogoutSec) {
        this.message = message;
        this.auth = auth;
        this.infos = infos;
        this.level = level;
        this.user = user;
        this.user_nm = user_nm;
        this.pwdFiredYn = pwdFiredYn;
        this.infoColumns = infoColumns;
        this.autoLogoutSec = autoLogoutSec;
    }
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public List<String> getInfos() {
        return infos;
    }

    public void setInfos(List<String> infos) {
        this.infos = infos;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getUser_nm() {
        return user_nm;
    }

    public void setUser_nm(String user_nm) {
        this.user_nm = user_nm;
    }

    public String getPwdFiredYn() {
        return pwdFiredYn;
    }

    public void setPwdFiredYn(String pwdFiredYn) {
        this.pwdFiredYn = pwdFiredYn;
    }

    public Map<String, List<String>> getInfoColumns() {
        return infoColumns;
    }

    public void setInfoColumns(Map<String, List<String>> infoColumns) {
        this.infoColumns = infoColumns;
    }

    public int getAutoLogoutSec() {
        return autoLogoutSec;
    }

    public void setAutoLogoutSec(int autoLogoutSec) {
        this.autoLogoutSec = autoLogoutSec;
    }
}
