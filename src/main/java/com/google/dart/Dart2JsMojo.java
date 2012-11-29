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
import java.io.OutputStreamWriter;
import java.util.*;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;
import org.codehaus.plexus.util.cli.*;

/**
 * Goal which compile dart files to javascript.
 *
 * @author Daniel Zwicker
 */
@Mojo(name = "dart2js", defaultPhase = LifecyclePhase.COMPILE)
public class Dart2JsMojo
		extends AbstractDartVmMojo {

	private final static String ARGUMENT_CECKED_MODE = "-c";

	/**
	 * Generate the output into <file>
	 *
	 * @since 1.0
	 */
	private final static String ARGUMENT_OUTPUT_FILE = "-o";

	/**
	 * Display verbose information.
	 *
	 * @since 1.0.3
	 */
	private final static String ARGUMENT_VERBOSE = "-v";

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
	 * The source directories containing the dart sources to be compiled.
	 * <p/>
	 * If not specified the default is 'src/main/dart'.
	 *
	 * @since 1.0
	 */
	@Parameter
	private List<String> compileSourceRoots = new ArrayList<String>();

	/**
	 * Skip the execution of dart2js.
	 *
	 * @since 1.0
	 */
	@Parameter(defaultValue = "false", property = "dart.skip")
	private boolean skip;

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
	 * If not specified the default is '**&#47;*.dart'
	 *
	 * @since 1.0
	 */
	@Parameter
	private Set<String> includes = new HashSet<String>();

	/**
	 * A list of exclusion filters for the dart2js compiler.
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

	public void execute()
			throws MojoExecutionException {
		if (isSkip()) {
			getLog().info("skipping execute as per configuraion");
			return;
		}

		String compilerPath = null;
		try {
			compilerPath = generateDartExecutable();
		} catch (final Exception e) {
			throw new MojoExecutionException("Unable to download dart vm", e);
		}

		getLog().debug("Using compiler '" + compilerPath + "'.");

		final List<String> compileSourceRoots = removeEmptyCompileSourceRoots(getCompileSourceRoots());

		if (compileSourceRoots.isEmpty()) {
			getLog().info("No sources to compile");

			return;
		}

		if (getLog().isDebugEnabled()) {
			getLog().debug("Source directories: " + compileSourceRoots.toString().replace(',', '\n'));
			getLog().debug("Output directory: " + getOutputDirectory());
		}

		final StreamConsumer output = new WriterStreamConsumer(new OutputStreamWriter(System.out));
		final StreamConsumer error = new WriterStreamConsumer(new OutputStreamWriter(System.err));

		final Commandline cl = new Commandline(compilerPath);

		final List<String> arguments = new ArrayList<String>();

		if (checkedMode) {
			arguments.add(ARGUMENT_CECKED_MODE);
		}

		if (verbose) {
			arguments.add(ARGUMENT_VERBOSE);
		}

		if (minify) {
			arguments.add(ARGUMENT_MINIFY);
		}

		if (suppressWarnings) {
			arguments.add(ARGUMENT_SUPPRESS_WARNINGS);
		}

		if (diagnosticColors) {
			arguments.add(ARGUMENT_DIAGNOSTIC_COLORS);
		}
		final Set<File> staleDartSources =
				computeStaleSources(getSourceInclusionScanner(staleMillis));

		if (getLog().isDebugEnabled()) {
			getLog().debug("Source roots:");

			for (final String root : getCompileSourceRoots()) {
				getLog().debug(" " + root);
			}

			getLog().debug("staleMillis: " + staleMillis);
			getLog().debug("basedir: " + getBasedir());
			getLog().debug("dependencyOutputDirectory: " + outputDirectory);

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

		for (final File dartSourceFile : staleDartSources) {
			try {
				final List<String> compilerArguments = new ArrayList<String>(arguments);
				final File dartOutputFile = createOutputFileArgument(compilerArguments, dartSourceFile);
				createDartfileArgument(compilerArguments, dartSourceFile);

				cl.clearArgs();
				cl.addArguments(compilerArguments.toArray(EMPTY_STRING_ARRAY));

				getLog().debug(cl.toString());
				if (!dartOutputFile.getParentFile().exists()) {
					getLog().debug("Create directory " + dartOutputFile.getParentFile().getAbsolutePath());
					dartOutputFile.getParentFile().mkdirs();
				}
				final int returnValue = CommandLineUtils.executeCommandLine(cl, output, error);
				getLog().debug("dart2js returncode: " + returnValue);
			} catch (final CommandLineException e) {
				getLog().debug("dart2js error: ", e);
			}
		}

		if (staleDartSources.isEmpty()) {
			getLog().info("Nothing to compile - all dart javascripts are up to date");
		} else {
			getLog().info(
					"Compiling " + staleDartSources.size() + " dart file" + (staleDartSources.size() == 1 ? "" : "s")
							+ " to " + outputDirectory.getAbsolutePath());
		}
		getLog().info("");
	}

	protected boolean isSkip() {
		return skip;
	}

	protected File getOutputDirectory() {
		return outputDirectory;
	}

	protected Set<String> getExcludes() {
		return excludes;
	}

	protected boolean isCheckedMode() {
		return checkedMode;
	}

	protected int getStaleMillis() {
		return staleMillis;
	}

	protected List<String> getCompileSourceRoots() {
		if (compileSourceRoots.isEmpty()) {
			return Collections.singletonList(getBasedir() + "/src/main/dart");
		}
		return compileSourceRoots;
	}

	/**
	 * @todo also in ant plugin. This should be resolved at some point so that it does not need to
	 * be calculated continuously - or should the plugins accept empty source roots as is?
	 */
	private static List<String> removeEmptyCompileSourceRoots(final List<String> compileSourceRootsList) {
		final List<String> newCompileSourceRootsList = new ArrayList<String>();
		if (compileSourceRootsList != null) {
			// copy as I may be modifying it
			for (final String srcDir : compileSourceRootsList) {
				if (!newCompileSourceRootsList.contains(srcDir) && new File(srcDir).exists()) {
					newCompileSourceRootsList.add(srcDir);
				}
			}
		}
		return newCompileSourceRootsList;
	}

	private void checkAndCreateOutputDirectory() throws MojoExecutionException {
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		} else if (!outputDirectory.isDirectory()) {
			throw new MojoExecutionException("Fatal error compiling dart to js. Outputdirectory is not a directory");
		}
	}

	private void createDartfileArgument(final List<String> compilerArguments, final File dartSourceFile) {
		final String dartSourceFileAbsolutePath = dartSourceFile.getAbsolutePath();
		compilerArguments.add(dartSourceFileAbsolutePath);
		getLog().debug("dartfile to compile: " + dartSourceFileAbsolutePath);
	}

	private File createOutputFileArgument(final List<String> compilerArguments, final File dartSourceFile) {
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

		getLog().debug(
				"dart2js compiles dart-file '" + dartSourceFileAbsolutePath + "' to outputdirectory '"
						+ dartOutputFile + "'");
		compilerArguments.add(ARGUMENT_OUTPUT_FILE + dartOutputFile);
		return new File(dartOutputFile);
	}

	private Set<File> computeStaleSources(final SourceInclusionScanner scanner)
			throws MojoExecutionException {
		final SourceMapping mapping = new SuffixMapping("dart", "dart.js");
		final File outputDirectory = getOutputDirectory();

		scanner.addSourceMapping(mapping);

		final Set<File> staleSources = new HashSet<File>();

		for (final String sourceRoot : getCompileSourceRoots()) {
			final File rootFile = new File(sourceRoot);

			if (!rootFile.isDirectory()) {
				continue;
			}

			try {
				staleSources.addAll(scanner.getIncludedSources(rootFile, outputDirectory));
			} catch (final InclusionScanException e) {
				throw new MojoExecutionException(
						"Error scanning source root: \'" + sourceRoot + "\' for stale files to recompile.", e);
			}
		}

		return staleSources;
	}

	private SourceInclusionScanner getSourceInclusionScanner(final int staleMillis) {
		return new StaleSourceScanner(staleMillis, getIncludes(), getExcludes());
	}

	public Set<String> getIncludes() {
		if (includes.isEmpty()) {
			return Collections.singleton("**/*.dart");
		}
		return includes;
	}
}
