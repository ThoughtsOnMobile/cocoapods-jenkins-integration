/**
 * Copyright 2012 http://www.thoughtsonmobile.com
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.thoughtsonmobile.jenkins.cocoapods;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;

import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import hudson.util.ArgumentListBuilder;

/**
 * CocoaPods Builder. This builder performs a "pod install" and "pod
 * update" if it is configured in a jenkins build.
 *
 * @author Leif Janzik (leif.janzik@gmail.com)
 * @version 0.2
 */
public class CocoaPodsBuilder extends Builder {
  /**
   * Descriptor for {@link CocoaPodsBuilder}. Used as a singleton. The
   * class is marked as public so that it can be accessed from views.
   */
  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    /**
     * This human readable name is used in the configuration screen.
     *
     * @return Title which is shown in a build configuration
     */
    @Override
    public String getDisplayName() {
      return "Update CocoaPods";
    }

    /**
     * checks if plugin is available in project.
     *
     * @param aClass project class
     *
     * @return always true
     */
    @Override
    public boolean isApplicable(final Class<?extends AbstractProject> aClass) {
      return true;
    }
  }

  /**
   * if true &quot;Pods&quot; folder will be removed before refreshing.
   */
  private final boolean cleanPods;

  /**
   * if {@code true} cocoapods will be verbose in the log trail.
   */
  private final boolean verbose;

  /**
   * Where the Xcode project resides (ie. where the working directory cocoapods should be run with 
   */
  private final String projectRoot;

  public boolean getCleanPods() {
      return cleanPods;
  }

  public boolean getVerbose() {
      return verbose;
  }

  public String getProjectRoot() {
      return projectRoot;
  }

/**
   * Creates a new CocoaPodsBuilder object.
   *
   * @param cleanpods value of per-project checkbox, if true the &quot;pod&quot; folder will
   *   be removed before refreshing pods
   */
  @DataBoundConstructor
  public CocoaPodsBuilder(final boolean cleanpods, final boolean verbose, final String projectRoot) {
    cleanPods = cleanpods;
    this.verbose = verbose;
    this.projectRoot = projectRoot;
  }

  /**
   * returns builder descriptor.
   *
   * @return builder descriptor
   */
  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  /**
   * This method is called during a jenkins build.
   *
   * @param build current build
   * @param launcher the launcher
   * @param listener the build listener
   *
   * @return true if build step was successfull, false otherwise.
   */
  @Override
  public boolean perform(
                         final AbstractBuild<?, ?> build,
                         final Launcher launcher, final BuildListener listener) {
    try {
      final EnvVars env = build.getEnvironment(listener);
      env.putAll(build.getBuildVariables());

      FilePath projectRootPath = build.getWorkspace().child(projectRoot);

      final ArgumentListBuilder args = new ArgumentListBuilder();

      if (cleanPods) {
        args.addTokenized("rm -r -f Pods");
      }

      args.addTokenized("pod repo update");

      final ArgumentListBuilder args2 = new ArgumentListBuilder();
      args2.addTokenized("pod install");

      if (verbose) {
	      args2.add("--verbose");
      }

      final int resultInstall =
        launcher.decorateFor(build.getBuiltOn()).launch().cmds(args).envs(env)
                 .stdout(listener).pwd(projectRootPath).join();
      final int resultUpdate  =
        launcher.decorateFor(build.getBuiltOn()).launch().cmds(args2).envs(env)
                 .stdout(listener).pwd(projectRootPath).join();

      return (resultInstall == 0) && (resultUpdate == 0);
    } catch (final IOException e) {
      e.printStackTrace();
    } catch (final InterruptedException e) {
      e.printStackTrace();
    }

    return false;
  }
}
