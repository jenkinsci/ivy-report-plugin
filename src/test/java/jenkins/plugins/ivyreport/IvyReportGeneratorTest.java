package jenkins.plugins.ivyreport;

import java.io.File;
import java.io.IOException;

import jenkins.plugins.ivyreport.IvyReportGenerator;
import jenkins.plugins.ivyreport.utils.TestAreaUtils;

import org.jvnet.hudson.test.HudsonTestCase;

public class IvyReportGeneratorTest extends HudsonTestCase {
	private static final File RESOLUTION_CACHE_ROOT = new File(
			"resources/testResolutionCache");
	private static final String RESOLVE_ID = "entropysoft-test";
	private IvyReportGenerator generator;

	public void setUp() throws Exception {
		super.setUp();
		generator = new IvyReportGenerator(hudson, RESOLVE_ID, new String[] {
				"compile", "default" }, RESOLUTION_CACHE_ROOT,
				TestAreaUtils.getNonExistingFileInTestArea("target"));
	}

	public void testReportGenerator() throws IOException, InterruptedException {
		File file = generator.generateReports();
		assertTrue(file.exists());
	}
}
