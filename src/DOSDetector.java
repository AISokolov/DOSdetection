import java.io.*;
import java.net.*;
import java.util.*;

public class DOSDetector {
    private static final int PORT = 8081;
    private static final double DENY_UPPER_LIMIT = 0.6;
    private static final double DENY_LOWER_LIMIT = 0.5;

    private boolean throttling = false;

    private static int LIMIT_MEM_USAGE = 100;

    private int totalPacketCount = 0;
    private int acceptedPackets = 0;
    private boolean isRunning = true;
    private ServerSocket server;

    private final List<String> recentPackets = new ArrayList<>();

    public void startServer() {
        try {
            server = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);
            while (isRunning) {
                Socket clientSocket = server.accept();
                handleClient(clientSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        isRunning = false;
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String packet;
            while ((packet = reader.readLine()) != null) {
                    totalPacketCount++;
                    if (denyBrokenPackets(packet)) {
                        continue;
                    }
                    if (!packet.isEmpty()) {
                        acceptedPackets++;
                    }
                    //logging
                    updateTrafficData();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean denyBrokenPackets(String packet) {
        boolean isEmpty = packet.isEmpty();
            recentPackets.add(packet);

            //not overflow the buffer
            if (recentPackets.size() > LIMIT_MEM_USAGE) {
                recentPackets.remove(0);
            }

            double loadPercentage = calculateLoad();


            if (loadPercentage >= DENY_UPPER_LIMIT) {
                throttling = true;
                System.out.println("Throttling activated. Denying empty packets.");
            } else if (loadPercentage <= DENY_LOWER_LIMIT) {
                throttling = false;
                System.out.println("Throttling deactivated. Accepting all packets.");
            }
            if (throttling) {
                System.out.println("Denying packets due to throttling.");
                return true;
            }
        return false;
    }

    private double calculateLoad() {
            //filter for valid no empty packets
            long acceptedCount = recentPackets.stream().filter(p -> !p.isEmpty()).count();
            if (recentPackets.isEmpty()) {
                return 0;
            }else{
                return (double) acceptedCount / recentPackets.size();
            }
    }

    private void updateTrafficData() {
        System.out.printf("Total Packets: %d, Accepted Packets: %d%n", totalPacketCount, acceptedPackets);
        System.out.printf("Current Load: %.2f%%%n", calculateLoad() * 100);
    }

    public int getTotalPacketCount() {
        return totalPacketCount;
    }

    public int getAcceptedPackets() {
        return acceptedPackets;
    }

    public double getLoadPercentage() {
        return calculateLoad();
    }
}
