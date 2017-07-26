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

import com.github.kristofa.test.http.MockHttpServer;
import com.github.kristofa.test.http.SimpleHttpResponseProvider;
import com.github.kristofa.test.http.UnsatisfiedExpectationException;
import org.junit.*;

import java.io.IOException;

import static com.github.kristofa.test.http.Method.POST;

public class MetricsManagerTest {

    private static final int BEEINSTANT_PORT = 8989;
    private static final String TEST_SERVICE_NAME = "ImageSharing";
    private static final String TEST_HOST_NAME = "test.beeinstant.com";
    private static MockHttpServer server;
    private static SimpleHttpResponseProvider responseProvider;

    static {
        // stop scheduled flush
        System.setProperty("beeinstant.flush.manual", "true");
        System.setProperty("beeinstant.endpoint", "http://localhost:" + BEEINSTANT_PORT);
        MetricsManager.init(TEST_SERVICE_NAME, TEST_HOST_NAME);
    }

    @BeforeClass
    public static void startMockBeeInstantServer() throws IOException {
        responseProvider = new SimpleHttpResponseProvider();
        server = new MockHttpServer(BEEINSTANT_PORT, responseProvider);
        server.start();
    }

    @AfterClass
    public static void stopMockBeeInstantServer() throws IOException {
        server.stop();
    }

    @After
    public void tearDown() throws IOException {
        responseProvider.reset();
    }

    @Test
    public void testFlushAll() throws UnsatisfiedExpectationException {
        responseProvider.expect(POST, "/PutMetric", "application/json", "{\"metrics\":\"d.service=ImageSharing,m.MetricErrors=2\nd.api=Upload,d.service=ImageSharing,m.NumOfExceptions=1\nd.api=Download,d.service=ImageSharing,m.NumOfExceptions=1\n\"}").respondWith(200, "application/json", "");
        collectTestMetrics("api=Upload");
        collectTestMetrics("api=Download");
        MetricsManager.flushAll();
        responseProvider.verify();
    }

    @Test
    public void testFlushIndividualMetricsLogger() throws UnsatisfiedExpectationException {
        responseProvider.expect(POST, "/PutMetric", "application/json", "{\"metrics\":\"d.api=Upload,d.service=ImageSharing,m.NumOfExceptions=1\nd.service=ImageSharing,m.MetricErrors=1\n\"}").respondWith(200, "application/json", "");
        responseProvider.expect(POST, "/PutMetric", "application/json", "{\"metrics\":\"d.api=Download,d.service=ImageSharing,m.NumOfExceptions=1\nd.service=ImageSharing,m.MetricErrors=1\n\"}").respondWith(200, "application/json", "");
        collectTestMetrics("api=Upload").flush();
        collectTestMetrics("api=Download").flush();
        MetricsManager.flushAll();
        responseProvider.verify();
    }

    @Test
    public void testGetRootMetricsLogger() throws UnsatisfiedExpectationException {
        responseProvider.expect(POST, "/PutMetric", "application/json", "{\"metrics\":\"d.service=ImageSharing,m.NumOfExceptions=1\n\"}").respondWith(200, "application/json", "");
        MetricsManager.getRootMetricsLogger().incCounter("NumOfExceptions", 1);
        MetricsManager.getRootMetricsLogger().flush();
        MetricsManager.flushAll();
        responseProvider.verify();
    }

    @Test
    public void testExtendInvalidDimensionsIgnoreAndReportError() throws UnsatisfiedExpectationException {
        responseProvider.expect(POST, "/PutMetric", "application/json", "{\"metrics\":\"d.service=ImageSharing,m.MetricErrors=2\n\"}").respondWith(200, "application/json", "");
        final MetricsLogger metricsLogger = MetricsManager.getMetricsLogger("api=Upload");
        metricsLogger.extendDimensions("invalid-dimensions").incCounter("NumOfExceptions", 1);
        metricsLogger.extendDimensions("invalid=@dimensions").incCounter("NumOfExceptions", 1);
        MetricsManager.flushAll();
        responseProvider.verify();
    }

    @Test
    public void testGetMetricsLoggerWithInvalidDimensionsThrowException() {
        try {
            collectTestMetrics("invalidDimensions");
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        } finally {
            MetricsManager.flushAll();
        }
    }

    @Test
    public void testGetHostInfo() {
        Assert.assertEquals(TEST_HOST_NAME, MetricsManager.getHostInfo());
    }

    @Test
    public void testGetServiceName() {
        Assert.assertEquals(TEST_SERVICE_NAME, MetricsManager.getServiceName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidServiceNameThrowException() throws IOException {
        MetricsManager.init("Invalid@Service@Name");
    }

    private MetricsLogger collectTestMetrics(final String dimensions) {
        final MetricsLogger metricsLogger = MetricsManager.getMetricsLogger(dimensions);
        metricsLogger.incCounter("NumOfExceptions", 1);
        metricsLogger.incCounter("Invalid@Name@Will@Be@Ignored@Logged@Emit@ErrorMetric", 1);
        return metricsLogger;
    }
}