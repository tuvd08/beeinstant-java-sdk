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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static com.github.kristofa.test.http.Method.POST;

public class MetricsManagerSignatureTest {

    private static final int BEEINSTANT_PORT = 8989;
    private static final String TEST_SERVICE_NAME = "ImageSharing";
    private static final String TEST_HOST_NAME = "test.beeinstant.com";
    private static MockHttpServer server;
    private static SimpleHttpResponseProvider responseProvider;

    static {
        // stop scheduled flush
        System.setProperty("flush.manual", "true");
        System.setProperty("beeinstant.port", String.valueOf(BEEINSTANT_PORT));
        System.setProperty("publicKey", "Hello");
        System.setProperty("secretKey", "World");
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
    public void testGetRootMetricsLogger() throws UnsatisfiedExpectationException {
        responseProvider.expect(POST,
                "/PutMetric?signature=VYN3G0IpEBW2u8EDo%2B9F8sECXa8yldOCpyum7B6EmdU%3D&publicKey=Hello",
                "application/json",
                "{\"metrics\":\"d.service=ImageSharing,m.NumOfExceptions=1\n\"}").respondWith(200, "application/json", "");
        MetricsManager.getRootMetricsLogger().incCounter("NumOfExceptions", 1);
        MetricsManager.getRootMetricsLogger().flush();
        MetricsManager.flushAll();
        responseProvider.verify();
    }
}
