package com.taisiia.gitApp.git.dto;

import java.util.List;

public record GitRepositoryDto(String repositoryName, String ownerLogin, List<Branch> branches) {
}
