# Git Application

## Description

This application interacts with the GitHub API to retrieve information about repositories and branches.
https://developer.github.com/v3 was used as a backing API.

## Features

- Retrieve repositories by a GitHub username and a list of branches for a specific repository.

## Installation

1. Clone the repository: `git clone https://github.com/Taisiia-97/gitApplication.git`
2. Navigate to the project directory: `cd gitApplication`
3. Build the project.
4. Run the application.

## Usage

To use the application, send GET requests to the following endpoints:

- Retrieve repositories by username:
  - Endpoint: `/api/git/repo/{userName}`
  - Example: `GET /api/git/repo/Taisiia-97`

## Dependencies

- Java 21
- Spring Boot 3
- WebClient
- Lombok



## Endpoints

### Retrieve Repositories by Username

- **URL:** `/api/git/repo/{userName}`
- **Method:** `GET`
- **URL Params:**
  - `userName`: The GitHub username for which to retrieve repositories.
- **Response:**
  ```json
  [
    {
      "name": "repository_name",
      "owner": "repository_owner",
      "branches": [
        {
          "name": "branch_name",
          "lastCommitSha": "last_commit_sha"
        },
        ...
      ]
    },
    ...
  ]
