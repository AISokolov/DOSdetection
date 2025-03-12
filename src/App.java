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
    public DOSDetector detector;
    public PacketSender sender;
    public ExecutorService dosDetectorExecutor;
    public ExecutorService packetSenderExecutor;
    private int currentTime = 0;
    private Timer timer = new Timer();
    private ImageView warningIcon;
    public static boolean isAttcked = false;
    private boolean isUpdating = true;

    @Override
    public void start(Stage primaryStage) {
        detector = new DOSDetector(this,  0.75, 500, 0.5);
        sender = new PacketSender(this, 100, 10, 10, 100);

        // chart: total Packets
        NumberAxis xAxis1 = new NumberAxis();
        xAxis1.setLabel("Time");
        xAxis1.setAutoRanging(false);
        xAxis1.setLowerBound(0);
        xAxis1.setUpperBound(100); // Initial range
        NumberAxis yAxis1 = new NumberAxis();
        yAxis1.setLabel("Packets");

        LineChart<Number, Number> packetChart = new LineChart<>(xAxis1, yAxis1);
        packetChart.getStylesheets().add("file:src/packetChartStyle.css");

        yAxis1.setAutoRanging(true);
        packetChart.setTitle("Total Packets");

        XYChart.Series<Number, Number> totalPacketsSeries = new XYChart.Series<>();
        totalPacketsSeries.setName("Total Packets");

        packetChart.getData().add(totalPacketsSeries);

        // chart: moving average
        NumberAxis xAxis2 = new NumberAxis();
        xAxis2.setLabel("Time");
        xAxis2.setAutoRanging(false);
        xAxis2.setLowerBound(0);
        xAxis2.setUpperBound(100); // Initial range
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
        warningIcon = new ImageView(new Image("warning.png"));
        warningIcon.setFitWidth(30); // Set the width of the icon
        warningIcon.setFitHeight(30); // Set the height of the icon
        warningIcon.setVisible(false);

        // layout
        HBox chartsLayout = new HBox(10, packetChart, loadChart, warningIcon);

        StackPane root = new StackPane();
        Button startButton = new Button("Start");
        Button attackButton = new Button("Attack");
        Button stopButton = new Button("Stop");

        attackButton.setOnAction(e -> startAttack());
        stopButton.setOnAction(e -> stopSimulation());
        startButton.setOnAction(e -> startDetecting(totalPacketsSeries, movingAverageSeries, xAxis1, xAxis2));

        VBox layout = new VBox(10, startButton, attackButton, stopButton, chartsLayout);
        root.getChildren().add(layout);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void stopSimulation() {
        isUpdating = false;
        dosDetectorExecutor.shutdown();
        packetSenderExecutor.shutdown();
        detector.stopServer();
        sender.stopSending();
    }

    private void startAttack() {
        isAttcked = true;
    }

    private void startUpdatingGraph(XYChart.Series<Number, Number> totalPacketsSeries,
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
        }, 0, 100); // Update every second (100 ms)
    }

    private void startDetecting(XYChart.Series<Number, Number> totalPacketsSeries,
                                XYChart.Series<Number, Number> movingAverageSeries,
                                NumberAxis xAxis1, NumberAxis xAxis2) {
        clearLogFile(); // Clear the log file at the start of each simulation
        timer = new Timer();
        startUpdatingGraph(totalPacketsSeries, movingAverageSeries, xAxis1, xAxis2);

        isUpdating = true;

        dosDetectorExecutor = Executors.newSingleThreadExecutor();
        dosDetectorExecutor.execute(() -> detector.startServer());

        packetSenderExecutor = Executors.newSingleThreadExecutor();
        packetSenderExecutor.execute(() -> sender.sendPacket());
    }

    public void showWarningIcon() {
        warningIcon.setVisible(true);
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(500), e -> warningIcon.setVisible(false)));
        timeline.play();
    }

    public void clearLogFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("warning_log.txt"))) {
            writer.write(""); // Clear the file content
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}