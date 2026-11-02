# Automation: The Game

**COSI 103A Software Engineering · Brandeis University · Spring 2026**

A team-built Java project centered on a Dominion-inspired deck-building game. Our group developed game logic and automated player strategies using a course-provided API, along with tools to test and compare how different strategies perform.

## Highlights

- **Strategy-based players:** multiple automated player implementations, including balanced, priority-based, and Big Money strategies.
- **Game simulation:** game engine and a player-rating harness for running repeated simulated games.
- **Evaluation:** the rating harness reports games played, wins, win rate, and average score; the default is 1,000 games (configurable).
- **Testing:** JUnit tests for player behavior, the game engine, and other components.
- **Services:** a Spring Boot HTTP server with `POST /decide` and `POST /log-event` endpoints.

## Technology

Java · Maven · JUnit · Spring Boot · Git/GitHub · Docker (deployment configuration)

## Repository structure

- `automation/src/main/java/edu/brandeis/cosi103a/groupa/players/` — automated player implementations
- `automation/src/main/java/edu/brandeis/cosi103a/groupa/` — game logic and player rating harness
- `automation/src/main/java/edu/brandeis/cosi103a/groupa/server/` — HTTP server components
- `automation/src/test/` — automated tests
- `DEPLOY_TO_AZURE.md` — deployment guidance

## Running locally

Requires Java and Maven. From the repository root:

```bash
cd automation
mvn -DskipTests spring-boot:run -Dspring-boot.run.main-class=edu.brandeis.cosi103a.groupa.PlayerRatingHarness
```

For the HTTP server:

```bash
cd automation
mvn -DskipTests spring-boot:run -Dspring-boot.run.main-class=edu.brandeis.cosi103a.groupa.Application
```

The server uses port `8000`. These commands are documented in the original project; local execution has not been independently re-tested for this portfolio README.

## Team and attribution

Created collaboratively by **Tom Lin, Joyce Li, Zehan Zheng, and Houyi Chen** for Brandeis University's COSI 103A (Spring 2026). The course provided an API used by this project.

Individual contributions can be added here once verified with the team. This repository is a public portfolio copy of the course project.
