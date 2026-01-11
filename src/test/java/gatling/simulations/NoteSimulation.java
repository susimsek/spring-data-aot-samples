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
import java.util.UUID;

public class NoteSimulation extends Simulation {

    ChainBuilder scn =
            exec(http("First unauthenticated request")
                            .get("/api/auth/me")
                            .headers(GatlingDefaults.HEADERS_HTTP)
                            .check(status().is(401)))
                    .exitHereIfFailed()
                    .pause(GatlingDefaults.longPause())
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
                            session ->
                                    session.set(
                                            "access_token",
                                            session.getString("access_token").startsWith("Bearer ")
                                                    ? session.getString("access_token")
                                                    : "Bearer "
                                                            + session.getString("access_token")))
                    .exec(
                            session ->
                                    session.contains("access_token")
                                            ? session
                                            : session.markAsFailed())
                    .exitHereIfFailed()
                    .pause(GatlingDefaults.shortPause())
                    .exec(
                            http("Authenticated request")
                                    .get("/api/auth/me")
                                    .headers(GatlingDefaults.HEADERS_HTTP_AUTHENTICATED)
                                    .check(status().is(200)))
                    .pause(GatlingDefaults.longPause())
                    .repeat(2)
                    .on(
                            exec(http("Get all notes")
                                            .get("/api/notes?page=0&size=20&sort=id,desc")
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
                                    .exec(
                                            session ->
                                                    session.contains("note_id")
                                                            ? session
                                                            : session.markAsFailed())
                                    .exitHereIfFailed()
                                    .pause(GatlingDefaults.longPause())
                                    .repeat(5)
                                    .on(
                                            exec(session ->
                                                            session.contains("note_id")
                                                                    ? session
                                                                    : session.markAsFailed())
                                                    .exitHereIfFailed()
                                                    .exec(
                                                            http("Get created note")
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
                                                    .pause(GatlingDefaults.longPause()))
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
                                    .pause(GatlingDefaults.longPause())
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
                                    .pause(GatlingDefaults.longPause())
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
                                    .pause(GatlingDefaults.longPause()));

    ScenarioBuilder users =
            scenario("Test notes")
                    .exec(session -> session.set("note_uuid", UUID.randomUUID().toString()))
                    .exec(scn);

    {
        setUp(
                        users.injectOpen(
                                rampUsers(GatlingDefaults.users())
                                        .during(GatlingDefaults.rampDuration())))
                .protocols(GatlingDefaults.httpProtocol());
    }
}
