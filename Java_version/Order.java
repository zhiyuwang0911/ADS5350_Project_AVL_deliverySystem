public class Order {
    private int orderId;
    private int createTime;
    private int orderValue;
    private int deliveryTime;
    private int ETA;

    public Order() {
    }

    public Order(int orderId, int currentSystemTime, int orderValue, int deliveryTime) {
        this.orderId = orderId;
        this.createTime = currentSystemTime;
        this.orderValue = orderValue;
        this.deliveryTime = deliveryTime;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public double getPriority() {
        return 0.3 * (orderValue / 50) - 0.7 * createTime;
    }

    public int getOrderValue() {
        return orderValue;
    }

    public void setOrderValue(int orderValue) {
        this.orderValue = orderValue;
    }

    public int getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(int deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public int getETA() {
        return ETA;
    }

    public void setETA(int ETA) {
        this.ETA = ETA;
    }

    public int getCreateTime() {
        return createTime;
    }

    public void setCreateTime(int createTime) {
        this.createTime = createTime;
    }
}
