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

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

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
    private static PoolingHttpClientConnectionManager poolManager = null;
    private static MetricsLogger rootMetricsLogger = null;
    private static volatile MetricsManager instance = null;
    private static ScheduledExecutorService executorService = null;

    private static HttpHost beeInstantHost;
    private final String serviceName;
    private final String env;
    private final String hostInfo;
    private final Map<String, MetricsLogger> metricsLoggers = new ConcurrentHashMap<String, MetricsLogger>();

    private MetricsManager(final String serviceName, final String env, final String hostInfo) {
        this.serviceName = serviceName;
        this.env = env.trim();
        this.hostInfo = hostInfo;
        beeInstantHost = createHostFromEndpoint(endpoint);
    }

    /**
     * Initialize MetricsManager by providing a service name, environment, customized hostInfo,
     * for example, an IP address of the localhost.
     * <p>
     * Note: only the first init call wins. Subsequent init calls will be ignored.
     *
     * @param serviceName, used to identify your service
     * @param env,         the environment the service is running in like Development, Production, etc.
     * @param hostInfo,    customized hostInfo info, can be an IP address of the localhost
     */
    public static void init(final String serviceName, final String env, final String hostInfo) {
        if (!DimensionsUtils.isValidName(serviceName)) {
            throw new IllegalArgumentException("Invalid service name");
        }
        if (MetricsManager.instance == null) {
            synchronized (MetricsManager.class) {
                if (MetricsManager.instance == null) {
                    MetricsManager.instance = new MetricsManager(serviceName, env, hostInfo);
                    String envDimension = EMPTY_STRING;
                    if (env.trim().length() > 0) {
                        envDimension = ",env=" + env.trim();
                    }
                    
//                    MetricsManager.rootMetricsLogger = MetricsManager.instance.metricsLoggers
//                            .computeIfAbsent("service=" + serviceName + envDimension, MetricsLogger::new);
                    String infoEnvDimension = "service=" + serviceName + envDimension;
                    MetricsLogger rootMetricsLogger = MetricsManager.instance.metricsLoggers.get(infoEnvDimension);
                    if(rootMetricsLogger == null) {
                        rootMetricsLogger = new MetricsLogger(infoEnvDimension);
                        MetricsManager.instance.metricsLoggers.put(infoEnvDimension, rootMetricsLogger);
                    }
                    MetricsManager.rootMetricsLogger = rootMetricsLogger;
                    
                    MetricsManager.poolManager = new PoolingHttpClientConnectionManager(Integer.MAX_VALUE, TimeUnit.DAYS); //no more than 2 concurrent connections per given route
                    MetricsManager.httpClient = HttpClients.custom()
                            .setConnectionManager(poolManager)
                            .setKeepAliveStrategy(myStrategy)
                            .setRetryHandler(new DefaultHttpRequestRetryHandler()) // 3 times retry by default
                            .build();
                    
                    if (!manualFlush) {
                        try {
                            executorService = Executors.newScheduledThreadPool(1);
                            executorService.scheduleAtFixedRate(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        flushAll(System.currentTimeMillis() / 1000);
                                    } catch (Throwable e) {
                                        // Don't stop the thread
                                        LOG.error(e);
                                    }
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
    
    private static ConnectionKeepAliveStrategy myStrategy = new ConnectionKeepAliveStrategy() {
        @Override
        public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
            HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();
                if (value != null && param.equalsIgnoreCase("timeout")) {
                    return Long.parseLong(value) * 1000;
                }
            }
            return 60 * 1000;
        }
    };

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
            MetricsManager.poolManager.shutdown();
            MetricsManager.poolManager = null;
            MetricsManager.httpClient = null;
            MetricsManager.rootMetricsLogger = null;
        }
    }

    /**
     * Initialize MetricsManager by providing a service name, environment, customized hostInfo,
     * for example, an IP address of the localhost.
     * <p>
     * Note: only the first init call wins. Subsequent init calls will be ignored.
     *
     * @param serviceName, a name to identify your service
     * @param env,         the environment the service is running in like Development, Production, etc.
     */
    public static void init(final String serviceName, final String env) {
        try {
            MetricsManager.init(serviceName, env, InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            throw new RuntimeException("Cannot get hostname of the localhost");
        }
    }

    /**
     * Initialize MetricsManager by providing a service name, environment, customized hostInfo,
     * for example, an IP address of the localhost.
     * <p>
     * Note: only the first init call wins. Subsequent init calls will be ignored.
     *
     * @param serviceName, a name to identify your service
     */
    public static void init(final String serviceName) {
        MetricsManager.init(serviceName, EMPTY_STRING);
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
                if (MetricsManager.instance.env.length() > 0) {
                    dimensionsMap.put("env", MetricsManager.instance.env);
                }
//                return MetricsManager.instance.metricsLoggers.computeIfAbsent(
//                        DimensionsUtils.serializeDimensionsToString(dimensionsMap), key -> new MetricsLogger(dimensionsMap));
                String dimentions = DimensionsUtils.serializeDimensionsToString(dimensionsMap);
                MetricsLogger metricsLogger = MetricsManager.instance.metricsLoggers.get(dimentions);
                if (metricsLogger == null) {
                    metricsLogger = new MetricsLogger(dimensionsMap);
                    MetricsManager.instance.metricsLoggers.put(dimentions, metricsLogger);
                }
                System.out.println("getMetricsLogger " + metricsLogger.getRootDimensions());
                return metricsLogger;
            } else {
                throw new IllegalArgumentException("Dimensions must be valid and non-empty");
            }
        }
        return dummyLogger;
    }

    /**
     * Get Root metrics logger which manages the root dimensions "service=YourServiceName"
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
    public static void flushAll(long now) {
        if (MetricsManager.instance != null) {
//            MetricsManager.instance.metricsLoggers.values().forEach(MetricsManager::flushMetricsLogger);
            for (MetricsLogger metricsLogger : MetricsManager.instance.metricsLoggers.values()) {
                flushMetricsLogger(metricsLogger);
            }
            flushToServer(now);
        }
    }

    /**
     * Flush metrics to BeeInstant Server
     */
    static void flushToServer(long now) {
        LOG.debug("Flush to BeeInstant Server");
        Collection<String> readyToSubmit = new ArrayList<>();
        metricsQueue.drainTo(readyToSubmit);
        StringBuilder builder = new StringBuilder();
//        readyToSubmit.forEach(string -> {
//            builder.append(string);
//            builder.append("\n");
//        });
        for (String metricsString : readyToSubmit) {
            if (!metricsString.isEmpty()) {
                builder.append(metricsString);
                builder.append("\n");
            }
        }
        if (!readyToSubmit.isEmpty() && beeInstantHost != null) {
            try {
                final String body = builder.toString();
                StringEntity entity = new StringEntity(body);
                entity.setContentType("text/plain");

                String uri = "/PutMetric";
                final String signature = sign(entity);
                if (!signature.isEmpty()) {
                    uri += "?signature=" + URLEncoder.encode(signature, "UTF-8");
                    uri += "&publicKey=" + URLEncoder.encode(publicKey, "UTF-8");
                    uri += "&timestamp=" + now;

                    HttpPost putMetricCommand = new HttpPost(uri);
                    try {
                        putMetricCommand.setEntity(entity);
                        HttpResponse response = httpClient.execute(beeInstantHost, putMetricCommand);
                        LOG.info("Response: " + response.getStatusLine().getStatusCode());
                    } finally {
                        putMetricCommand.releaseConnection();
                    }
                }

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
     * Get environment which is used to initialize MetricsManager
     *
     * @return environment
     */
    public static String getEnvironment() {
        if (MetricsManager.instance != null) {
            return MetricsManager.instance.env;
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
        if (MetricsManager.instance != null && MetricsManager.rootMetricsLogger != null) {
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
//        metricsLogger.flushToString(MetricsManager::queue);
//        MetricsManager.rootMetricsLogger.flushToString(MetricsManager::queue);
        flushMetricsLoggers(metricsLogger.flushToString());
        flushMetricsLoggers(MetricsManager.rootMetricsLogger.flushToString());
    }

    private static void flushMetricsLoggers(final Map<String, MetricsCollector> mMetricsCollector) {
        for (String dimensions : mMetricsCollector.keySet()) {
            final String metricsString = mMetricsCollector.get(dimensions).flushToString();
            if (!metricsString.isEmpty()) {
                MetricsManager.queue(dimensions + "," + metricsString);
            }
        }
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
