import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javafx.application.Platform;
import javafx.scene.control.Alert;

public class DOSDetector {
    private static final int PORT = 8081;
    private static final int MOVING_AVERAGE_WINDOW = 10; // Window size for moving average


    private static double warningThreshold;// Say 75% of packets are empty or full
    private static int maximumPacketsLimit;
    private static double timeGap;
    private int totalPacketCount = 0;
    private int acceptedPackets = 0;
    private boolean isRunning = true;
    private ServerSocket server;

    private final List<Double> movingAverageList = new ArrayList<>(); // Store moving average values

    private long startTime = System.currentTimeMillis(); // Track start time for performance metrics

    private final App app;

    public DOSDetector(App app, double warningThreshold, int maximumPacketsLimit, double timeGap) {
        this.app = app;
        this.warningThreshold = warningThreshold;
        this.maximumPacketsLimit = maximumPacketsLimit;
        this.timeGap = timeGap;
    }

    public void startServer() {
        isRunning = true;
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

    private void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String packet;
            while ((packet = reader.readLine()) != null) {
                totalPacketCount++;
                if (!packet.isEmpty()) {
                    acceptedPackets++;
                        updateMovingAverage(1); // Increase moving average for normal packet
                } else {
                        updateMovingAverage(-1); // Decrease moving average for empty packet
                }
                // Logging
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

    private synchronized void updateMovingAverage(int value) {
        movingAverageList.add((double) value);

        //Delete the oldest element
        if (movingAverageList.size() > MOVING_AVERAGE_WINDOW) {
            movingAverageList.remove(0);
        }
    }

    private synchronized double calculateMovingAverage() {
        if (movingAverageList.isEmpty()) {
            return 0;
        }
        double sum = 0;

        for (int i=0; i<movingAverageList.size(); i++) {
            sum += movingAverageList.get(i);
        }

        return sum / movingAverageList.size();
    }

    private synchronized void updateTrafficData() {
        double movingAverage = calculateMovingAverage();
        long currentTime = System.currentTimeMillis();
        double elapsedTimeInMinutes = (currentTime - startTime) / 60000.0; // Convert to minutes

        System.out.printf("Total Packets: %d, Accepted Packets: %d%n", totalPacketCount, acceptedPackets);
        System.out.printf("Moving Average: %.2f%n", movingAverage);
        System.out.printf("Packets per Minute: %.2f, Accepted Packets per Minute: %.2f%n",
                totalPacketCount / elapsedTimeInMinutes,
                acceptedPackets / elapsedTimeInMinutes);

        // Check for warning condition
        if (Math.abs(movingAverage) >= warningThreshold) {
            showWarningNotification(currentTime, movingAverage);
        }

        if (totalPacketCount >= maximumPacketsLimit && elapsedTimeInMinutes >= timeGap) {
            showMaximumTrafficNotification(currentTime);
        }
    }

    private void showMaximumTrafficNotification(long currentTime) {
        app.stopSimulation();
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText("Warning Notification");
            alert.setContentText(String.format("Maximum amount of traffic has been reached at %s! The server has been stopped", new Date(currentTime)));
            alert.showAndWait();
        });
    }

    private void showWarningNotification(long time, double movingAverage) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        String formattedTime = sdf.format(new Date(time));
        Platform.runLater(() -> {
            app.showWarningIcon();
        });

        // Log warning to file
        synchronized (this){
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("warning_log.txt", true))) {
                writer.write(String.format("At time %s, the moving average reached %.2f%n", formattedTime, movingAverage));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public synchronized int getTotalPacketCount() {
        return totalPacketCount;
    }

    public synchronized double getMovingAverage() {
        return calculateMovingAverage();
    }

    public void stopServer() {
        isRunning = false;
        if (server != null && !server.isClosed()) {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}