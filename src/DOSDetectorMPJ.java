import mpi.*;

public class DOSDetectorMPJ {
    private int numSenders;

    public DOSDetectorMPJ(int numSenders) {
        this.numSenders = numSenders;
    }

    public void run() {
        int totalPackets = 0;
        while (true) {
            int[] msg = new int[2];
            Status status = MPI.COMM_WORLD.Recv(msg, 0, 2, MPI.INT, MPI.ANY_SOURCE, 99);
            int packetType = msg[0];
            int senderRank = msg[1];
            totalPackets++;
            System.out.println("Получен пакет от процесса " + senderRank + ", тип: " + packetType + ", всего: " + totalPackets);
            // Здесь можно добавить логику анализа и предупреждений
        }
    }
}