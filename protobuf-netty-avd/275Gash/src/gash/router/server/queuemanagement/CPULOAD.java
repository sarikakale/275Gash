package gash.router.server.queuemanagement;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class CPULOAD {

	public static void main(String[] args) {
		OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();

		/*
		 * JavaSysMon monitor = new JavaSysMon(); String osName =
		 * monitor.osName();
		 */
		System.out.println("getCpuProcessTime()" + " = " + operatingSystemMXBean.getSystemLoadAverage());
		// System.out.println("CPU Usage
		// "+monitor.cpuTimes().getCpuUsage(arg0));
		calCPU();
	}

	public static void calCPU() {
		for (int i = 0; i < 2; i++) {

			OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();

			for (Method method : operatingSystemMXBean.getClass().getDeclaredMethods()) {

				method.setAccessible(true);

				if (method.getName().startsWith("get")

						&& Modifier.isPublic(method.getModifiers())) {

					Object value;

					try {

						// System.out.println(" method "+method.getName());

						if (method.getName().equals("getProcessCpuTime"))
							;

						value = method.invoke(operatingSystemMXBean);

					} catch (Exception e) {

						value = e;
						System.out.println("...............CPU Time............." + value);

					}
					if (method.getName().equals("getProcessCpuLoad")) {
						System.out.println("...............CPU LOAD............." + value);
					}
					// try

					// System.out.println(method.getName() + " = " + value);

				} // if

			} // for

			// long nanoBefore = System.nanoTime();

			// long cpuBefore =
			// operatingSystemMXBean.com.sun.management.UnixOperatingSystem.getProcessCpuTime();

		}
	}

}