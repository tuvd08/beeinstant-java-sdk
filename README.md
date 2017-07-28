# beeinstant-java-sdk

## Maven
```
<dependency>
  <groupId>com.beeinstant</groupId>
  <artifactId>metrics</artifactId>
  <version>1.1.1</version>
</dependency>
```
## Gradle
```
dependencies {
    compile 'com.beeinstant:metrics:1.1.1'
}
```
## SBT
```
libraryDependencies += "com.beeinstant" % "metrics" % "1.1.1"
```
## Usage

#### Initialization
Initialized MetricsManager via static block to ensure all metrics can be emitted approriately

Java
```java
static {
 MetricsManager.init("Your service name");
}
```

### Extends metrics by adding your own defined measurement
Java
```
 final MetricsLogger metricsLogger = MetricsManager.getMetricsLogger(dimensions);
 metricsLogger.incCounter("NumOfExceptions", 1);
```

### Check out our unit test for further advanced use cases

