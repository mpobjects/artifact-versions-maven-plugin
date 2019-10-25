/*
 * Copyright 2019, MP Objects, http://www.mp-objects.com
 */
package com.mpobjects.maven.plugins.artifactversions;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * Like the <em>get</em> goal, but will copy the artifact to a givend estination.
 */
@Mojo(name = "copy", requiresProject = false, defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true)
public class CopyMojo extends GetMojo {

	// TODO: support dependency:copy parameters
	// https://github.com/apache/maven-dependency-plugin/blob/master/src/main/java/org/apache/maven/plugins/dependency/fromConfiguration/CopyMojo.java

	/**
	 * Destination file.
	 */
	@Parameter(property = "outputFile")
	private File outputFile;

	protected File getDestination() {
		if (outputFile != null) {
			return outputFile;
		}
		return null;
	}

	@Override
	protected void processArtifactResult(ArtifactResult aResult) throws MojoExecutionException {
		File file = aResult.getArtifact().getFile();
		File destinationFile = getDestination();
		try {
			FileUtils.copyFile(file, destinationFile);
		} catch (IOException e) {
			throw new MojoExecutionException(file, "Cannot copy to " + destinationFile, "Unable to copy file from " + file + " to " + destinationFile);
		}
	}

}
