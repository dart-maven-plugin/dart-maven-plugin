package com.google.dart.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;
import org.sonatype.aether.util.filter.DependencyFilterUtils;

/**
 * Resolver of one artifact name into a list of JAR files.
 *
 * @author Yegor Bugayenko (yegor@rempl.com)
 * @version $Id$
 */
public abstract class MavenResolver {

	/**
	 * The entry point to Aether, i.e. the component
	 * doing all the work.
	 */
	private RepositorySystem system;

	/**
	 * The current repository/network configuration of Maven.
	 */
	private RepositorySystemSession session;

	/**
	 * The project's remote repositories to use for the resolution
	 * of plugins and their dependencies.
	 */
	private List<RemoteRepository> remotes;

	/**
	 * Public ctor.
	 *
	 * @param sys  Repository system
	 * @param ses  Repository system session
	 * @param reps Remote reps
	 */
	public MavenResolver(final RepositorySystem sys,
			final RepositorySystemSession ses, final List<RemoteRepository> reps) {
		this.system = sys;
		this.session = ses;
		this.remotes = reps;
		if (getLog().isDebugEnabled()) {
			getLog().debug("#MavenResolver(" + sys.getClass().getName() + ", " + ses.getClass().getName() + ", reps)");
		}
	}

	/**
	 * Resolves the Artifact from the remote repository
	 * if nessessary.
	 *
	 * @param artifact The artifact to find
	 * @return Collection of JAR files
	 */
	public Collection<File> resolve(final Artifact artifact) throws DependencyResolutionException {
		final Collection<ArtifactResult> found = this.artifacts(artifact);
		final Collection<File> jars = new ArrayList<File>();
		for (ArtifactResult aresult : found) {
			if (getLog().isDebugEnabled()) {
				getLog().debug(aresult.getArtifact() + " resolved to " + aresult.getArtifact().getFile());
			}
			jars.add(aresult.getArtifact().getFile());
		}
		return jars;
	}

	/**
	 * Return a list of found artifacts.
	 *
	 * @param artifact The name of the artifact to find
	 * @return The collection of artifacts found
	 */
	private Collection<ArtifactResult> artifacts(final Artifact artifact) throws DependencyResolutionException {

		final CollectRequest collectRequest = request(artifact);
		final DependencyRequest request =
				new DependencyRequest(collectRequest, DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE));
		return system.resolveDependencies(session, request).getArtifactResults();
	}

	/**
	 * Create dependency request.
	 *
	 * @param artifact The artifact to find
	 * @return The request
	 * @see #artifacts(Artifact)
	 */
	public CollectRequest request(final Artifact artifact) {
		final CollectRequest crq = new CollectRequest();
		crq.setRoot(
				new Dependency(
						new DefaultArtifact(artifact.toString()),
						JavaScopes.COMPILE
				)
		);
		for (RemoteRepository repo : this.remotes) {
			crq.addRepository(repo);
		}
		return crq;
	}

	protected abstract Log getLog();

}
