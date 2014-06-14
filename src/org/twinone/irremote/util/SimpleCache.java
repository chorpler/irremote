package org.twinone.irremote.util;

import java.io.File;

import org.twinone.irremote.util.FileUtils;

import android.content.Context;

public class SimpleCache {

	private static File getFile(Context c, String name) {
		return new File(c.getCacheDir(), name);
	}

	public static void remove(Context c, String filename) {
		FileUtils.remove(getFile(c, filename));
	}

	public static boolean isAvailable(Context c, String filename) {
		return FileUtils.exists(getFile(c, filename));
	}

	public static String get(Context c, String filename) {
		return FileUtils.read(getFile(c, filename));
	}

	public static void put(Context c, String filename, String data) {
		FileUtils.write(getFile(c, filename), data);
	}

}