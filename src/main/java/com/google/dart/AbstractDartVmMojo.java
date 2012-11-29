package com.google.dart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.deployment.DeploymentException;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import com.google.dart.json.JSONException;
import com.google.dart.json.JSONObject;
import com.google.dart.json.JSONTokener;
import com.google.dart.markers.DefaultFileMarkerHandler;
import com.google.dart.util.DependencyUtil;
import com.google.dart.util.OsUtil;
import com.google.dart.util.WagonUtils;

public abstract class AbstractDartVmMojo extends AbstractDartMojo {

	/**
	 * Skip downloading dart VM.
	 *
	 * @since 1.0
	 */
	@Parameter(defaultValue = "false", property = "dart.skipVM")
	private boolean skipVM;

	/**
	 * provide a dart2js executable
	 *
	 * @since 1.0
	 */
	@Parameter
	private File executable;

	/**
	 * Strip artifact version during copy
	 *
	 * @since 1.0
	 */
	@Parameter(property = "dart.stripVersion", defaultValue = "false")
	protected boolean stripVersion = false;

	/**
	 * Default location used for mojo unless overridden in ArtifactItem
	 *
	 * @since 1.0
	 */
	@Parameter(defaultValue = "${project.build.directory}/dependency")
	protected File dependencyOutputDirectory;

	/**
	 * Place each artifact in the same directory layout as a default repository.
	 * <br/>example: /dependencyOutputDirectory/junit/junit/3.8.1/junit-3.8.1.jar
	 *
	 * @since 1.0.2
	 */
	@Parameter(property = "dart.useRepositoryLayout", defaultValue = "false")
	protected boolean useRepositoryLayout;

	/**
	 * Place each type of file in a separate subdirectory. (example
	 * /dependencyOutputDirectory/jars /dependencyOutputDirectory/wars etc)
	 *
	 * @since 1.0.2
	 */
	@Parameter(property = "dart.useSubDirectoryPerType", defaultValue = "false")
	protected boolean useSubDirectoryPerType;

	/**
	 * Place each file in a separate subdirectory. (example
	 * <code>/dependencyOutputDirectory/junit-3.8.1-jar</code>)
	 *
	 * @since 1.0.2
	 */
	@Parameter(property = "dart.useSubDirectoryPerArtifact", defaultValue = "false")
	protected boolean useSubDirectoryPerArtifact;

	/**
	 * Directory to store flag files after unpack
	 *
	 * @since 1.0
	 */
	@Parameter(defaultValue = "${project.build.directory}/dependency-maven-plugin-markers")
	private File markersDirectory;

	/**
	 * The Version of the dart SDK
	 *
	 * @since 1.0
	 */
	@Parameter(defaultValue = "latest", required = true)
	private String dartVersion;

	/**
	 * settings.xml's server id for the URL.
	 * This is used when wagon needs extra authentication information.
	 *
	 * @since 1.0
	 */
	@Parameter(defaultValue = "serverId", required = true)
	private String serverId;

	/**
	 * The base URL for Downloading the dart SDK from
	 */
	@Parameter(defaultValue = "https://gsdview.appspot.com/dart-editor-archive-integration", required = true)
	private String dartServerUrl;

	@Component
	private WagonManager wagonManager;

	/**
	 */
	@Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
	private ArtifactRepository localRepository;

	/**
	 * To look up Archiver/UnArchiver implementations
	 */
	@Component
	private ArchiverManager archiverManager;

	protected String generateDartExecutable()
			throws RepositoryException, MojoExecutionException, NoSuchArchiverException, WagonException,
			FileNotFoundException, ParseException, JSONException, MojoFailureException {
		if (executable != null && executable.exists()) {
			checkDart2Js();
			final String execPath = executable.getAbsolutePath();
			getLog().info("Executable configured to " + execPath);
			return execPath;
		}

		if (getLog().isDebugEnabled()) {
			getLog().debug("Check for dartVM.");
		}

		Artifact dartVMArtifact = createArtifact();
		DefaultFileMarkerHandler handler = new DefaultFileMarkerHandler(dartVMArtifact, this.markersDirectory);

		final File destDir = DependencyUtil.getFormattedOutputDirectory(useSubDirectoryPerType,
				useSubDirectoryPerArtifact, useRepositoryLayout,
				stripVersion, dependencyOutputDirectory, dartVMArtifact);
		if (!handler.isMarkerSet()) {
			File zip;
			try {
				zip = resolveZip(dartVMArtifact);
			} catch (DependencyResolutionException e) {
				if (getLog().isDebugEnabled()) {
					getLog().debug("Unable to resolve dartVM in maven repositories.");
					getLog().debug(e);
				}
				downloadFromGoogle(dartVMArtifact);
				zip = resolveZip(dartVMArtifact);
			}

			unpack(zip, destDir);
			handler.setMarker();
		}

		executable = new File(destDir, "dart-sdk/bin/dart2js" + (OsUtil.isWindows() ? ".bat" : ""));

		checkDart2Js();
		return executable.getAbsolutePath();
	}

	private File resolveZip(final Artifact dartVMArtifact) throws RepositoryException {
		final File zip;
		Collection<File> zips = resolve(dartVMArtifact);
		if (zips.size() != 1) {
			throw new RepositoryException("Only one artifact for dart-sdk expected.");
		}
		zip = zips.iterator().next();
		return zip;
	}

	protected void unpack(final File file, final File location)
			throws NoSuchArchiverException, MojoExecutionException {

		logUnpack(file, location);

		location.mkdirs();

		if (file.isDirectory()) {
			// usual case is a future jar packaging, but there are special cases: classifier and other packaging
			throw new MojoExecutionException("Artifact has not been packaged yet. When used on reactor artifact, "
					+ "unpack should be executed after packaging: see MDEP-98.");
		}

		final UnArchiver unArchiver = archiverManager.getUnArchiver(file);
		unArchiver.setSourceFile(file);
		unArchiver.setDestDirectory(location);
		unArchiver.extract();

		logUnpackDone(file, location);
	}

	private void logUnpack(final File file, final File location) {
		if (!getLog().isInfoEnabled()) {
			return;
		}

		final StringBuffer msg = new StringBuffer();
		msg.append("Unpacking ");
		msg.append(file);
		msg.append(" to ");
		msg.append(location);

		getLog().info(msg.toString());
	}

	private void logUnpackDone(final File file, final File location) {
		if (!getLog().isInfoEnabled()) {
			return;
		}

		final StringBuffer msg = new StringBuffer();
		msg.append("Unpacked ");
		msg.append(file);
		msg.append(" to ");
		msg.append(location);

		getLog().info(msg.toString());
	}

	private void downloadFromGoogle(final Artifact dartVMArtifact)
			throws FileNotFoundException, JSONException, WagonException, DeploymentException, MojoExecutionException,
			MojoFailureException, ParseException {
		final File dartVersionFile = new File(dependencyOutputDirectory, "VERSION");
		if (dartVersionFile.exists()) {

			if (!skipVM) {

				final JSONObject dartVersionInformation = readDartVersionJson(dartVersionFile);
				if (dartVersion.equals("latest")) {
					deployDartToRepository(dartVMArtifact);
				} else {
					final long dartVersionPresent = readDartVersion(dartVersionInformation);
					if (dartVersionPresent != Long.parseLong(dartVersion)) {
						deployDartToRepository(dartVMArtifact);
					}
				}
			}
		} else {
			if (!dependencyOutputDirectory.exists()) {
				dependencyOutputDirectory.mkdirs();
			}
			deployDartToRepository(dartVMArtifact);
		}
	}

	private void deployDartToRepository(Artifact artifact)
			throws WagonException, MojoExecutionException, MojoFailureException, DeploymentException {

		String dartSDKFileName = "dartsdk-" + classifier() + ".zip";

		final String url = dartServerUrl + "/" + dartVersion;

		final Wagon wagon = WagonUtils.createWagon(serverId, url, wagonManager, getSettings(), getLog());
		getLog().info("Download dart SDK  " + url + "/" + dartSDKFileName);
		final File latestDartSDKFile = new File(dependencyOutputDirectory, dartSDKFileName);
		wagon.get(dartSDKFileName, latestDartSDKFile);
		getLog().info("Downloaded dart SDK  " + url + "/" + dartSDKFileName);

		artifact = artifact.setFile(latestDartSDKFile);

		deploy(artifact);

	}

	private File downloadVersionInformation(final String postFix) throws WagonException,
			TransferFailedException, ResourceDoesNotExistException,
			AuthorizationException {
		final String url = dartServerUrl + "/latest";
		final Wagon wagon = WagonUtils.createWagon(serverId, url, wagonManager, getSettings(), getLog());
		final File latestDartVersionFile = new File(dependencyOutputDirectory, "VERSION" + postFix);
		wagon.get("VERSION", latestDartVersionFile);
		return latestDartVersionFile;
	}

	private long readDartVersion(final JSONObject dartVersionInformation) throws JSONException, ParseException {
		final long dartVersion = dartVersionInformation.getLong("revision");
		return dartVersion;
	}

	private Date readDartVersionDate(final JSONObject dartVersionInformation) throws JSONException, ParseException {
		final String dateString = dartVersionInformation.getString("date");
		final Date date = new SimpleDateFormat("yyyymmddhhss").parse(dateString);
		return date;
	}

	private JSONObject readDartVersionJson(final File dartVersionFile) throws FileNotFoundException, JSONException {
		final InputStream dartVersionInputStream = new FileInputStream(dartVersionFile);
		final JSONTokener dartVersionTokener = new JSONTokener(dartVersionInputStream);
		final JSONObject dartVersionJSON = new JSONObject(dartVersionTokener);
		return dartVersionJSON;
	}

	private void checkDart2Js() {
		if (executable == null) {
			throw new NullPointerException("Dart2js required. Configuration erro for executable?");
		}
		if (!executable.isFile()) {
			throw new IllegalArgumentException("Dart2js required. Configuration erro for executable? executable="
					+ executable.getAbsolutePath());
		}
		if (!executable.canExecute()) {
			throw new IllegalArgumentException("Dart2js not executable! Configuration erro for executable? executable="
					+ executable.getAbsolutePath());
		}
	}

	private Artifact createArtifact() throws FileNotFoundException, ParseException, JSONException, WagonException {
		final String version = resolveVersion();
		return new DefaultArtifact("com.google.dart:dart-sdk:zip:" + classifier() + ":" + version);
	}

	private String resolveVersion() throws FileNotFoundException, JSONException, ParseException, WagonException {
		if (dartVersion != null && !dartVersion.isEmpty() && !dartVersion.equals("latest")) {
			return dartVersion;
		}

		downloadVersionInformation("");

		final File dartVersionFile = new File(dependencyOutputDirectory, "VERSION");
		final JSONObject dartVersionInformation = readDartVersionJson(dartVersionFile);
		long dartVersionDownloaded = readDartVersion(dartVersionInformation);

		return Long.toString(dartVersionDownloaded);
	}

	private String classifier() {
		String classifier = "";
		if (OsUtil.isWindows()) {
			classifier += "win32";
		} else if (OsUtil.isMac()) {
			classifier += "macos";
		} else if (OsUtil.isUnix()) {
			classifier += "linux";
		}

		if (OsUtil.isArch64()) {
			classifier += "-64";
		} else {
			classifier += "-32";
		}
		return classifier;
	}

	protected boolean isSkipVM() {
		return skipVM;
	}

	protected File getExecutable() {
		return executable;
	}

	protected String getDartVersion() {
		return dartVersion;
	}

	protected String getServerId() {
		return serverId;
	}

	protected String getDartServerUrl() {
		return dartServerUrl;
	}

	protected WagonManager getWagonManager() {
		return wagonManager;
	}

	protected ArchiverManager getArchiverManager() {
		return archiverManager;
	}
}
