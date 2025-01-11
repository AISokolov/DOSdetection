import javafx.application.Platform;

import java.io.*;
import java.net.*;
import java.util.Random;

public class PacketSender {

    private static final String HOST = "localhost";
    private static final int PORT = 8081;
    private static final int MAX_PACKET_COUNT = 20;
    private static final int MIN_PACKET_COUNT = 5;
    public static boolean isRunning = true;

    public static void main(String[] args) {
        sendPacket();
    }

    public static void sendPacket() {
        Random random = new Random();
        while (isRunning) {
            int packetCount = random.nextInt(MAX_PACKET_COUNT - MIN_PACKET_COUNT + 1) + MIN_PACKET_COUNT;
            for (int i = 0; i < packetCount; i++) {
                if (random.nextBoolean()) {
                    sendNormalPacket();
                } else {
                    sendEmptyPacket();
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public void stopSending() {
        isRunning = false;
    }
    private static void sendNormalPacket() {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            writer.println("normal packet");
            System.out.println("Sent a normal packet.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendEmptyPacket() {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            writer.println("");
            System.out.println("Sent an empty packet.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
