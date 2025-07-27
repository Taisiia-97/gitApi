package com.taisiia.gitApp;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.not;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock(@ConfigureWireMock(port = 8089))
@ActiveProfiles("test")
public class GitHubControllerIntegrationTest {


    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() throws IOException {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        String reposJson = Files.readString(Path.of("src/test/resources/__files/repos-response.json"));
        String branchesJson = Files.readString(Path.of("src/test/resources/__files/branches-response.json"));

        stubFor(get(urlPathMatching("/users/[^/]+/repos"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(reposJson)));

        stubFor(get(urlPathMatching("/repos/[^/]+/[^/]+/branches"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(branchesJson)));
    }


    @Test
    void shouldReturnRepositoriesWithBranchesForValidUser() {
        // given
        String userName = "octocat";

        // when + then
        RestAssured
                .given()
                .accept(ContentType.JSON)
                .when()
                .get("/api/git/repo/" + userName)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", not(empty()))
                .body("[0].repositoryName", notNullValue())
                .body("[0].ownerLogin", equalToIgnoringCase(userName))
                .body("[0].branches", not(empty()))
                .body("[0].branches[0].name", notNullValue())
                .body("[0].branches[0].commit.sha", notNullValue());
    }
}
