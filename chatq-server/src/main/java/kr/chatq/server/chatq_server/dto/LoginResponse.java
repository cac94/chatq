package kr.chatq.server.chatq_server.dto;

import java.util.List;

public class LoginResponse {
    private String message;
    private String auth;
    private List<String> infos;
    private int level;
    private String user_nm;
    private String pwdFiredYn;

    public LoginResponse() {
    }

    public LoginResponse(String message, String auth, List<String> infos, int level, String user_nm, String pwdFiredYn) {
        this.message = message;
        this.auth = auth;
        this.infos = infos;
        this.level = level;
        this.user_nm = user_nm;
        this.pwdFiredYn = pwdFiredYn;
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
}
