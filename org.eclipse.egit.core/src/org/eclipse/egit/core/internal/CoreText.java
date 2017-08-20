/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2012, Markus Duft <markus.duft@salomon.at>
 * Copyright (C) 2013, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2013, Daniel Megert <daniel_megert@ch.ibm.com>
 * Copyright (C) 2015, Obeo.
 * Copyright (C) 2017, SATO Yusuke <yusuke.sato.zz@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import org.eclipse.osgi.util.NLS;

/**
 * Possibly Translated strings for the Egit plugin.
 */
public class CoreText extends NLS {

	/**
	 * Do not in-line this into the static initializer as the
	 * "Find Broken Externalized Strings" tool will not be
	 * able to find the corresponding bundle file.
	 */
	private static final String BUNDLE_NAME = "org.eclipse.egit.core.internal.coretext"; //$NON-NLS-1$

	/** */
	public static String Activator_invalidPreferredMergeStrategy;

	/** */
	public static String Activator_autoIgnoreDerivedResources;

	/** */
	public static String Activator_AutoShareJobName;

	/** */
	public static String Activator_AutoSharingFailed;

	/** */
	public static String Activator_ignoreResourceFailed;

	/** */
	public static String Activator_ReconfigureWindowCacheError;

	/** */
	public static String AssumeUnchangedOperation_adding;

	/** */
	public static String AssumeUnchangedOperation_writingIndex;

	/** */
	public static String CherryPickOperation_cherryPicking;

	/** */
	public static String CommitFileRevision_errorLookingUpPath;

	/** */
	public static String CommitFileRevision_pathNotIn;

	/** */
	public static String CommitOperation_ParseCherryPickCommitFailed;

	/** */
	public static String CommitOperation_PerformingCommit;

	/** */
	public static String CommitOperation_couldNotFindRepositoryMapping;

	/** */
	public static String CommitOperation_errorParsingPersonIdent;

	/** */
	public static String ConfigureFetchAfterCloneTask_couldNotFetch;

	/** */
	public static String ConnectProviderOperation_autoIgnoreMetaData;

	/** */
	public static String ConnectProviderOperation_connecting;

	/** */
	public static String ConnectProviderOperation_ConnectingProject;

	/** */
	public static String ConnectProviderOperation_ConnectErrors;

	/** */
	public static String ConnectProviderOperation_NoRepositoriesError;

	/** */
	public static String ConnectProviderOperation_UnexpectedRepositoryError;

	/** */
	public static String ContainerTreeIterator_DeletedFile;

	/** */
	public static String DeleteBranchOperation_Canceled;

	/** */
	public static String DeleteBranchOperation_TaskName;

	/** */
	public static String DeleteTagOperation_exceptionMessage;

	/** */
	public static String DiffHeaderFormat_Email;

	/** */
	public static String DiffHeaderFormat_None;

	/** */
	public static String DiffHeaderFormat_Oneline;

	/** */
	public static String DiffHeaderFormat_Workspace;

	/** */
	public static String DiscardChangesOperation_discardFailed;

	/** */
	public static String DiscardChangesOperation_discardFailedSeeLog;

	/** */
	public static String DiscardChangesOperation_discardingChanges;

	/** */
	public static String DiscardChangesOperation_refreshFailed;

	/** */
	public static String DeleteResourcesOperation_deletingResources;

	/** */
	public static String DeleteResourcesOperation_deleteFailed;

	/** */
	public static String DeleteResourcesOperation_deleteFailedSeeLog;

	/** */
	public static String DisconnectProviderOperation_disconnecting;

	/** */
	public static String BlobStorage_blobNotFound;

	/** */
	public static String BlobStorage_errorReadingBlob;

	/** */
	public static String UntrackOperation_adding;

	/** */
	public static String UntrackOperation_failed;

	/** */
	public static String UntrackOperation_writingIndex;

	/** */
	public static String GerritUtil_ConfigSaveError;

	/** */
	public static String GitFileHistory_errorParsingHistory;

	/** */
	public static String GitFileHistory_gitNotAttached;

	/** */
	public static String GitFileHistory_invalidCommit;

	/** */
	public static String GitFileHistory_invalidHeadRevision;

	/** */
	public static String GitFileHistory_noHeadRevisionAvailable;

	/** */
	public static String GitProjectData_mappedResourceGone;

	/** */
	public static String GitProjectData_failedFindingRepoMapping;

	/** */
	public static String GitProjectData_failedToCacheRepoMapping;

	/** */
	public static String GitProjectData_failedToUnmapRepoMapping;

	/** */
	public static String GitProjectData_FailedToMarkTeamPrivate;

	/** */
	public static String GitProjectData_missing;

	/** */
	public static String GitProjectData_saveFailed;

	/** */
	public static String RebaseInteractivePlan_WriteRebaseTodoFailed;

	/** */
	public static String RepositoryFinder_finding;

	/** */
	public static String RepositoryPathChecker_errAbsoluteRepoPath;

	/** */
	public static String RepositoryPathChecker_errNoCloneCommand;

	/** */
	public static String RepositoryPathChecker_errNoDirectory;

	/** */
	public static String RepositoryPathChecker_errNotAbsoluteRepoPath;

	/** */
	public static String RepositoryPathChecker_errNoURL;

	/** */
	public static String RepositoryUtil_DirectoryIsNotGitDirectory;

	/** */
	public static String RepositoryUtil_noHead;

	/** */
	public static String RemoteRefUpdateCantBeReused;

	/** */
	public static String RenameBranchOperation_TaskName;

	/** */
	public static String ResetOperation_performingReset;

	/** */
	public static String ResourceUtil_SaveLocalHistoryFailed;

	/** */
	public static String ResourceUtil_mapProjectJob;

	/** */
	public static String MergeOperation_InternalError;

	/** */
	public static String MergeOperation_MergeFailedNoHead;

	/** */
	public static String MergeOperation_MergeFailedRefUpdate;

	/** */
	public static String MergeOperation_ProgressMerge;

	/** */
	public static String MergeStrategy_MissingName;

	/** */
	public static String MergeStrategy_DuplicateName;

	/** */
	public static String MergeStrategy_ReservedName;

	/** */
	public static String MergeStrategy_LoadError;

	/** */
	public static String MergeStrategy_UnloadError;

	/** */
	public static String MoveDeleteHook_cannotModifyFolder;

	/** */
	public static String MoveDeleteHook_operationError;

	/** */
	public static String MoveDeleteHook_unmergedFileError;

	/** */
	public static String MoveDeleteHook_unmergedFileInFolderError;

	/** */
	public static String MoveDeleteHook_cannotAutoStageDeletion;

	/** */
	public static String Error_CanonicalFile;

	/** */
	public static String CloneOperation_failed_cleanup;

	/** */
	public static String CloneOperation_submodule_title;

	/** */
	public static String CloneOperation_title;

	/** */
	public static String CloneOperation_configuring;

	/** */
	public static String CreateLocalBranchOperation_CreatingBranchMessage;

	/** */
	public static String CreatePatchOperation_repoRequired;

	/** */
	public static String CreatePatchOperation_cannotCreatePatchForMergeCommit;

	/** */
	public static String CreatePatchOperation_couldNotFindProject;

	/** */
	public static String CreatePatchOperation_patchFileCouldNotBeWritten;

	/** */
	public static String IndexDiffCacheEntry_cannotReadIndex;

	/** */
	public static String IndexDiffCacheEntry_errorCalculatingIndexDelta;

	/** */
	public static String IndexDiffCacheEntry_refreshingProjects;

	/** */
	public static String IndexDiffCacheEntry_reindexing;

	/** */
	public static String IndexDiffCacheEntry_reindexingIncrementally;

	/** */
	public static String IndexFileRevision_errorLookingUpPath;

	/** */
	public static String ListRemoteOperation_title;

	/** */
	public static String ProjectUtil_refreshingProjects;

	/** */
	public static String ProjectUtil_refreshing;

	/** */
	public static String ProjectUtil_taskCheckingDirectory;

	/** */
	public static String PullOperation_DetachedHeadMessage;

	/** */
	public static String PullOperation_PullNotConfiguredMessage;

	/** */
	public static String PullOperation_TaskName;

	/** */
	public static String PushOperation_InternalExceptionOccurredMessage;

	/** */
	public static String PushOperation_ExceptionOccurredDuringPushOnUriMessage;

	/** */
	public static String PushOperation_resultCancelled;

	/** */
	public static String PushOperation_taskNameDryRun;

	/** */
	public static String PushOperation_taskNameNormalRun;

	/** */
	public static String AddToIndexOperation_failed;

	/** */
	public static String RemoveFromIndexOperation_removingFilesFromIndex;

	/** */
	public static String RevertCommitOperation_reverting;

	/** */
	public static String RewordCommitOperation_rewording;

	/** */
	public static String EditCommitOperation_editing;

	/** */
	public static String BranchOperation_closingMissingProject;

	/** */
	public static String BranchOperation_performingBranch;

	/** */
	public static String TagOperation_performingTagging;

	/** */
	public static String TagOperation_taggingFailure;

	/** */
	public static String TagOperation_objectIdNotFound;

	/** */
	public static String GitResourceVariantTree_fetchingVariant;

	/** */
	public static String GitResourceVariantTreeSubscriber_CouldNotFindSourceVariant;

	/** */
	public static String GitBranchResourceVariantTreeSubscriber_gitRepository;

	/** */
	public static String GitLazyResourceVariantTreeSubscriber_name;

	/** */
	public static String OperationAlreadyExecuted;

	/** */
	public static String OperationNotYetExecuted;

	/** */
	public static String ProjectReference_InvalidTokensCount;

	/** */
	public static String GitProjectSetCapability_CloneToExistingDirectory;

	/** */
	public static String GitProjectSetCapability_ExportCouldNotGetBranch;

	/** */
	public static String GitProjectSetCapability_ExportNoRemote;

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

	/** */
	public static String GitSubscriberMergeContext_FailedUpdateRevs;

	/** */
	public static String GitSubscriberMergeContext_FailedRefreshSyncView;

	/** */
	public static String GitProjectData_repositoryChangedJobName;

	/** */
	public static String GitProjectData_repositoryChangedTaskName;

	/** */
	public static String GitProjectData_UnmapJobName;

	/** */
	public static String GitProjectData_UnmappingGoneResourceFailed;

	/** */
	public static String GitResourceVariantTreeSubscriber_name;

	/** */
	public static String GitResourceVariantTreeSubscriber_fetchTaskName;

	/** */
	public static String GitSyncObjectCache_noData;

	/** */
	public static String GitRemoteFolder_fetchingMembers;

	/** */
	public static String GitURI_InvalidSCMURL;

	/** */
	public static String GitURI_InvalidURI;

	/** */
	public static String SquashCommitsOperation_squashing;

	/** */
	public static String SubmoduleUpdateOperation_updating;

	/** */
	public static String SubmoduleUpdateOperation_cloning;

	/** */
	public static String Utils_InvalidAdapterError;

	/** */
	public static String Start_Clone;

	/** */
	public static String End_Clone;

	/** */
	public static String Error_Clone;

	/** */
	public static String Start_Checkout;

	/** */
	public static String End_Checkout;

	/** */
	public static String Error_Checkout;

	/** */
	public static String Start_CherryPick;

	/** */
	public static String End_CherryPick;

	/** */
	public static String Error_CherryPick;

	/** */
	public static String Start_Commit;

	/** */
	public static String End_Commit;

	/** */
	public static String Error_Commit;

	/** */
	public static String Start_Fetch;

	/** */
	public static String End_Fetch;

	/** */
	public static String Error_Fetch;

	/** */
	public static String Start_Branch;

	/** */
	public static String End_Branch;

	/** */
	public static String Error_Branch;

	/** */
	public static String Start_Merge;

	/** */
	public static String End_Merge;

	/** */
	public static String Error_Merge;

	/** */
	public static String Start_Pull;

	/** */
	public static String End_Pull;

	/** */
	public static String Error_Pull;

	/** */
	public static String Start_Push;

	/** */
	public static String End_Push;

	/** */
	public static String Error_Push;

	/** */
	public static String Start_Rebase;

	/** */
	public static String End_Rebase;

	/** */
	public static String Error_Rebase;

	/** */
	public static String Start_Reset;

	/** */
	public static String End_Reset;

	/** */
	public static String Error_Reset;

	/** */
	public static String Start_Revert;

	/** */
	public static String End_Revert;

	/** */
	public static String Error_Revert;

	/** */
	public static String Start_Amend;

	/** */
	public static String End_Amend;

	/** */
	public static String Error_Amend;

	/** */
	public static String Start_Squash;

	/** */
	public static String End_Squash;

	/** */
	public static String Error_Squash;

	/** */
	public static String Start_Stash_Apply;

	/** */
	public static String End_Stash_Apply;

	/** */
	public static String Error_Stash_Apply;

	/** */
	public static String Start_Stash_Create;

	/** */
	public static String End_Stash_Create;

	/** */
	public static String Error_Stash_Create;

	static {
		initializeMessages(BUNDLE_NAME,	CoreText.class);
	}

}
