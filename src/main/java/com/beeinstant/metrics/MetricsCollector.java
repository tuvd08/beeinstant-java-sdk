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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collect data for Counter, Timer and Recorder
 */
class MetricsCollector implements Metrics {

    private final Map<String, Metric> metrics = new ConcurrentHashMap<String, Metric>();

    @Override
    public void incCounter(final String counterName, final int value) {
        if (DimensionsUtils.isValidName(counterName)) {
//            this.metrics.computeIfAbsent(counterName, key -> new Counter()).incCounter(value);
            Metric metric = this.metrics.get(counterName);
            if (metric == null) {
                Counter metricC = new Counter();
                if ((metric = this.metrics.putIfAbsent(counterName, metricC)) == null) {
                    metric = metricC;
                }
            }
            metric.incCounter(value);
        } else {
            MetricsManager.reportError("Invalid counter name " + counterName);
        }
    }

    @Override
    public TimerMetric startTimer(final String timerName) {
        if (DimensionsUtils.isValidName(timerName)) {
//            return new TimerMetric(this, timerName, this.metrics.computeIfAbsent(timerName, key -> new Timer()).startTimer());
            Metric metric = this.metrics.get(timerName);
            if (metric == null) {
                Timer metricT = new Timer();
                if ((metric = this.metrics.putIfAbsent(timerName, metricT)) == null) {
                    metric = metricT;
                }
            }
            return new TimerMetric(this, timerName, metric.startTimer());
        }
        MetricsManager.reportError("Invalid timer name " + timerName);
        return null;
    }

    void stopTimer(final String timerName, final long startTime) {
        if (DimensionsUtils.isValidName(timerName)) {
//            this.metrics.computeIfAbsent(timerName, key -> new Timer()).stopTimer(startTime);
            Metric metric = this.metrics.get(timerName);
            if (metric == null) {
                Timer metricT = new Timer();
                if ((metric = this.metrics.putIfAbsent(timerName, metricT)) == null) {
                    metric = metricT;
                }
            }
            metric.stopTimer(startTime);
        } else {
            MetricsManager.reportError("Invalid timer name " + timerName);
        }
    }

    @Override
    public void record(final String metricName, final double value, final Unit unit) {
        if (DimensionsUtils.isValidName(metricName)) {
//            this.metrics.computeIfAbsent(metricName, key -> new Recorder(unit)).record(value, unit);
            Metric metric = this.metrics.get(metricName);
            if (metric == null) {
                Recorder metricR = new Recorder(unit);
                if ((metric = this.metrics.putIfAbsent(metricName, metricR)) == null) {
                    metric = metricR;
                }
            }
            metric.record(value, unit);
        } else {
            MetricsManager.reportError("Invalid recorder name " + metricName);
        }
    }

    public String flushToString() {
        final List<String> metrics = new ArrayList<String>();
//        this.metrics.forEach((metricName, metricData) -> {
//            final String metricDataString = metricData.flushToString();
//            if (!metricDataString.isEmpty()) {
//                metrics.add("m." + metricName + "=" + metricDataString);
//            }
//        });
//        return metrics.stream().collect(Collectors.joining(","));
        for (String metricName : this.metrics.keySet()) {
            Metric metricData = this.metrics.get(metricName);
            final String metricDataString = metricData.flushToString();
            if (!metricDataString.isEmpty()) {
                metrics.add("m." + metricName + "=" + metricDataString);
            }
        }
        return metrics.toString().replace("[", "").replace(" ", "").replace("]", "");
    }

    void merge(final MetricsCollector metricsCollector) {
        if (this != metricsCollector) {
//            metricsCollector.metrics.forEach((metricName, metricData) -> this.metrics.merge(metricName, metricData, Metric::merge));
            Map<String, Metric> mMetrics = metricsCollector.metrics;
            for (String metricName : mMetrics.keySet()) {
                Metric fromMetricData = mMetrics.get(metricName);
                Metric toMetricData = this.metrics.get(metricName);
                if (toMetricData == null) {
                    this.metrics.put(metricName, fromMetricData);
                } else {
                    toMetricData.merge(fromMetricData);
                }
            }
        }
    }

    // for testing purpose
    Map<String, Metric> getMetrics() {
        return Collections.unmodifiableMap(this.metrics);
    }
}
