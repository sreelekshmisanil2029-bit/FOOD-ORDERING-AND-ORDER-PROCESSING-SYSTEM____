import java.util.ArrayList;
import java.util.List;

/** Model classes for an order and its individual food items. */
public class FoodOrder {
    public static class Line {
        private final int foodId;
        private final String name;
        private final double price;
        private final int quantity;

        public Line(int foodId, String name, double price, int quantity) {
            this.foodId = foodId;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }

        public int getFoodId() { return foodId; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }
        public double getSubtotal() { return price * quantity; }

        public String toJson() {
            return "{\"foodId\":" + foodId
                + ",\"name\":\"" + jsonEscape(name) + "\""
                + ",\"price\":" + price
                + ",\"quantity\":" + quantity
                + ",\"subtotal\":" + getSubtotal() + "}";
        }
    }

    private final int orderId;
    private final String customerName;
    private final List<Line> items;
    private String status = "PENDING";

    public FoodOrder(int orderId, String customerName, List<Line> items) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.items = new ArrayList<>(items);
    }

    public int getOrderId() { return orderId; }
    public String getCustomerName() { return customerName; }
    public List<Line> getItems() { return new ArrayList<>(items); }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTotal() {
        return items.stream().mapToDouble(Line::getSubtotal).sum();
    }

    public String toJson() {
        StringBuilder itemJson = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) itemJson.append(",");
            itemJson.append(items.get(i).toJson());
        }
        itemJson.append("]");
        return "{\"orderId\":" + orderId
            + ",\"customerName\":\"" + jsonEscape(customerName) + "\""
            + ",\"status\":\"" + status + "\""
            + ",\"items\":" + itemJson
            + ",\"total\":" + String.format(java.util.Locale.US, "%.2f", getTotal()) + "}";
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", "\\n").replace("\r", "\\r");
    }
}
