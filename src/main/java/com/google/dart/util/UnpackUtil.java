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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

/**
 * @author Daniel Zwicker
 */
public class UnpackUtil {

	public static void unpack(final File file, final File location, final Log logger,
			final ArchiverManager archiverManager)
			throws NoSuchArchiverException, MojoExecutionException {

		logUnpack(file, location, logger);

		location.mkdirs();

		if (file.isDirectory())
		{
			// usual case is a future jar packaging, but there are special cases: classifier and other packaging
			throw new MojoExecutionException("Artifact has not been packaged yet. When used on reactor artifact, "
					+ "unpack should be executed after packaging: see MDEP-98.");
		}

		final UnArchiver unArchiver = archiverManager.getUnArchiver(file);
		unArchiver.setSourceFile(file);
		unArchiver.setDestDirectory(location);
		unArchiver.extract();

		logUnpackDone(file, location, logger);
	}

	private static void logUnpack(final File file, final File location, final Log logger)
	{
		if (!logger.isInfoEnabled())
		{
			return;
		}

		final StringBuffer msg = new StringBuffer();
		msg.append("Unpacking ");
		msg.append(file);
		msg.append(" to ");
		msg.append(location);

		logger.info(msg.toString());
	}

	private static void logUnpackDone(final File file, final File location, final Log logger)
	{
		if (!logger.isInfoEnabled())
		{
			return;
		}

		final StringBuffer msg = new StringBuffer();
		msg.append("Unpacked ");
		msg.append(file);
		msg.append(" to ");
		msg.append(location);

		logger.info(msg.toString());
	}

}
