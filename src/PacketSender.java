import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PacketSender {
    private static final String HOST = "localhost";
    private static final int PORT = 8081;

    private int maxPacketCount;
    private int minPacketCount;
    public boolean isRunning = true;
    private int sleepTimeLBorder;
    private int sleepTimeUBorder;
    private Random random = new Random();
    private final InstanceController controller;
    private int instancePortOffset;
    private  int defaultSleepTime; // Default sleep time in milliseconds
    private ExecutorService packetSendExecutor = Executors.newFixedThreadPool(4); // или другое число потоков

    public PacketSender(InstanceController controller, int maxPacketCount, int minPacketCount,
                        int sleepTimeLBorder, int sleepTimeUBorder, int defaultSleepTime) {
        this.controller = controller;
        this.maxPacketCount = maxPacketCount;
        this.minPacketCount = minPacketCount;
        this.sleepTimeLBorder = sleepTimeLBorder;
        this.sleepTimeUBorder = sleepTimeUBorder;
        this.instancePortOffset = controller.getInstanceNumber() - 1; // 8081 + (instNum - 1)
        this.defaultSleepTime = defaultSleepTime;
    }

    public void sendPacket() {
        isRunning = true;
        while (isRunning) {
            int packetCount = random.nextInt(maxPacketCount - minPacketCount + 1) + minPacketCount;
            for (int i = 0; i < packetCount && isRunning; i++) {
                if (random.nextBoolean()) {
                    packetSendExecutor.submit(() -> sendNormalPacket());
                } else {
                    packetSendExecutor.submit(() -> sendEmptyPacket());
                }
                try {
                    if (!controller.isAttacked) {
                        Thread.sleep(defaultSleepTime);
                    } else {
                        Thread.sleep(random.nextInt(sleepTimeUBorder - sleepTimeLBorder + 1) + sleepTimeLBorder);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendNormalPacket() {
        try (Socket socket = new Socket(HOST, PORT + instancePortOffset);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            writer.println("normal packet");
            System.out.println("Instance " + controller.getInstanceNumber() + " sent a normal packet.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendEmptyPacket() {
        try (Socket socket = new Socket(HOST, PORT + instancePortOffset);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            writer.println("");
            System.out.println("Instance " + controller.getInstanceNumber() + " sent an empty packet.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopSending() {
        isRunning = false;
    }
}