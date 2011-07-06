/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.SelectUriWizard;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
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
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

/**
 * A simplified wizard for configuring push
 */
public class SimpleConfigurePushDialog extends TitleAreaDialog {
	private static final int DRY_RUN = 98;

	private static final int SAVE_ONLY = 97;

	private static final int REVERT = 96;

	private static final String ADVANCED_MODE_PREFERENCE = SimpleConfigurePushDialog.class
			.getName()
			+ "_ADVANCED_MODE"; //$NON-NLS-1$

	private final Repository repository;

	private RemoteConfig config;

	private final boolean showBranchInfo;

	private Text commonUriText;

	private TableViewer uriViewer;

	private TableViewer specViewer;

	private Button changeCommonUri;

	private Button deleteCommonUri;

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
		return new SimpleConfigurePushDialog(shell, repository, configToUse,
				true);
	}

	/**
	 * @param shell
	 * @param repository
	 * @param remoteName
	 *            the remote to use
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
		return new SimpleConfigurePushDialog(shell, repository, configToUse,
				false);
	}

	/**
	 * @param repository
	 * @return the configured remote for the current branch if any, or null
	 */
	public static RemoteConfig getConfiguredRemote(Repository repository) {
		String branch;
		try {
			branch = repository.getBranch();
		} catch (IOException e) {
			Activator.handleError(e.getMessage(), e, true);
			return null;
		}

		String remoteName;
		if (ObjectId.isId(branch)) {
			remoteName = Constants.DEFAULT_REMOTE_NAME;
		} else {
			remoteName = repository.getConfig().getString(
					ConfigConstants.CONFIG_BRANCH_SECTION, branch,
					ConfigConstants.CONFIG_REMOTE_SECTION);
		}

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
	 *
	 * @param shell
	 * @param repository
	 * @param config
	 * @param showBranchInfo
	 *            should be true if this is used for upstream configuration; if
	 *            false, branch information will be hidden in the dialog
	 */
	private SimpleConfigurePushDialog(Shell shell, Repository repository,
			RemoteConfig config, boolean showBranchInfo) {
		super(shell);
		setHelpAvailable(false);
		setShellStyle(getShellStyle() | SWT.SHELL_TRIM);
		this.repository = repository;
		this.config = config;
		this.showBranchInfo = showBranchInfo;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		boolean advancedMode = Activator.getDefault().getPreferenceStore()
				.getBoolean(ADVANCED_MODE_PREFERENCE);
		final Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		Composite repositoryGroup = new Composite(main, SWT.SHADOW_ETCHED_IN);
		repositoryGroup.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(repositoryGroup);
		Label repositoryLabel = new Label(repositoryGroup, SWT.NONE);
		repositoryLabel
				.setText(UIText.SimpleConfigurePushDialog_RepositoryLabel);
		Text repositoryText = new Text(repositoryGroup, SWT.BORDER
				| SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(repositoryText);
		repositoryText.setText(Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository));

		if (showBranchInfo) {
			Label branchLabel = new Label(repositoryGroup, SWT.NONE);
			branchLabel.setText(UIText.SimpleConfigurePushDialog_BranchLabel);
			String branch;
			try {
				branch = repository.getBranch();
			} catch (IOException e2) {
				branch = null;
			}
			if (branch == null || ObjectId.isId(branch)) {
				branch = UIText.SimpleConfigurePushDialog_DetachedHeadMessage;
			}
			Text branchText = new Text(repositoryGroup, SWT.BORDER
					| SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false)
					.applyTo(branchText);
			branchText.setText(branch);
		}

		Group remoteGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		remoteGroup.setLayout(new GridLayout());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(remoteGroup);
		remoteGroup.setText(NLS.bind(
				UIText.SimpleConfigurePushDialog_RemoteGroupTitle, config
						.getName()));

		addDefaultOriginWarningIfNeeded(remoteGroup);

		Group uriGroup = new Group(remoteGroup, SWT.SHADOW_ETCHED_IN);
		uriGroup.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(uriGroup);
		uriGroup.setText(UIText.SimpleConfigurePushDialog_UriGroup);

		final Composite sameUriDetails = new Composite(uriGroup, SWT.NONE);
		sameUriDetails.setLayout(new GridLayout(4, false));
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(sameUriDetails);
		Label commonUriLabel = new Label(sameUriDetails, SWT.NONE);
		commonUriLabel.setText(UIText.SimpleConfigurePushDialog_URILabel);
		commonUriText = new Text(sameUriDetails, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(commonUriText);
		changeCommonUri = new Button(sameUriDetails, SWT.PUSH);
		changeCommonUri
				.setText(UIText.SimpleConfigurePushDialog_ChangeUriButton);
		changeCommonUri.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SelectUriWizard wiz;
				if (commonUriText.getText().length() > 0)
					wiz = new SelectUriWizard(false, commonUriText.getText());
				else
					wiz = new SelectUriWizard(false);
				if (new WizardDialog(getShell(), wiz).open() == Window.OK) {
					if (commonUriText.getText().length() > 0)
						try {
							config
									.removeURI(new URIish(commonUriText
											.getText()));
						} catch (URISyntaxException ex) {
							Activator.handleError(ex.getMessage(), ex, true);
						}
					config.addURI(wiz.getUri());
					updateControls();
				}
			}
		});

		deleteCommonUri = new Button(sameUriDetails, SWT.PUSH);
		deleteCommonUri
				.setText(UIText.SimpleConfigurePushDialog_DeleteUriButton);
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

		final Composite pushUriDetails = new Composite(uriGroup, SWT.NONE);
		pushUriDetails.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(pushUriDetails);
		Label urisLabel = new Label(pushUriDetails, SWT.NONE);
		urisLabel.setText(UIText.SimpleConfigurePushDialog_PushUrisLabel);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(urisLabel);
		uriViewer = new TableViewer(pushUriDetails, SWT.BORDER | SWT.MULTI);
		GridDataFactory.fillDefaults().grab(true, true).span(3, 1).minSize(
				SWT.DEFAULT, 30).applyTo(uriViewer.getTable());
		uriViewer.setContentProvider(ArrayContentProvider.getInstance());
		Button addUri = new Button(pushUriDetails, SWT.PUSH);
		addUri.setText(UIText.SimpleConfigurePushDialog_AddPushUriButton);
		addUri.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SelectUriWizard wiz = new SelectUriWizard(false);
				if (new WizardDialog(getShell(), wiz).open() == Window.OK) {
					config.addPushURI(wiz.getUri());
					updateControls();
				}
			}

		});
		final Button changeUri = new Button(pushUriDetails, SWT.PUSH);
		changeUri.setText(UIText.SimpleConfigurePushDialog_ChangePushUriButton);
		changeUri.setEnabled(false);
		changeUri.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				URIish uri = (URIish) ((IStructuredSelection) uriViewer
						.getSelection()).getFirstElement();
				SelectUriWizard wiz = new SelectUriWizard(false, uri
						.toPrivateString());
				if (new WizardDialog(getShell(), wiz).open() == Window.OK) {
					config.removePushURI(uri);
					config.addPushURI(wiz.getUri());
					updateControls();
				}
			}
		});
		final Button deleteUri = new Button(pushUriDetails, SWT.PUSH);
		deleteUri.setText(UIText.SimpleConfigurePushDialog_DeletePushUriButton);
		deleteUri.setEnabled(false);
		deleteUri.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				URIish uri = (URIish) ((IStructuredSelection) uriViewer
						.getSelection()).getFirstElement();
				config.removePushURI(uri);
				updateControls();
			}
		});

		uriViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				deleteUri.setEnabled(!uriViewer.getSelection().isEmpty());
				changeUri.setEnabled(((IStructuredSelection) uriViewer
						.getSelection()).size() == 1);
			}
		});

		final Group refSpecGroup = new Group(remoteGroup, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(refSpecGroup);
		refSpecGroup.setText(UIText.SimpleConfigurePushDialog_RefMappingGroup);
		refSpecGroup.setLayout(new GridLayout(5, false));

		ExpandableComposite advanced = new ExpandableComposite(refSpecGroup,
				ExpandableComposite.TREE_NODE
						| ExpandableComposite.CLIENT_INDENT);
		if (advancedMode)
			advanced.setExpanded(true);
		advanced.setText(UIText.SimpleConfigurePushDialog_AdvancedButton);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL)
				.span(5, 1).grab(true, false).applyTo(advanced);
		advanced.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				Activator.getDefault().getPreferenceStore().setValue(
						ADVANCED_MODE_PREFERENCE, e.getState());
				GridData data = (GridData) changeRefSpec.getLayoutData();
				data.exclude = !e.getState();
				changeRefSpec.setVisible(!data.exclude);
				refSpecGroup.layout(true);
			}
		});

		Label refSpecLabel = new Label(refSpecGroup, SWT.NONE);
		refSpecLabel.setText(UIText.SimpleConfigurePushDialog_RefSpecLabel);
		GridDataFactory.fillDefaults().span(5, 1).applyTo(refSpecLabel);

		specViewer = new TableViewer(refSpecGroup, SWT.BORDER | SWT.MULTI);
		specViewer.setContentProvider(ArrayContentProvider.getInstance());
		GridDataFactory.fillDefaults().span(5, 1).grab(true, true).minSize(
				SWT.DEFAULT, 30).applyTo(specViewer.getTable());
		specViewer.getTable().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.stateMask == SWT.MOD1 && e.keyCode == 'v') {
					doPaste();
				}
			}
		});

		addRefSpec = new Button(refSpecGroup, SWT.PUSH);
		addRefSpec.setText(UIText.SimpleConfigurePushDialog_AddRefSpecButton);
		addRefSpec.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				RefSpecDialog dlg = new RefSpecDialog(getShell(), repository,
						config, true);
				if (dlg.open() == Window.OK) {
					config.addPushRefSpec(dlg.getSpec());
				}
				updateControls();
			}
		});

		changeRefSpec = new Button(refSpecGroup, SWT.PUSH);
		changeRefSpec
				.setText(UIText.SimpleConfigurePushDialog_ChangeRefSpecButton);
		changeRefSpec.setEnabled(false);
		GridDataFactory.fillDefaults().exclude(!advancedMode).applyTo(
				changeRefSpec);
		changeRefSpec.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				RefSpec oldSpec = (RefSpec) ((IStructuredSelection) specViewer
						.getSelection()).getFirstElement();
				RefSpecDialog dlg = new RefSpecDialog(getShell(), repository,
						config, oldSpec, true);
				if (dlg.open() == Window.OK) {
					config.removePushRefSpec(oldSpec);
					config.addPushRefSpec(dlg.getSpec());
				}
				updateControls();
			}
		});
		final Button deleteRefSpec = new Button(refSpecGroup, SWT.PUSH);
		deleteRefSpec
				.setText(UIText.SimpleConfigurePushDialog_DeleteRefSpecButton);
		deleteRefSpec.setEnabled(false);
		deleteRefSpec.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Object spec : ((IStructuredSelection) specViewer
						.getSelection()).toArray()) {
					config.removePushRefSpec((RefSpec) spec);

				}
				updateControls();
			}
		});

		final Button copySpec = new Button(refSpecGroup, SWT.PUSH);
		copySpec.setText(UIText.SimpleConfigurePushDialog_CopyRefSpecButton);
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

		final Button pasteSpec = new Button(refSpecGroup, SWT.PUSH);
		pasteSpec.setText(UIText.SimpleConfigurePushDialog_PasteRefSpecButton);
		pasteSpec.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doPaste();
			}
		});

		addRefSpecAdvanced = new Button(advanced, SWT.PUSH);
		advanced.setClient(addRefSpecAdvanced);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL)
				.span(3, 1).applyTo(addRefSpecAdvanced);

		addRefSpecAdvanced
				.setText(UIText.SimpleConfigurePushDialog_EditAdvancedButton);
		addRefSpecAdvanced.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (new WizardDialog(getShell(), new RefSpecWizard(repository,
						config, true)).open() == Window.OK)
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
				UIText.SimpleConfigurePushDialog_SaveAndPushButton, true);
		createButton(parent, SAVE_ONLY,
				UIText.SimpleConfigurePushDialog_SaveButton, false);
		createButton(parent, DRY_RUN,
				UIText.SimpleConfigurePushDialog_DryRunButton, false);

		createButton(parent, REVERT,
				UIText.SimpleConfigurePushDialog_RevertButton, false);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	public void create() {
		super.create();
		String repoName = Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository);

		setTitle(NLS.bind(UIText.SimpleConfigurePushDialog_DialogTitle,
				repoName, config.getName()));
		setMessage(UIText.SimpleConfigurePushDialog_DialogMessage);

		updateControls();
	}

	private void updateControls() {
		boolean anyFetchUri = !config.getURIs().isEmpty();
		boolean anyPushUri = !config.getPushURIs().isEmpty();
		boolean anyUri = anyFetchUri || anyPushUri;
		setErrorMessage(null);
		if (!anyUri)
			setErrorMessage(UIText.SimpleConfigurePushDialog_MissingUriMessage);

		if (anyFetchUri) {
			commonUriText.setText(config.getURIs().get(0).toPrivateString());
		} else {
			commonUriText.setText(""); //$NON-NLS-1$
		}
		uriViewer.getTable().setEnabled(anyPushUri);
		if (anyPushUri)
			uriViewer.setInput(config.getPushURIs());
		else if (anyFetchUri)
			uriViewer.setInput(new String[] { NLS.bind(
					UIText.SimpleConfigurePushDialog_UseUriForPushUriMessage,
					commonUriText.getText()) });
		else
			uriViewer.setInput(null);

		if (config.getPushRefSpecs().isEmpty())
			specViewer.setInput(new String[] { NLS.bind(
					UIText.SimpleConfigurePushDialog_PushAllBranchesMessage,
					PushOperationUI.DEFAULT_PUSH_REF_SPEC) });
		else
			specViewer.setInput(config.getPushRefSpecs());

		specViewer.getTable().setEnabled(!config.getPushRefSpecs().isEmpty());

		addRefSpec.setEnabled(anyUri);
		addRefSpecAdvanced.setEnabled(anyUri);
		changeCommonUri.setEnabled(!anyPushUri);
		deleteCommonUri.setEnabled(!anyPushUri);
		commonUriText.setEnabled(!anyPushUri);

		getButton(OK).setEnabled(anyUri);
		getButton(DRY_RUN).setEnabled(anyUri);
		getButton(SAVE_ONLY).setEnabled(anyUri);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.SimpleConfigurePushDialog_WindowTitle);
	}

	@Override
	public void buttonPressed(int buttonId) {
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
								PushOperationUI op = new PushOperationUI(
										repository, config, timeout, true);
								try {
									PushOperationResult result = op
											.execute(monitor);
									PushResultDialog dlg = new PushResultDialog(
											getShell(), repository, result, op
													.getDestinationString());
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
			if (buttonId == OK) {
				try {
					new ProgressMonitorDialog(getShell()).run(false, true,
							new IRunnableWithProgress() {
								public void run(IProgressMonitor monitor)
										throws InvocationTargetException,
										InterruptedException {
									int timeout = Activator
											.getDefault()
											.getPreferenceStore()
											.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT);
									PushOperationUI op = new PushOperationUI(
											repository, config.getName(), timeout, false);
									op.start();
								}
							});
				} catch (InvocationTargetException e) {
					Activator.handleError(e.getMessage(), e, true);
				} catch (InterruptedException e) {
					Activator.handleError(e.getMessage(), e, true);
				}
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
			if (content == null) {
				MessageDialog
						.openConfirm(
								getShell(),
								UIText.SimpleConfigurePushDialog_EmptyClipboardDialogTitle,
								UIText.SimpleConfigurePushDialog_EmptyClipboardDialogMessage);
			}
			try {
				RefSpec spec = new RefSpec(content);
				Ref source;
				try {
					// TODO better checks for wild-cards and such
					source = repository.getRef(spec.getSource());
				} catch (IOException e1) {
					source = null;
				}
				if (source != null
						|| MessageDialog
								.openQuestion(
										getShell(),
										UIText.SimpleConfigurePushDialog_InvalidRefDialogTitle,
										NLS
												.bind(
														UIText.SimpleConfigurePushDialog_InvalidRefDialogMessage,
														spec.toString())))
					config.addPushRefSpec(spec);

				updateControls();
			} catch (IllegalArgumentException ex) {
				MessageDialog
						.openError(
								getShell(),
								UIText.SimpleConfigurePushDialog_NoRefSpecDialogTitle,
								UIText.SimpleConfigurePushDialog_NoRefSpecDialogMessage);
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
				UIText.SimpleConfigurePushDialog_ReusedOriginWarning, config
						.getName(), Integer.valueOf(otherBranches.size())));
		warningText.setToolTipText(otherBranches.toString());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(warningLabel);
	}
}
