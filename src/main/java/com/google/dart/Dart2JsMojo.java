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

import com.google.common.collect.ImmutableSet;
import com.google.dart.util.OsUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;
import org.codehaus.plexus.util.cli.Arg;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Goal to compile dart files to javascript.
 *
 * @author Daniel Zwicker
 */
@Mojo(name = "dart2js", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class Dart2JsMojo
    extends PubMojo {

    /**
     * Where to find packages, that is, "package:..." imports.
     *
     * @since 2.0
     */
    private final static String ARGUMENT_CHECKED_MODE = "-c";

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

    /**
     * Name of the global variable used by dart2js compiler in the generated code.
     * The name must match the regular expression "\$[a-z]*".
     *
     * @since 2.1.2
     */
    private final static String ARGUMENT_GLOBAL_JS_NAME = "--global-js-name=";

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
    private File packagePath;

    /**
     * Force compilation of all files.
     *
     * @since 2.0.2
     */
    @Parameter(defaultValue = "false", property = "dart.force")
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
    private Set<String> includes = new HashSet<>();

    /**
     * A list of exclusion filters for the dart2js compiler.
     * <p/>
     * If not specified the default is 'web&#47;**&#47;packages&#47;**'
     *
     * @since 1.0
     */
    @Parameter
    private final Set<String> excludes = new HashSet<>();

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
     * @since 2.0.1
     */
    @Parameter(defaultValue = "false", property = "dart.pup.skip")
    private boolean skipPub;

    /**
     * Name of the global variable used by dart2js compiler in the generated code.
     * The name must match the regular expression "\$[a-z]*".
     *
     * @since 2.1.2
     */
    @Parameter(defaultValue = "$", property = "dart.global.js.name")
    private String globalJsName;

    /**
     * The number of threads used to span dart2js instances.
     *
     * @since 3.0.0
     */
    @Parameter(defaultValue = "1", property = "dart.thread.count")
    private int threadCount;

    /**
     * The maximum time in ms all dart files should be compiled.
     *
     * @since 3.0.0
     */
    @Parameter(defaultValue = "0", property = "dart.thread.timeout")
    private int timeout;

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

        System.out.println();
        System.out.println();

        //threads
        if (staleDartSources.isEmpty()) {
            getLog().info("Nothing to compile - all dart javascripts are up to date");
        } else {

            checkDart2Js();
            final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            getLog().info("Run " + threadCount + " dart2js's in parallel.");
            getLog().info("Compile " + staleDartSources.size() + " dart files");

            final List<Future<List<String>>> logging = new ArrayList<>(staleDartSources.size());
            for (final File dartSourceFile : staleDartSources) {
                getLog().info("Queue " + dartSourceFile.getAbsolutePath() + " to compile.");
                logging.add(
                    executor.submit(new Callable<List<String>>() {
                        @Override
                        public List<String> call() throws Exception {

                            getLog().info("compile " + dartSourceFile.getAbsolutePath());
                            final List<String> messages = new ArrayList<>();
                            try {
                                final Commandline cl = createBaseCommandline(messages);
                                final Arg outPutFileArg = cl.createArg();
                                final Arg dartFileArg = cl.createArg();
                                final File dartOutputFile = createOutputFileArgument(messages, outPutFileArg, dartSourceFile);
                                createDartFileArgument(messages, dartFileArg, dartSourceFile);

                                if (getLog().isDebugEnabled()) {
                                    messages.add("debug#" + cl.toString());
                                }
                                if (!dartOutputFile.getParentFile().exists()) {
                                    if (getLog().isDebugEnabled()) {
                                        messages.add("debug#Create directory " + dartOutputFile.getParentFile().getAbsolutePath());
                                    }
                                    dartOutputFile.getParentFile().mkdirs();
                                }

                                if (getLog().isDebugEnabled()) {
                                    messages.add("debug#" + cl.toString());
                                }

                                final StringBuilderWriter writer = new StringBuilderWriter();
                                final StreamConsumer output = new WriterStreamConsumer(writer);

                                final int returnValue = CommandLineUtils.executeCommandLine(cl, output, output);

                                writer.flush();
                                writer.close();
                                final StringBuilder stringBuilder = writer.getBuilder();
                                messages.add("info#" + stringBuilder.toString());

                                if (getLog().isDebugEnabled()) {
                                    messages.add("debug#dart2js return code: " + returnValue);
                                }
                                if (returnValue != 0) {
                                    throw new MojoExecutionException("Dart2Js returned error code " + returnValue);
                                }

                                System.out.println();
                                System.out.println();

                            } catch (final CommandLineException e) {
                                messages.add("error#dart2js error: " + e.getMessage());
                                getLog().error("dart2js error", e);
                            }
                            getLog().info("done " + dartSourceFile.getAbsolutePath());
                            return messages;
                        }
                    })
                );
            }

            executor.shutdown();
            try {
                if (timeout > 0) {
                    executor.awaitTermination(timeout, TimeUnit.MILLISECONDS);
                }

                logResults(logging);
                getLog().info(
                    "Compiling " + staleDartSources.size() + " dart file" + (staleDartSources.size() == 1 ? ""
                        : "s")
                        + " to " + outputDirectory.getAbsolutePath());
            } catch (InterruptedException | TimeoutException e) {
                throw new MojoExecutionException("Unable to compile al dart files with in " + timeout + "ms. Perhaps increase it.", e);
            } catch (ExecutionException e) {
                throw new MojoExecutionException("Unable to compile al dart files.", e);
            }
        }

        System.out.println();
        System.out.println();
    }

    private void logResults(List<Future<List<String>>> logging) throws InterruptedException, ExecutionException, TimeoutException {
        for (final Future<List<String>> future : logging) {
            final List<String> messages = future.get(0, TimeUnit.MILLISECONDS);
            for (final String logMessage : messages) {
                final String[] m = logMessage.split("#");
                if (m.length > 1) {
                    final String level = m[0];
                    final String message = m[1];
                    switch (level) {
                        case "debug":
                            getLog().debug(message);
                            break;
                        case "info":
                            getLog().info(message);
                            break;
                        case "warn":
                            getLog().warn(message);
                            break;
                        case "error":
                            getLog().error(message);
                            break;
                    }
                } else {
                    // defaults to info
                    getLog().info(logMessage);
                }
            }
        }
    }

    private Commandline createBaseCommandline(List<String> messages) throws MojoExecutionException {
        final String dart2jsPath = getDart2JsExecutable().getAbsolutePath();

        if (getLog().isDebugEnabled()) {
            messages.add("debug#Using dart2js '" + dart2jsPath + "'.");
            messages.add("debug#Source directories: " + getCompileSourceRoots().toString().replace(',', '\n'));
            messages.add("debug#Output directory: " + getOutputDirectory());
        }

        final Commandline cl = new Commandline();
        cl.setExecutable(dart2jsPath);

        if (isCheckedMode()) {
            cl.createArg().setValue(ARGUMENT_CHECKED_MODE);
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
            cl.createArg().setValue(ARGUMENT_PACKAGE_PATH + packagePath.getAbsolutePath());
        }

        cl.createArg().setValue(ARGUMENT_GLOBAL_JS_NAME + globalJsName);

        if (getLog().isDebugEnabled()) {
            messages.add("debug#Base dart2js command: " + cl.toString());
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
                "Fatal error compiling dart to js. Output directory is not a directory");
        }
    }

    private void createDartFileArgument(List<String> messages, final Arg compilerArguments, final File dartSourceFile) {
        final String dartSourceFileAbsolutePath = dartSourceFile.getAbsolutePath();
        compilerArguments.setValue(dartSourceFileAbsolutePath);
        messages.add("info#dart2js for '" + relativePath(dartSourceFile) + "'");
    }

    private File createOutputFileArgument(List<String> messages, final Arg outPutFileArg, final File dartSourceFile)
        throws MojoExecutionException {
        final String dartSourceFileAbsolutePath = dartSourceFile.getAbsolutePath();

        String dartOutputFileRelativeToBasedir = null;
        for (final File compileSourceRoot : getCompileSourceRoots()) {
            final String compileSourceRootAsString = compileSourceRoot.getAbsolutePath();
            if (dartSourceFileAbsolutePath.startsWith(compileSourceRootAsString)) {
                dartOutputFileRelativeToBasedir = dartSourceFileAbsolutePath.replace(compileSourceRootAsString, "");
                dartOutputFileRelativeToBasedir += ".js";
                break;
            }

        }

        if (dartOutputFileRelativeToBasedir == null) {
            messages.add("error#Unable to find compilerSourceRoot for dart file '" + dartSourceFileAbsolutePath + "'");
            messages.add("error#compilerSourceRoots are:");
            for (final File compileSourceRoot : getCompileSourceRoots()) {
                getLog().error(compileSourceRoot.getAbsolutePath());
            }

            System.out.println("");
            System.out.println("");

            throw new MojoExecutionException("There is something wrong. ");
        }

        final String dartOutputFile = outputDirectory.getAbsolutePath() + dartOutputFileRelativeToBasedir;

        if (getLog().isDebugEnabled()) {
            messages.add("debug#dart2js compiles dart-file '" + dartSourceFileAbsolutePath + "' to outputdirectory '"
                + dartOutputFile + "'");
        }
        outPutFileArg.setValue(ARGUMENT_OUTPUT_FILE + dartOutputFile);
        return new File(dartOutputFile);
    }

    private Set<File> computeStaleSources(final Set<File> packageRoots, final SourceInclusionScanner scanner)
        throws MojoExecutionException {
        final SourceMapping mapping = new SuffixMapping("dart", "dart.js");
        scanner.addSourceMapping(mapping);

        final Set<File> staleSources = new HashSet<>();
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
        for (final File compileSourceRoot : getCompileSourceRoots()) {
            final String compileSourceRootAsString = compileSourceRoot.getAbsolutePath();
            if (packageRootOffset.startsWith(compileSourceRootAsString)) {
                packageRootOffset = packageRootOffset.replace(compileSourceRootAsString + "/", "");
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
            return ImmutableSet.copyOf(Arrays.asList(new String[]{"web/**/*.dart"}));
        }
        return includes;
    }

    protected Set<String> getExcludes() {
        if (excludes.isEmpty()) {
            return ImmutableSet.copyOf(Arrays.asList(new String[]{"web/**/packages/**"}));
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
        return packagePath != null;
    }
}
