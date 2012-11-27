package jenkins.plugins.ivyreport;

import hudson.ivy.IvyModuleSet;
import hudson.ivy.builder.AntIvyBuilderType;
import hudson.model.TopLevelItem;
import hudson.model.Hudson;

import java.io.File;
import java.lang.reflect.Field;

import org.jvnet.hudson.test.ExtractResourceSCM;

/**
 * 
 * @author Cedric Chabanois (cchabanois at gmail.com)
 * 
 */
public class JenkinsJobProjectBuilder {
    private final String name;
    private File resource;
    private String ivySettingsFile;
    private String ivySettingsPropertyFiles;

    private JenkinsJobProjectBuilder(String name) {
        this.name = name;
    }

    public static JenkinsJobProjectBuilder aJenkinsJobProject(String name) {
        return new JenkinsJobProjectBuilder(name);
    }

    public JenkinsJobProjectBuilder withProjectZipFile(File resource) {
        this.resource = resource;
        return this;
    }

    public JenkinsJobProjectBuilder withIvySettingsFile(String ivySettingsFile) {
        this.ivySettingsFile = ivySettingsFile;
        return this;
    }

    public JenkinsJobProjectBuilder withIvySettingsPropertyFiles(
            String ivySettingsPropertyFiles) {
        this.ivySettingsPropertyFiles = ivySettingsPropertyFiles;
        return this;
    }

    public JenkinsJob create() throws Exception {
        Hudson jenkins = Hudson.getInstance();
        TopLevelItem item = jenkins.getItem(name);
        if (item != null) {
            item.delete();
        }
        IvyModuleSet job = jenkins.createProject(IvyModuleSet.class, name);
        job.setIvySettingsFile(ivySettingsFile);
        job.setIvySettingsPropertyFiles(ivySettingsPropertyFiles);
        Field field = IvyModuleSet.class.getDeclaredField("ivyBuilderType");
        try {
            field.setAccessible(true);
            field.set(job, new AntIvyBuilderType("default", "", "", "", ""));
        } finally {
            field.setAccessible(false);
        }
        job.getPublishers().add(
                new IvyReportPublisher("default,compile", "entropysoft-test"));
        job.setScm(new ExtractResourceSCM(resource.toURI().toURL()));

        job.save();
        return new JenkinsJob(job);
    }

}
