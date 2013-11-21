package com.google.dart;

import com.google.dart.util.OsUtil;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

import java.io.File;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Set;

/**
 * Goal to invoke the dart pub package manager.
 *
 * @author Daniel Zwicker
 */
@Mojo(name = "pub", threadSafe = true)
public class PubMojo extends AbstractDartMojo {

    /**
     * The command pub should execute.
     *
     * @since 3.0.0
     */
    @Parameter(defaultValue = "get", property = "dart.pub.command")
    private String pubCommand;

    /**
     * Options to be passed to the pub command.
     *
     * @since 3.0.0
     */
    @Parameter
    private List<String> pubOptions;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Set<File> dartPackageRoots = findDartPackageRoots();
        processPubDependencies(dartPackageRoots);
    }

    protected void processPubDependencies(final Set<File> dartPackageRoots) throws MojoExecutionException {

        if (isPubSkipped()) {
            getLog().info("Updating dependencies (pub packagemanager) is skipped.");
            return;
        }
        String pubPath;
        checkPub();
        pubPath = getPubExecutable().getAbsolutePath();

        if (getLog().isDebugEnabled()) {
            getLog().debug("Using pub '" + pubPath + "'.");
            getLog().debug("basedir: " + getBasedir());
        }

        final StreamConsumer output = new WriterStreamConsumer(new OutputStreamWriter(System.out));
        final StreamConsumer error = new WriterStreamConsumer(new OutputStreamWriter(System.err));

        final Commandline cl = new Commandline();
        cl.setExecutable(pubPath);

        cl.createArg().setValue(pubCommand);

        if (pubOptions != null) {
            for (final String option : pubOptions) {
                cl.createArg().setValue(option);
            }
        }

        if (getLog().isDebugEnabled()) {
            getLog().debug("Base pub command: " + cl.toString());
        }

        System.out.println();
        System.out.println();

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

        System.out.println();
        System.out.println();
    }

    protected void checkPub() throws MojoExecutionException {
        checkDartSdk();
        if (!getPubExecutable().canExecute()) {
            throw new MojoExecutionException("Pub not executable! Configuration error for dartSdk? dartSdk="
                + getDartSdk().getAbsolutePath());
        }
    }

    private File getPubExecutable() {
        return new File(getDartSdk(), "bin/pub" + (OsUtil.isWindows() ? ".bat" : ""));
    }

    public boolean isPubSkipped() {
        return false;
    }
}
