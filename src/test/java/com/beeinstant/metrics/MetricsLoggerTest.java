/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 BeeInstant
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.beeinstant.metrics;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MetricsLoggerTest {

    private MetricsLogger metricsLogger;

    @Before
    public void setUp() {
        this.metricsLogger = new MetricsLogger("service=ImageSharing");
    }

    @Test
    public void testEmptyLogger() {
        Assert.assertTrue(flushMetricsLoggerToString(new MetricsLogger()).isEmpty());
    }

    @Test
    public void testLogToRootDimensions() {
        collectTestMetrics(this.metricsLogger);
        Assert.assertTrue(flushMetricsLoggerToString(this.metricsLogger).matches("d.service=ImageSharing,m.NumOfUploadedImages=1000,m.ImageSize=100.0\\+200.0kb,m.Latency=\\d+.\\dms\n"));
        Assert.assertTrue(flushMetricsLoggerToString(this.metricsLogger).isEmpty());
    }

    @Test
    public void testExtendEmptyDimensions() {
        final Metrics metrics = this.metricsLogger.extendDimensions("");
        collectTestMetrics(metrics);
        Assert.assertTrue(flushMetricsLoggerToString(this.metricsLogger).isEmpty());
    }

    @Test
    public void testExtendDimensions() {
        final Metrics metrics = this.metricsLogger.extendDimensions("api=Upload, location=Hanoi");
        collectTestMetrics(metrics);
        Assert.assertTrue(flushMetricsLoggerToString(this.metricsLogger).matches("d.api=Upload,d.location=Hanoi,d.service=ImageSharing,m.NumOfUploadedImages=1000,m.ImageSize=100.0\\+200.0kb,m.Latency=\\d+.\\dms\n"));
        Assert.assertTrue(flushMetricsLoggerToString(this.metricsLogger).isEmpty());
        // can reuse metrics object after flush
        collectTestMetrics(metrics);
        Assert.assertTrue(flushMetricsLoggerToString(this.metricsLogger).matches("d.api=Upload,d.location=Hanoi,d.service=ImageSharing,m.NumOfUploadedImages=1000,m.ImageSize=100.0\\+200.0kb,m.Latency=\\d+.\\dms\n"));
        Assert.assertTrue(flushMetricsLoggerToString(this.metricsLogger).isEmpty());
    }

    @Test
    public void testExtendMultipleDimensions() {
        final Metrics metrics = this.metricsLogger.extendMultipleDimensions("api=Upload, location=Hanoi", "api=Download", "api=Download", "");
        collectTestMetrics(metrics);
        Assert.assertTrue(flushMetricsLoggerToString(this.metricsLogger).matches("d.api=Download,d.service=ImageSharing,m.NumOfUploadedImages=1000,m.ImageSize=100.0\\+200.0kb,m.Latency=\\d+.\\dms\n" +
                "d.api=Upload,d.location=Hanoi,d.service=ImageSharing,m.NumOfUploadedImages=1000,m.ImageSize=100.0\\+200.0kb,m.Latency=\\d+.\\dms\n"));
        Assert.assertTrue(flushMetricsLoggerToString(this.metricsLogger).isEmpty());
        // can reuse metrics object after flush
        collectTestMetrics(metrics);
        Assert.assertTrue(flushMetricsLoggerToString(this.metricsLogger).matches("d.api=Download,d.service=ImageSharing,m.NumOfUploadedImages=1000,m.ImageSize=100.0\\+200.0kb,m.Latency=\\d+.\\dms\n" +
                "d.api=Upload,d.location=Hanoi,d.service=ImageSharing,m.NumOfUploadedImages=1000,m.ImageSize=100.0\\+200.0kb,m.Latency=\\d+.\\dms\n"));
        Assert.assertTrue(flushMetricsLoggerToString(this.metricsLogger).isEmpty());
    }

    @Test
    public void testExtendMultipleDimensionsIncludeRoot() {
        final Metrics metrics = this.metricsLogger.extendMultipleDimensionsIncludeRoot("api=Upload, location=Hanoi", "api=Download");
        collectTestMetrics(metrics);
        Assert.assertTrue(flushMetricsLoggerToString(this.metricsLogger).matches(
                "d.api=Upload,d.location=Hanoi,d.service=ImageSharing,m.NumOfUploadedImages=1000,m.ImageSize=100.0\\+200.0kb,m.Latency=\\d+.\\dms\n" +
                "d.api=Download,d.service=ImageSharing,m.NumOfUploadedImages=1000,m.ImageSize=100.0\\+200.0kb,m.Latency=\\d+.\\dms\n" +
                "d.service=ImageSharing,m.NumOfUploadedImages=1000,m.ImageSize=100.0\\+200.0kb,m.Latency=\\d+.\\dms\n"));
        Assert.assertTrue(flushMetricsLoggerToString(this.metricsLogger).isEmpty());
        // can reuse metrics object after flush
        collectTestMetrics(metrics);
        Assert.assertTrue(flushMetricsLoggerToString(this.metricsLogger).matches(
                "d.api=Upload,d.location=Hanoi,d.service=ImageSharing,m.NumOfUploadedImages=1000,m.ImageSize=100.0\\+200.0kb,m.Latency=\\d+.\\dms\n" +
                "d.api=Download,d.service=ImageSharing,m.NumOfUploadedImages=1000,m.ImageSize=100.0\\+200.0kb,m.Latency=\\d+.\\dms\n" +
                "d.service=ImageSharing,m.NumOfUploadedImages=1000,m.ImageSize=100.0\\+200.0kb,m.Latency=\\d+.\\dms\n"));
        Assert.assertTrue(flushMetricsLoggerToString(this.metricsLogger).isEmpty());
    }

    @Test
    public void testLoggingAndFlushingMetricsInMultipleThreads() throws InterruptedException {
        final ExecutorService executor = Executors.newWorkStealingPool();
        final List<Callable<Void>> tasks = new ArrayList<>();
        final ConcurrentLinkedQueue<String> output = new ConcurrentLinkedQueue<>();

        // start 30 threads to collect recorder, timer and counter values, collect 1500 times
        tasks.addAll(createCounterTasks(this.metricsLogger, 50, 10));
        tasks.addAll(createTimerTasks(this.metricsLogger, 50, 10));
        tasks.addAll(createRecorderTasks(this.metricsLogger, 50, 10));

        // start 10 threads to flush randomly while collecting metric data is still in progress
        tasks.addAll(createFushTasks(this.metricsLogger, 10, output));

        Collections.shuffle(tasks);

        executor.invokeAll(tasks).forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        executor.shutdown();
        executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);

        // flush everything left
        output.add(flushMetricsLoggerToString(this.metricsLogger));

        final List<Double> recorderValues = new ArrayList<>();
        final List<Double> timerValues = new ArrayList<>();
        final List<Double> counterValues = new ArrayList<>();
        final String expectedDimensions = "d.service=ImageSharing,";

        // collect metric data from flushed strings
        output.forEach(logEntry -> {
            if (!logEntry.isEmpty()) {
                Assert.assertTrue(logEntry.startsWith(expectedDimensions));
                Assert.assertTrue(logEntry.length() > expectedDimensions.length());
                assertAndExtractValues(recorderValues, logEntry, "MyRecorder", "s");
                assertAndExtractValues(timerValues, logEntry, "MyTimer", "ms");
                assertAndExtractValues(counterValues, logEntry, "MyCounter", "");
            }
        });

        // assert recorder
        Assert.assertEquals(500, recorderValues.size());
        Assert.assertEquals(500, recorderValues.stream().mapToDouble(Double::doubleValue).sum(), 0.0);
        // assert timer
        Assert.assertEquals(500, timerValues.size());
        // assert counter
        Assert.assertEquals(500, counterValues.stream().mapToDouble(Double::doubleValue).sum(), 0.0);
    }

    private void assertAndExtractValues(final List<Double> values, final String logEntry, final String metricName, final String unit) {
        final String recorderValuesString = extractMetricValues(metricName, logEntry);
        if (!recorderValuesString.isEmpty()) {
            Assert.assertTrue(recorderValuesString.endsWith(unit));
            values.addAll(convertValuesStringToList(recorderValuesString.substring(0, recorderValuesString.length() - unit.length())));
        }
    }

    private Collection<? extends Double> convertValuesStringToList(final String valuesString) {
        final String[] values = valuesString.split("\\+");
        return Arrays.stream(values).map(Double::parseDouble).collect(Collectors.toList());
    }

    private String extractMetricValues(final String metricName, final String output) {
        final Pattern reg = Pattern.compile(".+m." + metricName + "=([^\\n,]+).*");
        final Matcher matcher = reg.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private Collection<? extends Callable<Void>> createFushTasks(final MetricsLogger metricsLogger, final int numOfTasks, final ConcurrentLinkedQueue<String> output) {
        final List<Callable<Void>> tasks = new ArrayList<>();
        final Random rand = new Random();
        for (int i = 0; i < numOfTasks; i++) {
            tasks.add(() -> {
                Thread.sleep(rand.nextInt(30));
                output.add(flushMetricsLoggerToString(metricsLogger));
                return null;
            });
        }
        return tasks;
    }

    private Collection<? extends Callable<Void>> createRecorderTasks(final MetricsLogger metricsLogger, final int numOfSamples, final int numOfTasks) {
        final List<Callable<Void>> tasks = new ArrayList<>();
        final Random rand = new Random();
        for (int i = 0; i < numOfTasks; i++) {
            tasks.add(() -> {
                for (int j = 0; j < numOfSamples; j++) {
                    Thread.sleep(rand.nextInt(50));
                    metricsLogger.record("MyRecorder", 1, Unit.SECOND);
                }
                return null;
            });
        }
        return tasks;
    }

    private Collection<? extends Callable<Void>> createTimerTasks(final MetricsLogger metricsLogger, final int numOfSamples, final int numOfTasks) {
        final List<Callable<Void>> tasks = new ArrayList<>();
        final Random rand = new Random();
        for (int i = 0; i < numOfTasks; i++) {
            tasks.add(() -> {
                for (int j = 0; j < numOfSamples; j++) {
                    Thread.sleep(rand.nextInt(40));
                    final long startTime;
                    startTime = metricsLogger.startTimer("MyTimer");
                    Thread.sleep(rand.nextInt(10));
                    metricsLogger.stopTimer("MyTimer", startTime);
                }
                return null;
            });
        }
        return tasks;
    }

    private Collection<? extends Callable<Void>> createCounterTasks(final MetricsLogger metricsLogger, final int numOfSamples, final int numOfTasks) {
        final List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < numOfTasks; i++) {
            tasks.add(() -> {
                for (int j = 0; j < numOfSamples; j++) {
                    Thread.sleep(80);
                    metricsLogger.incCounter("MyCounter", 1);
                }
                return null;
            });
        }
        return tasks;
    }

    private void collectTestMetrics(final Metrics metrics) {
        final long startTime;
        metrics.incCounter("NumOfUploadedImages", 1000);
        startTime = metrics.startTimer("Latency");
        metrics.stopTimer("Latency", startTime);
        metrics.record("ImageSize", 100, Unit.KILO_BYTE);
        metrics.record("ImageSize", 200, Unit.KILO_BYTE);
    }

    private String flushMetricsLoggerToString(final MetricsLogger metricsLogger) {
        final StringBuilder sb = new StringBuilder();
        metricsLogger.flushToString(logEntry -> {
            sb.append(logEntry);
            sb.append("\n");
        });
        return sb.toString();
    }
}
