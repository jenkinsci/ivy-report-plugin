/*
 * The MIT License
 *
 * Copyright (c) 2012, The original author or authors
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

import hudson.AbortException;
import hudson.FilePath;
import hudson.ivy.IvyMessageImpl;
import hudson.ivy.IvyModule;
import hudson.ivy.IvyModuleSetBuild;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.ivy.Ivy;
import org.apache.ivy.Ivy.IvyCallback;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.ModuleDescriptorParserRegistry;
import org.apache.ivy.util.ConfigurationUtils;
import org.apache.ivy.util.Message;

/**
 * A bunch of code copied from the Ivy plugin.
 */
class IvyAccess {
    /**
     * The name of a copy of the ivy file relative to the projects root dir
     * since the workspace may not always be accessible.
     */
    private static final String BACKUP_IVY_FILE_NAME = "ivy.xml";

    private static final Logger LOGGER = Logger.getLogger(IvyAccess.class
            .getName());

    private final IvyModuleSetBuild build;
    private final IvyModule ivyModule;
    private ModuleDescriptor moduleDescriptor;

    IvyAccess(IvyModuleSetBuild build, IvyModule ivyModule) {
        this.build = build;
        this.ivyModule = ivyModule;
    }

    /**
     * Will only copy the file from the repository if its last modified time
     * exceeds what the instance thinks is the last recorded modified time of
     * the localFile, which is the local backup ivy file copy. For this to
     * operate properly for remoting circumstances, the master and slave
     * instances must be reasonably time synchronized.
     * 
     * @param source
     *            Workspace root Directory
     * @param target
     *            The local file to be copied to
     * @return true iff the file was actually copied
     * @throws IOException
     *             If unable to access/copy the workspace ivy file
     * @throws InterruptedException
     *             If interrupted while accessing the workspace ivy file
     */
    private boolean copyIvyFileFromWorkspaceIfNecessary(FilePath source,
            File target) throws IOException, InterruptedException {
        boolean copied = false;
        if (source != null) { // Unless the workspace is non-null we can not
                              // copy a new ivy file
            // Copy the ivy file from the workspace (possibly at a slave) to the
            // projects dir (at Master)
            FilePath backupCopy = new FilePath(target);
            long flastModified = source.lastModified();
            if (flastModified == 0l)
                throw new FileNotFoundException("Can't stat file " + source);
            if (flastModified > backupCopy.lastModified()) {
                source.copyTo(backupCopy);
                target.setLastModified(flastModified);
                copied = true;
                LOGGER.info("Copied the workspace ivy file to backup");
            }
        }
        return copied;
    }

    /**
     * Force the creation of the module descriptor.
     * 
     * @param b
     *            a build this trigger belongs to
     */
    private void recomputeModuleDescriptor() {
        // The build may be null if no build with a workspace was found
        if (build == null) {
            return;
        }
        LOGGER.fine("Recomputing ModuleDescriptor for Project "
                + build.getProject().getFullDisplayName());
        Ivy ivy = getIvy();
        if (ivy == null) {
            return;
        }
        final File ivyF = new File(build.getProject().getRootDir(), ivyModule
                .getModuleName().toFileSystemName()
                + '$'
                + BACKUP_IVY_FILE_NAME);
        try {
            copyIvyFileFromWorkspaceIfNecessary(
                    build.getWorkspace().child(ivyModule.getRelativePath()),
                    ivyF);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,
                    "Failed to access the workspace ivy file", e);
            LOGGER.log(Level.WARNING, "Removing ModuleDescriptor");
            return;
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING,
                    "Interrupted while accessing the workspace ivy file", e);
            if (ivyF.canRead())
                LOGGER.log(Level.WARNING, "Will try to use use existing backup");
        }
        // Calculate ModuleDescriptor from the backup copy
        if (!ivyF.canRead()) {
            LOGGER.log(Level.WARNING,
                    "Cannot read ivy file backup...removing ModuleDescriptor");
            return;
        }
        moduleDescriptor = (ModuleDescriptor) ivy.execute(new IvyCallback() {
            public Object doInIvyContext(Ivy ivy, IvyContext context) {
                try {
                    return ModuleDescriptorParserRegistry.getInstance()
                            .parseDescriptor(ivy.getSettings(),
                                    ivyF.toURI().toURL(),
                                    ivy.getSettings().doValidate());
                } catch (MalformedURLException e) {
                    LOGGER.log(Level.WARNING, "The URL is malformed : " + ivyF,
                            e);
                    return null;
                } catch (ParseException e) {
                    LOGGER.log(Level.WARNING,
                            "Parsing error while reading the ivy file " + ivyF,
                            e);
                    return null;
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING,
                            "I/O error while reading the ivy file " + ivyF, e);
                    return null;
                }
            }
        });
    }

    /**
     * 
     * @return the Ivy instance based on the {@link #ivyConfName}
     * @throws AbortException
     * 
     * @throws ParseException
     * @throws IOException
     */
    public Ivy getIvy() {
        Message.setDefaultLogger(new IvyMessageImpl());
        String ivySettingsFile = build.getProject().getIvySettingsFile();
        File settingsLoc = (ivySettingsFile == null) ? null : new File(build
                .getWorkspace().getRemote(), ivySettingsFile);

        if ((settingsLoc != null) && (!settingsLoc.exists())) {
            return null;
        }

        String ivySettingsPropertyFiles = build.getProject()
                .getIvySettingsPropertyFiles();
        ArrayList<File> propertyFiles = new ArrayList<File>();
        if (StringUtils.isNotBlank(ivySettingsPropertyFiles)) {
            for (String file : ivySettingsPropertyFiles.split(",")) {
                File propertyFile = new File(build.getWorkspace().getRemote(),
                        file.trim());
                if (!propertyFile.exists()) {
                    LOGGER.warning("Skipped property file " + file);
                }
                propertyFiles.add(propertyFile);
            }
        }

        try {
            IvySettings ivySettings = new IvySettings();
            for (File file : propertyFiles) {
                ivySettings.loadProperties(file);
            }
            if (settingsLoc != null) {
                ivySettings.load(settingsLoc);
                LOGGER.fine("Configured Ivy using custom settings "
                        + settingsLoc.getAbsolutePath());
            } else {
                ivySettings.loadDefault();
                LOGGER.fine("Configured Ivy using default 2.1 settings");
            }
            return Ivy.newInstance(ivySettings);
        } catch (Exception e) {
            LOGGER.severe("Error while reading the default Ivy 2.1 settings: "
                    + e.getMessage());
            LOGGER.severe(Arrays.toString(e.getStackTrace()));
        }
        return null;
    }

    String[] expandConfs(String[] requested) {
        if (moduleDescriptor == null) {
            recomputeModuleDescriptor();
            if (moduleDescriptor == null) {
                return requested;
            }
        }
        String[] expanded = ConfigurationUtils.replaceWildcards(requested,
                moduleDescriptor);
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        Collections.addAll(result, expanded);
        result.retainAll(Arrays.asList(moduleDescriptor
                .getConfigurationsNames()));
        return result.toArray(new String[result.size()]);
    }
}
