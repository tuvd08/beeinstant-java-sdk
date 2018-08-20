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

import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.concurrent.atomic.AtomicBoolean;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
//public class TimerMetric implements AutoCloseable {
public class TimerMetric {

    final private Metrics metrics;
    final private String timerName;
    final private long startTime;
    final private AtomicBoolean closed;

    public TimerMetric(final Metrics metrics, final String timerName, final long startTime) {
        this.metrics = metrics;
        this.timerName = timerName;
        this.startTime = startTime;
        this.closed = new AtomicBoolean(false);
    }

    public long getStartTime() {
        return startTime;
    }

//    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (this.metrics instanceof MetricsLogger) {
                ((MetricsLogger) this.metrics).stopTimer(timerName, startTime);
            } else if (this.metrics instanceof MetricsCollector) {
                ((MetricsCollector) this.metrics).stopTimer(timerName, startTime);
            } else if (this.metrics instanceof MetricsGroup) {
                ((MetricsGroup) this.metrics).stopTimer(timerName, startTime);
            }
        }
    }
}
