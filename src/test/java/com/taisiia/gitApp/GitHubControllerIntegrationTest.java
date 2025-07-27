package com.taisiia.gitApp;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GitHubControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }


    @Test
    void shouldReturnRepositoriesWithBranchesForValidUser() {
        // given
        String userName = "Taisiia-97";

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
