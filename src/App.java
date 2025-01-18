import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App extends Application {
    public DOSDetector detector = new DOSDetector();
    public PacketSender sender = new PacketSender();
    public ExecutorService dosDetectorExecutor;
    public ExecutorService packetSenderExecutor;
    private int currentTime = 0;
    private Timer timer = new Timer();

    @Override
    public void start(Stage primaryStage) {

        // chart: total and accepted Packets
        NumberAxis xAxis1 = new NumberAxis();
        xAxis1.setLabel("Time");
        NumberAxis yAxis1 = new NumberAxis();
        yAxis1.setLabel("Packets");

        LineChart<Number, Number> packetChart = new LineChart<>(xAxis1, yAxis1);
        packetChart.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        yAxis1.setAutoRanging(true);
        packetChart.setTitle("Total and Accepted Packets");

        XYChart.Series<Number, Number> totalPacketsSeries = new XYChart.Series<>();
        totalPacketsSeries.setName("Total Packets");

        XYChart.Series<Number, Number> acceptedPacketsSeries = new XYChart.Series<>();
        acceptedPacketsSeries.setName("Accepted Packets");

        packetChart.getData().addAll(totalPacketsSeries, acceptedPacketsSeries);

        // chart: network Load and limit
        NumberAxis xAxis2 = new NumberAxis();
        xAxis2.setLabel("Time");
        NumberAxis yAxis2 = new NumberAxis();
        yAxis2.setLabel("Load (%)");

        LineChart<Number, Number> loadChart = new LineChart<>(xAxis2, yAxis2);
        yAxis2.setAutoRanging(true);
        loadChart.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        loadChart.setTitle("Network Load and Limit");

        XYChart.Series<Number, Number> networkLoadSeries = new XYChart.Series<>();
        networkLoadSeries.setName("Network Load (%)");

        XYChart.Series<Number, Number> limitSeries = new XYChart.Series<>();
        limitSeries.setName("100% (Limit)");

        loadChart.getData().addAll(networkLoadSeries, limitSeries);

        // layout
        VBox chartsLayout = new VBox(20, packetChart, loadChart);

        StackPane root = new StackPane();
        Button startButton = new Button("Start");
        Button stopButton = new Button("Stop");

        startButton.setOnAction(e -> startDetecting(totalPacketsSeries, acceptedPacketsSeries, networkLoadSeries, limitSeries));
        stopButton.setOnAction(e -> stopDetecting());

        VBox layout = new VBox(10, startButton, stopButton, chartsLayout);
        root.getChildren().add(layout);


        Scene scene = new Scene(root, 800, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void startUpdatingGraph(XYChart.Series<Number, Number> totalPacketsSeries,
                                    XYChart.Series<Number, Number> acceptedPacketsSeries,
                                    XYChart.Series<Number, Number> networkLoadSeries,
                                    XYChart.Series<Number, Number> limitSeries) {
        Timer graphTimer = new Timer();
        graphTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                        if (detector.getTotalPacketCount() == 0) {
                            return;
                        }
                        totalPacketsSeries.getData().add(new XYChart.Data<>(currentTime * 10, detector.getTotalPacketCount()));
                        acceptedPacketsSeries.getData().add(new XYChart.Data<>(currentTime * 10, detector.getAcceptedPackets()));

                        networkLoadSeries.getData().add(new XYChart.Data<>(currentTime * 10, detector.getLoadPercentage() * 100));
                        limitSeries.getData().add(new XYChart.Data<>(currentTime * 10, 60));

                        currentTime++;
                });
            }
        }, 0, 100);
    }

    private void startDetecting(XYChart.Series<Number, Number> totalPacketsSeries,
                                XYChart.Series<Number, Number> acceptedPacketsSeries,
                                XYChart.Series<Number, Number> networkLoadSeries,
                                XYChart.Series<Number, Number> limitSeries) {
        timer = new Timer();
        startUpdatingGraph(totalPacketsSeries, acceptedPacketsSeries, networkLoadSeries, limitSeries);

        dosDetectorExecutor = Executors.newSingleThreadExecutor();
        dosDetectorExecutor.execute(() -> detector.startServer());

        packetSenderExecutor = Executors.newSingleThreadExecutor();
        packetSenderExecutor.execute(() -> sender.sendPacket());
    }

    //DOESNT WORK WELL
    private void stopDetecting() {
        if (timer != null) {
            timer.cancel();
        }
        if (dosDetectorExecutor != null && !dosDetectorExecutor.isShutdown()) {
            dosDetectorExecutor.shutdown();
        }
        if (packetSenderExecutor != null && !packetSenderExecutor.isShutdown()) {
            packetSenderExecutor.shutdown();
        }

        detector.stopServer();
        PacketSender.isRunning = false;
    }

    @Override
    public void stop() {
        stopDetecting();
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
