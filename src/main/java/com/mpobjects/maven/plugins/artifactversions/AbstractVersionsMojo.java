package com.mpobjects.maven.plugins.artifactversions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;

import com.mpobjects.maven.plugins.artifactversions.util.VersionUtil;

/**
 *
 */
public abstract class AbstractVersionsMojo extends AbstractMojo {

	private static final Pattern REPO_PATTERN = Pattern.compile("(.+)::(.*)::(.+)");

	@Component
	protected RepositorySystem repositorySystem;

	@Parameter(defaultValue = "${session}", required = true, readonly = true)
	protected MavenSession session;

	/**
	 * A string of the form groupId:artifactId[:version[:packaging[:classifier]]].
	 */
	@Parameter(property = "artifact")
	private String artifact;

	/**
	 * The artifactId of the artifact to download. Ignored if {@link #artifact} is used.
	 */
	@Parameter(property = "artifactId")
	private String artifactId;

	/**
	 * The classifier of the artifact. Ignored if {@link #artifact} is used.
	 */
	@Parameter(property = "classifier")
	private String classifier;

	/**
	 * Version qualifiers which should be excluded from the result
	 */
	@Parameter(property = "excludeQualifiers")
	private String excludeQualifiers;

	/**
	 * The groupId of the artifact. Ignored if {@link #artifact} is used.
	 */
	@Parameter(property = "groupId")
	private String groupId;

	/**
	 * The packaging of the artifact. Ignored if {@link #artifact} is used.
	 */
	@Parameter(property = "packaging", defaultValue = "jar")
	private String packaging = "jar";

	private Set<String> qualifiersExcluded;

	private Set<String> qualifiersRequired;

	/**
	 * Repositories in the format id::[layout]::url or just url, separated by comma. ie.
	 * central::default::http://repo1.maven.apache.org/maven2,myrepo::::http://repo.acme.com,http://repo.acme2.com
	 */
	@Parameter(property = "remoteRepositories")
	private String remoteRepositories;

	/**
	 * Version qualifiers which are required in the result
	 */
	@Parameter(property = "requireQualifiers")
	private String requireQualifiers;

	/**
	 * The version (range) to accept. Ignored if {@link #artifact} is used.
	 */
	@Parameter(property = "version", defaultValue = "[0,)")
	private String version;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		qualifiersExcluded = new HashSet<>(Arrays.asList(StringUtils.split(Objects.toString(excludeQualifiers, ""), ",")));
		qualifiersRequired = new HashSet<>(Arrays.asList(StringUtils.split(Objects.toString(requireQualifiers, ""), ",")));

		try {
			VersionRangeRequest request = createVersionRangeRequest(createArtifact());
			VersionRangeResult response = repositorySystem.resolveVersionRange(session.getRepositorySession(), request);
			processVersionRangeResponse(response);
		} catch (VersionRangeResolutionException e) {
			throw new MojoExecutionException("Failed retrieving artifact version range information.", e);
		}
	}

	protected boolean acceptedVersion(Version aVersion) {
		if (qualifiersRequired.isEmpty() && qualifiersExcluded.isEmpty()) {
			return true;
		}

		Set<String> qualifiers = VersionUtil.getQualifiers(aVersion.toString());

		if (!qualifiersRequired.isEmpty() && !qualifiersRequired.stream().allMatch(qualifiers::contains)) {
			return false;
		}

		return !qualifiersExcluded.stream().anyMatch(qualifiers::contains);
	}

	protected VersionRangeRequest createVersionRangeRequest(Artifact aArtifact) throws MojoFailureException {
		VersionRangeRequest request = new VersionRangeRequest();
		request.setArtifact(aArtifact);
		request.setRepositories(getRemoteRepositories());
		return request;
	}

	protected List<RemoteRepository> getRemoteRepositories() throws MojoFailureException {
		List<RemoteRepository> repos = new ArrayList<>();
		// Include standard repos
		List<String> activeProfiles = session.getSettings().getActiveProfiles();
		for (Profile profile : session.getSettings().getProfiles()) {
			if (!activeProfiles.contains(profile.getId())) {
				continue;
			}
			for (Repository repo : profile.getRepositories()) {
				repos.add(toRemoteRepository(repo));
			}
		}
		repos = repositorySystem.newResolutionRepositories(session.getRepositorySession(), repos);

		// Add additional repos
		repos.addAll(repositorySystem.newResolutionRepositories(session.getRepositorySession(), parseRemoteRepositories()));
		return repos;
	}

	protected abstract void processArtifactVersions(Artifact aArtifact, List<Version> aVersions) throws MojoExecutionException, MojoFailureException;

	protected void processVersionRangeResponse(VersionRangeResult aResponse) throws MojoExecutionException, MojoFailureException {
		Artifact art = aResponse.getRequest().getArtifact();
		List<Version> versions = aResponse.getVersions().stream().filter(this::acceptedVersion).collect(Collectors.toList());
		processArtifactVersions(art, versions);
	}

	private Artifact createArtifact() throws MojoFailureException {
		if (artifact != null) {
			String[] tokens = artifact.split(":");
			if (tokens.length < 2 || tokens.length > 5) {
				throw new MojoFailureException("Invalid artifact, you must specify groupId:artifactId[:version[:packaging[:classifier]]] " + artifact);
			}

			String lver = "[0,)";
			String lpack = "jar";
			String lclass = "";
			if (tokens.length > 2 && !"".equals(tokens[2])) {
				lver = tokens[2];
			}
			if (tokens.length > 3 && !"".equals(tokens[3])) {
				lpack = tokens[3];
			}
			if (tokens.length > 4 && !"".equals(tokens[4])) {
				lclass = tokens[4];
			}
			return new DefaultArtifact(tokens[0], tokens[1], lclass, lpack, lver);
		} else {
			return new DefaultArtifact(groupId, artifactId, classifier, packaging, version);
		}
	}

	private List<RemoteRepository> parseRemoteRepositories() throws MojoFailureException {
		if (remoteRepositories == null) {
			return Collections.emptyList();
		}
		try {
			return Arrays.stream(StringUtils.split(remoteRepositories, ",")).map(this::parseRepositoryLocation).filter(Objects::nonNull)
				.collect(Collectors.toList());
		} catch (RuntimeException e) {
			throw new MojoFailureException(e.getMessage(), e);
		}
	}

	private RemoteRepository parseRepositoryLocation(String aRepo) {
		RepositoryPolicy always = new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_ALWAYS, RepositoryPolicy.CHECKSUM_POLICY_WARN);
		if (aRepo.contains("::")) {
			Matcher matcher = REPO_PATTERN.matcher(aRepo);
			if (!matcher.matches()) {
				throw new IllegalArgumentException("Invalid syntax for repository: " + aRepo + ". Use \"id::layout::url\" or \"URL\".");
			} else {
				String type = matcher.group(2);
				if ("".equals(type)) {
					type = "default";
				}
				return new RemoteRepository.Builder(matcher.group(1), type, matcher.group(3))
					.setPolicy(always).build();
			}
		} else {
			return new RemoteRepository.Builder("temp", "default", aRepo).setPolicy(always).build();
		}
	}

	private RemoteRepository toRemoteRepository(Repository aRepo) {
		RemoteRepository.Builder builder = new RemoteRepository.Builder(aRepo.getId(), aRepo.getLayout(), aRepo.getUrl());
		builder.setReleasePolicy(toRepositoryPolicy(aRepo.getReleases()));
		builder.setSnapshotPolicy(toRepositoryPolicy(aRepo.getSnapshots()));
		return builder.build();
	}

	private RepositoryPolicy toRepositoryPolicy(org.apache.maven.settings.RepositoryPolicy aPolicy) {
		boolean enabled = true;
		String checksums = RepositoryPolicy.CHECKSUM_POLICY_WARN;
		String updates = RepositoryPolicy.UPDATE_POLICY_DAILY;

		if (aPolicy != null) {
			enabled = aPolicy.isEnabled();
			if (aPolicy.getUpdatePolicy() != null) {
				updates = aPolicy.getUpdatePolicy();
			}
			if (aPolicy.getChecksumPolicy() != null) {
				checksums = aPolicy.getChecksumPolicy();
			}
		}

		return new RepositoryPolicy(enabled, updates, checksums);
	}
}
