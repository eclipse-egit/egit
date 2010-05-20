/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
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
	public static String AddToIndexAction_addingFiles;

	/** */
	public static String AddToIndexAction_addingFilesFailed;

	/** */
	public static String AddToIndexAction_indexesWithUnmergedEntries;

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
	public static String WizardProjectsImportPage_projectLabel;

	/** */
	public static String WizardProjectsImportPage_SearchingMessage;

	/** */
	public static String WizardProjectsImportPage_ProcessingMessage;

	/** */
	public static String WizardProjectsImportPage_projectsInWorkspace;

	/** */
	public static String WizardProjectsImportPage_CheckingMessage;

	/** */
	public static String WizardProjectImportPage_errorMessage;

	/** */
	public static String WizardProjectsImportPage_CreateProjectsTask;

	/** */
	public static String SelectRemoteNamePage_ConfigureFetch_button;

	/** */
	public static String SelectRemoteNamePage_ConfigurePush_button;

	/** */
	public static String SelectRemoteNamePage_MustConfigureSomething_message;

	/** */
	public static String SelectRemoteNamePage_NameInUseMessage;

	/** */
	public static String SelectRemoteNamePage_NameMustNotBeEmptyMessage;

	/** */
	public static String SelectRemoteNamePage_RemoteNameLabel;

	/** */
	public static String SelectRemoteNamePage_RemoteNameMessage;

	/** */
	public static String SelectRemoteNamePage_RemoteNameTitle;

	/** */
	public static String SelectRemoteNamePage_SelectRemoteNameMessage;

	/** */
	public static String SetQuickdiffBaselineAction_setQuickdiffBaseline;

	/** */
	public static String SharingWizard_windowTitle;

	/** */
	public static String SharingWizard_failed;

	/** */
	public static String GenerateHistoryJob_errorComputingHistory;

	/** */
	public static String ExistingOrNewPage_CreateButton;

	/** */
	public static String ExistingOrNewPage_title;

	/** */
	public static String ExistingOrNewPage_description;

	/** */
	public static String ExistingOrNewPage_ErrorFailedToCreateRepository;

	/** */
	public static String ExistingOrNewPage_ErrorFailedToRefreshRepository;

	/** */
	public static String ExistingOrNewPage_HeaderPath;

	/** */
	public static String ExistingOrNewPage_HeaderProject;

	/** */
	public static String ExistingOrNewPage_HeaderRepository;

	/** */
	public static String ExistingOrNewPage_SymbolicValueEmptyMapping;

	/** */
	public static String GitCloneWizard_abortingCloneMsg;

	/** */
	public static String GitCloneWizard_abortingCloneTitle;

	/** */
	public static String GitCloneWizard_CloneFailedHeading;

	/** */
	public static String GitCloneWizard_CloneCanceledMessage;

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
	public static String GitDocument_errorLoadCommit;

	/** */
	public static String GitDocument_errorLoadTree;

	/** */
	public static String GitDocument_errorRefreshQuickdiff;

	/** */
	public static String GitDocument_errorResolveQuickdiff;

	/** */
	public static String GitHistoryPage_compareMode;

	/** */
	public static String GitHistoryPage_CompareVersions;

	/** */
	public static String GitHistoryPage_CompareWithWorking;

	/** */
	public static String GitHistoryPage_errorLookingUpPath;

	/** */
	public static String GitHistoryPage_errorParsingHead;

	/** */
	public static String GitHistoryPage_errorReadingHeadCommit;

	/** */
	public static String GitHistoryPage_CreatePatch;

	/** */
	public static String GitHistoryPage_Date;

	/** */
	public static String GitHistoryPage_FileNotInCommit;

	/** */
	public static String GitHistoryPage_fileNotFound;

	/** */
	public static String GitHistoryPage_find;

	/** */
	public static String GitHistoryPage_notContainedInCommits;

	/** */
	public static String GitHistoryPage_open;

	/** */
	public static String GitHistoryPage_openFailed;

	/** */
	public static String GitHistoryPage_From;

	/** */
	public static String GitHistoryPage_Subject;

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
	public static String GitProjectPropertyPage_ValueEmptyRepository;

	/** */
	public static String GitProjectPropertyPage_ValueUnbornBranch;

	/** */
	public static String GitProjectsImportPage_NoProjectsMessage;

	/** */
	public static String RepositoryPropertySource_ConfigureKeysAction;

	/** */
	public static String RepositoryPropertySource_EffectiveConfigurationAction;

	/** */
	public static String RepositoryPropertySource_EffectiveConfigurationCategory;

	/** */
	public static String RepositoryPropertySource_ErrorHeader;

	/** */
	public static String RepositoryPropertySource_GlobalConfigurationCategory;

	/** */
	public static String RepositoryPropertySource_RepositoryConfigurationCategory;

	/** */
	public static String RepositoryPropertySource_RestoreStandardAction;

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
	public static String RepositorySearchDialog_DeepSearch_button;

	/** */
	public static String RepositorySearchDialog_RepositoriesFound_message;

	/** */
	public static String RepositorySearchDialog_ScanningForRepositories_message;

	/** */
	public static String RepositorySearchDialog_ToggleSelection_button;

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
	public static String RepositorySearchDialog_browse;

	/** */
	public static String RepositorySearchDialog_directory;

	/** */
	public static String RepositorySearchDialog_errorOccurred;

	/** */
	public static String RepositorySearchDialog_search;

	/** */
	public static String RepositorySearchDialog_searchRepositories;

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
	public static String RepositorySelectionPage_ShowPreviousURIs_HoverText;

	/** */
	public static String RepositorySelectionPage_uriChoice;

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
	public static String CloneDestinationPage_fieldRequired;

	/** */
	public static String CloneDestinationPage_browseButton;

	/** */
	public static String CloneDestinationPage_errorNotEmptyDir;

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
	public static String RefSpecPanel_errorRemoteConfigDescription;

	/** */
	public static String RefSpecPanel_errorRemoteConfigTitle;

	/** */
	public static String RefSpecPanel_fetch;

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
	public static String QuickdiffBaselineOperation_baseline;

	/** */
	public static String ResetAction_errorResettingHead;

	/** */
	public static String ResetAction_repositoryState;

	/** */
	public static String ResetAction_reset;

	/** */
	public static String ResetQuickdiffBaselineAction_resetQuickdiffBaseline;

	/** */
	public static String ResetQuickdiffBaselineHeadParentAction_0;

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
	public static String HistoryPage_pathnameColumn;

	/** */
	public static String HistoryPage_refreshJob;

	/** */
	public static String HistoryPage_findbar_findTooltip;

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
	public static String HistoryPreferencePage_title;

	/** */
	public static String PushAction_wrongURIDescription;

	/** */
	public static String PushAction_wrongURITitle;

	/** */
	public static String PushConfiguredRemoteAction_NoSpecDefined;

	/** */
	public static String PushConfiguredRemoteAction_NoUpdatesFoundMessage;

	/** */
	public static String PushConfiguredRemoteAction_NoUrisMessage;

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
	public static String CommitAction_errorCommittingChanges;

	/** */
	public static String CommitAction_errorComputingDiffs;

	/** */
	public static String CommitAction_errorDuringCommit;

	/** */
	public static String CommitAction_errorOnCommit;

	/** */
	public static String CommitAction_errorPreparingTrees;

	/** */
	public static String CommitAction_errorRetrievingCommit;

	/** */
	public static String CommitAction_errorWritingTrees;

	/** */
	public static String CommitAction_failedToUpdate;

	/** */
	public static String CommitAction_InternalError;

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
	public static String CommitDialog_File;

	/** */
	public static String CommitDialog_problemFindingFileStatus;

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
	public static String ConfigureKeysDialog_AddStandardButton;

	/** */
	public static String ConfigureKeysDialog_AlreadyThere_Message;

	/** */
	public static String ConfigureKeysDialog_DeleteButton;

	/** */
	public static String ConfigureKeysDialog_DialogTitle;

	/** */
	public static String ConfigureKeysDialog_NewButton;

	/** */
	public static String ConfigureKeysDialog_NewKeyLabel;

	/** */
	public static String ConfigureRemoteWizard_WizardTitle_Change;

	/** */
	public static String ConfigureRemoteWizard_WizardTitle_New;

	/** */
	public static String ConfigureUriPage_Add_button;

	/** */
	public static String ConfigureUriPage_Change_button;

	/** */
	public static String ConfigureUriPage_ConfigureFetch_pagetitle;

	/** */
	public static String ConfigureUriPage_ConfigurePush_pagetitle;

	/** */
	public static String ConfigureUriPage_FetchUri_label;

	/** */
	public static String ConfigureUriPage_MissingUri_message;

	/** */
	public static String ConfigureUriPage_MissingUris_message;

	/** */
	public static String ConfigureUriPage_ParsingProblem_message;

	/** */
	public static String ConfigureUriPage_Remove_button;

	/** */
	public static String CommitDialog_ValueHelp_Message;

	/** */
	public static String CommitMessageViewer_author;

	/** */
	public static String CommitMessageViewer_child;

	/** */
	public static String CommitMessageViewer_commit;

	/** */
	public static String CommitMessageViewer_committer;

	/** */
	public static String CommitMessageViewer_errorGettingFileDifference;

	/** */
	public static String CommitMessageViewer_parent;

	/** */
	public static String CompareWithIndexAction_errorOnAddToIndex;

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
	public static String CreateBranchPage_BranchAlreadyExistsMessage;

	/** */
	public static String CreateBranchPage_BranchNameLabel;

	/** */
	public static String CreateBranchPage_CheckingOutMessage;

	/** */
	public static String CreateBranchPage_CheckoutButton;

	/** */
	public static String CreateBranchPage_ChooseBranchAndNameMessage;

	/** */
	public static String CreateBranchPage_ChosseNameMessage;

	/** */
	public static String CreateBranchPage_CreatingBranchMessage;

	/** */
	public static String CreateBranchPage_MissingNameMessage;

	/** */
	public static String CreateBranchPage_MissingSourceMessage;

	/** */
	public static String CreateBranchPage_SourceBranchLabel;

	/** */
	public static String CreateBranchPage_SourceBranchTooltip;

	/** */
	public static String CreateBranchPage_Title;

	/** */
	public static String PushResultTable_columnStatusRepo;

	/** */
	public static String PushResultTable_columnDst;

	/** */
	public static String PushResultTable_columnSrc;

	/** */
	public static String PushResultTable_columnMode;

	/** */
	public static String PushResultTable_statusUnexpected;

	/** */
	public static String PushResultTable_statusConnectionFailed;

	/** */
	public static String PushResultTable_statusDetailChanged;

	/** */
	public static String PushResultTable_refNonExisting;

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
	public static String FetchConfiguredRemoteAction_NoSpecsDefinedMessage;

	/** */
	public static String FetchConfiguredRemoteAction_NoUrisDefinedMessage;

	/** */
	public static String FetchResultDialog_labelEmptyResult;

	/** */
	public static String FetchResultDialog_labelNonEmptyResult;

	/** */
	public static String FetchResultDialog_title;

	/** */
	public static String FetchResultTable_columnDst;

	/** */
	public static String FetchResultTable_columnSrc;

	/** */
	public static String FetchResultTable_columnStatus;

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
	public static String FetchWizard_cantSaveMessage;

	/** */
	public static String FetchWizard_cantSaveTitle;

	/** */
	public static String FetchWizard_fetchNotSupported;

	/** */
	public static String FetchWizard_jobName;

	/** */
	public static String FetchWizard_transportError;

	/** */
	public static String FetchWizard_transportNotSupportedMessage;

	/** */
	public static String FetchWizard_transportNotSupportedTitle;

	/** */
	public static String FetchWizard_windowTitleDefault;

	/** */
	public static String FetchWizard_windowTitleWithSource;

	/** */
	public static String FileDiffContentProvider_errorGettingDifference;

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
	public static String BranchAction_branchFailed;

	/** */
	public static String BranchAction_cannotCheckout;

	/** */
	public static String BranchAction_checkingOut;

	/** */
	public static String BranchAction_repositoryState;

	/** */
	public static String BranchSelectionDialog_TitleCheckout;

	/** */
	public static String BranchSelectionDialog_TitleReset;

	/** */
	public static String BranchSelectionDialog_OkReset;

	/** */
	public static String BranchSelectionDialog_ErrorCouldNotCreateNewRef;

	/** */
	public static String BranchSelectionDialog_ErrorCouldNotRenameRef;

	/** */
	public static String BranchSelectionDialog_ResetType;

	/** */
	public static String BranchSelectionDialog_ResetTypeSoft;

	/** */
	public static String BranchSelectionDialog_ResetTypeMixed;

	/** */
	public static String BranchSelectionDialog_ResetTypeHard;

	/** */
	public static String BranchSelectionDialog_ReallyResetTitle;

	/** */
	public static String BranchSelectionDialog_ReallyResetMessage;

	/** */
	public static String BranchSelectionDialog_QuestionNewBranchTitle;

	/** */
	public static String BranchSelectionDialog_QuestionNewBranchNameMessage;

	/** */
	public static String BranchSelectionDialog_QuestionNewBranchMessage;

	/** */
	public static String BranchSelectionDialog_NewBranch;

	/** */
	public static String BranchSelectionDialog_ErrorAlreadyExists;

	/** */
	public static String BranchSelectionDialog_ErrorCouldNotResolve;

	/** */
	public static String BranchSelectionDialog_ErrorInvalidRefName;

	/** */
	public static String BranchSelectionDialog_OkCheckout;

	/** */
	public static String BranchSelectionDialog_Refs;

	/** */
	public static String BranchSelectionDialog_Rename;

	/** */
	public static String HistoryPage_ShowAllVersionsForProject;

	/** */
	public static String HistoryPage_ShowAllVersionsForRepo;

	/** */
	public static String HistoryPage_ShowAllVersionsForFolder;

	/** */
	public static String Decorator_exceptionMessage;

	/** */
	public static String DecoratorPreferencesPage_addVariablesTitle;

	/** */
	public static String DecoratorPreferencesPage_addVariablesAction;

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
	public static String DecoratorPreferencesPage_fileFormatDefault;

	/** */
	public static String DecoratorPreferencesPage_projectFormatDefault;

	/** */
	public static String DecoratorPreferencesPage_folderFormatDefault;

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
	public static String DecoratorPreferencesPage_selectVariablesToAdd;

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
	public static String IgnoreAction_jobName;

	/** */
	public static String IgnoreAction_taskName;

	/** */
	public static String IgnoreAction_error;

	/** */
	public static String Track_addToVersionControl;

	/** */
	public static String RepositoriesView_ActionCanceled_Message;

	/** */
	public static String RepositoriesView_Add_Button;

	/** */
	public static String RepositoriesView_AddRepository_MenuItem;

	/** */
	public static String RepositoriesView_AddRepository_Tooltip;

	/** */
	public static String RepositoriesView_BranchCreationFailureMessage;

	/** */
	public static String RepositoriesView_BranchDeletionFailureMessage;

	/** */
	public static String RepositoriesView_Branches_Nodetext;

	/** */
	public static String RepositoriesView_CheckingOutMessage;

	/** */
	public static String RepositoriesView_CheckOut_MenuItem;

	/** */
	public static String RepositoriesView_ClipboardContentNoGitRepoMessage;

	/** */
	public static String RepositoriesView_ClipboardContentNotDirectoryMessage;

	/** */
	public static String RepositoriesView_Clone_Tooltip;

	/** */
	public static String RepositoriesView_CollapseAllMenu;

	/** */
	public static String RepositoriesView_ConfigureFetchMenu;

	/** */
	public static String RepositoriesView_ConfigurePushMenu;

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
	public static String RepositoriesView_CopyPathToClipboardMenu;

	/** */
	public static String RepositoriesView_CreateFetch_menu;

	/** */
	public static String RepositoriesView_CreatePush_menu;

	/** */
	public static String RepositoriesView_DeleteBranchMenu;

	/** */
	public static String RepositoriesView_DeleteRepoDeterminProjectsMessage;

	/** */
	public static String RepositoriesView_DoPushMenuItem;

	/** */
	public static String RepositoriesView_Error_WindowTitle;

	/** */
	public static String RepositoriesView_ErrorHeader;

	/** */
	public static String RepositoriesView_FetchMenu;

	/** */
	public static String RepositoriesView_Import_Button;

	/** */
	public static String RepositoriesView_ImportProjectsMenu;

	/** */
	public static String RepositoriesView_ImportRepository_MenuItem;

	/** */
	public static String RepositoriesView_LinkWithSelection_action;

	/** */
	public static String RepositoriesView_NewBranchMenu;

	/** */
	public static String RepositoriesView_NewBranchTitle;

	/** */
	public static String RepositoriesView_NewRemoteMenu;

	/** */
	public static String RepositoriesView_NothingToPasteMessage;

	/** */
	public static String RepositoriesView_OpenInTextEditor_menu;

	/** */
	public static String RepositoriesView_OpenPropertiesMenu;

	/** */
	public static String RepositoriesView_PasteFailureTitle;

	/** */
	public static String RepositoriesView_PasteMenu;

	/** */
	public static String RepositoriesView_PasteRepoAlreadyThere;

	/** */
	public static String RepositoriesView_Refresh_Button;

	/** */
	public static String RepositoriesView_RemotesNodeText;

	/** */
	public static String RepositoriesView_Remove_MenuItem;

	/** */
	public static String RepositoriesView_RemoveFetch_menu;

	/** */
	public static String RepositoriesView_RemovePush_menu;

	/** */
	public static String RepositoriesView_RemoveRemoteMenu;

	/** */
	public static String RepositoriesView_WorkingDir_treenode;

	/** */
	public static String RepositoriesViewContentProvider_ExceptionNodeText;

	/** */
	public static String RepositoriesViewLabelProvider_BareRepositoryMessage;

	/** */
	public static String RepositoriesViewLabelProvider_LocalBranchesNodetext;

	/** */
	public static String RepositoriesViewLabelProvider_RemoteBrancheNodetext;

	/** */
	public static String RepositoriesViewLabelProvider_SymbolicRefNodeText;

	/** */
	public static String RepositoriesViewLabelProvider_TagsNodeText;

	/** */
	public static String DiscardChangesAction_confirmActionTitle;

	/** */
	public static String DiscardChangesAction_confirmActionMessage;

	/** */
	public static String DiscardChangesAction_discardChanges;

	/** */
	public static String Disconnect_disconnect;

	/** */
	public static String GitCompareFileRevisionEditorInput_contentIdentifier;

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
	public static String GitSelectWizardPage_AutoShareButton;

	/** */
	public static String GitSelectWizardPage_ImportAsGeneralButton;

	/** */
	public static String GitSelectWizardPage_ImportExistingButton;

	/** */
	public static String GitSelectWizardPage_InteractiveShareButton;

	/** */
	public static String GitSelectWizardPage_NoShareButton;

	/** */
	public static String GitSelectWizardPage_ProjectCreationHeader;

	/** */
	public static String GitSelectWizardPage_SharingProjectsHeader;

	/** */
	public static String GitSelectWizardPage_UseNewProjectsWizardButton;

	/** */
	public static String GitSelectWizardPage_WizardTitle;

	/** */
	public static String GitShareProjectsPage_AbortedMessage;

	/** */
	public static String GitShareProjectsPage_NoNewProjectMessage;

	/** */
	public static String GitShareProjectsPage_NoRepoForProjectMessage;

	/** */
	public static String GitShareProjectsPage_NoRepoFoundMessage;

	/** */
	public static String GitShareProjectsPage_NothingSelectedMessage;

	/** */
	public static String GitShareProjectsPage_PageTitle;

	/** */
	public static String GitShareProjectsPage_ProjectAlreadySharedMessage;

	/** */
	public static String GitShareProjectsPage_ProjectNameLabel;

	/** */
	public static String GitShareProjectsPage_RepositoryLabel;

 	/** */
	public static String MergeAction_CannotMerge;

	/** */
	public static String MergeAction_ChangedFiles;

	/** */
	public static String MergeAction_ErrorMergeEnabling;

	/** */
	public static String MergeAction_HeadIsNoBranch;

	/** */
	public static String MergeAction_JobNameMerge;

	/** */
	public static String MergeAction_ProblemMerge;

	/** */
	public static String MergeAction_UnableMerge;

	/** */
	public static String MergeAction_WrongRepositoryState;

	/** */
	public static String MergeTargetSelectionDialog_ButtonMerge;

	/** */
	public static String MergeTargetSelectionDialog_OnlyFastForward;

	/** */
	public static String MergeTargetSelectionDialog_SelectRef;

	/** */
	public static String MergeTargetSelectionDialog_TitleMerge;

	/** */
	public static String MixedResetToRevisionAction_mixedReset;

	/** */
	public static String UIIcons_errorDeterminingIconBase;

	/** */
	public static String UIIcons_errorLoadingPluginImage;

	/** */
	public static String Untrack_untrack;

	/** */
	public static String Update_update;

	/** */
	public static String TagAction_cannotCheckout;

	/** */
	public static String TagAction_cannotGetBranchName;

	/** */
	public static String TagAction_repositoryState;

	/** */
	public static String TagAction_errorDuringTagging;

	/** */
	public static String TagAction_errorWhileGettingRevCommits;

	/** */
	public static String TagAction_unableToResolveHeadObjectId;

	/** */
	public static String TagAction_errorWhileMappingRevTag;

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
	public static String CommitCombo_showSuggestedCommits;

	/**
	 * Do not in-line this into the static initializer as the
	 * "Find Broken Externalized Strings" tool will not be
	 * able to find the corresponding bundle file.
	 */
	private static final String BUNDLE_NAME = "org.eclipse.egit.ui.uitext"; //$NON-NLS-1$

	static {
		initializeMessages(BUNDLE_NAME, UIText.class);
	}
}
