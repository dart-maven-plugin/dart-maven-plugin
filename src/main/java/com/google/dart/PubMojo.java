package com.google.dart;

import java.io.File;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

import com.google.dart.util.OsUtil;

/**
 * Goal to invoke the dart pub package manager.
 *
 * @author Daniel Zwicker
 */
@Mojo(name = "pub")
public class PubMojo extends AbstractDartSDKMojo {

	private final static String COMMAND_INSTALL = "install";

	private final static String COMMAND_UPDATE = "update";

	/**
	 * Skip the execution of dart2js.
	 *
	 * @since 1.0.3
	 */
	@Parameter(defaultValue = "false", property = "dart.pub.update")
	private boolean update;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		final Set<File> dartPackageRoots = findDartPackageRoots();
		processPubDependencies(dartPackageRoots);
	}

	protected void processPubDependencies(final Set<File> dartPackageRoots) throws MojoExecutionException {

		String pubPath = null;
		try {
			checkAndDownloadDartSDK();
			pubPath = getPubExecutable().getAbsolutePath();
		} catch (final Exception e) {
			throw new MojoExecutionException("Unable to download dart-sdk", e);
		}

		if (getLog().isDebugEnabled()) {
			getLog().debug("Using pub '" + pubPath + "'.");
			getLog().debug("basedir: " + getBasedir());
		}

		final StreamConsumer output = new WriterStreamConsumer(new OutputStreamWriter(System.out));
		final StreamConsumer error = new WriterStreamConsumer(new OutputStreamWriter(System.err));

		final Commandline cl = new Commandline();
		cl.setExecutable(pubPath);

		cl.createArg().setValue(update ? COMMAND_UPDATE : COMMAND_INSTALL);

		if (getLog().isDebugEnabled()) {
			getLog().debug("Base pub command: " + cl.toString());
		}

		try {
			for (final File dartPackageRoot : dartPackageRoots) {
				getLog().info("Run pub for package root: " + relativePath(dartPackageRoot));
				cl.setWorkingDirectory(dartPackageRoot);
				if (getLog().isDebugEnabled()) {
					getLog().debug("Execute pub command: " + cl.toString());
				}
				final int returnCode = CommandLineUtils.executeCommandLine(cl, output, error);
				if (getLog().isDebugEnabled()) {
					getLog().debug("pub return code: " + returnCode);
				}
				if (returnCode != 0) {
					throw new MojoExecutionException("Pub returned error code " + returnCode);
				}
			}
		} catch (CommandLineException e) {
			throw new MojoExecutionException("Unable to execute pub", e);
		}

		getLog().info("");
	}

	protected Set<File> findDartPackageRoots() throws MojoExecutionException {
		final Set<File> dartPackageRoots = new HashSet<File>();
		for (final String compileSourceRoot : getCompileSourceRoots()) {
			final File directory = new File(compileSourceRoot);
			if (!directory.exists()) {
				throw new MojoExecutionException("Compiler-source-root '" + compileSourceRoot + "'  does not exist.");
			}
			if (!directory.isDirectory()) {
				throw new MojoExecutionException(
						"Compiler-source-root '" + compileSourceRoot + "'  must be a directory.");
			}
			if (!directory.canRead()) {
				throw new MojoExecutionException("Compiler-source-root '" + compileSourceRoot + "'  must be readable.");
			}
			if (!directory.canWrite()) {
				throw new MojoExecutionException("Compiler-source-root '" + compileSourceRoot + "'  must be writable.");
			}

			if (getLog().isDebugEnabled()) {
				getLog().debug("Check compile-source-root '" + compileSourceRoot + "' for dart packages.");
			}

			final Collection<File> pubSpecs =
					FileUtils.listFiles(directory, new NameFileFilter("pubspec.yaml"), DirectoryFileFilter.DIRECTORY);

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

	protected void logDartPackageRoots(final Set<File> dartPackageRoots) {
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

	protected String relativePath(final File absolutePath) {
		return absolutePath.getAbsolutePath().replace(getBasedir() + "/", "");
	}

	private File getPubExecutable() {
		return new File(getDartHome(), "dart-sdk/bin/pub" + (OsUtil.isWindows() ? ".bat" : ""));
	}

}
