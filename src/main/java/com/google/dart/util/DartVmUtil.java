package com.google.dart.util;

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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.maven.artifact.manager.WagonConfigurationException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

import com.google.dart.json.JSONException;
import com.google.dart.json.JSONObject;
import com.google.dart.json.JSONTokener;

/**
 * @author Daniel Zwicker
 */
public class DartVmUtil {

	private File					executable;

	private final File				dartOutputDirectory;

	private final String			dartVersion;

	private final String			dartServerUrl;

	private final Settings			settings;

	private final Log				logger;

	private final String			serverId;

	private final WagonManager		wagonManager;

	private final ArchiverManager	archiverManager;

	private final boolean			skipVM;

	public DartVmUtil(final File executable, final File dartOutputDirectory, final String dartVersion,
			final String dartServerUrl, final Settings settings, final Log logger,
			final String serverId, final WagonManager wagonManager, final ArchiverManager archiverManager,
			final boolean skipVM) {
		super();
		this.executable = executable;
		this.dartOutputDirectory = dartOutputDirectory;
		this.dartVersion = dartVersion;
		this.dartServerUrl = dartServerUrl;
		this.settings = settings;
		this.logger = logger;
		this.serverId = serverId;
		this.wagonManager = wagonManager;
		this.archiverManager = archiverManager;
		this.skipVM = skipVM;
	}

	public String generateExecFilePath()
			throws FileNotFoundException, JSONException, ParseException, WagonConfigurationException,
			UnsupportedProtocolException, WagonException, MojoExecutionException, NoSuchArchiverException {
		if (executable == null) {
			checkOnlineDartVersionAndDownload();
		}
		checkDart2Js();
		return executable.getAbsolutePath();
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

	private void checkOnlineDartVersionAndDownload() throws FileNotFoundException, JSONException, ParseException,
			WagonConfigurationException, UnsupportedProtocolException, WagonException, MojoExecutionException,
			NoSuchArchiverException {
		final File dartVersionFile = new File(dartOutputDirectory, "VERSION");
		if (dartVersionFile.exists()) {

			if (!skipVM) {

				final JSONObject dartVersionInformation = readDartVersionJson(dartVersionFile);
				if (dartVersion.equals("latest")) {
					final Date dartVersionDatePresent = readDartVersionDate(dartVersionInformation);
					if (checkLatestVersion(dartVersionDatePresent)) {
						downloadDart();
					}
				} else {
					final long dartVersionPresent = readDartVersion(dartVersionInformation);
					if (dartVersionPresent != Long.parseLong(dartVersion)) {
						downloadDart();
					}
				}
			}
		} else {
			if (!dartOutputDirectory.exists()) {
				dartOutputDirectory.mkdirs();
			}
			downloadDart();
		}

		executable = new File(dartOutputDirectory, "dart-sdk/bin/dart2js" + (OsUtil.isWindows() ? ".bat" : ""));
	}

	private void downloadDart() throws WagonConfigurationException, UnsupportedProtocolException, WagonException,
			MojoExecutionException, NoSuchArchiverException {

		String dartSDKFileName = "dartsdk-";
		if (OsUtil.isWindows()) {
			dartSDKFileName += "win32";
		} else if (OsUtil.isMac()) {
			dartSDKFileName += "macos";
		} else if (OsUtil.isUnix()) {
			dartSDKFileName += "linux";
		}

		if (OsUtil.isArch64()) {
			dartSDKFileName += "-64.zip";
		} else {
			dartSDKFileName += "-32.zip";
		}

		final String url = dartServerUrl + "/" + dartVersion;

		final Wagon wagon = WagonUtils.createWagon(serverId, url, wagonManager, settings, logger);
		logger.info("Download dart SDK  " + url + "/" + dartSDKFileName);
		final File latestDartSDKFile = new File(dartOutputDirectory, dartSDKFileName);
		wagon.get(dartSDKFileName, latestDartSDKFile);
		logger.info("Downloaded dart SDK  " + url + "/" + dartSDKFileName);

		unpackDartSDK(latestDartSDKFile);

		downloadVersionInformation("");

	}

	private void unpackDartSDK(final File latestDartSDKFile) throws MojoExecutionException, NoSuchArchiverException {
		final File location = latestDartSDKFile.getParentFile();

		UnpackUtil.unpack(latestDartSDKFile, location, logger, archiverManager);
	}

	private boolean checkLatestVersion(final Date dartVersionDatePresent) throws WagonConfigurationException,
			UnsupportedProtocolException, WagonException, FileNotFoundException, JSONException, ParseException {
		final File latestDartVersionFile = downloadVersionInformation("-Check");
		final JSONObject latestDartVersionJSON = readDartVersionJson(latestDartVersionFile);
		final Date latestDartVersionDate = readDartVersionDate(latestDartVersionJSON);
		return latestDartVersionDate.after(dartVersionDatePresent);
	}

	private File downloadVersionInformation(final String postFix) throws WagonException, UnsupportedProtocolException,
			WagonConfigurationException, TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
		final String url = dartServerUrl + "/latest";
		final Wagon wagon = WagonUtils.createWagon(serverId, url, wagonManager, settings, logger);
		final File latestDartVersionFile = new File(dartOutputDirectory, "VERSION" + postFix);
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
}
