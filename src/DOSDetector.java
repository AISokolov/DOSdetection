import java.io.*;
import java.net.*;
import java.util.*;

public class DOSDetector {

    private static final int PORT = 8081;
    private static final double MAX_EMPTY_PACKET_THRESHOLD = 0.1;

    private List<PacketStatus> packets = new ArrayList<>();
    private int deniedBrokenPacketCount = 0;
    public int totalPacketCount = 0;
    public static boolean isRunning = true;
    public int acceptedPackets = 0;

    public static void main(String[] args) {
        DOSDetector detector = new DOSDetector();
        detector.startServer();
    }

    private static class PacketStatus {
        private final boolean isBroken;

        public PacketStatus(boolean isBroken) {
            this.isBroken = isBroken;
        }

        public boolean isBroken() {
            return isBroken;
        }
    }

    public void startServer() {
        while (isRunning) {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Server started on port " + PORT);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    handleClient(clientSocket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                System.out.println("Packet received: " + packet );

                if (DenyBrokenPackets(packet)) {
                    deniedBrokenPacketCount++;
                    System.out.println("Denied empty packet.");
                    continue;
                }

                boolean isEmpty = packet.isEmpty();
                packets.add(new PacketStatus(isEmpty));
                analyzeTraffic();
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

    //Deny empty packets only if the mean is above or equal to the threshold
    private boolean DenyBrokenPackets(String packet) {
        double meanBrokenPackets = getMeanBrokenPackets();
        boolean isEmpty = packet.isEmpty();

        if (meanBrokenPackets >= MAX_EMPTY_PACKET_THRESHOLD && isEmpty) {
            System.out.printf("Current mean: %.2f, Threshold: %.2f. Empty packet will be denied.\n",
                    meanBrokenPackets, MAX_EMPTY_PACKET_THRESHOLD);
            return true;
        }

        return false;
    }

    private void analyzeTraffic() {
        double meanBrokenPackets = getMeanBrokenPackets();

        System.out.printf("Mean broken packets: %.2f%n", meanBrokenPackets);
        System.out.println("Total packets received: " + totalPacketCount);
        System.out.println("Total denied packets: " + deniedBrokenPacketCount);

        if (meanBrokenPackets >= MAX_EMPTY_PACKET_THRESHOLD) {
            System.out.println("Denying empty packets");
        } else {
            System.out.println("Accepted.");
            acceptedPackets++;
        }
    }

    public double getMeanBrokenPackets() {
        long brokenPackets = packets.stream().filter(packets -> packets.isBroken).count();
        if (packets.isEmpty()){
            return 0.0;
        } else {
            return (double) brokenPackets / packets.size();
        }
    }
}