/*******************************************************************************
 * Copyright (c) 2010, 2016 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Lars Vogel <Lars.Vogel@vogella.com> - Bug 497820
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.internal.FileChecker;
import org.eclipse.egit.core.internal.FileChecker.CheckResult;
import org.eclipse.egit.core.internal.FileChecker.CheckResultEntry;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commands.shared.AbortRebaseCommand;
import org.eclipse.egit.ui.internal.commands.shared.AbstractRebaseCommandHandler;
import org.eclipse.egit.ui.internal.commands.shared.SkipRebaseCommand;
import org.eclipse.egit.ui.internal.dialogs.CheckoutConflictDialog;
import org.eclipse.egit.ui.internal.merge.GitMergeEditorInput;
import org.eclipse.egit.ui.internal.merge.MergeModeDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.RebaseResult.Status;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Display the result of a rebase.
 */
public class RebaseResultDialog extends MessageDialog {
	private static final String SPACE = " "; //$NON-NLS-1$

	private static final Image INFO = PlatformUI.getWorkbench()
			.getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK);

	private final RebaseResult result;

	private final Repository repo;

	private final Set<String> conflictPaths = new HashSet<>();

	private Button toggleButton;

	private Button startMergeButton;

	private Button skipCommitButton;

	private Button abortRebaseButton;

	private Button doNothingButton;

	private Group nextStepsGroup;

	/**
	 * @param result
	 *            the result to show
	 * @param repository
	 */
	public static void show(final RebaseResult result,
			final Repository repository) {
		switch (result.getStatus()) {
		case ABORTED:
		case INTERACTIVE_PREPARED:
			// Don't show the dialog
			return;
		case CONFLICTS:
			PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
				Shell shell = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getShell();
				new CheckoutConflictDialog(shell, repository,
						result.getConflicts()).open();
			});
			return;
		case STOPPED:
		case STASH_APPLY_CONFLICTS:
			// Show the dialog
			break;
		default:
			if (!Activator.getDefault().getPreferenceStore()
					.getBoolean(UIPreferences.SHOW_REBASE_CONFIRM)) {
				return;
			}
			break;
		}
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getShell();
			new RebaseResultDialog(shell, repository, result).open();
		});
	}

	private static String getTitle(Status status) {
		switch (status) {
		case OK:
			return UIText.RebaseResultDialog_SuccessfullyFinished;
		case ABORTED:
			return UIText.RebaseResultDialog_Aborted;
		case STOPPED:
			return UIText.RebaseResultDialog_Stopped;
		case EDIT:
			return UIText.RebaseResultDialog_Edit;
		case FAILED:
			return UIText.RebaseResultDialog_Failed;
		case UP_TO_DATE:
			return UIText.RebaseResultDialog_UpToDate;
		case FAST_FORWARD:
			return UIText.RebaseResultDialog_FastForward;
		case NOTHING_TO_COMMIT:
			return UIText.RebaseResultDialog_NothingToCommit;
		case INTERACTIVE_PREPARED:
			return UIText.RebaseResultDialog_InteractivePrepared;
		case UNCOMMITTED_CHANGES:
			return UIText.RebaseResultDialog_UncommittedChanges;
		case STASH_APPLY_CONFLICTS:
			return UIText.RebaseResultDialog_SuccessfullyFinished + ".\n" + //$NON-NLS-1$
					UIText.RebaseResultDialog_stashApplyConflicts;
		default:
			throw new IllegalStateException(status.name());
		}
	}

	/**
	 * @param status
	 * @return text describing rebase status in short form
	 */
	public static String getStatusText(Status status) {
		switch (status) {
		case OK:
			return UIText.RebaseResultDialog_StatusOK;
		case ABORTED:
			return UIText.RebaseResultDialog_StatusAborted;
		case STOPPED:
			return UIText.RebaseResultDialog_StatusStopped;
		case EDIT:
			return UIText.RebaseResultDialog_StatusEdit;
		case FAILED:
			return UIText.RebaseResultDialog_StatusFailed;
		case CONFLICTS:
			return UIText.RebaseResultDialog_StatusConflicts;
		case UP_TO_DATE:
			return UIText.RebaseResultDialog_StatusUpToDate;
		case FAST_FORWARD:
			return UIText.RebaseResultDialog_StatusFastForward;
		case NOTHING_TO_COMMIT:
			return UIText.RebaseResultDialog_StatusNothingToCommit;
		case UNCOMMITTED_CHANGES:
			return UIText.RebaseResultDialog_UncommittedChanges;
		case INTERACTIVE_PREPARED:
			return UIText.RebaseResultDialog_StatusInteractivePrepared;
		case STASH_APPLY_CONFLICTS:
			return UIText.RebaseResultDialog_SuccessfullyFinished + ".\n" + //$NON-NLS-1$
					UIText.RebaseResultDialog_stashApplyConflicts;
		}
		return status.toString();
	}

	private static String[] getButtonLabel(Status status) {
		String[] buttonLabel = new String[1];
		switch (status) {
		case EDIT:
		case CONFLICTS:
		case STOPPED:
		case INTERACTIVE_PREPARED:
		case STASH_APPLY_CONFLICTS:
			buttonLabel[0] = IDialogConstants.PROCEED_LABEL;
			break;
		default:
			buttonLabel[0] = IDialogConstants.CLOSE_LABEL;
		}
		return buttonLabel;
	}

	/**
	 * @param shell
	 * @param repository
	 * @param result
	 */
	private RebaseResultDialog(Shell shell, Repository repository,
			RebaseResult result) {
		super(shell, UIText.RebaseResultDialog_DialogTitle, INFO,
				getTitle(result.getStatus()),
				result.getStatus() == Status.FAILED ? MessageDialog.ERROR
						: MessageDialog.INFORMATION,
				getButtonLabel(result.getStatus()), 0);
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
		this.repo = repository;
		this.result = result;
	}

	@Override
	protected Control createCustomArea(Composite parent) {

		if (result.getStatus() == Status.STOPPED)
			return createStoppedDialogArea(parent);
		if (result.getStatus() == Status.FAILED
				|| result.getStatus() == Status.CONFLICTS)
			return createFailedOrConflictDialog(parent);
		createToggleButton(parent);
		return null;
	}

	private Control createFailedOrConflictDialog(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		createFailedOrConflictsParts(composite, result);

		return composite;
	}

	/**
	 * Create the items in composite necessary for a rebase result
	 *
	 * @param composite
	 * @param result
	 */
	public static void createFailedOrConflictsParts(Composite composite,
			RebaseResult result) {
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		composite.setLayout(gridLayout);
		// result
		Label resultLabel = new Label(composite, SWT.NONE);
		resultLabel.setText(UIText.MergeResultDialog_result);
		resultLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false,
				false));
		Text resultText = new Text(composite, SWT.READ_ONLY);
		resultText.setText(getStatusText(result.getStatus()));
		if (!result.getStatus().isSuccessful())
			resultText.setForeground(composite.getParent().getDisplay()
					.getSystemColor(SWT.COLOR_RED));
		resultText.setSelection(resultText.getCaretPosition());
		resultText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		if (result.getStatus() == Status.FAILED) {
			StringBuilder paths = new StringBuilder();
			Label pathsLabel = new Label(composite, SWT.NONE);
			pathsLabel.setText(UIText.MergeResultDialog_failed);
			pathsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false,
					false));
			Text pathsText = new Text(composite, SWT.READ_ONLY);
			pathsText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false,
					false));
			Set<Entry<String, MergeFailureReason>> failedPaths = result
					.getFailingPaths().entrySet();
			int n = 0;
			for (Map.Entry<String, MergeFailureReason> e : failedPaths) {
				if (n > 0)
					paths.append(Text.DELIMITER);
				paths.append(e.getValue());
				paths.append("\t"); //$NON-NLS-1$
				paths.append(e.getKey());
				n++;
				if (n > 10 && failedPaths.size() > 15)
					break;
			}
			if (n < failedPaths.size()) {
				paths.append(Text.DELIMITER);
				paths.append(MessageFormat.format(
						UIText.MergeResultDialog_nMore,
						Integer.valueOf(n - failedPaths.size())));
			}
			pathsText.setText(paths.toString());
		} else if (result.getStatus() == Status.CONFLICTS) {
			StringBuilder paths = new StringBuilder();
			Label pathsLabel = new Label(composite, SWT.NONE);
			pathsLabel.setText(UIText.MergeResultDialog_conflicts);
			pathsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false,
					false));
			Text pathsText = new Text(composite, SWT.READ_ONLY);
			pathsText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false,
					false));
			List<String> conflList = result.getConflicts();
			int n = 0;
			for (String e : conflList) {
				if (n > 0)
					paths.append(Text.DELIMITER);
				paths.append(e);
				n++;
				if (n > 10 && conflList.size() > 15)
					break;
			}
			if (n < conflList.size()) {
				paths.append(Text.DELIMITER);
				paths.append(MessageFormat.format(
						UIText.MergeResultDialog_nMore,
						Integer.valueOf(n - conflList.size())));
			}
			pathsText.setText(paths.toString());
		}
	}

	private Control createStoppedDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().indent(0, 0).grab(true, true).applyTo(
				main);

		Group commitGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(commitGroup);
		commitGroup.setText(UIText.RebaseResultDialog_DetailsGroup);
		commitGroup.setLayout(new GridLayout(1, false));

		Label commitIdLabel = new Label(commitGroup, SWT.NONE);
		commitIdLabel.setText(UIText.RebaseResultDialog_CommitIdLabel);
		Text commitId = new Text(commitGroup, SWT.READ_ONLY | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(commitId);

		Label commitMessageLabel = new Label(commitGroup, SWT.NONE);
		commitMessageLabel
				.setText(UIText.RebaseResultDialog_CommitMessageLabel);
		TextViewer commitMessage = new TextViewer(commitGroup, SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 60)
				.applyTo(commitMessage.getControl());

		boolean conflictListFailure = false;
		DirCache dc = null;
		try (RevWalk rw = new RevWalk(repo)) {
			// the commits might not have been fully loaded
			RevCommit commit = rw.parseCommit(result.getCurrentCommit());
			commitMessage.getTextWidget().setText(commit.getFullMessage());
			commitId.setText(commit.name());
			dc = repo.lockDirCache();
			for (int i = 0; i < dc.getEntryCount(); i++)
				if (dc.getEntry(i).getStage() > 0)
					conflictPaths.add(dc.getEntry(i).getPathString());
			if (conflictPaths.size() > 0) {
				message = NLS.bind(UIText.RebaseResultDialog_Conflicting,
						Integer.valueOf(conflictPaths.size()));
				messageLabel.setText(message);
			}
		} catch (IOException e) {
			// the file list will be empty
			conflictListFailure = true;
		} finally {
			if (dc != null)
				dc.unlock();
		}

		boolean mergeToolAvailable = true;
		final CheckResult checkResult;
		if (!conflictListFailure) {
			checkResult = FileChecker.checkFiles(repo, conflictPaths);
			mergeToolAvailable = checkResult.isOk();
		}
		else {
			checkResult = null;
			mergeToolAvailable = false;
		}

		if (conflictListFailure) {
			Label failureLabel = new Label(main, SWT.NONE);
			failureLabel
					.setText(UIText.RebaseResultDialog_ConflictListFailureMessage);
		} else {
			if (checkResult != null && !checkResult.isOk()) {
				Label failureLabel = new Label(main, SWT.NONE);
				failureLabel
					.setText(getProblemDescription(checkResult));
			}
			Label conflictListLabel = new Label(main, SWT.NONE);
			conflictListLabel
			.setText(UIText.RebaseResultDialog_DiffDetailsLabel);
			TableViewer conflictList = new TableViewer(main, SWT.BORDER);
			GridDataFactory.fillDefaults().span(2, 1).grab(true, true).applyTo(
					conflictList.getTable());
			conflictList.setContentProvider(ArrayContentProvider.getInstance());
			conflictList.setInput(conflictPaths);
			conflictList.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					String path = (String) element;
					if (checkResult != null && !checkResult.isOk()) {
						CheckResultEntry entry = checkResult.getEntry(path);
						if (entry != null) {
							if (!entry.inWorkspace)
								return UIText.RebaseResultDialog_notInWorkspace + SPACE + path;
							if (!entry.shared)
								return UIText.RebaseResultDialog_notShared + SPACE + path;
						}
					}
					return super.getText(element);
				}

			});
		}

		Group actionGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(actionGroup);
		actionGroup.setText(UIText.RebaseResultDialog_ActionGroupTitle);
		actionGroup.setLayout(new GridLayout(1, false));

		nextStepsGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(nextStepsGroup);
		nextStepsGroup.setText(UIText.RebaseResultDialog_NextSteps);
		nextStepsGroup.setLayout(new GridLayout(1, false));
		final TextViewer nextSteps = new TextViewer(nextStepsGroup, SWT.MULTI
				| SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 60)
				.applyTo(nextSteps.getControl());
		nextSteps.getTextWidget().setText(
				UIText.RebaseResultDialog_NextStepsAfterResolveConflicts);

		startMergeButton = new Button(actionGroup, SWT.RADIO);
		startMergeButton.setText(UIText.RebaseResultDialog_StartMergeRadioText);
		startMergeButton.setEnabled(mergeToolAvailable);
		startMergeButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (startMergeButton.getSelection()) {
					nextSteps.getTextWidget().setText(
							UIText.RebaseResultDialog_NextStepsAfterResolveConflicts);
					getButton(getDefaultButtonIndex())
							.setText(IDialogConstants.PROCEED_LABEL);
				}
			}
		});

		skipCommitButton = new Button(actionGroup, SWT.RADIO);
		skipCommitButton.setText(UIText.RebaseResultDialog_SkipCommitButton);
		skipCommitButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (skipCommitButton.getSelection()) {
					nextSteps.getTextWidget().setText(""); //$NON-NLS-1$
					getButton(getDefaultButtonIndex())
							.setText(IDialogConstants.PROCEED_LABEL);
				}
			}
		});

		abortRebaseButton = new Button(actionGroup, SWT.RADIO);
		abortRebaseButton
				.setText(UIText.RebaseResultDialog_AbortRebaseRadioText);
		abortRebaseButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (abortRebaseButton.getSelection()) {
					nextSteps.getTextWidget().setText(""); //$NON-NLS-1$
					getButton(getDefaultButtonIndex())
							.setText(IDialogConstants.ABORT_LABEL);
				}
			}
		});

		doNothingButton = new Button(actionGroup, SWT.RADIO);
		doNothingButton.setText(UIText.RebaseResultDialog_DoNothingRadioText);
		doNothingButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (doNothingButton.getSelection()) {
					nextSteps.getTextWidget().setText(
							UIText.RebaseResultDialog_NextStepsDoNothing);
					getButton(getDefaultButtonIndex())
							.setText(IDialogConstants.CLOSE_LABEL);
				}
			}
		});

		if (mergeToolAvailable)
			startMergeButton.setSelection(true);
		else
			doNothingButton.setSelection(true);

		commitGroup.pack();
		applyDialogFont(main);

		return main;
	}

	private static String getProblemDescription(CheckResult checkResult) {
		StringBuilder result = new StringBuilder();
		if (checkResult.containsNonWorkspaceFiles())
			result.append(UIText.RebaseResultDialog_notInWorkspaceMessage);
		if (checkResult.containsNotSharedResources()) {
			if (result.length() > 0)
				result.append('\n');
			result.append(UIText.RebaseResultDialog_notSharedMessage);
		}
		return result.toString();
	}

	@Override
	protected void buttonPressed(int buttonId) {
		// store the preference to hide these dialogs
		if (toggleButton != null)
			Activator.getDefault().getPreferenceStore().setValue(
					UIPreferences.SHOW_REBASE_CONFIRM,
					!toggleButton.getSelection());
		if (buttonId == IDialogConstants.OK_ID) {
			if (result.getStatus() != Status.STOPPED) {
				super.buttonPressed(buttonId);
				return;
			}
			if (startMergeButton.getSelection()) {
				super.buttonPressed(buttonId);
				// open the merge tool
				IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
						.getProjects();
				for (IProject project : projects) {
					RepositoryMapping mapping = RepositoryMapping
							.getMapping(project);
					if (mapping != null && mapping.getRepository().equals(repo)) {
						try {
							// make sure to refresh before opening the merge
							// tool
							project
									.refreshLocal(IResource.DEPTH_INFINITE,
											null);
						} catch (CoreException e) {
							Activator.handleError(e.getMessage(), e, false);
						}
					}
				}
				List<IPath> locationList = new ArrayList<>();
				IPath repoWorkdirPath = new Path(repo.getWorkTree().getPath());
				for (String repoPath : conflictPaths) {
					IPath location = repoWorkdirPath.append(repoPath);
					locationList.add(location);
				}
				IPath[] locations = locationList.toArray(new IPath[locationList
						.size()]);
				int mergeMode = Activator.getDefault().getPreferenceStore()
						.getInt(UIPreferences.MERGE_MODE);
				CompareEditorInput input;
				if (mergeMode == 0) {
					MergeModeDialog dlg = new MergeModeDialog(getParentShell());
					if (dlg.open() != Window.OK)
						return;
					input = new GitMergeEditorInput(dlg.useWorkspace(),
							locations);
				} else {
					boolean useWorkspace = mergeMode == 1;
					input = new GitMergeEditorInput(useWorkspace,
							locations);
				}
				CompareUI.openCompareEditor(input);
				return;
			} else if (skipCommitButton.getSelection()) {
				// skip the rebase
				SkipRebaseCommand skipCommand = new SkipRebaseCommand();
				execute(skipCommand);
			} else if (abortRebaseButton.getSelection()) {
				// abort the rebase
				AbortRebaseCommand abortCommand = new AbortRebaseCommand();
				execute(abortCommand);
			} else if (doNothingButton.getSelection()) {
				// nothing
			}
		}
		super.buttonPressed(buttonId);
	}

	private void execute(AbstractRebaseCommandHandler command) {
		try {
			command.execute(repo);
		} catch (ExecutionException e) {
			Activator.showError(e.getMessage(), e);
		}
	}

	private void createToggleButton(Composite parent) {
		boolean toggleState = !Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.SHOW_REBASE_CONFIRM);
		toggleButton = new Button(parent, SWT.CHECK | SWT.LEFT);
		toggleButton.setText(UIText.RebaseResultDialog_ToggleShowButton);
		toggleButton.setSelection(toggleState);
	}
}
