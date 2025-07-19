public class ConfigParams {
    public final String name;
    public final double warningThreshold;
    public final int maximumPacketsLimit;
    public final double timeGap;

    public final int maxPacketCount;
    public final int minPacketCount;
    public final int sleepTimeLBorder;
    public final int sleepTimeUBorder;
    public final int defaultSleepTime;

    public ConfigParams(
            String name,
            double warningThreshold,
            int maximumPacketsLimit,
            double timeGap,
            int maxPacketCount,
            int minPacketCount,
            int sleepTimeLBorder,
            int sleepTimeUBorder,
            int defaultSleepTime
    ) {
        this.name = name;
        this.warningThreshold = warningThreshold;
        this.maximumPacketsLimit = maximumPacketsLimit;
        this.timeGap = timeGap;
        this.maxPacketCount = maxPacketCount;
        this.minPacketCount = minPacketCount;
        this.sleepTimeLBorder = sleepTimeLBorder;
        this.sleepTimeUBorder = sleepTimeUBorder;
        this.defaultSleepTime = defaultSleepTime;
    }
}