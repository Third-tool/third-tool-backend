package com.example.thirdtool.Common.security.auth.token;

public enum TokenType {

    ACCESS("access"),
    REFRESH("refresh");

    private final String claim;

    TokenType(String claim) {
        this.claim = claim;
    }

    public String claim() {
        return claim;
    }
}
