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

package com.beeinstant.metrics.integration;

import com.beeinstant.metrics.MetricsManager;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;

import static com.jayway.jsonpath.JsonPath.read;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MetricsManagerIT {

    private static final String TEST_SERVICE_NAME = "test";
    private static final String TEST_BEEINSTANT_HOST = "192.168.1.11";

    @Before
    public void setUp() {
        System.setProperty("flush.interval", "5");
        System.setProperty("flush.startDelay", "5");
        System.setProperty("beeinstant.host", TEST_BEEINSTANT_HOST);
        MetricsManager.init(TEST_SERVICE_NAME, TEST_BEEINSTANT_HOST);
    }

    @Test
    public void testEmitMetricsToBeeInstantServer() throws IOException, InterruptedException {

        // Use automatic flush
        MetricsManager.getMetricsLogger("api=Upload").incCounter("NumOfExceptionsTest", 1);
        MetricsManager.getMetricsLogger("api=Download").incCounter("NumOfMetricsTest", 1);

        Thread.sleep(10000);

        HttpClient client = HttpClientBuilder.create().build();
        HttpPost searchMetricCommand = new HttpPost("/SearchMetric");
        searchMetricCommand.setEntity(new StringEntity("{\"query\":\"Test\", \"limit\":\"50\"}"));
        HttpResponse response = client.execute(HttpHost.create("http://"+ TEST_BEEINSTANT_HOST + ":9999"), searchMetricCommand);
        assertEquals(response.getStatusLine().getStatusCode(), 200);
        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));

        String output;
        StringBuilder builder = new StringBuilder();
        while ((output = br.readLine()) != null) {
            builder.append(output);
        }

        assertTrue("should find emitted metrics", (Double) read(builder.toString(), "$.totalFound") >= 1);
        assertTrue(((Collection<String>) read(builder.toString(), "$.metrics[*].metric")).containsAll(Arrays.asList("NumOfExceptionsTest", "NumOfExceptionsTest")));
    }
}
