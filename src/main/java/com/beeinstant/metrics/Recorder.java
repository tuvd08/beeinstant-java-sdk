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
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Record measurements with Recorder such as response size of a request etc
 */
class Recorder implements Metric {

    private final ConcurrentLinkedQueue<Double> values = new ConcurrentLinkedQueue<Double>();
    private final Unit unit;

    Recorder(final Unit unit) {
        this.unit = unit;
    }

    @Override
    public void record(final double value, final Unit unit) {
        if (this.unit.equals(unit)) {
            values.add(Math.max(0.0, value));
        }
    }

    @Override
    public String flushToString() {
        Double value;
        final List<Double> values = new ArrayList<Double>();
        while ((value = this.values.poll()) != null) {
            values.add(value);
        }
        if (!values.isEmpty()) {
//            return values.stream().map(String::valueOf).collect(Collectors.joining("+")) + unit;
            return values.toString().replace("[", "").replace(", ", "+").replace("]", "") + unit;
        }
        return "";
    }

    @Override
    public Metric merge(final Metric newData) {
        if (newData instanceof Recorder) {
            Double value;
            final Recorder newRecorder = (Recorder) newData;
            while ((value = newRecorder.values.poll()) != null) {
                record(value, newRecorder.unit);
            }
            return this;
        }
        throw new RuntimeException("Merge with an invalid Recorder");
    }

    @Override
    public void incCounter(final int value) {
        throw new UnsupportedOperationException("Cannot increase a counter in a Recorder");
    }

    @Override
    public long startTimer() {
        throw new UnsupportedOperationException("Cannot start a timer in a Recorder");
    }

    @Override
    public void stopTimer(long startTime) {
        throw new UnsupportedOperationException("Cannot stop a timer in a Recorder");
    }
}
