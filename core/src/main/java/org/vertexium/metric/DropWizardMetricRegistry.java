package org.vertexium.metric;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import org.vertexium.VertexiumException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class DropWizardMetricRegistry implements VertexiumMetricRegistry {
    private final MetricRegistry metricRegistry;
    private final Map<String, Counter> countersByName = new ConcurrentHashMap<>();
    private final Map<String, Timer> timersByName = new ConcurrentHashMap<>();
    private final Map<String, Histogram> histogramsByName = new ConcurrentHashMap<>();
    private final Map<String, Gauge> gaugesByName = new ConcurrentHashMap<>();
    private final Map<String, StackTraceTracker> stackTraceTrackersByName = new ConcurrentHashMap<>();
    private boolean consoleReporterStarted;
    private ConsoleReporter consoleReporter;

    public DropWizardMetricRegistry() {
        this(new MetricRegistry());
    }

    public DropWizardMetricRegistry(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    public void startConsoleReporter(long periodMillis) {
        startConsoleReporter(periodMillis, TimeUnit.MILLISECONDS);
    }

    public void startConsoleReporter(long period, TimeUnit timeUnit) {
        if (consoleReporterStarted) {
            throw new VertexiumException("console reporter already started");
        }
        getConsoleReporter().start(period, timeUnit);
        consoleReporterStarted = true;
    }

    public void stopConsoleReporter() {
        if (!consoleReporterStarted) {
            throw new VertexiumException("console reporter not started");
        }
        getConsoleReporter().stop();
        consoleReporterStarted = false;
    }

    private ConsoleReporter getConsoleReporter() {
        if (consoleReporter == null) {
            consoleReporter = ConsoleReporter.forRegistry(getMetricRegistry())
                .build();
        }
        return consoleReporter;
    }

    public boolean isConsoleReporterStarted() {
        return consoleReporterStarted;
    }

    @Override
    public String createName(Class clazz, String... nameParts) {
        return MetricRegistry.name(clazz, nameParts);
    }

    @Override
    public Counter getCounter(String name) {
        return countersByName.computeIfAbsent(name, n -> new Counter(metricRegistry.counter(n)));
    }

    @Override
    public Iterable<Counter> getCounters() {
        return countersByName.values();
    }

    @Override
    public Timer getTimer(String name) {
        return timersByName.computeIfAbsent(name, n -> new Timer(metricRegistry.timer(n)));
    }

    @Override
    public Iterable<Timer> getTimers() {
        return timersByName.values();
    }

    @Override
    public Histogram getHistogram(String name) {
        return histogramsByName.computeIfAbsent(name, n -> new Histogram(metricRegistry.histogram(n)));
    }

    @Override
    public Iterable<Histogram> getHistograms() {
        return histogramsByName.values();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Gauge<T> getGauge(String name, Supplier<T> supplier) {
        return gaugesByName.computeIfAbsent(name, n -> {
            com.codahale.metrics.Gauge<T> g = metricRegistry.register(name, (com.codahale.metrics.Gauge<T>) supplier::get);
            return new Gauge<T>(g);
        });
    }

    @Override
    public Iterable<Gauge> getGauges() {
        return gaugesByName.values();
    }

    @Override
    public StackTraceTracker getStackTraceTracker(String name) {
        return stackTraceTrackersByName.computeIfAbsent(name, s -> new StackTraceTracker());
    }

    @Override
    public Iterable<? extends StackTraceTracker> getStackTraceTrackers() {
        return stackTraceTrackersByName.values();
    }

    @Override
    public void shutdown() {
        if (consoleReporter != null && consoleReporterStarted) {
            consoleReporter.report();
        }
        countersByName.keySet().forEach(metricRegistry::remove);
        timersByName.keySet().forEach(metricRegistry::remove);
        histogramsByName.keySet().forEach(metricRegistry::remove);
        gaugesByName.keySet().forEach(metricRegistry::remove);
        stackTraceTrackersByName.keySet().forEach(metricRegistry::remove);
    }

    public static class Gauge<T> implements org.vertexium.metric.Gauge<T> {
        private final com.codahale.metrics.Gauge gauge;

        public Gauge(com.codahale.metrics.Gauge gauge) {
            this.gauge = gauge;
        }

        public com.codahale.metrics.Gauge getGauge() {
            return gauge;
        }
    }

    public static class Counter implements org.vertexium.metric.Counter {
        private final com.codahale.metrics.Counter counter;

        public Counter(com.codahale.metrics.Counter counter) {
            this.counter = counter;
        }

        public com.codahale.metrics.Counter getCounter() {
            return counter;
        }

        @Override
        public void increment(long amount) {
            counter.inc(amount);
        }

        @Override
        public void decrement(long amount) {
            counter.dec(amount);
        }

        @Override
        public long getCount() {
            return counter.getCount();
        }
    }

    public static class Timer implements org.vertexium.metric.Timer {
        private final com.codahale.metrics.Timer timer;

        public Timer(com.codahale.metrics.Timer timer) {
            this.timer = timer;
        }

        public com.codahale.metrics.Timer getTimer() {
            return timer;
        }

        @Override
        public <T> T time(Supplier<T> supplier) {
            com.codahale.metrics.Timer.Context ctx = timer.time();
            try {
                return supplier.get();
            } finally {
                ctx.stop();
            }
        }

        @Override
        public void update(long duration, TimeUnit unit) {
            timer.update(duration, unit);
        }
    }

    public static class Histogram implements org.vertexium.metric.Histogram {
        private final com.codahale.metrics.Histogram histogram;

        public Histogram(com.codahale.metrics.Histogram histogram) {
            this.histogram = histogram;
        }

        public com.codahale.metrics.Histogram getHistogram() {
            return histogram;
        }

        @Override
        public void update(int value) {
            histogram.update(value);
        }

        @Override
        public void update(long value) {
            histogram.update(value);
        }
    }
}
