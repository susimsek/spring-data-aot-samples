package gatling.simulations;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.headerRegex;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import gatling.GatlingDefaults;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.util.UUID;
import org.springframework.http.HttpHeaders;

public class NoteSimulation extends Simulation {

    HttpProtocolBuilder httpConf = GatlingDefaults.httpProtocol();

    ChainBuilder scn =
            exec(http("Authentication")
                            .post("/api/auth/login")
                            .headers(GatlingDefaults.HEADERS_HTTP_AUTHENTICATION)
                            .body(
                                    StringBody(
                                            """
                                            {
                                              "username": "%s",
                                              "password": "%s",
                                              "rememberMe": false
                                            }
                                            """
                                                    .formatted(
                                                            GatlingDefaults.username(),
                                                            GatlingDefaults.password())))
                            .asJson()
                            .check(status().is(200))
                            .check(
                                    jsonPath("$.token")
                                            .ofString()
                                            .find()
                                            .exists()
                                            .saveAs("access_token")))
                    .exitHereIfFailed()
                    .pause(2)
                    .pause(GatlingDefaults.pause())
                    .exec(
                            http("Current user")
                                    .get("/api/auth/me")
                                    .headers(GatlingDefaults.HEADERS_HTTP_AUTHENTICATED)
                                    .check(status().is(200)))
                    .pause(GatlingDefaults.pause())
                    .repeat(2)
                    .on(
                            exec(http("List notes")
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
                                            http("Create note")
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
                                                                      "tags": ["audit"]
                                                                    }
                                                                    """))
                                                    .asJson()
                                                    .check(status().is(201))
                                                    .check(
                                                            headerRegex(
                                                                            HttpHeaders.LOCATION,
                                                                            "(.*)")
                                                                    .saveAs("new_note_url")))
                                    .exitHereIfFailed()
                                    .pause(GatlingDefaults.pause())
                                    .repeat(5)
                                    .on(
                                            exec(http("Get note")
                                                            .get("#{new_note_url}")
                                                            .headers(
                                                                    GatlingDefaults
                                                                            .HEADERS_HTTP_AUTHENTICATED)
                                                            .check(status().is(200)))
                                                    .pause(GatlingDefaults.pause()))
                                    .exec(
                                            http("Update note")
                                                    .put("#{new_note_url}")
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
                                                                      "tags": ["audit","performance"]
                                                                    }
                                                                    """))
                                                    .asJson()
                                                    .check(status().is(200)))
                                    .pause(GatlingDefaults.pause())
                                    .exec(
                                            http("Delete note")
                                                    .delete("#{new_note_url}")
                                                    .headers(
                                                            GatlingDefaults
                                                                    .HEADERS_HTTP_AUTHENTICATED)
                                                    .check(status().is(204)))
                                    .exitHereIfFailed()
                                    .pause(GatlingDefaults.pause())
                                    .exec(
                                            http("Delete note permanently")
                                                    .delete("#{new_note_url}/permanent")
                                                    .headers(
                                                            GatlingDefaults
                                                                    .HEADERS_HTTP_AUTHENTICATED)
                                                    .check(status().is(204)))
                                    .pause(GatlingDefaults.pause()));

    ScenarioBuilder users = scenario("Test the Note").exec(scn);

    {
        setUp(
                        users.injectOpen(
                                rampUsers(GatlingDefaults.users())
                                        .during(GatlingDefaults.rampDuration())))
                .protocols(httpConf)
                .maxDuration(GatlingDefaults.maxDuration());
    }
}
