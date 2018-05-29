/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow;

import static org.eclipse.egit.gitflow.GitFlowDefaults.DEVELOP;
import static org.eclipse.egit.gitflow.GitFlowDefaults.FEATURE_PREFIX;
import static org.eclipse.egit.gitflow.GitFlowDefaults.HOTFIX_PREFIX;
import static org.eclipse.egit.gitflow.GitFlowDefaults.MASTER;
import static org.eclipse.egit.gitflow.GitFlowDefaults.RELEASE_PREFIX;
import static org.eclipse.egit.gitflow.GitFlowDefaults.VERSION_TAG;

/**
 * Git flow branch names and prefixes.
 *
 * @since 4.1
 */
public final class InitParameters {
	/** */
	public String master = MASTER;

	/** */
	public String develop = DEVELOP;

	/** */
	public String feature = FEATURE_PREFIX;

	/** */
	public String release = RELEASE_PREFIX;

	/** */
	public String hotfix = HOTFIX_PREFIX;

	/** */
	public String versionTag = VERSION_TAG;

	/** */
	public final static String MASTER_BRANCH_PROPERTY = "master"; //$NON-NLS-1$

	/** */
	public final static String DEVELOP_BRANCH_PROPERTY = "develop"; //$NON-NLS-1$

	/** */
	public final static String FEATURE_BRANCH_PREFIX_PROPERTY = "feature"; //$NON-NLS-1$

	/** */
	public final static String RELEASE_BRANCH_PREFIX_PROPERTY = "release"; //$NON-NLS-1$

	/** */
	public final static String HOTFIX_BRANCH_PREFIX_PROPERTY = "hotfix"; //$NON-NLS-1$

	/** */
	public final static String VERSION_TAG_PROPERTY = "versionTag"; //$NON-NLS-1$

	/**
	 * @return parameter
	 */
	public String getMaster() {
		return master;
	}

	/**
	 * @param master
	 */
	public void setMaster(String master) {
		this.master = master;
	}

	/**
	 * @return parameter
	 */
	public String getDevelop() {
		return develop;
	}

	/**
	 * @param develop
	 */
	public void setDevelop(String develop) {
		this.develop = develop;
	}

	/**
	 * @return parameter
	 */
	public String getFeature() {
		return feature;
	}

	/**
	 * @param feature
	 */
	public void setFeature(String feature) {
		this.feature = feature;
	}

	/**
	 * @return parameter
	 */
	public String getRelease() {
		return release;
	}

	/**
	 * @param release
	 */
	public void setRelease(String release) {
		this.release = release;
	}

	/**
	 * @return parameter
	 */
	public String getHotfix() {
		return hotfix;
	}

	/**
	 * @param hotfix
	 */
	public void setHotfix(String hotfix) {
		this.hotfix = hotfix;
	}

	/**
	 * @return parameter
	 */
	public String getVersionTag() {
		return versionTag;
	}

	/**
	 * @param versionTag
	 */
	public void setVersionTag(String versionTag) {
		this.versionTag = versionTag;
	}

	/**
	 * @generated
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((develop == null) ? 0 : develop.hashCode());
		result = prime * result
				+ ((feature == null) ? 0 : feature.hashCode());
		result = prime * result
				+ ((hotfix == null) ? 0 : hotfix.hashCode());
		result = prime * result + ((master == null) ? 0 : master.hashCode());
		result = prime * result
				+ ((release == null) ? 0 : release.hashCode());
		result = prime * result
				+ ((versionTag == null) ? 0 : versionTag.hashCode());
		return result;
	}

	/**
	 * @generated
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InitParameters other = (InitParameters) obj;
		if (develop == null) {
			if (other.develop != null)
				return false;
		} else if (!develop.equals(other.develop))
			return false;
		if (feature == null) {
			if (other.feature != null)
				return false;
		} else if (!feature.equals(other.feature))
			return false;
		if (hotfix == null) {
			if (other.hotfix != null)
				return false;
		} else if (!hotfix.equals(other.hotfix))
			return false;
		if (master == null) {
			if (other.master != null)
				return false;
		} else if (!master.equals(other.master))
			return false;
		if (release == null) {
			if (other.release != null)
				return false;
		} else if (!release.equals(other.release))
			return false;
		if (versionTag == null) {
			if (other.versionTag != null)
				return false;
		} else if (!versionTag.equals(other.versionTag))
			return false;
		return true;
	}

	/**
	 * @generated
	 */
	@Override
	public String toString() {
		return "GitFlowInitParameters [master=" + master + ", develop=" //$NON-NLS-1$ //$NON-NLS-2$
				+ develop + ", feature=" + feature //$NON-NLS-1$
				+ ", release=" + release + ", hotfix=" //$NON-NLS-1$ //$NON-NLS-2$
				+ hotfix + ", versionTag=" + versionTag + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
