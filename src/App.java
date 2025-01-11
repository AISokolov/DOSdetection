import javafx.application.*;
import javafx.scene.*;
import javafx.scene.chart.*;

import javafx.scene.layout.*;
import javafx.stage.Stage;

import javafx.scene.control.*;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App extends Application {
    public DOSDetector detector = new DOSDetector();
    public PacketSender sender = new PacketSender();
    public ExecutorService dosDetectorExecutor;
    public ExecutorService packetSenderExecutor;
    int currentTime = 0;
    private Timer timer = new Timer();

    @Override
    public void start(Stage primaryStage) {

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Time");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Value");

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Detection Results");
        lineChart.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        XYChart.Series<Number, Number> totalNumberPackets = new XYChart.Series<>();

        XYChart.Series<Number, Number> acceptedPackets = new XYChart.Series<>();

        yAxis.setAutoRanging(true);

        lineChart.getData().add(totalNumberPackets);
        lineChart.getData().add(acceptedPackets);
        StackPane root = new StackPane();
        Button startButton = new Button("Start");
        Button stopButton = new Button("Stop");
        startButton.setOnAction( e -> startDetecting(totalNumberPackets, acceptedPackets));
        stopButton.setOnAction( e -> stopDetecting());
        VBox layout = new VBox(10, startButton, stopButton, lineChart);
        root.getChildren().add(layout);
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }

    private int getDetectedValue() {
        return detector.totalPacketCount;
    }

    private int getAcceptedPackets() {
        return detector.acceptedPackets;
    }

    private int startTimer() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                currentTime++;
            }
        },0, 100);
        return currentTime;
    }

    private void startUpdatingGraph(XYChart.Series<Number, Number> dataSeries, XYChart.Series<Number, Number> acceptedPackets) {
        Timer graphTimer = new Timer();
        graphTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    dataSeries.getData().add(new XYChart.Data<>(currentTime, getDetectedValue()));
                    acceptedPackets.getData().add(new XYChart.Data<>(currentTime, getAcceptedPackets()));
                });
            }
        }, 0, 100);
    }

    private void startDetecting(XYChart.Series<Number, Number> dataSeries, XYChart.Series<Number, Number> calculatedMean) {
        startTimer();
        startUpdatingGraph(dataSeries, calculatedMean);

        dosDetectorExecutor = Executors.newSingleThreadExecutor();
        dosDetectorExecutor.execute(() -> detector.startServer());
        packetSenderExecutor = Executors.newSingleThreadExecutor();
        packetSenderExecutor.execute(() -> sender.sendPacket());
    }

    private void stopDetecting() {
        timer.cancel();
        dosDetectorExecutor.shutdownNow();
        packetSenderExecutor.shutdownNow();
        detector.stopServer();
        sender.stopSending();
    }

    @Override
    public void stop() throws Exception {
        Platform.exit();
        timer.cancel();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}