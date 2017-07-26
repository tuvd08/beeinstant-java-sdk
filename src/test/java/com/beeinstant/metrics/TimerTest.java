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

public class TimerTest {

    private Timer timer;

    @Before
    public void setUp() {
        this.timer = new Timer();
    }

    @Test
    public void testEmptyTimer() {
        Assert.assertTrue(this.timer.flushToString().isEmpty());
    }

    @Test
    public void testStopTimerBeforeStartNothingHappens() {
        this.timer.stopTimer(0);
        Assert.assertTrue("Timer has unexpected data. It should be empty", this.timer.flushToString().isEmpty());
    }

    @Test
    public void testStartButNotStopTimerNothingHappens() {
        this.timer.startTimer();
        Assert.assertTrue("Timer has unexpected data. It should be empty", this.timer.flushToString().isEmpty());
    }

    @Test
    public void testStartTimerMultipleTimesCausesReset() throws InterruptedException {
        long startTime = 0;
        startTime = this.timer.startTimer();
        Thread.sleep(100);
        startTime = this.timer.startTimer();
        Thread.sleep(100);
        this.timer.stopTimer(startTime);

        TestHelper.assertRecorderOutput(Arrays.asList(100.0), Unit.MILLI_SECOND, this.timer.flushToString(), 30.0);
        Assert.assertTrue("Some data are still left after being flushed", this.timer.flushToString().isEmpty());
    }

    @Test
    public void testStartStopTimerMultipleTimes() throws InterruptedException {
        long startTime = 0;
        startTime = this.timer.startTimer();
        Thread.sleep(100);
        this.timer.stopTimer(startTime);
        startTime = this.timer.startTimer();
        Thread.sleep(100);
        this.timer.stopTimer(startTime);

        TestHelper.assertRecorderOutput(Arrays.asList(100.0, 100.0), Unit.MILLI_SECOND, this.timer.flushToString(), 30.0);
        Assert.assertTrue("Some data are still left after being flushed", this.timer.flushToString().isEmpty());
    }

    @Test
    public void testMergeEmptyTimerNothingHappens() {
        this.timer.merge(new Timer());
        Assert.assertTrue("Timer has unexpected data. It should be empty", this.timer.flushToString().isEmpty());
    }

    @Test
    public void testMergeAfterStartAndStop() throws InterruptedException {
        final Timer timer2 = new Timer();
        this.timer.stopTimer(0);
        Assert.assertTrue("Timer has unexpected data. It should be empty", this.timer.flushToString().isEmpty());
        final long startTime = timer2.startTimer();
        Thread.sleep(300);
        timer2.stopTimer(startTime);
        this.timer.merge(timer2);
        this.timer.stopTimer(0);
        TestHelper.assertRecorderOutput(Arrays.asList(300.0), Unit.MILLI_SECOND, this.timer.flushToString(), 30.0);
        Assert.assertTrue("Some data are still left after being flushed", this.timer.flushToString().isEmpty());
        Assert.assertTrue("Some data are still left after being flushed", timer2.flushToString().isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testThrowUnsupportedOperationForCounter() {
        this.timer.incCounter(1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testThrowUnsupportedOperationForRecorder() {
        this.timer.record(1, Unit.SECOND);
    }
}
