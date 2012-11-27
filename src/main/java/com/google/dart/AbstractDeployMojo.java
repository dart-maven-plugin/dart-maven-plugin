package com.google.dart;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.deployment.DeployRequest;
import org.sonatype.aether.deployment.DeploymentException;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.RemoteRepository;

/**
 * @version $Id$
 */
public abstract class AbstractDeployMojo
		extends AbstractMojo {

	private static final Pattern ALT_REPO_SYNTAX_PATTERN = Pattern.compile("(.+)::(.+)::(.+)");

	/**
	 */
	@Component
	private MavenProject project;

	/**
	 * The entry point to Aether, i.e. the component
	 * doing all the work.
	 */
	@Component
	protected RepositorySystem system;

	/**
	 * The current repository/network configuration of Maven.
	 */
	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	protected RepositorySystemSession session;

	/**
	 * The project's remote repositories to use for the resolution
	 * of plugins and their dependencies.
	 */
	@Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
	protected List<RemoteRepository> remotes;

	/**
	 * Flag whether Maven is currently in online/offline mode.
	 */
	@Parameter(defaultValue = "${settings.offline}", readonly = true)
	private boolean offline;

	/**
	 * Parameter used to control how many times a failed deployment will be retried before giving up and failing.
	 * If a value outside the range 1-10 is specified it will be pulled to the nearest value within the range 1-10.
	 *
	 * @since 2.7
	 */
	@Parameter(property = "retryFailedDeploymentCount", defaultValue = "1")
	private int retryFailedDeploymentCount;

	/* Setters and Getters */

	void failIfOffline()
			throws MojoFailureException {
		if (offline) {
			throw new MojoFailureException("Cannot deploy artifacts when Maven is in offline mode");
		}
	}

	/**
	 * Deploy an artifact from a particular file.
	 *
	 * @param artifact the artifact definition
	 */
	protected void deploy(Artifact artifact)
			throws DeploymentException, MojoExecutionException, MojoFailureException {

		failIfOffline();

		int retryFailedDeploymentCount = Math.max(1, Math.min(10, this.retryFailedDeploymentCount));
		DeploymentException exception = null;
		for (int count = 0; count < retryFailedDeploymentCount; count++) {
			try {
				if (count > 0) {
					getLog().info(
							"Retrying deployment attempt " + (count + 1) + " of " + retryFailedDeploymentCount);
				}
				final DeployRequest request = deployRequest(artifact);
				system.deploy(session, request);
				exception = null;
				break;
			} catch (DeploymentException e) {
				if (count + 1 < retryFailedDeploymentCount) {
					getLog().warn("Encountered issue during deployment: " + e.getLocalizedMessage());
					getLog().debug(e);
				}
				if (exception == null) {
					exception = e;
				}
			}
		}
		if (exception != null) {
			throw exception;
		}
	}

	private DeployRequest deployRequest(final Artifact artifact) throws MojoExecutionException, MojoFailureException {
		DeployRequest request = new DeployRequest();
		request.addArtifact(artifact);
		request.setRepository(getDeploymentRepository());
		return request;
	}

	protected RemoteRepository getDeploymentRepository()
			throws MojoExecutionException, MojoFailureException {

		DistributionManagement distributionManagement = project.getDistributionManagement();
		if (distributionManagement == null) {
			return distributionManagementMissing();
		}

		DeploymentRepository repo = distributionManagement.getRepository();

		if (repo == null) {
			distributionManagementMissing();
		}

		RemoteRepository remoteRepository = new RemoteRepository(repo.getId(), repo.getLayout(), repo.getUrl());
		final RepositoryPolicy snapshots = repo.getSnapshots();
		if (snapshots != null) {
			org.sonatype.aether.repository.RepositoryPolicy aetherSnapshots =
					new org.sonatype.aether.repository.RepositoryPolicy();
			aetherSnapshots.setEnabled(snapshots.isEnabled());
			aetherSnapshots.setUpdatePolicy(snapshots.getUpdatePolicy());
			aetherSnapshots.setChecksumPolicy(snapshots.getChecksumPolicy());
			remoteRepository.setPolicy(true, aetherSnapshots);
		}

		final RepositoryPolicy releases = repo.getReleases();
		if (releases != null) {
			org.sonatype.aether.repository.RepositoryPolicy aetherReleases =
					new org.sonatype.aether.repository.RepositoryPolicy();
			aetherReleases.setEnabled(releases.isEnabled());
			aetherReleases.setUpdatePolicy(releases.getUpdatePolicy());
			aetherReleases.setChecksumPolicy(releases.getChecksumPolicy());
			remoteRepository.setPolicy(false, aetherReleases);
		}
		Authentication authentication = session.getAuthenticationSelector().getAuthentication(remoteRepository);
		remoteRepository.setAuthentication(authentication);

		return remoteRepository;
	}

	private RemoteRepository distributionManagementMissing() throws MojoExecutionException {
		String msg = "Deployment failed: repository element was not specified in the POM inside"
				+ " distributionManagement element or in -DaltDeploymentRepository=id::layout::url parameter";

		throw new MojoExecutionException(msg);
	}
}