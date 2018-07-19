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
package org.eclipse.egit.gitflow.ui.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Text resources for the plugin. Strings here can be i18n-ed simpler and avoid
 * duplicating strings.
 */
public class UIText extends NLS {

	/**
	 * Do not in-line this into the static initializer as the
	 * "Find Broken Externalized Strings" tool will not be able to find the
	 * corresponding bundle file.
	 */
	private static final String BUNDLE_NAME = "org.eclipse.egit.gitflow.ui.internal.uitext"; //$NON-NLS-1$

	static {
		initializeMessages(BUNDLE_NAME, UIText.class);
	}

	/**	 */
	public static String DynamicHistoryMenu_startGitflowReleaseFrom;

	/**	 */
	public static String FeatureFinishHandler_Conflicts;

	/**	 */
	public static String FilteredBranchesWidget_lastModified;

	/**	 */
	public static String FinishHandler_conflictsWhileMergingFromTo;

	/**	 */
	public static String AbstractGitFlowHandler_rebaseConflicts;

	/**	 */
	public static String AbstractGitFlowHandler_finishConflicts;

	/** */
	public static String BranchSelectionTree_IdColumn;

	/** */
	public static String BranchSelectionTree_MessageColumn;

	/** */
	public static String BranchSelectionTree_NameColumn;

	/**	 */
	public static String FeatureFinishHandler_finishingFeature;

	/**	 */
	public static String FeatureFinishHandler_rewordSquashedCommitMessage;

	/**	 */
	public static String FeatureRebaseHandler_problemsOccurred;

	/**	 */
	public static String FeatureRebaseHandler_rebaseFailed;

	/**	 */
	public static String FeatureRebaseHandler_rebasingFeature;

	/**	 */
	public static String FeatureRebaseHandler_problemsOcccurredDuringRebase;

	/**	 */
	public static String FeatureRebaseHandler_statusWas;

	/**	 */
	public static String FeatureStartHandler_pleaseProvideANameForTheNewFeature;

	/**	 */
	public static String FeatureStartHandler_provideFeatureName;

	/**	 */
	public static String FeatureStartHandler_startingNewFeature;

	/**	 */
	public static String FeatureTrackHandler_ButtonOK;

	/**	 */
	public static String FeatureTrackHandler_fetchingRemoteFeatures;

	/**	 */
	public static String FeatureTrackHandler_noRemoteFeatures;

	/**	 */
	public static String FeatureTrackHandler_noRemoteFeaturesFoundOnTheConfiguredRemote;

	/**	 */
	public static String FeatureTrackHandler_remoteFeatures;

	/**	 */
	public static String FeatureTrackHandler_selectFeature;

	/**	 */
	public static String FeatureTrackHandler_trackingFeature;

	/**	 */
	public static String Handlers_noGitflowRepositoryFound;

	/**	 */
	public static String HotfixFinishHandler_Conflicts;

	/**	 */
	public static String HotfixFinishHandler_finishingHotfix;

	/**	 */
	public static String HotfixFinishHandler_hotfixFinishConflicts;

	/**	 */
	public static String HotfixStartHandler_pleaseProvideANameForTheNewHotfix;

	/**	 */
	public static String HotfixStartHandler_provideHotfixName;

	/**	 */
	public static String HotfixStartHandler_startingNewHotfix;

	/**	 */
	public static String InitDialog_branchDoesNotExistYetAndWillBeCreated;

	/**	 */
	public static String InitDialog_chooseBranchNamesAndPrefixes;

	/**	 */
	public static String InitDialog_developBranch;

	/**	 */
	public static String InitDialog_featureBranchPrefix;

	/**	 */
	public static String InitDialog_hotfixBranchPrefix;

	/**	 */
	public static String InitDialog_initializeRepository;

	/**	 */
	public static String InitDialog_invalidBranchName;

	/**	 */
	public static String InitDialog_invalidPrefix;

	/**	 */
	public static String InitDialog_masterBranch;

	/**	 */
	public static String InitDialog_masterBranchIsMissing;

	/**	 */
	public static String InitDialog_releaseBranchPrefix;

	/**	 */
	public static String InitDialog_selectedMasterBranchDoesNotExistCreateNow;

	/**	 */
	public static String InitDialog_versionTagPrefix;

	/**	 */
	public static String InitDialog_ButtonOK;

	/**	 */
	public static String InitHandler_doYouWantToInitNow;

	/**	 */
	public static String InitHandler_emptyRepository;

	/**	 */
	public static String InitHandler_initialCommit;

	/**	 */
	public static String InitHandler_initializing;

	/**	 */
	public static String ReleaseFinishHandler_Conflicts;

	/**	 */
	public static String ReleaseFinishHandler_finishingRelease;

	/**	 */
	public static String ReleaseFinishHandler_releaseFinishConflicts;

	/**	 */
	public static String ReleaseStartHandler_provideANameForTheNewRelease;

	/**	 */
	public static String ReleaseStartHandler_provideReleaseName;

	/**	 */
	public static String ReleaseStartHandler_startCommitCouldNotBeDetermined;

	/**	 */
	public static String ReleaseStartHandler_startingNewRelease;

	/**	 */
	public static String FeatureCheckoutHandler_ButtonOK;

	/**	 */
	public static String FeatureCheckoutHandler_checkingOutFeature;

	/**	 */
	public static String FeatureCheckoutHandler_localFeatures;

	/**	 */
	public static String FeatureCheckoutHandler_selectFeature;

	/**	 */
	public static String FeaturePublishHandler_publishingFeature;

	/**	 */
	public static String ReleasePublishHandler_publishingRelease;

	/**	 */
	public static String HotfixPublishHandler_publishingHotfix;

	/**	 */
	public static String NameValidator_invalidName;

	/**	 */
	public static String NameValidator_nameAlreadyExists;

	/** */
	public static String FeatureCheckoutHandler_cleanupDialog_title;

	/** */
	public static String FeatureCheckoutHandler_cleanupDialog_text;

	/** */
	public static String FinishFeatureDialog_ButtonOK;

	/** */
	public static String FinishFeatureDialog_keepBranch;

	/** */
	public static String FinishFeatureDialog_saveAsDefault;

	/** */
	public static String FinishFeatureDialog_squashCheck;

	/** */
	public static String FinishFeatureDialog_title;

	/** */
	public static String FinishFeatureDialog_setParameterForFinishing;

	/** */
	public static String HotfixFinishOperation_unexpectedConflictsHotfixAborted;

	/** */
	public static String ReleaseFinishOperation_unexpectedConflictsReleaseAborted;

	/** */
	public static String StartDialog_ButtonOK;

	/** */
	public static String UIIcons_errorDeterminingIconBase;

	/** */
	public static String UIIcons_errorLoadingPluginImage;
}
