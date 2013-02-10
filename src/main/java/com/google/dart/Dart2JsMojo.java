package com.google.dart;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.Arg;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

import com.google.common.collect.ImmutableSet;
import com.google.dart.util.OsUtil;

/**
 * Goal to compile dart files to javascript.
 *
 * @author Daniel Zwicker
 */
@Mojo(name = "dart2js", defaultPhase = LifecyclePhase.COMPILE)
public class Dart2JsMojo
		extends PubMojo {

	/**
	 * Where to find packages, that is, "package:..." imports.
	 *
	 * @since 2.0
	 */
	private final static String ARGUMENT_CECKED_MODE = "-c";

	/**
	 * Generate the output into <file>
	 *
	 * @since 1.0
	 */
	private final static String ARGUMENT_OUTPUT_FILE = "-o";

	/**
	 * Where to find packages, that is, "package:..." imports.
	 *
	 * @since 2.0
	 */
	private final static String ARGUMENT_PACKAGE_PATH = "-p";

	/**
	 * Display verbose information.
	 *
	 * @since 1.0.3
	 */
	private final static String ARGUMENT_VERBOSE = "-v";

	/**
	 * Analyze all code. Without this option, the compiler only analyzes code that is reachable from [main].
	 * This option is useful for finding errors in libraries,
	 * but using it can result in bigger and slower output.
	 *
	 * @since 1.0.3
	 */
	private final static String ARGUMENT_ANALYSE_ALL = "--analyze-all";

	/**
	 * Generate minified output.
	 *
	 * @since 1.0.3
	 */
	private final static String ARGUMENT_MINIFY = "--minify";

	/**
	 * Do not display any warnings.
	 *
	 * @since 1.0.3
	 */
	private final static String ARGUMENT_SUPPRESS_WARNINGS = "--suppress-warnings";

	/**
	 * Add colors to diagnostic messages.
	 *
	 * @since 1.0.3
	 */
	private final static String ARGUMENT_DIAGNOSTIC_COLORS = "--enable-diagnostic-colors";

	private static final String[] EMPTY_STRING_ARRAY = {};

	/**
	 * Skip the execution of dart2js.
	 *
	 * @since 1.1
	 */
	@Parameter(defaultValue = "false", property = "dart.skip")
	private boolean skipDart2Js;

	/**
	 * Insert runtime type checks and enable assertions (checked mode).
	 *
	 * @since 1.0
	 */
	@Parameter(defaultValue = "false", property = "dart.checkedMode")
	private boolean checkedMode;

	/**
	 * Display verbose information.
	 *
	 * @since 1.0.3
	 */
	@Parameter(defaultValue = "false", property = "dart.verbose")
	private boolean verbose;

	/**
	 * Analyze all code. Without this option, the compiler only analyzes code that is reachable from [main].
	 * This option is useful for finding errors in libraries,
	 * but using it can result in bigger and slower output.
	 *
	 * @since 1.0.3
	 */
	@Parameter(defaultValue = "false", property = "dart.analyseAll")
	private boolean analyseAll;

	/**
	 * Generate minified output.
	 *
	 * @since 1.0.3
	 */
	@Parameter(defaultValue = "false", property = "dart.minify")
	private boolean minify;

	/**
	 * Do not display any warnings.
	 *
	 * @since 1.0.3
	 */
	@Parameter(defaultValue = "false", property = "dart.suppressWarnings")
	private boolean suppressWarnings;

	/**
	 * Add colors to diagnostic messages.
	 *
	 * @since 1.0.3
	 */
	@Parameter(defaultValue = "false", property = "dart.diagnosticColors")
	private boolean diagnosticColors;

	/**
	 * Where to find packages, that is, "package:..." imports.
	 *
	 * @since 2.0
	 */
	@Parameter(property = "dart.packagepath")
	private String packagePath;

	/**
	 * Force compilation of all files.
	 *
	 * @since 1.1
	 */
	@Parameter(defaultValue = "true", property = "dart.force")
	private boolean force;

	/**
	 * The directory to place the js files after compiling.
	 * <p/>
	 * If not specified the default is 'target/dart'.
	 *
	 * @since 1.0
	 */
	@Parameter(defaultValue = "${project.build.directory}/dart", required = true, property = "dart.outputDirectory")
	private File outputDirectory;

	/**
	 * A list of inclusion filters for the dart2js compiler.
	 * <p/>
	 * If not specified the default is 'web&#47;**&#47;*.dart'
	 *
	 * @since 1.0
	 */
	@Parameter
	private Set<String> includes = new HashSet<String>();

	/**
	 * A list of exclusion filters for the dart2js compiler.
	 * <p/>
	 * If not specified the default is 'web&#47;**&#47;packages&#47;**'
	 *
	 * @since 1.0
	 */
	@Parameter
	private final Set<String> excludes = new HashSet<String>();

	/**
	 * Sets the granularity in milliseconds of the last modification
	 * date for testing whether a dart source needs recompilation.
	 *
	 * @since 1.0
	 */
	@Parameter(property = "lastModGranularityMs", defaultValue = "0")
	private int staleMillis;

	/**
	 * Set this to 'true' to skip running dart's packagemanager pub.
	 *
	 * @since 2.0
	 */
	@Parameter(defaultValue = "true", property = "dart.pup.skip")
	private boolean skipPub;

	public void execute()
			throws MojoExecutionException {
		if (isSkipDart2Js()) {
			getLog().info("skipping dart2js execution");
			return;
		}

		final Set<File> dartPackageRoots = findDartPackageRoots();
		processPubDependencies(dartPackageRoots);
		processDart2Js(dartPackageRoots);
	}

	private void processDart2Js(final Set<File> dartPackageRoots) throws MojoExecutionException {

		final Commandline cl = createBaseCommandline();

		if (isForce()) {
			clearOutputDirectory();
		}

		final Set<File> staleDartSources =
				computeStaleSources(dartPackageRoots, getSourceInclusionScanner());

		if (getLog().isDebugEnabled()) {
			getLog().debug("staleMillis: " + staleMillis);
			getLog().debug("basedir: " + getBasedir());
			getLog().debug("outputDirectory: " + outputDirectory);

			getLog().debug("Source includes:");

			for (final String include : getIncludes()) {
				getLog().debug(" " + include);
			}

			getLog().debug("Source excludes:");
			for (final String exclude : getExcludes()) {
				getLog().debug(" " + exclude);
			}
		}

		checkAndCreateOutputDirectory();

		final Arg outPutFileArg = cl.createArg();
		final Arg dartFileArg = cl.createArg();

		final StreamConsumer output = new WriterStreamConsumer(new OutputStreamWriter(System.out));
		final StreamConsumer error = new WriterStreamConsumer(new OutputStreamWriter(System.err));

		System.out.println();
		System.out.println();

		for (final File dartSourceFile : staleDartSources) {
			try {
				final File dartOutputFile = createOutputFileArgument(outPutFileArg, dartSourceFile);
				createDartfileArgument(dartFileArg, dartSourceFile);

				if (getLog().isDebugEnabled()) {
					getLog().debug(cl.toString());
				}
				if (!dartOutputFile.getParentFile().exists()) {
					if (getLog().isDebugEnabled()) {
						getLog().debug("Create directory " + dartOutputFile.getParentFile().getAbsolutePath());
					}
					dartOutputFile.getParentFile().mkdirs();
				}

				if (getLog().isDebugEnabled()) {
					getLog().debug(cl.toString());
				}

				final int returnValue = CommandLineUtils.executeCommandLine(cl, output, error);

				if (getLog().isDebugEnabled()) {
					getLog().debug("dart2js return code: " + returnValue);
				}
				if (returnValue != 0) {
					throw new MojoExecutionException("Dart2Js returned error code " + returnValue);
				}

				System.out.println();
				System.out.println();

			} catch (final CommandLineException e) {
				getLog().debug("dart2js error: ", e);
			}
		}

		if (staleDartSources.isEmpty()) {
			getLog().info("Nothing to compile - all dart javascripts are up to date");
		} else {
			getLog().info(
					"Compiling " + staleDartSources.size() + " dart file" + (staleDartSources.size() == 1 ? ""
							: "s")
							+ " to " + outputDirectory.getAbsolutePath());
		}

		System.out.println();
		System.out.println();
	}

	private Commandline createBaseCommandline() throws MojoExecutionException {

		String dart2jsPath = null;
		checkDart2Js();
		dart2jsPath = getDart2JsExecutable().getAbsolutePath();

		if (getLog().isDebugEnabled()) {
			getLog().debug("Using dart2js '" + dart2jsPath + "'.");
		}

		if (getLog().isDebugEnabled()) {
			getLog().debug("Source directories: " + getCompileSourceRoots().toString().replace(',', '\n'));
			getLog().debug("Output directory: " + getOutputDirectory());
		}

		final Commandline cl = new Commandline();
		cl.setExecutable(dart2jsPath);

		if (isCheckedMode()) {
			cl.createArg().setValue(ARGUMENT_CECKED_MODE);
		}

		if (isVerbose()) {
			cl.createArg().setValue(ARGUMENT_VERBOSE);
		}

		if (isAnalyseAll()) {
			cl.createArg().setValue(ARGUMENT_ANALYSE_ALL);
		}

		if (isMinify()) {
			cl.createArg().setValue(ARGUMENT_MINIFY);
		}

		if (isSuppressWarnings()) {
			cl.createArg().setValue(ARGUMENT_SUPPRESS_WARNINGS);
		}

		if (isDiagnosticColors()) {
			cl.createArg().setValue(ARGUMENT_DIAGNOSTIC_COLORS);
		}

		if (isPackagePath()) {
			cl.createArg().setValue(ARGUMENT_PACKAGE_PATH + packagePath);
		}

		if (getLog().isDebugEnabled()) {
			getLog().debug("Base dart2js command: " + cl.toString());
		}

		return cl;
	}

	protected void checkDart2Js() {
		checkDartSdk();
		if (!getDart2JsExecutable().canExecute()) {
			throw new IllegalArgumentException("Dart2js not executable! Configuration error for dartSdk? dartSdk="
					+ getDartSdk().getAbsolutePath());
		}
	}

	protected File getDart2JsExecutable() {
		return new File(getDartSdk(), "bin/dart2js" + (OsUtil.isWindows() ? ".bat" : ""));
	}

	private void clearOutputDirectory() throws MojoExecutionException {
		try {
			if (outputDirectory.exists()) {
				FileUtils.cleanDirectory(outputDirectory);
				getLog().info("Cleared all compiled dart-files.");
			}
		} catch (IOException e) {
			getLog().debug("Unable to clear directory '" + outputDirectory.getAbsolutePath() + "'.", e);
			throw new MojoExecutionException("Unable to clear directory '"
					+ outputDirectory.getAbsolutePath() + "'.", e);
		}
	}

	private void checkAndCreateOutputDirectory() throws MojoExecutionException {
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		} else if (!outputDirectory.isDirectory()) {
			throw new MojoExecutionException(
					"Fatal error compiling dart to js. Outputdirectory is not a directory");
		}
	}

	private void createDartfileArgument(final Arg compilerArguments, final File dartSourceFile) {
		final String dartSourceFileAbsolutePath = dartSourceFile.getAbsolutePath();
		compilerArguments.setValue(dartSourceFileAbsolutePath);
		getLog().info("dart2js for '" + relativePath(dartSourceFile) + "'");
	}

	private File createOutputFileArgument(final Arg outPutFileArg, final File dartSourceFile) {
		final String dartSourceFileAbsolutePath = dartSourceFile.getAbsolutePath();
		final String baseDirAbsolutePath = getBasedir().getAbsolutePath();
		final String dartSourceFileRelativeToBasedir = dartSourceFileAbsolutePath.replace(baseDirAbsolutePath,
				"") + ".js";

		String dartOutputFileRelativeToBasedir = null;
		for (final String compileSourceRoot : getCompileSourceRoots()) {

			if (dartSourceFileAbsolutePath.startsWith(compileSourceRoot)) {
				dartOutputFileRelativeToBasedir = dartSourceFileAbsolutePath.replace(compileSourceRoot, "");
				dartOutputFileRelativeToBasedir += ".js";
				break;
			}

		}

		final String dartOutputFile = outputDirectory.getAbsolutePath() + dartOutputFileRelativeToBasedir;

		if (getLog().isDebugEnabled()) {
			getLog().debug(
					"dart2js compiles dart-file '" + dartSourceFileAbsolutePath + "' to outputdirectory '"
							+ dartOutputFile + "'");
		}
		outPutFileArg.setValue(ARGUMENT_OUTPUT_FILE + dartOutputFile);
		return new File(dartOutputFile);
	}

	private Set<File> computeStaleSources(final Set<File> packageRoots, final SourceInclusionScanner scanner)
			throws MojoExecutionException {
		final SourceMapping mapping = new SuffixMapping("dart", "dart.js");
		scanner.addSourceMapping(mapping);

		final Set<File> staleSources = new HashSet<File>();
		for (final File packageRoot : packageRoots) {
			try {
				final File packageOutputDirectory = getPackageOutputDirectory(packageRoot);
				staleSources.addAll(scanner.getIncludedSources(packageRoot, packageOutputDirectory));
			} catch (final InclusionScanException e) {
				throw new MojoExecutionException(
						"Error scanning source root: \'" + relativePath(packageRoot)
								+ "\' for stale files to recompile.", e);
			}
		}

		return staleSources;
	}

	private File getPackageOutputDirectory(final File packageRoot) {
		String packageRootOffset = packageRoot.getAbsolutePath();
		for (final String compileSourceRoot : getCompileSourceRoots()) {
			if (packageRootOffset.startsWith(compileSourceRoot)) {
				packageRootOffset = packageRootOffset.replace(compileSourceRoot + "/", "");
				break;
			}
		}
		return new File(getOutputDirectory(), packageRootOffset);
	}

	private SourceInclusionScanner getSourceInclusionScanner() {
		return new StaleSourceScanner(getStaleMillis(), getIncludes(), getExcludes());
	}

	public Set<String> getIncludes() {
		if (includes.isEmpty()) {
			return ImmutableSet.copyOf(Arrays.asList(new String[] {"web/**/*.dart"}));
		}
		return includes;
	}

	protected Set<String> getExcludes() {
		if (excludes.isEmpty()) {
			return ImmutableSet.copyOf(Arrays.asList(new String[] {"web/**/packages/**"}));
		}
		return excludes;
	}

	@Override
	public boolean isPubSkipped() {
		return skipPub;
	}

	protected boolean isSkipDart2Js() {
		return skipDart2Js;
	}

	protected File getOutputDirectory() {
		return outputDirectory;
	}

	protected boolean isCheckedMode() {
		return checkedMode;
	}

	protected int getStaleMillis() {
		return staleMillis;
	}

	protected boolean isVerbose() {
		return verbose;
	}

	protected boolean isAnalyseAll() {
		return analyseAll;
	}

	protected boolean isMinify() {
		return minify;
	}

	protected boolean isSuppressWarnings() {
		return suppressWarnings;
	}

	protected boolean isDiagnosticColors() {
		return diagnosticColors;
	}

	protected boolean isForce() {
		return force;
	}

	protected boolean isPackagePath() {
		return !StringUtils.isEmpty(packagePath);
	}
}
