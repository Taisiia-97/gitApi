package com.taisiia.gitApp.git.service;

import com.taisiia.gitApp.git.dto.Branch;
import com.taisiia.gitApp.git.dto.GitHubRepo;
import com.taisiia.gitApp.git.dto.GitRepositoryDto;
import com.taisiia.gitApp.git.exception.GitHubException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

@Service
public class GitHubService {


    private final RestClient restClient;

    private final ExecutorService executor;

    public GitHubService(RestClient restClient, ExecutorService executor) {
        this.restClient = restClient;
        this.executor = executor;
    }

    public List<GitRepositoryDto> getRepoByUserName(String userName) {
        List<GitHubRepo> gitHubRepoList = getGitHubRepoList(userName);

        return gitHubRepoList.stream()
                .map(repo -> executor.submit(() -> {
                    List<Branch> branchList = getBranchList(repo.owner().login(), repo.name());
                    return new GitRepositoryDto(repo.name(), repo.owner().login(), branchList);
                }))
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        throw new GitHubException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error while fetching branches: " + e.getMessage());
                    }
                })
                .toList();
    }

    public List<GitHubRepo> getGitHubRepoList(String userName) {
        try {
            GitHubRepo[] repos = restClient
                    .get()
                    .uri("/users/{username}/repos", userName)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatus.NOT_FOUND::equals, (req, res) -> {
                        throw new GitHubException(HttpStatus.NOT_FOUND.value(), "User not found");
                    })
                    .onStatus(HttpStatus.UNAUTHORIZED::equals, (req, res) -> {
                        throw new GitHubException(HttpStatus.UNAUTHORIZED.value(), "Unauthorized access");
                    })
                    .body(GitHubRepo[].class);

            return Objects.isNull(repos) ? Collections.emptyList() : Arrays.stream(repos).filter(repo -> !repo.fork()).toList();
        } catch (RestClientException e) {
            throw new GitHubException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "GitHub API error: " + e.getMessage());
        }
    }

    public List<Branch> getBranchList(String owner, String repo) {
        try {
            Branch[] branches = restClient
                    .get()
                    .uri("/repos/{owner}/{repo}/branches", owner, repo)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(HttpStatus.NOT_FOUND::equals, (req, res) -> {
                        throw new GitHubException(HttpStatus.NOT_FOUND.value(), "Repository or branches not found");
                    })
                    .onStatus(HttpStatus.UNAUTHORIZED::equals, (req, res) -> {
                        throw new GitHubException(HttpStatus.UNAUTHORIZED.value(), "Unauthorized access");
                    })
                    .body(Branch[].class);
            return Objects.isNull(branches) ? Collections.emptyList() : List.of(branches);

        } catch (RestClientException e) {
            throw new GitHubException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "GitHub API error: " + e.getMessage());
        }
    }
}
