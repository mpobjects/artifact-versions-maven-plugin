/*
 * Copyright 2019, MP Objects, http://www.mp-objects.com
 */
package com.mpobjects.maven.plugins.artifactversions;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * Like the <em>get</em> goal, but it will also unpack the retrieved artifact.
 */
@Mojo(name = "unpack", requiresProject = false, defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true)
public class UnpackMojo extends GetMojo {

	@Component
	private ArchiverManager archiverManager;

	@Parameter(property = "mdep.unpack.encoding")
	private String encoding;

	@Parameter(property = "mdep.unpack.excludes")
	private String excludes;

	@Parameter(property = "dependency.ignorePermissions", defaultValue = "false")
	private boolean ignorePermissions;

	@Parameter(property = "mdep.unpack.includes")
	private String includes;

	/**
	 * Output location.
	 */
	@Parameter(property = "outputDirectory", required = true)
	private File outputDirectory;

	@Override
	protected void processArtifactResult(ArtifactResult aResult) throws MojoExecutionException {
		File file = aResult.getArtifact().getFile();
		try {
			if (!outputDirectory.mkdirs()) {
				getLog().debug("mkdirs() failed for " + outputDirectory);
			}

			if (!outputDirectory.exists()) {
				throw new MojoExecutionException("Location to write unpacked files to could not be created: " + outputDirectory);
			}

			UnArchiver unArchiver = getUnArchiver(aResult.getArtifact().getExtension(), file);

			unArchiver.setSourceFile(file);
			unArchiver.setDestDirectory(outputDirectory);

			unArchiver.extract();
		} catch (NoSuchArchiverException e) {
			throw new MojoExecutionException("Unknown archiver type", e);
		} catch (ArchiverException e) {
			throw new MojoExecutionException("Error unpacking file: " + file + " to: " + outputDirectory
					+ System.lineSeparator() + e.toString(), e);
		}
	}

	private FileSelector[] getFileSelectors() {
		if (StringUtils.isEmpty(excludes) && StringUtils.isEmpty(includes)) {
			return new FileSelector[0];
		}

		IncludeExcludeFileSelector selector = new IncludeExcludeFileSelector();
		if (StringUtils.isNotEmpty(excludes)) {
			selector.setExcludes(excludes.split(","));
		}
		if (StringUtils.isNotEmpty(includes)) {
			selector.setIncludes(includes.split(","));
		}
		return new FileSelector[] { selector };
	}

	private UnArchiver getUnArchiver(String aExtension, File aFile) throws NoSuchArchiverException {
		UnArchiver unArchiver;

		try {
			unArchiver = archiverManager.getUnArchiver(aExtension);
			getLog().debug("Found unArchiver by type: " + unArchiver);
		} catch (NoSuchArchiverException e) {
			unArchiver = archiverManager.getUnArchiver(aFile);
			getLog().debug("Found unArchiver by extension: " + unArchiver);
		}

		if (encoding != null && unArchiver instanceof ZipUnArchiver) {
			((ZipUnArchiver) unArchiver).setEncoding(encoding);
			getLog().info("Unpacks '" + aExtension + "' with encoding '" + encoding + "'.");
		}

		unArchiver.setIgnorePermissions(ignorePermissions);
		unArchiver.setFileSelectors(getFileSelectors());

		return unArchiver;
	}

}
