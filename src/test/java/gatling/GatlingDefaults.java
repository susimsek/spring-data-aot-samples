package gatling;

import io.gatling.javaapi.http.HttpDsl;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public final class GatlingDefaults {

    private GatlingDefaults() {}

    public static final String BASE_URL =
            Optional.ofNullable(System.getProperty("baseURL")).orElse("http://localhost:8080");

    public static int users() {
        return Integer.getInteger("users", 10);
    }

    public static Duration rampDuration() {
        return Duration.ofMinutes(Integer.getInteger("ramp", 1));
    }

    public static Duration testDuration() {
        return Duration.ofMinutes(Integer.getInteger("duration", 1));
    }

    public static Duration maxDuration() {
        return rampDuration().plus(testDuration()).plusSeconds(30);
    }

    public static Duration minPause() {
        return Duration.ofSeconds(Long.getLong("minPauseSeconds", 10));
    }

    public static Duration maxPause() {
        return Duration.ofSeconds(Long.getLong("maxPauseSeconds", 20));
    }

    public static Duration pause() {
        return Duration.ofSeconds(Long.getLong("pauseSeconds", 10));
    }

    public static String username() {
        return Optional.ofNullable(System.getProperty("username")).orElse("user");
    }

    public static String password() {
        return Optional.ofNullable(System.getProperty("password")).orElse("user");
    }

    public static final Map<String, String> HEADERS_HTTP_AUTHENTICATION =
            Map.of(
                    HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_JSON_VALUE,
                    HttpHeaders.ACCEPT,
                    MediaType.APPLICATION_JSON_VALUE);

    public static final Map<String, String> HEADERS_HTTP_AUTHENTICATED =
            Map.of(
                    HttpHeaders.ACCEPT,
                    MediaType.APPLICATION_JSON_VALUE,
                    HttpHeaders.AUTHORIZATION,
                    "Bearer #{access_token}");

    public static HttpProtocolBuilder httpProtocol() {
        return HttpDsl.http
                .baseUrl(BASE_URL)
                .acceptHeader("*/*")
                .acceptEncodingHeader("gzip, deflate")
                .acceptLanguageHeader("en-US,en;q=0.9")
                .connectionHeader("keep-alive")
                .userAgentHeader(
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:33.0) Gecko/20100101"
                                + " Firefox/33.0")
                .silentResources();
    }
}
