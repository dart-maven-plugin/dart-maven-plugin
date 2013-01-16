package com.google.dart;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.resolution.DependencyResolutionException;

import com.google.dart.util.MavenResolver;

public abstract class AbstractDartMojo extends AbstractDeployMojo {

	// ----------------------------------------------------------------------
	// Read-only parameters
	// ----------------------------------------------------------------------

	/**
	 * The directory to run the compiler from if fork is true.
	 *
	 * @since 1.0
	 */
	@Parameter(defaultValue = "${basedir}", required = true, readonly = true)
	private File basedir;

	/**
	 * The current user system settings for use in Maven.
	 *
	 * @since 1.0
	 */
	@Parameter(defaultValue = "${settings}", readonly = true)
	private Settings settings;

	/**
	 * The source directories containing the dart sources to be compiled.
	 * <p/>
	 * If not specified the default is 'src/main/dart'.
	 *
	 * @since 1.0
	 */
	@Parameter
	private List<String> compileSourceRoots = new ArrayList<String>();

	protected Collection<File> resolve(final Artifact artifact) throws DependencyResolutionException {
		if (getLog().isDebugEnabled()) {
			getLog().debug("Resolve artifact '" + artifact + "'");
		}

		return resolver().resolve(artifact);
	}

	/**
	 * Construct a resolver.
	 *
	 * @return The resolver
	 * @see #execute()
	 */
	protected final MavenResolver resolver() {
		return new MavenResolver(this.system, this.session, this.remotes) {

			@Override
			protected Log getLog() {
				return AbstractDartMojo.this.getLog();
			}
		};
	}

	protected File getBasedir() {
		return basedir;
	}

	protected Settings getSettings() {
		return settings;
	}

	protected List<String> getCompileSourceRoots() {
		if (compileSourceRoots.isEmpty()) {
			
			final StringBuilder defaultPath = new StringBuilder(); // /src/main/dart or \src\main\dart
			defaultPath.append(File.separator);
			defaultPath.append("src");
			defaultPath.append(File.separator);
			defaultPath.append("main");
			defaultPath.append(File.separator);
			defaultPath.append("dart"); 
			
			return Collections.singletonList(getBasedir() + defaultPath.toString());
		}
		return compileSourceRoots;
	}
}
