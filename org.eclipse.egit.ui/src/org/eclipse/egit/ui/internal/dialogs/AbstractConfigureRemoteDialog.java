/*******************************************************************************
 * Copyright (C) 2017 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.ActionUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.components.TitleAndImageDialog;
import org.eclipse.egit.ui.internal.gerrit.GerritDialogSettings;
import org.eclipse.egit.ui.internal.push.RefSpecDialog;
import org.eclipse.egit.ui.internal.push.RefSpecWizard;
import org.eclipse.egit.ui.internal.repository.SelectUriWizard;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

/**
 * Common super class for fetch and push remote configuration dialogs. The
 * dialog has, besides the usual OK and CANCEL buttons, three more buttons:
 * {@link #DRY_RUN} to do a dry-run of the operation, {@link #SAVE_ONLY} to save
 * the {@link RemoteConfig} without performing the operation, and
 * {@link #REVERT} to undo all configuration changes and re-load the
 * {@link RemoteConfig} from the {@link Repository}. The OK button saves the
 * {@link RemoteConfig} <em>and</em> performs the operation.
 * <p>
 * Provides UI for the common URI and changing it, and also for adding,
 * changing, and deleting {@link RefSpec}s. Can be extended via
 * {@link #createAdditionalUriArea(Composite)} to provide more UI components.
 * </p>
 */
public abstract class AbstractConfigureRemoteDialog
		extends TitleAndImageDialog {

	/** Button ID for the dry-run button. */
	protected static final int DRY_RUN = 98;

	/** Button ID for the "save only" button. */
	protected static final int SAVE_ONLY = 97;

	/** Button ID for the revert button. */
	protected static final int REVERT = 96;

	private final boolean isPush;

	private final Repository repository;

	private RemoteConfig config;

	private final boolean showBranchInfo;

	// UI components

	/** A {@link StyledText} field for the URI. */
	protected StyledText commonUriText;

	/** A {@link TableViewer} showing {@link RefSpec}s. */
	protected TableViewer specViewer;

	/** An {@link IAction} that allows changing the common URI. */
	protected IAction changeCommonUriAction;

	/** An {@link IAction} that allows deleting the common URI. */
	protected IAction deleteCommonUriAction;

	/**
	 * An {@link IAction} to add a new {@link RefSpec} to the
	 * {@link #specViewer}.
	 */
	protected IAction addRefSpecAction;

	/**
	 * An {@link IAction} to change an existing {@link RefSpec} in the
	 * {@link #specViewer}.
	 */
	protected IAction changeRefSpecAction;

	/**
	 * An {@link IAction} opening an advanced {@link RefSpec} dialog.
	 */
	protected IAction addRefSpecAdvancedAction;

	/**
	 * button for changing the URI
	 */
	protected Button changeButton;

	/**
	 * Create a new {@link AbstractConfigureRemoteDialog}.
	 *
	 * @param parent
	 *            SWT {@link Shell} to parent the dialog on
	 * @param repository
	 *            the remote belongs to
	 * @param config
	 *            of the remote to be configured
	 * @param showBranchInfo
	 *            whether to show additional branch information
	 * @param isPush
	 *            whether this dialog is for configuring push
	 */
	protected AbstractConfigureRemoteDialog(Shell parent, Repository repository,
			RemoteConfig config, boolean showBranchInfo, boolean isPush) {
		super(parent, isPush ? UIIcons.WIZBAN_PUSH : UIIcons.WIZBAN_FETCH);
		setHelpAvailable(false);
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
		this.repository = repository;
		this.config = config;
		this.showBranchInfo = showBranchInfo;
		this.isPush = isPush;
	}

	/**
	 * Retrieves the {@link Repository} for which the remote is to be
	 * configured.
	 *
	 * @return the {@link Repository}
	 */
	protected Repository getRepository() {
		return repository;
	}

	/**
	 * Retrieves the {@link RemoteConfig} as configured currently.
	 *
	 * @return the {@link RemoteConfig}
	 */
	protected RemoteConfig getConfig() {
		return config;
	}

	/**
	 * Performs a dry-run of the operation.
	 *
	 * @param monitor
	 *            for progress reporting and cancellation, never {@code null}
	 */
	protected abstract void dryRun(IProgressMonitor monitor);

	/**
	 * Performs the operation for real. Invoked in the UI thread; lengthy
	 * operations should be performed in a background job.
	 */
	protected abstract void performOperation();

	/**
	 * Creates the OK button.
	 *
	 * @param parent
	 *            {@link Composite} containing the buttons
	 */
	protected abstract void createOkButton(Composite parent);

	/**
	 * Asks the user for a new {@link RefSpec} to be added to the current
	 * {@link RemoteConfig}.
	 *
	 * @return the new {@link RefSpec}, or {@code null} if none.
	 */
	protected abstract RefSpec getNewRefSpec();

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite main = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(main);
		GridDataFactory.fillDefaults().grab(true, true)
				.minSize(SWT.DEFAULT, SWT.DEFAULT).applyTo(main);

		if (showBranchInfo) {
			Composite branchArea = new Composite(main, SWT.NONE);
			GridLayoutFactory.swtDefaults().numColumns(2).equalWidth(false)
					.applyTo(branchArea);
			GridDataFactory.fillDefaults().grab(true, false)
					.applyTo(branchArea);

			Label branchLabel = new Label(branchArea, SWT.NONE);
			branchLabel.setText(UIText.AbstractConfigureRemoteDialog_BranchLabel);
			String branch;
			try {
				branch = getRepository().getBranch();
			} catch (IOException e) {
				branch = null;
			}
			if (branch == null || ObjectId.isId(branch)) {
				branch = UIText.AbstractConfigureRemoteDialog_DetachedHeadMessage;
			}
			Text branchText = new Text(branchArea, SWT.BORDER | SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false)
					.applyTo(branchText);
			branchText.setText(branch);

			addDefaultOriginWarning(main);

		}

		final Composite sameUriDetails = new Composite(main, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(4).equalWidth(false)
				.applyTo(sameUriDetails);
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(sameUriDetails);
		Label commonUriLabel = new Label(sameUriDetails, SWT.NONE);
		commonUriLabel.setText(UIText.AbstractConfigureRemoteDialog_UriLabel);
		commonUriText = new StyledText(sameUriDetails,
				SWT.SINGLE | SWT.READ_ONLY);
		commonUriText.setBackground(sameUriDetails.getBackground());
		commonUriText.setCaret(null);
		GridDataFactory.fillDefaults().grab(true, false)
				.align(SWT.FILL, SWT.CENTER).applyTo(commonUriText);
		changeCommonUriAction = new Action(
				UIText.AbstractConfigureRemoteDialog_ChangeUriLabel) {

			@Override
			public void run() {
				SelectUriWizard wiz;
				if (!commonUriText.getText().isEmpty()) {
					wiz = new SelectUriWizard(true, commonUriText.getText());
				} else {
					wiz = new SelectUriWizard(true);
				}
				if (new WizardDialog(getShell(), wiz).open() == Window.OK) {
					if (!commonUriText.getText().isEmpty()) {
						try {
							getConfig().removeURI(
									new URIish(commonUriText.getText()));
						} catch (URISyntaxException ex) {
							Activator.handleError(ex.getMessage(), ex, true);
						}
					}
					getConfig().addURI(wiz.getUri());
					updateControls();
				}
			}
		};
		deleteCommonUriAction = new Action(
				UIText.AbstractConfigureRemoteDialog_DeleteUriLabel) {

			@Override
			public void run() {
				getConfig().removeURI(getConfig().getURIs().get(0));
				updateControls();
			}
		};
		changeButton = createActionButton(sameUriDetails, SWT.PUSH,
				changeCommonUriAction);
		createActionButton(sameUriDetails, SWT.PUSH, deleteCommonUriAction)
				.setEnabled(false);

		commonUriText.addModifyListener(event -> deleteCommonUriAction
				.setEnabled(!commonUriText.getText().isEmpty()));

		createAdditionalUriArea(main);

		final Group refSpecGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, true)
				.minSize(SWT.DEFAULT, SWT.DEFAULT).applyTo(refSpecGroup);
		refSpecGroup.setText(UIText.AbstractConfigureRemoteDialog_RefMappingGroup);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(refSpecGroup);

		specViewer = new TableViewer(refSpecGroup, SWT.BORDER | SWT.MULTI);
		specViewer.setContentProvider(ArrayContentProvider.getInstance());
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 150)
				.minSize(SWT.DEFAULT, 30).grab(true, true)
				.applyTo(specViewer.getTable());

		addRefSpecAction = new Action(
				UIText.AbstractConfigureRemoteDialog_AddRefSpecLabel) {

			@Override
			public void run() {
				doAddRefSpec();
			}
		};
		changeRefSpecAction = new Action(
				UIText.AbstractConfigureRemoteDialog_ChangeRefSpecLabel) {

			@Override
			public void run() {
				doChangeRefSpec();
			}
		};
		addRefSpecAdvancedAction = new Action(
				UIText.AbstractConfigureRemoteDialog_EditAdvancedLabel) {

			@Override
			public void run() {
				doAdvanced();
			}
		};
		IAction deleteRefSpecAction = ActionUtils.createGlobalAction(
				ActionFactory.DELETE, this::doDeleteRefSpecs);
		IAction copyRefSpecAction = ActionUtils
				.createGlobalAction(ActionFactory.COPY, this::doCopy);
		IAction pasteRefSpecAction = ActionUtils
				.createGlobalAction(ActionFactory.PASTE, this::doPaste);
		IAction selectAllRefSpecsAction = ActionUtils.createGlobalAction(
				ActionFactory.SELECT_ALL,
				() -> {
					specViewer.getTable().selectAll();
					// selectAll doesn't fire a "selection changed" event
					specViewer.setSelection(specViewer.getSelection());
				});

		Composite buttonArea = new Composite(refSpecGroup, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(buttonArea);
		GridDataFactory.fillDefaults().grab(false, true)
				.minSize(SWT.DEFAULT, SWT.DEFAULT).applyTo(buttonArea);

		createActionButton(buttonArea, SWT.PUSH, addRefSpecAction);
		createActionButton(buttonArea, SWT.PUSH, changeRefSpecAction);
		createActionButton(buttonArea, SWT.PUSH, deleteRefSpecAction);
		createActionButton(buttonArea, SWT.PUSH, copyRefSpecAction);
		createActionButton(buttonArea, SWT.PUSH, pasteRefSpecAction);
		createActionButton(buttonArea, SWT.PUSH, addRefSpecAdvancedAction);

		MenuManager contextMenu = new MenuManager();
		contextMenu.setRemoveAllWhenShown(true);
		contextMenu.addMenuListener(manager -> {
			specViewer.getTable().setFocus();
			if (addRefSpecAction.isEnabled()) {
				manager.add(addRefSpecAction);
			}
			if (changeRefSpecAction.isEnabled()) {
				manager.add(changeRefSpecAction);
			}
			if (deleteRefSpecAction.isEnabled()) {
				manager.add(deleteRefSpecAction);
			}
			manager.add(new Separator());
			manager.add(copyRefSpecAction);
			manager.add(pasteRefSpecAction);
			manager.add(selectAllRefSpecsAction);
		});
		specViewer.getTable()
				.setMenu(contextMenu.createContextMenu(specViewer.getTable()));
		ActionUtils.setGlobalActions(specViewer.getTable(), deleteRefSpecAction,
				copyRefSpecAction, pasteRefSpecAction, selectAllRefSpecsAction);

		specViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = (IStructuredSelection) specViewer
						.getSelection();
				copyRefSpecAction.setEnabled(sel.size() == 1);
				changeRefSpecAction.setEnabled(sel.size() == 1);
				deleteRefSpecAction.setEnabled(!sel.isEmpty());
				selectAllRefSpecsAction.setEnabled(specViewer.getTable()
						.getItemCount() > 0
						&& sel.size() != specViewer.getTable().getItemCount());
			}
		});

		specViewer.addDoubleClickListener(event -> doChangeRefSpec());

		specViewer.getTable().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.DEL && deleteRefSpecAction.isEnabled()) {
					doDeleteRefSpecs();
				}
			}
		});

		// Initial action enablement (no selection in the specViewer):
		copyRefSpecAction.setEnabled(false);
		changeRefSpecAction.setEnabled(false);
		deleteRefSpecAction.setEnabled(false);

		applyDialogFont(main);
		return main;
	}

	/**
	 * Add a warning about this remote being used by other branches
	 *
	 * @param parent
	 */
	private void addDefaultOriginWarning(Composite parent) {
		List<String> otherBranches = new ArrayList<>();
		String currentBranch;
		try {
			currentBranch = getRepository().getBranch();
		} catch (IOException e) {
			// just don't show this warning
			return;
		}
		String currentRemote = getConfig().getName();
		Config repositoryConfig = getRepository().getConfig();
		Set<String> branches = repositoryConfig
				.getSubsections(ConfigConstants.CONFIG_BRANCH_SECTION);
		for (String branch : branches) {
			if (branch.equals(currentBranch)) {
				continue;
			}
			String remote = repositoryConfig.getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, branch,
					ConfigConstants.CONFIG_KEY_REMOTE);
			if ((remote == null
					&& currentRemote.equals(Constants.DEFAULT_REMOTE_NAME))
					|| (remote != null && remote.equals(currentRemote))) {
				otherBranches.add(branch);
			}
		}
		if (otherBranches.isEmpty()) {
			return;
		}
		Composite warningAboutOrigin = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2)
				.applyTo(warningAboutOrigin);
		Label warningLabel = new Label(warningAboutOrigin, SWT.NONE);
		warningLabel.setImage(PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJS_WARN_TSK));
		Text warningText = new Text(warningAboutOrigin, SWT.READ_ONLY);
		warningText.setText(NLS.bind(
				UIText.AbstractConfigureRemoteDialog_ReusedRemoteWarning,
				getConfig().getName(), Integer.valueOf(otherBranches.size())));
		warningText.setToolTipText(otherBranches.toString());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(warningLabel);
	}

	private void addRefSpec(RefSpec spec) {
		if (isPush) {
			getConfig().addPushRefSpec(spec);
		} else {
			getConfig().addFetchRefSpec(spec);
		}
	}

	private void removeRefSpec(RefSpec spec) {
		if (isPush) {
			getConfig().removePushRefSpec(spec);
		} else {
			getConfig().removeFetchRefSpec(spec);
		}
	}

	/**
	 * Hook method to create an additional area between the URI and the
	 * {@link RefSpec} viewer. This default implementation does nothing.
	 *
	 * @param parent
	 *            {@link Composite} the additional area shall use as parent
	 * @return the {@link Control}, or {@code null} if none
	 */
	protected Control createAdditionalUriArea(Composite parent) {
		return null;
	}

	/**
	 * Validate and enable/disable controls depending on the current state.
	 */
	protected abstract void updateControls();

	/**
	 * Set the initial focus in the dialog after creating all controls.
	 */
	protected void setInitialFocus() {
		if (commonUriText.getText().isEmpty()) {
			changeButton.setFocus();
		}
	}

	@Override
	protected final void createButtonsForButtonBar(Composite parent) {
		createOkButton(parent);
		createButton(parent, SAVE_ONLY,
				UIText.AbstractConfigureRemoteDialog_SaveButton, false);
		createButton(parent, DRY_RUN,
				UIText.AbstractConfigureRemoteDialog_DryRunButton, false);
		createButton(parent, REVERT,
				UIText.AbstractConfigureRemoteDialog_RevertButton, false);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected final void buttonPressed(int buttonId) {
		switch (buttonId) {
		case DRY_RUN:
			try {
				new ProgressMonitorDialog(getShell()).run(true, true,
						this::dryRun);
			} catch (InvocationTargetException e) {
				Activator.showError(e.getMessage(), e);
			} catch (InterruptedException e1) {
				// Ignore cancellation here
			}
			return;
		case REVERT:
			try {
				config = new RemoteConfig(repository.getConfig(),
						config.getName());
				updateControls();
			} catch (URISyntaxException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
			return;
		case OK:
		case SAVE_ONLY:
			StoredConfig repoConfig = getRepository().getConfig();
			boolean saved = false;
			try {
				config.update(repoConfig);
				repoConfig.save();
				saved = true;
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
			if (saved) {
				GerritDialogSettings.updateRemoteConfig(repository, config);
			}
			if (buttonId == OK) {
				performOperation();
			}
			okPressed();
			return;
		default:
			break;
		}
		super.buttonPressed(buttonId);
	}

	private void doPaste() {
		Clipboard clipboard = new Clipboard(getShell().getDisplay());
		try {
			String content = (String) clipboard
					.getContents(TextTransfer.getInstance());
			if (content == null) {
				MessageDialog.openConfirm(getShell(),
						UIText.AbstractConfigureRemoteDialog_EmptyClipboardDialogTitle,
						UIText.AbstractConfigureRemoteDialog_EmptyClipboardDialogMessage);
			}
			try {
				RefSpec spec = new RefSpec(content);
				Ref source;
				try {
					// TODO better checks for wild-cards and such
					source = getRepository().findRef(isPush ? spec.getSource()
							: spec.getDestination());
				} catch (IOException e) {
					source = null;
				}
				if (source != null || MessageDialog.openQuestion(getShell(),
						UIText.AbstractConfigureRemoteDialog_InvalidRefDialogTitle,
						NLS.bind(
								UIText.AbstractConfigureRemoteDialog_InvalidRefDialogMessage,
								spec.toString()))) {
					addRefSpec(spec);
				}
				updateControls();
			} catch (IllegalArgumentException e) {
				MessageDialog.openError(getShell(),
						UIText.AbstractConfigureRemoteDialog_NoRefSpecDialogTitle,
						UIText.AbstractConfigureRemoteDialog_NoRefSpecDialogMessage);
			}
		} finally {
			clipboard.dispose();
		}
	}

	private void doCopy() {
		String toCopy = ((IStructuredSelection) specViewer.getSelection())
				.getFirstElement().toString();
		Clipboard clipboard = new Clipboard(getShell().getDisplay());
		try {
			clipboard.setContents(new String[] { toCopy },
					new TextTransfer[] { TextTransfer.getInstance() });
		} finally {
			clipboard.dispose();
		}
	}

	private void doAddRefSpec() {
		RefSpec spec = getNewRefSpec();
		if (spec != null) {
			addRefSpec(spec);
			updateControls();
		}
	}

	private void doChangeRefSpec() {
		RefSpec oldSpec = (RefSpec) ((IStructuredSelection) specViewer
				.getSelection()).getFirstElement();
		RefSpecDialog dlg = new RefSpecDialog(getShell(), getRepository(),
				getConfig(), oldSpec, isPush);
		if (dlg.open() == Window.OK) {
			removeRefSpec(oldSpec);
			addRefSpec(dlg.getSpec());
		}
		updateControls();
	}

	private void doDeleteRefSpecs() {
		for (Object spec : ((IStructuredSelection) specViewer.getSelection())
				.toArray()) {
			removeRefSpec((RefSpec) spec);
		}
		updateControls();
	}

	private void doAdvanced() {
		RefSpecWizard wizard = new RefSpecWizard(getRepository(), getConfig(),
				isPush);
		if (new WizardDialog(getShell(), wizard).open() == Window.OK) {
			updateControls();
		}
	}

	private Button createActionButton(Composite parent, int style,
			IAction action) {
		Button button = new Button(parent, style);
		button.setText(action.getText());
		button.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				action.run();
			}
		});
		IPropertyChangeListener listener = event -> {
			if (IAction.ENABLED.equals(event.getProperty())) {
				if (!button.isDisposed()) {
					if (Display.getCurrent() == null) {
						button.getShell().getDisplay().syncExec(() -> {
							if (!button.isDisposed()) {
								button.setEnabled(action.isEnabled());
							}
						});
					} else {
						button.setEnabled(action.isEnabled());
					}
				}
			}
		};
		button.addDisposeListener(
				event -> action.removePropertyChangeListener(listener));
		action.addPropertyChangeListener(listener);
		GridDataFactory.fillDefaults().applyTo(button);
		return button;
	}
}
