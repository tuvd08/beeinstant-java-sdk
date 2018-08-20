# BeeInstant Android SDK

BeeInstant Android SDK is a powerful tool for software/dev-ops engineers to define and publish custom metrics to BeeInstant. By using clean and simple APIs, engineers can track their software performance with counters, timers, and recorders in real-time.

The SDK provides multi-dimensional metrics that allow engineers to tailor make their very own metrics that suits unique cases. The metrics are aggregated at global levels across hosts, times and dimensions. For example, engineers can easily visualize percentile 99.99% of their API latencies at service level by aggregating metrics published from all of their hosts.

Example: Measure execution time of a critical process.
```
// Initialize the sdk once at the beginning
MetricsManager.init("MyCriticalService");

// Get a metric logger inspired from traditional logger
final Metrics metricsLogger = MetricsManager.getRootMetricsLogger();

// Measure execution time with metric name "ExecutionTime"
try (TimerMetric timer = metricsLogger.startTimer("ExecutionTime")) {
    //
    // Some critical processing is happening here...
    //
}

// Clean up
MetricsManager.shutdown();
Installation
```

## Open BeeInstant Account

Open a BeeInstant account on https://beeinstant.com. After your account is activated, you can log into https://app.beeinstant.com to get endpoint and credentials required by the SDK to publish metrics. Launch your service with JVM options:

```
-Dbeeinstant.endpoint=<endpoint> -Dbeeinstant.publicKey=<public_key> -Dbeeinstant.secretKey=<secret_key>
Add SDK as module
```
Import this module into your android projects

### Initialization

The first step, we need to initialize the SDK before using it.

```
MetricsManager.init("VideoSharing");
	System.setProperty("beeinstant.endpoint", "https://{your endpoint}");
	System.setProperty("beeinstant.publicKey", "{your publicKey}");
	System.setProperty("beeinstant.secretKey", "{your secretKey}");
	System.setProperty("beeinstant.flush.interval", "30");
	MetricsManager.init("{Your service name}", "", "localhost");
```
Later, when your service is shutting down, you can also shutdown the SDK.

```
MetricsManager.shutdown();
```

Let say our VideoSharing service provides an Upload API for users to upload their videos. Let's monitor how long it takes to process a video. We call the metric ProcessingTime. We will use TimerMetric to capture it.

Create a metric logger dedicated for Upload API.

final MetricsLogger metricsLogger = MetricsManager.getMetricsLogger("api=Upload");
A metric logger is inspired from traditional loggers for example Log4J. But instead of logging text, a metric logger publishes timing metrics, counters or arbitrary metrics in real-time.

### Measuring time

Let's capture ProcessingTime of Upload API using TimerMetric.
```
void handleVideoUpload() {
    // ...
    try (TimerMetric timer = metricsLogger.startTimer("ProcessingTime")) {
        // ...
        // Read video
        // Compress video
        // Store video
        // ...
    }
    // ...
}
```

ProcessingTime's unit is milliseconds.

### Counting

During video processing, we will encounter some cases when we cannot read, compress or store video for various reasons. How can we know this thing happens in real-time? BeeInstant provides counter to handle this case.

```
void handleVideoUpload() {
    // ...
    try (TimerMetric timer = metricsLogger.startTimer("ProcessingTime")) {
        
        // Read video
        try {
            // ...
            // Reading video
            // ...
            metricsLogger.incCounter("ReadSuccess", 1);
            
        } catch (Exception e) {
            metricsLogger.incCounter("ReadFailure", 1);
        }
        
        // Compress video
        // Store video
        // ...
    }
    // ...
}
```

### Recording

As a VideoSharing service, we are always interested in how big are videos uploaded by users. Size of a video is not timing or counter metric. In this case, BeeInstant provides recorder, a tool to record arbitrary metrics. Let's use a recorder to capture the number of bytes read for each video we process.

```
void handleVideoUpload() {
    // ...
    try (TimerMetric timer = metricsLogger.startTimer("ProcessingTime")) {
    
        // Read video
        try {
            long numOfReadBytes = 0;
            // Reading ...
            // numOfReadBytes += 100
            // Reading ...
            // numOfReadBytes += 200
            // ...
            metricsLogger.record("ReadBytes", numOfReadBytes, Unit.BYTE);
            
            metricsLogger.incCounter("ReadSuccess", 1);
    
        } catch (Exception e) {
            metricsLogger.incCounter("ReadFailure", 1);
        }
    
        // Compress video
        // Store video
        // ...
    }
    // ...
}
```

### Advanced dimension manipulations

#### Drill down

We have built a pretty good set of metrics around our Upload API. But can we drill down even further? Yes, BeeInstant provides dimension extension feature that allows us to monitor even further details of Upload API. Imagine Upload API can be broken down into three steps read, compress and store videos. Let's monitor down to that level.
```
void handleVideoUpload() {

    // ...
    try (TimerMetric timer = metricsLogger.startTimer("ProcessingTime")) {

        final Metrics readMetrics = metricsLogger.extendDimensions("step=Read");
        try (TimerMetric readTimer = readMetrics.startTimer("Time")) {
            // Reading video here...
        }

        final Metrics compressMetrics = metricsLogger.extendDimensions("step=Compress");
        try (TimerMetric compressTimer = compressMetrics.startTimer("Time")) {
            // Compressing video here...
        }

        final Metrics storeMetrics = metricsLogger.extendDimensions("step=Store");
        try (TimerMetric storeTimer = storeMetrics.startTimer("Time")) {
            // Storing video here...
        }
    }
    // ...
}
```

#### Aggregate up

We are passionate about building high-quality VideoSharing service which will be always available to our customers. And so we are building an AutoTester for our service. This AutoTester will continuously send requests to Upload API and measure its availability. We do this for both testing and prod stacks.

Let say we monitor availabilities of both APIs Upload and Download.

Test Upload API
```
final MetricsLogger uploadMetricsLogger = MetricsManager.getMetricsLogger("api=Upload");
bool success = AutoTester.upload("sample-video");
uploadMetricsLogger.record("Availability", success ? 1 : 0, Unit.NONE);
```

Test Download API
```
final MetricsLogger downloadMetricsLogger = MetricsManager.getMetricsLogger("api=Download");
bool success = AutoTester.download("sample-video");
downloadMetricsLogger.record("Availability", success ? 1 : 0, Unit.NONE);
```

We have availability metric for each API. So what is the global availability of the whole VideoSharing service? BeeInstant provides multiple dimension extension feature to answer this question. Whenever we record availability for Upload API or Download API, we contribute that availability to service level by setting api=ALL. Here is how.

Test Upload API
```
final Metrics metrics = metricsLogger.extendMultipleDimensions("api=Upload", "api=ALL");
bool success = AutoTester.upload("sample-video");
metrics.record("Availability", success ? 1 : 0, Unit.NONE);
```

Test Download API
```
final Metrics metrics = metricsLogger.extendMultipleDimensions("api=Download", "api=ALL");
bool success = AutoTester.download("sample-video");
metrics.record("Availability", success ? 1 : 0, Unit.NONE);
```

Using combinations of timer, counter, recorder and advanced dimension manipulations, in no time, we have access to the most insightful visibilities of our own service. With these metrics, we can build a handy set of dashboards to look into every corner of our service. BeeInstant will also provide an intelligent alarming system (powered by machine learning) with auto-recovery actions for dev-ops engineers in near future.

Head to https://app.beeinstant.com/graph to see all metrics.
