package io.lighty.core.controller.impl.services.infrautils;


import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class LightyMetricsFileReporter extends ScheduledReporter {

    private static final Logger LOG = LoggerFactory.getLogger(LightyMetricsFileReporter.class);

    private static final String DATA_DIRECTORY = "data";
    private static final String COUNTERS_DIRECTORY = "metrics";
    private static final String COUNTER_FILE_PREFIX = "metrics.";
    private static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;
    private static final String SEPARATOR = ",";

    private final File parentDirectory;
    private final Map<String, Long> oldCounters = new HashMap<>();
    private final MetricRegistry registry;
    private final Duration interval;

    public LightyMetricsFileReporter(MetricRegistry registry, Duration interval) {
        super(registry, "file-reporter", MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.SECONDS);
        this.parentDirectory = new File(DATA_DIRECTORY, COUNTERS_DIRECTORY);
        this.registry = registry;
        this.interval = interval;
    }

    public void startReporter() {
        start(interval.getSeconds(), TimeUnit.SECONDS);
    }

    Duration getInterval() {
        return interval;
    }

    public void report(PrintWriter pw) {
        report(pw, registry.getGauges(), registry.getCounters(),
                registry.getHistograms(), registry.getMeters(), registry.getTimers());
    }

    private void report(PrintWriter pw, @SuppressWarnings("rawtypes") SortedMap<String, Gauge> gauges,
                        SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms,
                        SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
        pw.print("date,");
        pw.print(new Date());
        pw.println();

        pw.println("Counters:");
        for (Map.Entry<String, Counter> entry : counters.entrySet()) {
            Counter newCounter = entry.getValue();
            // avoid unnecessary write to report file
            // report the counter only if there is a change
            Long oldCounterObj = oldCounters.get(entry.getKey());
            long oldCounter = oldCounterObj != null ? oldCounterObj.longValue() : 0;
            if (newCounter.getCount() != oldCounter) {
                pw.print(entry.getKey());
                printWithSeparator(pw, "count", entry.getValue().getCount());
                printWithSeparator(pw, "diff",
                        entry.getValue().getCount() - oldCounter);
                pw.println();
            }
        }
        pw.println("Gauges:");
        for (@SuppressWarnings("rawtypes") Map.Entry<String, Gauge> entry : gauges.entrySet()) {
            pw.print(entry.getKey());
            pw.println(entry.getValue().getValue());
        }
        pw.println("Histograms:");
        for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
            pw.print(entry.getKey());
            printWithSeparator(pw, "count", entry.getValue().getCount());
            printSampling(pw, entry.getValue());
            pw.println();
        }
        pw.println("Meters:");
        for (Map.Entry<String, Meter> entry : meters.entrySet()) {
            pw.print(entry.getKey());
            printMeter(pw, entry.getValue());
        }
        pw.println("Timers:");
        for (Map.Entry<String, Timer> entry : timers.entrySet()) {
            pw.print(entry.getKey());
            printSampling(pw, entry.getValue());
            printMeter(pw, entry.getValue());
        }
        counters.forEach((key, value) -> oldCounters.put(key, value.getCount()));
    }

    @Override
    public void report(@SuppressWarnings("rawtypes")
                               SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
        try {
            Calendar calendar = Calendar.getInstance();
            int hourOfTheDay = calendar.get(Calendar.HOUR_OF_DAY);
            int dayOfTheWeek = calendar.get(Calendar.DAY_OF_WEEK);
            // retains one week worth of counters
            rotateLastWeekFile(dayOfTheWeek, hourOfTheDay);
            boolean append = true;
            File file = createFile(dayOfTheWeek, hourOfTheDay);
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, append),
                    DEFAULT_ENCODING));
            report(pw, gauges, counters, histograms, meters, timers);
            pw.close();
        } catch (IOException e) {
            LOG.error("Failed to report counters to files", e);
        }
    }

    private static void printSampling(PrintWriter pw, Sampling sampling) {
        Snapshot snapshot = sampling.getSnapshot();
        printWithSeparator(pw, "min", snapshot.getMin());
        printWithSeparator(pw, "max", snapshot.getMax());
        printWithSeparator(pw, "mean", snapshot.getMean());
    }

    private static void printMeter(PrintWriter pw, Metered meter) {
        printWithSeparator(pw, "count", meter.getCount());
        printWithSeparator(pw, "oneMinuteRate", meter.getOneMinuteRate());
        printWithSeparator(pw, "fiveMinuteRate", meter.getFiveMinuteRate());
        printWithSeparator(pw, "fifteenMinuteRate", meter.getFifteenMinuteRate());
        pw.println();
    }

    private static void printWithSeparator(PrintWriter pw, String name, Object val) {
        printSeparator(pw);
        pw.print(name);
        printSeparator(pw);
        pw.print(val);
    }

    private static void printSeparator(PrintWriter pw) {
        pw.print(SEPARATOR);
    }

    private static String getFileName(int dayOfTheWeek, int hourOfTheDay) {
        return COUNTER_FILE_PREFIX + dayOfTheWeek + "." + hourOfTheDay;
    }

    public File createFile(int dayOfTheWeek, int hourOfTheDay) throws IOException {
        if (!parentDirectory.exists()) {
            LOG.info("Directory does not exist, creating it: {}", parentDirectory.getName());
            if (!parentDirectory.mkdirs()) {
                throw new IOException("Failed to make directories: " + parentDirectory.toString());
            }
        }
        File file = new File(parentDirectory, getFileName(dayOfTheWeek, hourOfTheDay));
        if (!file.exists()) {
            LOG.info("File does not exist, creating it: {}", file.getPath());
            if (!file.createNewFile()) {
                throw new IOException("Failed to create file: " + file.toString());
            }
        }
        return file;
    }

    private void rotateLastWeekFile(int dayOfTheWeek, int hourOfTheDay) throws IOException {
        int nextHour = hourOfTheDay < 23 ? hourOfTheDay + 1 : 0;
        File nextHourFile = new File(parentDirectory , getFileName(dayOfTheWeek, nextHour));
        if (nextHourFile.exists()) {
            boolean append = false;
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(nextHourFile, append),
                    DEFAULT_ENCODING));
            pw.write(new Date().toString());
            pw.close();
        }
    }
}
