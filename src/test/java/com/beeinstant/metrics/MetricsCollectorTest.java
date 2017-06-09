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

import java.util.Arrays;
import java.util.Map;

public class MetricsCollectorTest {

    private MetricsCollector metricsCollector;

    @Before
    public void setUp() {
        this.metricsCollector = new MetricsCollector();
    }

    @Test
    public void testIncCounters() {
        this.metricsCollector.incCounter("NumOfExceptions", 0);
        this.metricsCollector.incCounter("NumOfExceptions", 1);
        this.metricsCollector.incCounter("Succeeded", 1);
        this.metricsCollector.incCounter("Succeeded", 2);

        final Map<String, Metric> metrics = this.metricsCollector.getMetrics();
        Assert.assertEquals(2, metrics.size());
        Assert.assertEquals("1", metrics.get("NumOfExceptions").flushToString());
        Assert.assertEquals("3", metrics.get("Succeeded").flushToString());
        Assert.assertTrue("Some data are still left after being flushed", metrics.get("NumOfExceptions").flushToString().isEmpty());
        Assert.assertTrue("Some data are still left after being flushed", metrics.get("Succeeded").flushToString().isEmpty());
    }

    @Test
    public void testStartStopTimers() throws InterruptedException {
        this.metricsCollector.startTimer("Clock1");
        Thread.sleep(100);
        this.metricsCollector.stopTimer("Clock1");
        this.metricsCollector.startTimer("Clock2");
        Thread.sleep(200);
        this.metricsCollector.stopTimer("Clock2");

        final Map<String, Metric> metrics = this.metricsCollector.getMetrics();
        Assert.assertEquals(2, metrics.size());
        TestHelper.assertRecorderOutput(Arrays.asList(100.0), Unit.MILLI_SECOND, metrics.get("Clock1").flushToString(), 10.0);
        TestHelper.assertRecorderOutput(Arrays.asList(200.0), Unit.MILLI_SECOND, metrics.get("Clock2").flushToString(), 10.0);
        Assert.assertTrue("Some data are still left after being flushed", metrics.get("Clock1").flushToString().isEmpty());
        Assert.assertTrue("Some data are still left after being flushed", metrics.get("Clock2").flushToString().isEmpty());
    }

    @Test
    public void testRecorders() {
        this.metricsCollector.record("Recorder1", 1.0, Unit.MILLI_SECOND);
        this.metricsCollector.record("Recorder1", 11.0, Unit.MILLI_SECOND);
        this.metricsCollector.record("Recorder2", 2.0, Unit.SECOND);
        this.metricsCollector.record("Recorder2", 22.0, Unit.SECOND);
        final Map<String, Metric> metrics = this.metricsCollector.getMetrics();
        Assert.assertEquals(2, metrics.size());
        TestHelper.assertRecorderOutput(Arrays.asList(1.0, 11.0), Unit.MILLI_SECOND, metrics.get("Recorder1").flushToString(), 0.0);
        TestHelper.assertRecorderOutput(Arrays.asList(2.0, 22.0), Unit.SECOND, metrics.get("Recorder2").flushToString(), 0.0);
        Assert.assertTrue("Some data are still left after being flushed", metrics.get("Recorder1").flushToString().isEmpty());
        Assert.assertTrue("Some data are still left after being flushed", metrics.get("Recorder2").flushToString().isEmpty());
    }

    @Test
    public void testIgnoreInvalidMetricNames() {
        this.metricsCollector.incCounter("Invalid@Name", 1);
        this.metricsCollector.startTimer("Invalid@Name");
        this.metricsCollector.stopTimer("Invalid@Name");
        this.metricsCollector.record("Invalid@Name", 1, Unit.SECOND);
        Assert.assertTrue(this.metricsCollector.flushToString().isEmpty());
        Assert.assertTrue(this.metricsCollector.flushToString().isEmpty());
    }

    @Test
    public void testFlushEmptyMetricsCollectorToString() {
        Assert.assertTrue(this.metricsCollector.flushToString().isEmpty());
    }

    @Test
    public void testFlushMetricsCollectorToString() {
        this.metricsCollector.incCounter("MyCounter", 99);
        this.metricsCollector.startTimer("MyTimer");
        this.metricsCollector.stopTimer("MyTimer");
        this.metricsCollector.record("Recorder", 100, Unit.BYTE);
        Assert.assertTrue(this.metricsCollector.flushToString().matches("m.MyTimer=\\d.\\dms,m.MyCounter=99,m.Recorder=100.0b"));
        Assert.assertTrue("Some data are still left after being flushed", this.metricsCollector.flushToString().isEmpty());
    }

    @Test
    public void testMergeMetricsCollectorDoNotMergeItself() {
        assertMerge(this.metricsCollector, "m.MyTimer=\\d.\\dms,m.MyCounter=99,m.Recorder=100.0b");
    }

    @Test
    public void testMergeEmptyMetricsCollectorNothingChanges() {
        assertMerge(new MetricsCollector(), "m.MyTimer=\\d.\\dms,m.MyCounter=99,m.Recorder=100.0b");
    }

    @Test(expected = RuntimeException.class)
    public void testMergeInvalidTimerThrowException() {
        this.metricsCollector.startTimer("MyTimer");
        final MetricsCollector metricsCollector2 = new MetricsCollector();
        metricsCollector2.incCounter("MyTimer", 1);
        this.metricsCollector.merge(metricsCollector2);
    }

    @Test(expected = RuntimeException.class)
    public void testMergeInvalidCounterThrowException() {
        this.metricsCollector.incCounter("MyCounter", 1);
        final MetricsCollector metricsCollector2 = new MetricsCollector();
        metricsCollector2.record("MyCounter", 1, Unit.SECOND);
        this.metricsCollector.merge(metricsCollector2);
    }

    @Test(expected = RuntimeException.class)
    public void testMergeInvalidRecorderThrowException() {
        this.metricsCollector.record("MyRecorder", 1, Unit.MILLI_SECOND);
        final MetricsCollector metricsCollector2 = new MetricsCollector();
        metricsCollector2.incCounter("MyRecorder", 1);
        this.metricsCollector.merge(metricsCollector2);
    }

    @Test
    public void testMergeMetricsCollector() {
        final MetricsCollector metricsCollector2 = new MetricsCollector();
        metricsCollector2.incCounter("MyCounter", 2);
        metricsCollector2.startTimer("MyTimer");
        metricsCollector2.stopTimer("MyTimer");
        metricsCollector2.record("Recorder", 200, Unit.BYTE);
        metricsCollector2.incCounter("MyCounter2", 1);
        metricsCollector2.startTimer("MyTimer2");
        metricsCollector2.stopTimer("MyTimer2");
        metricsCollector2.record("Recorder2", 300, Unit.SECOND);
        assertMerge(metricsCollector2, "m.MyTimer=\\d.\\d\\+\\d.\\dms,m.MyCounter=101,m.MyTimer2=\\d.\\dms,m.Recorder=100.0\\+200.0b,m.Recorder2=300.0s,m.MyCounter2=1");
    }

    private void assertMerge(final MetricsCollector metricsCollector, final String expectedOutput) {
        this.metricsCollector.incCounter("MyCounter", 99);
        this.metricsCollector.startTimer("MyTimer");
        this.metricsCollector.stopTimer("MyTimer");
        this.metricsCollector.record("Recorder", 100, Unit.BYTE);
        this.metricsCollector.merge(metricsCollector);
        Assert.assertTrue(this.metricsCollector.flushToString().matches(expectedOutput));
        Assert.assertTrue("Some data are still left after being flushed", this.metricsCollector.flushToString().isEmpty());
        Assert.assertTrue("Some data are still left after being flushed", metricsCollector.flushToString().isEmpty());
    }
}
