import mpi.*;
import javax.swing.*;

public class MainMPJ {
    public static void main(String[] args) {
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();

        ConfigParams[] configs = {
                new ConfigParams("Prime_Time", 0.8, 1000, 0.5, 200, 50, 5, 50, 350),
                new ConfigParams("Morning", 0.3, 300, 0.5, 50, 5, 100, 200, 600),
                new ConfigParams("Night", 0.5, 700, 0.5, 100, 10, 10, 100, 800)
        };

        if (rank == 0) {
            SwingUtilities.invokeLater(() -> {
                for (ConfigParams config : configs) {
                    new AppMPJ(config).setVisible(true);
                }
            });
        } else {
            int configIdx = (rank - 1) % configs.length;
            PacketSenderMPJ sender = new PacketSenderMPJ(rank, configs[configIdx]);
            sender.run();
        }

        MPI.Finalize();
    }
}