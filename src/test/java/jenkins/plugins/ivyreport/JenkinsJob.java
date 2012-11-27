package jenkins.plugins.ivyreport;

import hudson.ivy.IvyModuleSet;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Run;
import hudson.scm.SubversionSCM;
import hudson.scm.SubversionSCM.ModuleLocation;

import java.util.concurrent.Future;

public class JenkinsJob {
	private final AbstractProject project;

	public JenkinsJob(AbstractProject project) {
		this.project = project;
	}

	public static JenkinsJob aJenkinsJob(String name) {
		Hudson jenkins = Hudson.getInstance();
		AbstractProject job = (AbstractProject) jenkins.getItem(name);
		if (job == null) {
			return null;
		} else {
			return new JenkinsJob(job);
		}
	}
	
	public AbstractProject getProject() {
		return project;
	}
	
	public Run run() throws Exception {
		Future<Run> future = project.scheduleBuild2(0);
		Run run = future.get();
		return run;
	}

	public void delete() throws Exception {
		project.delete();
	}

	public IvyModuleSet getIvyModuleSet() {
		return (IvyModuleSet)project;
	}
	
}
