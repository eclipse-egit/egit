/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui;

import org.eclipse.osgi.util.NLS;

/**
 * Text resources for the plugin. Strings here can be i18n-ed simpler and avoid
 * duplicating strings.
 */
public class UIText extends NLS {

	/** */
	public static String WizardProjectsImportPage_filterText;

	/** */
	public static String AbortRebaseCommand_CancelDialogMessage;

	/** */
	public static String AbortRebaseCommand_JobName;

	/** */
	public static String AbstractHistoryCommanndHandler_CouldNotGetRepositoryMessage;

	/** */
	public static String AbstractHistoryCommanndHandler_NoInputMessage;

	/** */
	public static String AbstractHistoryCommanndHandler_NoUniqueRepository;

	/** */
	public static String AbstractRebaseCommand_DialogTitle;

	/** */
	public static String Activator_DefaultRepoFolderIsFile;

	/** */
	public static String Activator_DefaultRepoFolderNotCreated;

	/** */
	public static String Activator_refreshFailed;

	/** */
	public static String Activator_refreshingProjects;

	/** */
	public static String Activator_refreshJobName;

	/** */
	public static String Activator_repoScanJobName;

	/** */
	public static String Activator_scanError;

	/** */
	public static String Activator_scanningRepositories;

	/** */
	public static String AddConfigEntryDialog_AddConfigTitle;

	/** */
	public static String AddConfigEntryDialog_ConfigKeyTooltip;

	/** */
	public static String AddConfigEntryDialog_DialogMessage;

	/** */
	public static String AddConfigEntryDialog_EnterValueMessage;

	/** */
	public static String AddConfigEntryDialog_EntryExistsMessage;

	/** */
	public static String AddConfigEntryDialog_KeyComponentsMessage;

	/** */
	public static String AddConfigEntryDialog_KeyLabel;

	/** */
	public static String AddConfigEntryDialog_MustEnterKeyMessage;

	/** */
	public static String AddConfigEntryDialog_ValueLabel;

	/** */
	public static String AddToIndexAction_addingFiles;

	/** */
	public static String AddToIndexCommand_addingFilesFailed;

	/** */
	public static String RemoveFromIndexAction_removingFiles;

	/** */
	public static String BlameInformationControl_Author;

	/** */
	public static String BlameInformationControl_Commit;

	/** */
	public static String BlameInformationControl_Committer;

	/** */
	public static String AssumeUnchanged_assumeUnchanged;

	/** */
	public static String AssumeUnchanged_noAssumeUnchanged;

	/** */
	public static String WizardProjectsImportPage_ImportProjectsTitle;

	/** */
	public static String WizardProjectsImportPage_ImportProjectsDescription;

	/** */
	public static String WizardProjectsImportPage_ProjectsListTitle;

	/** */
	public static String WizardProjectsImportPage_selectAll;

	/** */
	public static String WizardProjectsImportPage_deselectAll;

	/** */
	public static String WizardProjectsImportPage_SearchingMessage;

	/** */
	public static String WizardProjectsImportPage_ProcessingMessage;

	/** */
	public static String WizardProjectsImportPage_projectsInWorkspace;

	/** */
	public static String WizardProjectsImportPage_CheckingMessage;

	/** */
	public static String WizardProjectsImportPage_CreateProjectsTask;

	/** */
	public static String SecureStoreUtils_writingCredentialsFailed;

	/** */
	public static String SelectResetTypePage_PageMessage;

	/** */
	public static String SelectResetTypePage_PageTitle;

	/** */
	public static String SharingWizard_windowTitle;

	/** */
	public static String SharingWizard_failed;

	/** */
	public static String SharingWizard_MoveProjectActionLabel;

	/** */
	public static String ShowBlameHandler_JobName;

	/** */
	public static String GenerateHistoryJob_BuildingListMessage;

	/** */
	public static String GenerateHistoryJob_CancelMessage;

	/** */
	public static String GenerateHistoryJob_errorComputingHistory;

	/** */
	public static String GenerateHistoryJob_NoCommits;

	/** */
	public static String GerritConfigurationPage_configurePushToGerrit;

	/** */
	public static String GerritConfigurationPage_errorBranchName;

	/** */
	public static String GerritConfigurationPage_groupPush;

	/** */
	public static String GerritConfigurationPage_labelDestinationBranch;

	/** */
	public static String GerritConfigurationPage_PageDescription;

	/** */
	public static String GerritConfigurationPage_pushUri;

	/** */
	public static String GerritConfigurationPage_title;

	/** */
	public static String EGitCredentialsProvider_errorReadingCredentials;

	/** */
	public static String EgitUiUtils_CouldNotOpenEditorMessage;

	/** */
	public static String ExistingOrNewPage_BrowseRepositoryButton;

	/** */
	public static String ExistingOrNewPage_CreateButton;

	/** */
	public static String ExistingOrNewPage_CreateRepositoryButton;

	/** */
	public static String ExistingOrNewPage_CreationInWorkspaceWarningTooltip;

	/** */
	public static String ExistingOrNewPage_CurrentLocationColumnHeader;

	/** */
	public static String ExistingOrNewPage_title;

	/** */
	public static String ExistingOrNewPage_description;

	/** */
	public static String ExistingOrNewPage_DescriptionExternalMode;

	/** */
	public static String ExistingOrNewPage_ErrorFailedToCreateRepository;

	/** */
	public static String ExistingOrNewPage_ErrorFailedToRefreshRepository;

	/** */
	public static String ExistingOrNewPage_ExistingRepositoryLabel;

	/** */
	public static String ExistingOrNewPage_ExistingTargetErrorMessage;

	/** */
	public static String ExistingOrNewPage_FailedToDetectRepositoryMessage;

	/** */
	public static String ExistingOrNewPage_FolderWillBeCreatedMessage;

	/** */
	public static String ExistingOrNewPage_HeaderPath;

	/** */
	public static String ExistingOrNewPage_HeaderProject;

	/** */
	public static String ExistingOrNewPage_HeaderRepository;

	/** */
	public static String ExistingOrNewPage_InternalModeCheckbox;

	/** */
	public static String ExistingOrNewPage_NestedProjectErrorMessage;

	/** */
	public static String ExistingOrNewPage_NewLocationTargetHeader;

	/** */
	public static String ExistingOrNewPage_NoRepositorySelectedMessage;

	/** */
	public static String ExistingOrNewPage_ProjectNameColumnHeader;

	/** */
	public static String ExistingOrNewPage_RelativePathLabel;

	/** */
	public static String ExistingOrNewPage_RepoCreationInWorkspaceCreationWarning;

	/** */
	public static String ExistingOrNewPage_SymbolicValueEmptyMapping;

	/** */
	public static String ExistingOrNewPage_WorkingDirectoryLabel;

	/** */
	public static String ExistingOrNewPage_WrongPathErrorDialogMessage;

	/** */
	public static String ExistingOrNewPage_WrongPathErrorDialogTitle;

	/** */
	public static String GitCloneWizard_abortingCloneMsg;

	/** */
	public static String GitCloneWizard_abortingCloneTitle;

	/** */
	public static String GitCloneWizard_title;

	/** */
	public static String GitCloneWizard_jobName;

	/** */
	public static String GitCloneWizard_failed;

	/** */
	public static String GitCloneWizard_errorCannotCreate;

	/** */
	public static String GitDecoratorPreferencePage_bindingRepositoryNameFlag;

	/** */
	public static String GitDecoratorPreferencePage_iconsShowDirty;

	/** */
	public static String GitDocument_errorLoadCommit;

	/** */
	public static String GitDocument_errorLoadTree;

	/** */
	public static String GitDocument_errorRefreshQuickdiff;

	/** */
	public static String GitDocument_errorResolveQuickdiff;

	/** */
	public static String GitHistoryPage_AllChangesInRepoHint;

	/** */
	public static String GitHistoryPage_AllChangesOfResourceHint;

	/** */
	public static String GitHistoryPage_AllChangesInFolderHint;

	/** */
	public static String GitHistoryPage_AllChangesInProjectHint;

	/** */
	public static String GitHistoryPage_AllInParentMenuLabel;

	/** */
	public static String GitHistoryPage_AllInParentTooltip;

	/** */
	public static String GitHistoryPage_AllInProjectMenuLabel;

	/** */
	public static String GitHistoryPage_AllInProjectTooltip;

	/** */
	public static String GitHistoryPage_AllInRepoMenuLabel;

	/** */
	public static String GitHistoryPage_AllInRepoTooltip;

	/** */
	public static String GitHistoryPage_AllOfResourceMenuLabel;

	/** */
	public static String GitHistoryPage_AllOfResourceTooltip;

	/** */
	public static String GitHistoryPage_cherryPickMenuItem;

	/** */
	public static String GitHistoryPage_compareMode;

	/** */
	public static String GitHistoryPage_showAllBranches;

	/** */
	public static String GitHistoryPage_CheckoutMenuLabel;

	/** */
	public static String GitHistoryPage_CompareModeMenuLabel;

	/** */
	public static String GitHistoryPage_ReuseCompareEditorMenuLabel;

	/** */
	public static String GitHistoryPage_CompareWithCurrentHeadMenu;

	/** */
	public static String GitHistoryPage_CompareWithEachOtherMenuLabel;

	/** */
	public static String GitHistoryPage_CompareWithWorkingTreeMenuMenuLabel;

	/** */
	public static String GitHistoryPage_CreateBranchMenuLabel;

	/** */
	public static String GitHistoryPage_CreatePatchMenuLabel;

	/** */
	public static String GitHistoryPage_CreateTagMenuLabel;

	/** */
	public static String GitHistoryPage_errorLookingUpPath;

	/** */
	public static String GitHistoryPage_errorParsingHead;

	/** */
	public static String GitHistoryPage_errorReadingAdditionalRefs;

	/** */
	public static String GitHistoryPage_errorSettingStartPoints;

	/** */
	public static String GitHistoryPage_FileNotInCommit;

	/** */
	public static String GitHistoryPage_FileOrFolderPartOfGitDirMessage;

	/** */
	public static String GitHistoryPage_FileType;

	/** */
	public static String GitHistoryPage_FindMenuLabel;

	/** */
	public static String GitHistoryPage_FindTooltip;

	/** */
	public static String GitHistoryPage_FolderType;

	/** */
	public static String GitHistoryPage_fileNotFound;

	/** */
	public static String GitHistoryPage_notContainedInCommits;

	/** */
	public static String GitHistoryPage_MultiResourcesType;

	/** */
	public static String GitHistoryPage_OpenInTextEditorLabel;

	/** */
	public static String GitHistoryPage_NoInputMessage;

	/** */
	public static String GitHistoryPage_openFailed;

	/** */
	public static String GitHistoryPage_OpenMenuLabel;

	/** */
	public static String GitHistoryPage_PreferencesLink;

	/** */
	public static String GitHistoryPage_ProjectType;

	/** */
	public static String GitHistoryPage_QuickdiffMenuLabel;

	/** */
	public static String GitHistoryPage_RefreshMenuLabel;

	/** */
	public static String GitHistoryPage_RepositoryNamePattern;

	/** */
	public static String GitHistoryPage_ResetBaselineToHeadMenuLabel;

	/** */
	public static String GitHistoryPage_ResetBaselineToParentOfHeadMenuLabel;

	/** */
	public static String GitHistoryPage_ResetHardMenuLabel;

	/** */
	public static String GitHistoryPage_ResetMenuLabel;

	/** */
	public static String GitHistoryPage_ResetMixedMenuLabel;

	/** */
	public static String GitHistoryPage_ResetSoftMenuLabel;

	/** */
	public static String GitHistoryPage_revertMenuItem;

	/** */
	public static String GitHistoryPage_mergeMenuItem;

	/** */
	public static String GitHistoryPage_rebaseMenuItem;

	/** */
	public static String GitHistoryPage_SetAsBaselineMenuLabel;

	/** */
	public static String GitHistoryPage_ShowAllBranchesMenuLabel;

	/** */
	public static String GitHistoryPage_FilterSubMenuLabel;

	/** */
	public static String GitHistoryPage_IncompleteListTooltip;

	/** */
	public static String GitHistoryPage_ListIncompleteWarningMessage;

	/** */
	public static String GitHistoryPage_ShowSubMenuLabel;

	/** */
	public static String GitPreferenceRoot_automaticallyEnableChangesetModel;

	/** */
	public static String GitPreferenceRoot_BlameGroupHeader;

	/** */
	public static String GitPreferenceRoot_BlameIgnoreWhitespaceLabel;

	/** */
	public static String GitPreferenceRoot_fetchBeforeSynchronization;

	/** */
	public static String GitPreferenceRoot_CloningRepoGroupHeader;

	/** */
	public static String GitPreferenceRoot_DefaultRepoFolderLabel;

	/** */
	public static String GitPreferenceRoot_DefaultRepoFolderTooltip;

	/** */
	public static String GitPreferenceRoot_HistoryGroupHeader;

	/** */
	public static String GitPreferenceRoot_MergeGroupHeader;

	/** */
	public static String GitPreferenceRoot_MergeMode_0_Label;

	/** */
	public static String GitPreferenceRoot_MergeMode_1_Label;

	/** */
	public static String GitPreferenceRoot_MergeMode_2_Label;

	/** */
	public static String GitPreferenceRoot_MergeModeLabel;

	/** */
	public static String GitPreferenceRoot_MergeModeTooltip;

	/** */
	public static String GitPreferenceRoot_RemoteConnectionsGroupHeader;

	/** */
	public static String GitPreferenceRoot_RepoChangeScannerGroupHeader;

	/** */
	public static String GitPreferenceRoot_SynchronizeView;

	/** */
	public static String GitProjectPropertyPage_LabelBranch;

	/** */
	public static String GitProjectPropertyPage_LabelGitDir;

	/** */
	public static String GitProjectPropertyPage_LabelId;

	/** */
	public static String GitProjectPropertyPage_LabelState;

	/** */
	public static String GitProjectPropertyPage_LabelWorkdir;

	/** */
	public static String GitProjectPropertyPage_UnableToGetCommit;

	/** */
	public static String GitProjectPropertyPage_ValueEmptyRepository;

	/** */
	public static String GitProjectPropertyPage_ValueUnbornBranch;

	/** */
	public static String GitProjectsImportPage_NoProjectsMessage;

	/** */
	public static String RebaseCurrentRefCommand_RebaseCanceledMessage;

	/** */
	public static String RebaseCurrentRefCommand_RebaseCanceledTitle;

	/** */
	public static String RebaseCurrentRefCommand_RebasingCurrentJobName;

	/** */
	public static String RebaseResultDialog_Aborted;

	/** */
	public static String RebaseResultDialog_AbortRebaseRadioText;

	/** */
	public static String RebaseResultDialog_ActionGroupTitle;

	/** */
	public static String RebaseResultDialog_CommitIdLabel;

	/** */
	public static String RebaseResultDialog_CommitMessageLabel;

	/** */
	public static String RebaseResultDialog_Conflicting;

	/** */
	public static String RebaseResultDialog_ConflictListFailureMessage;

	/** */
	public static String RebaseResultDialog_DetailsGroup;

	/** */
	public static String RebaseResultDialog_DialogTitle;

	/** */
	public static String RebaseResultDialog_DiffDetailsLabel;

	/** */
	public static String RebaseResultDialog_DoNothingRadioText;

	/** */
	public static String RebaseResultDialog_FastForward;

	/** */
	public static String RebaseResultDialog_Failed;

	/** */
	public static String RebaseResultDialog_NextSteps;

	/** */
	public static String RebaseResultDialog_NextStepsAfterResolveConflicts;

	/** */
	public static String RebaseResultDialog_NextStepsDoNothing;

	/** */
	public static String RebaseResultDialog_SkipCommitButton;

	/** */
	public static String RebaseResultDialog_StartMergeRadioText;

	/** */
	public static String RebaseResultDialog_StatusLabel;

	/** */
	public static String RebaseResultDialog_Stopped;

	/** */
	public static String RebaseResultDialog_SuccessfullyFinished;

	/** */
	public static String RebaseResultDialog_ToggleShowButton;

	/** */
	public static String RebaseResultDialog_UpToDate;

	/** */
	public static String RebaseTargetSelectionDialog_DialogMessage;

	/** */
	public static String RebaseTargetSelectionDialog_DialogTitle;

	/** */
	public static String RebaseTargetSelectionDialog_RebaseButton;

	/** */
	public static String RebaseTargetSelectionDialog_RebaseTitle;

	/** */
	public static String ReplaceTargetSelectionDialog_ReplaceButton;

	/** */
	public static String ReplaceTargetSelectionDialog_ReplaceMessage;

	/** */
	public static String ReplaceTargetSelectionDialog_ReplaceTitle;

	/** */
	public static String ReplaceTargetSelectionDialog_ReplaceWindowTitle;

	/** */
	public static String RepositoryPropertySource_EditConfigButton;

	/** */
	public static String RepositoryPropertySource_EditConfigurationTitle;

	/** */
	public static String RepositoryPropertySource_EditorMessage;

	/** */
	public static String RepositoryPropertySource_EffectiveConfigurationAction;

	/** */
	public static String RepositoryPropertySource_EffectiveConfigurationCategory;

	/** */
	public static String RepositoryPropertySource_ErrorHeader;

	/** */
	public static String RepositoryPropertySource_GlobalConfigurationCategory;

	/** */
	public static String RepositoryPropertySource_GlobalConfigurationMenu;

	/** */
	public static String RepositoryPropertySource_RepositoryConfigurationButton;

	/** */
	public static String RepositoryPropertySource_RepositoryConfigurationCategory;

	/** */
	public static String RepositoryPropertySource_SystemConfigurationMenu;

	/** */
	public static String RepositoryPropertySource_SelectModeTooltip;

	/** */
	public static String RepositoryPropertySource_SingleValueButton;

	/** */
	public static String RepositoryPropertySource_SuppressMultipleValueTooltip;

	/** */
	public static String RepositoryRemotePropertySource_ErrorHeader;

	/** */
	public static String RepositoryRemotePropertySource_FetchLabel;

	/** */
	public static String RepositoryRemotePropertySource_PushLabel;

	/** */
	public static String RepositoryRemotePropertySource_RemoteFetchURL_label;

	/** */
	public static String RepositoryRemotePropertySource_RemotePushUrl_label;

	/** */
	public static String RepositorySearchDialog_AddGitRepositories;

	/** */
	public static String RepositorySearchDialog_DeepSearch_button;

	/** */
	public static String RepositorySearchDialog_RepositoriesFound_message;

	/** */
	public static String RepositorySearchDialog_ScanningForRepositories_message;

	/** */
	public static String RepositorySearchDialog_DirectoryNotFoundMessage;

	/** */
	public static String RepositorySearchDialog_Search;

	/** */
	public static String RepositorySearchDialog_SearchCriteriaGroup;

	/** */
	public static String RepositorySearchDialog_SearchRecursiveToolTip;

	/** */
	public static String RepositorySearchDialog_SearchResultGroup;

	/** */
	public static String RepositorySearchDialog_SearchTitle;

	/** */
	public static String RepositorySearchDialog_SearchTooltip;

	/** */
	public static String RepositorySearchDialog_SomeDirectoriesHiddenMessage;

	/** */
	public static String ClearCredentialsCommand_clearingCredentialsFailed;

	/** */
	public static String CheckoutDialog_Message;

	/** */
	public static String CheckoutDialog_Title;

	/** */
	public static String CheckoutDialog_WindowTitle;

	/** */
	public static String CheckoutHandler_SelectBranchMessage;

	/** */
	public static String CheckoutHandler_SelectBranchTitle;

	/** */
	public static String CherryPickHandler_NoCherryPickPerformedMessage;

	/** */
	public static String CherryPickHandler_NoCherryPickPerformedTitle;

	/** */
	public static String CherryPickHandler_CherryPickConflictsMessage;

	/** */
	public static String CherryPickHandler_CherryPickConflictsTitle;

	/** */
	public static String CherryPickHandler_CherryPickFailedMessage;

	/** */
	public static String CherryPickHandler_CouldNotDeleteFile;

	/** */
	public static String CherryPickHandler_ErrorMsgTemplate;

	/** */
	public static String CherryPickHandler_IndexDirty;

	/** */
	public static String CherryPickHandler_unknown;

	/** */
	public static String CherryPickHandler_WorktreeDirty;

	/** */
	public static String CherryPickOperation_Failed;

	/** */
	public static String CherryPickOperation_InternalError;

	/** */
	public static String CompareTargetSelectionDialog_CompareButton;

	/** */
	public static String CompareTargetSelectionDialog_CompareMessage;

	/** */
	public static String CompareTargetSelectionDialog_CompareTitle;

	/** */
	public static String CompareTargetSelectionDialog_WindowTitle;

	/** */
	public static String CompareTreeView_AnalyzingRepositoryTaskText;

	/** */
	public static String CompareTreeView_CollapseAllTooltip;

	/** */
	public static String CompareTreeView_ComparingTwoVersionDescription;

	/** */
	public static String CompareTreeView_ComparingWorkspaceVersionDescription;

	/** */
	public static String CompareTreeView_EqualFilesTooltip;

	/** */
	public static String CompareTreeView_IndexVersionText;

	/** */
	public static String CompareTreeView_ItemNotFoundInVersionMessage;

	/** */
	public static String CompareTreeView_MultipleResourcesHeaderText;

	/** */
	public static String CompareTreeView_NoDifferencesFoundMessage;

	/** */
	public static String CompareTreeView_NoInputText;

	/** */
	public static String CompareTreeView_RepositoryRootName;

	/** */
	public static String CompareTreeView_WorkspaceVersionText;

	/** */
	public static String CompareUtils_errorGettingEncoding;

	/** */
	public static String CompareUtils_errorGettingHeadCommit;

	/** */
	public static String MergeHandler_SelectBranchMessage;

	/** */
	public static String MergeHandler_SelectBranchTitle;

	/** */
	public static String CompareWithIndexAction_FileNotInIndex;

	/** */
	public static String RepositoryAction_errorFindingRepo;

	/** */
	public static String RepositoryAction_errorFindingRepoTitle;

	/** */
	public static String RepositoryAction_multiRepoSelection;

	/** */
	public static String RepositoryAction_multiRepoSelectionTitle;

	/** */
	public static String RepositoryCommit_UserAndDate;

	/** */
	public static String RepositorySearchDialog_browse;

	/** */
	public static String RepositorySearchDialog_CheckAllRepositories;

	/** */
	public static String RepositorySearchDialog_directory;

	/** */
	public static String RepositorySearchDialog_EnterDirectoryToolTip;

	/** */
	public static String RepositorySearchDialog_errorOccurred;

	/** */
	public static String RepositorySearchDialog_NoSearchAvailableMessage;

	/** */
	public static String RepositorySearchDialog_NothingFoundMessage;

	/** */
	public static String RepositorySearchDialog_searchRepositoriesMessage;

	/** */
	public static String RepositorySearchDialog_UncheckAllRepositories;

	/** */
	public static String RepositorySelectionPage_BrowseLocalFile;

	/** */
	public static String RepositorySelectionPage_sourceSelectionTitle;

	/** */
	public static String RepositorySelectionPage_sourceSelectionDescription;

	/** */
	public static String RepositorySelectionPage_destinationSelectionTitle;

	/** */
	public static String RepositorySelectionPage_destinationSelectionDescription;

	/** */
	public static String RepositorySelectionPage_groupLocation;

	/** */
	public static String RepositorySelectionPage_groupAuthentication;

	/** */
	public static String RepositorySelectionPage_groupConnection;

	/** */
	public static String RepositorySelectionPage_promptURI;

	/** */
	public static String RepositorySelectionPage_promptHost;

	/** */
	public static String RepositorySelectionPage_promptPath;

	/** */
	public static String RepositorySelectionPage_promptUser;

	/** */
	public static String RepositorySelectionPage_promptPassword;

	/** */
	public static String RepositorySelectionPage_promptScheme;

	/** */
	public static String RepositorySelectionPage_promptPort;

	/** */
	public static String RepositorySelectionPage_fieldRequired;

	/** */
	public static String RepositorySelectionPage_fieldNotSupported;

	/** */
	public static String RepositorySelectionPage_fileNotFound;

	/** */
	public static String RepositorySelectionPage_internalError;

	/** */
	public static String RepositorySelectionPage_configuredRemoteChoice;

	/** */
	public static String RepositorySelectionPage_errorValidating;

	/** */
	public static String RepositorySelectionPage_storeInSecureStore;

	/** */
	public static String RepositorySelectionPage_tip_file;

	/** */
	public static String RepositorySelectionPage_tip_ftp;

	/** */
	public static String RepositorySelectionPage_tip_git;

	/** */
	public static String RepositorySelectionPage_tip_http;

	/** */
	public static String RepositorySelectionPage_tip_https;

	/** */
	public static String RepositorySelectionPage_tip_sftp;

	/** */
	public static String RepositorySelectionPage_tip_ssh;

	/** */
	public static String RepositorySelectionPage_uriChoice;

	/** */
	public static String RepositorySelectionPage_UriMustNotHaveTrailingSpacesMessage;

	/** */
	public static String SoftResetToRevisionAction_softReset;

	/** */
	public static String SourceBranchPage_repoEmpty;

	/** */
	public static String SourceBranchPage_title;

	/** */
	public static String SourceBranchPage_description;

	/** */
	public static String SourceBranchPage_branchList;

	/** */
	public static String SourceBranchPage_selectAll;

	/** */
	public static String SourceBranchPage_selectNone;

	/** */
	public static String SourceBranchPage_errorBranchRequired;

	/** */
	public static String SourceBranchPage_transportError;

	/** */
	public static String SourceBranchPage_cannotListBranches;

	/** */
	public static String SourceBranchPage_remoteListingCancelled;

	/** */
	public static String SourceBranchPage_cannotCreateTemp;

	/** */
	public static String CloneDestinationPage_title;

	/** */
	public static String CloneDestinationPage_description;

	/** */
	public static String CloneDestinationPage_groupDestination;

	/** */
	public static String CloneDestinationPage_groupConfiguration;

	/** */
	public static String CloneDestinationPage_promptDirectory;

	/** */
	public static String CloneDestinationPage_promptInitialBranch;

	/** */
	public static String CloneDestinationPage_promptRemoteName;

	/** */
	public static String CloneDestinationPage_browseButton;

	/** */
	public static String CloneDestinationPage_DefaultRepoFolderTooltip;

	/** */
	public static String CloneDestinationPage_errorDirectoryRequired;

	/** */
	public static String CloneDestinationPage_errorInitialBranchRequired;

	/** */
	public static String CloneDestinationPage_errorNotEmptyDir;

	/** */
	public static String CloneDestinationPage_errorRemoteNameRequired;

	/** */
	public static String RefContentProposal_blob;

	/** */
	public static String RefContentProposal_branch;

	/** */
	public static String RefContentProposal_by;

	/** */
	public static String RefContentProposal_commit;

	/** */
	public static String RefContentProposal_errorReadingObject;

	/** */
	public static String RefContentProposal_tag;

	/** */
	public static String RefContentProposal_trackingBranch;

	/** */
	public static String RefContentProposal_tree;

	/** */
	public static String RefContentProposal_unknownObject;

	/** */
	public static String RefSelectionDialog_Messsage;

	/** */
	public static String RefSelectionDialog_Title;

	/** */
	public static String RefSpecDialog_AutoSuggestCheckbox;

	/** */
	public static String RefSpecDialog_DestinationFetchLabel;

	/** */
	public static String RefSpecDialog_DestinationPushLabel;

	/** */
	public static String RefSpecDialog_FetchMessage;

	/** */
	public static String RefSpecDialog_FetchTitle;

	/** */
	public static String RefSpecDialog_ForceUpdateCheckbox;

	/** */
	public static String RefSpecDialog_GettingRemoteRefsMonitorMessage;

	/** */
	public static String RefSpecDialog_MissingDataMessage;

	/** */
	public static String RefSpecDialog_PushMessage;

	/** */
	public static String RefSpecDialog_PushTitle;

	/** */
	public static String RefSpecDialog_SourceBranchFetchLabel;

	/** */
	public static String RefSpecDialog_SourceBranchPushLabel;

	/** */
	public static String RefSpecDialog_SpecificationLabel;

	/** */
	public static String RefSpecDialog_WindowTitle;

	/** */
	public static String RefSpecPanel_refChooseSome;

	/** */
	public static String RefSpecPanel_refChooseSomeWildcard;

	/** */
	public static String RefSpecPanel_refChooseRemoteName;

	/** */
	public static String RefSpecPanel_clickToChange;

	/** */
	public static String RefSpecPanel_columnDst;

	/** */
	public static String RefSpecPanel_columnForce;

	/** */
	public static String RefSpecPanel_columnMode;

	/** */
	public static String RefSpecPanel_columnRemove;

	/** */
	public static String RefSpecPanel_columnSrc;

	/** */
	public static String RefSpecPanel_creationButton;

	/** */
	public static String RefSpecPanel_creationButtonDescription;

	/** */
	public static String RefSpecPanel_creationDst;

	/** */
	public static String RefSpecPanel_creationGroup;

	/** */
	public static String RefSpecPanel_creationSrc;

	/** */
	public static String RefSpecPanel_deletionButton;

	/** */
	public static String RefSpecPanel_deletionButtonDescription;

	/** */
	public static String RefSpecPanel_deletionGroup;

	/** */
	public static String RefSpecPanel_deletionRef;

	/** */
	public static String RefSpecPanel_dstDeletionDescription;

	/** */
	public static String RefSpecPanel_dstFetchDescription;

	/** */
	public static String RefSpecPanel_dstPushDescription;

	/** */
	public static String RefSpecPanel_fetch;

	/** */
	public static String RefSpecPanel_fetchTitle;

	/** */
	public static String RefSpecPanel_srcFetchDescription;

	/** */
	public static String RefSpecPanel_forceAll;

	/** */
	public static String RefSpecPanel_forceAllDescription;

	/** */
	public static String RefSpecPanel_forceDeleteDescription;

	/** */
	public static String RefSpecPanel_forceFalseDescription;

	/** */
	public static String RefSpecPanel_forceTrueDescription;

	/** */
	public static String RefSpecPanel_modeDelete;

	/** */
	public static String RefSpecPanel_modeDeleteDescription;

	/** */
	public static String RefSpecPanel_modeUpdate;

	/** */
	public static String RefSpecPanel_modeUpdateDescription;

	/** */
	public static String RefSpecPanel_predefinedAll;

	/** */
	public static String RefSpecPanel_predefinedAllDescription;

	/** */
	public static String RefSpecPanel_predefinedConfigured;

	/** */
	public static String RefSpecPanel_predefinedConfiguredDescription;

	/** */
	public static String RefSpecPanel_predefinedGroup;

	/** */
	public static String RefSpecPanel_predefinedTags;

	/** */
	public static String RefSpecPanel_predefinedTagsDescription;

	/** */
	public static String RefSpecPanel_push;

	/** */
	public static String RefSpecPanel_pushTitle;

	/** */
	public static String RefSpecPanel_srcPushDescription;

	/** */
	public static String RefSpecPanel_removeAll;

	/** */
	public static String RefSpecPanel_removeAllDescription;

	/** */
	public static String RefSpecPanel_removeDescription;

	/** */
	public static String RefSpecPanel_specifications;

	/** */
	public static String RefSpecPanel_srcDeleteDescription;

	/** */
	public static String RefSpecPanel_validationDstInvalidExpression;

	/** */
	public static String RefSpecPanel_validationDstRequired;

	/** */
	public static String RefSpecPanel_validationRefDeleteRequired;

	/** */
	public static String RefSpecPanel_validationRefDeleteWildcard;

	/** */
	public static String RefSpecPanel_validationRefInvalidExpression;

	/** */
	public static String RefSpecPanel_validationRefInvalidLocal;

	/** */
	public static String RefSpecPanel_validationRefNonExistingRemote;

	/** */
	public static String RefSpecPanel_validationRefNonExistingRemoteDelete;

	/** */
	public static String RefSpecPanel_validationRefNonMatchingLocal;

	/** */
	public static String RefSpecPanel_validationRefNonMatchingRemote;

	/** */
	public static String RefSpecPanel_validationSpecificationsOverlappingDestination;

	/** */
	public static String RefSpecPanel_validationSrcUpdateRequired;

	/** */
	public static String RefSpecPanel_validationWildcardInconsistent;

	/** */
	public static String RefSpecPage_descriptionFetch;

	/** */
	public static String RefSpecPage_descriptionPush;

	/** */
	public static String RefSpecPage_errorDontMatchSrc;

	/** */
	public static String RefSpecPage_errorTransportDialogMessage;

	/** */
	public static String RefSpecPage_errorTransportDialogTitle;

	/** */
	public static String RefSpecPage_operationCancelled;

	/** */
	public static String RefSpecPage_saveSpecifications;

	/** */
	public static String RefSpecPage_titleFetch;

	/** */
	public static String RefSpecPage_titlePush;

	/** */
	public static String RefSpecPage_annotatedTagsGroup;

	/** */
	public static String RefSpecPage_annotatedTagsAutoFollow;

	/** */
	public static String RefSpecPage_annotatedTagsFetchTags;

	/** */
	public static String RefSpecPage_annotatedTagsNoTags;

	/** */
	public static String QuickDiff_failedLoading;

	/** */
	public static String ResetAction_errorResettingHead;

	/** */
	public static String ResetAction_repositoryState;

	/** */
	public static String ResetAction_reset;

	/** */
	public static String ResetCommand_ResetFailureMessage;

	/** */
	public static String ResetCommand_WizardTitle;

	/** */
	public static String ResetQuickdiffBaselineHandler_NoTargetMessage;

	/** */
	public static String ResetTargetSelectionDialog_ResetButton;

	/** */
	public static String ResetTargetSelectionDialog_ResetConfirmQuestion;

	/** */
	public static String ResetTargetSelectionDialog_ResetQuestion;

	/** */
	public static String ResetTargetSelectionDialog_ResetTitle;

	/** */
	public static String ResetTargetSelectionDialog_ResetTypeGroup;

	/** */
	public static String ResetTargetSelectionDialog_ResetTypeHardButton;

	/** */
	public static String ResetTargetSelectionDialog_ResetTypeMixedButton;

	/** */
	public static String ResetTargetSelectionDialog_ResetTypeSoftButton;

	/** */
	public static String ResetTargetSelectionDialog_SelectBranchForResetMessage;

	/** */
	public static String ResetTargetSelectionDialog_WindowTitle;

	/** */
	public static String ResourceHistory_MaxNumCommitsInList;

	/** */
	public static String ResourceHistory_ShowTagSequence;

	/** */
	public static String ResourceHistory_toggleRelativeDate;

	/** */
	public static String ResourceHistory_toggleCommentWrap;

	/** */
	public static String ResourceHistory_toggleCommentFill;

	/** */
	public static String ResourceHistory_toggleRevDetail;

	/** */
	public static String ResourceHistory_toggleRevComment;

	/** */
	public static String HardResetToRevisionAction_hardReset;

	/** */
	public static String HistoryPage_authorColumn;

	/** */
	public static String HistoryPage_dateColumn;

	/** */
	public static String HistoryPage_refreshJob;

	/** */
	public static String HistoryPage_findbar_find;

	/** */
	public static String HistoryPage_findbar_next;

	/** */
	public static String HistoryPage_findbar_previous;

	/** */
	public static String HistoryPage_findbar_ignorecase;

	/** */
	public static String HistoryPage_findbar_commit;

	/** */
	public static String HistoryPage_findbar_comments;

	/** */
	public static String HistoryPage_findbar_author;

	/** */
	public static String HistoryPage_findbar_committer;

	/** */
	public static String HistoryPage_findbar_changeto_commit;

	/** */
	public static String HistoryPage_findbar_changeto_comments;

	/** */
	public static String HistoryPage_findbar_changeto_author;

	/** */
	public static String HistoryPage_findbar_changeto_committer;

	/** */
	public static String HistoryPage_findbar_exceeded;

	/** */
	public static String HistoryPage_findbar_notFound;

	/** */
	public static String PullOperationUI_NotTriedMessage;

	/** */
	public static String PullOperationUI_PullCanceledWindowTitle;

	/** */
	public static String PullOperationUI_PullErrorWindowTitle;

	/** */
	public static String PullOperationUI_PullingMultipleTaskName;

	/** */
	public static String PullOperationUI_PullingTaskName;

	/** */
	public static String PullOperationUI_PullOperationCanceledMessage;

	/** */
	public static String PullResultDialog_DialogTitle;

	/** */
	public static String PullResultDialog_FetchResultGroupHeader;

	/** */
	public static String PullResultDialog_MergeAlreadyUpToDateMessage;

	/** */
	public static String PullResultDialog_MergeResultGroupHeader;

	/** */
	public static String PullResultDialog_NothingToFetchFromLocal;

	/** */
	public static String PullResultDialog_RebaseStatusLabel;

	/** */
	public static String PullResultDialog_RebaseStoppedMessage;

	/** */
	public static String PushAction_wrongURIDescription;

	/** */
	public static String PushAction_wrongURITitle;

	/** */
	public static String PushOperationUI_MultiRepositoriesDestinationString;

	/** */
	public static String PushOperationUI_PushJobName;

	/** */
	public static String PushWizard_cantConnectToAny;

	/** */
	public static String PushWizard_cantPrepareUpdatesMessage;

	/** */
	public static String PushWizard_cantPrepareUpdatesTitle;

	/** */
	public static String PushWizard_cantSaveMessage;

	/** */
	public static String PushWizard_cantSaveTitle;

	/** */
	public static String PushWizard_jobName;

	/** */
	public static String PushWizard_missingRefsMessage;

	/** */
	public static String PushWizard_missingRefsTitle;

	/** */
	public static String PushWizard_unexpectedError;

	/** */
	public static String PushWizard_windowTitleDefault;

	/** */
	public static String PushWizard_windowTitleWithDestination;

	/** */
	public static String CommitAction_amendCommit;

	/** */
	public static String CommitAction_amendNotPossible;

	/** */
	public static String CommitAction_cannotCommit;

	/** */
	public static String CommitAction_CommittingChanges;

	/** */
	public static String CommitAction_CommittingFailed;

	/** */
	public static String CommitAction_errorComputingDiffs;

	/** */
	public static String CommitAction_errorRetrievingCommit;

	/** */
	public static String CommitAction_noFilesToCommit;

	/** */
	public static String CommitAction_repositoryState;

	/** */
	public static String CommitDialog_AddFileOnDiskToIndex;

	/** */
	public static String CommitDialog_AddSOB;

	/** */
	public static String CommitDialog_AmendPreviousCommit;

	/** */
	public static String CommitDialog_Author;

	/** */
	public static String CommitDialog_Commit;

	/** */
	public static String CommitDialog_CommitChanges;

	/** */
	public static String CommitDialog_Committer;

	/** */
	public static String CommitDialog_CommitMessage;

	/** */
	public static String CommitDialog_DeselectAll;

	/** */
	public static String CommitDialog_ErrorAddingFiles;

	/** */
	public static String CommitDialog_ErrorInvalidAuthor;

	/** */
	public static String CommitDialog_ErrorInvalidAuthorSpecified;

	/** */
	public static String CommitDialog_ErrorInvalidCommitterSpecified;

	/** */
	public static String CommitDialog_ErrorMustEnterCommitMessage;

	/** */
	public static String CommitDialog_ErrorNoItemsSelected;

	/** */
	public static String CommitDialog_ErrorNoItemsSelectedToBeCommitted;

	/** */
	public static String CommitDialog_ErrorNoMessage;

	/** */
	public static String CommitDialog_SelectAll;

	/** */
	public static String CommitDialog_ShowUntrackedFiles;

	/** */
	public static String CommitDialog_Status;

	/** */
	public static String CommitDialog_StatusAdded;

	/** */
	public static String CommitDialog_StatusAddedIndexDiff;

	/** */
	public static String CommitDialog_StatusAssumeUnchaged;

	/** */
	public static String CommitDialog_StatusModified;

	/** */
	public static String CommitDialog_StatusModifiedIndexDiff;

	/** */
	public static String CommitDialog_StatusModifiedNotStaged;

	/** */
	public static String CommitDialog_StatusRemoved;

	/** */
	public static String CommitDialog_StatusRemovedNotStaged;

	/** */
	public static String CommitDialog_StatusUnknown;

	/** */
	public static String CommitDialog_StatusUntracked;

	/** */
	public static String CommitDialog_StatusRemovedUntracked;

	/** */
	public static String CommitDialog_AddChangeIdLabel;

	/** */
	public static String CommitDialog_WrongTypeOfCommitMessageProvider;

	/** */
	public static String CommitDialog_ConfigureLink;

	/** */
	public static String CommitDialog_Files;

	/** */
	public static String CommitDialog_Message;

	/** */
	public static String CommitDialog_Path;

	/** */
	public static String CommitDialog_Title;

	/** */
	public static String ConfigurationChecker_checkHomeDirectory;

	/** */
	public static String ConfigurationChecker_checkConfiguration;

	/** */
	public static String ConfigurationChecker_doNotShowAgain;

	/** */
	public static String ConfigurationChecker_homeNotSet;

	/** */
	public static String ConfigurationEditorComponent_AddButton;

	/** */
	public static String ConfigurationEditorComponent_ChangeButton;

	/** */
	public static String ConfigurationEditorComponent_ConfigLocationLabel;

	/** */
	public static String ConfigurationEditorComponent_DeleteButton;

	/** */
	public static String ConfigurationEditorComponent_EmptyStringNotAllowed;

	/** */
	public static String ConfigurationEditorComponent_KeyColumnHeader;

	/** */
	public static String ConfigurationEditorComponent_NewValueButton;

	/** */
	public static String ConfigurationEditorComponent_NoConfigLocationKnown;

	/** */
	public static String ConfigurationEditorComponent_NoEntrySelectedMessage;

	/** */
	public static String ConfigurationEditorComponent_NoSectionSubsectionMessage;

	/** */
	public static String ConfigurationEditorComponent_OpenEditorButton;

	/** */
	public static String ConfigurationEditorComponent_OpenEditorTooltip;

	/** */
	public static String ConfigurationEditorComponent_ReadOnlyLocationFormat;

	/** */
	public static String ConfigurationEditorComponent_RemoveAllButton;

	/** */
	public static String ConfigurationEditorComponent_RemoveAllTooltip;

	/** */
	public static String ConfigurationEditorComponent_RemoveSectionMessage;

	/** */
	public static String ConfigurationEditorComponent_RemoveSectionTitle;

	/** */
	public static String ConfigurationEditorComponent_RemoveSubsectionMessage;

	/** */
	public static String ConfigurationEditorComponent_RemoveSubsectionTitle;

	/** */
	public static String ConfigurationEditorComponent_ValueColumnHeader;

	/** */
	public static String ConfigurationEditorComponent_ValueLabel;

	/** */
	public static String ConfigurationEditorComponent_WrongNumberOfTokensMessage;

	/** */
	public static String GlobalConfigurationPreferencePage_systemSettingTabTitle;

	/** */
	public static String GlobalConfigurationPreferencePage_userSettingTabTitle;

	/** */
	public static String GlobalConfigurationPreferencePage_repositorySettingTabTitle;

	/** */
	public static String GlobalConfigurationPreferencePage_repositorySettingRepositoryLabel;

	/** */
	public static String GlobalConfigurationPreferencePage_repositorySettingNoRepositories;

	/** */
	public static String SpellCheckingMessageArea_copy;

	/** */
	public static String SpellCheckingMessageArea_cut;

	/** */
	public static String SpellCheckingMessageArea_paste;

	/** */
	public static String SpellCheckingMessageArea_selectAll;

	/** */
	public static String CommitMessageViewer_author;

	/** */
	public static String CommitMessageViewer_child;

	/** */
	public static String CommitMessageViewer_branches;

	/** */
	public static String CommitMessageViewer_BuildDiffListTaskName;

	/** */
	public static String CommitMessageViewer_BuildDiffTaskName;

	/** */
	public static String CommitMessageViewer_CanNotRenderDiffMessage;

	/** */
	public static String CommitMessageViewer_tags;

	/** */
	public static String CommitMessageViewer_follows;

	/** */
	public static String CommitMessageViewer_precedes;

	/** */
	public static String CommitMessageViewer_commit;

	/** */
	public static String CommitMessageViewer_committer;

	/** */
	public static String CommitMessageViewer_FormattingMessageTaskName;

	/** */
	public static String CommitMessageViewer_GettingNextTagTaskName;

	/** */
	public static String CommitMessageViewer_GettingPreviousTagTaskName;

	/** */
	public static String CommitMessageViewer_parent;

	/** */
	public static String CommitMessageViewer_SelectOneCommitMessage;

	/** */
	public static String CompareWithIndexAction_errorOnAddToIndex;

	/** */
	public static String CompareWithPreviousActionHandler_MessageRevisionNotFound;

	/** */
	public static String CompareWithPreviousActionHandler_TaskGeneratingInput;

	/** */
	public static String CompareWithPreviousActionHandler_TitleRevisionNotFound;

	/** */
	public static String ConfirmationPage_cantConnectToAnyTitle;

	/** */
	public static String ConfirmationPage_cantConnectToAny;

	/** */
	public static String ConfirmationPage_description;

	/** */
	public static String ConfirmationPage_errorCantResolveSpecs;

	/** */
	public static String ConfirmationPage_errorInterrupted;

	/** */
	public static String ConfirmationPage_errorRefsChangedNoMatch;

	/** */
	public static String ConfirmationPage_errorUnexpected;

	/** */
	public static String ConfirmationPage_requireUnchangedButton;

	/** */
	public static String ConfirmationPage_showOnlyIfChanged;

	/** */
	public static String ConfirmationPage_title;

	/** */
	public static String ContinueRebaseCommand_CancelDialogMessage;

	/** */
	public static String ContinueRebaseCommand_JobName;

	/** */
	public static String CreateBranchDialog_DialogTitle;

	/** */
	public static String CreateBranchDialog_OKButtonText;

	/** */
	public static String CreateBranchDialog_SelectRefMessage;

	/** */
	public static String CreateBranchDialog_WindowTitle;

	/** */
	public static String CreateBranchPage_BranchNameLabel;

	/** */
	public static String CreateBranchPage_CheckingOutMessage;

	/** */
	public static String CreateBranchPage_CheckoutButton;

	/** */
	public static String CreateBranchPage_ChooseBranchAndNameMessage;

	/** */
	public static String CreateBranchPage_ChooseNameMessage;

	/** */
	public static String CreateBranchPage_CreatingBranchMessage;

	/** */
	public static String CreateBranchPage_LocalBranchWarningText;

	/** */
	public static String CreateBranchPage_LocalBranchWarningTooltip;

	/** */
	public static String CreateBranchPage_MergeRadioButton;

	/** */
	public static String CreateBranchPage_MissingSourceMessage;

	/** */
	public static String CreateBranchPage_NoneRadioButton;

	/** */
	public static String CreateBranchPage_PullMergeTooltip;

	/** */
	public static String CreateBranchPage_PullNoneTooltip;

	/** */
	public static String CreateBranchPage_PullRebaseTooltip;

	/** */
	public static String CreateBranchPage_PullStrategyGroupHeader;

	/** */
	public static String CreateBranchPage_PullStrategyTooltip;

	/** */
	public static String CreateBranchPage_RebaseRadioButton;

	/** */
	public static String CreateBranchPage_SourceBranchLabel;

	/** */
	public static String CreateBranchPage_SourceBranchTooltip;

	/** */
	public static String CreateBranchPage_SourceCommitLabel;

	/** */
	public static String CreateBranchPage_SourceCommitTooltip;

	/** */
	public static String CreateBranchPage_Title;

	/** */
	public static String CreateBranchWizard_CreationFailed;

	/** */
	public static String CreateBranchWizard_NewBranchTitle;

	/** */
	public static String CreateRepositoryPage_BareCheckbox;

	/** */
	public static String CreateRepositoryPage_BrowseButton;

	/** */
	public static String CreateRepositoryPage_DirectoryLabel;

	/** */
	public static String CreateRepositoryPage_MissingNameMessage;

	/** */
	public static String CreateRepositoryPage_NotADirectoryMessage;

	/** */
	public static String CreateRepositoryPage_NotEmptyMessage;

	/** */
	public static String CreateRepositoryPage_PageMessage;

	/** */
	public static String CreateRepositoryPage_PageTitle;

	/** */
	public static String CreateRepositoryPage_PleaseSelectDirectoryMessage;

	/** */
	public static String CreateRepositoryPage_PleaseUseAbsoluePathMessage;

	/** */
	public static String CreateRepositoryPage_RepositoryNameLabel;

	/** */
	public static String PushResultDialog_ConfigureButton;

	/** */
	public static String PushResultTable_columnStatusRepo;

	/** */
	public static String PushResultTable_columnDst;

	/** */
	public static String PushResultTable_columnSrc;

	/** */
	public static String PushResultTable_columnMode;

	/** */
	public static String PushResultTable_MesasgeText;

	/** */
	public static String PushResultTable_statusUnexpected;

	/** */
	public static String PushResultTable_statusConnectionFailed;

	/** */
	public static String PushResultTable_statusDetailChanged;

	/** */
	public static String PushResultTable_refNonExisting;

	/** */
	public static String PushResultTable_repository;

	/** */
	public static String PushResultTable_statusDetailDeleted;

	/** */
	public static String PushResultTable_statusDetailNonFastForward;

	/** */
	public static String PushResultTable_statusDetailNoDelete;

	/** */
	public static String PushResultTable_statusDetailNonExisting;

	/** */
	public static String PushResultTable_statusDetailForcedUpdate;

	/** */
	public static String PushResultTable_statusDetailFastForward;

	/** */
	public static String PushResultTable_statusRemoteRejected;

	/** */
	public static String PushResultTable_statusRejected;

	/** */
	public static String PushResultTable_statusNoMatch;

	/** */
	public static String PushResultTable_statusUpToDate;

	/** */
	public static String PushResultTable_statusOkDeleted;

	/** */
	public static String PushResultTable_statusOkNewBranch;

	/** */
	public static String PushResultTable_statusOkNewTag;

	/** */
	public static String ResultDialog_title;

	/** */
	public static String ResultDialog_label;

	/** */
	public static String FetchAction_wrongURITitle;

	/** */
	public static String FetchAction_wrongURIMessage;

	/** */
	public static String FetchOperationUI_FetchJobName;

	/** */
	public static String FetchDestinationPage_CouldNotGetBranchesMessage;

	/** */
	public static String FetchDestinationPage_DestinationLabel;

	/** */
	public static String FetchDestinationPage_ForceCheckbox;

	/** */
	public static String FetchDestinationPage_PageMessage;

	/** */
	public static String FetchDestinationPage_PageTitle;

	/** */
	public static String FetchDestinationPage_RepositoryLabel;

	/** */
	public static String FetchDestinationPage_SourceLabel;

	/** */
	public static String FetchDestinationPage_TrackingBranchNotFoundMessage;

	/** */
	public static String FetchGerritChangePage_AfterFetchGroup;

	/** */
	public static String FetchGerritChangePage_BranchNameText;

	/** */
	public static String FetchGerritChangePage_ChangeLabel;

	/** */
	public static String FetchGerritChangePage_CheckingOutTaskName;

	/** */
	public static String FetchGerritChangePage_CheckoutRadio;

	/** */
	public static String FetchGerritChangePage_ContentAssistDescription;

	/** */
	public static String FetchGerritChangePage_ContentAssistTooltip;

	/** */
	public static String FetchGerritChangePage_CreatingBranchTaskName;

	/** */
	public static String FetchGerritChangePage_CreatingTagTaskName;

	/** */
	public static String FetchGerritChangePage_ExistingRefMessage;

	/** */
	public static String FetchGerritChangePage_FetchingTaskName;

	/** */
	public static String FetchGerritChangePage_GeneratedTagMessage;

	/** */
	public static String FetchGerritChangePage_GetChangeTaskName;

	/** */
	public static String FetchGerritChangePage_LocalBranchRadio;

	/** */
	public static String FetchGerritChangePage_MissingChangeMessage;

	/** */
	public static String FetchGerritChangePage_PageMessage;

	/** */
	public static String FetchGerritChangePage_PageTitle;

	/** */
	public static String FetchGerritChangePage_ProvideRefNameMessage;

	/** */
	public static String FetchGerritChangePage_SuggestedRefNamePattern;

	/** */
	public static String FetchGerritChangePage_TagNameText;

	/** */
	public static String FetchGerritChangePage_TagRadio;

	/** */
	public static String FetchGerritChangePage_UpdateRadio;

	/** */
	public static String FetchGerritChangePage_UriLabel;

	/** */
	public static String FetchGerritChangeWizard_WizardTitle;

	/** */
	public static String FetchResultDialog_ConfigureButton;

	/** */
	public static String FetchResultDialog_labelEmptyResult;

	/** */
	public static String FetchResultDialog_labelNonEmptyResult;

	/** */
	public static String FetchResultDialog_title;

	/** */
	public static String FetchResultTable_counterCommits;

	/** */
	public static String FetchResultTable_statusDetailCouldntLock;

	/** */
	public static String FetchResultTable_statusDetailFastForward;

	/** */
	public static String FetchResultTable_statusDetailIOError;

	/** */
	public static String FetchResultTable_statusDetailNonFastForward;

	/** */
	public static String FetchResultTable_statusIOError;

	/** */
	public static String FetchResultTable_statusLockFailure;

	/** */
	public static String FetchResultTable_statusNewBranch;

	/** */
	public static String FetchResultTable_statusNew;

	/** */
	public static String FetchResultTable_statusNewTag;

	/** */
	public static String FetchResultTable_statusRejected;

	/** */
	public static String FetchResultTable_statusUnexpected;

	/** */
	public static String FetchResultTable_statusUpToDate;

	/** */
	public static String FetchSourcePage_GettingRemoteRefsTaskname;

	/** */
	public static String FetchSourcePage_PageMessage;

	/** */
	public static String FetchSourcePage_PageTitle;

	/** */
	public static String FetchSourcePage_RefNotFoundMessage;

	/** */
	public static String FetchSourcePage_RepositoryLabel;

	/** */
	public static String FetchSourcePage_SourceLabel;

	/** */
	public static String FetchWizard_cantSaveMessage;

	/** */
	public static String FetchWizard_cantSaveTitle;

	/** */
	public static String FetchWizard_windowTitleDefault;

	/** */
	public static String FetchWizard_windowTitleWithSource;

	/** */
	public static String FileDiffContentProvider_errorGettingDifference;

	/** */
	public static String FileRevisionEditorInput_NameAndRevisionTitle;

	/** */
	public static String FileTreeContentProvider_NonWorkspaceResourcesNode;

	/** */
	public static String FindToolbar_NextTooltip;

	/** */
	public static String FindToolbar_PreviousTooltip;

	/** */
	public static String FormatJob_buildingCommitInfo;

	/** */
	public static String WindowCachePreferencePage_title;

	/** */
	public static String WindowCachePreferencePage_packedGitWindowSize;

	/** */
	public static String WindowCachePreferencePage_packedGitLimit;

	/** */
	public static String WindowCachePreferencePage_deltaBaseCacheLimit;

	/** */
	public static String WindowCachePreferencePage_packedGitMMAP;

	/** */
	public static String BasicConfigurationDialog_DialogMessage;

	/** */
	public static String BasicConfigurationDialog_DialogTitle;

	/** */
	public static String BasicConfigurationDialog_UserEmailLabel;

	/** */
	public static String BasicConfigurationDialog_UserNameLabel;

	/** */
	public static String BasicConfigurationDialog_WindowTitle;

	/** */
	public static String BranchAction_branchFailed;

	/** */
	public static String BranchAction_cannotCheckout;

	/** */
	public static String BranchAction_checkingOut;

	/** */
	public static String BranchAction_repositoryState;

	/** */
	public static String BranchOperationUI_DetachedHeadTitle;

	/** */
	public static String BranchOperationUI_DetachedHeadMessage;

	/** */
	public static String BranchResultDialog_CheckoutConflictsMessage;

	/** */
	public static String BranchResultDialog_CheckoutConflictsTitle;

	/** */
	public static String CommitDialogPreferencePage_title;

	/** */
	public static String CommitDialogPreferencePage_hardWrapMessage;

	/** */
	public static String CommitDialogPreferencePage_hardWrapMessageTooltip;

	/** */
	public static String CommitDialogPreferencePage_footers;

	/** */
	public static String CommitDialogPreferencePage_formatting;

	/** */
	public static String CommitDialogPreferencePage_signedOffBy;

	/** */
	public static String CommitDialogPreferencePage_signedOffByTooltip;

	/** */
	public static String Decorator_exceptionMessage;

	/** */
	public static String DecoratorPreferencesPage_addVariablesTitle;

	/** */
	public static String DecoratorPreferencesPage_addVariablesAction;

	/** */
	public static String DecoratorPreferencesPage_addVariablesAction2;

	/** */
	public static String DecoratorPreferencesPage_addVariablesAction3;

	/** */
	public static String DecoratorPreferencesPage_recomputeAncestorDecorations;

	/** */
	public static String DecoratorPreferencesPage_recomputeAncestorDecorationsTooltip;

	/** */
	public static String DecoratorPreferencesPage_computeRecursiveLimit;

	/** */
	public static String DecoratorPreferencesPage_computeRecursiveLimitTooltip;

	/** */
	public static String DecoratorPreferencesPage_description;

	/** */
	public static String DecoratorPreferencesPage_preview;

	/** */
	public static String DecoratorPreferencesPage_fileFormatLabel;

	/** */
	public static String DecoratorPreferencesPage_folderFormatLabel;

	/** */
	public static String DecoratorPreferencesPage_projectFormatLabel;

	/** */
	public static String DecoratorPreferencesPage_generalTabFolder;

	/** */
	public static String DecoratorPreferencesPage_bindingResourceName;

	/** */
	public static String DecoratorPreferencesPage_bindingBranchName;

	/** */
	public static String DecoratorPreferencesPage_bindingDirtyFlag;

	/** */
	public static String DecoratorPreferencesPage_bindingStagedFlag;

	/** */
	public static String DecoratorPreferencesPage_bindingChangeSetAuthor;

	/** */
	public static String DecoratorPreferencesPage_bindingChangeSetCommitter;

	/** */
	public static String DecoratorPreferencesPage_bindingChangeSetDate;

	/** */
	public static String DecoratorPreferencesPage_bindingChangeSetShortMessage;

	/** */
	public static String DecoratorPreferencesPage_dateFormat;

	/** */
	public static String DecoratorPreferencesPage_dateFormatPreview;

	/** */
	public static String DecoratorPreferencesPage_wrongDateFormat;

	/** */
	public static String DecoratorPreferencesPage_selectVariablesToAdd;

	/** */
	public static String DecoratorPreferencesPage_otherDecorations;

	/** */
	public static String DecoratorPreferencesPage_changeSetLabelFormat;

	/** */
	public static String DecoratorPreferencesPage_textLabel;

	/** */
	public static String DecoratorPreferencesPage_iconLabel;

	/** */
	public static String DecoratorPreferencesPage_labelDecorationsLink;

	/** */
	public static String DecoratorPreferencesPage_iconsShowTracked;

	/** */
	public static String DecoratorPreferencesPage_iconsShowUntracked;

	/** */
	public static String DecoratorPreferencesPage_iconsShowStaged;

	/** */
	public static String DecoratorPreferencesPage_iconsShowConflicts;

	/** */
	public static String DecoratorPreferencesPage_iconsShowAssumeValid;

	/** */
	public static String DeleteBranchCommand_CannotDeleteCheckedOutBranch;

	/** */
	public static String DeleteBranchCommand_DeletingBranchesProgress;

	/** */
	public static String DeleteBranchDialog_DialogMessage;

	/** */
	public static String DeleteBranchDialog_DialogTitle;

	/** */
	public static String DeleteBranchDialog_WindowTitle;

	/** */
	public static String DeleteRepositoryConfirmDialog_DeleteRepositoryMessage;

	/** */
	public static String DeleteRepositoryConfirmDialog_DeleteRepositoryTitle;

	/** */
	public static String DeleteRepositoryConfirmDialog_DeleteRepositoryWindowTitle;

	/** */
	public static String DeleteRepositoryConfirmDialog_DeleteWorkingDirectoryCheckbox;

	/** */
	public static String IgnoreActionHandler_addToGitignore;

	/** */
	public static String RepositoriesView_BranchDeletionFailureMessage;

	/** */
	public static String RepositoriesView_Branches_Nodetext;

	/** */
	public static String RepositoriesView_ClipboardContentNoGitRepoMessage;

	/** */
	public static String RepositoriesView_ClipboardContentNotDirectoryMessage;

	/** */
	public static String RepositoriesView_ConfirmBranchDeletionMessage;

	/** */
	public static String RepositoriesView_ConfirmDeleteRemoteHeader;

	/** */
	public static String RepositoriesView_ConfirmDeleteRemoteMessage;

	/** */
	public static String RepositoriesView_ConfirmDeleteTitle;

	/** */
	public static String RepositoriesView_ConfirmProjectDeletion_Question;

	/** */
	public static String RepositoriesView_ConfirmProjectDeletion_WindowTitle;

	/** */
	public static String RepositoriesView_DeleteRepoDeterminProjectsMessage;

	/** */
	public static String RepositoriesView_Error_WindowTitle;

	/** */
	public static String RepositoriesView_ErrorHeader;

	/** */
	public static String RepositoriesView_ExceptionLookingUpRepoMessage;

	/** */
	public static String RepositoriesView_NothingToPasteMessage;

	/** */
	public static String RepositoriesView_PasteRepoAlreadyThere;

	/** */
	public static String RepositoriesView_RemotesNodeText;

	/** */
	public static String RepositoriesView_RenameBranchFailure;

	/** */
	public static String RepositoriesView_RenameBranchMessage;

	/** */
	public static String RepositoriesView_RenameBranchTitle;

	/** */
	public static String RepositoriesView_WorkingDir_treenode;

	/** */
	public static String RepositoriesViewContentProvider_ExceptionNodeText;

	/** */
	public static String RepositoriesViewLabelProvider_BareRepositoryMessage;

	/** */
	public static String RepositoriesViewLabelProvider_LocalNodetext;

	/** */
	public static String RepositoriesViewLabelProvider_RemoteTrackingNodetext;

	/** */
	public static String RepositoriesViewLabelProvider_SymbolicRefNodeText;

	/** */
	public static String RepositoriesViewLabelProvider_TagsNodeText;

	/** */
	public static String DialogsPreferencePage_DetachedHeadCombo;

	/** */
	public static String DialogsPreferencePage_DontShowDialog;

	/** */
	public static String DialogsPreferencePage_HideConfirmationGroupHeader;

	/** */
	public static String DialogsPreferencePage_HomeDirWarning;

	/** */
	public static String DialogsPreferencePage_RebaseCheckbox;

	/** */
	public static String DialogsPreferencePage_ShowDialog;

	/** */
	public static String DialogsPreferencePage_ShowInitialConfigCheckbox;

	/** */
	public static String DiffEditorPage_TaskGeneratingDiff;

	/** */
	public static String DiffEditorPage_TaskUpdatingViewer;

	/** */
	public static String DiffEditorPage_Title;

	/** */
	public static String DiscardChangesAction_confirmActionTitle;

	/** */
	public static String DiscardChangesAction_confirmActionMessage;

	/** */
	public static String DiscardChangesAction_discardChanges;

	/** */
	public static String Disconnect_disconnect;

	/** */
	public static String GitCompareEditorInput_CompareResourcesTaskName;

	/** */
	public static String GitCompareEditorInput_EditorTitle;

	/** */
	public static String GitCompareEditorInput_EditorTitleMultipleResources;

	/** */
	public static String GitCompareEditorInput_EditorTitleSingleResource;

	/** */
	public static String GitCompareEditorInput_ResourcesInDifferentReposMessagge;

	/** */
	public static String GitCompareFileRevisionEditorInput_CompareInputTitle;

	/** */
	public static String GitCompareFileRevisionEditorInput_CompareTooltip;

	/** */
	public static String GitCompareFileRevisionEditorInput_CurrentRevision;

	/** */
	public static String GitCompareFileRevisionEditorInput_CurrentTitle;

	/** */
	public static String GitCompareFileRevisionEditorInput_contentIdentifier;

	/** */
	public static String GitCompareFileRevisionEditorInput_LocalHistoryLabel;

	/** */
	public static String GitCompareFileRevisionEditorInput_LocalLabel;

	/** */
	public static String GitCompareFileRevisionEditorInput_LocalRevision;

	/** */
	public static String GitCompareFileRevisionEditorInput_RevisionLabel;

	/** */
	public static String GitCompareFileRevisionEditorInput_LocalVersion;

	/** */
	public static String GitCompareFileRevisionEditorInput_StagedVersion;

	/** */
	public static String GitCreateGeneralProjectPage_DirLabel;

	/** */
	public static String GitCreateGeneralProjectPage_DirNotExistMessage;

	/** */
	public static String GitCreateGeneralProjectPage_EnterProjectNameMessage;

	/** */
	public static String GitCreateGeneralProjectPage_FileExistsInDirMessage;

	/** */
	public static String GitCreateGeneralProjectPage_FileNotDirMessage;

	/** */
	public static String GitCreateGeneralProjectPage_PorjectAlreadyExistsMessage;

	/** */
	public static String GitCreateGeneralProjectPage_ProjectNameLabel;

	/** */
	public static String GitCreatePatchWizard_Browse;

	/** */
	public static String GitCreatePatchWizard_Clipboard;

	/** */
	public static String GitCreatePatchWizard_CreatePatchTitle;

	/** */
	public static String GitCreatePatchWizard_File;

	/** */
	public static String GitCreatePatchWizard_GitFormat;

	/** */
	public static String GitCreatePatchWizard_InternalError;

	/** */
	public static String GitCreatePatchWizard_SelectLocationDescription;

	/** */
	public static String GitCreatePatchWizard_SelectLocationTitle;

	/** */
	public static String GitCreatePatchWizard_SelectOptionsDescription;

	/** */
	public static String GitCreatePatchWizard_SelectOptionsTitle;

	/** */
	public static String GitCreatePatchWizard_FilesystemError;

	/** */
	public static String GitCreatePatchWizard_FilesystemInvalidError;

	/** */
	public static String GitCreatePatchWizard_FilesystemDirectoryError;

	/** */
	public static String GitCreatePatchWizard_FilesystemDirectoryNotExistsError;

	/** */
	public static String GitCreateProjectViaWizardWizard_AbortedMessage;

	/** */
	public static String GitCreateProjectViaWizardWizard_WizardTitle;

	/** */
	public static String GitImportWithDirectoriesPage_PageMessage;

	/** */
	public static String GitImportWithDirectoriesPage_PageTitle;

	/** */
	public static String GitImportWithDirectoriesPage_SelectFolderMessage;

	/** */
	public static String GitImportWizard_WizardTitle;

	/** */
	public static String GitSelectRepositoryPage_AddButton;

	/** */
	public static String GitSelectRepositoryPage_AddTooltip;

	/** */
	public static String GitSelectRepositoryPage_CloneButton;

	/** */
	public static String GitSelectRepositoryPage_CloneTooltip;

	/** */
	public static String GitSelectRepositoryPage_NoRepoFoundMessage;

	/** */
	public static String GitSelectRepositoryPage_PageMessage;

	/** */
	public static String GitSelectRepositoryPage_PageTitle;

	/** */
	public static String GitSelectRepositoryPage_PleaseSelectMessage;

	/** */
	public static String GitSelectWizardPage_ImportAsGeneralButton;

	/** */
	public static String GitSelectWizardPage_ImportExistingButton;

	/** */
	public static String GitSelectWizardPage_ProjectCreationHeader;

	/** */
	public static String GitSelectWizardPage_UseNewProjectsWizardButton;

	/** */
	public static String MergeAction_CannotMerge;

	/** */
	public static String MergeAction_HeadIsNoBranch;

	/** */
	public static String MergeAction_JobNameMerge;

	/** */
	public static String MergeAction_MergeCanceledMessage;

	/** */
	public static String MergeAction_MergeCanceledTitle;

	/** */
	public static String MergeAction_MergeResultTitle;

	/** */
	public static String MergeAction_WrongRepositoryState;

	/** */
	public static String MergeModeDialog_DialogTitle;

	/** */
	public static String MergeModeDialog_DontAskAgainLabel;

	/** */
	public static String MergeModeDialog_MergeMode_1_Label;

	/** */
	public static String MergeModeDialog_MergeMode_2_Label;

	/** */
	public static String MergeResultDialog_couldNotFindCommit;

	/** */
	public static String MergeResultDialog_description;

	/** */
	public static String MergeResultDialog_id;

	/** */
	public static String MergeResultDialog_mergeInput;

	/** */
	public static String MergeResultDialog_mergeResult;

	/** */
	public static String MergeResultDialog_newHead;

	/** */
	public static String MergeResultDialog_result;

	/** */
	public static String MergeTargetSelectionDialog_ButtonMerge;

	/** */
	public static String MergeTargetSelectionDialog_SelectRef;

	/** */
	public static String MergeTargetSelectionDialog_TitleMerge;

	/** */
	public static String MixedResetToRevisionAction_mixedReset;

	/** */
	public static String MultiPullResultDialog_DetailsButton;

	/** */
	public static String MultiPullResultDialog_FetchStatusColumnHeader;

	/** */
	public static String MultiPullResultDialog_MergeResultMessage;

	/** */
	public static String MultiPullResultDialog_NothingFetchedStatus;

	/** */
	public static String MultiPullResultDialog_NothingUpdatedStatus;

	/** */
	public static String MultiPullResultDialog_OkStatus;

	/** */
	public static String MultiPullResultDialog_FailedStatus;

	/** */
	public static String MultiPullResultDialog_OverallStatusColumnHeader;

	/** */
	public static String MultiPullResultDialog_RebaseResultMessage;

	/** */
	public static String MultiPullResultDialog_RepositoryColumnHeader;

	/** */
	public static String MultiPullResultDialog_UnknownStatus;

	/** */
	public static String MultiPullResultDialog_UpdatedMessage;

	/** */
	public static String MultiPullResultDialog_UpdateStatusColumnHeader;

	/** */
	public static String MultiPullResultDialog_WindowTitle;

	/** */
	public static String UIIcons_errorDeterminingIconBase;

	/** */
	public static String UIIcons_errorLoadingPluginImage;

	/** */
	public static String UIUtils_CollapseAll;

	/** */
	public static String UIUtils_ExpandAll;

	/** */
	public static String UIUtils_PressShortcutMessage;

	/** */
	public static String UIUtils_StartTypingForPreviousValuesMessage;

	/** */
	public static String Untrack_untrack;

	/** */
	public static String TagAction_cannotCheckout;

	/** */
	public static String TagAction_cannotGetBranchName;

	/** */
	public static String TagAction_repositoryState;

	/** */
	public static String TagAction_errorWhileGettingRevCommits;

	/** */
	public static String TagAction_unableToResolveHeadObjectId;

	/** */
	public static String TagAction_creating;

	/** */
	public static String TagAction_taggingFailed;

	/** */
	public static String CreateTagDialog_tagName;

	/** */
	public static String CreateTagDialog_tagMessage;

	/** */
	public static String CreateTagDialog_questionNewTagTitle;

	/** */
	public static String CreateTagDialog_overwriteTag;

	/** */
	public static String CreateTagDialog_overwriteTagToolTip;

	/** */
	public static String CreateTagDialog_existingTags;

	/** */
	public static String CreateTagDialog_advanced;

	/** */
	public static String CreateTagDialog_advancedToolTip;

	/** */
	public static String CreateTagDialog_advancedMessage;

	/** */
	public static String CreateTagDialog_tagNameToolTip;

	/** */
	public static String CreateTagDialog_clearButton;

	/** */
	public static String CreateTagDialog_clearButtonTooltip;

	/** */
	public static String CreateTagDialog_CreateTagOnCommitTitle;

	/** */
	public static String CreateTagDialog_ExceptionRetrievingTagsMessage;

	/** */
	public static String CreateTagDialog_GetTagJobName;

	/** */
	public static String CreateTagDialog_LightweightTagMessage;

	/** */
	public static String CreateTagDialog_LoadingMessageText;

	/** */
	public static String CreateTagDialog_Message;

	/** */
	public static String CreateTagDialog_NewTag;

	/** */
	public static String CommitCombo_showSuggestedCommits;

	/** */
	public static String CommitCommand_committingNotPossible;

	/** */
	public static String CommitCommand_noProjectsImported;

	/**
	 * Do not in-line this into the static initializer as the
	 * "Find Broken Externalized Strings" tool will not be
	 * able to find the corresponding bundle file.
	 */
	private static final String BUNDLE_NAME = "org.eclipse.egit.ui.uitext"; //$NON-NLS-1$

	/** */
	public static String CommitAction_commit;

	/** */
	public static String CommitAction_ErrorReadingMergeMsg;

	/** */
	public static String CommitAction_MergeHeadErrorMessage;

	/** */
	public static String CommitAction_MergeHeadErrorTitle;

	/** */
	public static String CommitActionHandler_calculatingChanges;

	/** */
	public static String CommitActionHandler_repository;

	/** */
	public static String CommitEditor_couldNotShowRepository;

	/** */
	public static String CommitEditor_showGitRepo;

	/** */
	public static String CommitEditor_TitleHeader;

	/** */
	public static String CommitEditorInput_Name;

	/** */
	public static String CommitEditorInput_ToolTip;

	/** */
	public static String CommitEditorPage_LabelAuthor;

	/** */
	public static String CommitEditorPage_LabelCommitter;

	/** */
	public static String CommitEditorPage_LabelParent;

	/** */
	public static String CommitEditorPage_LabelTags;

	/** */
	public static String CommitEditorPage_SectionBranches;

	/** */
	public static String CommitEditorPage_SectionFiles;

	/** */
	public static String CommitEditorPage_SectionMessage;

	/** */
	public static String CommitEditorPage_Title;

	/** */
	public static String CommitEditorPage_TooltipAuthor;

	/** */
	public static String CommitEditorPage_TooltipCommitter;

	/** */
	public static String CommitEditorPage_TooltipSignedOffByAuthor;

	/** */
	public static String CommitEditorPage_TooltipSignedOffByCommitter;

	/** */
	public static String CommitFileDiffViewer_CanNotOpenCompareEditorTitle;

	/** */
	public static String CommitFileDiffViewer_CompareMenuLabel;

	/** */
	public static String CommitFileDiffViewer_FileDoesNotExist;

	/** */
	public static String CommitFileDiffViewer_MergeCommitMultiAncestorMessage;

	/** */
	public static String CommitFileDiffViewer_OpenInEditorMenuLabel;

	/** */
	public static String CommitFileDiffViewer_notContainedInCommit;

	/** */
	public static String CommitFileDiffViewer_SelectOneCommitMessage;

	/** */
	public static String CommitGraphTable_CommitId;

	/** */
	public static String CommitGraphTable_Committer;

	/** */
	public static String CommitGraphTable_CompareWithEachOtherInTreeMenuLabel;

	/** */
	public static String CommitGraphTable_OpenCommitLabel;

	/** */
	public static String GitSynchronizeWizard_synchronize;

	/** */
	public static String GitBranchSynchronizeWizardPage_title;

	/** */
	public static String GitBranchSynchronizeWizardPage_description;

	/** */
	public static String GitBranchSynchronizeWizardPage_repositories;

	/** */
	public static String GitBranchSynchronizeWizardPage_destination;

	/** */
	public static String GitBranchSynchronizeWizardPage_includeUncommitedChanges;

	/** */
	public static String GitBranchSynchronizeWizardPage_fetchChangesFromRemote;

	/** */
	public static String GitBranchSynchronizeWizardPage_selectAll;

	/** */
	public static String GitBranchSynchronizeWizardPage_deselectAll;

	/** */
	public static String GitTraceConfigurationDialog_ApplyButton;

	/** */
	public static String GitTraceConfigurationDialog_DefaultButton;

	/** */
	public static String GitTraceConfigurationDialog_DialogTitle;

	/** */
	public static String GitTraceConfigurationDialog_LocationHeader;

	/** */
	public static String GitTraceConfigurationDialog_MainSwitchNodeText;

	/** */
	public static String GitTraceConfigurationDialog_OpenInEditorButton;

	/** */
	public static String GitTraceConfigurationDialog_PlatformSwitchCheckbox;

	/** */
	public static String GitTraceConfigurationDialog_PlatformTraceDisabledMessage;

	/** */
	public static String GitTraceConfigurationDialog_ShellTitle;

	/** */
	public static String GitTraceConfigurationDialog_TraceFileLocationLabel;

	/** */
	public static String LocalFileRevision_CurrentVersion;

	/** */
	public static String LocalFileRevision_currentVersionTag;

	/** */
	public static String LoginDialog_changeCredentials;

	/** */
	public static String LoginDialog_login;

	/** */
	public static String LoginDialog_password;

	/** */
	public static String LoginDialog_repository;

	/** */
	public static String LoginDialog_storeInSecureStore;

	/** */
	public static String LoginDialog_user;

	/** */
	public static String LoginService_readingCredentialsFailed;

	/** */
	public static String LoginService_storingCredentialsFailed;

	/** */
	public static String NewRemoteDialog_ConfigurationMessage;

	/** */
	public static String NewRemoteDialog_DialogTitle;

	/** */
	public static String NewRemoteDialog_FetchRadio;

	/** */
	public static String NewRemoteDialog_NameLabel;

	/** */
	public static String NewRemoteDialog_PushRadio;

	/** */
	public static String NewRemoteDialog_RemoteAlreadyExistsMessage;

	/** */
	public static String NewRemoteDialog_WindowTitle;

	/** */
	public static String NewRepositoryWizard_WizardTitle;

	/** */
	public static String NonDeletedFilesDialog_NonDeletedFilesMessage;

	/** */
	public static String NonDeletedFilesDialog_NonDeletedFilesTitle;

	/** */
	public static String NonDeletedFilesDialog_RetryDeleteButton;

	/** */
	public static String NonDeletedFilesTree_FileSystemPathsButton;

	/** */
	public static String NonDeletedFilesTree_RepoRelativePathsButton;

	/** */
	public static String NonDeletedFilesTree_RepositoryLabel;

	/** */
	public static String NonDeletedFilesTree_ResourcePathsButton;

	/** */
	public static String NoteDetailsPage_ContentSection;

	/** */
	public static String NotesBlock_NotesSection;

	/** */
	public static String NotesEditorPage_Title;

	/** */
	public static String OpenWorkingFileAction_text;

	/** */
	public static String OpenWorkingFileAction_tooltip;

	/** */
	public static String OpenWorkingFileAction_openWorkingFileShellTitle;

	/** */
	public static String RemoteConnectionPreferencePage_TimeoutLabel;

	/** */
	public static String RemoteConnectionPreferencePage_ZeroValueTooltip;

	/** */
	public static String RemoteSelectionCombo_sourceName;

	/** */
	public static String RemoteSelectionCombo_sourceRef;

	/** */
	public static String RefreshPreferencesPage_RefreshOnlyWhenActive;

	/** */
	public static String RefreshPreferencesPage_RefreshWhenIndexChange;

	/** */
	public static String RefUpdateElement_CommitCountDecoration;

	/** */
	public static String RefUpdateElement_CommitRangeDecoration;

	/** */
	public static String RefUpdateElement_UrisDecoration;

	/** */
	public static String RemoteSelectionCombo_destinationName;

	/** */
	public static String RemoteSelectionCombo_destinationRef;

	/** */
	public static String RemoveCommand_ConfirmDeleteBareRepositoryMessage;

	/** */
	public static String RemoveCommand_ConfirmDeleteBareRepositoryTitle;

	/** */
	public static String RenameBranchCommand_WrongNameMessage;

	/** */
	public static String RenameBranchDialog_DialogMessage;

	/** */
	public static String RenameBranchDialog_DialogTitle;

	/** */
	public static String RenameBranchDialog_NewNameInputDialogPrompt;

	/** */
	public static String RenameBranchDialog_RenameBranchDialogNewNameInputWindowTitle;

	/** */
	public static String RenameBranchDialog_RenameButtonLabel;

	/** */
	public static String RenameBranchDialog_RenameErrorMessage;

	/** */
	public static String RenameBranchDialog_WindowTitle;

	/** */
	public static String RevertHandler_AlreadyRevertedMessae;

	/** */
	public static String RevertHandler_NoRevertTitle;

	/** */
	public static String RevertOperation_Failed;

	/** */
	public static String RevertOperation_InternalError;

	/** */
	public static String SelectSynchronizeResourceDialog_header;

	/** */
	public static String SelectSynchronizeResourceDialog_selectProject;

	/** */
	public static String SelectSynchronizeResourceDialog_srcRef;

	/** */
	public static String SelectSynchronizeResourceDialog_dstRef;

	/** */
	public static String SelectSynchronizeResourceDialog_includeUncommitedChanges;

	/** */
	public static String SelectUriWiazrd_Title;

	/** */
	public static String SimpleConfigureFetchDialog_AddRefSpecButton;

	/** */
	public static String SimpleConfigureFetchDialog_AdvancedCompositeButton;

	/** */
	public static String SimpleConfigureFetchDialog_BranchLabel;

	/** */
	public static String SimpleConfigureFetchDialog_ChangeRefSpecButton;

	/** */
	public static String SimpleConfigureFetchDialog_ChangeUriButton;

	/** */
	public static String SimpleConfigureFetchDialog_CopyRefSpecButton;

	/** */
	public static String SimpleConfigureFetchDialog_DeleteRefSpecButton;

	/** */
	public static String SimpleConfigureFetchDialog_DeleteUriButton;

	/** */
	public static String SimpleConfigureFetchDialog_DetachedHeadMessage;

	/** */
	public static String SimpleConfigureFetchDialog_DialogMessage;

	/** */
	public static String SimpleConfigureFetchDialog_DialogTitle;

	/** */
	public static String SimpleConfigureFetchDialog_DryRunButton;

	/** */
	public static String SimpleConfigureFetchDialog_EditAdvancedButton;

	/** */
	public static String SimpleConfigureFetchDialog_EmptyClipboardMessage;

	/** */
	public static String SimpleConfigureFetchDialog_InvalidRefDialogMessage;

	/** */
	public static String SimpleConfigureFetchDialog_InvalidRefDialogTitle;

	/** */
	public static String SimpleConfigureFetchDialog_MissingMappingMessage;

	/** */
	public static String SimpleConfigureFetchDialog_MissingUriMessage;

	/** */
	public static String SimpleConfigureFetchDialog_NothingToPasteMessage;

	/** */
	public static String SimpleConfigureFetchDialog_NotRefSpecDialogMessage;

	/** */
	public static String SimpleConfigureFetchDialog_NotRefSpecDialogTitle;

	/** */
	public static String SimpleConfigureFetchDialog_PateRefSpecButton;

	/** */
	public static String SimpleConfigureFetchDialog_RefMappingGroup;

	/** */
	public static String SimpleConfigureFetchDialog_RefSpecLabel;

	/** */
	public static String SimpleConfigureFetchDialog_RemoteGroupHeader;

	/** */
	public static String SimpleConfigureFetchDialog_RepositoryLabel;

	/** */
	public static String SimpleConfigureFetchDialog_ReusedRemoteWarning;

	/** */
	public static String SimpleConfigureFetchDialog_RevertButton;

	/** */
	public static String SimpleConfigureFetchDialog_SaveAndFetchButton;

	/** */
	public static String SimpleConfigureFetchDialog_SaveButton;

	/** */
	public static String SimpleConfigureFetchDialog_UriLabel;

	/** */
	public static String SimpleConfigureFetchDialog_WindowTitle;

	/** */
	public static String SimpleConfigurePushDialog_AddPushUriButton;

	/** */
	public static String SimpleConfigurePushDialog_AddRefSpecButton;

	/** */
	public static String SimpleConfigurePushDialog_AdvancedButton;

	/** */
	public static String SimpleConfigurePushDialog_BranchLabel;

	/** */
	public static String SimpleConfigurePushDialog_ChangePushUriButton;

	/** */
	public static String SimpleConfigurePushDialog_ChangeRefSpecButton;

	/** */
	public static String SimpleConfigurePushDialog_ChangeUriButton;

	/** */
	public static String SimpleConfigurePushDialog_CopyRefSpecButton;

	/** */
	public static String SimpleConfigurePushDialog_DeletePushUriButton;

	/** */
	public static String SimpleConfigurePushDialog_DeleteRefSpecButton;

	/** */
	public static String SimpleConfigurePushDialog_DeleteUriButton;

	/** */
	public static String SimpleConfigurePushDialog_DetachedHeadMessage;

	/** */
	public static String SimpleConfigurePushDialog_DialogMessage;

	/** */
	public static String SimpleConfigurePushDialog_DialogTitle;

	/** */
	public static String SimpleConfigurePushDialog_DryRunButton;

	/** */
	public static String SimpleConfigurePushDialog_EditAdvancedButton;

	/** */
	public static String SimpleConfigurePushDialog_EmptyClipboardDialogMessage;

	/** */
	public static String SimpleConfigurePushDialog_EmptyClipboardDialogTitle;

	/** */
	public static String SimpleConfigurePushDialog_InvalidRefDialogMessage;

	/** */
	public static String SimpleConfigurePushDialog_InvalidRefDialogTitle;

	/** */
	public static String SimpleConfigurePushDialog_MissingUriMessage;

	/** */
	public static String SimpleConfigurePushDialog_NoRefSpecDialogMessage;

	/** */
	public static String SimpleConfigurePushDialog_NoRefSpecDialogTitle;

	/** */
	public static String SimpleConfigurePushDialog_PasteRefSpecButton;

	/** */
	public static String SimpleConfigurePushDialog_PushAllBranchesMessage;

	/** */
	public static String SimpleConfigurePushDialog_PushUrisLabel;

	/** */
	public static String SimpleConfigurePushDialog_RefMappingGroup;

	/** */
	public static String SimpleConfigurePushDialog_RefSpecLabel;

	/** */
	public static String SimpleConfigurePushDialog_RemoteGroupTitle;

	/** */
	public static String SimpleConfigurePushDialog_RepositoryLabel;

	/** */
	public static String SimpleConfigurePushDialog_ReusedOriginWarning;

	/** */
	public static String SimpleConfigurePushDialog_RevertButton;

	/** */
	public static String SimpleConfigurePushDialog_SaveAndPushButton;

	/** */
	public static String SimpleConfigurePushDialog_SaveButton;

	/** */
	public static String SimpleConfigurePushDialog_UriGroup;

	/** */
	public static String SimpleConfigurePushDialog_URILabel;

	/** */
	public static String SimpleConfigurePushDialog_UseUriForPushUriMessage;

	/** */
	public static String SimpleConfigurePushDialog_WindowTitle;

	/** */
	public static String SimpleFetchActionHandler_NothingToFetchDialogMessage;

	/** */
	public static String SimpleFetchActionHandler_NothingToFetchDialogTitle;

	/** */
	public static String SimpleFetchRefSpecWizard_WizardTitle;

	/** */
	public static String SimplePushActionHandler_NothingToPushDialogMessage;

	/** */
	public static String SimplePushActionHandler_NothingToPushDialogTitle;

	/** */
	public static String SkipRebaseCommand_CancelDialogMessage;

	/** */
	public static String SkipRebaseCommand_JobName;

	/** */
	public static String SwitchToMenu_NewBranchMenuLabel;

	/** */
	public static String SwitchToMenu_OtherMenuLabel;

	/** */
	public static String SynchronizeWithAction_localRepoName;

	/** */
	public static String SynchronizeWithAction_tagsName;

	/** */
	public static String ValidationUtils_CanNotResolveRefMessage;

	/** */
	public static String ValidationUtils_InvalidRefNameMessage;

	/** */
	public static String ValidationUtils_RefAlreadyExistsMessage;

	/** */
	public static String ValidationUtils_PleaseEnterNameMessage;

	/** */
	public static String GitMergeEditorInput_CalculatingDiffTaskName;

	/** */
	public static String GitMergeEditorInput_CheckingResourcesTaskName;

	/** */
	public static String GitMergeEditorInput_MergeEditorTitle;

	/** */
	public static String GitMergeEditorInput_WorkspaceHeader;

	/** */
	public static String GitModelIndex_index;

	/** */
	public static String GitModelWorkingTree_workingTree;

	/** */
	public static String CommitFileDiffViewer_OpenWorkingTreeVersionInEditorMenuLabel;

	/** */
	public static String CommitResultLabelProvider_SectionAuthor;

	/** */
	public static String CommitResultLabelProvider_SectionMessage;

	/** */
	public static String CommitResultLabelProvider_SectionRepository;

	/** */
	public static String CommitSearchPage_Author;

	/** */
	public static String CommitSearchPage_CaseSensitive;

	/** */
	public static String CommitSearchPage_CheckAll;

	/** */
	public static String CommitSearchPage_CommitId;

	/** */
	public static String CommitSearchPage_Committer;

	/** */
	public static String CommitSearchPage_ContainingText;

	/** */
	public static String CommitSearchPage_ContainingTextHint;

	/** */
	public static String CommitSearchPage_Message;

	/** */
	public static String CommitSearchPage_ParentIds;

	/** */
	public static String CommitSearchPage_RegularExpression;

	/** */
	public static String CommitSearchPage_Repositories;

	/** */
	public static String CommitSearchPage_Scope;

	/** */
	public static String CommitSearchPage_SearchAllBranches;

	/** */
	public static String CommitSearchPage_TreeId;

	/** */
	public static String CommitSearchPage_UncheckAll;

	/** */
	public static String CommitSearchQuery_Label;

	/** */
	public static String CommitSearchQuery_TaskSearchCommits;

	/** */
	public static String CommitSearchResult_LabelPlural;

	/** */
	public static String CommitSearchResult_LabelSingle;

	/** */
	public static String CommitSelectionDialog_BuildingCommitListMessage;

	/** */
	public static String CommitSelectionDialog_DialogMessage;

	/** */
	public static String CommitSelectionDialog_DialogTitle;

	/** */
	public static String CommitSelectionDialog_FoundCommitsMessage;

	/** */
	public static String CommitSelectionDialog_IncompleteListMessage;

	/** */
	public static String CommitSelectionDialog_LinkSearch;

	/** */
	public static String CommitSelectionDialog_Message;

	/** */
	public static String CommitSelectionDialog_SectionMessage;

	/** */
	public static String CommitSelectionDialog_SectionRepo;

	/** */
	public static String CommitSelectionDialog_TaskSearching;

	/** */
	public static String CommitSelectionDialog_Title;

	/** */
	public static String CommitSelectionDialog_WindowTitle;

	/** */
	public static String CommitUI_commitFailed;

	/** */
	public static String EgitUiEditorUtils_openFailed;

	/** */
	public static String GitActionContributor_ExpandAll;

	/** */
	public static String GitActionContributor_Push;

	/** */
	public static String GitActionContributor_Pull;

	/** */
	public static String GitLabelProvider_UnableToRetrieveLabel;

	/** */
	public static String GitVariableResolver_InternalError;

	/** */
	public static String GitVariableResolver_NoSelectedResource;

	/** */
	public static String GitVariableResolver_VariableReferencesNonExistentResource;

	/** */
	public static String DecoratableResourceHelper_noHead;

	/** */
	public static String StagingView_UnstagedChanges;

	/** */
	public static String StagingView_ShowFileNamesFirst;

	/** */
	public static String StagingView_StagedChanges;

	/** */
	public static String StagingView_CommitMessage;

	/** */
	public static String StagingView_Committer;

	/** */
	public static String StagingView_Author;

	/** */
	public static String StagingView_Ammend_Previous_Commit;

	/** */
	public static String StagingView_Add_Signed_Off_By;

	/** */
	public static String StagingView_Add_Change_ID;

	/** */
	public static String StagingView_Commit;

	/** */
	public static String StagingView_commitFailed;

	/** */
	public static String StagingView_committingNotPossible;

	/** */
	public static String StagingView_headCommitChanged;

	/** */
	public static String StagingView_noStagedFiles;

	/** */
	public static String StagingView_NoSelectionTitle;

	/** */
	public static String StagingView_OpenNewCommits;

	/** */
	public static String StagingView_ColumnLayout;

	/** */
	public static String StagingView_IndexDiffReload;

	/** */
	public static String StagingView_Refresh;

	/** */
	public static String StagingView_LinkSelection;

	/** */
	public static String StagingView_exceptionTitle;

	/** */
	public static String StagingView_exceptionMessage;

	/** */
	public static String StagingView_UnstageItemMenuLabel;

	/** */
	public static String SynchronizeWithMenu_custom;

	/** */
	public static String SynchronizeFetchJob_JobName;

	/** */
	public static String SynchronizeFetchJob_TaskName;

	/** */
	public static String SynchronizeFetchJob_SubTaskName;

	/** */
	public static String SynchronizeFetchJob_FetchFailedTitle;

	/** */
	public static String SynchronizeFetchJob_FetchFailedMessage;

	/** */
	public static String EGitCredentialsProvider_question;

	/** */
	public static String EGitCredentialsProvider_information;

	/** */
	public static String CustomPromptDialog_provide_information_for;

	/** */
	public static String CustomPromptDialog_information_about;

	static {
		initializeMessages(BUNDLE_NAME, UIText.class);
	}

}
