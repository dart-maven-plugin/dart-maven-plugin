package com.google.dart;

import java.io.File;
import java.io.OutputStreamWriter;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.cli.*;

import com.google.dart.util.OsUtil;

@Mojo(name = "pub")
public class PubMojo extends AbstractDartSDKMojo {

	private final static String COMMAND_INSTALL = "install";

	private final static String COMMAND_UPDATE = "update";

	/**
	 * Skip the execution of dart2js.
	 *
	 * @since 1.0.3
	 */
	@Parameter(defaultValue = "false", property = "dart.pub.skip")
	private boolean skip;

	/**
	 * Skip the execution of dart2js.
	 *
	 * @since 1.0.3
	 */
	@Parameter(defaultValue = "false", property = "dart.pub.update")
	private boolean update;

	/**
	 * The directory to place the packages directory in.
	 * <p/>
	 * If not specified the default is 'target/dependency/packages'.
	 *
	 * @since 1.0
	 */
	@Parameter(defaultValue = "${basedir}/packages", required = true,
			property = "dart.pub.pubOutputDirectory")
	//TODO pub does not support other location for pubspec.yaml will be supported in al later version
	//defaultValue = "${project.build.directory}/dependency/packages"
	private File pubOutputDirectory;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		processPubDependencies();
	}

	protected void processPubDependencies() throws MojoExecutionException {
		if (isSkip()) {
			getLog().info("skipping execute as per configuration");
			return;
		}

		String pubPath = null;
		try {
			checkAndDownloadDartSDK();
			pubPath = getPubExecutable().getAbsolutePath();
		} catch (final Exception e) {
			throw new MojoExecutionException("Unable to download dart vm", e);
		}

		if (getLog().isDebugEnabled()) {
			getLog().debug("Using pub '" + pubPath + "'.");
			getLog().debug("basedir: " + getBasedir());
			getLog().debug("pub output directory: " + pubOutputDirectory);
		}

		final StreamConsumer output = new WriterStreamConsumer(new OutputStreamWriter(System.out));
		final StreamConsumer error = new WriterStreamConsumer(new OutputStreamWriter(System.err));

		final Commandline cl = new Commandline(pubPath);

		//TODO pub does not support other location for pubspec.yaml will be supported in al later version
//		checkPubOutputDirectory();
//		cl.setWorkingDirectory(pubOutputDirectory);

		cl.createArg().setValue(update ? COMMAND_UPDATE : COMMAND_INSTALL);

		if (getLog().isDebugEnabled()) {
			getLog().debug(cl.toString());
		}

		try {
			final int returnValue = CommandLineUtils.executeCommandLine(cl, output, error);
			if (getLog().isDebugEnabled()) {
				getLog().debug("dart2js returncode: " + returnValue);
			}
			if (returnValue != 0) {
				throw new MojoExecutionException("Dart2Js returned error code " + returnValue);
			}
		} catch (CommandLineException e) {
			throw new MojoExecutionException("Unable to execute pub", e);
		}

		getLog().info("");
	}

	//TODO pub does not support other location for pubspec.yaml will be supported in al later version
//	private void checkPubOutputDirectory() throws MojoExecutionException {
//		if (pubOutputDirectory.exists() && pubOutputDirectory.isFile()) {
//			throw new MojoExecutionException("Output directory for pub not a directory.");
//		} else if (!pubOutputDirectory.exists()) {
//			pubOutputDirectory.mkdirs();
//		}
//	}

	private File getPubExecutable() {
		return new File(getDartHome(), "dart-sdk/bin/pub" + (OsUtil.isWindows() ? ".bat" : ""));
	}

	protected boolean isSkip() {
		return skip;
	}
}
