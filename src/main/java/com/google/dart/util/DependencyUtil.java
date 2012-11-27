package com.google.dart.util;

import java.io.*;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.aether.artifact.Artifact;

/**
 * Utility class with static helper methods
 */
public final class DependencyUtil {

	/**
	 * Builds the file name. If removeVersion is set, then the file name must be reconstructed from the artifactId,
	 * Classifier (if used) and Type. Otherwise, this method returns the artifact file name.
	 *
	 * @param artifact      File to be formatted.
	 * @param removeVersion Specifies if the version should be removed from the file name.
	 * @return Formatted file name in the format artifactId-[version]-[classifier].[type]
	 * @see {@link #getFormattedFileName(Artifact, boolean, boolean)}.
	 */
	public static String getFormattedFileName(Artifact artifact, boolean removeVersion) {
		return getFormattedFileName(artifact, removeVersion, false);
	}

	/**
	 * Builds the file name. If removeVersion is set, then the file name must be
	 * reconstructed from the groupId (if <b>prependGroupId</b> is true) artifactId,
	 * Classifier (if used) and Type.
	 * Otherwise, this method returns the artifact file name.
	 *
	 * @param artifact       File to be formatted.
	 * @param removeVersion  Specifies if the version should be removed from the file name.
	 * @param prependGroupId Specifies if the groupId should be prepended to the file name.
	 * @return Formatted file name in the format
	 *         [groupId].artifactId-[version]-[classifier].[type]
	 */
	public static String getFormattedFileName(Artifact artifact, boolean removeVersion, boolean prependGroupId) {
		return getFormattedFileName(artifact, removeVersion, prependGroupId, false);
	}

	/**
	 * Builds the file name. If removeVersion is set, then the file name must be
	 * reconstructed from the groupId (if <b>prependGroupId</b> is true) artifactId,
	 * Classifier (if used) and Type.
	 * Otherwise, this method returns the artifact file name.
	 *
	 * @param artifact       File to be formatted.
	 * @param removeVersion  Specifies if the version should be removed from the file name.
	 * @param prependGroupId Specifies if the groupId should be prepended to the file name.
	 * @param useBaseVersion Specifies if the baseVersion of the artifact should be used instead of the version.
	 * @return Formatted file name in the format
	 *         [groupId].artifactId-[version]-[classifier].[type]
	 */
	public static String getFormattedFileName(Artifact artifact, boolean removeVersion, boolean prependGroupId,
			boolean useBaseVersion) {
		StringBuilder destFileName = new StringBuilder();

		if (prependGroupId) {
			destFileName.append(artifact.getGroupId()).append(".");
		}

		String versionString = null;
		if (!removeVersion) {
			if (useBaseVersion) {
				versionString = "-" + artifact.getBaseVersion();
			} else {
				versionString = "-" + artifact.getVersion();
			}
		} else {
			versionString = "";
		}

		String classifierString = "";

		if (StringUtils.isNotEmpty(artifact.getClassifier())) {
			classifierString = "-" + artifact.getClassifier();
		}
		destFileName.append(artifact.getArtifactId()).append(versionString);
		destFileName.append(classifierString).append(".");
		destFileName.append(artifact.getExtension());

		return destFileName.toString();
	}

	/**
	 * Formats the dependencyOutputDirectory based on type.
	 *
	 * @param useSubdirsPerType    if a new sub directory should be used for each type.
	 * @param useSubdirPerArtifact if a new sub directory should be used for each artifact.
	 * @param useRepositoryLayout  if dependencies must be moved into a Maven repository layout, if set, other settings
	 *                             will be ignored.
	 * @param removeVersion        if the version must not be mentioned in the filename
	 * @param outputDirectory      base dependencyOutputDirectory.
	 * @param artifact             information about the artifact.
	 * @return a formatted File object to use for output.
	 */
	public static File getFormattedOutputDirectory(boolean useSubdirsPerType,
			boolean useSubdirPerArtifact, boolean useRepositoryLayout,
			boolean removeVersion, File outputDirectory, Artifact artifact) {
		StringBuilder sb = new StringBuilder(128);
		if (useRepositoryLayout) {
			// group id
			sb.append(artifact.getGroupId().replace('.', File.separatorChar)).append(File.separatorChar);
			// artifact id
			sb.append(artifact.getArtifactId()).append(File.separatorChar);
			// version
			sb.append(artifact.getBaseVersion()).append(File.separatorChar);
		} else {
			if (useSubdirsPerType) {
				sb.append(artifact.getExtension()).append("s").append(File.separatorChar);
			}
			if (useSubdirPerArtifact) {
				String artifactString = getDependencyId(artifact, removeVersion);
				sb.append(artifactString).append(File.separatorChar);
			}
		}
		return new File(outputDirectory, sb.toString());
	}

	private static String getDependencyId(Artifact artifact, boolean removeVersion) {
		StringBuilder sb = new StringBuilder();

		sb.append(artifact.getArtifactId());

		if (StringUtils.isNotEmpty(artifact.getClassifier())) {
			sb.append("-");
			sb.append(artifact.getClassifier());
		}

		if (!removeVersion) {
			sb.append("-");
			sb.append(artifact.getVersion());
			sb.append("-");
			sb.append(artifact.getExtension());
		} else {
			// if the classifier and type are the same (sources), then don't
			// repeat.
			// avoids names like foo-sources-sources
			if (!StringUtils.equals(artifact.getClassifier(), artifact.getExtension())) {
				sb.append("-");
				sb.append(artifact.getExtension());
			}
		}
		return sb.toString();
	}

	/**
	 * Writes the specified string to the specified file.
	 *
	 * @param string the string to write
	 * @param file   the file to write to
	 * @throws IOException if an I/O error occurs
	 */
	public static synchronized void write(String string, File file, boolean append, Log log)
			throws IOException {
		file.getParentFile().mkdirs();

		FileWriter writer = null;

		try {
			writer = new FileWriter(file, append);

			writer.write(string);
		} finally {
			IOUtil.close(writer);
		}
	}

	/**
	 * Writes the specified string to the log at info level.
	 *
	 * @param string the string to write
	 * @throws IOException if an I/O error occurs
	 */
	public static synchronized void log(String string, Log log)
			throws IOException {
		BufferedReader reader = new BufferedReader(new StringReader(string));

		String line;

		while ((line = reader.readLine()) != null) {
			log.info(line);
		}

		reader.close();
	}

	//
	// mainly used to parse excludes,includes configuration
	//
	public static String[] tokenizer(String str) {
		return StringUtils.split(cleanToBeTokenizedString(str), ",");
	}

	//
	// clean up configuration string before it can be tokenized
	//
	public static String cleanToBeTokenizedString(String str) {
		String ret = "";
		if (!StringUtils.isEmpty(str)) {
			// remove initial and ending spaces, plus all spaces next to commas
			ret = str.trim().replaceAll("[\\s]*,[\\s]*", ",");
		}

		return ret;
	}
}