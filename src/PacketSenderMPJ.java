import mpi.*;
import java.util.Random;

public class PacketSenderMPJ {
    private int rank;
    private ScatterParams config;
    private Random random = new Random();
    public boolean isRunning = true;

    public PacketSenderMPJ(int rank, ScatterParams config) {
        this.rank = rank;
        this.config = config;
    }

    public void run() {
        isRunning = true;
        while (isRunning) {
            int packetCount = random.nextInt(config.maxPacketCount - config.minPacketCount + 1) + config.minPacketCount;
            for (int i = 0; i < packetCount && isRunning; i++) {
                int packetType = random.nextBoolean() ? 1 : 0; // 1 - normal, 0 - empty
                int[] msg = new int[]{packetType, rank};
                MPI.COMM_WORLD.Send(msg, 0, 2, MPI.INT, 0, 99);
                try {
                    Thread.sleep(config.defaultSleepTime);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}