package com.google.dart;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.google.dart.util.Pub;

public abstract class AbstractDartMojo extends AbstractMojo {

	/**
	 * The directory to run the compiler from if fork is true.
	 *
	 * @since 1.0
	 */
	@Parameter(defaultValue = "${basedir}", required = true, readonly = true)
	private File basedir;

	/**
	 * provide a dart-sdk
	 *
	 * @since 2.0.0
	 */
	@Parameter(required = true, defaultValue = "${env.DART_SDK}")
	private File dartSdk;

	/**
	 * The source directories containing the dart sources to be compiled.
	 * <p/>
	 * If not specified the default is 'src/main/dart'.
	 *
	 * @since 1.0
	 */
	@Parameter
	private List<File> compileSourceRoots = new ArrayList<>();

	// ----------------------------------------------------------------------
	// Read-only parameters
	// ----------------------------------------------------------------------

	protected void checkDartSdk() {

		if (getLog().isDebugEnabled()) {
			getLog().debug("Check for DART_SDK.");
		}

		if (dartSdk == null) {
			throw new NullPointerException("Dart-sdk required. Configuration error for dartSdk?");
		}
		if (!dartSdk.isDirectory()) {
			throw new IllegalArgumentException("Dart-sdk required. Configuration error for dartSdk? dartSdk="
					+ dartSdk.getAbsolutePath());
		}
		getLog().info("Dart-sdk configured to " + dartSdk);
		getLog().info("Version: " + readDartVersion());

	}

	protected File getDartSdk() {
		return dartSdk;
	}

	public File getBasedir() {
		return basedir;
	}

	protected List<File> getCompileSourceRoots() {
		if (compileSourceRoots.isEmpty()) {
			return Collections.singletonList(new File(getBasedir(), "/src/main/dart"));
		}
		return compileSourceRoots;
	}

	private String readDartVersion() {
		File dartVersionFile = new File(dartSdk, "version");
		if (!dartVersionFile.isFile()) {
			throw new IllegalArgumentException("Dart version file missing. Configuration error for dartSdk? dartSdk="
					+ dartSdk.getAbsolutePath());
		}
		try (BufferedReader in = new BufferedReader(new FileReader(dartVersionFile))) {
			final String dartVersion = in.readLine();
			if (StringUtils.isEmpty(dartVersion)) {
				throw new NullPointerException("Unable to read dart version. Configuration error for dartSdk?");
			}
			return dartVersion;
		} catch (IOException e) {
			throw new IllegalStateException("Unable to read dart version. Configuration error for dartSdk?", e);
		}
	}

	protected Map<String, Pub> getDartPackagesByName() throws MojoExecutionException {
		return Maps.uniqueIndex(
				Collections2.transform(findDartPackageRoots(), new Function<File, Pub>() {

					@Override
					public Pub apply(File file) {
						try {
							return new Pub(new File(file, "pubspec.yaml"));
						} catch (FileNotFoundException e) {
							throw Throwables.propagate(e);
						}
					}
				}), new Function<Pub, String>() {

			@Override
			public String apply(Pub pub) {
				return pub.getName();
			}
		});
	}

	protected Set<File> findDartPackageRoots() throws MojoExecutionException {
		final Set<File> dartPackageRoots = new HashSet<File>();
		for (final File compileSourceRoot : getCompileSourceRoots()) {
			if (!compileSourceRoot.exists()) {
				throw new MojoExecutionException("Compiler-source-root '" + compileSourceRoot + "'  does not exist.");
			}
			if (!compileSourceRoot.isDirectory()) {
				throw new MojoExecutionException(
						"Compiler-source-root '" + compileSourceRoot + "'  must be a directory.");
			}
			if (!compileSourceRoot.canRead()) {
				throw new MojoExecutionException("Compiler-source-root '" + compileSourceRoot + "'  must be readable.");
			}
			if (!compileSourceRoot.canWrite()) {
				throw new MojoExecutionException("Compiler-source-root '" + compileSourceRoot + "'  must be writable.");
			}

			if (getLog().isDebugEnabled()) {
				getLog().debug("Check compile-source-root '" + compileSourceRoot + "' for dart packages.");
			}

			final Collection<File> pubSpecs =
					FileUtils.listFiles(compileSourceRoot, new NameFileFilter("pubspec.yaml"),
							DirectoryFileFilter.DIRECTORY);

			if (getLog().isDebugEnabled()) {
				getLog().debug("");
				final StringBuilder builder = new StringBuilder();
				builder.append("Found pubspec.yaml in ");
				builder.append(compileSourceRoot);
				builder.append(":\n");
				for (final File pubSpec : pubSpecs) {
					builder.append("\t");
					builder.append(pubSpec.getAbsolutePath().replace(compileSourceRoot + "/", ""));
					builder.append("\n");
				}
				getLog().debug(builder.toString());
				getLog().debug("");
			}

			for (final File pubSpec : pubSpecs) {
				final File dartPackageRoot = pubSpec.getParentFile();
				dartPackageRoots.add(dartPackageRoot);
			}
		}
		logDartPackageRoots(dartPackageRoots);
		return dartPackageRoots;
	}

	protected String relativePath(final File absolutePath) {
		return absolutePath.getAbsolutePath().replace(getBasedir() + "/", "");
	}

	private void logDartPackageRoots(final Set<File> dartPackageRoots) {
		getLog().info("");
		final StringBuilder builder = new StringBuilder();
		builder.append("Found package roots:\n");
		for (final File dartPackageRoot : dartPackageRoots) {
			builder.append("\t");
			builder.append(relativePath(dartPackageRoot));
			builder.append("\n");
		}
		getLog().info(builder.toString());
		getLog().info("");
	}

}
