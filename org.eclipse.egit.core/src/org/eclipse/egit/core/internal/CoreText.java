/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2012, Markus Duft <markus.duft@salomon.at>
 * Copyright (C) 2013, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2013, Daniel Megert <daniel_megert@ch.ibm.com>
 * Copyright (C) 2015, Obeo.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
	public static String Activator_HttpClientUnknown;

	/** */
	public static String Activator_ignoreResourceFailed;

	/** */
	public static String Activator_noBuiltinLfsSupportDetected;

	/** */
	public static String Activator_ReconfigureWindowCacheError;

	/** */
	public static String Activator_refreshFailed;

	/** */
	public static String Activator_SshClientNoJsch;

	/** */
	public static String Activator_SshClientUnknown;

	/** */
	public static String AssumeUnchangedOperation_adding;

	/** */
	public static String AssumeUnchangedOperation_writingIndex;

	/** */
	public static String CachingRepository_cacheLevelZero;

	/** */
	public static String CherryPickOperation_cherryPicking;

	/** */
	public static String CommitFileRevision_errorLookingUpPath;

	/** */
	public static String CommitFileRevision_errorLookingUpTags;

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
	public static String RepositoryUtil_ReflogCorrupted;

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
	public static String BranchOperation_checkoutError;

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
	public static String ValidationUtils_CanNotResolveRefMessage;

	/** */
	public static String ValidationUtils_InvalidRefNameMessage;

	/** */
	public static String ValidationUtils_InvalidRevision;

	/** */
	public static String ValidationUtils_RefAlreadyExistsMessage;

	/** */
	public static String ValidationUtils_RefNameConflictsWithExistingMessage;

	/** */
	public static String ValidationUtils_PleaseEnterNameMessage;

	/** */
	public static String ReportingTypedConfigGetter_invalidConfig;

	/** */
	public static String ReportingTypedConfigGetter_invalidConfigIgnored;

	/** */
	public static String ReportingTypedConfigGetter_invalidConfigWithLocation;

	/** */
	public static String ReportingTypedConfigGetter_invalidConfigWithLocationIgnored;

	/** */
	public static String SshPreferencesMirror_invalidDirectory;

	/** */
	public static String SshPreferencesMirror_invalidKeyFile;

	/** */
	public static String RebaseInteractiveStep_Edit;

	/** */
	public static String RebaseInteractiveStep_Fixup;

	/** */
	public static String RebaseInteractiveStep_Pick;

	/** */
	public static String RebaseInteractiveStep_Reword;

	/** */
	public static String RebaseInteractiveStep_Skip;

	/** */
	public static String RebaseInteractiveStep_Squash;

	static {
		initializeMessages(BUNDLE_NAME,	CoreText.class);
	}

}
