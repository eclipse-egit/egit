/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
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
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.repository.SelectUriWizard;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Displays the push URIs
 */
public class PushUriPage extends WizardPage {

	private final Repository repository;

	private final RemoteConfig config;

	private Text commonUriText;

	private Button pushUris;

	private Button sameUri;

	private TableViewer uriViewer;

	private TableViewer specViewer;

	private Button changeCommonUri;

	private Button addRefSpec;

	private Button addRefSpecAdvanced;

	private Button dryRun;

	private Button pushUponFinish;

	/**
	 * @param repository
	 * @param config
	 */
	protected PushUriPage(Repository repository, RemoteConfig config) {
		super(PushUriPage.class.getName());
		this.repository = repository;
		this.config = config;
		setTitle(NLS.bind(UIText.PushUriPage_ConfigurePushTitle, config
				.getName()));
		setMessage(UIText.PushUriPage_ConfigurePushMessage);
	}

	public void createControl(Composite parent) {
		final Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		Composite repositoryGroup = new Composite(main, SWT.SHADOW_ETCHED_IN);
		repositoryGroup.setLayout(new GridLayout(2, false));
		GridDataFactory.fillDefaults().grab(true, true)
				.applyTo(repositoryGroup);
		Label repositoryLabel = new Label(repositoryGroup, SWT.NONE);
		repositoryLabel.setText(UIText.PushUriPage_RepsitoryLabel);
		Text repositoryText = new Text(repositoryGroup, SWT.BORDER
				| SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(repositoryText);
		repositoryText.setText(Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository));

		Label branchLabel = new Label(repositoryGroup, SWT.NONE);
		branchLabel.setText(UIText.PushUriPage_BranchLabel);
		String branch;
		try {
			branch = repository.getBranch();
		} catch (IOException e2) {
			branch = null;
		}
		if (branch == null || ObjectId.isId(branch)) {
			branch = UIText.PushUriPage_DetachedHeadMessage;
		}
		Text branchText = new Text(repositoryGroup, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(branchText);
		branchText.setText(branch);

		Label remoteLabel = new Label(repositoryGroup, SWT.NONE);
		remoteLabel.setText(UIText.PushUriPage_RemoteLabel);
		Text remoteText = new Text(repositoryGroup, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(remoteText);
		remoteText.setText(config.getName());

		Group uriGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		uriGroup.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(uriGroup);
		uriGroup.setText(UIText.PushUriPage_URIGroupHeader);
		sameUri = new Button(uriGroup, SWT.RADIO);
		sameUri.setEnabled(false);
		sameUri.setText(UIText.PushUriPage_CommonUriMessage);

		final Composite sameUriDetails = new Composite(uriGroup, SWT.NONE);
		sameUriDetails.setLayout(new GridLayout(4, false));
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(sameUriDetails);
		Label commonUriLabel = new Label(sameUriDetails, SWT.NONE);
		commonUriLabel.setText(UIText.PushUriPage_CommonUriLabel);
		commonUriText = new Text(sameUriDetails, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(commonUriText);
		changeCommonUri = new Button(sameUriDetails, SWT.PUSH);
		changeCommonUri.setText(UIText.PushUriPage_ChangeCommonUriButton);
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

		final Button deleteCommonUri = new Button(sameUriDetails, SWT.PUSH);
		deleteCommonUri.setText(UIText.PushUriPage_DeleteCommonUriButton);
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

		pushUris = new Button(uriGroup, SWT.RADIO);
		pushUris.setText(UIText.PushUriPage_PushUrisMessage);
		pushUris.setEnabled(false);

		final Composite pushUriDetails = new Composite(uriGroup, SWT.NONE);
		pushUriDetails.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(pushUriDetails);
		Label urisLabel = new Label(pushUriDetails, SWT.NONE);
		urisLabel.setText(UIText.PushUriPage_PushUrisLabel);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(urisLabel);
		uriViewer = new TableViewer(pushUriDetails, SWT.BORDER | SWT.MULTI);
		GridDataFactory.fillDefaults().grab(true, true).span(3, 1).applyTo(
				uriViewer.getTable());
		uriViewer.setContentProvider(ArrayContentProvider.getInstance());
		Button addUri = new Button(pushUriDetails, SWT.PUSH);
		addUri.setText(UIText.PushUriPage_AddPushUriButton);
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
		changeUri.setText(UIText.PushUriPage_ChangePushUriButton);
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
		deleteUri.setText(UIText.PushUriPage_DeletePushUriButton);
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

		Group refSpecGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(refSpecGroup);
		refSpecGroup.setText(UIText.PushUriPage_RefSpecGroupHeader);
		refSpecGroup.setLayout(new GridLayout(5, false));

		Label refSpecLabel = new Label(refSpecGroup, SWT.NONE);
		refSpecLabel.setText(UIText.PushUriPage_RefSpecLabel);
		GridDataFactory.fillDefaults().span(5, 1).applyTo(refSpecLabel);

		specViewer = new TableViewer(refSpecGroup, SWT.BORDER | SWT.MULTI);
		specViewer.setContentProvider(ArrayContentProvider.getInstance());
		GridDataFactory.fillDefaults().span(5, 1).grab(true, true).applyTo(
				specViewer.getTable());

		addRefSpec = new Button(refSpecGroup, SWT.PUSH);
		addRefSpec.setText(UIText.PushUriPage_AddRefSpecButton);
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

		final Button changeRefSpec = new Button(refSpecGroup, SWT.PUSH);
		changeRefSpec.setText(UIText.PushUriPage_ChangeRefSpecButton);
		changeRefSpec.setEnabled(false);
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
		deleteRefSpec.setText(UIText.PushUriPage_DeleteRefSpecButton);
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
		copySpec.setText(UIText.PushUriPage_CopyRefSpecButton);
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
		pasteSpec.setText(UIText.PushUriPage_PasteRefSpecButton);
		pasteSpec.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Clipboard clipboard = new Clipboard(getShell().getDisplay());
				try {
					String content = (String) clipboard
							.getContents(TextTransfer.getInstance());
					if (content == null) {
						MessageDialog.openConfirm(getShell(),
								UIText.PushUriPage_NothingToPasteWindowTitle,
								UIText.PushUriPage_EmptyClipboardMessage);
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
												UIText.PushUriPage_InavlidRefWindowTitle,
												NLS
														.bind(
																UIText.PushUriPage_InvalidRefMessage,
																spec.toString())))
							config.addPushRefSpec(spec);

						updateControls();
					} catch (IllegalArgumentException ex) {
						MessageDialog.openError(getShell(),
								UIText.PushUriPage_NotASpecWindowTitle,
								UIText.PushUriPage_NotASpecMessage);
					}
				} finally {
					clipboard.dispose();
				}
			}
		});

		addRefSpecAdvanced = new Button(refSpecGroup, SWT.PUSH);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL)
				.span(3, 1).applyTo(addRefSpecAdvanced);

		addRefSpecAdvanced.setText(UIText.PushUriPage_EditAdvancedButton);
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

		dryRun = new Button(main, SWT.PUSH);
		dryRun.setText(UIText.PushUriPage_DryRunButton);
		dryRun.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					getContainer().run(false, true,
							new IRunnableWithProgress() {
								public void run(IProgressMonitor monitor)
										throws InvocationTargetException,
										InterruptedException {
									PushConfiguredRemoteAction op = new PushConfiguredRemoteAction(
											repository,
											config,
											Activator
													.getDefault()
													.getPreferenceStore()
													.getInt(
															UIPreferences.REMOTE_CONNECTION_TIMEOUT));
									op.setDryRun(true);
									op.execute(monitor);
									PushOperationResult result = op
											.getOperationResult();
									PushResultDialog dlg = new PushResultDialog(
											getShell(), repository, result,
											config.getName());
									dlg.showConfigureButton(false);
									dlg.open();

								}
							});
				} catch (InvocationTargetException e1) {
					Activator.handleError(e1.getMessage(), e1, true);
				} catch (InterruptedException e1) {
					// ignore here
				}
			}
		});

		pushUponFinish = new Button(main, SWT.CHECK);
		pushUponFinish.setText(UIText.PushUriPage_PushAfterFinishCheckbox);
		pushUponFinish.setSelection(true);
		updateControls();

		setControl(main);
	}

	/**
	 * @return whether finish should also push
	 */
	public boolean shouldPushUponFinish() {
		return pushUponFinish.getSelection();
	}

	private void updateControls() {
		boolean anyUri = false;
		List<URIish> pushUrisList = config.getPushURIs();
		uriViewer.setInput(pushUrisList);
		if (!config.getURIs().isEmpty()) {
			commonUriText.setText(config.getURIs().get(0).toPrivateString());
			anyUri = true;
		} else {
			commonUriText.setText(""); //$NON-NLS-1$
		}

		if (pushUrisList.isEmpty()) {
			sameUri.setSelection(!config.getURIs().isEmpty());
			pushUris.setSelection(false);
		} else {
			sameUri.setSelection(false);
			pushUris.setSelection(true);
			anyUri = true;
		}
		specViewer.setInput(config.getPushRefSpecs());

		addRefSpec.setEnabled(anyUri);
		addRefSpecAdvanced.setEnabled(anyUri);
		dryRun.setEnabled(anyUri);

		setPageComplete(!config.getPushURIs().isEmpty()
				|| !config.getURIs().isEmpty());
	}

	/**
	 * @return the config
	 */
	public RemoteConfig getRemoteConfig() {
		return config;
	}
}
