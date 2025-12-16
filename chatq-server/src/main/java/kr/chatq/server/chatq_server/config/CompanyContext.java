package kr.chatq.server.chatq_server.config;

public class CompanyContext {
    private static final ThreadLocal<String> currentCompany = new ThreadLocal<>();

    public static void setCompany(String company) {
        currentCompany.set(company);
    }

    public static String getCompany() {
        return currentCompany.get();
    }

    public static void clear() {
        currentCompany.remove();
    }
}
