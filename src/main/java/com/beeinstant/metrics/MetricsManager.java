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

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * The entry point to start collecting metrics.
 * <p>
 * - First, initialize MetricsManager with a Service Name and hostInfo info.
 * - Second, get a MetricsLogger to start collecting metrics.
 * <p>
 * This class is thread-safe except method shutdown.
 */
public class MetricsManager {

    private static final Logger LOG = Logger.getLogger(MetricsManager.class);
    private static final String EMPTY_STRING = "";

    private static final DummyLogger dummyLogger = new DummyLogger();

    private static final BlockingDeque<String> metricsQueue = new LinkedBlockingDeque<>();

    private static final int flushInSeconds = Integer.valueOf(System.getProperty("beeinstant.flush.interval", "10"));
    private static final int flushStartDelayInSeconds = Integer.valueOf(System.getProperty("beeinstant.flush.startDelay", "5"));
    private static final boolean manualFlush = Boolean.valueOf(System.getProperty("beeinstant.flush.manual", "false"));
    private static final String publicKey = System.getProperty("beeinstant.publicKey", EMPTY_STRING);
    private static final String secretKey = System.getProperty("beeinstant.secretKey", EMPTY_STRING);
    private static final String endpoint = System.getProperty("beeinstant.endpoint", EMPTY_STRING);

    private static final String METRIC_ERRORS = "MetricErrors";
    private static CloseableHttpClient httpClient = null;
    private static MetricsLogger rootMetricsLogger = null;
    private static volatile MetricsManager instance = null;
    private static ScheduledExecutorService executorService = null;

    private static HttpHost beeInstantHost;
    private final String serviceName;
    private final String hostInfo;
    private final Map<String, MetricsLogger> metricsLoggers = new ConcurrentHashMap<>();

    private MetricsManager(final String serviceName, final String hostInfo) {
        this.serviceName = serviceName;
        this.hostInfo = hostInfo;
        beeInstantHost = createHostFromEndpoint(endpoint);
    }

    /**
     * Initialize MetricsManager by providing a service name and customized hostInfo, for example, an IP address of
     * the localhost.
     * <p>
     * Note: only the first init call wins. Subsequent init calls will be ignored.
     *
     * @param serviceName, used to identify your service
     * @param hostInfo,    customized hostInfo info, can be an IP address of the localhost
     */
    public static void init(final String serviceName, final String hostInfo) {
        if (!DimensionsUtils.isValidName(serviceName)) {
            throw new IllegalArgumentException("Invalid service name");
        }
        if (MetricsManager.instance == null) {
            synchronized (MetricsManager.class) {
                if (MetricsManager.instance == null) {
                    MetricsManager.instance = new MetricsManager(serviceName, hostInfo);
                    MetricsManager.rootMetricsLogger = MetricsManager.instance.metricsLoggers.computeIfAbsent("service=" + serviceName, MetricsLogger::new);
                    MetricsManager.httpClient = HttpClients.custom()
                            .setConnectionManager(new PoolingHttpClientConnectionManager(Integer.MAX_VALUE, TimeUnit.DAYS)) //no more than 2 concurrent connections per given route
                            .setKeepAliveStrategy((response, context) -> 60000)
                            .setRetryHandler(new DefaultHttpRequestRetryHandler()) // 3 times retry by default
                            .build();
                    if (!manualFlush) {
                        try {
                            executorService = Executors.newScheduledThreadPool(1);
                            executorService.scheduleAtFixedRate(() -> {
                                try {
                                    flushAll();
                                } catch (Throwable e) {
                                    // Don't stop the thread
                                    LOG.error(e);
                                }
                            }, flushStartDelayInSeconds, flushInSeconds, SECONDS);
                        } catch (Throwable e) {
                            LOG.error("Cannot submit metrics", e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Shutdown MetricsManager, clean up resources
     * This method is not thread-safe, only use it to clean up resources when your application is shutting down.
     */
    public static void shutdown() {
        if (MetricsManager.executorService != null) {
            MetricsManager.executorService.shutdown();
            MetricsManager.executorService = null;
        }
        if (MetricsManager.instance != null) {
            MetricsManager.instance = null;
            MetricsManager.httpClient = null;
            MetricsManager.rootMetricsLogger = null;
        }
    }

    /**
     * Initialize MetricsManager by providing a service name and localhost info. Localhost info by default will be
     * the hostname of the localhost.
     * <p>
     * Note: only the first init call wins. Subsequent init calls will be ignored.
     *
     * @param serviceName, a name to identify your service
     */
    public static void init(final String serviceName) {
        try {
            MetricsManager.init(serviceName, InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            throw new RuntimeException("Cannot get hostname of the localhost");
        }
    }

    /**
     * Get MetricsLogger to start collecting metrics. MetricsLogger can be used to collect Counter, Timer or Recorder
     *
     * @param dimensions, key-value pairs aka dimensions for example "api=Upload, region=DUB"
     * @return metrics logger
     */
    public static MetricsLogger getMetricsLogger(final String dimensions) {
        if (MetricsManager.instance != null) {
            final Map<String, String> dimensionsMap = DimensionsUtils.parseDimensions(dimensions);
            if (!dimensionsMap.isEmpty()) {
                dimensionsMap.put("service", MetricsManager.instance.serviceName);
                return MetricsManager.instance.metricsLoggers.computeIfAbsent(
                        DimensionsUtils.serializeDimensionsToString(dimensionsMap), key -> new MetricsLogger(dimensionsMap));
            } else {
                throw new IllegalArgumentException("Dimensions must be valid and non-empty");
            }
        }
        return dummyLogger;
    }

    /**
     * Get Root metrics logger which manages the root dimensions "service=<YourServiceName>"
     *
     * @return metrics logger for the root dimensions
     */
    public static MetricsLogger getRootMetricsLogger() {
        if (MetricsManager.instance != null) {
            return MetricsManager.rootMetricsLogger;
        }
        return dummyLogger;
    }

    /**
     * Flush all metrics which have been collected so far by all MetricsLoggers. Metrics can also be flushed by
     * each MetricsLogger individually.
     */
    public static void flushAll() {
        if (MetricsManager.instance != null) {
            MetricsManager.instance.metricsLoggers.values().forEach(MetricsManager::flushMetricsLogger);
            flushToServer();
        }
    }

    /**
     * Flush metrics to BeeInstant Server
     */
    static void flushToServer() {
        LOG.debug("Flush to BeeInstant Server");
        Collection<String> readyToSubmit = new ArrayList<>();
        metricsQueue.drainTo(readyToSubmit);
        StringBuilder builder = new StringBuilder();
        readyToSubmit.forEach(string -> {
            builder.append(string);
            builder.append("\n");
        });
        if (!readyToSubmit.isEmpty() && beeInstantHost != null) {
            try {
                StringEntity entity = new StringEntity("{\"metrics\":\"" + builder.toString() + "\"}");
                entity.setContentType("application/json");

                String uri = "/PutMetric";
                final String signature = sign(entity);
                if (!signature.isEmpty()) {
                    uri += "?signature=" + URLEncoder.encode(signature, "UTF-8");
                    uri += "&publicKey=" + URLEncoder.encode(publicKey, "UTF-8");
                }

                HttpPost putMetricCommand = new HttpPost(uri);
                putMetricCommand.setEntity(entity);
                HttpResponse response = httpClient.execute(beeInstantHost, putMetricCommand);
                LOG.info("Response: " + response.getStatusLine().getStatusCode());

            } catch (Throwable e) {
                LOG.error("Fail to emit metrics", e);
            }
        }
    }

    /**
     * Get localhost, this value can be a hostname or a customized value for example an IP address
     *
     * @return localhost
     */
    public static String getHostInfo() {
        if (MetricsManager.instance != null) {
            return MetricsManager.instance.hostInfo;
        }
        return EMPTY_STRING;
    }

    /**
     * Get Service name which is used to initialize MetricsManager
     *
     * @return service name
     */
    public static String getServiceName() {
        if (MetricsManager.instance != null) {
            return MetricsManager.instance.serviceName;
        }
        return EMPTY_STRING;
    }

    /**
     * Report errors during metric data collecting process. Report in two forms, a host level metric which counts
     * number of errors and a log line with message for each error. Will be used by MetricsLogger to report errors.
     *
     * @param errorMessage, error message during metric data collecting process
     */
    static void reportError(final String errorMessage) {
        if (MetricsManager.instance != null) {
            MetricsManager.rootMetricsLogger.incCounter(METRIC_ERRORS, 1);
        }
        LOG.error(errorMessage);
    }

    /**
     * Flush metrics collected by MetricsLogger to log files. Will be used by MetricsLogger to flush itself.
     *
     * @param metricsLogger, contain metric dimensions, metric names, metric data (counter, timer, recorder)
     */
    static void flushMetricsLogger(final MetricsLogger metricsLogger) {
        metricsLogger.flushToString(MetricsManager::queue);
        MetricsManager.rootMetricsLogger.flushToString(MetricsManager::queue);
    }

    private static void queue(String metricString) {
        metricsQueue.add(metricString);
    }

    private static String sign(StringEntity entity) throws IOException {
        if (!publicKey.isEmpty() && !secretKey.isEmpty()) {
            InputStream content = entity.getContent();
            byte[] contentBytes = new byte[content.available()];
            content.read(contentBytes);
            content.close();
            try {
                return new String(Signature.sign(contentBytes, secretKey));
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                LOG.error(e.getMessage());
            }
        }
        return EMPTY_STRING;
    }

    private static HttpHost createHostFromEndpoint(String endpoint) {
        HttpHost host = null;
        try {
            final URL url = new URL(endpoint);
            host = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
        } catch (MalformedURLException e) {
            LOG.error(e.getMessage());
        }
        return host;
    }
}
