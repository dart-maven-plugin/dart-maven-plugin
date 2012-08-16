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

/**
 * Wagon configuration to scan for a set of remote files.
 */
public class WagonFileSet
{

	/**
	 * Path after the url, this is where the scan starts
	 */

	private String		directory			= "";

	/**
	 * Ant's excludes path expression
	 */
	private String[]	excludes;

	/**
	 * Ant's includes path expression
	 */
	private String[]	includes;

	/**
     * 
     */
	private boolean		caseSensitive;

	/**
	 * User default exclude sets
	 */
	private boolean		useDefaultExcludes	= true;

	/**
	 * Local path to download the remote resource ( tree ) to.
	 */
	private File		downloadDirectory;

	/**
	 * Relative of a remote URL when it used to copy files between 2 URLs.
	 */
	private String		outputDirectory		= "";

	// ////////////////////////////////////////////////////////////////////////////////////

	public String getDirectory()
	{
		return directory;
	}

	public void setDirectory(final String remotePath)
	{
		directory = remotePath;
	}

	public File getDownloadDirectory()
	{
		return downloadDirectory;
	}

	public void setDownloadDirectory(final File downloadDirectory)
	{
		this.downloadDirectory = downloadDirectory;
	}

	public String[] getExcludes()
	{
		return excludes;
	}

	public void setExcludes(final String[] excludes)
	{
		this.excludes = excludes;
	}

	public String[] getIncludes()
	{
		return includes;
	}

	public void setIncludes(final String[] includes)
	{
		this.includes = includes;
	}

	public boolean isCaseSensitive()
	{
		return caseSensitive;
	}

	public void setCaseSensitive(final boolean caseSensitive)
	{
		this.caseSensitive = caseSensitive;
	}

	/**
	 * Retrieves the included and excluded files from this file-set's directory.
	 * Specifically, <code>"file-set: <I>[directory]</I> (included:
	 * <I>[included files]</I>, excluded: <I>[excluded files]</I>)"</code>
	 * 
	 * @return The included and excluded files from this file-set's directory.
	 *         Specifically, <code>"file-set: <I>[directory]</I> (included:
	 * <I>[included files]</I>, excluded: <I>[excluded files]</I>)"</code>
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "file-set: " + getDirectory() + " (included: " + getIncludes() + ", excluded: " + getExcludes() + ")";
	}

	public boolean isUseDefaultExcludes()
	{
		return useDefaultExcludes;
	}

	public void setUseDefaultExcludes(final boolean useDefaultExcludes)
	{
		this.useDefaultExcludes = useDefaultExcludes;
	}

	public String getOutputDirectory()
	{
		return outputDirectory;
	}

	public void setOutputDirectory(final String outputDirectory)
	{
		this.outputDirectory = outputDirectory;
	}

}