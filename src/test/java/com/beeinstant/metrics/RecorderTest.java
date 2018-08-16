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
import java.util.List;

public class RecorderTest {

    private Recorder recorder;

    @Before
    public void setUp() {
        this.recorder = new Recorder(Unit.MILLI_SECOND);
    }

    @Test
    public void testEmptyRecorder() {
        Assert.assertTrue(this.recorder.flushToString().isEmpty());
    }

    @Test
    public void testRecordersWithDifferentUnits() {
        assertValuesAndUnit("1.0ns", Arrays.asList(1.0), Unit.NANO_SECOND);
        assertValuesAndUnit("1.0+2.0us", Arrays.asList(1.0, 2.0), Unit.MICRO_SECOND);
        assertValuesAndUnit("1.0+2.0ms", Arrays.asList(1.0, 2.0), Unit.MILLI_SECOND);
        assertValuesAndUnit("1.0+2.0s", Arrays.asList(1.0, 2.0), Unit.SECOND);
        assertValuesAndUnit("1.0+2.0m", Arrays.asList(1.0, 2.0), Unit.MINUTE);
        assertValuesAndUnit("1.0+2.0h", Arrays.asList(1.0, 2.0), Unit.HOUR);
        assertValuesAndUnit("1.0+2.0b", Arrays.asList(1.0, 2.0), Unit.BYTE);
        assertValuesAndUnit("1.0+2.0kb", Arrays.asList(1.0, 2.0), Unit.KILO_BYTE);
        assertValuesAndUnit("1.0+2.0mb", Arrays.asList(1.0, 2.0), Unit.MEGA_BYTE);
        assertValuesAndUnit("1.0+2.0gb", Arrays.asList(1.0, 2.0), Unit.GIGA_BYTE);
        assertValuesAndUnit("1.0+2.0tb", Arrays.asList(1.0, 2.0), Unit.TERA_BYTE);
        assertValuesAndUnit("1.0+2.0bps", Arrays.asList(1.0, 2.0), Unit.BIT_PER_SEC);
        assertValuesAndUnit("1.0+2.0kbps", Arrays.asList(1.0, 2.0), Unit.KILO_BIT_PER_SEC);
        assertValuesAndUnit("1.0+2.0mbps", Arrays.asList(1.0, 2.0), Unit.MEGA_BIT_PER_SEC);
        assertValuesAndUnit("1.0+2.0gbps", Arrays.asList(1.0, 2.0), Unit.GIGA_BIT_PER_SEC);
        assertValuesAndUnit("1.0+2.0tbps", Arrays.asList(1.0, 2.0), Unit.TERA_BIT_PER_SEC);
        assertValuesAndUnit("1.0+2.0p", Arrays.asList(1.0, 2.0), Unit.PERCENT);
        assertValuesAndUnit("1.0+2.0", Arrays.asList(1.0, 2.0), Unit.NONE);
    }

    @Test
    public void testMergeEmptyRecorderNothingHappens() {
        this.recorder.record(1, Unit.MILLI_SECOND);
        final Recorder recorder2 = new Recorder(Unit.MILLI_SECOND);
        this.recorder.merge(recorder2);
        Assert.assertEquals("1.0ms", this.recorder.flushToString());
        Assert.assertTrue("Some data are still left after being flushed", this.recorder.flushToString().isEmpty());
    }

    @Test(expected = RuntimeException.class)
    public void testMergeInvalidRecorderThrowException() {
        final Counter counter = new Counter();
        this.recorder.merge(counter);
    }

    @Test
    public void testMergeValidRecorder() {
        this.recorder.record(200, Unit.MILLI_SECOND);
        final Recorder recorder2 = new Recorder(Unit.MILLI_SECOND);
        recorder2.record(100, Unit.MILLI_SECOND);
        this.recorder.merge(recorder2);
        Assert.assertEquals("200.0+100.0ms", this.recorder.flushToString());
        Assert.assertTrue("Some data are still left after being flushed", this.recorder.flushToString().isEmpty());
        Assert.assertTrue("Some data are still left after being flushed", recorder2.flushToString().isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testThrowUnsupportedOperationForCounter() {
        this.recorder.incCounter(1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testThrowUnsupportedOperationForTimerStart() {
        this.recorder.startTimer();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testThrowUnsupportedOperationForTimerStop() {
        this.recorder.stopTimer(0);
    }

    private void assertValuesAndUnit(final String expectedOutput, final List<Double> values, final Unit unit) {
        final Recorder recorder = new Recorder(unit);
//        values.forEach(value -> recorder.record(value, unit));
        for (Double value : values) {
            recorder.record(value, unit);
        }
        Assert.assertEquals(expectedOutput, recorder.flushToString());
        Assert.assertTrue("Some data are still left after being flushed", recorder.flushToString().isEmpty());
    }
}
