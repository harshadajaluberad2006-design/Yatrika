import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class YatrikaServer {

    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

    private static final Path WEB_ROOT =
            Paths.get(".").toAbsolutePath().normalize();

    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/yatrika_db";

    private static final String DB_USER =
            System.getenv().getOrDefault("YATRIKA_DB_USER", "root");

    private static final String DB_PASSWORD =
            System.getenv("YATRIKA_DB_PASSWORD");

    private static final String ADMIN_TOKEN =
            System.getenv("YATRIKA_ADMIN_TOKEN");

    /*
     * Allow only the local development origins you use.
     * Add another origin here only if you intentionally use it.
     */
    private static final String[] ALLOWED_ORIGINS = {
            "http://127.0.0.1:5500",
            "http://localhost:5500",
            "http://localhost:8080",
            "http://127.0.0.1:8080"
    };

    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(
                new InetSocketAddress(PORT), 0);

        server.createContext("/health", exchange -> {
            if (!addCorsHeaders(exchange)) {
                sendJson(exchange, 403,
                        "{\"success\":false,\"message\":\"Origin not allowed.\"}");
                return;
            }

            if (handlePreflight(exchange)) {
                return;
            }

            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendJson(exchange, 405,
                        "{\"success\":false,\"message\":\"Method not allowed.\"}");
                return;
            }

            sendJson(exchange, 200,
                    "{\"success\":true,\"message\":\"Yatrika server is running.\"}");
        });

        server.createContext(
                "/api/enquiries",
                YatrikaServer::handleEnquiry
        );

        server.createContext(
                "/api/admin/enquiries",
                YatrikaServer::handleAdminEnquiries
        );

        server.createContext("/", YatrikaServer::serveWebsite);

        server.setExecutor(null);
        server.start();

        System.out.println("====================================");
        System.out.println(" Yatrika server started successfully");
        System.out.println(" Website: http://localhost:" + PORT);
        System.out.println(" Contact: http://localhost:" + PORT + "/contact.html");
        System.out.println(" Health:  http://localhost:" + PORT + "/health");
        System.out.println(" Admin API: http://localhost:" + PORT + "/api/admin/enquiries");
        System.out.println("====================================");

        if (ADMIN_TOKEN == null || ADMIN_TOKEN.isBlank()) {
            System.out.println(
                    "WARNING: Set YATRIKA_ADMIN_TOKEN to enable admin access.");
        }

        try {
            new CountDownLatch(1).await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Server interrupted. Shutting down...");
            server.stop(0);
        }
    }

    /*
     * Add CORS headers only for explicitly allowed origins.
     * Return false when the supplied Origin is not allowed.
     */
    private static boolean addCorsHeaders(HttpExchange exchange) {

        String origin = exchange.getRequestHeaders().getFirst("Origin");

        if (origin == null || origin.isBlank()) {
            return true;
        }

        boolean allowed = false;

        for (String allowedOrigin : ALLOWED_ORIGINS) {
            if (allowedOrigin.equals(origin)) {
                allowed = true;
                break;
            }
        }

        if (!allowed) {
            return false;
        }

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Origin", origin);

        exchange.getResponseHeaders().set(
                "Vary", "Origin");

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Methods", "GET, POST, OPTIONS");

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Headers",
                "Content-Type, Accept, X-Admin-Token");

        exchange.getResponseHeaders().set(
                "Access-Control-Max-Age", "600");

        return true;
    }

    /*
     * Browsers send OPTIONS before cross-origin requests
     * containing the X-Admin-Token header.
     */
    private static boolean handlePreflight(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            return false;
        }

        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(204, -1);
        exchange.close();

        return true;
    }

    // Serve website files.
    private static void serveWebsite(HttpExchange exchange)
            throws IOException {

        String method = exchange.getRequestMethod();

        if (!method.equalsIgnoreCase("GET")
                && !method.equalsIgnoreCase("HEAD")) {
            sendText(exchange, 405, "Method not allowed.");
            return;
        }

        String requestPath = exchange.getRequestURI().getPath();

        if (requestPath.equals("/")) {
            requestPath = "/index.html";
        }

        Path file = WEB_ROOT.resolve(requestPath.substring(1)).normalize();

        if (!file.startsWith(WEB_ROOT)) {
            sendText(exchange, 403, "Access denied.");
            return;
        }

        if (!Files.isRegularFile(file)) {
            sendText(exchange, 404, "Page not found.");
            return;
        }

        String fileName = file.getFileName().toString().toLowerCase();
        String contentType;

        if (fileName.endsWith(".html")) {
            contentType = "text/html; charset=UTF-8";
        } else if (fileName.endsWith(".css")) {
            contentType = "text/css; charset=UTF-8";
        } else if (fileName.endsWith(".js")) {
            contentType = "application/javascript; charset=UTF-8";
        } else if (fileName.endsWith(".png")) {
            contentType = "image/png";
        } else if (fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (fileName.endsWith(".webp")) {
            contentType = "image/webp";
        } else if (fileName.endsWith(".svg")) {
            contentType = "image/svg+xml";
        } else if (fileName.endsWith(".ico")) {
            contentType = "image/x-icon";
        } else if (fileName.endsWith(".woff")) {
            contentType = "font/woff";
        } else if (fileName.endsWith(".woff2")) {
            contentType = "font/woff2";
        } else {
            sendText(exchange, 403, "File type not allowed.");
            return;
        }

        byte[] content = Files.readAllBytes(file);

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set(
                "X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");

        if (method.equalsIgnoreCase("HEAD")) {
            exchange.getResponseHeaders().set(
                    "Content-Length", String.valueOf(content.length));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(200, content.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(content);
        }
    }

    // Handle travel enquiry submissions.
    private static void handleEnquiry(HttpExchange exchange)
            throws IOException {

        if (!addCorsHeaders(exchange)) {
            sendJson(exchange, 403,
                    "{\"success\":false,\"message\":\"Origin not allowed.\"}");
            return;
        }

        if (handlePreflight(exchange)) {
            return;
        }

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendJson(exchange, 405,
                    "{\"success\":false,\"message\":\"Use POST to submit an enquiry.\"}");
            return;
        }

        String contentType = exchange.getRequestHeaders()
                .getFirst("Content-Type");

        if (contentType == null
                || !contentType.toLowerCase()
                .startsWith("application/x-www-form-urlencoded")) {
            sendJson(exchange, 415,
                    "{\"success\":false,\"message\":\"Unsupported form format.\"}");
            return;
        }

        try {
            byte[] requestBody =
                    exchange.getRequestBody().readNBytes(20001);

            if (requestBody.length > 20000) {
                sendJson(exchange, 413,
                        "{\"success\":false,\"message\":\"Form submission is too large.\"}");
                return;
            }

            String body = new String(
                    requestBody, StandardCharsets.UTF_8);

            Map<String, String> form = parseForm(body);

            String name = form.getOrDefault("name", "").trim();
            String email = form.getOrDefault("email", "").trim();
            String destination =
                    form.getOrDefault("destination", "").trim();
            String travelersText =
                    form.getOrDefault("travelers", "").trim();
            String dateText = form.getOrDefault("date", "").trim();
            String budget = form.getOrDefault("budget", "").trim();
            String message = form.getOrDefault("message", "").trim();

            if (name.isEmpty() || email.isEmpty()
                    || destination.isEmpty()
                    || travelersText.isEmpty()) {
                sendJson(exchange, 400,
                        "{\"success\":false,\"message\":\"Please fill in all required fields.\"}");
                return;
            }

            if (name.length() > 100 || email.length() > 150
                    || destination.length() > 100
                    || budget.length() > 50
                    || message.length() > 5000) {
                sendJson(exchange, 400,
                        "{\"success\":false,\"message\":\"One or more fields are too long.\"}");
                return;
            }

            if (!email.matches(
                    "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                sendJson(exchange, 400,
                        "{\"success\":false,\"message\":\"Please enter a valid email address.\"}");
                return;
            }

            int travelers;

            try {
                travelers = Integer.parseInt(travelersText);
            } catch (NumberFormatException e) {
                sendJson(exchange, 400,
                        "{\"success\":false,\"message\":\"Enter a valid number of travelers.\"}");
                return;
            }

            if (travelers < 1 || travelers > 100) {
                sendJson(exchange, 400,
                        "{\"success\":false,\"message\":\"Travelers must be between 1 and 100.\"}");
                return;
            }

            LocalDate travelDate = null;

            if (!dateText.isEmpty()) {
                try {
                    travelDate = LocalDate.parse(dateText);
                } catch (Exception e) {
                    sendJson(exchange, 400,
                            "{\"success\":false,\"message\":\"Enter a valid travel date.\"}");
                    return;
                }

                if (travelDate.isBefore(LocalDate.now())) {
                    sendJson(exchange, 400,
                            "{\"success\":false,\"message\":\"Travel date cannot be in the past.\"}");
                    return;
                }
            }

            if (DB_PASSWORD == null || DB_PASSWORD.isBlank()) {
                System.err.println(
                        "Set the YATRIKA_DB_PASSWORD environment variable.");

                sendJson(exchange, 500,
                        "{\"success\":false,\"message\":\"Database configuration is incomplete. Please contact the administrator.\"}");
                return;
            }

            String sql = "INSERT INTO travel_enquiries "
                    + "(name, email, destination, travelers, travel_date, budget, message) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (Connection connection = DriverManager.getConnection(
                         DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(1, name);
                statement.setString(2, email);
                statement.setString(3, destination);
                statement.setInt(4, travelers);

                if (travelDate == null) {
                    statement.setNull(5, java.sql.Types.DATE);
                } else {
                    statement.setDate(
                            5, java.sql.Date.valueOf(travelDate));
                }

                statement.setString(6, budget);
                statement.setString(7, message);

                statement.executeUpdate();
            }

            System.out.println(
                    "New Yatrika enquiry received for: " + destination);

            sendJson(exchange, 200,
                    "{\"success\":true,\"message\":\"Thank you! Your travel enquiry has been submitted successfully.\"}");

        } catch (SQLException e) {
            System.err.println("MySQL error: " + e.getMessage());

            sendJson(exchange, 500,
                    "{\"success\":false,\"message\":\"Could not save your enquiry. Please check the database connection and try again.\"}");

        } catch (Exception e) {
            System.err.println("Request error: " + e.getMessage());

            sendJson(exchange, 400,
                    "{\"success\":false,\"message\":\"Unable to process your enquiry. Please check your form and try again.\"}");
        }
    }

    // Return enquiries to an authenticated administrator.
    private static void handleAdminEnquiries(HttpExchange exchange)
            throws IOException {

        if (!addCorsHeaders(exchange)) {
            sendJson(exchange, 403,
                    "{\"success\":false,\"message\":\"Origin not allowed.\"}");
            return;
        }

        if (handlePreflight(exchange)) {
            return;
        }

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendJson(exchange, 405,
                    "{\"success\":false,\"message\":\"Use GET to view enquiries.\"}");
            return;
        }

        if (ADMIN_TOKEN == null || ADMIN_TOKEN.isBlank()) {
            sendJson(exchange, 503,
                    "{\"success\":false,\"message\":\"Admin API is not configured.\"}");
            return;
        }

        String suppliedToken = exchange.getRequestHeaders()
                .getFirst("X-Admin-Token");

        if (suppliedToken == null
                || !constantTimeEquals(ADMIN_TOKEN, suppliedToken)) {
            sendJson(exchange, 401,
                    "{\"success\":false,\"message\":\"Unauthorized.\"}");
            return;
        }

        if (DB_PASSWORD == null || DB_PASSWORD.isBlank()) {
            sendJson(exchange, 500,
                    "{\"success\":false,\"message\":\"Database configuration is incomplete.\"}");
            return;
        }

        String sql = "SELECT id, name, email, destination, travelers, "
                + "travel_date, budget, message, created_at "
                + "FROM travel_enquiries ORDER BY created_at DESC";

        StringBuilder json = new StringBuilder();
        json.append("{\"success\":true,\"enquiries\":[");

        try (Connection connection = DriverManager.getConnection(
                     DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {

            boolean first = true;

            while (results.next()) {
                if (!first) {
                    json.append(",");
                }

                first = false;

                json.append("{")
                        .append("\"id\":")
                        .append(results.getLong("id")).append(",")
                        .append("\"name\":")
                        .append(jsonString(results.getString("name"))).append(",")
                        .append("\"email\":")
                        .append(jsonString(results.getString("email"))).append(",")
                        .append("\"destination\":")
                        .append(jsonString(results.getString("destination"))).append(",")
                        .append("\"travelers\":")
                        .append(results.getInt("travelers")).append(",")
                        .append("\"travel_date\":")
                        .append(jsonString(results.getString("travel_date"))).append(",")
                        .append("\"budget\":")
                        .append(jsonString(results.getString("budget"))).append(",")
                        .append("\"message\":")
                        .append(jsonString(results.getString("message"))).append(",")
                        .append("\"created_at\":")
                        .append(jsonString(results.getString("created_at")))
                        .append("}");
            }

            json.append("]}");
            sendJson(exchange, 200, json.toString());

        } catch (SQLException e) {
            System.err.println(
                    "Admin enquiries MySQL error: " + e.getMessage());

            sendJson(exchange, 500,
                    "{\"success\":false,\"message\":\"Could not load enquiries.\"}");
        }
    }

    private static boolean constantTimeEquals(
            String expected, String actual) {

        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(StandardCharsets.UTF_8);

        if (a.length != b.length) {
            return false;
        }

        int difference = 0;

        for (int i = 0; i < a.length; i++) {
            difference |= a[i] ^ b[i];
        }

        return difference == 0;
    }

    private static String jsonString(String value) {

        if (value == null) {
            return "null";
        }

        StringBuilder escaped = new StringBuilder("\"");

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        escaped.append(
                                String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
            }
        }

        escaped.append("\"");
        return escaped.toString();
    }

    private static Map<String, String> parseForm(String body)
            throws IOException {

        Map<String, String> form = new HashMap<>();

        if (body.isEmpty()) {
            return form;
        }

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);

            String key = URLDecoder.decode(
                    parts[0], StandardCharsets.UTF_8);

            String value = parts.length > 1
                    ? URLDecoder.decode(
                            parts[1], StandardCharsets.UTF_8)
                    : "";

            form.put(key, value);
        }

        return form;
    }

    private static void sendJson(
            HttpExchange exchange, int statusCode, String json)
            throws IOException {

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");

        byte[] response = json.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(statusCode, response.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    private static void sendText(
            HttpExchange exchange, int statusCode, String message)
            throws IOException {

        exchange.getResponseHeaders().set(
                "Content-Type", "text/plain; charset=UTF-8");

        byte[] response = message.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(statusCode, response.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}
