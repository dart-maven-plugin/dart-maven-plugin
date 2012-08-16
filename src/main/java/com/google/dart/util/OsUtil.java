package com.google.dart.util;

import java.util.Locale;

public class OsUtil {

	private static final String	OS_NAME	= System.getProperty("os.name")
												.toLowerCase(Locale.US);

	private static final String	OS_ARCH	= System.getProperty("os.arch")
												.toLowerCase(Locale.US);

	public static boolean isWindows() {
		return OS_NAME.indexOf("win") >= 0;
	}

	public static boolean isMac() {
		return OS_NAME.indexOf("mac") >= 0;

	}

	public static boolean isUnix() {
		return OS_NAME.indexOf("nix") >= 0 || OS_NAME.indexOf("nux") >= 0;
	}

	public static boolean isSolaris() {
		return OS_NAME.indexOf("sunos") >= 0;
	}

	public static boolean isArch64() {
		return OS_ARCH.contains("64");
	}
}
