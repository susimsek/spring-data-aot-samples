package gatling.simulations;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import gatling.GatlingDefaults;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;
import java.util.UUID;

public class NoteSimulation extends Simulation {

    HttpProtocolBuilder httpConf = GatlingDefaults.httpProtocol();

    ChainBuilder scn =
            exec(http("First unauthenticated request")
                            .get("/api/auth/me")
                            .headers(GatlingDefaults.HEADERS_HTTP)
                            .check(status().is(401)))
                    .exitHereIfFailed()
                    .pause(GatlingDefaults.pause())
                    .exec(
                            http("Authentication")
                                    .post("/api/auth/login")
                                    .headers(GatlingDefaults.HEADERS_HTTP_AUTHENTICATION)
                                    .body(
                                            StringBody(
                                                    """
                                                    {
                                                      "username": "%s",
                                                      "password": "%s",
                                                      "rememberMe": true
                                                    }
                                                    """
                                                            .formatted(
                                                                    GatlingDefaults.userUsername(),
                                                                    GatlingDefaults
                                                                            .userPassword())))
                                    .asJson()
                                    .check(status().is(200))
                                    .check(
                                            jsonPath("$.token")
                                                    .ofString()
                                                    .find()
                                                    .exists()
                                                    .saveAs("access_token")))
                    .exec(
                            session -> {
                                var token = session.getString("access_token");
                                if (token == null || token.isBlank()) {
                                    return session.markAsFailed();
                                }
                                if (!token.startsWith("Bearer ")) {
                                    token = "Bearer " + token;
                                }
                                return session.set("access_token", token);
                            })
                    .exitHereIfFailed()
                    .pause(GatlingDefaults.pause())
                    .exec(
                            http("Authenticated request")
                                    .get("/api/auth/me")
                                    .headers(GatlingDefaults.HEADERS_HTTP_AUTHENTICATED)
                                    .check(status().is(200)))
                    .pause(GatlingDefaults.pause())
                    .repeat(2)
                    .on(
                            exec(http("Get all notes")
                                            .get("/api/notes")
                                            .headers(GatlingDefaults.HEADERS_HTTP_AUTHENTICATED)
                                            .check(status().is(200)))
                                    .pause(GatlingDefaults.minPause(), GatlingDefaults.maxPause())
                                    .exec(
                                            session ->
                                                    session.set(
                                                            "note_uuid",
                                                            UUID.randomUUID().toString()))
                                    .exec(
                                            http("Create new note")
                                                    .post("/api/notes")
                                                    .headers(
                                                            GatlingDefaults
                                                                    .HEADERS_HTTP_AUTHENTICATED)
                                                    .body(
                                                            StringBody(
                                                                    """
                                                                    {
                                                                      "title": "Perf Note ${note_uuid}",
                                                                      "content": "Perf content ${note_uuid} - lorem ipsum dolor sit amet",
                                                                      "pinned": false,
                                                                      "color": "#2563eb",
                                                                      "tags": ["audit","perf"]
                                                                    }
                                                                    """))
                                                    .asJson()
                                                    .check(status().is(201))
                                                    .check(
                                                            jsonPath("$.id")
                                                                    .ofLong()
                                                                    .find()
                                                                    .exists()
                                                                    .saveAs("note_id")))
                                    .exitHereIfFailed()
                                    .pause(GatlingDefaults.pause())
                                    .repeat(5)
                                    .on(
                                            exec(http("Get created note")
                                                            .get(
                                                                    session ->
                                                                            "/api/notes/"
                                                                                    + session
                                                                                            .getLong(
                                                                                                    "note_id"))
                                                            .headers(
                                                                    GatlingDefaults
                                                                            .HEADERS_HTTP_AUTHENTICATED)
                                                            .check(status().is(200)))
                                                    .pause(GatlingDefaults.pause()))
                                    .exec(
                                            http("Update note")
                                                    .put(
                                                            session ->
                                                                    "/api/notes/"
                                                                            + session.getLong(
                                                                                    "note_id"))
                                                    .headers(
                                                            GatlingDefaults
                                                                    .HEADERS_HTTP_AUTHENTICATED)
                                                    .body(
                                                            StringBody(
                                                                    """
                                                                    {
                                                                      "title": "Perf Note Updated ${note_uuid}",
                                                                      "content": "Perf content updated ${note_uuid} - lorem ipsum dolor sit amet",
                                                                      "pinned": true,
                                                                      "color": "#16a34a",
                                                                      "tags": ["audit","perf","updated"]
                                                                    }
                                                                    """))
                                                    .asJson()
                                                    .check(status().is(200)))
                                    .pause(GatlingDefaults.pause())
                                    .exec(
                                            http("Soft delete created note")
                                                    .delete(
                                                            session ->
                                                                    "/api/notes/"
                                                                            + session.getLong(
                                                                                    "note_id"))
                                                    .headers(
                                                            GatlingDefaults
                                                                    .HEADERS_HTTP_AUTHENTICATED)
                                                    .check(status().is(204)))
                                    .pause(GatlingDefaults.pause())
                                    .exec(
                                            http("Delete created note permanently")
                                                    .delete(
                                                            session ->
                                                                    "/api/notes/"
                                                                            + session.getLong(
                                                                                    "note_id")
                                                                            + "/permanent")
                                                    .headers(
                                                            GatlingDefaults
                                                                    .HEADERS_HTTP_AUTHENTICATED)
                                                    .check(status().is(204)))
                                    .exec(session -> session.remove("note_id"))
                                    .pause(GatlingDefaults.pause()));

    ScenarioBuilder users = scenario("Test the Note entity").exec(scn);

    {
        Duration maxDuration =
                GatlingDefaults.rampDuration().plus(GatlingDefaults.testDuration()).plusSeconds(30);
        setUp(
                        users.injectOpen(
                                rampUsers(GatlingDefaults.users())
                                        .during(GatlingDefaults.rampDuration())))
                .protocols(httpConf)
                .maxDuration(maxDuration);
    }
}
