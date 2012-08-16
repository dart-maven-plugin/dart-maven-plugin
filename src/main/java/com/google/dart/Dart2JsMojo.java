package com.google.dart;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

import com.google.dart.util.DartVmUtil;

/**
 * Goal which compile dart files to javascript.
 * 
 */
@Mojo(name = "dart2js", defaultPhase = LifecyclePhase.COMPILE)
public class Dart2JsMojo
		extends AbstractMojo
{

	private final static String		ARGUMENT_CECKED_MODE	= "-c";

	private final static String		ARGUMENT_OUTPUT_FILE	= "-o";

	private static final String[]	EMPTY_STRING_ARRAY		= {};

	/**
	 * The source directories containing the dart sources to be compiled.
	 * 
	 * @since 1.0
	 */
	@Parameter(defaultValue = "${basedir}/src/main/dart", readonly = true, required = true)
	private List<String>			compileSourceRoots;

	/**
	 * Skip the execution of dart2js.
	 * 
	 * @since 1.0
	 */
	@Parameter(defaultValue = "false", property = "dart.skip")
	private boolean					skip;

	/**
	 * The directory to place the js files after compiling.
	 * 
	 * @since 1.0
	 */
	@Parameter(defaultValue = "${project.build.directory}/dart", required = true)
	private File					outputDirectory;

	/**
	 * A list of inclusion filters for the dart2js compiler.
	 * 
	 * @since 1.0
	 */
	@Parameter(defaultValue = "**/*")
	private final Set<String>		includes				= new HashSet<String>();

	/**
	 * A list of exclusion filters for the dart2js compiler.
	 * 
	 * @since 1.0
	 */
	@Parameter
	private final Set<String>		excludes				= new HashSet<String>();

	/**
	 * Insert runtime type checks and enable assertions (checked mode).
	 * 
	 * @since 1.0
	 */
	@Parameter(defaultValue = "false", property = "dart.checkedMode")
	private boolean					checkedMode;

	/**
	 * provide a dart2js executable
	 */
	@Parameter
	private File					executable;

	/**
	 * The directory for downloading the dart SDK.
	 * 
	 * @since 1.0
	 */
	@Parameter(defaultValue = "${project.build.directory}/dependency/dart", required = true)
	private File					dartOutputDirectory;

	/**
	 * The Version of the dart SDK
	 * 
	 * @since 1.0
	 */
	@Parameter(defaultValue = "latest", required = true)
	private String					dartVersion;

	/**
	 * Sets the granularity in milliseconds of the last modification
	 * date for testing whether a dart source needs recompilation.
	 */
	@Parameter(property = "lastModGranularityMs", defaultValue = "0")
	private int						staleMillis;

	/**
	 * settings.xml's server id for the URL.
	 * This is used when wagon needs extra authentication information.
	 * 
	 */
	@Parameter(defaultValue = "serverId", required = true)
	private String					serverId;

	/**
	 * The base URL for Downloading the dart SDK from
	 * 
	 */
	@Parameter(defaultValue = "https://gsdview.appspot.com/dart-editor-archive-integration", required = true)
	private String					dartServerUrl;

	// ----------------------------------------------------------------------
	// Read-only parameters
	// ----------------------------------------------------------------------

	/**
	 * The directory to run the compiler from if fork is true.
	 */
	@Parameter(defaultValue = "${basedir}", required = true, readonly = true)
	private File					basedir;

	/**
	 * The current user system settings for use in Maven.
	 */
	@Parameter(defaultValue = "${settings}", readonly = true)
	protected Settings				settings;

	@Component
	protected WagonManager			wagonManager;

	/**
	 * To look up Archiver/UnArchiver implementations
	 */
	@Component
	protected ArchiverManager		archiverManager;

	protected File getOutputDirectory()
	{
		return outputDirectory;
	}

	protected boolean isCheckedMode() {
		return checkedMode;
	}

	protected List<String> getCompileSourceRoots()
	{
		return compileSourceRoots;
	}

	/**
	 * @todo also in ant plugin. This should be resolved at some point so that it does not need to
	 *       be calculated continuously - or should the plugins accept empty source roots as is?
	 */
	private static List<String> removeEmptyCompileSourceRoots(final List<String> compileSourceRootsList)
	{
		final List<String> newCompileSourceRootsList = new ArrayList<String>();
		if (compileSourceRootsList != null)
		{
			// copy as I may be modifying it
			for (final String srcDir : compileSourceRootsList)
			{
				if (!newCompileSourceRootsList.contains(srcDir) && new File(srcDir).exists())
				{
					newCompileSourceRootsList.add(srcDir);
				}
			}
		}
		return newCompileSourceRootsList;
	}

	/**
	 * Check if the execution should be skipped
	 * 
	 * @return true to skip
	 */
	protected boolean isSkip()
	{
		return skip;
	}

	public void execute()
			throws MojoExecutionException
	{
		if (isSkip())
		{
			getLog().info("skipping execute as per configuraion");
			return;
		}

		String compilerPath = null;
		try {
			compilerPath = new DartVmUtil(executable, dartOutputDirectory, dartVersion, dartServerUrl, settings,
					getLog(),
					serverId, wagonManager, archiverManager)
					.generateExecFilePath();
		} catch (final Exception e) {
			throw new MojoExecutionException("Unable to download dart vm", e);
		}

		getLog().debug("Using compiler '" + compilerPath + "'.");

		final List<String> compileSourceRoots = removeEmptyCompileSourceRoots(getCompileSourceRoots());

		if (compileSourceRoots.isEmpty())
		{
			getLog().info("No sources to compile");

			return;
		}

		if (getLog().isDebugEnabled())
		{
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

		final Set<File> staleDartSources =
				computeStaleSources(getSourceInclusionScanner(staleMillis));

		if (getLog().isDebugEnabled())
		{
			getLog().debug("Source roots:");

			for (final String root : getCompileSourceRoots())
			{
				getLog().debug(" " + root);
			}

			getLog().debug("staleMillis: " + staleMillis);
			getLog().debug("basedir: " + basedir);
			getLog().debug("outputDirectory: " + outputDirectory);

			getLog().debug("Source includes:");

			for (final String include : includes)
			{
				getLog().debug(" " + include);
			}

			getLog().debug("Source excludes:");
			for (final String exclude : excludes)
			{
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
		final String baseDirAbsolutePath = basedir.getAbsolutePath();
		final String dartSourceFileRelativeToBasedir = dartSourceFileAbsolutePath.replace(baseDirAbsolutePath,
				"").replace(".dart", ".js");

		String dartOutputFileRelativeToBasedir = null;
		for (final String compileSourceRoot : compileSourceRoots) {
			final String compileSourceRootRelativeToBasedir = compileSourceRoot.replace(baseDirAbsolutePath, "");
			if (dartSourceFileRelativeToBasedir.startsWith(compileSourceRootRelativeToBasedir)) {
				dartOutputFileRelativeToBasedir = dartSourceFileRelativeToBasedir.replace(
						compileSourceRootRelativeToBasedir, "");
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
			throws MojoExecutionException
	{
		final SourceMapping mapping = new SuffixMapping("dart", "js");
		final File outputDirectory = getOutputDirectory();

		scanner.addSourceMapping(mapping);

		final Set<File> staleSources = new HashSet<File>();

		for (final String sourceRoot : getCompileSourceRoots())
		{
			final File rootFile = new File(sourceRoot);

			if (!rootFile.isDirectory())
			{
				continue;
			}

			try
			{
				staleSources.addAll(scanner.getIncludedSources(rootFile, outputDirectory));
			} catch (final InclusionScanException e)
			{
				throw new MojoExecutionException(
						"Error scanning source root: \'" + sourceRoot + "\' for stale files to recompile.", e);
			}
		}

		return staleSources;
	}

	private SourceInclusionScanner getSourceInclusionScanner(final int staleMillis)
	{
		SourceInclusionScanner scanner = null;

		if (includes.isEmpty() && excludes.isEmpty())
		{
			scanner = new StaleSourceScanner(staleMillis);
		}
		else
		{
			if (includes.isEmpty())
			{
				includes.add("**/*.java");
			}
			scanner = new StaleSourceScanner(staleMillis, includes, excludes);
		}

		return scanner;
	}

}
