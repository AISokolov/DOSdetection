import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App extends Application {
    private static final int NUM_INSTANCES = 3;
    private ExecutorService appExecutor;
    public int availableProcessors = Runtime.getRuntime().availableProcessors() / 2;

    @Override
    public void start(Stage primaryStage) {
        appExecutor = Executors.newFixedThreadPool(availableProcessors);

        for (int i = 0; i < NUM_INSTANCES; i++) {
            int instanceNum = i + 1;
            appExecutor.execute(() -> {
                Platform.runLater(() -> createInstanceWindow(instanceNum));
            });
        }
    }

    private void createInstanceWindow(int instanceNumber) {
        Stage stage = new Stage(); // main window container
        InstanceController controller = new InstanceController(instanceNumber);
        switch (instanceNumber) {
            case 1:
                controller.setInstanceName("Prime_Time");
                controller.detector = new DOSDetector(controller, 0.8, 1000, 0.5);
                controller.sender = new PacketSender(controller, 200, 50, 5, 50, 350);
                stage.setTitle("DOS Detection - Prime Time Configuration");
                break;
            case 2:
                controller.setInstanceName("Morning");
                controller.detector = new DOSDetector(controller, 0.3, 300, 0.5);
                controller.sender = new PacketSender(controller, 50, 5, 100, 200, 600);
                stage.setTitle("DOS Detection - Morning Configuration");
                break;
            case 3:
                controller.setInstanceName("Night");
                controller.detector = new DOSDetector(controller, 0.5, 700, 0.5);
                controller.sender = new PacketSender(controller, 100, 10, 10, 100, 800);
                stage.setTitle("DOS Detection - Night Configuration");
                break;
        }

        // total Packets
        NumberAxis xAxis1 = new NumberAxis();
        xAxis1.setLabel("Time");
        xAxis1.setAutoRanging(false);
        xAxis1.setLowerBound(0);
        xAxis1.setUpperBound(100);
        NumberAxis yAxis1 = new NumberAxis();
        yAxis1.setLabel("Packets");

        LineChart<Number, Number> packetChart = new LineChart<>(xAxis1, yAxis1);
        packetChart.getStylesheets().add("file:src/packetChartStyle.css");
        yAxis1.setAutoRanging(true);
        packetChart.setTitle("Total Packets");

        XYChart.Series<Number, Number> totalPacketsSeries = new XYChart.Series<>();
        totalPacketsSeries.setName("Total Packets");
        packetChart.getData().add(totalPacketsSeries);

        // moving average
        NumberAxis xAxis2 = new NumberAxis();
        xAxis2.setLabel("Time");
        xAxis2.setAutoRanging(false);
        xAxis2.setLowerBound(0);
        xAxis2.setUpperBound(100);
        NumberAxis yAxis2 = new NumberAxis();
        yAxis2.setLabel("Average");

        LineChart<Number, Number> loadChart = new LineChart<>(xAxis2, yAxis2);
        loadChart.getStylesheets().add("file:src/loadStyle.css");
        yAxis2.setAutoRanging(true);
        loadChart.setTitle("Moving Average");

        XYChart.Series<Number, Number> movingAverageSeries = new XYChart.Series<>();
        movingAverageSeries.setName("Average");
        loadChart.getData().add(movingAverageSeries);

        // warning icon
        ImageView warningIcon = new ImageView(new Image("warning.png"));
        warningIcon.setFitWidth(30);
        warningIcon.setFitHeight(30);
        warningIcon.setVisible(false);
        controller.setWarningIcon(warningIcon);

        // layout
        HBox chartsLayout = new HBox(10, packetChart, loadChart, warningIcon);

        StackPane root = new StackPane();
        Button startButton = new Button("Start");
        Button attackButton = new Button("Attack");
        Button stopButton = new Button("Stop");

        attackButton.setOnAction(e -> controller.startAttack());
        stopButton.setOnAction(e -> controller.stopSimulation());
        startButton.setOnAction(e -> controller.startDetecting(totalPacketsSeries, movingAverageSeries, xAxis1, xAxis2, availableProcessors));

        VBox layout = new VBox(10, startButton, attackButton, stopButton, chartsLayout);
        root.getChildren().add(layout);

        Scene scene = new Scene(root, 600, 400);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        appExecutor.shutdownNow();
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

class InstanceController {
    public DOSDetector detector;
    public PacketSender sender;
    public ExecutorService dosDetectorExecutor;
    public ExecutorService packetSenderExecutor;
    private int currentTime = 0;
    private Timer timer = new Timer();
    private ImageView warningIcon;
    public boolean isAttacked = false;
    private boolean isUpdating = true;
    private final int instanceNumber;
    public String instanceName;

    public InstanceController(int instanceNumber) {
        this.instanceNumber = instanceNumber;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public int getInstanceNumber() {
        return instanceNumber;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setWarningIcon(ImageView warningIcon) {
        this.warningIcon = warningIcon;
    }

    public void stopSimulation() {
        isUpdating = false;
        if (dosDetectorExecutor != null) dosDetectorExecutor.shutdown();
        if (packetSenderExecutor != null) packetSenderExecutor.shutdown();
        if (detector != null) detector.stopServer();
        if (sender != null) sender.stopSending();
    }

    public void startAttack() {
        isAttacked = true;
        System.out.println("Attack started on instance " + instanceNumber + instanceName);
    }

    public void startDetecting(XYChart.Series<Number, Number> totalPacketsSeries,
                               XYChart.Series<Number, Number> movingAverageSeries,
                               NumberAxis xAxis1, NumberAxis xAxis2, int avalaibleProcessors) {
        clearLogFile();
        timer = new Timer();
        startUpdatingGraph(totalPacketsSeries, movingAverageSeries, xAxis1, xAxis2);

        isUpdating = true;

        dosDetectorExecutor = Executors.newFixedThreadPool(avalaibleProcessors);
        dosDetectorExecutor.execute(() -> detector.startServer());

        packetSenderExecutor = Executors.newFixedThreadPool(avalaibleProcessors);
        packetSenderExecutor.execute(() -> sender.sendPacket());
    }

    public void startUpdatingGraph(XYChart.Series<Number, Number> totalPacketsSeries,
                                   XYChart.Series<Number, Number> movingAverageSeries,
                                   NumberAxis xAxis1, NumberAxis xAxis2) {
        Timer graphTimer = new Timer();
        graphTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isUpdating) {
                    timer.cancel();
                    return;
                }
                Platform.runLater(() -> {
                    if (detector.getTotalPacketCount() == 0) {
                        return;
                    }
                    // Add new data points to the series
                    totalPacketsSeries.getData().add(new XYChart.Data<>(currentTime, detector.getTotalPacketCount()));
                    movingAverageSeries.getData().add(new XYChart.Data<>(currentTime, detector.getMovingAverage()));

                    // Move the x-axis range if necessary
                    if (currentTime > xAxis1.getUpperBound()) {
                        xAxis1.setLowerBound(xAxis1.getLowerBound() + 1);
                        xAxis1.setUpperBound(xAxis1.getUpperBound() + 1);
                        xAxis2.setLowerBound(xAxis2.getLowerBound() + 1);
                        xAxis2.setUpperBound(xAxis2.getUpperBound() + 1);
                    }

                    // Increment time
                    currentTime++;
                });
            }
        }, 0, 100);
    }

    public void showWarningIcon() {
        Platform.runLater(() -> {
            warningIcon.setVisible(true);
            Timeline timeline = new Timeline(new KeyFrame(Duration.millis(500), e -> warningIcon.setVisible(false)));
            timeline.play();
        });
    }

    public void clearLogFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("warning_log_" + instanceNumber + "_" + instanceName + ".txt"))) {
            writer.write("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}