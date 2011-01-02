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
	public static String Activator_ReconfigureWindowCacheError;

	/** */
	public static String AssumeUnchangedOperation_adding;

	/** */
	public static String AssumeUnchangedOperation_writingIndex;

	/** */
	public static String CommitFileRevision_errorLookingUpPath;

	/** */
	public static String CommitFileRevision_pathNotIn;

	/** */
	public static String CommitOperation_errorCommittingChanges;

	/** */
	public static String CommitOperation_errorPreparingTrees;

	/** */
	public static String CommitOperation_errorWritingTrees;

	/** */
	public static String CommitOperation_failedToUpdate;

	/** */
	public static String CommitOperation_PerformingCommit;

	/** */
	public static String ConnectProviderOperation_connecting;

	/** */
	public static String ConnectProviderOperation_ConnectingProject;

	/** */
	public static String DeleteBranchOperation_TaskName;

	/** */
	public static String DiscardChangesOperation_discardFailed;

	/** */
	public static String DiscardChangesOperation_discardFailedSeeLog;

	/** */
	public static String DiscardChangesOperation_discardingChanges;

	/** */
	public static String DiscardChangesOperation_refreshFailed;

	/** */
	public static String DiscardChangesOperation_repoNotFound;

	/** */
	public static String DiscardChangesOperation_writeIndexFailed;

	/** */
	public static String DisconnectProviderOperation_disconnecting;

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
	public static String BranchOperation_couldNotDelete;

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
	public static String RemoteRefUpdateCantBeReused;

	/** */
	public static String RenameBranchOperation_TaskName;

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
	public static String ResetOperation_resetMergeFailed;

	/** */
	public static String ResetOperation_updatingFailed;

	/** */
	public static String ResetOperation_writingIndex;

	/** */
	public static String MergeOperation_InternalError;

	/** */
	public static String MergeOperation_MergeFailedNoHead;

	/** */
	public static String MergeOperation_MergeFailedRefUpdate;

	/** */
	public static String MergeOperation_ProgressMerge;

	/** */
	public static String CherryPickOperation_InternalError;

	/** */
	public static String RevertOperation_InternalError;

	/** */
	public static String CherryPickOperation_Failed;

	/** */
	public static String RevertOperation_Failed;

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
	public static String CreateLocalBranchOperation_CreatingBranchMessage;

	/** */
	public static String CreateLocalBranchOperation_NoBranchMessage;

	/** */
	public static String IndexFileRevision_errorLookingUpPath;

	/** */
	public static String IndexFileRevision_indexEntryNotFound;

	/** */
	public static String ListRemoteOperation_title;

	/** */
	public static String ProjectUtil_refreshingProjects;

	/** */
	public static String ProjectUtil_refreshing;

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

	/** */
	public static String TagOperation_performingTagging;

	/** */
	public static String TagOperation_taggingFailure;

	/** */
	public static String TagOperation_objectIdNotFound;

	/** */
	public static String GitResourceVariantTree_couldNotFindResourceVariant;

	/** */
	public static String GitResourceVariantTree_couldNotFetchMembers;

	/** */
	public static String GitFolderResourceVariant_fetchingMembers;

	/** */
	public static String GitResourceVariantTree_fetchingVariant;

	/** */
	public static String GitResourceVariantTree_couldNotFetchMembersOf;

	/** */
	public static String GitBranchResourceVariantTreeSubscriber_gitRepository;

	/** */
	public static String OperationAlreadyExecuted;

	/** */
	public static String OperationNotYetExecuted;

	/** */
	public static String IgnoreOperation_error;

	/** */
	public static String IgnoreOperation_parentOutsideRepo;

	/** */
	public static String IgnoreOperation_creatingFailed;

	/** */
	public static String IgnoreOperation_taskName;

	/** */
	public static String IgnoreOperation_updatingFailed;

	static {
		initializeMessages("org.eclipse.egit.core.coretext", //$NON-NLS-1$
				CoreText.class);
	}

}
