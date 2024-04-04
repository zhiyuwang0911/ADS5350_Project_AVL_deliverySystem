import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class gatorDelivery {

    public static AvlTree<Order> priorityTree = new AvlTree<>((o1, o2) -> {
        if (Double.compare(o2.getPriority(), o1.getPriority()) == 0) {
            return o1.getOrderId() - o2.getOrderId();
        }
        return Double.compare(o2.getPriority(), o1.getPriority());
    });
    public static AvlTree<Order> etaPriorityTree = new AvlTree<>(Comparator.comparingInt(Order::getETA));
    // log canceled order
    public static Set<Integer> canceled = new HashSet<>();
    // log delivered order
    public static Set<Integer> delivered = new HashSet<>();
    // lock the delivering order
    public static Order deliveringOrder = null;
    // log end time of previous delivering order
    public static int preDeliveringOrderEndTime = 0;

    public static void main(String[] args) {
        String inputFilename = args[0];
        try (BufferedReader br = new BufferedReader(new FileReader(inputFilename));
             BufferedWriter bw = new BufferedWriter(new FileWriter(inputFilename.replace(".txt", "_output_file.txt")))) {
            String line, result;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                result = executeLine(line);
//                System.out.print(result);
                bw.write(result);
                bw.flush();
            }
        } catch (IOException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    //execute each line
    public static String executeLine(String line) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String methodName;
        String[] strs = line.split("[(), ]+");
        Object[] arguments = new Object[strs.length - 1];
        Class<?>[] classes = new Class[strs.length - 1];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = Integer.valueOf(strs[i + 1]);
            classes[i] = int.class;
        }

        methodName = strs[0];
        // Using reflection to automatically find execution methods
        return (String) gatorDelivery.class.getMethod(methodName, classes).invoke(null, arguments);
    }

    // Convert tree to list and search
    public static String print(int orderId) {
        List<Order> list = priorityTree.tolist();
        String output = null;
        if (deliveringOrder != null && deliveringOrder.getOrderId() == orderId) {
            output = String.format("[%d, %d, %d, %d, %d]\n",
                    deliveringOrder.getOrderId(), deliveringOrder.getCreateTime(), deliveringOrder.getOrderValue(), deliveringOrder.getDeliveryTime(), deliveringOrder.getETA());
        } else {
            for (Order order : list) {
                if (order.getOrderId() == orderId) {
                    output = String.format("[%d, %d, %d, %d, %d]\n",
                            order.getOrderId(), order.getCreateTime(), order.getOrderValue(), order.getDeliveryTime(), order.getETA());
                }
            }
        }
        return output;
    }

    // Convert tree to list and search
    public static String print(int time1, int time2) {
        List<Order> list = etaPriorityTree.tolist();
        List<String> orderIds = new ArrayList<>();
        if (deliveringOrder != null && deliveringOrder.getETA() >= time1 && deliveringOrder.getETA() <= time2) {
            orderIds.add(String.valueOf(deliveringOrder.getOrderId()));
        }
        String result;
        if (list.isEmpty() && orderIds.isEmpty()) {
            result = "There are no orders in that time period.\n";
        } else {
            for (Order order : list) {
                if (order.getETA() >= time1) {
                    if (order.getETA() <= time2) {
                        orderIds.add(String.valueOf(order.getOrderId()));
                    } else {
                        break;
                    }
                }
            }
            if (orderIds.isEmpty()) {
                result = "There are no orders in that time period\n";
            } else {
                result = "[" + String.join(",", orderIds) + "]\n";
            }
        }
        return result;
    }

    // By counting the number of unsent orders before orderid in the list
    public static String getRankOfOrder(int orderId) {
        if (deliveringOrder != null && deliveringOrder.getOrderId() == orderId) {
            return String.format("Order %d will be delivered after %d orders.\n", orderId, 0);
        }
        List<Order> list = etaPriorityTree.tolist();
        int i;
        for (i = 0; i < list.size(); i++) {
            if (list.get(i).getOrderId() == orderId) {
                return String.format("Order %d will be delivered after %d orders.\n", orderId, i + (deliveringOrder == null ? 0 : 1));
            }
        }
        return "";
    }

    // First, collect delivered orders. Then add the new order to the
    // priority tree. Finally, update the eta of order in the tree and reconstruct the eta priority tree.
    public static String createOrder(int orderId, int currentSystemTime, int orderValue, int deliveryTime) {
        StringBuilder delivered = new StringBuilder();
        List<Order> list;
        int orderEndTime = preDeliveringOrderEndTime;
        if (deliveringOrder != null && deliveringOrder.getETA() <= currentSystemTime) {
            delivered.append(String.format("Order %d has been delivered at time %d\n", deliveringOrder.getOrderId(), deliveringOrder.getETA()));
            orderEndTime = deliveringOrder.getETA() + deliveringOrder.getDeliveryTime();
            preDeliveringOrderEndTime = orderEndTime;
            gatorDelivery.delivered.add(deliveringOrder.getOrderId());
            deliveringOrder = null;
        }
        list = etaPriorityTree.tolist();
        for (Order order : list) {
            if (order.getETA() <= currentSystemTime) {
                orderEndTime = order.getETA() + order.getDeliveryTime();
                priorityTree.remove(order);
                etaPriorityTree.remove(order);
                delivered.append(String.format("Order %d has been delivered at time %d\n", order.getOrderId(), order.getETA()));
                gatorDelivery.delivered.add(order.getOrderId());
            } else {
                break;
            }
        }

        if (deliveringOrder == null) {
            deliveringOrder = priorityTree.findMin();
            if (deliveringOrder != null && deliveringOrder.getETA() - deliveringOrder.getDeliveryTime() < currentSystemTime) {
                priorityTree.remove(deliveringOrder);
                etaPriorityTree.remove(deliveringOrder);
                orderEndTime = deliveringOrder.getETA() + deliveringOrder.getDeliveryTime();
            } else {
                deliveringOrder = null;
            }
        } else {
            orderEndTime = deliveringOrder.getETA() + deliveringOrder.getDeliveryTime();
        }
        Order newOrder = new Order(orderId, currentSystemTime, orderValue, deliveryTime);
        priorityTree.insert(newOrder);
        list = priorityTree.tolist();
        String updated = "";
        if (list.get(list.size() - 1).getOrderId() != orderId) {
            List<String> updateList = new ArrayList<>();
            etaPriorityTree.makeEmpty();
            for (Order order : list) {
                int oldETA = order.getETA();
                order.setETA(Math.max(orderEndTime, order.getCreateTime()) + order.getDeliveryTime());
                orderEndTime = order.getETA() + order.getDeliveryTime();
                if (order.getOrderId() != orderId && order.getETA() != oldETA) {
                    updateList.add(String.format("%d:%d", order.getOrderId(), order.getETA()));
                }
                etaPriorityTree.insert(order);
            }
            if (!updateList.isEmpty()) {
                updated = "Updated ETAs: [" + String.join(",", updateList) + "]\n";
            }
        } else {
            if (list.size() == 1) {
                if (deliveringOrder == null) {
                    newOrder.setETA(currentSystemTime + deliveryTime);
                } else {
                    newOrder.setETA(Math.max(orderEndTime, currentSystemTime) + deliveryTime);
                }
            } else {
                int lastOrderEndTime = list.get(list.size() - 2).getETA() + list.get(list.size() - 2).getDeliveryTime();
                newOrder.setETA(Math.max(lastOrderEndTime, currentSystemTime) + deliveryTime);
            }
            etaPriorityTree.insert(newOrder);
        }
        String created = String.format("Order %d has been created - ETA: %d\n", newOrder.getOrderId(), newOrder.getETA());

        if (deliveringOrder == null) {
            deliveringOrder = priorityTree.findMin();
            if (deliveringOrder != null && deliveringOrder.getETA() - deliveringOrder.getDeliveryTime() < currentSystemTime) {
                priorityTree.remove(deliveringOrder);
                etaPriorityTree.remove(deliveringOrder);
            } else {
                deliveringOrder = null;
            }
        }

        return created + updated + delivered;
    }

    // Collect delivered orders. Check whether the orderid is being sent or has been delivered.
    // If it is not delivered, the orderid will be deleted from the priority
    // tree and the eta of the other order after the orderid will be recalculated.
    public static String cancelOrder(int orderId, int currentSystemTime) {
        StringBuilder delivered = new StringBuilder();
        List<Order> list;
        int orderEndTime;
        if (deliveringOrder != null && deliveringOrder.getETA() <= currentSystemTime) {
            delivered.append(String.format("Order %d has been delivered at time %d\n", deliveringOrder.getOrderId(), deliveringOrder.getETA()));
            orderEndTime = deliveringOrder.getETA() + deliveringOrder.getDeliveryTime();
            preDeliveringOrderEndTime = orderEndTime;
            gatorDelivery.delivered.add(deliveringOrder.getOrderId());
            deliveringOrder = null;
        }
        list = etaPriorityTree.tolist();
        for (Order order : list) {
            if (order.getETA() <= currentSystemTime) {
                priorityTree.remove(order);
                etaPriorityTree.remove(order);
                delivered.append(String.format("Order %d has been delivered at time %d\n", order.getOrderId(), order.getETA()));
                gatorDelivery.delivered.add(order.getOrderId());
            } else {
                break;
            }
        }

        List<String> updateList = new ArrayList<>();
        if (deliveringOrder != null) {
            if (deliveringOrder.getOrderId() == orderId) {
                return "Cannot cancel. Order " + orderId + " has already been delivered.\n"+delivered;
            }
        }

        if (gatorDelivery.delivered.contains(orderId)) {
            return "Cannot cancel. Order " + orderId + " has already been delivered.\n";
        }
        list = priorityTree.tolist();
        int i = 0;
        while (list.get(i).getOrderId() != orderId) {
            i++;
        }
        Order cancelOrder = list.get(i);
        if (cancelOrder.getETA() <= currentSystemTime || cancelOrder.getETA() - cancelOrder.getDeliveryTime() <= currentSystemTime) {
            return "Cannot cancel. Order " + orderId + " has already been delivered.\n"+delivered;
        }

        orderEndTime = list.get(0).getETA() - list.get(0).getDeliveryTime();
        priorityTree.remove(list.get(i));
        list = priorityTree.tolist();

        etaPriorityTree.makeEmpty();
        for (i = 0; i < list.size(); i++) {
            Order order = list.get(i);
            int oldEta=order.getETA();
            order.setETA(Math.max(orderEndTime, order.getCreateTime()) + order.getDeliveryTime());
            orderEndTime = order.getETA() + order.getDeliveryTime();
            if(order.getETA()!=oldEta){
                updateList.add(String.format("%d: %d", order.getOrderId(), order.getETA()));
            }
            etaPriorityTree.insert(order);
        }
        String canceled = String.format("Order %d has been canceled\n", orderId);
        gatorDelivery.canceled.add(orderId);
        if(updateList.isEmpty()){
            return canceled;
        }
        String updated = "Updated ETAs: [" + String.join(", ", updateList) + "]\n";

        return canceled + updated+delivered;
    }

    // Collect delivered orders. Check whether the orderid is being sent or has been sent. If it is not delivered,
    // delete the orderid from the priority tree. Update newDeliveryTime and then
    // rejoin the priority tree. Finally, recalculate the eta of the order after the orderid.
    public static String updateTime(int orderId, int currentSystemTime, int newDeliveryTime) {
        StringBuilder delivered = new StringBuilder();
        List<Order> list;
        int orderEndTime;
        if (deliveringOrder != null && deliveringOrder.getETA() <= currentSystemTime) {
            delivered.append(String.format("Order %d has been delivered at time %d\n", deliveringOrder.getOrderId(), deliveringOrder.getETA()));
            orderEndTime = deliveringOrder.getETA() + deliveringOrder.getDeliveryTime();
            preDeliveringOrderEndTime = orderEndTime;
            gatorDelivery.delivered.add(deliveringOrder.getOrderId());
            deliveringOrder = null;
        }
        list = etaPriorityTree.tolist();
        for (Order order : list) {
            if (order.getETA() <= currentSystemTime) {
                priorityTree.remove(order);
                etaPriorityTree.remove(order);
                delivered.append(String.format("Order %d has been delivered at time %d\n", order.getOrderId(), order.getETA()));
                gatorDelivery.delivered.add(order.getOrderId());
            } else {
                break;
            }
        }

        List<String> updateList = new ArrayList<>();
        if (deliveringOrder != null) {
            if (deliveringOrder.getOrderId() == orderId) {
                return "Cannot update. Order " + orderId + " has already been delivered.\n"+delivered;
            }
        }

        if (gatorDelivery.delivered.contains(orderId)) {
            return "Cannot update. Order " + orderId + " has already been delivered.\n"+delivered;
        }
        list = priorityTree.tolist();
        int i = 0;
        while (list.get(i).getOrderId() != orderId) {
            i++;
        }
        Order updateOrder = list.get(i);
        if (updateOrder.getETA() <= currentSystemTime || updateOrder.getETA() - updateOrder.getDeliveryTime() <= currentSystemTime) {
            return "Cannot update. Order " + orderId + " has already been delivered.\n"+delivered;
        }

        orderEndTime = list.get(0).getETA() - list.get(0).getDeliveryTime();
        priorityTree.remove(list.get(i));
        updateOrder.setDeliveryTime(newDeliveryTime);
        priorityTree.insert(updateOrder);
        list = priorityTree.tolist();

        etaPriorityTree.makeEmpty();
        for (i = 0; i < list.size(); i++) {
            Order order = list.get(i);
            int oldEta=order.getETA();
            order.setETA(Math.max(orderEndTime, order.getCreateTime()) + order.getDeliveryTime());
            orderEndTime = order.getETA() + order.getDeliveryTime();
            if(order.getETA()!=oldEta){
                updateList.add(String.format("%d:%d", order.getOrderId(), order.getETA()));
            }
            etaPriorityTree.insert(order);
        }
        if(updateList.isEmpty()){
            return "";
        }
        String updated = "Updated ETAs: [" + String.join(",", updateList) + "]\n";

        return updated+delivered;
    }

    // delivery the remaining order
    public static String Quit() {
        List<Order> list = priorityTree.tolist();
        StringBuilder delivered = new StringBuilder();
        if (deliveringOrder != null) {
            delivered.append(String.format("Order %d has been delivered at time %d\n", deliveringOrder.getOrderId(), deliveringOrder.getETA()));
        }
        for (Order order : list) {
            delivered.append(String.format("Order %d has been delivered at time %d\n", order.getOrderId(), order.getETA()));
        }
        return delivered.toString();
    }
}