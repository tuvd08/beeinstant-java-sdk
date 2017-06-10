# beeinstant-java-sdk

## Usage

### Initialization
Initialized MetricsManager via static block to ensure all metrics can be emitted approriately

```java
static {
 MetricsManager.init("Your service name");
}
```

### Extends metrics by adding your own defined measurement
```
 final MetricsLogger metricsLogger = MetricsManager.getMetricsLogger(dimensions);
 metricsLogger.incCounter("NumOfExceptions", 1);
```

### Check out our unit test for further advance use cases

