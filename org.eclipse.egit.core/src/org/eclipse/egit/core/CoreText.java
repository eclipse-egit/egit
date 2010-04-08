/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core;

import org.eclipse.osgi.util.NLS;

/**
 * Possibly Translated strings for the Egit plugin.
 */
public class CoreText extends NLS {

	/** */
	public static String AssumeUnchangedOperation_adding;

	/** */
	public static String AssumeUnchangedOperation_writingIndex;

	/** */
	public static String UpdateJob_updatingIndex;

	/** */
	public static String UpdateJob_writingIndex;

	/** */
	public static String UpdateOperation_updating;

	/** */
	public static String UpdateOperation_failed;

	/** */
	public static String CommitFileRevision_errorLookingUpPath;

	/** */
	public static String CommitFileRevision_pathNotIn;

	/** */
	public static String ConnectProviderOperation_connecting;

	/** */
	public static String ConnectProviderOperation_ConnectingProject;

	/** */
	public static String DisconnectProviderOperation_disconnecting;

	/** */
	public static String AddOperation_adding;

	/** */
	public static String AddOperation_failed;

	/** */
	public static String BlobStorage_blobNotFound;

	/** */
	public static String BlobStorage_errorReadingBlob;

	/** */
	public static String BranchOperation_checkoutMovingTo;

	/** */
	public static String BranchOperation_CheckoutOnlyBranchOrTag;

	/** */
	public static String BranchOperation_checkoutProblem;

	/** */
	public static String BranchOperation_mappingCommit;

	/** */
	public static String BranchOperation_mappingCommitHead;

	/** */
	public static String BranchOperation_mappingTrees;

	/** */
	public static String BranchOperation_updatingHeadToRef;

	/** */
	public static String BranchOperation_writingIndex;

	/** */
	public static String UntrackOperation_adding;

	/** */
	public static String UntrackOperation_failed;

	/** */
	public static String UntrackOperation_writingIndex;

	/** */
	public static String GitFileHistory_errorParsingHistory;

	/** */
	public static String GitFileHistory_gitNotAttached;

	/** */
	public static String GitFileHistory_invalidHeadRevision;

	/** */
	public static String GitFileHistory_noHeadRevisionAvailable;

	/** */
	public static String GitProjectData_lazyResolveFailed;

	/** */
	public static String GitProjectData_mappedResourceGone;

	/** */
	public static String GitProjectData_cannotReadHEAD;

	/** */
	public static String GitProjectData_failedFindingRepoMapping;

	/** */
	public static String GitProjectData_failedToCacheRepoMapping;

	/** */
	public static String GitProjectData_missing;

	/** */
	public static String GitProjectData_saveFailed;

	/** */
	public static String GitProjectData_notifyChangedFailed;

	/** */
	public static String RepositoryFinder_finding;

	/** */
	public static String ResetOperation_cantUpdate;

	/** */
	public static String ResetOperation_lookingUpCommit;

	/** */
	public static String ResetOperation_lookingUpRef;

	/** */
	public static String ResetOperation_mappingTreeForCommit;

	/** */
	public static String ResetOperation_performingReset;

	/** */
	public static String ResetOperation_readingIndex;

	/** */
	public static String ResetOperation_updatingFailed;

	/** */
	public static String ResetOperation_writingIndex;

	/** */
	public static String MoveDeleteHook_cannotModifyFolder;

	/** */
	public static String MoveDeleteHook_operationError;

	/** */
	public static String Error_CanonicalFile;

	/** */
	public static String CloneOperation_checkingOutFiles;

	/** */
	public static String CloneOperation_initializingRepository;

	/** */
	public static String CloneOperation_title;

	/** */
	public static String CloneOperation_writingIndex;

	/** */
	public static String IndexFileRevision_errorLookingUpPath;

	/** */
	public static String IndexFileRevision_indexEntryNotFound;

	/** */
	public static String ListRemoteOperation_title;

	/** */
	public static String ProjectUtil_refreshingProjects;

	/** */
	public static String PushOperation_resultCancelled;

	/** */
	public static String PushOperation_resultNotSupported;

	/** */
	public static String PushOperation_resultTransportError;

	/** */
	public static String PushOperation_resultNoServiceError;

	/** */
	public static String PushOperation_taskNameDryRun;

	/** */
	public static String PushOperation_taskNameNormalRun;

	/** */
	public static String AddToIndexOperation_failed;

	/** */
	public static String TrackOperation_writingIndex;

	/** */
	public static String BranchOperation_performingBranch;

	static {
		initializeMessages("org.eclipse.egit.core.coretext", //$NON-NLS-1$
				CoreText.class);
	}
}
