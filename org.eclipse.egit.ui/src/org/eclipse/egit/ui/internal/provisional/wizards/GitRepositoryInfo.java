/*******************************************************************************
 * Copyright (c) 2011 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *    Sascha Scholz (SAP) - improvements
 *******************************************************************************/
package org.eclipse.egit.ui.internal.provisional.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.core.securestorage.UserPasswordCredentials;

/**
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as part
 * of a work in progress. There is no guarantee that this API will work or that
 * it will remain the same. Please do not use this API without consulting with
 * the egit team.
 * </p>
 *
 * Encapsulates info of a git repository
 */
public class GitRepositoryInfo {

	private final String cloneUri;
	private UserPasswordCredentials credentials;
	private String repositoryName;
	private List<String> fetchRefSpecs = new ArrayList<String>();

	/** */
	public static class PushInfo {
		/** */
		public String pushRefSpec;
		/** */
		public String pushUri;

		/**
		 * @param pushRefSpec
		 * @param pushUri
		 */
		public PushInfo(String pushRefSpec, String pushUri) {
			this.pushRefSpec = pushRefSpec;
			this.pushUri = pushUri;
		}
	}
	private List<PushInfo> pushInfos = new ArrayList<PushInfo>();

	/** */
	public static class RepositoryConfigProperty {
		/** */
		public String section;
		/** */
		public String subsection;
		/** */
		public String name;
		/** */
		public String value;

		/**
		 * @param section
		 * @param subsection
		 * @param name
		 * @param value
		 */
		public RepositoryConfigProperty(String section, String subsection, String name, String value) {
			this.section = section;
			this.subsection = subsection;
			this.name = name;
			this.value = value;
		}
	}
	private List<RepositoryConfigProperty> repositoryConfigProperties = new ArrayList<RepositoryConfigProperty>();


	/**
	 * @param cloneUri
	 *            the URI where the repository can be cloned from
	 */
	public GitRepositoryInfo(String cloneUri) {
		this.cloneUri = cloneUri;
	}

	/**
	 * @return the URI where the repository can be cloned from
	 */
	public String getCloneUri() {
		return cloneUri;
	}

	/**
	 * @param user
	 * @param password
	 */
	public void setCredentials(String user, String password) {
		credentials = new UserPasswordCredentials(user, password);
	}

	/**
	 * @return the credentials needed to log in
	 */
	public UserPasswordCredentials getCredentials() {
		return credentials;
	}

	/**
	 * @param repositoryName
	 *            the name of the git repository
	 */
	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}

	/**
	 * @return the name of the git repository
	 */
	public String getRepositoryName() {
		return repositoryName;
	}

	/**
	 * Adds a fetch specification to the cloned repository
	 * @param fetchRefSpec the fetch ref spec which will be added
	 */
	public void addFetchRefSpec(String fetchRefSpec) {
		this.fetchRefSpecs.add(fetchRefSpec);
	}

	/**
	 * @return the fetch ref specs
	 */
	public List<String> getFetchRefSpecs() {
		return fetchRefSpecs;
	}

	/**
	 * Adds a push information to the cloned repository
	 * @param pushRefSpec the push ref spec which will be added
	 * @param pushUri the push uri which will be added
	 */
	public void addPushInfo(String pushRefSpec, String pushUri) {
		this.pushInfos.add(new PushInfo(pushRefSpec, pushUri));
	}

	/**
	 * @return the push information
	 */
	public List<PushInfo> getPushInfos() {
		return pushInfos;
	}

	/**
	 * Add an entry in the configuration of the cloned repository
	 *
	 * @param section
	 * @param subsection
	 * @param name
	 * @param value
	 */
	public void addRepositoryConfigProperty(String section, String subsection, String name, String value) {
		repositoryConfigProperties.add(new RepositoryConfigProperty(section, subsection, name, value));
	}

	/**
	 * @return the repository config property entries
	 */
	public List<RepositoryConfigProperty> getRepositoryConfigProperties() {
		return repositoryConfigProperties;
	}

}
