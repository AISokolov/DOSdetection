public class ScatterParams {
    public double warningThreshold;
    public int maximumPacketsLimit;
    public double timeGap;
    public int maxPacketCount;
    public int minPacketCount;
    public int sleepTimeLBorder;
    public int sleepTimeUBorder;
    public int defaultSleepTime;

    public ScatterParams() {}

    public ScatterParams(ConfigParams config) {
        this.warningThreshold = config.warningThreshold;
        this.maximumPacketsLimit = config.maximumPacketsLimit;
        this.timeGap = config.timeGap;
        this.maxPacketCount = config.maxPacketCount;
        this.minPacketCount = config.minPacketCount;
        this.sleepTimeLBorder = config.sleepTimeLBorder;
        this.sleepTimeUBorder = config.sleepTimeUBorder;
        this.defaultSleepTime = config.defaultSleepTime;
    }

    public double[] toDoubleArray() {
        return new double[] {
                warningThreshold,
                timeGap
        };
    }

    public int[] toIntArray() {
        return new int[] {
                maximumPacketsLimit,
                maxPacketCount,
                minPacketCount,
                sleepTimeLBorder,
                sleepTimeUBorder,
                defaultSleepTime
        };
    }

    public static ScatterParams fromArrays(double[] doubles, int[] ints) {
        ScatterParams p = new ScatterParams();
        p.warningThreshold = doubles[0];
        p.timeGap = doubles[1];
        p.maximumPacketsLimit = ints[0];
        p.maxPacketCount = ints[1];
        p.minPacketCount = ints[2];
        p.sleepTimeLBorder = ints[3];
        p.sleepTimeUBorder = ints[4];
        p.defaultSleepTime = ints[5];
        return p;
    }
}