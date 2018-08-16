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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Log metrics for dimensions. This class is thread-safe.
 */
public class MetricsLogger implements Metrics {

    private final Map<String, MetricsCollector> metricsCollectors = new ConcurrentHashMap<String, MetricsCollector>();
    private final Map<String, String> rootDimensions;
    private final MetricsGroup rootMetricsGroup;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    MetricsLogger() {
        this("");
    }

    MetricsLogger(final String dimensions) {
        this(DimensionsUtils.parseDimensions(dimensions));
    }

    MetricsLogger(final Map<String, String> dimensionsMap) {
        this.rootDimensions = dimensionsMap;
        this.rootMetricsGroup = new MetricsGroup(this, getRootDimensionsString());
    }

    /**
     * Extend the root dimensions with new dimensions.
     * For example:
     *     The root dimensions is "service=AwesomeService".
     *     extendDimensions("api=Upload") will create new dimensions "service=AwesomeService, api=Upload".
     *
     * @param dimensions, new dimensions
     * @return metrics object which contains new dimensions
     */
    public Metrics extendDimensions(final String dimensions) {
        return new MetricsGroup(this, dimensions);
    }

    /**
     * Extend the root dimensions with a group of new dimensions.
     * For example:
     *     The root dimensions is "service=AwesomeService".
     *     extendDimensions("api=Upload", "api=Download") will create new dimensions
     *         "service=AwesomeService, api=Upload" and "service=AwesomeService, api=Download".
     *
     * @param dimensionsGroup, group of new dimensions to extend the root dimensions
     * @return metrics object which contains a group of new dimensions
     */
    public Metrics extendMultipleDimensions(final String... dimensionsGroup) {
        return new MetricsGroup(this, dimensionsGroup);
    }

    /**
     * Extend the root dimensions with a group of new dimensions, but also include the root dimensions into the group.
     * For example:
     *    The root dimensions is "service=AwesomeService".
     *    extendMultipleDimensionsIncludeRoot("api=Upload") will create a metrics object which contains
     *        the root dimensions "service=AwesomeService" and the new one "service=AwesomeService, api=Upload".
     *
     * @param dimensionsGroup, group of dimensions
     * @return metrics object which contains a group of dimensions
     */
    public Metrics extendMultipleDimensionsIncludeRoot(final String... dimensionsGroup) {
        final String[] dimensionsGroupWithRoot = Arrays.copyOf(dimensionsGroup, dimensionsGroup.length + 1);
        // root dimensions is equivalent to extend root dimensions with no new dimension
        dimensionsGroupWithRoot[dimensionsGroup.length] = getRootDimensionsString();
        return new MetricsGroup(this, dimensionsGroupWithRoot);
    }

    public void flush(long now) {
        MetricsManager.flushMetricsLogger(this);
        MetricsManager.flushToServer(now);
    }

    @Override
    public void incCounter(final String counterName, final int value) {
        this.rootMetricsGroup.incCounter(counterName, value);
    }

    @Override
    public TimerMetric startTimer(final String timerName) {
        return this.rootMetricsGroup.startTimer(timerName);
    }

    void stopTimer(final String timerName, final long startTime) {
        this.rootMetricsGroup.stopTimer(timerName, startTime);
    }

    @Override
    public void record(final String metricName, final double value, final Unit unit) {
        this.rootMetricsGroup.record(metricName, value, unit);
    }

//    void flushToString(final Consumer<String> consumer) {
//        final Map<String, MetricsCollector> readyToFlush = new HashMap<>();
//
//        lock.writeLock().lock();
//        try {
//            readyToFlush.putAll(this.metricsCollectors);
//            readyToFlush.forEach(this.metricsCollectors::remove);
//        } finally {
//            lock.writeLock().unlock();
//        }
//
//        // do actual flush outside of critical section
//        readyToFlush.forEach((dimensions, metricsCollector) -> {
//            final String metricsString = metricsCollector.flushToString();
//            if (!metricsString.isEmpty()) {
//                consumer.accept(dimensions + "," + metricsString);
//            }
//        });
//    }

    public Map<String, MetricsCollector> flushToString() {
        lock.writeLock().lock();
        final Map<String, MetricsCollector> readyToFlush = new HashMap<String, MetricsCollector>();
        try {
            readyToFlush.putAll(this.metricsCollectors);
            this.metricsCollectors.clear();
        } finally {
            lock.writeLock().unlock();
        }
        return readyToFlush;
    }
    public 
    
    
    Map<String, String> getRootDimensions() {
        return this.rootDimensions;
    }

//    void updateMetricsCollector(final String dimensions, final Consumer<MetricsCollector> consumer) {
//        final MetricsCollector metricsCollector = this.metricsCollectors.computeIfAbsent(dimensions, key -> new MetricsCollector());
//        consumer.accept(metricsCollector);
//        if (metricsCollector != this.metricsCollectors.get(dimensions)) {
//            addOrMergeMetricsCollector(dimensions, metricsCollector);
//        }
//    }

    public MetricsCollector getUpdateMetricsCollector(final String dimensions) {
        MetricsCollector metricsCollector = this.metricsCollectors.get(dimensions);
        if (metricsCollector == null) {
            metricsCollector = new MetricsCollector();
            this.metricsCollectors.put(dimensions, metricsCollector);
        }
        if (metricsCollector != this.metricsCollectors.get(dimensions)) {
            addOrMergeMetricsCollector(dimensions, metricsCollector);
        }
        return metricsCollector;
    }

    private void addOrMergeMetricsCollector(final String dimensions, final MetricsCollector metricsCollector) {
        lock.readLock().lock();
        try {
//            this.metricsCollectors.computeIfAbsent(dimensions, key -> metricsCollector).merge(metricsCollector);
            MetricsCollector oldMetricsCollector = this.metricsCollectors.get(dimensions);
            if (oldMetricsCollector == null) {
                oldMetricsCollector = this.metricsCollectors.putIfAbsent(dimensions, metricsCollector);
                if (oldMetricsCollector == null) {
                    oldMetricsCollector = metricsCollector;
                }
            }
            oldMetricsCollector.merge(metricsCollector);
        } finally {
            lock.readLock().unlock();
        }
    }

    private String getRootDimensionsString() {
        //return this.rootDimensions.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining(","));
        return this.rootDimensions.toString().replace("{", "").replace(" ", "").replace("}", "");
    }
}
