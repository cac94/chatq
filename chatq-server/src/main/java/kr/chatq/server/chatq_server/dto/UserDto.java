package kr.chatq.server.chatq_server.dto;

public class UserDto {
    private String user;
    private String user_nm;
    private String password;
    private String auth;
    private Integer level;

    public UserDto() {}

    public UserDto(String user, String user_nm, String password, String auth, Integer level) {
        this.user = user;
        this.user_nm = user_nm;
        this.password = password;
        this.auth = auth;
        this.level = level;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getUser_nm() {
        return user_nm;
    }

    public void setUser_nm(String user_nm) {
        this.user_nm = user_nm;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }
}
