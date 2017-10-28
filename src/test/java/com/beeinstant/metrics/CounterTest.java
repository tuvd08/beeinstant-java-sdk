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

public class CounterTest {

    private Counter counter;

    @Before
    public void setUp() {
        this.counter = new Counter();
    }

    @Test
    public void testEmptyCounter() {
        Assert.assertTrue(this.counter.flushToString().isEmpty());
    }

    @Test
    public void testAddNonNegativeValues() {
        this.counter.incCounter(0);
        this.counter.incCounter(1);
        this.counter.incCounter(2);
        this.counter.incCounter(3);
        Assert.assertEquals("Counter is not added up correctly", "6", this.counter.flushToString());
        Assert.assertTrue("Still some data left after being flushed", this.counter.flushToString().isEmpty());
        this.counter.incCounter(1);
        Assert.assertEquals("Cannot inc counter after a flush", "1", this.counter.flushToString());
    }

    @Test
    public void testMergeEmptyCounterNothingHappens() {
        this.counter.incCounter(1);
        final Counter counter2 = new Counter();
        this.counter.merge(counter2);
        Assert.assertEquals("Counter value is not correct", "1", this.counter.flushToString());
        Assert.assertTrue("Still some data left after being flushed", this.counter.flushToString().isEmpty());
    }

    @Test(expected = RuntimeException.class)
    public void testMergeInvalidCounterThrowException() {
        this.counter.incCounter(1);
        final Recorder recorder = new Recorder(Unit.MILLI_SECOND);
        this.counter.merge(recorder);
    }

    @Test
    public void testMergeValidCounter() {
        final Counter counter2 = new Counter();
        counter2.incCounter(1);
        this.counter.merge(counter2);
        final Counter counter3 = new Counter();
        counter3.incCounter(2);
        this.counter.merge(counter3);
        Assert.assertEquals("Counter does not have value added up by merge", "3", this.counter.flushToString());
        Assert.assertTrue("Still some data left after being flushed", counter2.flushToString().isEmpty());
        Assert.assertTrue("Still some data left after being flushed", counter3.flushToString().isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testThrowUnsupportedOperationForTimerStart() {
        this.counter.startTimer();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testThrowUnsupportedOperationForTimerStop() {
        this.counter.stopTimer(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testThrowUnsupportedOperationForRecorder() {
        this.counter.record(1, Unit.SECOND);
    }
}
