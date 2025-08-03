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
    private PacketChartPanel chartPanel;

    private JButton startButton, stopButton, attackButton;
    private volatile boolean isRunning = false;
    private volatile boolean isAttacked = false;
    private Thread packetThread;

    private javax.swing.Timer graphTimer;
    private static final int HISTORY_LIMIT = 100;
    private static final int AVERAGE_WINDOW_SIZE = 10;

    private final Random random = new Random();

    private long startTime = System.currentTimeMillis();
    private boolean isStoppedByLimit = false;

    public AppMPJ(ConfigParams config) {
        this.config = config;
        setTitle("DOS Detection (MPJ) - " + config.name);
        setSize(600, 400);
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
        attackButton.setEnabled(false);
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
            chartPanel.repaint();
        });
        graphTimer.start();

        packetThread = new Thread(() -> {
            while (isRunning) {
                try {
                    int[] msg = new int[2];
                    MPI.COMM_WORLD.Recv(msg, 0, 2, MPI.INT, MPI.ANY_SOURCE, 1);
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
        if (movingAverageList.size() > AVERAGE_WINDOW_SIZE) {
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

            int w = getWidth() - 60;
            int h = getHeight() - 60;
            int offsetX = 40;
            int offsetY = 40;
            int n = movingAverageHistory.size();
            if (n < 2) return;

            double maxAvg = movingAverageHistory.get(0);
            double minAvg = movingAverageHistory.get(0);
            for (Double value : movingAverageHistory) {
                if (value > maxAvg) maxAvg = value;
                if (value < minAvg) minAvg = value;
            }
            double range = (maxAvg == minAvg) ? 1.0 : maxAvg - minAvg;

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

            if (!movingAverageHistory.isEmpty()) {
                double currentValue = movingAverageHistory.get(movingAverageHistory.size() - 1);
                String valueText = String.format("%.2f", currentValue);

                Font font = new Font("Arial", Font.BOLD, 14);
                g2.setFont(font);

                FontMetrics metrics = g2.getFontMetrics(font);
                int textWidth = metrics.stringWidth(valueText);

                g2.setColor(Color.BLACK);
                g2.drawString(valueText, getWidth() - textWidth - 10, getHeight() - 10);
            }
        }
    }
}