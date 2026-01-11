package gatling;

import io.gatling.javaapi.http.HttpDsl;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public final class GatlingDefaults {

    private GatlingDefaults() {}

    public static final String BASE_URL =
            Optional.ofNullable(System.getProperty("baseURL")).orElse("http://localhost:8080");

    private static String mode() {
        return Optional.ofNullable(System.getProperty("mode")).orElse("soak");
    }

    private static boolean stressMode() {
        return "stress".equalsIgnoreCase(mode());
    }

    private static long longPropertyOrDefault(String key, long defaultValue) {
        var value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Long.parseLong(value);
    }

    public static int users() {
        return Integer.getInteger("users", 100);
    }

    public static Duration rampDuration() {
        return Duration.ofMinutes(Integer.getInteger("ramp", 1));
    }

    public static Duration testDuration() {
        return Duration.ofMinutes(Integer.getInteger("duration", 10));
    }

    public static Duration minPause() {
        return Duration.ofSeconds(longPropertyOrDefault("minPauseSeconds", stressMode() ? 0 : 1));
    }

    public static Duration maxPause() {
        return Duration.ofSeconds(longPropertyOrDefault("maxPauseSeconds", stressMode() ? 1 : 3));
    }

    public static Duration shortPause() {
        return Duration.ofSeconds(longPropertyOrDefault("shortPauseSeconds", stressMode() ? 0 : 1));
    }

    public static Duration longPause() {
        return Duration.ofSeconds(longPropertyOrDefault("longPauseSeconds", stressMode() ? 1 : 2));
    }

    public static String adminUsername() {
        return Optional.ofNullable(System.getProperty("adminUsername")).orElse("admin");
    }

    public static String adminPassword() {
        return Optional.ofNullable(System.getProperty("adminPassword")).orElse("admin");
    }

    public static String userUsername() {
        return Optional.ofNullable(System.getProperty("userUsername")).orElse("user");
    }

    public static String userPassword() {
        return Optional.ofNullable(System.getProperty("userPassword")).orElse("user");
    }

    public static final Map<String, String> HEADERS_HTTP = Map.of("Accept", "application/json");

    public static final Map<String, String> HEADERS_HTTP_AUTHENTICATION =
            Map.of("Content-Type", "application/json", "Accept", "application/json");

    public static final Map<String, String> HEADERS_HTTP_AUTHENTICATED =
            Map.of("Accept", "application/json", "Authorization", "${access_token}");

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
