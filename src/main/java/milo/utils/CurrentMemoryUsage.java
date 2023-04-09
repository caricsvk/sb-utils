package milo.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CurrentMemoryUsage {

	public static void printBoth(String prefix) {
		printMemoryBean(prefix);
		printRuntime(prefix);
	}

	public static void printRuntime(String prefix) {
		Runtime runtime = Runtime.getRuntime();
		long totalMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();
		long maxMemory = runtime.maxMemory();
		long usedMemory = totalMemory - freeMemory;

		System.out.printf(LocalDateTime.now() + ": " + prefix +
				": runtime memory used: %d MB, max: %d MB%n", usedMemory / (1024 * 1024), maxMemory / (1024 * 1024));
	}

	public static void printMemoryBean(String prefix) {
		MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
		MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
		long usedMemory = heapUsage.getUsed();
		long maxMemory = heapUsage.getMax();
		System.out.printf(LocalDateTime.now() + ": " + prefix +
				": heap memory used: %d MB, max: %d MB%n", usedMemory / (1024 * 1024), maxMemory / (1024 * 1024));
	}
}
