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
package org.eclipse.egit.gitflow.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Visible strings for the GitFlow plugin.
 */
public class CoreText extends NLS {

	/**
	 * Do not in-line this into the static initializer as the
	 * "Find Broken Externalized Strings" tool will not be able to find the
	 * corresponding bundle file.
	 */
	private static final String BUNDLE_NAME = "org.eclipse.egit.gitflow.internal.coretext"; //$NON-NLS-1$

	/** */

	static {
		initializeMessages(BUNDLE_NAME, CoreText.class);
	}

	/** */
	public static String AbstractFeatureOperation_notOnAFeautreBranch;

	/** */
	public static String AbstractHotfixOperation_notOnAHotfixBranch;

	/** */
	public static String AbstractReleaseOperation_notOnAReleaseBranch;

	/** */
	public static String AbstractVersionFinishOperation_tagNameExists;

	/** */
	public static String FeatureListOperation_unableToParse;

	/** */
	public static String pushToRemoteFailed;

	/** */
	public static String unableToStoreGitConfig;

	/** */
	public static String FeatureRebaseOperation_notOnAFeatureBranch;

	/** */
	public static String FeatureStartOperation_notOn;

	/** */
	public static String FeatureTrackOperation_checkoutReturned;

	/** */
	public static String FeatureTrackOperation_localBranchExists;

	/** */
	public static String FeatureTrackOperation_unableToStoreGitConfig;

	/** */
	public static String GitFlowOperation_branchMissing;

	/** */
	public static String GitFlowOperation_branchNotFound;

	/** */
	public static String GitFlowOperation_unableToCheckout;

	/** */
	public static String GitFlowOperation_unableToMerge;

	/** */
	public static String GitFlowRepository_gitFlowRepositoryMayNotBeEmpty;

	/** */
	public static String HotfixFinishOperation_hotfix;

	/** */
	public static String HotfixFinishOperation_mergeFromHotfixToMasterFailed;

	/** */
	public static String InitOperation_initialCommit;

	/** */
	public static String InitOperation_localMasterDoesNotExist;

	/** */
	public static String ReleaseFinishOperation_releaseOf;

	/** */
	public static String ReleaseStartOperation_notOn;

	/** */
	public static String ReleaseStartOperation_releaseNameAlreadyExists;

	/** */
	public static String StartOperation_unableToFindCommitFor;

}
