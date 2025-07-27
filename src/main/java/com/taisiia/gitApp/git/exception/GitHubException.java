package com.taisiia.gitApp.git.exception;

public class GitHubException extends RuntimeException {

    private final int status;

    public GitHubException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
