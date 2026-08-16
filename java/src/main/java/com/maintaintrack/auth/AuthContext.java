package com.maintaintrack.auth;

/**
 * Singleton holding the JWT for the current session.
 * Never written to disk — lives in memory only.
 */
public class AuthContext {

    private static final AuthContext INSTANCE = new AuthContext();

    private String token;
    private String username;
    private String role;

    private AuthContext() {}

    public static AuthContext getInstance() {
        return INSTANCE;
    }

    public void setSession(String token, String username, String role) {
        this.token    = token;
        this.username = username;
        this.role     = role;
    }

    public void clearSession() {
        this.token    = null;
        this.username = null;
        this.role     = null;
    }

    public boolean isLoggedIn() {
        return token != null && !token.isEmpty();
    }

    public String getToken()    { return token; }
    public String getUsername() { return username; }
    public String getRole()     { return role; }

    public boolean isAdmin()   { return "ADMIN".equals(role); }
    public boolean isManager() { return "MANAGER".equals(role); }
}
