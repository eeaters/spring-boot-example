# springboot中log初始化流程

## 结论

1. 非自定义情况下, 系统优先拿logback.xml; 然后再拿logback-spring.xml @see AbstractLoggingSystem 
2. 默认情况下; 日志走的是编程式配置: @see DefaultLogbackConfiguration
3. log支持的配置key包含在: @see LoggingSystemProperty 
4. 除了logging.level是yml/properties中优先; 其他大多数是xml优先(这个没有具体分析, 只是结论)

## 涉及jar包

```xml
	<dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>
```

## 入口

spring-boot > spring.factories

```
# Logging Systems
org.springframework.boot.logging.LoggingSystemFactory=\
org.springframework.boot.logging.logback.LogbackLoggingSystem.Factory,\
org.springframework.boot.logging.log4j2.Log4J2LoggingSystem.Factory,\
org.springframework.boot.logging.java.JavaLoggingSystem.Factory

# Application Listeners
org.springframework.context.ApplicationListener=\
org.springframework.boot.ClearCachesApplicationListener,\
org.springframework.boot.builder.ParentContextCloserApplicationListener,\
org.springframework.boot.context.FileEncodingApplicationListener,\
org.springframework.boot.context.config.AnsiOutputApplicationListener,\
org.springframework.boot.context.config.DelegatingApplicationListener,\
org.springframework.boot.context.logging.LoggingApplicationListener,\
org.springframework.boot.env.EnvironmentPostProcessorApplicationListener
```

springboot容器启动的时候, 会在不同的阶段发送Event事件. 在LoggingApplicationListener中代码如下: 

```java
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationStartingEvent startingEvent) {
			onApplicationStartingEvent(startingEvent);
		}
		else if (event instanceof ApplicationEnvironmentPreparedEvent environmentPreparedEvent) {
			onApplicationEnvironmentPreparedEvent(environmentPreparedEvent);
		}
		else if (event instanceof ApplicationPreparedEvent preparedEvent) {
			onApplicationPreparedEvent(preparedEvent);
		}
		else if (event instanceof ContextClosedEvent contextClosedEvent) {
			onContextClosedEvent(contextClosedEvent);
		}
		else if (event instanceof ApplicationFailedEvent) {
			onApplicationFailedEvent();
		}
	}
```

### ApplicationStartingEvent > 指定日志框架

这里拿到具体使用哪一种框架作为应用的日志框架, 默认: LogbackLoggingSystem

```java
	private void onApplicationStartingEvent(ApplicationStartingEvent event) {
		this.loggingSystem = LoggingSystem.get(event.getSpringApplication().getClassLoader());
		this.loggingSystem.beforeInitialize();
	}
```

```java
	public static LoggingSystem get(ClassLoader classLoader) {
		String loggingSystemClassName = System.getProperty(SYSTEM_PROPERTY);
		if (StringUtils.hasLength(loggingSystemClassName)) {
			if (NONE.equals(loggingSystemClassName)) {
				return new NoOpLoggingSystem();
			}
			return get(classLoader, loggingSystemClassName);
		}
        // 以工厂+代理,获取spi中第一个返回的日志看框架: 第一个是logback
		LoggingSystem loggingSystem = SYSTEM_FACTORY.getLoggingSystem(classLoader);
		Assert.state(loggingSystem != null, "No suitable logging system located");
		return loggingSystem;
	}
```

### ApplicationEnvironmentPreparedEvent > 日志初始化

```java
	private void onApplicationEnvironmentPreparedEvent(ApplicationEnvironmentPreparedEvent event) {
		SpringApplication springApplication = event.getSpringApplication();
		if (this.loggingSystem == null) {
			this.loggingSystem = LoggingSystem.get(springApplication.getClassLoader());
		}
		initialize(event.getEnvironment(), springApplication.getClassLoader());
	}
```

在initialize中有 initializeSystem

```java
	public static final String CONFIG_PROPERTY = "logging.config";

	private void initializeSystem(ConfigurableEnvironment environment, LoggingSystem system, LogFile logFile) {
		String logConfig = environment.getProperty(CONFIG_PROPERTY);
		if (StringUtils.hasLength(logConfig)) {
			logConfig = logConfig.strip();
		}
			LoggingInitializationContext initializationContext = new LoggingInitializationContext(environment);
			if (ignoreLogConfig(logConfig)) {
				system.initialize(initializationContext, null, logFile);
			}
			else {
				system.initialize(initializationContext, logConfig, logFile);
			}
		}
	}
```



```java
	@Override
	public void initialize(LoggingInitializationContext initializationContext, String configLocation, LogFile logFile) {
		LoggerContext loggerContext = getLoggerContext();
		if (isAlreadyInitialized(loggerContext)) {
			return;
		}
		if (!initializeFromAotGeneratedArtifactsIfPossible(initializationContext, logFile)) {
			super.initialize(initializationContext, configLocation, logFile);
		}
	}
```

### AbstractLoggingSystem - 核心脉络

```java

	private void initializeWithConventions(LoggingInitializationContext initializationContext, LogFile logFile) {
		String config = getSelfInitializationConfig();
		if (config != null && logFile == null) {
			// self initialization has occurred, reinitialize in case of property changes
			reinitialize(initializationContext);
			return;
		}
		if (config == null) {
			config = getSpringInitializationConfig();
		}
		if (config != null) {
			loadConfiguration(initializationContext, config, logFile);
			return;
		}
		loadDefaults(initializationContext, logFile);
	}
```

#### reinitialize - 默认localtion

```java
	protected String getSelfInitializationConfig() {
		return findConfig(getStandardConfigLocations());
	}
	
	// logback的实现
	@Override
	protected String[] getStandardConfigLocations() {
		return new String[] { "logback-test.groovy", "logback-test.xml", "logback.groovy", "logback.xml" };
	}

```

#### getSpringInitializationConfig - spring环境配置location

```java
	//如logback是标准位置;没有找到走spring提供的配置路径
	protected String[] getSpringConfigLocations() {
		String[] locations = getStandardConfigLocations();
		for (int i = 0; i < locations.length; i++) {
			String extension = StringUtils.getFilenameExtension(locations[i]);
			locations[i] = locations[i].substring(0, locations[i].length() - extension.length() - 1) + "-spring."
					+ extension;
		}
		return locations;
	}

```

#### loadConfiguration - 将文件进行配置

```java
	@Override
	protected void loadConfiguration(LoggingInitializationContext initializationContext, String location,
			LogFile logFile) {
		LoggerContext loggerContext = getLoggerContext();
		stopAndReset(loggerContext);
		withLoggingSuppressed(() -> {
			if (initializationContext != null) {
				applySystemProperties(initializationContext.getEnvironment(), logFile);
			}
			try {
				Resource resource = new ApplicationResourceLoader().getResource(location);
				configureByResourceUrl(initializationContext, loggerContext, resource.getURL());
			}
			catch (Exception ex) {
				throw new IllegalStateException("Could not initialize Logback logging from " + location, ex);
			}
			loggerContext.start();
		});
		reportConfigurationErrorsIfNecessary(loggerContext);
	}

```

#### loadDefaults - java编程式日志配置

```java
	@Override
	protected void loadDefaults(LoggingInitializationContext initializationContext, LogFile logFile) {
		LoggerContext context = getLoggerContext();
		stopAndReset(context);
		withLoggingSuppressed(() -> {
			boolean debug = Boolean.getBoolean("logback.debug");
			if (debug) {
				StatusListenerConfigHelper.addOnConsoleListenerInstance(context, new OnConsoleStatusListener());
			}
			Environment environment = initializationContext.getEnvironment();
			// Apply system properties directly in case the same JVM runs multiple apps
			new LogbackLoggingSystemProperties(environment, getDefaultValueResolver(environment), context::putProperty)
				.apply(logFile);
			LogbackConfigurator configurator = debug ? new DebugLogbackConfigurator(context)
					: new LogbackConfigurator(context);
			new DefaultLogbackConfiguration(logFile).apply(configurator);
			context.setPackagingDataEnabled(true);
			context.start();
		});
	}
```

java编程配置类:

```java

/**
 * Default logback configuration used by Spring Boot. Uses {@link LogbackConfigurator} to
 * improve startup time. See also the {@code base.xml}, {@code defaults.xml},
 * {@code console-appender.xml} and {@code file-appender.xml} files provided for classic
 * {@code logback.xml} use.
 */
class DefaultLogbackConfiguration {
    
    
	void apply(LogbackConfigurator config) {
		config.getConfigurationLock().lock();
		try {
			defaults(config);
			Appender<ILoggingEvent> consoleAppender = consoleAppender(config);
			if (this.logFile != null) {
				Appender<ILoggingEvent> fileAppender = fileAppender(config, this.logFile.toString());
				config.root(Level.INFO, consoleAppender, fileAppender);
			}
			else {
				config.root(Level.INFO, consoleAppender);
			}
		}
		finally {
			config.getConfigurationLock().unlock();
		}
	}

	private void defaults(LogbackConfigurator config) {
		config.conversionRule("applicationName", ApplicationNameConverter.class);
		config.conversionRule("clr", ColorConverter.class);
		config.conversionRule("correlationId", CorrelationIdConverter.class);
		config.conversionRule("wex", WhitespaceThrowableProxyConverter.class);
		config.conversionRule("wEx", ExtendedWhitespaceThrowableProxyConverter.class);
		config.getContext()
			.putProperty("CONSOLE_LOG_PATTERN", resolve(config, "${CONSOLE_LOG_PATTERN:-"
					+ "%clr(%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd'T'HH:mm:ss.SSSXXX}}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) "
					+ "%clr(${PID:- }){magenta} %clr(---){faint} %clr(%applicationName[%15.15t]){faint} "
					+ "%clr(${LOG_CORRELATION_PATTERN:-}){faint}%clr(%-40.40logger{39}){cyan} "
					+ "%clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}}"));
		String defaultCharset = Charset.defaultCharset().name();
		config.getContext()
			.putProperty("CONSOLE_LOG_CHARSET", resolve(config, "${CONSOLE_LOG_CHARSET:-" + defaultCharset + "}"));
		config.getContext().putProperty("CONSOLE_LOG_THRESHOLD", resolve(config, "${CONSOLE_LOG_THRESHOLD:-TRACE}"));
		config.getContext()
			.putProperty("FILE_LOG_PATTERN", resolve(config, "${FILE_LOG_PATTERN:-"
					+ "%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd'T'HH:mm:ss.SSSXXX}} ${LOG_LEVEL_PATTERN:-%5p} ${PID:- } --- %applicationName[%t] "
					+ "${LOG_CORRELATION_PATTERN:-}"
					+ "%-40.40logger{39} : %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}}"));
		config.getContext()
			.putProperty("FILE_LOG_CHARSET", resolve(config, "${FILE_LOG_CHARSET:-" + defaultCharset + "}"));
		config.getContext().putProperty("FILE_LOG_THRESHOLD", resolve(config, "${FILE_LOG_THRESHOLD:-TRACE}"));
		config.logger("org.apache.catalina.startup.DigesterFactory", Level.ERROR);
		config.logger("org.apache.catalina.util.LifecycleBase", Level.ERROR);
		config.logger("org.apache.coyote.http11.Http11NioProtocol", Level.WARN);
		config.logger("org.apache.sshd.common.util.SecurityUtils", Level.WARN);
		config.logger("org.apache.tomcat.util.net.NioSelectorPool", Level.WARN);
		config.logger("org.eclipse.jetty.util.component.AbstractLifeCycle", Level.ERROR);
		config.logger("org.hibernate.validator.internal.util.Version", Level.WARN);
		config.logger("org.springframework.boot.actuate.endpoint.jmx", Level.WARN);
	}
}
```

以编程式的方式设置日志格式; 同时如果在同一个包下面分别有 `base.xml`, `console-appender.xml`,`defaults.xml`,`file-appender.xml`. 提供了xml式的配置参考; 并且支持引入

### Logback在Spring中支持那些配置

脉络中针对日志加载的顺序进行梳理; 但是log的一些property是如何配置的再找一下

#### LoggingApplicationListener 

```java
protected void initialize(ConfigurableEnvironment environment, ClassLoader classLoader) {		
		this.logFile = LogFile.get(environment);
   	 	if (this.logFile != null) {
            // 这里可以找到配置的key
        	this.logFile.applyToSystemProperties();
    	}
    }
```

#### `LogbackLoggingSystem`

```java
# 这里构建log的配置类	
new LogbackLoggingSystemProperties(environment, getDefaultValueResolver(environment), context::putProperty)
				.apply(logFile);
			LogbackConfigurator configurator = debug ? new DebugLogbackConfigurator(context)
					: new LogbackConfigurator(context);
			new DefaultLogbackConfiguration(logFile).apply(configurator);
```

#### LogbackLoggingSystemProperties

```java

	public final void apply(LogFile logFile) {
		PropertyResolver resolver = getPropertyResolver();
		apply(logFile, resolver);
	}
	protected void apply(LogFile logFile, PropertyResolver resolver) {
		String defaultCharsetName = getDefaultCharset().name();
		setApplicationNameSystemProperty(resolver);
		setSystemProperty(LoggingSystemProperty.PID, new ApplicationPid().toString());
		setSystemProperty(LoggingSystemProperty.CONSOLE_CHARSET, resolver, defaultCharsetName);
		setSystemProperty(LoggingSystemProperty.FILE_CHARSET, resolver, defaultCharsetName);
		setSystemProperty(LoggingSystemProperty.CONSOLE_THRESHOLD, resolver, this::thresholdMapper);
		setSystemProperty(LoggingSystemProperty.FILE_THRESHOLD, resolver, this::thresholdMapper);
		setSystemProperty(LoggingSystemProperty.EXCEPTION_CONVERSION_WORD, resolver);
		setSystemProperty(LoggingSystemProperty.CONSOLE_PATTERN, resolver);
		setSystemProperty(LoggingSystemProperty.FILE_PATTERN, resolver);
		setSystemProperty(LoggingSystemProperty.LEVEL_PATTERN, resolver);
		setSystemProperty(LoggingSystemProperty.DATEFORMAT_PATTERN, resolver);
		setSystemProperty(LoggingSystemProperty.CORRELATION_PATTERN, resolver);
		if (logFile != null) {
			logFile.applyToSystemProperties();
		}
	}
```

#### LoggingSystemProperty

```java
//这就是支持配置化的定义代码
public enum LoggingSystemProperty {

	CONSOLE_CHARSET("CONSOLE_LOG_CHARSET", "logging.charset.console"),

	FILE_CHARSET("FILE_LOG_CHARSET", "logging.charset.file"),

	CONSOLE_THRESHOLD("CONSOLE_LOG_THRESHOLD", "logging.threshold.console"),

	FILE_THRESHOLD("FILE_LOG_THRESHOLD", "logging.threshold.file"),

	EXCEPTION_CONVERSION_WORD("LOG_EXCEPTION_CONVERSION_WORD", "logging.exception-conversion-word"),

	CONSOLE_PATTERN("CONSOLE_LOG_PATTERN", "logging.pattern.console"),

	FILE_PATTERN("FILE_LOG_PATTERN", "logging.pattern.file"),
    //...
}
```

### 修改方式

#### logback.xml

自定义一个logback.xml或者logback-spring.xml; 这是大多数项目都统一的做法.公司会根据链路追踪的方式自定义格式. 忽略

#### 修改application.yml

临时项目或者项目前期, 可以简单修改配置文件进行日志打印

以`logging.pattern.console= "%m %n"`为例; 会只打印message并换行

#### 同时设置xml及修改默认配置

如果是logging.level ; 那么yml优先级高. 这也是生产环境应该有的预期行为

其他的绝大多数配置则是logback.xml这种文件优先级高, 

比如logging.pattern.console同时在xml和yml中设置,  xml优先级高





