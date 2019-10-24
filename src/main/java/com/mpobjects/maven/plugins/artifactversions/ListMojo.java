/*
 * Copyright 2019, MP Objects, http://www.mp-objects.com
 */
package com.mpobjects.maven.plugins.artifactversions;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.version.Version;

/**
 * List available versions of the specified artifact.
 */
@Mojo(name = "list", requiresProject = false)
public class ListMojo extends AbstractVersionsMojo {
	@Override
	protected void processArtifactVersions(Artifact aArtifact, List<Version> aVersions) throws MojoExecutionException, MojoFailureException {
		getLog().info("Available versions for: " + aArtifact);

		int idx = aVersions.size();
		for (Version version : aVersions) {
			getLog().info("[" + --idx + "] " + version);
		}
	}
}
