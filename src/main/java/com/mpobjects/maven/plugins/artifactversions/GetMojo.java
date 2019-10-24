/*
 * Copyright 2019, MP Objects, http://www.mp-objects.com
 */
package com.mpobjects.maven.plugins.artifactversions;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.version.Version;

/**
 * Get a specific version of the artifact. By default the latest version is retrieved. With the offset parameter an
 * older version can be retrieved.
 */
@Mojo(name = "get", requiresProject = false)
public class GetMojo extends AbstractVersionsMojo {

	/**
	 * Version offset to get. The latest selected version is at offset 0, the second latest is 1, etc.
	 */
	@Parameter(property = "offset")
	private int offset;

	protected void processArtifactResult(ArtifactResult aResult) throws MojoExecutionException {
		// nop
	}

	@Override
	protected void processArtifactVersions(Artifact aArtifact, List<Version> aVersions) throws MojoExecutionException, MojoFailureException {
		offset = Math.max(0, offset);
		if (offset >= aVersions.size()) {
			throw new MojoExecutionException("No version available at offset " + offset + " for artifact: " + aArtifact);
		}

		Version version = aVersions.get(aVersions.size() - offset - 1);
		Artifact getArtifact = aArtifact.setVersion(version.toString());

		ArtifactRequest request = new ArtifactRequest(getArtifact, getRemoteRepositories(), "");
		ArtifactResult result;
		try {
			result = repositorySystem.resolveArtifact(session.getRepositorySession(), request);
		} catch (ArtifactResolutionException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

		processArtifactResult(result);
	}

}
