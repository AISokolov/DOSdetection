import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.scene.control.Alert;

public class DOSDetector {
    private static final int BASE_PORT = 8081;
    private static final int MOVING_AVERAGE_WINDOW = 10;

    private double warningThreshold;
    private int maximumPacketsLimit;
    private double timeGap;
    private int totalPacketCount = 0;
    private int acceptedPackets = 0;
    private boolean isRunning = true;
    private ServerSocket server;
    private List<Double> movingAverageList = new ArrayList<>();
    private long startTime = System.currentTimeMillis();
    private final InstanceController controller;
    private int instancePortOffset;
    private ExecutorService clientHandlerExecutor = Executors.newFixedThreadPool(4);

    public DOSDetector(InstanceController controller, double warningThreshold,
                       int maximumPacketsLimit, double timeGap) {
        this.controller = controller;
        this.warningThreshold = warningThreshold;
        this.maximumPacketsLimit = maximumPacketsLimit;
        this.timeGap = timeGap;
        this.instancePortOffset = controller.getInstanceNumber() - 1; // Port offset based on instance number
    }

    public void startServer() {
        isRunning = true;
        try {
            server = new ServerSocket(BASE_PORT + instancePortOffset);
            System.out.println("Server for instance " + controller.getInstanceNumber() +
                    " started on port " + (BASE_PORT + instancePortOffset));
            while (isRunning) {
                try {
                    Socket clientSocket = server.accept();
                    clientHandlerExecutor.submit(() -> handleClient(clientSocket));
                }
                catch (IOException e) {
                    if (!isRunning) {
                        break; // Exit the loop if the server is stopped
                    }
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String packet;
            while ((packet = reader.readLine()) != null) {
                synchronized (this) {
                    totalPacketCount++;
                    if (!packet.isEmpty()) {
                        acceptedPackets++;
                        updateMovingAverage(1);
                    } else {
                        updateMovingAverage(-1);
                    }
                    updateTrafficData();
                }
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
        if (movingAverageList.size() > MOVING_AVERAGE_WINDOW) {
            movingAverageList.remove(0);
        }
    }

    private synchronized double calculateMovingAverage() {
        if (movingAverageList.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (Double value : movingAverageList) {
            sum += value;
        }
        return sum / movingAverageList.size();
    }

    private synchronized void updateTrafficData() {
        double movingAverage = calculateMovingAverage();
        long currentTime = System.currentTimeMillis();
        double elapsedTimeInMinutes = (currentTime - startTime) / 60000.0;

        System.out.printf("Instance %d - Packets per Minute: %.2f%n",
                controller.getInstanceNumber(), totalPacketCount / elapsedTimeInMinutes);

        System.out.printf("Instance %d - Packets Total: %d \n",
                controller.getInstanceNumber(), totalPacketCount);

        if (Math.abs(movingAverage) >= warningThreshold) {
            showWarningNotification(currentTime, movingAverage);
        }

        if (totalPacketCount >= maximumPacketsLimit && elapsedTimeInMinutes >= timeGap) {
            showMaximumTrafficNotification(currentTime);
        }
    }

    private void showMaximumTrafficNotification(long currentTime) {
        controller.stopSimulation();
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning - Instance " + controller.getInstanceName());
            alert.setHeaderText("Warning Notification");
            alert.setContentText(String.format("Maximum amount of traffic has been reached at %s! The server has been stopped",
                    new Date(currentTime)));
            alert.showAndWait();
        });
    }

    private void showWarningNotification(long time, double movingAverage) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        String formattedTime = sdf.format(new Date(time));
        Platform.runLater(() -> {
            controller.showWarningIcon();
        });

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter("warning_log_" + controller.getInstanceNumber() + "_" + controller.getInstanceName() + ".txt", true))) {
            writer.write(String.format("At time %s, the moving average reached %.2f%n",
                    formattedTime, movingAverage));
        } catch (IOException e) {
            e.printStackTrace();
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