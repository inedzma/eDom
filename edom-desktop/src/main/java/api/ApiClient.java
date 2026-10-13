package api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import service.Session;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class ApiClient {

    private static final String BAZNI_URL =
            System.getProperty("edom.api.url", "http://localhost:8080");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ApiClient() {}

    // ---------- javne metode ----------

    public static <T> T get(String putanja, Class<T> tip) {
        return posalji(zahtjev(putanja).GET(), JSON.constructType(tip));
    }

    public static <T> T get(String putanja, TypeReference<T> tip) {
        return posalji(zahtjev(putanja).GET(), JSON.constructType(tip));
    }

    public static <T> T post(String putanja, Object tijelo, Class<T> tip) {
        return posalji(zahtjev(putanja)
                .header("Content-Type", "application/json")
                .POST(kaoJson(tijelo)), JSON.constructType(tip));
    }

    public static <T> T put(String putanja, Object tijelo, Class<T> tip) {
        return posalji(zahtjev(putanja)
                .header("Content-Type", "application/json")
                .PUT(kaoJson(tijelo)), JSON.constructType(tip));
    }

    public static void delete(String putanja) {
        posalji(zahtjev(putanja).DELETE(), JSON.constructType(Void.class));
    }

    // ---------- interno ----------

    private static HttpRequest.Builder zahtjev(String putanja) {
        var b = HttpRequest.newBuilder(URI.create(BAZNI_URL + putanja))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json");

        String token = Session.getToken();
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        return b;
    }

    private static HttpRequest.BodyPublisher kaoJson(Object tijelo) {
        try {
            return HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(tijelo));
        } catch (IOException e) {
            throw new ApiException(0, "Greška pri pripremi zahtjeva.");
        }
    }

    private static <T> T posalji(HttpRequest.Builder b, JavaType tip) {
        HttpResponse<String> odg;
        try {
            odg = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ApiException(0, "Server nije dostupan. Provjerite vezu i pokušajte ponovo.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "Zahtjev je prekinut.");
        }

        int s = odg.statusCode();

        if (s >= 200 && s < 300) {
            if (tip.getRawClass() == Void.class || odg.body() == null || odg.body().isBlank()) {
                return null;
            }
            try {
                return JSON.readValue(odg.body(), tip);
            } catch (IOException e) {
                throw new ApiException(s, "Neočekivan odgovor servera.");
            }
        }

        throw new ApiException(s, porukaIz(odg.body(), s));
    }

    private static String porukaIz(String tijelo, int status) {
        try {
            var cvor = JSON.readTree(tijelo);
            if (cvor != null && cvor.hasNonNull("poruka")) {
                return cvor.get("poruka").asText();
            }
        } catch (IOException ignored) {
            // tijelo nije JSON, koristimo opštu poruku
        }
        return switch (status) {
            case 401 -> "Sesija je istekla. Prijavite se ponovo.";
            case 403 -> "Nemate pravo pristupa.";
            case 404 -> "Traženi podatak ne postoji.";
            default  -> "Greška na serveru (" + status + ").";
        };
    }

    public static String bazniUrl() {
        return BAZNI_URL;
    }
}