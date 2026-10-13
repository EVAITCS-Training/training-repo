package com.evaitcs.advanced.exceptions;

public class DependecyInversion {

    public static void main(String[] args) {
        NotificationService emailService = new EmailNotificationService();
        NotificationService smsService = new SmsNotificationService();

        OrderProcessor emailProcessor = new OrderProcessor(emailService);
        OrderProcessor smsProcessor = new OrderProcessor(smsService);

        emailProcessor.placeOrder("ORD-1001");
        smsProcessor.placeOrder("ORD-1002");
    }

    /*
     * DIP (Dependency Inversion Principle):
     * 1) High-level module (OrderProcessor) does NOT depend on low-level modules directly.
     * 2) Both depend on the abstraction (NotificationService).
     * 3) Low-level details (Email/SMS) can be swapped without changing OrderProcessor.
     */

    interface NotificationService {
        void send(String message);
    }

    static class EmailNotificationService implements NotificationService {
        @Override
        public void send(String message) {
            System.out.println("[EMAIL] " + message);
        }
    }

    static class SmsNotificationService implements NotificationService {
        @Override
        public void send(String message) {
            System.out.println("[SMS] " + message);
        }
    }

    static class OrderProcessor {
        private final NotificationService notificationService;

        OrderProcessor(NotificationService notificationService) {
            this.notificationService = notificationService;
        }

        void placeOrder(String orderId) {
            String message = "Order placed successfully: " + orderId;
            notificationService.send(message);
        }
    }
}
