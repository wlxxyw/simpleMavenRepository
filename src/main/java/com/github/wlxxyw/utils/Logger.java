package com.github.wlxxyw.utils;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Logger {
	private static final Executor LOG_POOL = Executors.newSingleThreadExecutor(r -> new Thread(r,"log"));
	public static final boolean debuggable;
	private static final PrintStream print;
	static {
		String debug = System.getenv("debug");
		if(null==debug)debug = System.getProperty("debug");
		debuggable = Boolean.parseBoolean(debug);
		String logFilePath = System.getenv("logfile");
		if(null==logFilePath)logFilePath=System.getProperty("logfile");
		PrintStream printStream = null;
		if (null != logFilePath) {
			try {
			File logFile = new File(logFilePath);
			if ((logFile.isFile() && logFile.canWrite())
					|| logFile.createNewFile()) {
				printStream = new PrintStream(Files.newOutputStream(logFile.toPath()));
			}
			} catch (Throwable ignored) {
			}
		}
		print = Optional.ofNullable(printStream).orElse(System.out);
	}
	private static final SimpleDateFormat sdf = new SimpleDateFormat(
			"HH:mm:ss.SSS");
	private static final Pattern m = Pattern.compile("\\{(\\d*)\\}");
	private static final int ERROR = 0;
	private static final int WARN = 10;
	private static final int INFO = 20;
	private static final int DEBUG = 30;

	public static void debug(final String debug, Object... args) {
		if (debuggable) {
			print(Thread.currentThread(), DEBUG, System.currentTimeMillis(), debug, args);
		}
	}

	public static void info(final String info, Object... args) {
		print(Thread.currentThread(), INFO, System.currentTimeMillis(), info, args);
	}

	public static void warn(final String warn, Object... args) {
		print(Thread.currentThread(), WARN, System.currentTimeMillis(), warn, args);
	}
	public static void error(final String warn, Object... args) {
		print(Thread.currentThread(), ERROR, System.currentTimeMillis(), warn, args);
	}

	private static Throwable formatter(StringBuffer sb, String template, Object... args) {
		Matcher matcher = m.matcher(template);
		int index = 0;
		while (matcher.find()) {
			String _index = matcher.group(1);
			if (null != _index && !_index.trim().isEmpty()) {
				matcher.appendReplacement(
						sb,
						String.valueOf(args[Integer.parseInt(_index)]).replace(
								"\\", "\\\\"));
			} else {
				matcher.appendReplacement(sb, String.valueOf(args[index])
						.replace("\\", "\\\\"));
			}
			if (index++ == args.length) {
				break;
			}
		}
		matcher.appendTail(sb);
		sb.append("\n");
		if(args.length>0&&args[args.length-1] instanceof Throwable){
			return ((Throwable)args[args.length-1]);
		}
		return null;
	}

	private static void print(final Thread t,final int level,final Long time,final String msg,final Object...args) {
		StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
		LOG_POOL.execute(() -> {
			StringBuilder cache = new StringBuilder();
			switch (level) {
				case ERROR:
					cache.append("\033[1;31m[ERROR]\033[0m");
					break;
				case WARN:
					cache.append("\033[1;31m[WRAN]\033[0m");
					break;
				case INFO:
					cache.append("[INFO]");
					break;
				case DEBUG:
					cache.append("\033[1;32m[DEBUG]\033[0m");
					break;
			}
			cache.append(" ");
			cache.append(t.getName());
			cache.append(" ");
			cache.append(sdf.format(new Date(time)));
			cache.append(" ");

			cache.append(stackTraceElement.getClassName()).append("[").append(stackTraceElement.getLineNumber()).append("] ");
			StringBuffer sub = new StringBuffer();
			Throwable t1 = formatter(sub,msg,args);
			cache.append(sub);
			synchronized (print){

				print.print(cache);
				if(null!= t1){
					t1.printStackTrace(print);
				}
			}

		});
	}
}
