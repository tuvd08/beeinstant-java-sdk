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

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Group of Metrics is used to access MetricsCollectors
 */
class MetricsGroup implements Metrics {

    private final MetricsLogger metricsLogger;
    private final Set<String> dimensionsGroup;

    MetricsGroup(final MetricsLogger metricsLogger, final String... dimensionsGroup) {
        this.metricsLogger = metricsLogger;
        this.dimensionsGroup = Arrays.stream(dimensionsGroup)
                .map(dimensions -> DimensionsUtils.extendAndSerializeDimensions(metricsLogger.getRootDimensions(), dimensions))
                .filter(dimensions -> !dimensions.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    public void incCounter(final String counterName, final int value) {
        updateMetricsCollector(metricsCollector -> metricsCollector.incCounter(counterName, value));
    }

    @Override
    public long startTimer(final String timerName) {
        final AtomicLong startTime = new AtomicLong(0);
        updateMetricsCollector(metricsCollector -> startTime.set(metricsCollector.startTimer(timerName)));
        return startTime.get();
    }

    @Override
    public void stopTimer(final String timerName, long startTime) {
        updateMetricsCollector(metricsCollector -> metricsCollector.stopTimer(timerName, startTime));
    }

    @Override
    public void record(final String metricName, final double value, final Unit unit) {
        updateMetricsCollector(metricsCollector -> metricsCollector.record(metricName, value, unit));
    }

    private void updateMetricsCollector(final Consumer<MetricsCollector> consumer) {
        this.dimensionsGroup.forEach(dimensions -> {
            this.metricsLogger.updateMetricsCollector(dimensions, consumer);
        });
    }
}
