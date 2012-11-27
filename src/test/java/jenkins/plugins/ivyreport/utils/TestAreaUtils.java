package jenkins.plugins.ivyreport.utils;

import java.io.File;
import java.io.IOException;

public class TestAreaUtils {

	private static File getTestArea() {
		return new File(System.getProperty("testarea",
				"target/testarea"));
	}

	public static File getNonExistingFileInTestArea(String name)
			throws IOException {
		File file = getFileInTestArea(name);
		if (file.exists()) {
			if (!deleteFileRecursively(file))
				throw new IOException("Cannot delete file " + file.toString());
		}
		return file;
	}

	private static File getFileInTestArea(String name) {
		return new File(getTestArea(), name);
	}

	private static boolean deleteFileRecursively(File file) throws IOException {
		File[] files = file.listFiles();

		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory())
					deleteFileRecursively(files[i]);
				else
					files[i].delete();
			}
		}
		return file.delete();
	}
}
