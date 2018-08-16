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

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

class DummyLogger extends MetricsLogger {

    private static final DummyMetrics dummyMetrics = new DummyMetrics();

    DummyLogger() {
        super("service=Dummy");
    }

    @Override
    public Metrics extendDimensions(String dimensions) {
        return dummyMetrics;
    }

    @Override
    public Metrics extendMultipleDimensions(String... dimensionsGroup) {
        return dummyMetrics;
    }

    @Override
    public Metrics extendMultipleDimensionsIncludeRoot(String... dimensionsGroup) {
        return dummyMetrics;
    }

    @Override
    public void flush(long now) {
        //do nothing
    }

    @Override
    public void incCounter(String counterName, int value) {
        //do nothing
    }

    @Override
    public TimerMetric startTimer(String timerName) {
        return null;
    }

    @Override
    public void record(String metricName, double value, Unit unit) {
        //do nothing
    }

    @Override
    void flushToString(Consumer<String> consumer) {
        //do nothing
    }

    @Override
    public Map<String, String> getRootDimensions() {
        return Collections.EMPTY_MAP;
    }

    @Override
    void updateMetricsCollector(String dimensions, Consumer<MetricsCollector> consumer) {
        //do nothing
    }
}
