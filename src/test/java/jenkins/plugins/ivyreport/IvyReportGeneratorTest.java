package jenkins.plugins.ivyreport;

import java.io.File;
import java.io.IOException;

import jenkins.plugins.ivyreport.IvyReportGenerator;
import jenkins.plugins.ivyreport.utils.TestAreaUtils;

import org.jvnet.hudson.test.HudsonTestCase;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * 
 * @author Cedric Chabanois (cchabanois at gmail.com)
 *
 */
public class IvyReportGeneratorTest extends HudsonTestCase {
	private static final File RESOLUTION_CACHE_ROOT = new File(
			"resources/testResolutionCache");
	private static final String RESOLVE_ID = "entropysoft-test";
	private IvyReportGenerator generator;
	private File targetDir;

	public void setUp() throws Exception {
		super.setUp();
		targetDir = TestAreaUtils.getNonExistingFileInTestArea("target");
		generator = new IvyReportGenerator(hudson, RESOLVE_ID, new String[] {
				"compile", "default" }, RESOLUTION_CACHE_ROOT, targetDir);
	}

	public void testReportGenerator() throws IOException, InterruptedException {

		// When
		generator.generateReports();

		// Then
		File compileHtmlFile = new File(targetDir,
				"entropysoft-test-compile.html");
		assertTrue(compileHtmlFile.exists());
		assertTrue(Files.toString(compileHtmlFile, Charsets.UTF_8).contains(
				"<object data=\"entropysoft-test-compile.svg\" type=\"image/svg+xml\">"));
		assertTrue(new File(targetDir, "entropysoft-test-compile.svg").exists());
		File defaultHtmlFile = new File(targetDir,
				"entropysoft-test-default.html");
		assertTrue(defaultHtmlFile.exists());
		assertTrue(Files.toString(defaultHtmlFile, Charsets.UTF_8).contains(
				"<object data=\"entropysoft-test-default.svg\" type=\"image/svg+xml\">"));
		assertTrue(new File(targetDir, "entropysoft-test-default.svg").exists());
	}

}
