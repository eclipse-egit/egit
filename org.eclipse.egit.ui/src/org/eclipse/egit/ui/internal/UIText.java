/*******************************************************************************
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, 2013 Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2011, Daniel Megert <daniel_megert@ch.ibm.com>
 * Copyright (C) 2012, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2012, Daniel Megert <daniel_megert@ch.ibm.com>
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 * Copyright (C) 2012, Laurent Goubet <laurent.goubet@obeo.fr>
 * Copyright (C) 2012, Gunnar Wagenknecht <gunnar@wagenknecht.org>
 * Copyright (C) 2013, Ben Hammen <hammenb@gmail.com>
 * Copyright (C) 2014, Marc Khouzam <marc.khouzam@ericsson.com>
 * Copyright (C) 2014, Red Hat Inc.
 * Copyright (C) 2014, Axel Richard <axel.richard@obeo.fr>
 * Copyright (C) 2015, SAP SE (Christian Georgi <christian.georgi@sap.com>)
 * Copyright (C) 2015, Jan-Ove Weichel <ovi.weichel@gmail.com>
 * Copyright (C) 2015, Laurent Delaigue <laurent.delaigue@obeo.fr>
 * Copyright (C) 2015, Denis Zygann <d.zygann@web.de>
 * Copyright (C) 2016, Lars Vogel <Lars.Vogel@vogella.com>
 * Copyright (C) 2017, Wim Jongman <wim.jongman@remainsoftware.com> bug 358152
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

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
	public static String AbstractHistoryCommandHandler_ActionRequiresOneSelectedCommitMessage;

	/** */
	public static String AbstractHistoryCommanndHandler_CouldNotGetRepositoryMessage;

	/** */
	public static String AbstractHistoryCommanndHandler_NoInputMessage;

	/** */
	public static String AbstractHistoryCommitHandler_cantGetBranches;

	/** */
	public static String AbstractRebaseCommand_DialogTitle;

	/** */
	public static String AbstractRebaseCommandHandler_cleanupDialog_text;

	/** */
	public static String AbstractRebaseCommandHandler_cleanupDialog_title;

	/** */
	public static String AbstractReflogCommandHandler_NoInput;

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
	public static String Activator_setupFocusListener;

	/** */
	public static String Activator_setupJdtTemplateResolver;

	/** */
	public static String AddCommand_AddButtonLabel;

	/** */
	public static String AddConfigEntryDialog_AddConfigTitle;

	/** */
	public static String AddConfigEntryDialog_ConfigKeyTooltip;

	/** */
	public static String AddConfigEntryDialog_DialogMessage;

	/** */
	public static String AddConfigEntryDialog_EnterValueMessage;

	/** */
	public static String AddConfigEntryDialog_InvalidKeyMessage;

	/** */
	public static String AddConfigEntryDialog_KeyComponentsMessage;

	/** */
	public static String AddConfigEntryDialog_KeyLabel;

	/** */
	public static String AddConfigEntryDialog_MustEnterKeyMessage;

	/** */
	public static String AddConfigEntryDialog_ValueLabel;

	/** */
	public static String AddConfigEntryDialog_ButtonOK;

	/** */
	public static String AddRemotePage_EnterRemoteNameMessage;

	/** */
	public static String AddRemotePage_RemoteNameAlreadyExistsError;

	/** */
	public static String AddRemotePage_RemoteNameEmptyError;

	/** */
	public static String AddRemotePage_RemoteNameInvalidError;

	/** */
	public static String AddRemotePage_RemoteNameLabel;

	/** */
	public static String AddRemoteWizard_Title;

	/** */
	public static String AddSubmoduleWizard_WindowTitle;

	/** */
	public static String AddToIndexAction_addingFiles;

	/** */
	public static String AddToIndexCommand_addingFilesFailed;

	/** */
	public static String AsynchronousRefProposalProvider_FetchingRemoteRefsMessage;

	/** */
	public static String AsynchronousRefProposalProvider_ShowingProposalsJobName;

	/** */
	public static String RemoveFromIndexAction_removingFiles;

	/** */
	public static String BlameInformationControl_Author;

	/** */
	public static String BlameInformationControl_Commit;

	/** */
	public static String BlameInformationControl_Committer;

	/** */
	public static String BlameInformationControl_DiffHeaderLabel;

	/** */
	public static String BlameInformationControl_OpenCommitLink;

	/** */
	public static String BlameInformationControl_ShowAnnotationsLink;

	/** */
	public static String BlameInformationControl_ShowInHistoryLink;

	/** */
	public static String AssumeUnchanged_assumeUnchanged;

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
	public static String WizardProjectsImportPage_CreateProjectsTask;

	/** */
	public static String SecureStoreUtils_errorClearingCredentials;

	/** */
	public static String SecureStoreUtils_errorReadingCredentials;

	/** */
	public static String SecureStoreUtils_writingCredentialsFailed;

	/** */
	public static String SelectResetTypePage_labelCurrentHead;

	/** */
	public static String SelectResetTypePage_labelResettingTo;

	/** */
	public static String SelectResetTypePage_PageMessage;

	/** */
	public static String SelectResetTypePage_PageTitle;

	/** */
	public static String SelectResetTypePage_tooltipCurrentHead;

	/** */
	public static String SelectResetTypePage_tooltipResettingTo;

	/** */
	public static String SharingWizard_windowTitle;

	/** */
	public static String SharingWizard_failed;

	/** */
	public static String SharingWizard_MoveProjectActionLabel;

	/** */
	public static String ShowBlameHandler_errorMessage;

	/** */
	public static String ShowBlameHandler_JobName;

	/** */
	public static String GenerateHistoryJob_errorComputingHistory;

	/** */
	public static String GenerateHistoryJob_taskFoundCommits;

	/** */
	public static String GerritConfigurationPage_BranchTooltipHover;

	/** */
	public static String GerritConfigurationPage_BranchTooltipStartTyping;

	/** */
	public static String GerritConfigurationPage_ConfigureFetchReviewNotes;

	/** */
	public static String GerritConfigurationPage_errorBranchName;

	/** */
	public static String GerritConfigurationPage_groupFetch;

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
	public static String GerritConfigurationPage_UserLabel;

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
	public static String ExistingOrNewPage_HeaderLocation;

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
	public static String GitCloneSourceProviderExtension_Local;

	/** */
	public static String GitCloneWizard_abortingCloneMsg;

	/** */
	public static String GitCloneWizard_abortingCloneTitle;

	/** */
	public static String GitCloneWizard_title;

	/** */
	public static String GitCloneWizard_jobImportProjects;

	/** */
	public static String GitCloneWizard_jobName;

	/** */
	public static String GitCloneWizard_failed;

	/** */
	public static String GitCloneWizard_errorCannotCreate;

	/** */
	public static String GitDecorator_jobTitle;

	/** */
	public static String GitDecoratorPreferencePage_bindingRepositoryNameFlag;

	/** */
	public static String GitDecoratorPreferencePage_iconsShowDirty;

	/** */
	public static String GitDocument_errorLoadCommit;

	/** */
	public static String GitDocument_errorLoadTree;

	/** */
	public static String GitDocument_errorResolveQuickdiff;

	/** */
	public static String GitDocument_ReloadJobError;

	/** */
	public static String GitDocument_ReloadJobName;

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
	public static String GitHistoryPage_squashMenuItem;

	/** */
	public static String GitHistoryPage_CheckoutMenuLabel;

	/** */
	public static String GitHistoryPage_CheckoutMenuLabel2;

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
	public static String GitHistoryPage_FindShowTooltip;

	/** */
	public static String GitHistoryPage_FindHideTooltip;

	/** */
	public static String GitHistoryPage_FolderType;

	/** */
	public static String GitHistoryPage_fileNotFound;

	/** */
	public static String GitHistoryPage_notContainedInCommits;

	/** */
	public static String GitHistoryPage_ModifyMenuLabel;

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
	public static String GitHistoryPage_rebaseInteractiveMenuItem;

	/** */
	public static String GitHistoryPage_rewordMenuItem;

	/** */
	public static String GitHistoryPage_editMenuItem;

	/** */
	public static String GitHistoryPage_SetAsBaselineMenuLabel;

	/** */
	public static String GitHistoryPage_ShowAdditionalRefsMenuLabel;

	/** */
	public static String GitHistoryPage_ShowAllBranchesMenuLabel;

	/** */
	public static String GitHistoryPage_FollowRenames;

	/** */
	public static String GitHistoryPage_FormatDiffJobName;

	/** */
	public static String GitHistoryPage_FilterSubMenuLabel;

	/** */
	public static String GitHistoryPage_IncompleteListTooltip;

	/** */
	public static String GitHistoryPage_InRevisionCommentSubMenuLabel;

	/** */
	public static String GitHistoryPage_ListIncompleteWarningMessage;

	/** */
	public static String GitHistoryPage_pushCommit;

	/** */
	public static String GitHistoryPage_ShowSubMenuLabel;

	/** */
	public static String GitHistoryPage_ColumnsSubMenuLabel;

	/** */
	public static String GitHistoryPage_toggleEmailAddresses;

	/** */
	public static String GitLightweightDecorator_name;

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
	public static String GitPreferenceRoot_DefaultRepoFolderVariableButton;

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
	public static String GitPreferenceRoot_SecureStoreGroupLabel;

	/** */
	public static String GitPreferenceRoot_SecureStoreUseByDefault;

	/** */
	public static String GitPreferenceRoot_SshClient_Jsch_Label;

	/** */
	public static String GitPreferenceRoot_SshClient_Apache_Label;

	/** */
	public static String GitPreferenceRoot_useLogicalModel;

	/** */
	public static String GitPreferenceRoot_preferreMergeStrategy_group;

	/** */
	public static String GitPreferenceRoot_preferreMergeStrategy_label;

	/** */
	public static String GitPreferenceRoot_defaultMergeStrategyLabel;

	/** */
	public static String GitPreferenceRoot_lfsSupportCaption;

	/** */
	public static String GitPreferenceRoot_lfsSupportCaptionNotAvailable;

	/** */
	public static String GitPreferenceRoot_lfsSupportInstall;

	/** */
	public static String GitPreferenceRoot_lfsSupportSuccessMessage;

	/** */
	public static String GitPreferenceRoot_lfsSupportSuccessTitle;

	/** */
	public static String ProcessStepsRebaseCommand_CancelDialogMessage;

	/** */
	public static String ProcessStepsRebaseCommand_JobName;

	/** */
	public static String ProjectsPreferencePage_AutoShareProjects;

	/** */
	public static String ProjectsPreferencePage_RestoreBranchProjects;

	/** */
	public static String ProjectsPreferencePage_AutoIgnoreDerivedResources;

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
	public static String GitProjectsImportPage_SearchForNestedProjects;

	/** */
	public static String RebaseCurrentRefCommand_RebaseCanceledMessage;

	/** */
	public static String RebaseCurrentRefCommand_RebasingCurrentJobName;

	/** */
	public static String RebaseCurrentRefCommand_ErrorGettingCurrentBranchMessage;

	/** */
	public static String RebaseInteractiveHandler_EditMessageDialogText;

	/** */
	public static String RebaseInteractiveHandler_EditMessageDialogTitle;

	/** */
	public static String RebaseInteractiveStepActionToolBarProvider_SkipText;

	/** */
	public static String RebaseInteractiveStepActionToolBarProvider_SkipDesc;

	/** */
	public static String RebaseInteractiveStepActionToolBarProvider_EditText;

	/** */
	public static String RebaseInteractiveStepActionToolBarProvider_EditDesc;

	/** */
	public static String RebaseInteractiveStepActionToolBarProvider_FixupText;

	/** */
	public static String RebaseInteractiveStepActionToolBarProvider_FixupDesc;

	/** */
	public static String RebaseInteractiveStepActionToolBarProvider_MoveDownText;

	/** */
	public static String RebaseInteractiveStepActionToolBarProvider_MoveDownDesc;

	/** */
	public static String RebaseInteractiveStepActionToolBarProvider_MoveUpText;

	/** */
	public static String RebaseInteractiveStepActionToolBarProvider_MoveUpDesc;

	/** */
	public static String RebaseInteractiveStepActionToolBarProvider_PickText;

	/** */
	public static String RebaseInteractiveStepActionToolBarProvider_PickDesc;

	/** */
	public static String RebaseInteractiveStepActionToolBarProvider_RewordText;

	/** */
	public static String RebaseInteractiveStepActionToolBarProvider_RewordDesc;

	/** */
	public static String RebaseInteractiveStepActionToolBarProvider_SquashText;

	/** */
	public static String RebaseInteractiveStepActionToolBarProvider_SquashDesc;

	/** */
	public static String RebaseInteractiveView_HeadingStep;

	/** */
	public static String RebaseInteractiveView_HeadingAction;

	/** */
	public static String RebaseInteractiveView_HeadingCommitId;

	/** */
	public static String RebaseInteractiveView_HeadingMessage;

	/** */
	public static String RebaseInteractiveView_HeadingStatus;

	/** */
	public static String RebaseInteractiveView_HeadingAuthor;

	/** */
	public static String RebaseInteractiveView_HeadingAuthorDate;

	/** */
	public static String RebaseInteractiveView_HeadingCommitter;

	/** */
	public static String RebaseInteractiveView_HeadingCommitDate;

	/** */
	public static String RebaseInteractiveView_NoSelection;

	/** */
	public static String RebaseInteractiveView_StatusCurrent;

	/** */
	public static String RebaseInteractiveView_StatusDone;

	/** */
	public static String RebaseInteractiveView_StatusTodo;

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
	public static String RebaseResultDialog_Edit;

	/** */
	public static String RebaseResultDialog_FastForward;

	/** */
	public static String RebaseResultDialog_Failed;

	/** */
	public static String RebaseResultDialog_InteractivePrepared;

	/** */
	public static String RebaseResultDialog_NextSteps;

	/** */
	public static String RebaseResultDialog_NextStepsAfterResolveConflicts;

	/** */
	public static String RebaseResultDialog_NextStepsDoNothing;

	/** */
	public static String RebaseResultDialog_NothingToCommit;

	/** */
	public static String RebaseResultDialog_notInWorkspace;

	/** */
	public static String RebaseResultDialog_notInWorkspaceMessage;

	/** */
	public static String RebaseResultDialog_notShared;

	/** */
	public static String RebaseResultDialog_notSharedMessage;

	/** */
	public static String RebaseResultDialog_SkipCommitButton;

	/** */
	public static String RebaseResultDialog_StartMergeRadioText;

	/** */
	public static String RebaseResultDialog_stashApplyConflicts;

	/** */
	public static String RebaseResultDialog_StatusAborted;

	/** */
	public static String RebaseResultDialog_StatusConflicts;

	/** */
	public static String RebaseResultDialog_StatusFailed;

	/** */
	public static String RebaseResultDialog_StatusFastForward;

	/** */
	public static String RebaseResultDialog_StatusNothingToCommit;

	/** */
	public static String RebaseResultDialog_StatusInteractivePrepared;

	/** */
	public static String RebaseResultDialog_StatusOK;

	/** */
	public static String RebaseResultDialog_StatusStopped;

	/** */
	public static String RebaseResultDialog_StatusEdit;

	/** */
	public static String RebaseResultDialog_StatusUpToDate;

	/** */
	public static String RebaseResultDialog_Stopped;

	/** */
	public static String RebaseResultDialog_SuccessfullyFinished;

	/** */
	public static String RebaseResultDialog_ToggleShowButton;

	/** */
	public static String RebaseResultDialog_UncommittedChanges;

	/** */
	public static String RebaseResultDialog_UpToDate;

	/** */
	public static String RebaseTargetSelectionDialog_DialogMessage;

	/** */
	public static String RebaseTargetSelectionDialog_DialogMessageWithBranch;

	/** */
	public static String RebaseTargetSelectionDialog_DialogTitle;

	/** */
	public static String RebaseTargetSelectionDialog_DialogTitleWithBranch;

	/** */
	public static String RebaseTargetSelectionDialog_RebaseButton;

	/** */
	public static String RebaseTargetSelectionDialog_RebaseTitle;

	/** */
	public static String RebaseTargetSelectionDialog_RebaseTitleWithBranch;

	/** */
	public static String RebaseTargetSelectionDialog_InteractiveButton;

	/** */
	public static String RebaseTargetSelectionDialog_PreserveMergesButton;

	/** */
	public static String ReplaceTargetSelectionDialog_ReplaceButton;

	/** */
	public static String ReplaceTargetSelectionDialog_ReplaceMessage;

	/** */
	public static String ReplaceTargetSelectionDialog_ReplaceTitle;

	/** */
	public static String ReplaceTargetSelectionDialog_ReplaceTitleEmptyPath;

	/** */
	public static String ReplaceTargetSelectionDialog_ReplaceWindowTitle;

	/** */
	public static String ReplaceWithOursTheirsMenu_CalculatingOursTheirsCommitsError;

	/** */
	public static String ReplaceWithOursTheirsMenu_OursWithCommitLabel;

	/** */
	public static String ReplaceWithOursTheirsMenu_OursWithoutCommitLabel;

	/** */
	public static String ReplaceWithOursTheirsMenu_TheirsWithCommitLabel;

	/** */
	public static String ReplaceWithOursTheirsMenu_TheirsWithoutCommitLabel;

	/** */
	public static String ReplaceWithPreviousActionHandler_NoParentCommitDialogMessage;

	/** */
	public static String ReplaceWithPreviousActionHandler_NoParentCommitDialogTitle;

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
	public static String RepositorySearchDialog_SearchResult;

	/** */
	public static String RepositorySearchDialog_SearchResultGroup;

	/** */
	public static String RepositorySearchDialog_SearchTitle;

	/** */
	public static String RepositorySearchDialog_SearchTooltip;

	/** */
	public static String RepositorySearchDialog_SkipHidden;

	/** */
	public static String RepositorySearchDialog_SkipHiddenTooltip;

	/** */
	public static String RepositorySearchDialog_SomeDirectoriesHiddenMessage;

	/** */
	public static String CancelAfterSaveDialog_Title;

	/** */
	public static String CleanRepositoryPage_cleanDirs;

	/** */
	public static String CleanRepositoryPage_cleanFiles;

	/** */
	public static String CleanRepositoryPage_cleaningItems;

	/** */
	public static String CleanRepositoryPage_findingItems;

	/** */
	public static String CleanRepositoryPage_includeIgnored;

	/** */
	public static String CleanRepositoryPage_message;

	/** */
	public static String CleanRepositoryPage_SelectFilesToClean;

	/** */
	public static String CleanRepositoryPage_title;

	/** */
	public static String CleanRepositoryPage_RefreshingRepositories;

	/** */
	public static String CleanWizard_title;

	/** */
	public static String ClearCredentialsCommand_clearingCredentialsFailed;

	/** */
	public static String CheckoutCommand_CheckoutLabel;

	/** */
	public static String CheckoutCommand_CheckoutLabelWithQuestion;

	/** */
	public static String CheckoutConflictDialog_conflictMessage;

	/** */
	public static String CheckoutDialog_OkCheckout;

	/** */
	public static String CheckoutDialog_OkCheckoutWithQuestion;

	/** */
	public static String CheckoutDialog_Title;

	/** */
	public static String CheckoutHandler_CheckoutBranchDialogButton;

	/** */
	public static String CheckoutHandler_CheckoutBranchDialogMessage;

	/** */
	public static String CheckoutHandler_CheckoutBranchDialogTitle;

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
	public static String CherryPickHandler_CherryPickMergeMessage;

	/** */
	public static String CherryPickHandler_CouldNotDeleteFile;

	/** */
	public static String CherryPickHandler_ErrorMsgTemplate;

	/** */
	public static String CherryPickHandler_IndexDirty;

	/** */
	public static String CherryPickHandler_JobName;

	/** */
	public static String CherryPickHandler_ConfirmMessage;

	/** */
	public static String CherryPickHandler_ConfirmTitle;

	/** */
	public static String CherryPickHandler_UncommittedFilesTitle;

	/** */
	public static String CherryPickHandler_unknown;

	/** */
	public static String CherryPickHandler_WorktreeDirty;

	/** */
	public static String CherryPickHandler_cherryPickButtonLabel;

	/** */
	public static String CherryPickOperation_InternalError;

	/** */
	public static String CompareTargetSelectionDialog_CompareButton;

	/** */
	public static String CompareTargetSelectionDialog_CompareMessage;

	/** */
	public static String CompareTargetSelectionDialog_CompareTitle;

	/** */
	public static String CompareTargetSelectionDialog_CompareTitleEmptyPath;

	/** */
	public static String CompareTargetSelectionDialog_WindowTitle;

	/** */
	public static String CompareTreeView_AnalyzingRepositoryTaskText;

	/** */
	public static String CompareTreeView_ExpandAllTooltip;

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
	public static String CompareTreeView_CompareModeTooltip;

	/** */
	public static String CompareUtils_errorGettingEncoding;

	/** */
	public static String CompareUtils_errorGettingHeadCommit;

	/** */
	public static String CompareUtils_wrongResourceArgument;

	/** */
	public static String MergeHandler_MergeBranchDialogButton;

	/** */
	public static String MergeHandler_MergeBranchDialogMessage;

	/** */
	public static String MergeHandler_MergeBranchDialogTitle;

	/** */
	public static String RepositoryAction_errorFindingRepo;

	/** */
	public static String RepositoryAction_errorFindingRepoTitle;

	/** */
	public static String RepositoryAction_multiRepoSelection;

	/** */
	public static String RepositoryAction_multiRepoSelectionTitle;

	/** */
	public static String RepositoryToolbarAction_label;

	/** */
	public static String RepositoryToolbarAction_tooltip;

	/** */
	public static String RepositoryTreeNodeDecorator_name;

	/** */
	public static String RepositoryCommit_AuthorDate;

	/** */
	public static String RepositoryCommit_AuthorDateCommitter;

	/** */
	public static String RepositoryLocationPage_info;

	/** */
	public static String RepositoryLocationPage_title;

	/** */
	public static String RepositoryLocationContentProvider_errorProvidingRepoServer;

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
	public static String RepositorySearchDialog_InvalidDirectoryMessage;

	/** */
	public static String RepositorySearchDialog_NoSearchAvailableMessage;

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
	public static String SourceBranchPage_remoteListingCancelled;

	/** */
	public static String SourceBranchPage_CompositeTransportErrorMessage;

	/** */
	public static String SourceBranchPage_AuthFailMessage;

	/** */
	public static String CloneDestinationPage_title;

	/** */
	public static String CloneDestinationPage_description;

	/** */
	public static String CloneDestinationPage_groupDestination;

	/** */
	public static String CloneDestinationPage_groupConfiguration;

	/** */
	public static String CloneDestinationPage_groupProjects;

	/** */
	public static String CloneDestinationPage_promptDirectory;

	/** */
	public static String CloneDestinationPage_promptInitialBranch;

	/** */
	public static String CloneDestinationPage_promptRemoteName;

	/** */
	public static String CloneDestinationPage_browseButton;

	/** */
	public static String CloneDestinationPage_cloneSubmodulesButton;

	/** */
	public static String CloneDestinationPage_DefaultRepoFolderTooltip;

	/** */
	public static String CloneDestinationPage_errorInitialBranchRequired;

	/** */
	public static String CloneDestinationPage_errorInvalidRemoteName;

	/** */
	public static String CloneDestinationPage_errorNotEmptyDir;

	/** */
	public static String CloneDestinationPage_errorRemoteNameRequired;

	/** */
	public static String CloneDestinationPage_importButton;

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
	public static String RefContentProposal_newRemoteObject;

	/** */
	public static String RefContentProposal_tag;

	/** */
	public static String RefContentProposal_trackingBranch;

	/** */
	public static String RefContentProposal_tree;

	/** */
	public static String RefContentProposal_unknownObject;

	/** */
	public static String RefContentProposal_unknownRemoteObject;

	/** */
	public static String ReflogView_DateColumnHeader;

	/** */
	public static String ReflogView_ErrorOnLoad;

	/** */
	public static String ReflogView_ErrorOnOpenCommit;

	/** */
	public static String ReflogView_MessageColumnHeader;

	/** */
	public static String ReflogView_CommitColumnHeader;

	/** */
	public static String ReflogView_CommitMessageColumnHeader;

	/** */
	public static String RefSelectionDialog_Message;

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
	public static String RefSpecWizard_pushTitle;

	/** */
	public static String RefSpecWizard_fetchTitle;

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
	public static String ResetTargetSelectionDialog_AuthorLabel;

	/** */
	public static String ResetTargetSelectionDialog_CommitLabel;

	/** */
	public static String ResetTargetSelectionDialog_CommitterLabel;

	/** */
	public static String ResetTargetSelectionDialog_DetachedHeadState;

	/** */
	public static String ResetTargetSelectionDialog_ExpressionLabel;

	/** */
	public static String ResetTargetSelectionDialog_ExpressionTooltip;

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
	public static String ResetTargetSelectionDialog_ResetTypeHEADHardButton;

	/** */
	public static String ResetTargetSelectionDialog_ResetTypeHEADMixedButton;

	/** */
	public static String ResetTargetSelectionDialog_SelectBranchForResetMessage;

	/** */
	public static String ResetTargetSelectionDialog_SubjectLabel;

	/** */
	public static String ResetTargetSelectionDialog_UnresolvableExpressionError;

	/** */
	public static String ResetTargetSelectionDialog_WindowTitle;

	/** */
	public static String ResourceHistory_MaxNumCommitsInList;

	/** */
	public static String ResourceHistory_ShowTagSequence;

	/** */
	public static String ResourceHistory_ShowBranchSequence;

	/** */
	public static String ResourceHistory_toggleRelativeDate;

	/** */
	public static String ResourceHistory_toggleShowNotes;

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
	public static String HistoryPage_authorDateColumn;

	/** */
	public static String HistoryPage_refreshJob;

	/** */
	public static String HistoryPage_findbar_find;

	/** */
	public static String HistoryPage_findbar_find_msg;

	/** */
	public static String HistoryPage_findbar_next;

	/** */
	public static String HistoryPage_findbar_previous;

	/** */
	public static String HistoryPage_findbar_ignorecase;

	/** */
	public static String HistoryPage_findbar_all;

	/** */
	public static String HistoryPage_findbar_commit;

	/** */
	public static String HistoryPage_findbar_comments;

	/** */
	public static String HistoryPage_findbar_author;

	/** */
	public static String HistoryPage_findbar_committer;

	/** */
	public static String HistoryPage_findbar_changeto_all;

	/** */
	public static String HistoryPage_findbar_changeto_commit;

	/** */
	public static String HistoryPage_findbar_changeto_comments;

	/** */
	public static String HistoryPage_findbar_changeto_author;

	/** */
	public static String HistoryPage_findbar_changeto_committer;

	/** */
	public static String HistoryPage_findbar_changeto_reference;

	/** */
	public static String HistoryPage_findbar_exceeded;

	/** */
	public static String HistoryPage_findbar_notFound;

	/** */
	public static String HistoryPage_findbar_reference;

	/** */
	public static String HistoryPreferencePage_MaxBranchLength;

	/** */
	public static String HistoryPreferencePage_MaxDiffLines;

	/** */
	public static String HistoryPreferencePage_MaxTagLength;

	/** */
	public static String HistoryPreferencePage_toggleShortenAtStart;

	/** */
	public static String HistoryPreferencePage_ShowGroupLabel;

	/** */
	public static String HistoryPreferencePage_ShowInRevCommentGroupLabel;

	/** */
	public static String HistoryPreferencePage_toggleAdditionalRefs;

	/** */
	public static String HistoryPreferencePage_toggleAllBranches;

	/** */
	public static String HistoryPreferencePage_toggleEmailAddresses;

	/** */
	public static String HistoryColumnsPreferencePage_description;

	/** */
	public static String HistoryColumnsPreferencePage_title;

	/** */
	public static String PullWizardPage_PageName;

	/** */
	public static String PullWizardPage_PageTitle;

	/** */
	public static String PullWizardPage_PageMessage;

	/** */
	public static String PullWizardPage_referenceLabel;

	/** */
	public static String PullWizardPage_referenceTooltip;

	/** */
	public static String PullWizardPage_ChooseReference;

	/** */
	public static String PullOperationUI_ConnectionProblem;

	/** */
	public static String PullOperationUI_NotTriedMessage;

	/** */
	public static String PullOperationUI_PullCanceledWindowTitle;

	/** */
	public static String PullOperationUI_PullFailed;

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
	public static String PushAction_wrongURIDescription;

	/** */
	public static String PushAction_wrongURITitle;

	/** */
	public static String PushBranchPage_RemoteBranchNameLabel;

	/** */
	public static String PushBranchPage_CannotAccessCommitDescription;

	/** */
	public static String PushBranchPage_Source;

	/** */
	public static String PushBranchPage_Destination;

	/** */
	public static String PushBranchPage_ChooseBranchNameError;

	/** */
	public static String PushBranchPage_ChooseRemoteError;

	/** */
	public static String PushBranchPage_ForceUpdateButton;

	/** */
	public static String PushBranchPage_InvalidBranchNameError;

	/** */
	public static String PushBranchPage_NewRemoteButton;

	/** */
	public static String PushBranchPage_PageMessage;

	/** */
	public static String PushBranchPage_PageName;

	/** */
	public static String PushBranchPage_PageTitle;

	/** */
	public static String PushBranchPage_RemoteLabel;

	/** */
	public static String PushBranchPage_UpstreamConfigOverwriteWarning;

	/** */
	public static String PushBranchPage_advancedWizardLink;

	/** */
	public static String PushBranchPage_advancedWizardLinkTooltip;

	/** */
	public static String PushBranchWizard_WindowTitle;

	/** */
	public static String PushBranchWizard_previewButton;

	/** */
	public static String PushBranchWizard_pushButton;

	/** */
	public static String PushCommitHandler_pushCommitTitle;

	/** */
	public static String PushOperationUI_MultiRepositoriesDestinationString;

	/** */
	public static String PushOperationUI_PushJobName;

	/** */
	public static String RepositoryJob_NullStatus;

	/** */
	public static String RepositoryJobResultAction_RepositoryGone;

	/** */
	public static String ShowPushResultAction_name;

	/** */
	public static String PushJob_cantConnectToAny;

	/** */
	public static String PushJob_unexpectedError;

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
	public static String CommitDialog_CommitAndPush;

	/** */
	public static String CommitDialog_CommitChanges;

	/** */
	public static String CommitDialog_Committer;

	/** */
	public static String CommitDialog_CommitMessage;

	/** */
	public static String CommitDialog_CompareWithHeadRevision;

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
	public static String CommitDialog_SelectForCommit;

	/** */
	public static String CommitDialog_SignCommit;

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
	public static String CommitDialog_ErrorCreatingCommitMessageProvider;

	/** */
	public static String CommitDialog_CaretPositionOutOfBounds;

	/** */
	public static String CommitDialog_IgnoreCaretPosition;

	/** */
	public static String CommitDialog_ConfigureLink;

	/** */
	public static String CommitDialog_ContentAssist;

	/** */
	public static String CommitDialog_Files;

	/** */
	public static String CommitDialog_Message;

	/** */
	public static String CommitDialog_MessageNoFilesSelected;

	/** */
	public static String CommitDialog_OpenStagingViewError;

	/** */
	public static String CommitDialog_OpenStagingViewLink;

	/** */
	public static String CommitDialog_OpenStagingViewToolTip;

	/** */
	public static String CommitDialog_Path;

	/** */
	public static String CommitDialog_Title;

	/** */
	public static String CommitDialog_IgnoreErrors;

	/** */
	public static String CommitDialog_MessageErrors;

	/** */
	public static String ConfigurationChecker_checkConfiguration;

	/** */
	public static String ConfigurationChecker_homeNotSet;

	/** */
	public static String ConfigurationChecker_installLfsCannotInstall;

	/** */
	public static String ConfigurationChecker_installLfsCannotLoadConfig;

	/** */
	public static String ConfigurationEditorComponent_ConfigLocationLabel;

	/** */
	public static String ConfigurationEditorComponent_EmptyStringNotAllowed;

	/** */
	public static String ConfigurationEditorComponent_KeyColumnHeader;

	/** */
	public static String ConfigurationEditorComponent_AddButton;

	/** */
	public static String ConfigurationEditorComponent_NoConfigLocationKnown;

	/** */
	public static String ConfigurationEditorComponent_OpenEditorButton;

	/** */
	public static String ConfigurationEditorComponent_OpenEditorTooltip;

	/** */
	public static String ConfigurationEditorComponent_ReadOnlyLocationFormat;

	/** */
	public static String ConfigurationEditorComponent_RemoveButton;

	/** */
	public static String ConfigurationEditorComponent_RemoveTooltip;

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
	public static String ConfigurationEditorComponent_WrongNumberOfTokensMessage;

	/** */
	public static String ConfigureGerritWizard_title;

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
	public static String SpellcheckableMessageArea_showWhitespace;

	/** */
	public static String CommitMessageComponent_MessageInvalidAuthor;

	/** */
	public static String CommitMessageComponent_MessageInvalidCommitter;

	/** */
	public static String CommitMessageComponent_AmendingCommitInRemoteBranch;

	/** */
	public static String CommitMessageComponent_MessageSecondLineNotEmpty;

	/** */
	public static String CommitMessageComponent_ErrorMissingSigningKey;

	/** */
	public static String CommitMessageComponent_ErrorNoSigningKeyFound;

	/** */
	public static String CommitMessageEditorDialog_EditCommitMessageTitle;

	/** */
	public static String CommitMessageViewer_author;

	/** */
	public static String CommitMessageViewer_child;

	/** */
	public static String CommitMessageViewer_branches;

	/** */
	public static String CommitMessageViewer_MoreBranches;

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
	public static String CompareWithIndexAction_errorOnAddToIndex;

	/** */
	public static String CompareWithRefAction_errorOnSynchronize;

	/** */
	public static String CompareWithPreviousActionHandler_MessageRevisionNotFound;

	/** */
	public static String CompareWithPreviousActionHandler_TitleRevisionNotFound;

	/** */
	public static String CompareUtils_jobName;

	/** */
	public static String CompareUtils_errorCommonAncestor;

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
	public static String CreateBranchPage_BranchNameLabel;

	/** */
	public static String CreateBranchPage_BranchNameToolTip;

	/** */
	public static String CreateBranchPage_CheckingOutMessage;

	/** */
	public static String CreateBranchPage_CheckoutButton;

	/** */
	public static String CreateBranchPage_ChooseBranchAndNameMessage;

	/** */
	public static String CreateBranchPage_ChooseNameMessage;

	/** */
	public static String CreateBranchPage_CreateBranchNameProviderFailed;

	/** */
	public static String CreateBranchPage_CreatingBranchMessage;

	/** */
	public static String CreateBranchPage_LocalBranchWarningMessage;

	/** */
	public static String CreateBranchPage_MissingSourceMessage;

	/** */
	public static String CreateBranchPage_SourceLabel;

	/** */
	public static String CreateBranchPage_SourceSelectButton;

	/** */
	public static String CreateBranchPage_SourceSelectionDialogMessage;

	/** */
	public static String CreateBranchPage_SourceSelectionDialogTitle;

	/** */
	public static String CreateBranchPage_SourceTooltip;

	/** */
	public static String CreateBranchPage_Title;

	/** */
	public static String CreateBranchWizard_CreationFailed;

	/** */
	public static String CreateBranchWizard_NewBranchTitle;

	/** */
	public static String CreateRepositoryCommand_CreateButtonLabel;

	/** */
	public static String CreateRepositoryPage_BareCheckbox;

	/** */
	public static String CreateRepositoryPage_BrowseButton;

	/** */
	public static String CreateRepositoryPage_DefaultRepositoryName;

	/** */
	public static String CreateRepositoryPage_DirectoryLabel;

	/** */
	public static String CreateRepositoryPage_NotEmptyMessage;

	/** */
	public static String CreateRepositoryPage_PageMessage;

	/** */
	public static String CreateRepositoryPage_PageTitle;

	/** */
	public static String CreateRepositoryPage_PleaseSelectDirectoryMessage;

	/** */
	public static String PushResultDialog_ConfigureButton;

	/** */
	public static String PushResultDialog_label;

	/** */
	public static String PushResultDialog_label_failed;

	/** */
	public static String PushResultDialog_title;

	/** */
	public static String PushResultTable_MessageText;

	/** */
	public static String PushResultTable_repository;

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
	public static String PushTagsPage_ForceUpdateButton;

	/** */
	public static String PushTagsPage_PageMessage;

	/** */
	public static String PushTagsPage_PageName;

	/** */
	public static String PushTagsPage_PageTitle;

	/** */
	public static String PushTagsPage_RemoteLabel;

	/** */
	public static String PushTagsPage_TagsLabelNoneSelected;

	/** */
	public static String PushTagsPage_TagsLabelSelected;

	/** */
	public static String PushTagsWizard_WindowTitle;

	/** */
	public static String PushToGerritPage_BranchLabel;

	/** */
	public static String PushToGerritPage_ContentProposalHoverText;

	/** */
	public static String PushToGerritPage_ContentProposalStartTypingText;

	/** */
	public static String PushToGerritPage_Message;

	/** */
	public static String PushToGerritPage_MissingBranchMessage;

	/** */
	public static String PushToGerritPage_MissingUriMessage;

	/** */
	public static String PushToGerritPage_Title;

	/** */
	public static String PushToGerritPage_TopicCollidesWithBranch;

	/** */
	public static String PushToGerritPage_TopicContentProposalHoverText;

	/** */
	public static String PushToGerritPage_TopicHasWhitespace;

	/** */
	public static String PushToGerritPage_TopicInvalidCharacters;

	/** */
	public static String PushToGerritPage_TopicLabel;

	/** */
	public static String PushToGerritPage_TopicSaveFailure;

	/** */
	public static String PushToGerritPage_TopicUseLabel;

	/** */
	public static String PushToGerritPage_UriLabel;

	/** */
	public static String PushToGerritWizard_Title;

	/** */
	public static String FetchAction_wrongURITitle;

	/** */
	public static String FetchAction_wrongURIMessage;

	/** */
	public static String FetchOperationUI_FetchJobName;

	/** */
	public static String FetchOperationUI_ShowFetchResult;

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
	public static String FetchGerritChangePage_ActivateAdditionalRefsButton;

	/** */
	public static String FetchGerritChangePage_ActivateAdditionalRefsTooltip;

	/** */
	public static String FetchGerritChangePage_AfterFetchGroup;

	/** */
	public static String FetchGerritChangePage_BranchEditButton;

	/** */
	public static String FetchGerritChangePage_BranchNameText;

	/** */
	public static String FetchGerritChangePage_ChangeLabel;

	/** */
	public static String FetchGerritChangePage_CheckingOutTaskName;

	/** */
	public static String FetchGerritChangePage_CheckoutRadio;

	/** */
	public static String FetchGerritChangePage_CherryPickRadio;

	/** */
	public static String FetchGerritChangePage_CherryPickTaskName;

	/** */
	public static String FetchGerritChangePage_ContentAssistDescription;

	/** */
	public static String FetchGerritChangePage_ContentAssistTooltip;

	/** */
	public static String FetchGerritChangePage_CreatingBranchTaskName;

	/** */
	public static String FetchGerritChangePage_CreatingTagTaskName;

	/** */
	public static String FetchGerritChangePage_FetchingTaskName;

	/** */
	public static String FetchGerritChangePage_GeneratedTagMessage;

	/** */
	public static String FetchGerritChangePage_GetChangeTaskName;

	/** */
	public static String FetchGerritChangePage_LocalBranchRadio;

	/** */
	public static String FetchGerritChangePage_LocalBranchCheckout;

	/** */
	public static String FetchGerritChangePage_MissingChangeMessage;

	/** */
	public static String FetchGerritChangePage_NoSuchChangeMessage;

	/** */
	public static String FetchGerritChangePage_PageMessage;

	/** */
	public static String FetchGerritChangePage_PageTitle;

	/** */
	public static String FetchGerritChangePage_SuggestedRefNamePattern;

	/** */
	public static String FetchGerritChangePage_TagNameText;

	/** */
	public static String FetchGerritChangePage_TagRadio;

	/** */
	public static String FetchGerritChangePage_UnknownChangeRefMessage;

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
	public static String FetchResultDialog_CloseButton;

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
	public static String FetchResultTable_statusPruned;

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
	public static String CommitFileDiffViewer_computingFileDiffs;

	/** */
	public static String CommitFileDiffViewer_errorGettingDifference;

	/** */
	public static String CommitFileDiffViewer_updatingFileDiffs;

	/** */
	public static String FileDiffLabelProvider_RenamedFromToolTip;

	/** */
	public static String FileRevisionEditorInput_NameAndRevisionTitle;

	/** */
	public static String FileRevisionEditorInput_cannotWriteTempFile;

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
	public static String WindowCachePreferencePage_streamFileThreshold;

	/** */
	public static String BasicConfigurationDialog_ConfigLocationInfo;

	/** */
	public static String BasicConfigurationDialog_DialogMessage;

	/** */
	public static String BasicConfigurationDialog_DialogTitle;

	/** */
	public static String BasicConfigurationDialog_DontShowAgain;

	/** */
	public static String BasicConfigurationDialog_OpenPreferencePage;

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
	public static String BranchAction_checkingOutMultiple;

	/** */
	public static String BranchAction_repositoryState;

	/** */
	public static String BranchConfigurationDialog_BranchConfigurationTitle;

	/** */
	public static String BranchConfigurationDialog_EditBranchConfigMessage;

	/** */
	public static String BranchConfigurationDialog_ExceptionGettingRefs;

	/** */
	public static String BranchConfigurationDialog_RemoteLabel;

	/** */
	public static String BranchConfigurationDialog_SaveBranchConfigFailed;

	/** */
	public static String BranchConfigurationDialog_UpstreamBranchLabel;

	/** */
	public static String BranchConfigurationDialog_ButtonOK;

	/** */
	public static String BranchOperationUI_CheckoutError_DialogMessage;

	/** */
	public static String BranchOperationUI_CheckoutError_DialogTitle;

	/** */
	public static String BranchOperationUI_CheckoutRemoteTrackingAsLocal;

	/** */
	public static String BranchOperationUI_CheckoutRemoteTrackingCommit;

	/** */
	public static String BranchOperationUI_CheckoutRemoteTrackingQuestion;

	/** */
	public static String BranchOperationUI_CheckoutRemoteTrackingTitle;

	/** */
	public static String BranchOperationUI_Continue;

	/** */
	public static String BranchOperationUI_DetachedHeadTitle;

	/** */
	public static String BranchOperationUI_DetachedHeadMessage;

	/** */
	public static String LaunchFinder_RunningLaunchDontShowAgain;

	/** */
	public static String LaunchFinder_RunningLaunchMessage;

	/** */
	public static String LaunchFinder_RunningLaunchTitle;

	/** */
	public static String LaunchFinder_SearchLaunchConfiguration;

	/** */
	public static String LaunchFinder_ContinueQuestion;

	/** */
	public static String BranchNameNormalizer_Tooltip;

	/** */
	public static String BranchNameNormalizer_TooltipForTag;

	/** */
	public static String BranchRebaseMode_Rebase;

	/** */
	public static String BranchRebaseMode_Preserve;

	/** */
	public static String BranchRebaseMode_Interactive;

	/** */
	public static String BranchRebaseMode_None;

	/** */
	public static String BranchRenameDialog_Message;

	/** */
	public static String BranchRenameDialog_NewNameLabel;

	/** */
	public static String BranchRenameDialog_RenameExceptionMessage;

	/** */
	public static String BranchRenameDialog_Title;

	/** */
	public static String BranchRenameDialog_WindowTitle;

	/** */
	public static String BranchRenameDialog_WrongPrefixErrorMessage;

	/** */
	public static String BranchRenameDialog_ButtonOK;

	/** */
	public static String BranchPropertySource_RebaseDescriptor;

	/** */
	public static String BranchPropertySource_RemoteDescriptor;

	/** */
	public static String BranchPropertySource_UpstreamBranchDescriptor;

	/** */
	public static String BranchPropertySource_UpstreamConfigurationCategory;

	/** */
	public static String BranchPropertySource_ValueNotSet;

	/** */
	public static String BranchResultDialog_buttonCommit;

	/** */
	public static String BranchResultDialog_buttonReset;

	/** */
	public static String BranchResultDialog_buttonStash;

	/** */
	public static String BranchResultDialog_CheckoutConflictsMessage;

	/** */
	public static String BranchResultDialog_CheckoutConflictsTitle;

	/** */
	public static String BranchResultDialog_DetachedHeadWarningDontShowAgain;

	/** */
	public static String BranchSelectionAndEditDialog_Message;

	/** */
	public static String BranchSelectionAndEditDialog_Title;

	/** */
	public static String BranchSelectionAndEditDialog_WindowTitle;

	/** */
	public static String BranchSelectionAndEditDialog_OkClose;

	/** */
	public static String BranchSelectionAndEditDialog_ErrorCouldNotCreateNewRef;

	/** */
	public static String BranchSelectionAndEditDialog_ErrorCouldNotDeleteRef;

	/** */
	public static String BranchSelectionAndEditDialog_ErrorCouldNotRenameRef;

	/** */
	public static String BranchSelectionAndEditDialog_NewBranch;

	/** */
	public static String BranchSelectionAndEditDialog_Rename;

	/** */
	public static String BranchSelectionAndEditDialog_Delete;

	/** */
	public static String CommittingPreferencePage_commitMessageHistory;

	/** */
	public static String CommittingPreferencePage_title;

	/** */
	public static String CommittingPreferencePage_hardWrapMessage;

	/** */
	public static String CommittingPreferencePage_hardWrapMessageTooltip;

	/** */
	public static String CommittingPreferencePage_warnAboutCommitMessageSecondLine;

	/** */
	public static String CommittingPreferencePage_footers;

	/** */
	public static String CommittingPreferencePage_formatting;

	/** */
	public static String CommittingPreferencePage_includeUntrackedFiles;

	/** */
	public static String CommittingPreferencePage_includeUntrackedFilesTooltip;

	/** */
	public static String CommittingPreferencePage_signedOffBy;

	/** */
	public static String CommittingPreferencePage_signedOffByTooltip;

	/** */
	public static String CommittingPreferencePage_CheckBeforeCommitting;

	/** */
	public static String CommittingPreferencePage_WarnBeforeCommitting;

	/** */
	public static String CommittingPreferencePage_WarnBeforeCommittingTitle;

	/** */
	public static String CommittingPreferencePage_WarnBlock_Errors;

	/** */
	public static String CommittingPreferencePage_WarnBlock_WarningsAndErrors;

	/** */
	public static String CommittingPreferencePage_BlockCommit;

	/** */
	public static String CommittingPreferencePage_BlockCommitCombo;

	/** */
	public static String CommittingPreferencePage_AlwaysUseStagingView;

	/** */
	public static String CommittingPreferencePage_autoStageDeletion;

	/** */
	public static String CommittingPreferencePage_autoStageMoves;

	/** */
	public static String CommittingPreferencePage_AutoStageOnCommit;

	/** */
	public static String CommittingPreferencePage_general;

	/** */
	public static String DateFormatPreferencePage_title;

	/** */
	public static String DateFormatPreferencePage_formatChooser_label;

	/** */
	public static String DateFormatPreferencePage_formatInput_label;

	/** */
	public static String DateFormatPreferencePage_invalidDateFormat_message;

	/** */
	public static String DateFormatPreferencePage_datePreview_label;

	/** */
	public static String DateFormatPreferencePage_choiceGitDefault_label;

	/** */
	public static String DateFormatPreferencePage_choiceGitLocal_label;

	/** */
	public static String DateFormatPreferencePage_choiceGitRelative_label;

	/** */
	public static String DateFormatPreferencePage_choiceGitIso_label;

	/** */
	public static String DateFormatPreferencePage_choiceGitRfc_label;

	/** */
	public static String DateFormatPreferencePage_choiceGitShort_label;

	/** */
	public static String DateFormatPreferencePage_choiceGitLocale_label;

	/** */
	public static String DateFormatPreferencePage_choiceGitLocaleLocal_label;

	/** */
	public static String DateFormatPreferencePage_choiceCustom_label;

	/** */
	public static String DateFormatPreferencePage_gitRelative_format_text;

	/** */
	public static String DateFormatPreferencePage_gitLocale_format_text;

	/** */
	public static String DateFormatPreferencePage_gitLocaleLocal_format_text;

	/** */
	public static String DateFormatPreferencePage_helpGitDefault_label;

	/** */
	public static String DateFormatPreferencePage_helpGitLocal_label;

	/** */
	public static String DateFormatPreferencePage_helpGitRelative_label;

	/** */
	public static String DateFormatPreferencePage_helpGitIso_label;

	/** */
	public static String DateFormatPreferencePage_helpGitRfc_label;

	/** */
	public static String DateFormatPreferencePage_helpGitShort_label;

	/** */
	public static String DateFormatPreferencePage_helpGitLocale_label;

	/** */
	public static String DateFormatPreferencePage_helpGitLocaleLocal_label;

	/** */
	public static String DateFormatPreferencePage_helpCustom_label;

	/** */
	public static String Decorator_exceptionMessage;

	/** */
	public static String Decorator_exceptionMessageCommon;

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
	public static String DecoratorPreferencesPage_submoduleFormatLabel;

	/** */
	public static String DecoratorPreferencesPage_generalTabFolder;

	/** */
	public static String DecoratorPreferencesPage_bindingResourceName;

	/** */
	public static String DecoratorPreferencesPage_bindingBranchName;

	/** */
	public static String DecoratorPreferencesPage_bindingBranchStatus;

	/** */
	public static String DecoratorPreferencesPage_bindingCommitMessage;

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
	public static String DecoratorPreferencesPage_colorsAndFontsLink;

	/** */
	public static String DecoratorPreferencesPage_iconsShowTracked;

	/** */
	public static String DecoratorPreferencesPage_iconsShowUntracked;

	/** */
	public static String DecoratorPreferencesPage_iconsShowStaged;

	/** */
	public static String DecoratorPreferencesPage_iconsShowConflicts;

	/** */
	public static String DecoratorPreferencesPage_iconsShowAssumeUnchanged;

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
	public static String DeleteBranchOnCommitHandler_DeleteBranchesDialogButton;

	/** */
	public static String DeleteBranchOnCommitHandler_DeleteBranchesDialogMessage;

	/** */
	public static String DeleteBranchOnCommitHandler_DeleteBranchesDialogTitle;

	/** */
	public static String DeletePathsOperationUI_ButtonOK;

	/** */
	public static String DeleteRepositoryConfirmDialog_DeleteGitDirCheckbox;

	/** */
	public static String DeleteRepositoryConfirmDialog_DeleteRepositoryConfirmMessage;

	/** */
	public static String DeleteRepositoryConfirmDialog_DeleteRepositoryNoUndoWarning;

	/** */
	public static String DeleteRepositoryConfirmDialog_DeleteRepositoryConfirmButton;
	/** */
	public static String DeleteRepositoryConfirmDialog_DeleteRepositoryTitle;

	/** */
	public static String DeleteRepositoryConfirmDialog_DeleteRepositoryWindowTitle;

	/** */
	public static String DeleteRepositoryConfirmDialog_DeleteWorkingDirectoryCheckbox;

	/** */
	public static String DeleteRepositoryConfirmDialog_DeleteProjectsCheckbox;

	/** */
	public static String DeleteTagOnCommitHandler_DeleteTagsDialogButton;

	/** */
	public static String DeleteTagOnCommitHandler_DeleteTagsDialogMessage;

	/** */
	public static String DeleteTagOnCommitHandler_DeleteTagsDialogTitle;

	/** */
	public static String DeleteTagCommand_deletingTagsProgress;

	/** */
	public static String DeleteTagCommand_messageConfirmMultipleTag;

	/** */
	public static String DeleteTagCommand_messageConfirmSingleTag;

	/** */
	public static String DeleteTagCommand_taskName;

	/** */
	public static String DeleteTagCommand_titleConfirm;

	/** */
	public static String DeleteResourcesOperationUI_confirmActionTitle;

	/** */
	public static String DeleteResourcesOperationUI_confirmActionMessage;

	/** */
	public static String DeleteResourcesOperationUI_deleteFailed;

	/** */
	public static String IgnoreActionHandler_addToGitignore;

	/** */
	public static String IgnoreActionHandler_manyFilesToBeIgnoredTitle;

	/** */
	public static String IgnoreActionHandler_manyFilesToBeIgnoredQuestion;

	/** */
	public static String RepositoriesView_BranchDeletionFailureMessage;

	/** */
	public static String RepositoriesView_Branches_Nodetext;

	/** */
	public static String RepositoriesView_CheckoutConfirmationDefaultButtonLabel;

	/** */
	public static String RepositoriesView_CheckoutConfirmationMessage;

	/** */
	public static String RepositoriesView_CheckoutConfirmationTitle;

	/** */
	public static String RepositoriesView_CheckoutConfirmationToggleMessage;

	/** */
	public static String RepositoriesView_ClipboardContentNoGitRepoMessage;

	/** */
	public static String RepositoriesView_ClipboardContentNotDirectoryOrURIMessage;

	/** */
	public static String RepositoriesView_ConfirmDeleteRemoteHeader;

	/** */
	public static String RepositoriesView_ConfirmDeleteRemoteMessage;

	/** */
	public static String RepositoriesView_ConfirmProjectDeletion_Question;

	/** */
	public static String RepositoriesView_ConfirmProjectDeletion_WindowTitle;

	/** */
	public static String RepositoriesView_DeleteRepoDeterminProjectsMessage;

	/** */
	public static String RepositoriesView_ErrorHeader;

	/** */
	public static String RepositoriesView_ExceptionLookingUpRepoMessage;

	/** */
	public static String RepositoriesView_linkAdd;

	/** */
	public static String RepositoriesView_linkClone;

	/** */
	public static String RepositoriesView_linkCreate;

	/** */
	public static String RepositoriesView_messageEmpty;

	/** */
	public static String RepositoriesView_NothingToPasteMessage;

	/** */
	public static String RepositoriesView_PasteRepoAlreadyThere;

	/** */
	public static String RepositoriesView_RemotesNodeText;

	/** */
	public static String RepositoriesView_TagDeletionFailureMessage;

	/** */
	public static String RepositoriesView_WorkingDir_treenode;

	/** */
	public static String RepositoriesViewActionProvider_OpenWithMenu;

	/** */
	public static String RepositoriesViewContentProvider_ExceptionNodeText;

	/** */
	public static String RepositoriesViewContentProvider_ReadReferencesJob;

	/** */
	public static String RepositoriesViewLabelProvider_LocalNodetext;

	/** */
	public static String RepositoriesViewLabelProvider_RemoteTrackingNodetext;

	/** */
	public static String RepositoriesViewLabelProvider_StashNodeText;

	/** */
	public static String RepositoriesViewLabelProvider_SubmodulesNodeText;

	/** */
	public static String RepositoriesViewLabelProvider_SymbolicRefNodeText;

	/** */
	public static String RepositoriesViewLabelProvider_TagsNodeText;

	/** */
	public static String RepositoriesViewLabelProvider_UnbornBranchText;

	/** */
	public static String DialogsPreferencePage_autoConfigureLfs;

	/** */
	public static String DialogsPreferencePage_DetachedHeadCombo;

	/** */
	public static String DialogsPreferencePage_HideConfirmationGroupHeader;

	/** */
	public static String DialogsPreferencePage_ShowInfoGroupHeader;

	/** */
	public static String DialogsPreferencePage_ShowFetchInfoDialog;

	/** */
	public static String DialogsPreferencePage_ShowPushInfoDialog;

	/** */
	public static String DialogsPreferencePage_ShowTooltip;

	/** */
	public static String DialogsPreferencePage_HideWarningGroupHeader;

	/** */
	public static String DialogsPreferencePage_HomeDirWarning;

	/** */
	public static String DialogsPreferencePage_RebaseCheckbox;

	/** */
	public static String DialogsPreferencePage_RunningLaunchOnCheckout;

	/** */
	public static String DialogsPreferencePage_ShowInitialConfigCheckbox;

	/** */
	public static String DialogsPreferencePage_ShowCheckoutConfirmation;

	/** */
	public static String DialogsPreferencePage_ShowCloneFailedDialog;

	/** */
	public static String DiffEditorPage_TaskGeneratingDiff;

	/** */
	public static String DiffEditorPage_TaskUpdatingViewer;

	/** */
	public static String DiffEditorPage_Title;

	/** */
	public static String DiffEditorPage_ToggleLineNumbers;

	/** */
	public static String DiffEditorPage_WarningNoDiffForMerge;

	/** */
	public static String DiscardChangesAction_confirmActionTitle;

	/** */
	public static String DiscardChangesAction_confirmActionMessage;

	/** */
	public static String DiscardChangesAction_discardChanges;

	/** */
	public static String DiscardChangesAction_discardChangesButtonText;

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
	public static String GitCompareFileRevisionEditorInput_CurrentTitle;

	/** */
	public static String GitCompareFileRevisionEditorInput_LocalLabel;

	/** */
	public static String GitCompareFileRevisionEditorInput_IndexLabel;

	/** */
	public static String GitCompareFileRevisionEditorInput_IndexEditableLabel;

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
	public static String GitCreatePatchAction_cannotCreatePatch;

	/** */
	public static String GitCreatePatchAction_workingTreeClean;

	/** */
	public static String GitCreatePatchWizard_Browse;

	/** */
	public static String GitCreatePatchWizard_Clipboard;

	/** */
	public static String GitCreatePatchWizard_ContextMustBePositiveInt;

	/** */
	public static String GitCreatePatchWizard_CreatePatchTitle;

	/** */
	public static String GitCreatePatchWizard_File;

	/** */
	public static String GitCreatePatchWizard_Format;

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
	public static String GitCreatePatchWizard_LinesOfContext;

	/** */
	public static String GitCreatePatchWizard_ReadOnlyTitle;

	/** */
	public static String GitCreatePatchWizard_ReadOnlyMsg;

	/** */
	public static String GitCreatePatchWizard_OverwriteTitle;

	/** */
	public static String GitCreatePatchWizard_OverwriteMsg;

	/** */
	public static String GitCreatePatchWizard_Workspace;

	/** */
	public static String GitCreatePatchWizard_WorkspacePatchDialogTitle;

	/** */
	public static String GitCreatePatchWizard_WorkspacePatchDialogDescription;

	/** */
	public static String GitCreatePatchWizard_WorkspacePatchDialogEnterFileName;

	/** */
	public static String GitCreatePatchWizard_WorkspacePatchDialogEnterValidLocation;

	/** */
	public static String GitCreatePatchWizard_WorkspacePatchDialogFileName;

	/** */
	public static String GitCreatePatchWizard_WorkspacePatchDialogSavePatch;

	/** */
	public static String GitCreatePatchWizard_WorkspacePatchEnterValidFileName;

	/** */
	public static String GitCreatePatchWizard_WorkspacePatchFolderExists;

	/** */
	public static String GitCreatePatchWizard_WorkspacePatchProjectClosed;

	/** */
	public static String GitCreatePatchWizard_WorkspacePatchSelectByBrowsing;

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
	public static String GitImportWizard_errorParsingURI;

	/** */
	public static String GitImportWizard_noRepositoryInfo;

	/** */
	public static String GitImportWizard_WizardTitle;

	/** */
	public static String GitScopeOperation_GitScopeManager;

	/** */
	public static String GitSelectRepositoryPage_AddButton;

	/** */
	public static String GitSelectRepositoryPage_AddTooltip;

	/** */
	public static String GitSelectRepositoryPage_NoRepoFoundMessage;

	/** */
	public static String GitSelectRepositoryPage_NoRepository;

	/** */
	public static String GitSelectRepositoryPage_PageMessage;

	/** */
	public static String GitSelectRepositoryPage_BareRepositoriesHidden;

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
	public static String GitSelectWizardPage_Selected;

	/** */
	public static String GitSelectWizardPage_UseNewProjectsWizardButton;

	/** */
	public static String GerritSelectRepositoryPage_PageTitle;

	/** */
	public static String GerritSelectRepositoryPage_FinishButtonLabel;

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
	public static String MergeResultDialog_conflicts;

	/** */
	public static String MergeResultDialog_failed;

	/** */
	public static String MergeResultDialog_mergeInput;

	/** */
	public static String MergeResultDialog_newHead;

	/** */
	public static String MergeResultDialog_nMore;

	/** */
	public static String MergeResultDialog_result;

	/** */
	public static String MergeResultDialog_StatusAborted;

	/** */
	public static String MergeResultDialog_StatusAlreadyUpToDate;

	/** */
	public static String MergeResultDialog_StatusCheckoutConflict;

	/** */
	public static String MergeResultDialog_StatusConflicting;

	/** */
	public static String MergeResultDialog_StatusFailed;

	/** */
	public static String MergeResultDialog_StatusFastForward;

	/** */
	public static String MergeResultDialog_StatusFastForwardSquashed;

	/** */
	public static String MergeResultDialog_StatusMerged;

	/** */
	public static String MergeResultDialog_StatusMergedNotCommitted;

	/** */
	public static String MergeResultDialog_StatusMergedSquashed;

	/** */
	public static String MergeResultDialog_StatusMergedSquashedNotCommitted;

	/** */
	public static String MergeResultDialog_StatusNotSupported;

	/** */
	public static String MergeTargetSelectionDialog_ButtonMerge;

	/** */
	public static String MergeTargetSelectionDialog_SelectRef;

	/** */
	public static String MergeTargetSelectionDialog_SelectRefWithBranch;

	/** */
	public static String MergeTargetSelectionDialog_TitleMerge;

	/** */
	public static String MergeTargetSelectionDialog_TitleMergeWithBranch;

	/** */
	public static String MergeTargetSelectionDialog_FastForwardGroup;

	/** */
	public static String MergeTargetSelectionDialog_FastForwardButton;

	/** */
	public static String MergeTargetSelectionDialog_NoFastForwardButton;

	/** */
	public static String MergeTargetSelectionDialog_OnlyFastForwardButton;

	/** */
	public static String MergeTargetSelectionDialog_MergeTypeGroup;

	/** */
	public static String MergeTargetSelectionDialog_MergeTypeCommitButton;

	/** */
	public static String MergeTargetSelectionDialog_MergeTypeNoCommitButton;

	/** */
	public static String MergeTargetSelectionDialog_MergeTypeSquashButton;

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
	public static String MultiPullResultDialog_UpdatedOneMessage;

	/** */
	public static String MultiPullResultDialog_UpdateStatusColumnHeader;

	/** */
	public static String MultiPullResultDialog_WindowTitle;

	/** */
	public static String MultiBranchOperationResultDialog_WindowTitle;

	/** */
	public static String MultiBranchOperationResultDialog_RepositoryColumnHeader;

	/** */
	public static String MultiBranchOperationResultDialog_CheckoutStatusColumnHeader;

	/** */
	public static String MultiBranchOperationResultDialog_DialogTitle;

	/** */
	public static String MultiBranchOperationResultDialog_DialogErrorMessage;

	/** */
	public static String MultiBranchOperationResultDialog_CheckoutResultError;

	/** */
	public static String MultiBranchOperationResultDialog_CheckoutResultNonDeleted;

	/** */
	public static String MultiBranchOperationResultDialog_CheckoutResultConflicts;

	/** */
	public static String MultiBranchOperationResultDialog_CheckoutResultOK;

	/** */
	public static String MultiBranchOperationResultDialog_CheckoutResultNotTried;

	/** */
	public static String MultiBranchOperationResultDialog_OkStatus;

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
	public static String UIUtils_PressShortcutForRemoteRefMessage;

	/** */
	public static String UIUtils_StartTypingForRemoteRefMessage;

	/** */
	public static String UIUtils_ShowInMenuLabel;

	/** */
	public static String UnmergedBranchDialog_Message;

	/** */
	public static String UnmergedBranchDialog_Title;

	/** */
	public static String UnmergedBranchDialog_deleteButtonLabel;

	/** */
	public static String Untrack_untrack;

	/** */
	public static String UpstreamConfigComponent_ConfigureUpstreamCheck;

	/** */
	public static String UpstreamConfigComponent_ConfigureUpstreamToolTip;

	/** */
	public static String BranchRebaseModeCombo_RebaseModeLabel;

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
	public static String CreateTagDialog_clearButton;

	/** */
	public static String CreateTagDialog_clearButtonTooltip;

	/** */
	public static String CreateTagDialog_CreateTagAndStartPushButton;

	/** */
	public static String CreateTagDialog_CreateTagAndStartPushToolTip;

	/** */
	public static String CreateTagDialog_CreateTagButton;

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

	/**
	 * Do not in-line this into the static initializer as the
	 * "Find Broken Externalized Strings" tool will not be
	 * able to find the corresponding bundle file.
	 */
	private static final String BUNDLE_NAME = "org.eclipse.egit.ui.internal.uitext"; //$NON-NLS-1$

	/** */
	public static String CommitActionHandler_calculatingChanges;

	/** */
	public static String CommitActionHandler_errorBuildingScope;

	/** */
	public static String CommitActionHandler_lookingForChanges;

	/** */
	public static String CommitActionHandler_repository;

	/** */
	public static String CommitEditor_couldNotShowRepository;

	/** */
	public static String CommitEditor_couldNotFindStashCommit;

	/** */
	public static String CommitEditor_couldNotGetStashIndex;

	/** */
	public static String CommitEditor_couldNotGetTags;

	/** */
	public static String CommitEditor_showGitRepo;

	/** */
	public static String CommitEditor_toolbarApplyStash;

	/** */
	public static String CommitEditor_toolbarDeleteStash;

	/** */
	public static String CommitEditor_toolbarCreateTag;

	/** */
	public static String CommitEditor_toolbarCreateBranch;

	/** */
	public static String CommitEditor_toolbarCheckOut;

	/** */
	public static String CommitEditor_toolbarCherryPick;

	/** */
	public static String CommitEditor_toolbarRevert;

	/** */
	public static String CommitEditor_toolbarShowInHistory;

	/** */
	public static String CommitEditor_TitleHeaderCommit;

	/** */
	public static String CommitEditor_TitleHeaderStashedCommit;

	/** */
	public static String CommitEditorInput_Name;

	/** */
	public static String CommitEditorInput_ToolTip;

	/** */
	public static String CommitEditorPage_JobName;

	/** */
	public static String CommitEditorPage_SectionBranchesEmpty;

	/** */
	public static String CommitEditorPage_LabelAuthor;

	/** */
	public static String CommitEditorPage_LabelAuthorRelative;

	/** */
	public static String CommitEditorPage_LabelCommitter;

	/** */
	public static String CommitEditorPage_LabelCommitterRelative;

	/** */
	public static String CommitEditorPage_LabelParent;

	/** */
	public static String CommitEditorPage_LabelTags;

	/** */
	public static String CommitEditorPage_SectionBranches;

	/** */
	public static String CommitEditorPage_SectionFiles;

	/** */
	public static String CommitEditorPage_SectionFilesEmpty;

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
	public static String StashEditorPage_UnstagedChanges;

	/** */
	public static String StashEditorPage_StagedChanges;

	/** */
	public static String StashEditorPage_LabelParent0;

	/** */
	public static String StashEditorPage_LabelParent1;

	/** */
	public static String StashEditorPage_LabelParent2;

	/** */
	public static String MultiPageEditorContentOutlinePage_NoOutline;

	/** */
	public static String Header_contextMenu_copy;

	/** */
	public static String Header_contextMenu_copy_SHA1;

	/** */

	public static String Header_copy_SHA1_error_title;

	/** */
	public static String Header_copy_SHA1_error_message;

	/** */
	public static String CommitFileDiffViewer_CanNotOpenCompareEditorTitle;

	/** */
	public static String CommitFileDiffViewer_CompareMenuLabel;

	/** */
	public static String CommitFileDiffViewer_CompareWorkingDirectoryMenuLabel;

	/** */
	public static String CommitFileDiffViewer_CopyFilePathMenuLabel;

	/** */
	public static String CommitFileDiffViewer_CopyAllFilePathsMenuLabel;

	/** */
	public static String CommitFileDiffViewer_MergeCommitMultiAncestorMessage;

	/** */
	public static String CommitFileDiffViewer_OpenInEditorMenuLabel;

	/** */
	public static String CommitFileDiffViewer_OpenPreviousInEditorMenuLabel;

	/** */
	public static String CommitFileDiffViewer_ShowAnnotationsMenuLabel;

	/** */
	public static String CommitFileDiffViewer_ShowInHistoryLabel;

	/** */
	public static String DiffViewer_FileDoesNotExist;

	/** */
	public static String DiffViewer_OpenComparisonLinkLabel;

	/** */
	public static String DiffViewer_OpenWorkingTreeLinkLabel;

	/** */
	public static String DiffViewer_OpenInEditorLinkLabel;

	/** */
	public static String DiffViewer_OpenPreviousLinkLabel;

	/** */
	public static String DiffViewer_notContainedInCommit;

	/** */
	public static String CommitGraphTable_CommitId;

	/** */
	public static String CommitGraphTable_CopyCommitIdLabel;

	/** */
	public static String CommitGraphTable_Committer;

	/** */
	public static String CommitGraphTable_committerDateColumn;

	/** */
	public static String CommitGraphTable_CompareWithEachOtherInTreeMenuLabel;

	/** */
	public static String CommitGraphTable_DeleteBranchAction;

	/** */
	public static String CommitGraphTable_DeleteTagAction;

	/** */
	public static String CommitGraphTable_HoverAdditionalTags;

	/** */
	public static String CommitGraphTable_messageColumn;

	/** */
	public static String CommitGraphTable_OpenCommitLabel;

	/** */
	public static String CommitGraphTable_RenameBranchMenuLabel;

	/** */
	public static String CommitGraphTable_UnableToCreatePatch;

	/** */
	public static String CommitGraphTable_UnableToWritePatch;

	/** */
	public static String GitSynchronizeWizard_synchronize;

	/** */
	public static String GitChangeSetModelProviderLabel;

	/** */
	public static String GitBranchSynchronizeWizardPage_title;

	/** */
	public static String GitBranchSynchronizeWizardPage_description;

	/** */
	public static String GitBranchSynchronizeWizardPage_repository;

	/** */
	public static String GitBranchSynchronizeWizardPage_destination;

	/** */
	public static String GitBranchSynchronizeWizardPage_includeUncommitedChanges;

	/** */
	public static String GitBranchSynchronizeWizardPage_fetchChangesFromRemote;

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
	public static String ImportProjectsWrongSelection;

	/** */
	public static String ImportProjectsSelectionInRepositoryRequired;

	/** */
	public static String ImportChangedProjectsCommand_ImportingChangedProjects;

	/** */
	public static String ImportChangedProjectsCommand_NoProjectsChangedTitle;

	/** */
	public static String ImportChangedProjectsCommand_NoProjectsChangedMessage;

	/** */
	public static String InteractiveRebaseView_abortItem_text;

	/** */
	public static String InteractiveRebaseView_continueItem_text;

	/** */
	public static String InteractiveRebaseView_LinkSelection;

	/** */
	public static String InteractiveRebaseView_refreshItem_text;

	/** */
	public static String InteractiveRebaseView_skipItem_text;

	/** */
	public static String InteractiveRebaseView_startItem_text;

	/** */
	public static String InteractiveRebaseView_this_partName;

	/** */
	public static String LocalNonWorkspaceTypedElement_errorWritingContents;

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
	public static String LoginDialog_ButtonLogin;

	/** */
	public static String LoginDialog_ButtonSave;

	/** */
	public static String NewRemoteDialog_ConfigurationMessage;

	/** */
	public static String NewRemoteDialog_DialogTitle;

	/** */
	public static String NewRemoteDialog_FetchRadio;

	/** */
	public static String NewRemoteDialog_InvalidRemoteName;

	/** */
	public static String NewRemoteDialog_NameLabel;

	/** */
	public static String NewRemoteDialog_PushRadio;

	/** */
	public static String NewRemoteDialog_RemoteAlreadyExistsMessage;

	/** */
	public static String NewRemoteDialog_WindowTitle;

	/** */
	public static String NewRemoteDialog_ButtonOK;

	/** */
	public static String NewRepositoryWizard_WizardTitle;

	/** */
	public static String NonBlockingWizardDialog_BackgroundJobName;

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
	public static String GitPreferenceRoot_MaxPullThreadsCount;

	/** */
	public static String GitPreferenceRoot_MaxPullThreadsCountTooltip;

	/** */
	public static String RemoteConnectionPreferencePage_SshClientLabel;

	/** */
	public static String RemoteConnectionPreferencePage_TimeoutLabel;

	/** */
	public static String RemoteConnectionPreferencePage_ZeroValueTooltip;

	/** */
	public static String RefreshPreferencesPage_RefreshIndexInterval;

	/** */
	public static String RefreshPreferencesPage_RefreshIndexIntervalTooltip;

	/** */
	public static String RefreshPreferencesPage_RefreshOnlyWhenActive;

	/** */
	public static String RefreshPreferencesPage_RefreshWhenIndexChange;

	/** */
	public static String RefUpdateElement_CommitCountDecoration;

	/** */
	public static String RefUpdateElement_CommitRangeDecoration;

	/** */
	public static String RefUpdateElement_statusRejectedNonFastForward;

	/** */
	public static String RefUpdateElement_UrisDecoration;

	/** */
	public static String RemoveCommand_ConfirmDeleteBareRepositoryMessage;

	/** */
	public static String RemoveCommand_ConfirmDeleteBareRepositoryTitle;

	/** */
	public static String RemoveCommand_RemoveRepositoriesJob;

	/** */
	public static String RemoveOrDeleteRepositoryCommand_DeleteRepositoryButton;

	/** */
	public static String RemoveOrDeleteRepositoryCommand_DialogMessage;

	/** */
	public static String RemoveOrDeleteRepositoryCommand_DialogTitle;

	/** */
	public static String RemoveOrDeleteRepositoryCommand_RemoveFromViewButton;

	/** */
	public static String RenameBranchDialog_DialogMessage;

	/** */
	public static String RenameBranchDialog_DialogTitle;

	/** */
	public static String RenameBranchDialog_RenameButtonLabel;

	/** */
	public static String RenameBranchDialog_WindowTitle;

	/** */
	public static String RenameBranchOnCommitHandler_RenameBranchDialogButton;

	/** */
	public static String RenameBranchOnCommitHandler_RenameBranchDialogMessage;

	/** */
	public static String RenameBranchOnCommitHandler_RenameBranchDialogTitle;

	/** */
	public static String RepositoryStatistics_Description;

	/** */
	public static String RepositoryStatistics_LooseObjects;

	/** */
	public static String RepositoryStatistics_NrOfObjects;

	/** */
	public static String RepositoryStatistics_NrOfPackfiles;

	/** */
	public static String RepositoryStatistics_NrOfRefs;

	/** */
	public static String RepositoryStatistics_PackedObjects;

	/** */
	public static String RepositoryStatistics_SpaceNeededOnFilesystem;

	/** */
	public static String RevertFailureDialog_Message;

	/** */
	public static String RevertFailureDialog_MessageNoFiles;

	/** */
	public static String RevertFailureDialog_ReasonChangesInIndex;

	/** */
	public static String RevertFailureDialog_ReasonChangesInWorkingDirectory;

	/** */
	public static String RevertFailureDialog_ReasonDeleteFailure;

	/** */
	public static String RevertFailureDialog_Title;

	/** */
	public static String RevertHandler_AlreadyRevertedMessage;

	/** */
	public static String RevertHandler_CommitsNotOnCurrentBranch;

	/** */
	public static String RevertHandler_Error_Title;

	/** */
	public static String RevertHandler_ErrorCheckingIfCommitsAreOnCurrentBranch;

	/** */
	public static String RevertHandler_JobName;

	/** */
	public static String RevertHandler_NoRevertTitle;

	/** */
	public static String RevertOperation_InternalError;

	/** */
	public static String SelectUriWizard_Title;

	/** */
	public static String AbstractConfigureRemoteDialog_AddRefSpecLabel;

	/** */
	public static String AbstractConfigureRemoteDialog_BranchLabel;

	/** */
	public static String AbstractConfigureRemoteDialog_ChangeRefSpecLabel;

	/** */
	public static String AbstractConfigureRemoteDialog_ChangeUriLabel;

	/** */
	public static String AbstractConfigureRemoteDialog_DeleteUriLabel;

	/** */
	public static String AbstractConfigureRemoteDialog_DetachedHeadMessage;

	/** */
	public static String AbstractConfigureRemoteDialog_DryRunButton;

	/** */
	public static String AbstractConfigureRemoteDialog_EditAdvancedLabel;

	/** */
	public static String AbstractConfigureRemoteDialog_EmptyClipboardDialogMessage;

	/** */
	public static String AbstractConfigureRemoteDialog_EmptyClipboardDialogTitle;

	/** */
	public static String AbstractConfigureRemoteDialog_InvalidRefDialogMessage;

	/** */
	public static String AbstractConfigureRemoteDialog_InvalidRefDialogTitle;

	/** */
	public static String AbstractConfigureRemoteDialog_MissingUriMessage;

	/** */
	public static String AbstractConfigureRemoteDialog_NoRefSpecDialogMessage;

	/** */
	public static String AbstractConfigureRemoteDialog_NoRefSpecDialogTitle;

	/** */
	public static String AbstractConfigureRemoteDialog_PasteRefSpecButton;

	/** */
	public static String AbstractConfigureRemoteDialog_RefMappingGroup;

	/** */
	public static String AbstractConfigureRemoteDialog_ReusedRemoteWarning;

	/** */
	public static String AbstractConfigureRemoteDialog_RevertButton;

	/** */
	public static String AbstractConfigureRemoteDialog_SaveButton;

	/** */
	public static String AbstractConfigureRemoteDialog_UriLabel;

	/** */
	public static String SimpleConfigureFetchDialog_DialogMessage;

	/** */
	public static String SimpleConfigureFetchDialog_DialogTitle;

	/** */
	public static String SimpleConfigureFetchDialog_MissingMappingMessage;

	/** */
	public static String SimpleConfigureFetchDialog_SaveAndFetchButton;

	/** */
	public static String SimpleConfigureFetchDialog_WindowTitle;

	/** */
	public static String SimpleConfigurePushDialog_AddPushUriButton;

	/** */
	public static String SimpleConfigurePushDialog_ChangePushUriButton;

	/** */
	public static String SimpleConfigurePushDialog_DefaultPushNoRefspec;

	/** */
	public static String SimpleConfigurePushDialog_DeletePushUriButton;

	/** */
	public static String SimpleConfigurePushDialog_DialogMessage;

	/** */
	public static String SimpleConfigurePushDialog_DialogTitle;

	/** */
	public static String SimpleConfigurePushDialog_PushUrisLabel;

	/** */
	public static String SimpleConfigurePushDialog_SaveAndPushButton;

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
	public static String SquashHandler_CommitsNotOnCurrentBranch;

	/** */
	public static String SquashHandler_Error_Title;

	/** */
	public static String SquashHandler_ErrorCheckingIfCommitsAreOnCurrentBranch;

	/** */
	public static String SquashHandler_InternalError;

	/** */
	public static String SquashHandler_JobName;

	/** */
	public static String SwitchToMenu_NewBranchMenuLabel;

	/** */
	public static String SwitchToMenu_NoCommonBranchesFound;

	/** */
	public static String SwitchToMenu_OtherMenuLabel;

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
	public static String CommitHelper_couldNotFindMergeMsg;

	/** */
	public static String CommitJob_AbortedByHook;

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
	public static String CommitSelectDialog_AuthoColumn;

	/** */
	public static String CommitSelectDialog_DateColumn;

	/** */
	public static String CommitSelectDialog_IdColumn;

	/** */
	public static String CommitSelectDialog_Message;

	/** */
	public static String CommitSelectDialog_MessageColumn;

	/** */
	public static String CommitSelectDialog_Title;

	/** */
	public static String CommitSelectDialog_WindowTitle;

	/** */
	public static String CommitSelectDialog_ChooseParentTitle;

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
	public static String CommitSelectionDialog_ButtonOK;

	/** */
	public static String HistoryCommitSelectionDialog_ButtonOK;

	/** */
	public static String CommitUI_commitFailed;

	/** */
	public static String CommitUI_pushFailedMessage;

	/** */
	public static String EgitUiEditorUtils_openFailed;

	/** */
	public static String GitActionContributor_ExpandAll;

	/** */
	public static String GitActionContributor_Push;

	/** */
	public static String GitActionContributor_Pull;

	/** */
	public static String GitLabelProvider_RefDescriptionFetchHead;

	/** */
	public static String GitLabelProvider_RefDescriptionHead;

	/** */
	public static String GitLabelProvider_RefDescriptionHeadSymbolic;

	/** */
	public static String GitLabelProvider_RefDescriptionOrigHead;

	/** */
	public static String GitLabelProvider_RefDescriptionStash;

	/** */
	public static String GitLabelProvider_UnableToRetrieveLabel;

	/** */
	public static String GitVariableResolver_InternalError;

	/** */
	public static String GitVariableResolver_NoSelectedResource;

	/** */
	public static String GitVariableResolver_VariableReferencesNonExistentResource;

	/** */
	public static String GitTemplateVariableResolver_GitConfigDescription;

	/** */
	public static String StagingView_CompareWithIndexMenuLabel;

	/** */
	public static String StagingView_UnstagedChanges;

	/** */
	public static String StagingView_UnstagedChangesTooltip;

	/** */
	public static String StagingView_ShowFileNamesFirst;

	/** */
	public static String StagingView_StagedChanges;

	/** */
	public static String StagingView_StagedChangesTooltip;

	/** */
	public static String StagingView_CommitMessage;

	/** */
	public static String StagingView_CommitAndPush;

	/** */
	public static String StagingView_Committer;

	/** */
	public static String StagingView_Author;

	/** */
	public static String StagingView_Ammend_Previous_Commit;

	/** */
	public static String StagingView_Add_Signed_Off_By;

	/** */
	public static String StagingView_Sign_Commit;

	/** */
	public static String StagingView_Add_Change_ID;

	/** */
	public static String StagingView_Assume_Unchanged;

	/** */
	public static String StagingView_Commit;

	/** */
	public static String StagingView_CommitToolTip;

	/** */
	public static String StagingView_cancelCommitAfterSaving;

	/** */
	public static String StagingView_checkoutFailed;

	/** */
	public static String StagingView_commitFailed;

	/** */
	public static String StagingView_committingNotPossible;

	/** */
	public static String StagingView_headCommitChanged;

	/** */
	public static String StagingView_noStagedFiles;

	/** */
	public static String StagingView_BareRepoSelection;

	/** */
	public static String StagingView_NoSelectionTitle;

	/** */
	public static String StagingView_CompareMode;

	/** */
	public static String StagingView_OpenNewCommits;

	/** */
	public static String StagingView_ColumnLayout;

	/** */
	public static String StagingView_RebaseAbort;

	/** */
	public static String StagingView_RebaseContinue;

	/** */
	public static String StagingView_RebaseLabel;

	/** */
	public static String StagingView_RebaseSkip;

	/** */
	public static String StagingView_Refresh;

	/** */
	public static String StagingView_GetRepo;

	/** */
	public static String StagingView_ReplaceWith;

	/** */
	public static String StagingView_LinkSelection;

	/** */
	public static String StagingView_replaceWithFileInGitIndex;

	/** */
	public static String StagingView_replaceWithHeadRevision;

	/** */
	public static String StagingView_UnstageAllItemMenuLabel;

	/** */
	public static String StagingView_UnstageAllItemTooltip;

	/** */
	public static String StagingView_UnstageItemMenuLabel;

	/** */
	public static String StagingView_UnstageItemTooltip;

	/** */
	public static String StagingView_UnstagedSort;

	/** */
	public static String StagingView_Untrack;

	/** */
	public static String StagingView_StageAllItemMenuLabel;

	/** */
	public static String StagingView_StageAllItemTooltip;

	/** */
	public static String StagingView_StageItemMenuLabel;

	/** */
	public static String StagingView_StageItemTooltip;

	/** */
	public static String StagingView_IgnoreItemMenuLabel;

	/** */
	public static String StagingView_InitialCommitText;

	/** */
	public static String StagingView_IgnoreFolderMenuLabel;

	/** */
	public static String StagingView_DeleteItemMenuLabel;

	/** */
	public static String StagingView_Presentation;

	/** */
	public static String StagingView_List;

	/** */
	public static String StagingView_Tree;

	/** */
	public static String StagingView_CompactTree;

	/** */
	public static String StagingView_Find;

	/** */
	public static String StagingView_MergeTool;

	/** */
	public static String StagingView_AddJob;

	/** */
	public static String StagingView_RemoveJob;

	/** */
	public static String StagingView_ResetJob;

	/** */
	public static String StagingView_IgnoreErrors;

	/** */
	public static String StagingView_MessageErrors;

	/** */
	public static String StagingView_LoadJob;

	/** */
	public static String StagingView_CopyPaths;

	/** */
	public static String StashApplyCommand_applyFailed;

	/** */
	public static String StashApplyCommand_jobTitle;

	/** */
	public static String StashCreateCommand_jobTitle;

	/** */
	public static String StashCreateCommand_messageEnterCommitMessage;

	/** */
	public static String StashCreateCommand_messageNoChanges;

	/** */
	public static String StashCreateCommand_stashFailed;

	/** */
	public static String StashCreateCommand_titleEnterCommitMessage;

	/** */
	public static String StashCreateCommand_titleNoChanges;

	/** */
	public static String StashCreateCommand_includeUntrackedLabel;

	/** */
	public static String StashCreateCommand_ButtonOK;

	/** */
	public static String StashDropCommand_confirmSingle;

	/** */
	public static String StashDropCommand_confirmMultiple;

	/** */
	public static String StashDropCommand_confirmTitle;

	/** */
	public static String StashDropCommand_dropFailed;

	/** */
	public static String StashDropCommand_stashCommitNotFound;

	/** */
	public static String StashDropCommand_jobTitle;

	/** */
	public static String StashDropCommand_buttonDelete;

	/** */
	public static String StashesMenu_StashChangesActionText;

	/** */
	public static String StashesMenu_StashListError;

	/** */
	public static String StashesMenu_NoStashedChangesText;

	/** */
	public static String StashesMenu_StashItemText;

	/** */
	public static String SubmoduleAddCommand_AddError;

	/** */
	public static String SubmoduleAddCommand_JobTitle;

	/** */
	public static String SubmodulePathWizardPage_ErrorPathMustBeEmpty;

	/** */
	public static String SubmodulePathWizardPage_Message;

	/** */
	public static String SubmodulePathWizardPage_PathLabel;

	/** */
	public static String SubmodulePathWizardPage_Title;

	/** */
	public static String SubmoduleSyncCommand_SyncError;

	/** */
	public static String SubmoduleSyncCommand_Title;

	/** */
	public static String SubmoduleUpdateCommand_Title;

	/** */
	public static String SubmoduleUpdateCommand_UpdateError;

	/** */
	public static String SubmoduleUpdateCommand_UncommittedChanges;

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

	/** */
	public static String GitModelSynchronize_fetchGitDataJobName;

	/** */
	public static String RebasePulldownAction_Continue;

	/** */
	public static String RebasePulldownAction_Skip;

	/** */
	public static String RebasePulldownAction_Abort;

	/** */
	public static String RewordHandler_CommitNotOnCurrentBranch;

	/** */
	public static String RewordHandler_Error_Title;

	/** */
	public static String RewordHandler_ErrorCheckingIfCommitIsOnCurrentBranch;

	/** */
	public static String RewordHandler_InternalError;

	/** */
	public static String RewordHandler_JobName;

	/** */
	public static String EditHandler_JobName;

	/** */
	public static String EditHandler_CommitNotOnCurrentBranch;

	/** */
	public static String EditHandler_Error_Title;

	/** */
	public static String EditHandler_ErrorCheckingIfCommitIsOnCurrentBranch;

	/** */
	public static String EditHandler_OpenStagingAndRebaseInteractiveViews;

	/** */
	public static String SynchronizeCommand_jobName;

	/** */
	public static String CloneFailureDialog_tile;

	/** */
	public static String CloneFailureDialog_dontShowAgain;

	/** */
	public static String CloneFailureDialog_checkList;

	/** */
	public static String CloneFailureDialog_checkList_git;

	/** */
	public static String CloneFailureDialog_checkList_ssh;

	/** */
	public static String CloneFailureDialog_checkList_https;

	/** */
	public static String GarbageCollectCommand_jobTitle;

	/** */
	public static String GarbageCollectCommand_failed;

	/** */
	public static String GitModelSynchronizeParticipant_initialScopeName;

	/** */
	public static String GitModelSynchronizeParticipant_noCachedSourceVariant;

	/** */
	public static String GitScmUrlImportWizardPage_title;

	/** */
	public static String GitScmUrlImportWizardPage_description;

	/** */
	public static String GitScmUrlImportWizardPage_importMaster;

	/** */
	public static String GitScmUrlImportWizardPage_importVersion;

	/** */
	public static String GitScmUrlImportWizardPage_counter;

	/** */
	public static String BranchEditDialog_Title;

	/** */
	public static String PushMenu_PushHEAD;

	/** */
	public static String PushMenu_PushBranch;

	/** */
	public static String DiffStyleRangeFormatter_diffTruncated;

	/** */
	public static String StagingViewPreferencePage_title;

	/** */
	public static String StagingViewPreferencePage_maxLimitListMode;

	/** */
	public static String CommandConfirmationHardResetDialog_resetButtonLabel;

	/** */
	public static String RepositorySearchDialog_BrowseDialogMessage;

	/** */
	public static String ProjectUtils_Invalid_ProjectFile;

	/** */
	public static String ShowInSystemExplorerActionHandler_JobTitle;

	/** */
	public static String EditableRevision_CannotSave;

	static {
		initializeMessages(BUNDLE_NAME, UIText.class);
	}

}
