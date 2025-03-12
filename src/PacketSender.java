import java.io.*;
import java.net.*;
import java.util.Random;

public class PacketSender {

    private static final String HOST = "localhost";
    private static final int PORT = 8081;


    private static int maxPacketCount;
    private static int minPacketCount;
    public static boolean isRunning = true;
    private static int sleepTimeLBorder;
    private static int sleepTimeUBorder;

    private static Random random = new Random();

    private final App app;

    public PacketSender(App app, int maxPacketCount, int minPacketCount, int sleepTimeLBorder, int sleepTimeUBorder) {
        this.app = app;
        this.maxPacketCount = maxPacketCount;
        this.minPacketCount = minPacketCount;
        this.sleepTimeLBorder = sleepTimeLBorder;
        this.sleepTimeUBorder = sleepTimeUBorder;
    }

    public static void sendPacket() {
        isRunning = true;
        while (isRunning) {
            int packetCount = random.nextInt(maxPacketCount - minPacketCount + 1) + minPacketCount;
            for (int i = 0; i < packetCount && isRunning; i++) {
                if (random.nextBoolean()) {
                    sendNormalPacket();
                } else {
                    sendEmptyPacket();
                }
                try {
                    if (!App.isAttcked) {
                        Thread.sleep(333);
                    } else {
                        Thread.sleep(random.nextInt(sleepTimeUBorder - sleepTimeLBorder + 1) + sleepTimeLBorder);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
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

    public void stopSending() {
        isRunning = false;
    }
}
