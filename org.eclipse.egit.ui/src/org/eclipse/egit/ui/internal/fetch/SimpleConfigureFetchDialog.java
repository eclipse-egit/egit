/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.push.RefSpecDialog;
import org.eclipse.egit.ui.internal.push.RefSpecWizard;
import org.eclipse.egit.ui.internal.repository.SelectUriWizard;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
 * A simplified wizard for configuring fetch
 */
public class SimpleConfigureFetchDialog extends TitleAreaDialog {
	private static final int DRY_RUN = 98;

	private static final int SAVE_ONLY = 97;

	private static final int REVERT = 96;

	private static final String ADVANCED_MODE_PREFERENCE = SimpleConfigureFetchDialog.class
			.getName()
			+ "_ADVANCED_MODE"; //$NON-NLS-1$

	private final Repository repository;

	private RemoteConfig config;

	private final boolean showBranchInfo;

	private Text commonUriText;

	private TableViewer specViewer;

	private Button changeCommonUri;

	private Button addRefSpec;

	private Button changeRefSpec;

	private Button addRefSpecAdvanced;

	/**
	 * @param shell
	 * @param repository
	 * @return the dialog to open, or null
	 */
	public static Dialog getDialog(Shell shell, Repository repository) {
		RemoteConfig configToUse = getConfiguredRemote(repository);
		return new SimpleConfigureFetchDialog(shell, repository, configToUse,
				true);
	}

	/**
	 * @param shell
	 * @param repository
	 * @param remoteName
	 *            the remote name to use
	 * @return the dialog to open, or null
	 */
	public static Dialog getDialog(Shell shell, Repository repository,
			String remoteName) {
		RemoteConfig configToUse;
		try {
			configToUse = new RemoteConfig(repository.getConfig(), remoteName);
		} catch (URISyntaxException e) {
			Activator.handleError(e.getMessage(), e, true);
			return null;
		}
		return new SimpleConfigureFetchDialog(shell, repository, configToUse,
				false);
	}

	/**
	 * @param repository
	 * @return the configured remote for the current branch, or the default
	 *         remote; <code>null</code> if a local branch is checked out that
	 *         points to "." as remote
	 */
	public static RemoteConfig getConfiguredRemote(Repository repository) {
		String branch;
		try {
			branch = repository.getBranch();
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
			return null;
		}
		if (branch == null)
			return null;

		String remoteName;
		if (ObjectId.isId(branch))
			remoteName = Constants.DEFAULT_REMOTE_NAME;
		else
			remoteName = repository.getConfig().getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, branch,
					ConfigConstants.CONFIG_REMOTE_SECTION);

		// check if we find the configured and default Remotes
		List<RemoteConfig> allRemotes;
		try {
			allRemotes = RemoteConfig.getAllRemoteConfigs(repository
					.getConfig());
		} catch (URISyntaxException e) {
			allRemotes = new ArrayList<RemoteConfig>();
		}

		RemoteConfig defaultConfig = null;
		RemoteConfig configuredConfig = null;
		for (RemoteConfig config : allRemotes) {
			if (config.getName().equals(Constants.DEFAULT_REMOTE_NAME))
				defaultConfig = config;
			if (remoteName != null && config.getName().equals(remoteName))
				configuredConfig = config;
		}

		RemoteConfig configToUse = configuredConfig != null ? configuredConfig
				: defaultConfig;
		return configToUse;
	}

	/**
	 * @param shell
	 * @param repository
	 * @param config
	 * @param showBranchInfo
	 *            should be true if this is used for upstream configuration; if
	 *            false, branch information will be hidden in the dialog
	 */
	private SimpleConfigureFetchDialog(Shell shell, Repository repository,
			RemoteConfig config, boolean showBranchInfo) {
		super(shell);
		setHelpAvailable(false);
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
		this.repository = repository;
		this.config = config;
		this.showBranchInfo = showBranchInfo;

		// Add default fetch ref spec if this is a new remote config
		if (config.getFetchRefSpecs().isEmpty()
				&& !repository.getConfig()
						.getSubsections(ConfigConstants.CONFIG_REMOTE_SECTION)
						.contains(config.getName())) {
			StringBuilder defaultRefSpec = new StringBuilder();
			defaultRefSpec.append('+');
			defaultRefSpec.append(Constants.R_HEADS);
			defaultRefSpec.append('*').append(':');
			defaultRefSpec.append(Constants.R_REMOTES);
			defaultRefSpec.append(config.getName());
			defaultRefSpec.append(RefSpec.WILDCARD_SUFFIX);
			config.addFetchRefSpec(new RefSpec(defaultRefSpec.toString()));
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		boolean advancedMode = Activator.getDefault().getPreferenceStore()
				.getBoolean(ADVANCED_MODE_PREFERENCE);
		final Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		if (showBranchInfo) {
			Composite branchArea = new Composite(main, SWT.NONE);
			GridLayoutFactory.swtDefaults().numColumns(2).equalWidth(false)
					.applyTo(branchArea);
			GridDataFactory.fillDefaults().grab(true, false)
					.applyTo(branchArea);

			Label branchLabel = new Label(branchArea, SWT.NONE);
			branchLabel.setText(UIText.SimpleConfigureFetchDialog_BranchLabel);
			String branch;
			try {
				branch = repository.getBranch();
			} catch (IOException e2) {
				branch = null;
			}
			if (branch == null || ObjectId.isId(branch))
				branch = UIText.SimpleConfigureFetchDialog_DetachedHeadMessage;
			Text branchText = new Text(branchArea, SWT.BORDER | SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false)
					.applyTo(branchText);
			branchText.setText(branch);
		}

		addDefaultOriginWarningIfNeeded(main);

		final Composite sameUriDetails = new Composite(main, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(4).equalWidth(false)
				.applyTo(sameUriDetails);
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(sameUriDetails);
		Label commonUriLabel = new Label(sameUriDetails, SWT.NONE);
		commonUriLabel.setText(UIText.SimpleConfigureFetchDialog_UriLabel);
		commonUriText = new Text(sameUriDetails, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(commonUriText);
		changeCommonUri = new Button(sameUriDetails, SWT.PUSH);
		changeCommonUri
				.setText(UIText.SimpleConfigureFetchDialog_ChangeUriButton);
		changeCommonUri.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SelectUriWizard wiz;
				if (commonUriText.getText().length() > 0)
					wiz = new SelectUriWizard(true, commonUriText.getText());
				else
					wiz = new SelectUriWizard(true);
				if (new WizardDialog(getShell(), wiz).open() == Window.OK) {
					if (commonUriText.getText().length() > 0)
						try {
							config.removeURI(new URIish(commonUriText.getText()));
						} catch (URISyntaxException ex) {
							Activator.handleError(ex.getMessage(), ex, true);
						}
					config.addURI(wiz.getUri());
					updateControls();
				}
			}
		});

		final Button deleteCommonUri = new Button(sameUriDetails, SWT.PUSH);
		deleteCommonUri
				.setText(UIText.SimpleConfigureFetchDialog_DeleteUriButton);
		deleteCommonUri.setEnabled(false);
		deleteCommonUri.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				config.removeURI(config.getURIs().get(0));
				updateControls();
			}
		});

		commonUriText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				deleteCommonUri
						.setEnabled(commonUriText.getText().length() > 0);
			}
		});

		final Group refSpecGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(refSpecGroup);
		refSpecGroup.setText(UIText.SimpleConfigureFetchDialog_RefMappingGroup);
		refSpecGroup.setLayout(new GridLayout(2, false));

		specViewer = new TableViewer(refSpecGroup, SWT.BORDER | SWT.MULTI);
		specViewer.setContentProvider(ArrayContentProvider.getInstance());
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 150)
				.minSize(SWT.DEFAULT, 30).grab(true, true)
				.applyTo(specViewer.getTable());

		specViewer.getTable().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.stateMask == SWT.MOD1 && e.keyCode == 'v')
					doPaste();
			}
		});

		Composite buttonArea = new Composite(refSpecGroup, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(buttonArea);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(buttonArea);

		addRefSpec = new Button(buttonArea, SWT.PUSH);
		addRefSpec.setText(UIText.SimpleConfigureFetchDialog_AddRefSpecButton);
		GridDataFactory.fillDefaults().applyTo(addRefSpec);
		addRefSpec.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SimpleFetchRefSpecWizard wiz = new SimpleFetchRefSpecWizard(
						repository, config);
				WizardDialog dlg = new WizardDialog(getShell(), wiz);
				if (dlg.open() == Window.OK)
					config.addFetchRefSpec(wiz.getSpec());
				updateControls();
			}
		});

		changeRefSpec = new Button(buttonArea, SWT.PUSH);
		changeRefSpec
				.setText(UIText.SimpleConfigureFetchDialog_ChangeRefSpecButton);
		changeRefSpec.setEnabled(false);
		GridDataFactory.fillDefaults().exclude(!advancedMode)
				.applyTo(changeRefSpec);
		changeRefSpec.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				RefSpec oldSpec = (RefSpec) ((IStructuredSelection) specViewer
						.getSelection()).getFirstElement();
				RefSpecDialog dlg = new RefSpecDialog(getShell(), repository,
						config, oldSpec, false);
				if (dlg.open() == Window.OK) {
					config.removeFetchRefSpec(oldSpec);
					config.addFetchRefSpec(dlg.getSpec());
				}
				updateControls();
			}
		});
		final Button deleteRefSpec = new Button(buttonArea, SWT.PUSH);
		deleteRefSpec
				.setText(UIText.SimpleConfigureFetchDialog_DeleteRefSpecButton);
		GridDataFactory.fillDefaults().applyTo(deleteRefSpec);
		deleteRefSpec.setEnabled(false);
		deleteRefSpec.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Object spec : ((IStructuredSelection) specViewer
						.getSelection()).toArray())
					config.removeFetchRefSpec((RefSpec) spec);
				updateControls();
			}
		});

		final Button copySpec = new Button(buttonArea, SWT.PUSH);
		copySpec.setText(UIText.SimpleConfigureFetchDialog_CopyRefSpecButton);
		GridDataFactory.fillDefaults().applyTo(copySpec);
		copySpec.setEnabled(false);
		copySpec.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String toCopy = ((IStructuredSelection) specViewer
						.getSelection()).getFirstElement().toString();
				Clipboard clipboard = new Clipboard(getShell().getDisplay());
				try {
					clipboard.setContents(new String[] { toCopy },
							new TextTransfer[] { TextTransfer.getInstance() });
				} finally {
					clipboard.dispose();
				}
			}
		});

		final Button pasteSpec = new Button(buttonArea, SWT.PUSH);
		pasteSpec.setText(UIText.SimpleConfigureFetchDialog_PateRefSpecButton);
		GridDataFactory.fillDefaults().applyTo(pasteSpec);
		pasteSpec.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doPaste();
			}
		});

		addRefSpecAdvanced = new Button(buttonArea, SWT.PUSH);
		GridDataFactory.fillDefaults().applyTo(addRefSpecAdvanced);

		addRefSpecAdvanced
				.setText(UIText.SimpleConfigureFetchDialog_EditAdvancedButton);
		addRefSpecAdvanced.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (new WizardDialog(getShell(), new RefSpecWizard(repository,
						config, false)).open() == Window.OK)
					updateControls();
			}
		});

		specViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = (IStructuredSelection) specViewer
						.getSelection();
				copySpec.setEnabled(sel.size() == 1);
				changeRefSpec.setEnabled(sel.size() == 1);
				deleteRefSpec.setEnabled(!sel.isEmpty());
			}
		});

		applyDialogFont(main);
		return main;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				UIText.SimpleConfigureFetchDialog_SaveAndFetchButton, true);
		createButton(parent, SAVE_ONLY,
				UIText.SimpleConfigureFetchDialog_SaveButton, false);
		createButton(parent, DRY_RUN,
				UIText.SimpleConfigureFetchDialog_DryRunButton, false);

		createButton(parent, REVERT,
				UIText.SimpleConfigureFetchDialog_RevertButton, false);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.SimpleConfigureFetchDialog_WindowTitle);
	}

	@Override
	public void create() {
		super.create();
		setTitle(NLS.bind(UIText.SimpleConfigureFetchDialog_DialogTitle,
				config.getName()));
		setMessage(UIText.SimpleConfigureFetchDialog_DialogMessage);

		updateControls();
	}

	private void updateControls() {
		setErrorMessage(null);
		boolean anyUri = false;
		if (!config.getURIs().isEmpty()) {
			commonUriText.setText(config.getURIs().get(0).toPrivateString());
			anyUri = true;
		} else
			commonUriText.setText(""); //$NON-NLS-1$

		specViewer.setInput(config.getFetchRefSpecs());
		specViewer.getTable().setEnabled(true);

		addRefSpec.setEnabled(anyUri);
		addRefSpecAdvanced.setEnabled(anyUri);

		if (config.getURIs().isEmpty())
			setErrorMessage(UIText.SimpleConfigureFetchDialog_MissingUriMessage);
		else if (config.getFetchRefSpecs().isEmpty())
			setErrorMessage(UIText.SimpleConfigureFetchDialog_MissingMappingMessage);

		boolean anySpec = !config.getFetchRefSpecs().isEmpty();
		getButton(OK).setEnabled(anyUri && anySpec);
		getButton(SAVE_ONLY).setEnabled(anyUri && anySpec);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == DRY_RUN) {
			try {
				new ProgressMonitorDialog(getShell()).run(false, true,
						new IRunnableWithProgress() {
							public void run(IProgressMonitor monitor)
									throws InvocationTargetException,
									InterruptedException {
								int timeout = Activator
										.getDefault()
										.getPreferenceStore()
										.getInt(
												UIPreferences.REMOTE_CONNECTION_TIMEOUT);
								FetchOperationUI op = new FetchOperationUI(
										repository, config, timeout, true);
								try {
									FetchResultDialog dlg;
									dlg = new FetchResultDialog(getShell(),
											repository, op.execute(monitor), op
													.getSourceString());
									dlg.showConfigureButton(false);
									dlg.open();
								} catch (CoreException e) {
									Activator.handleError(e.getMessage(), e,
											true);
								}

							}
						});
			} catch (InvocationTargetException e1) {
				Activator.handleError(e1.getMessage(), e1, true);
			} catch (InterruptedException e1) {
				// ignore here
			}
			return;
		}
		if (buttonId == REVERT) {
			try {
				config = new RemoteConfig(repository.getConfig(), config
						.getName());
				updateControls();
			} catch (URISyntaxException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
			return;
		}
		if (buttonId == OK || buttonId == SAVE_ONLY) {
			config.update(repository.getConfig());
			try {
				repository.getConfig().save();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, true);
			}
			if (buttonId == OK)
				try {
					new ProgressMonitorDialog(getShell()).run(false, true,
							new IRunnableWithProgress() {
								public void run(IProgressMonitor monitor)
										throws InvocationTargetException,
										InterruptedException {
									int timeout = Activator
											.getDefault()
											.getPreferenceStore()
											.getInt(
													UIPreferences.REMOTE_CONNECTION_TIMEOUT);
									FetchOperationUI op = new FetchOperationUI(
											repository, config, timeout, false);
									op.start();
								}
							});
				} catch (InvocationTargetException e) {
					Activator.handleError(e.getMessage(), e, true);
				} catch (InterruptedException e) {
					Activator.handleError(e.getMessage(), e, true);
				}
			okPressed();
			return;
		}
		super.buttonPressed(buttonId);
	}

	private void doPaste() {
		Clipboard clipboard = new Clipboard(getShell().getDisplay());
		try {
			String content = (String) clipboard.getContents(TextTransfer
					.getInstance());
			if (content == null)
				MessageDialog
						.openConfirm(
								getShell(),
								UIText.SimpleConfigureFetchDialog_NothingToPasteMessage,
								UIText.SimpleConfigureFetchDialog_EmptyClipboardMessage);
			try {
				RefSpec spec = new RefSpec(content);
				Ref source;
				try {
					// TODO better checks for wild-cards and such
					source = repository.getRef(spec.getDestination());
				} catch (IOException e1) {
					source = null;
				}
				if (source != null
						|| MessageDialog
								.openQuestion(
										getShell(),
										UIText.SimpleConfigureFetchDialog_InvalidRefDialogTitle,
										NLS
												.bind(
														UIText.SimpleConfigureFetchDialog_InvalidRefDialogMessage,
														spec.toString())))
					config.addFetchRefSpec(spec);

				updateControls();
			} catch (IllegalArgumentException ex) {
				MessageDialog
						.openError(
								getShell(),
								UIText.SimpleConfigureFetchDialog_NotRefSpecDialogTitle,
								UIText.SimpleConfigureFetchDialog_NotRefSpecDialogMessage);
			}
		} finally {
			clipboard.dispose();
		}
	}

	/**
	 * Add a warning about this remote being used by other branches
	 *
	 * @param parent
	 */
	private void addDefaultOriginWarningIfNeeded(Composite parent) {
		if (!showBranchInfo)
			return;
		List<String> otherBranches = new ArrayList<String>();
		String currentBranch;
		try {
			currentBranch = repository.getBranch();
		} catch (IOException e) {
			// just don't show this warning
			return;
		}
		String currentRemote = config.getName();
		Config repositoryConfig = repository.getConfig();
		Set<String> branches = repositoryConfig
				.getSubsections(ConfigConstants.CONFIG_BRANCH_SECTION);
		for (String branch : branches) {
			if (branch.equals(currentBranch))
				continue;
			String remote = repositoryConfig.getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, branch,
					ConfigConstants.CONFIG_KEY_REMOTE);
			if ((remote == null && currentRemote
					.equals(Constants.DEFAULT_REMOTE_NAME))
					|| (remote != null && remote.equals(currentRemote)))
				otherBranches.add(branch);
		}
		if (otherBranches.isEmpty())
			return;

		Composite warningAboutOrigin = new Composite(parent, SWT.NONE);
		warningAboutOrigin.setLayout(new GridLayout(2, false));
		Label warningLabel = new Label(warningAboutOrigin, SWT.NONE);
		warningLabel.setImage(PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJS_WARN_TSK));
		Text warningText = new Text(warningAboutOrigin, SWT.READ_ONLY);
		warningText.setText(NLS.bind(
				UIText.SimpleConfigureFetchDialog_ReusedRemoteWarning, config
						.getName(), Integer.valueOf(otherBranches.size())));
		warningText.setToolTipText(otherBranches.toString());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(warningLabel);
	}
}
