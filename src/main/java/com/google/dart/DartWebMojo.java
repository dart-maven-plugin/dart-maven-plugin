package com.google.dart;

import com.google.common.base.Throwables;
import com.google.dart.util.Pub;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Set;

/**
 * Goal to invoke the dart web compiler.
 *
 * @author nigel magnay
 */
@Mojo(name = "dwc", defaultPhase = LifecyclePhase.COMPILE)
public class DartWebMojo extends DartMojo {

    private final static String ARGUMENT_OUT = "--out";

    @Parameter(property = "output")
    private String output;

    @Parameter(property = "htmlFile")
    private String htmlFile;

    @Parameter(property = "dwcScript", defaultValue = "packages/web_ui/dwc.dart")
    private String dwcScript;

    @Parameter(property = "packageName")
    private String packageName;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        final Set<File> dartPackageRoots = findDartPackageRoots();
        processPubDependencies(dartPackageRoots);

        File sourceDirectory;

        if( packageName != null ) {
            Map<String, Pub> packages = getDartPackagesByName();
            Pub pub = packages.get(packageName);

            if( pub == null ) {
                getLog().info("No package named " + packageName);
                for( String name : packages.keySet() ) {
                    getLog().info("  " + name);
                }
                throw new MojoFailureException("Missing Package");
            }

            sourceDirectory = pub.getPath();
        } else {
            sourceDirectory = getBasedir();
        }

        checkDart();
        String dartPath = getDartExecutable().getAbsolutePath();

        if (getLog().isDebugEnabled()) {
            getLog().debug("Using dart '" + dartPath + "'.");
        }

        final Commandline cl = new Commandline();
        cl.setExecutable(dartPath);


        cl.createArg().setValue(ARGUMENT_PACKAGE_PATH + new File(sourceDirectory,packagePath==null?"packages":packagePath).getAbsolutePath() + "/");


        File dwc = new File(sourceDirectory, dwcScript);
        if( !dwc.exists() )
            throw new MojoExecutionException("The dwc script does not exist here: " + dwc.getAbsolutePath());

        cl.createArg().setValue(dwc.getAbsolutePath());

        // Ensure the output location exists.

        File outputDir = new File(sourceDirectory, output);

        try {
            FileUtils.deleteDirectory(outputDir);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }

        outputDir.mkdirs();

        cl.createArg().setValue(ARGUMENT_OUT);
        cl.createArg().setValue(outputDir.getAbsolutePath());


        // Root HTML:
        File html = new File(sourceDirectory, htmlFile);
        if( !html.exists() )
            throw new MojoExecutionException("The HTML file does not exist here: " + html.getAbsolutePath());
        cl.createArg().setValue(html.getAbsolutePath());



        final StreamConsumer output = new WriterStreamConsumer(new OutputStreamWriter(System.out));
        final StreamConsumer error = new WriterStreamConsumer(new OutputStreamWriter(System.err));

        getLog().info("Execute dart: " + cl.toString());

        System.out.println();
        System.out.println();

        try {

            final int returnValue = CommandLineUtils.executeCommandLine(cl, output, error);

            if (getLog().isDebugEnabled()) {
                getLog().debug("dart return code: " + returnValue);
            }
            if (returnValue != 0) {
                throw new MojoExecutionException("Dart returned error code " + returnValue);
            }
        } catch (final CommandLineException e) {
            getLog().debug("dart error: ", e);
        }

        System.out.println();
        System.out.println();
    }

}
