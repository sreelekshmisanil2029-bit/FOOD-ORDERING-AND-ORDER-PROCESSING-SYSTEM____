import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Food Ordering and Order Processing System.
 * Uses Java's built-in HTTP server; no external Java libraries required.
 * Keep index.html and style.css in the same folder as this file.
 */
public class FoodOrderingServer {
    private static final int PORT = 8080;
    private static final List<FoodOrder> allOrders = new ArrayList<>();
    private static final Queue<FoodOrder> pendingOrders = new LinkedList<>();
    private static int nextOrderId = 1001;

    private static final String[] FOOD_NAMES = {
        "Classic Burger", "Margherita Pizza", "Veg Sandwich",
        "Fried Rice", "Fresh Juice", "French Fries"
    };
    private static final double[] FOOD_PRICES = {100, 250, 80, 150, 50, 70};

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", FoodOrderingServer::handle);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Food Ordering System is running.");
        System.out.println("Open http://localhost:" + PORT + " in your browser.");
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.equals("/") && method.equals("GET")) {
                sendFile(exchange, "index.html", "text/html; charset=UTF-8");
            } else if (path.equals("/style.css") && method.equals("GET")) {
                sendFile(exchange, "style.css", "text/css; charset=UTF-8");
            } else if (path.equals("/api/menu") && method.equals("GET")) {
                sendJson(exchange, menuJson());
            } else if (path.equals("/api/orders") && method.equals("GET")) {
                sendJson(exchange, ordersJson(allOrders));
            } else if (path.equals("/api/pending") && method.equals("GET")) {
                sendJson(exchange, ordersJson(new ArrayList<>(pendingOrders)));
            } else if (path.equals("/api/summary") && method.equals("GET")) {
                sendJson(exchange, summaryJson());
            } else if (path.equals("/api/place") && method.equals("POST")) {
                placeOrder(exchange);
            } else if (path.equals("/api/process") && method.equals("POST")) {
                processOrder(exchange);
            } else if (path.startsWith("/api/orders/") && method.equals("GET")) {
                showOrder(exchange, path.substring("/api/orders/".length()));
            } else {
                send(exchange, 404, "text/plain; charset=UTF-8", "Not found");
            }
        } catch (Exception ex) {
            sendJsonStatus(exchange, 400, "{\"error\":\"" + escape(ex.getMessage()) + "\"}");
        }
    }

    private static void sendFile(HttpExchange exchange, String fileName, String contentType) throws IOException {
        Path path = Path.of(fileName);
        if (!Files.exists(path)) {
            send(exchange, 404, "text/plain; charset=UTF-8",
                    "Cannot find " + fileName + ". Keep it in the same folder as the Java files.");
            return;
        }
        send(exchange, 200, contentType, Files.readString(path, StandardCharsets.UTF_8));
    }

    private static synchronized void placeOrder(HttpExchange exchange) throws IOException {
        Map<String, String> form = parseForm(exchange);
        String customer = form.getOrDefault("customer", "").trim();
        if (customer.isEmpty()) throw new IllegalArgumentException("Please enter the customer's name.");

        List<FoodOrder.Line> lines = new ArrayList<>();
        for (int i = 0; i < FOOD_NAMES.length; i++) {
            String raw = form.getOrDefault("q" + (i + 1), "0");
            int quantity;
            try {
                quantity = Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Quantity must be a whole number.");
            }
            if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative.");
            if (quantity > 0) {
                lines.add(new FoodOrder.Line(i + 1, FOOD_NAMES[i], FOOD_PRICES[i], quantity));
            }
        }
        if (lines.isEmpty()) throw new IllegalArgumentException("Please choose at least one food item.");

        FoodOrder order = new FoodOrder(nextOrderId++, customer, lines);
        allOrders.add(order);
        pendingOrders.offer(order);
        sendJsonStatus(exchange, 201, order.toJson());
    }

    private static synchronized void processOrder(HttpExchange exchange) throws IOException {
        FoodOrder order = pendingOrders.poll();
        if (order == null) {
            sendJsonStatus(exchange, 400, "{\"error\":\"There are no pending orders to process.\"}");
            return;
        }
        order.setStatus("COMPLETED");
        sendJson(exchange, order.toJson());
    }

    private static synchronized void showOrder(HttpExchange exchange, String idText) throws IOException {
        int id;
        try {
            id = Integer.parseInt(idText);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid order ID.");
        }
        for (FoodOrder order : allOrders) {
            if (order.getOrderId() == id) {
                sendJson(exchange, order.toJson());
                return;
            }
        }
        sendJsonStatus(exchange, 404, "{\"error\":\"Order not found.\"}");
    }

    private static String menuJson() {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < FOOD_NAMES.length; i++) {
            if (i > 0) out.append(",");
            out.append("{\"id\":").append(i + 1)
               .append(",\"name\":\"").append(escape(FOOD_NAMES[i]))
               .append("\",\"price\":").append(FOOD_PRICES[i]).append("}");
        }
        return out.append("]").toString();
    }

    private static synchronized String ordersJson(List<FoodOrder> orders) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < orders.size(); i++) {
            if (i > 0) out.append(",");
            out.append(orders.get(i).toJson());
        }
        return out.append("]").toString();
    }

    private static synchronized String summaryJson() {
        int completed = 0;
        double sales = 0;
        for (FoodOrder order : allOrders) {
            if ("COMPLETED".equals(order.getStatus())) {
                completed++;
                sales += order.getTotal();
            }
        }
        return "{\"totalOrders\":" + allOrders.size()
                + ",\"pendingOrders\":" + pendingOrders.size()
                + ",\"completedOrders\":" + completed
                + ",\"totalSales\":" + String.format(Locale.US, "%.2f", sales) + "}";
    }

    private static Map<String, String> parseForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> values = new HashMap<>();
        if (body.isEmpty()) return values;
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1
                    ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }
        return values;
    }

    private static void sendJson(HttpExchange exchange, String json) throws IOException {
        send(exchange, 200, "application/json; charset=UTF-8", json);
    }

    private static void sendJsonStatus(HttpExchange exchange, int status, String json) throws IOException {
        send(exchange, status, "application/json; charset=UTF-8", json);
    }

    private static void send(HttpExchange exchange, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                   .replace("\n", "\\n").replace("\r", "\\r");
    }
}
