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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Count things such as number of exceptions, number of requests etc
 */
class Counter implements Metric {

    private final AtomicLong counter = new AtomicLong(-1);

    @Override
    public void incCounter(final int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Value cannot be negative");
        }
        if (!this.counter.compareAndSet(-1, value)) {
            this.counter.addAndGet(value);
        }
    }

    @Override
    public void startTimer() {
        throw new UnsupportedOperationException("Cannot start a timer in a Counter");
    }

    @Override
    public void stopTimer() {
        throw new UnsupportedOperationException("Cannot stop a timer in a Counter");
    }

    @Override
    public void record(final double value, final Unit unit) {
        throw new UnsupportedOperationException("Cannot record measurements in a Counter");
    }

    @Override
    public String flushToString() {
        if (this.counter.get() >= 0) {
            return String.valueOf(this.counter.getAndSet(-1));
        }
        return "";
    }

    @Override
    public Metric merge(final Metric newData) {
        if (newData instanceof Counter) {
            final Counter newCounter = (Counter) newData;
            final int newValue = (int) newCounter.counter.getAndSet(-1);
            if (newValue >= 0) {
                incCounter(newValue);
            }
            return this;
        }
        throw new RuntimeException("Merge with an invalid Counter");
    }
}
