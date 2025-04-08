import java.io.*;
import java.net.*;
import java.util.Random;

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

    public PacketSender(InstanceController controller, int maxPacketCount, int minPacketCount,
                        int sleepTimeLBorder, int sleepTimeUBorder) {
        this.controller = controller;
        this.maxPacketCount = maxPacketCount;
        this.minPacketCount = minPacketCount;
        this.sleepTimeLBorder = sleepTimeLBorder;
        this.sleepTimeUBorder = sleepTimeUBorder;
        this.instancePortOffset = controller.getInstanceNumber() - 1; // Port offset based on instance number
    }

    public void sendPacket() {
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
                    if (!controller.isAttacked) {
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

    private synchronized void sendNormalPacket() {
        try (Socket socket = new Socket(HOST, PORT + instancePortOffset);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            writer.println("normal packet");
            System.out.println("Instance " + controller.getInstanceNumber() + " sent a normal packet.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void sendEmptyPacket() {
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