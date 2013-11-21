package com.google.dart;

import com.google.common.collect.ImmutableSet;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.cli.Arg;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

import java.io.File;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Goal to invoke the dart tests.
 *
 * @author Daniel Zwicker
 */
@Mojo(name = "test", threadSafe = true, defaultPhase = LifecyclePhase.TEST)
public class TestMojo extends DartMojo {

    /**
     * A list of inclusion filters for the dart2js compiler.
     * <p/>
     * If not specified the default is 'test&#47;**&#47;*.dart'
     *
     * @since 2.0
     */
    @Parameter
    private Set<String> includes = new HashSet<>();

    /**
     * A list of exclusion filters for the dart2js compiler.
     * <p/>
     * If not specified the default is 'test&#47;**&#47;packages&#47;**'
     *
     * @since 2.0
     */
    @Parameter
    private final Set<String> excludes = new HashSet<>();

    /**
     * Set this to 'true' to skip running tests, but still compile them. Its use is NOT RECOMMENDED, but quite
     * convenient on occasion.
     *
     * @since 2.0
     */
    @Parameter(property = "skipTests")
    private boolean skipTests;

    /**
     * Set this to "true" to cause a failure if there are no tests to run.
     *
     * @since 2.0
     */
    @Parameter(property = "failIfNoTests", defaultValue = "false")
    private boolean failIfNoTests;

    /**
     * Set this to true to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite convenient on
     * occasion.
     *
     * @since 2.0
     */
    @Parameter(property = "maven.test.failure.ignore", defaultValue = "false")
    private boolean testFailureIgnore;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        final Set<File> dartPackageRoots = findDartPackageRoots();

        processPubDependencies(dartPackageRoots);

        if (isSkipTests()) {
            getLog().info("Tests are skipped.");
        } else {
            executeTests(dartPackageRoots);
        }
    }

    private void executeTests(final Set<File> dartPackageRoots) throws MojoExecutionException, MojoFailureException {

        final Commandline cl = createBaseCommandline();

        final Arg scriptArg = cl.createArg(true);

        final StreamConsumer output = new WriterStreamConsumer(new OutputStreamWriter(System.out));
        final StreamConsumer error = new WriterStreamConsumer(new OutputStreamWriter(System.err));

        final Set<File> testSources = computeTestToRun(dartPackageRoots);

        System.out.println();
        System.out.println();

        boolean fail = false;

        for (final File dartTestFile : testSources) {
            try {

                scriptArg.setValue(dartTestFile.getAbsolutePath());

                getLog().info("Execute test: " + dartTestFile.getAbsolutePath());

                if (getLog().isDebugEnabled()) {
                    getLog().debug("Execute test command: " + cl.toString());
                }

                final int returnValue = CommandLineUtils.executeCommandLine(cl, output, error);
                System.out.println();
                System.out.println();

                if (getLog().isDebugEnabled()) {
                    getLog().debug("test return code: " + returnValue);
                }
                if (returnValue != 0 && returnValue != 255) {
                    throw new MojoExecutionException("Test fail returned error code " + returnValue);
                } else if (returnValue == 255) {
                    fail = true;
                }

            } catch (final CommandLineException e) {
                getLog().error("error running tests: ", e);
                fail = true;
            }
        }

        reportExecution(testSources, fail);

        System.out.println();
        System.out.println();

    }

    private void reportExecution(final Set<File> testSources, final boolean fail) throws MojoFailureException {

        String msg;

        if (testSources.isEmpty()) {
            if (isFailIfNoTests()) {
                return;
            }
            // TODO: i18n
            throw new MojoFailureException(
                "No tests were executed!  (Set -DfailIfNoTests=false to ignore this error.)");
        }

        if (fail) {
            // TODO: i18n
            msg = "There are test failures.\n\nPlease refer to output for the individual test results.";

            if (isTestFailureIgnore()) {
                getLog().error(msg);
            } else {
                throw new MojoFailureException(msg);
            }
        }
    }

    public Set<String> getIncludes() {
        if (includes.isEmpty()) {
            return ImmutableSet.copyOf(Arrays.asList(new String[]{"test/**/*.dart"}));
        }
        return includes;
    }

    protected Set<String> getExcludes() {
        if (excludes.isEmpty()) {
            return ImmutableSet.copyOf(Arrays.asList(new String[]{"test/**/packages/**"}));
        }
        return excludes;
    }

    private Set<File> computeTestToRun(final Set<File> packageRoots)
        throws MojoExecutionException {

        final Set<File> testToRun = new HashSet<>();
        for (final File packageRoot : packageRoots) {
            testToRun.addAll(scanForTests(packageRoot, getIncludes(), getExcludes()));
        }

        return testToRun;
    }

    private Set<File> scanForTests(File sourceDir, Set<String> sourceIncludes, Set<String> sourceExcludes) {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setFollowSymlinks(true);
        ds.setBasedir(sourceDir);

        String[] includes;
        if (sourceIncludes.isEmpty()) {
            includes = new String[0];
        } else {
            includes = sourceIncludes.toArray(new String[sourceIncludes.size()]);
        }

        ds.setIncludes(includes);

        String[] excludes;
        if (sourceExcludes.isEmpty()) {
            excludes = new String[0];
        } else {
            excludes = sourceExcludes.toArray(new String[sourceExcludes.size()]);
        }

        ds.setExcludes(excludes);
        ds.addDefaultExcludes();

        ds.scan();

        String[] potentialSources = ds.getIncludedFiles();

        Set<File> matchingSources = new HashSet<>(potentialSources != null ? potentialSources.length : 0);

        if (potentialSources != null) {
            for (String potentialSource : potentialSources) {
                matchingSources.add(new File(sourceDir, potentialSource));
            }
        }

        return matchingSources;
    }

    public boolean isFailIfNoTests() {
        return failIfNoTests;
    }

    public boolean isTestFailureIgnore() {
        return testFailureIgnore;
    }

    public boolean isSkipTests() {
        return skipTests;
    }
}
