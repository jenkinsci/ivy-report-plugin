/*
 * The MIT License
 *
 * Copyright (c) 2012, Cedric Chabanois
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE. 
 */
package jenkins.plugins.ivyreport;

import hudson.FilePath;
import hudson.ivy.ModuleName;
import hudson.ivy.IvyModuleSetBuild;
import hudson.model.Action;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Action used to display the ivy report for the build
 * 
 * @author Cedric Chabanois (cchabanois at gmail.com)
 * 
 */
public class IvyReportBuildAction implements Action {
    private static final String ICON_FILENAME = "/plugin/ivy-report/ivyReport.png";

    private File dir;
    private List<IvyReport> reports;

    // backward compatibility:
    private transient IvyModuleSetBuild build;
    private transient String indexFileName;

    public IvyReportBuildAction(File dir, List<IvyReport> reports) {
        super();
        this.dir = dir;
        this.reports = reports;
    }

    public String getUrlName() {
        return "ivyreport";
    }

    public String getDisplayName() {
        return "Ivy report";
    }

    public String getIconFileName() {
        return ICON_FILENAME;
    }

    public List<IvyReport> getReports() {
        return reports;
    }

    public IvyReport doReport(StaplerRequest req, StaplerResponse res)
            throws MalformedURLException, ServletException, IOException {
        String moduleName = req.getRestOfPath();
        if (!moduleName.isEmpty()) {
            if (moduleName.charAt(0) == '/') {
                moduleName = moduleName.substring(1);
            }
            for (IvyReport report : getReports()) {
                if (moduleName.equals(report.getName().toFileSystemName())) {
                    return report;
                }
            }
        }
        // handle CSS, etc.
        File siblingFile = new File(dir, moduleName);
        if (siblingFile.exists()) {
            res.serveFile(req, siblingFile.toURI().toURL());
        }
        return null;
    }

    private Object readResolve() {
        if (indexFileName != null) {
            dir = new File(build.getRootDir(), "ivyreport");
            final File report = new File(new File(build.getRootDir(), "ivyreport"),
                    indexFileName);
            final ModuleName dummy = new ModuleName(null, null) {
                private static final long serialVersionUID = 1L;

                @Override
                public String toFileSystemName() {
                    return indexFileName;
                }
            };
            reports = Collections.singletonList(new IvyReport(dummy,
                    new FilePath(report)));
        }
        return this;
    }
}
