import mpi.*;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;

public class AppMPJ extends JFrame {
    private final ConfigParams config;
    private int totalPackets = 0;
    private List<Integer> packetHistory = new ArrayList<>();
    private List<Double> movingAverageHistory = new ArrayList<>();
    private List<Double> movingAverageList = new ArrayList<>();

    private JLabel warningLabel;
    private int timeIndex = 0;
    private PacketChartPanel chartPanel;

    private JButton startButton, stopButton, attackButton;
    private volatile boolean isRunning = false;
    private volatile boolean isAttacked = false;
    private Thread packetThread;

    private javax.swing.Timer graphTimer;
    private static final int HISTORY_LIMIT = 100;
    private final Random random = new Random();

    // Для контроля лимита трафика
    private long startTime = System.currentTimeMillis();
    private boolean isStoppedByLimit = false;

    public AppMPJ(ConfigParams config) {
        this.config = config;
        setTitle("DOS Detection (MPJ) - " + config.name);
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        chartPanel = new PacketChartPanel();

        ImageIcon icon = new ImageIcon("warning.png");
        Image scaledImage = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        warningLabel = new JLabel(new ImageIcon(scaledImage));
        warningLabel.setVisible(false);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.add(warningLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        attackButton = new JButton("Attack");
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(attackButton);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(chartPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        startButton.addActionListener(e -> startReceiving());
        stopButton.addActionListener(e -> stopReceiving());
        attackButton.addActionListener(e -> {
            isAttacked = true;
            attackButton.setEnabled(false);
        });

        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        attackButton.setEnabled(true);
    }

    private void startReceiving() {
        if (isRunning) return;
        isRunning = true;
        isAttacked = false;
        isStoppedByLimit = false;
        startTime = System.currentTimeMillis();
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        attackButton.setEnabled(true);

        graphTimer = new javax.swing.Timer(100, e -> {
            packetHistory.add(totalPackets);
            if (packetHistory.size() > HISTORY_LIMIT) {
                packetHistory.remove(0);
            }
            double currentAvg = calculateMovingAverage();
            movingAverageHistory.add(currentAvg);
            if (movingAverageHistory.size() > HISTORY_LIMIT) {
                movingAverageHistory.remove(0);
            }
            timeIndex++;
            chartPanel.repaint();
        });
        graphTimer.start();

        packetThread = new Thread(() -> {
            while (isRunning) {
                try {
                    int[] msg = new int[2];
                    MPI.COMM_WORLD.Recv(msg, 0, 2, MPI.INT, MPI.ANY_SOURCE, 99);
                    int packetType = msg[0];
                    totalPackets++;
                    updateMovingAverage(packetType == 1 ? 1 : -1);
                    updateTrafficData();

                    if (isAttacked) {
                        int sleepTime = random.nextInt(config.sleepTimeUBorder - config.sleepTimeLBorder + 1) + config.sleepTimeLBorder;
                        Thread.sleep(sleepTime);
                    } else {
                        Thread.sleep(config.defaultSleepTime);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        packetThread.start();
    }

    private void stopReceiving() {
        isRunning = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        attackButton.setEnabled(true);
        if (packetThread != null) {
            packetThread.interrupt();
        }
        if (graphTimer != null) {
            graphTimer.stop();
        }
    }

    private synchronized void updateMovingAverage(int value) {
        movingAverageList.add((double) value);
        if (movingAverageList.size() > 10) {
            movingAverageList.remove(0);
        }
    }

    private synchronized double calculateMovingAverage() {
        if (movingAverageList.isEmpty()) return 0;
        double sum = 0;
        for (Double v : movingAverageList) sum += v;
        return sum / movingAverageList.size();
    }

    private synchronized void updateTrafficData() {
        double movingAverage = calculateMovingAverage();
        if (Math.abs(movingAverage) >= config.warningThreshold) {
            showWarningIcon();
        }

        // Логика остановки по лимиту трафика
        long currentTime = System.currentTimeMillis();
        double elapsedTimeInMinutes = (currentTime - startTime) / 60000.0;
        if (!isStoppedByLimit && totalPackets >= config.maximumPacketsLimit && elapsedTimeInMinutes >= config.timeGap) {
            isStoppedByLimit = true;
            stopReceiving();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    this,
                    "Maximum amount of traffic has been reached! \n The server has been stopped",
                    "Warning Notification",
                    JOptionPane.WARNING_MESSAGE
            ));
        }
    }

    private void showWarningIcon() {
        SwingUtilities.invokeLater(() -> {
            warningLabel.setVisible(true);
            java.util.Timer timer = new java.util.Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    SwingUtilities.invokeLater(() -> warningLabel.setVisible(false));
                }
            }, 500);
        });
    }

    class PacketChartPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            g.setColor(Color.BLACK);
            g.drawString("Time", getWidth() / 2 - 20, getHeight() - 10);
            g.drawString("Moving avg value", 10, 30);

            int w = getWidth() - 60;
            int h = getHeight() - 60;
            int offsetX = 40;
            int offsetY = 40;
            int n = movingAverageHistory.size();
            if (n < 2) return;

            g.drawLine(offsetX, offsetY, offsetX, offsetY + h); // Y
            g.drawLine(offsetX, offsetY + h, offsetX + w, offsetY + h); // X

            double maxAvg = movingAverageHistory.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
            double minAvg = movingAverageHistory.stream().mapToDouble(Double::doubleValue).min().orElse(-1.0);
            double range = Math.max(1e-6, maxAvg - minAvg);

            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(Color.RED);

            java.awt.geom.GeneralPath path = new java.awt.geom.GeneralPath();
            for (int i = 0; i < n; i++) {
                int x = offsetX + (w * i) / (n - 1);
                double avg = movingAverageHistory.get(i);
                int y = offsetY + h - (int) ((avg - minAvg) / range * h);
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            g2.draw(path);

            g.setColor(Color.GRAY);
            int step = Math.max(1, n / 10);
            for (int i = 0; i < n; i += step) {
                int x = offsetX + (w * i) / (n - 1);
                g.drawLine(x, offsetY + h, x, offsetY + h + 5);
                g.drawString(String.valueOf((timeIndex - n + i) * 100 / 1000.0), x - 5, offsetY + h + 20); // X: секунды
            }
            for (int i = 0; i <= 5; i++) {
                int y = offsetY + h - (h * i) / 5;
                double value = minAvg + (range * i) / 5;
                g.drawLine(offsetX - 5, y, offsetX, y);
                g.drawString(String.format("%.2f", value), 2, y + 5);
            }
        }
    }
}