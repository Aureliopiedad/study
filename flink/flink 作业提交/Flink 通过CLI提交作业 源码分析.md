# Flink 通过CLI提交作业 源码分析

通过Flink Cli提交命令为：

```shell
$ ./bin/flink [ACTION] [OPTION] [ARGUMENTS]
```

执行cli命令后，进行作业提交阶段。相关的类位置在：

```yaml
#定义了执行程序的前端命令行实现
org.apache.flink.client.cli.CliFrontend.java
#命令行提取器，用于提取命令行选项
org.apache.flink.client.cli.CliFrontendParser.java
```

大致的作业提交流程为：

加载配置->参数解析->程序参数设置

```java
public class CliFrontend {
	...
    //flink 命令 [action]
    //编译并运行
    //flink run [OPTIONS] <jar-file> <arguments>
    private static final String ACTION_RUN = "run";
    //在run-application模式下，m
    private static final String ACTION_RUN_APPLICATION = "run-application";
    //显示程序的执行计划
    //flink info [OPTIONS] <jar-file> <arguments>
    private static final String ACTION_INFO = "info";
    //列出job
    //flink list [OPTIONS]
    private static final String ACTION_LIST = "list";
    //取消一个job
    //flink cancel [OPTIONS] <Job ID>
    private static final String ACTION_CANCEL = "cancel";
    //仅用于流处理，停止一个job并提供一个保存点
    //flink stop [OPTIONS] <Job ID>
    private static final String ACTION_STOP = "stop";
    //为正在运行的job触发保存点或部署存在的一个保存点
    //flink savepoint [OPTIONS] <Job ID> [<target directory>]
    private static final String ACTION_SAVEPOINT = "savepoint";
    ...
    public CliFrontend(
        	Configuration configuration, 
        	List<CustomCommandLine> customCommandLines) {
        this(configuration, new DefaultClusterClientServiceLoader(), customCommandLines);
    }
    
    public CliFrontend(
            Configuration configuration,
            ClusterClientServiceLoader clusterClientServiceLoader,
            List<CustomCommandLine> customCommandLines) {
        this.configuration = 
            checkNotNull(configuration);
        this.customCommandLines = 
            checkNotNull(customCommandLines);
        this.clusterClientServiceLoader = 
            checkNotNull(clusterClientServiceLoader);

        FileSystem.initialize(
                configuration, PluginUtils.createPluginManagerFromRootFolder(configuration));

        this.customCommandLineOptions = new Options();

        for (CustomCommandLine customCommandLine : customCommandLines) {
            customCommandLine.addGeneralOptions(customCommandLineOptions);
            customCommandLine.addRunOptions(customCommandLineOptions);
        }

        this.clientTimeout = 
            configuration.get(ClientOptions.CLIENT_TIMEOUT);
        this.defaultParallelism = 
            configuration.getInteger(CoreOptions.DEFAULT_PARALLELISM);
    }
    
	public static void main(final String[] args) {
		//加载日志文件，输出到./flink/log/flink-root-client-{hostname}.log
		//输出内容包括：version、scala、date、os user、Hadoop user、jvm、heap、JAVA_HOME等
		EnvironmentInformation.logEnvironmentInfo(LOG, "Command Line Client", args);
		
		//获取flink conf文件地址，先获取系统变量$FLINK_CONF_DIR，若无，则判断是否存在../conf文件夹，再无，则判断./conf；若都没有则抛出异常
		final String configurationDirectory = 
            getConfigurationDirectoryFromEnv();
		
		//根据配置文件地址加载全局配置，需要命名为"flink-conf.yaml"
		//逐行读取文件，通过#:区分有效配置
		final Configuration configuration = 
            GlobalConfiguration.loadConfiguration(configurationDirectory);
		
		//读取cli参数
		final List<CustomCommandLine> customCommandLines = 
            loadCustomCommandLines(configuration, configurationDirectory);
        
        //？
        int retCode = 31;
        
        //真正执行
        try {
            final CliFrontend cli = new CliFrontend(
                configuration, 
                customCommandLines);

            SecurityUtils.install(
                new SecurityConfiguration(
                	cli.configuration)
            );
            
            retCode = SecurityUtils
                .getInstalledContext()
                .runSecured(
                	() -> cli.parseAndRun(args)
            	);
        } catch (Throwable t) {
            ...
        } finally {
            System.exit(retCode);
        }
	}
    
    public int parseAndRun(String[] args) {
        if (args.length < 1) {
        	//输出合法cli格式，相当于-h
            CliFrontendParser.printHelp(customCommandLines);
            System.out.println("Please specify an action.");
            return 1;
        }
        
        String action = args[0];
        
        final String[] params = Arrays.copyOfRange(args, 1, args.length);
        
        try {
            switch (action) {
                case ACTION_RUN:
                	//篇幅问题，这里只说明run
                    run(params);
                    return 0;
                case ACTION_RUN_APPLICATION:
                    runApplication(params);
                    return 0;
                case ACTION_LIST:
                    list(params);
                    return 0;
                case ACTION_INFO:
                    info(params);
                    return 0;
                case ACTION_CANCEL:
                    cancel(params);
                    return 0;
                case ACTION_STOP:
                    stop(params);
                    return 0;
                case ACTION_SAVEPOINT:
                    savepoint(params);
                    return 0;
                case "-h":
                case "--help":
                    CliFrontendParser.printHelp(customCommandLines);
                    return 0;
                case "-v":
                case "--version":
                    //控制台输出版本信息
                    return 0;
                default:
                    //控制台输出不合法警告
                    return 1;
            }
        } catch (CliArgsException ce) {
            return handleArgException(ce);
        } catch (ProgramParametrizationException ppe) {
            return handleParametrizationException(ppe);
        } catch (ProgramMissingJobException pmje) {
            return handleMissingJobException();
        } catch (Exception e) {
            return handleError(e);
        }
    }
    
    protected void run(String[] args) throws Exception {
    	//同样打印在./flink/log/flink-root-client-{hostname}.log
    	LOG.info("Running 'run' command.");
    	
        //返回一个包括十几种常规option的options
    	final Options commandOptions = 
            CliFrontendParser.getRunCommandOptions();
        //将options转化为CommandLine
        final CommandLine commandLine = 
            getCommandLine(commandOptions, args, true);
        
        //输入的是run ... -h，控制台输出对应cli的help
        if (commandLine.hasOption(HELP_OPTION.getOpt())) {
            CliFrontendParser.printHelpForRun(customCommandLines);
            return;
        }
        
        //检查是否valid
        final CustomCommandLine activeCommandLine = 
            validateAndGetActiveCommandLine(
            	checkNotNull(commandLine));
        
        //ProgramOptions是使用JAR的一个基础类，包括文件地址、main类地址等
        final ProgramOptions programOptions = ProgramOptions.create(commandLine);
        
        final List<URL> jobJars = getJobJarAndDependencies(programOptions);
        
        //将CommandLine转为conf
        final Configuration effectiveConfiguration = 
            getEffectiveConfiguration(
            	activeCommandLine, 
            	commandLine, 
            	programOptions, 
            	jobJars);
        
        try (PackagedProgram program = 
             getPackagedProgram(programOptions, effectiveConfiguration)) {
            //调用ClientUtils#executeProgram，其中执行前文中获取的main方法
            executeProgram(effectiveConfiguration, program);
        }
    }
	...
}
```

## CustomCommandLine 接口及通用实现

```java
//用于加载命令行界面
public interface CustomCommandLine {
	//返回该命令行是否需要执行
	boolean isActive(CommandLine commandLine);
	
	//每个CustomCommandLine都有唯一标识符
	String getId();
	
	//向运行选项添加自定义配置
	void addRunOptions(Options baseOptions);
	
	//向常规选项添加自定义配置
	void addGeneralOptions(Options baseOptions);
	
	//将命令行中的命令参数转化为配置
	Configuration toConfiguration(CommandLine commandLine) throws FlinkException;
	
	default CommandLine parseCommandLineOptions(
        	String[] args, boolean stopAtNonOptions) throws CliArgsException {
        final Options options = new Options();
        addGeneralOptions(options);
        addRunOptions(options);
        //提取命令行，转化为CommandLine
        return CliFrontendParser.parse(options, args, stopAtNonOptions);
    }
}
```

CustomCommandLine接口的通用实现为GenericCLI：

```java
public class GenericCLI implements CustomCommandLine {
	...
	public boolean isActive(CommandLine commandLine) {
		//当配置文件中指定execution.target或cli中指定executor或target，返回true
	}
	
	public String getId() {
        return "Generic CLI";
    }
    
    public void addRunOptions(Options baseOptions) {
        // nothing to add here
    }
    
    public void addGeneralOptions(Options baseOptions) {
        baseOptions.addOption(executorOption);//executor
        baseOptions.addOption(targetOption);//target
        //-D
        baseOptions.addOption(DynamicPropertiesUtil.DYNAMIC_PROPERTIES);
    }
}
```

## CliFrontendParser 类

//命令行选项提取器
```java
public class CliFrontendParser {
	static final Option HELP_OPTION =
            new Option("h","help",false,...);
    
    static final Option JAR_OPTION = 
    		new Option("j", "jarfile", true, "Flink program JAR file.");
    
    static final Option CLASS_OPTION =
            new Option("c","class",true, ...);
    //同理把所有的[option]列出
    ...
    
}
```