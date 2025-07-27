package com.taisiia.gitApp.git.service;

import com.taisiia.gitApp.git.dto.Branch;
import com.taisiia.gitApp.git.dto.GitHubRepo;
import com.taisiia.gitApp.git.dto.GitRepositoryDto;
import com.taisiia.gitApp.git.exception.GitHubException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

@Service
public class GitHubService {


    private final RestTemplate restTemplate;

    private final ExecutorService executor;

    @Value("${github.api.url}")
    private String baseUrl;

    public GitHubService(RestTemplate restTemplate, ExecutorService executor) {
        this.restTemplate = restTemplate;
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
        String url = baseUrl + "/users/{username}/repos";

        try {
            ResponseEntity<GitHubRepo[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    getHttpEntity(),
                    GitHubRepo[].class,
                    userName
            );

            GitHubRepo[] repos = response.getBody();
            return Objects.isNull(repos) ? Collections.emptyList() : Arrays.stream(repos)
                    .filter(repo -> !repo.fork())
                    .toList();

        } catch (HttpClientErrorException.NotFound e) {
            throw new GitHubException(HttpStatus.NOT_FOUND.value(), "User not found");
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new GitHubException(HttpStatus.UNAUTHORIZED.value(), "Unauthorized access");
        } catch (RestClientException e) {
            throw new GitHubException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "GitHub API error: " + e.getMessage());
        }
    }

    public List<Branch> getBranchList(String owner, String repo) {
        String url = baseUrl + "/repos/{owner}/{repo}/branches";

        try {
            ResponseEntity<Branch[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    getHttpEntity(),
                    Branch[].class,
                    owner,
                    repo
            );

            Branch[] branches = response.getBody();
            return Objects.isNull(branches) ? Collections.emptyList() : List.of(branches);

        } catch (HttpClientErrorException.NotFound e) {
            throw new GitHubException(HttpStatus.NOT_FOUND.value(), "Repository or branches not found");
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new GitHubException(HttpStatus.UNAUTHORIZED.value(), "Unauthorized access");
        } catch (RestClientException e) {
            throw new GitHubException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "GitHub API error: " + e.getMessage());
        }
    }

    private HttpEntity<Void> getHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(headers);
    }
}
