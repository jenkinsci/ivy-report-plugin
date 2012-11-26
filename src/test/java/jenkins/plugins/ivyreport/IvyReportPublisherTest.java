package jenkins.plugins.ivyreport;

import static jenkins.plugins.ivyreport.JenkinsJobProjectBuilder.aJenkinsJobProject;
import hudson.model.Result;
import hudson.model.Run;

import java.io.File;

import org.jvnet.hudson.test.HudsonTestCase;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * This test won't work if ant is not installed
 * 
 * @author Cedric Chabanois (cchabanois at gmail.com)
 * 
 */
public class IvyReportPublisherTest extends HudsonTestCase {
	private static final String IVY_SETTINGS_PROPERTY_FILES = "ivyconf.properties";
	private static final String IVY_SETTINGS_FILE = "ivyconf.xml";

	public void testIvyReport() throws Exception {
		// Given
		JenkinsJob job = aJenkinsJobProject("ivyProject")
				.withProjectZipFile(new File("resources/ivyProject.zip"))
				.withIvySettingsFile(IVY_SETTINGS_FILE)
				.withIvySettingsPropertyFiles(IVY_SETTINGS_PROPERTY_FILES)
				.create();

		// When
		Run run = job.run();

		// Then
		assertEquals(run.getLog(), Result.SUCCESS, run.getResult());
		assertTrue(run.getLog().contains("Publishing ivy report..."));
		File ivyReportDir = new File(run.getRootDir(), "ivyreport");

		File compileHtmlFile = new File(ivyReportDir,
				"entropysoft-test-compile.html");
		assertTrue(compileHtmlFile.exists());
		assertTrue(new File(ivyReportDir, "entropysoft-test-compile.svg")
				.exists());
		File defaultHtmlFile = new File(ivyReportDir,
				"entropysoft-test-default.html");
		assertTrue(defaultHtmlFile.exists());
		assertTrue(new File(ivyReportDir, "entropysoft-test-default.svg")
				.exists());
	}

	public void testMultiModuleIvyReport() throws Exception {
		// Given
		JenkinsJob job = aJenkinsJobProject("ivyMultiModuleProject")
				.withProjectZipFile(new File("resources/ivyMultiModuleProject.zip"))
				.withIvySettingsFile(IVY_SETTINGS_FILE)
				.withIvySettingsPropertyFiles(IVY_SETTINGS_PROPERTY_FILES)
				.create();
		
		// When
		Run run = job.run();
		
		// Then
		assertEquals(run.getLog(), Result.SUCCESS, run.getResult());
		assertTrue(run.getLog().contains("Publishing ivy report..."));
		File ivyReportDir = new File(run.getRootDir(), "ivyreport");
	
		for (int i = 1; i <= 3; i++) {
			String module = "entropysoft-module" + i;
			for (String conf : new String[] { "compile", "default" }) {
				for (String ext : new String[] { ".html", ".svg" }) {
					final File file = new File(ivyReportDir, module + '-' + conf + ext);
					assertTrue("missing " + file, file.exists());
				}
			}
		}
	}
}
