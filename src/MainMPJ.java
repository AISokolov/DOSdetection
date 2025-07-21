import mpi.*;
import javax.swing.*;
import java.util.Arrays;

public class MainMPJ {
    public static void main(String[] args) {
        MPI.Init(args);
        //props of the current process
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        ConfigParams[] configs = {
                new ConfigParams("Prime_Time", 0.8, 1000, 0.5, 200, 50, 5, 50, 350),
                new ConfigParams("Morning", 0.3, 300, 0.5, 50, 5, 100, 200, 600),
                new ConfigParams("Night", 0.5, 700, 0.5, 100, 10, 10, 100, 800)
        };

        // Prepare arrays for Scatter
        double[] allDoubles = new double[2 * size];
        int[] allInts = new int[6 * size];
        //root(sender) process (rank 0) fills the arrays with data
        if (rank == 0) {
            for (int i = 1; i < size; i++) {
                // i-1 MOD len
                int configIdx = (i - 1) % configs.length;
                ScatterParams sp = new ScatterParams(configs[configIdx]);
                System.arraycopy(sp.toDoubleArray(), 0, allDoubles, i * 2, 2);
                System.arraycopy(sp.toIntArray(), 0, allInts, i * 6, 6);
            }
        }

        double[] recvDoubles = new double[2];
        int[] recvInts = new int[6];

        MPI.COMM_WORLD.Scatter(allDoubles, 0, 2, MPI.DOUBLE, recvDoubles, 0, 2, MPI.DOUBLE, 0);
        MPI.COMM_WORLD.Scatter(allInts, 0, 6, MPI.INT, recvInts, 0, 6, MPI.INT, 0);

        if (rank == 0) {
            //start GUI
            SwingUtilities.invokeLater(() -> {
                for (ConfigParams config : configs) {
                    new AppMPJ(config).setVisible(true);
                }
            });
        } else {
            ScatterParams sp = ScatterParams.fromArrays(recvDoubles, recvInts);
            PacketSenderMPJ sender = new PacketSenderMPJ(rank, sp);
            sender.run();
        }

        MPI.Finalize();
    }
}