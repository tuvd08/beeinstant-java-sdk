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

import com.github.kristofa.test.http.UnsatisfiedExpectationException;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.fail;

public class MetricsManagerUninitializedTest {

    @Test
    public void testFlushAll() throws UnsatisfiedExpectationException {
        try {
            collectTestMetrics("api=Upload");
            collectTestMetrics("api=Download");
            MetricsManager.flushAll(System.currentTimeMillis() / 1000);
        } catch (Throwable e) {
            fail("Not expecting any exception");
        }
    }

    @Test
    public void testFlushIndividualMetricsLogger() throws UnsatisfiedExpectationException {
        try {
            collectTestMetrics("api=Upload").flush(System.currentTimeMillis() / 1000);
            collectTestMetrics("api=Download").flush(System.currentTimeMillis() / 1000);
            MetricsManager.flushAll(System.currentTimeMillis() / 1000);
        } catch (Throwable e) {
            fail("ot expecting any exception");
        }
    }

    @Test
    public void testGetRootMetricsLogger() throws UnsatisfiedExpectationException {
        try {
            MetricsManager.getRootMetricsLogger().incCounter("NumOfExceptions", 1);
            MetricsManager.getRootMetricsLogger().flush(System.currentTimeMillis() / 1000);
            MetricsManager.flushAll(System.currentTimeMillis() / 1000);
        } catch (Throwable e) {
            fail("Not expecting any exception");
        }
    }

    @Test
    public void testGetHostInfo() {
        Assert.assertEquals("", MetricsManager.getHostInfo());
    }

    @Test
    public void testGetServiceName() {
        Assert.assertEquals("", MetricsManager.getServiceName());
    }

    private MetricsLogger collectTestMetrics(final String dimensions) {
        final MetricsLogger metricsLogger = MetricsManager.getMetricsLogger(dimensions);
        metricsLogger.incCounter("NumOfExceptions", 1);
        metricsLogger.incCounter("Invalid@Name@Will@Be@Ignored@Logged@Emit@ErrorMetric", 1);
        return metricsLogger;
    }
}