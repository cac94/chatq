package kr.chatq.server.chatq_server.dto;

public class AuthDto {
    private String auth;
    private String auth_nm;
    private String infos;
    private String infonms;

    public AuthDto() {}

    public AuthDto(String auth, String auth_nm, String infos, String infonms) {
        this.auth = auth;
        this.auth_nm = auth_nm;
        this.infos = infos;
        this.infonms = infonms;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public String getAuth_nm() {
        return auth_nm;
    }

    public void setAuth_nm(String auth_nm) {
        this.auth_nm = auth_nm;
    }

    public String getInfos() {
        return infos;
    }

    public void setInfos(String infos) {
        this.infos = infos;
    }

    public String getInfonms() {
        return infonms;
    }

    public void setInfonms(String infonms) {
        this.infonms = infonms;
    }
}
