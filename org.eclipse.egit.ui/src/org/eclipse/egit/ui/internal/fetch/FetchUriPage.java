/*******************************************************************************
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.FetchOperationResult;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.push.RefSpecDialog;
import org.eclipse.egit.ui.internal.push.RefSpecWizard;
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
 * Displays the push URIs and {@link RefSpec}s
 */
public class FetchUriPage extends WizardPage {
	private final Repository repository;

	private final RemoteConfig config;

	private Text commonUriText;

	private TableViewer specViewer;

	private Button changeCommonUri;

	private Button addRefSpec;

	private Button addRefSpecAdvanced;

	private Button dryRun;

	private Button fetchUponFinish;

	/**
	 * @param repository
	 * @param config
	 */
	protected FetchUriPage(Repository repository, RemoteConfig config) {
		super(FetchUriPage.class.getName());
		this.repository = repository;
		this.config = config;
		setTitle(NLS.bind(UIText.FetchUriPage_ConfigureFetchURIsTitle, config
				.getName()));
		setMessage(UIText.FetchUriPage_ConfigureFetchURIsMessage);
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
		repositoryLabel.setText(UIText.FetchUriPage_RepositoryLabel);
		Text repositoryText = new Text(repositoryGroup, SWT.BORDER
				| SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(repositoryText);
		repositoryText.setText(Activator.getDefault().getRepositoryUtil()
				.getRepositoryName(repository));

		Label branchLabel = new Label(repositoryGroup, SWT.NONE);
		branchLabel.setText(UIText.FetchUriPage_BranchLabel);
		String branch;
		try {
			branch = repository.getBranch();
		} catch (IOException e2) {
			branch = null;
		}
		if (branch == null || ObjectId.isId(branch)) {
			branch = UIText.FetchUriPage_DetachedHeadMessage;
		}
		Text branchText = new Text(repositoryGroup, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(branchText);
		branchText.setText(branch);

		Label remoteLabel = new Label(repositoryGroup, SWT.NONE);
		remoteLabel.setText(UIText.FetchUriPage_RemoteLabel);
		Text remoteText = new Text(repositoryGroup, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(remoteText);
		remoteText.setText(config.getName());

		Group uriGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		uriGroup.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(uriGroup);
		uriGroup.setText(UIText.FetchUriPage_URIGroupHeader);

		final Composite sameUriDetails = new Composite(uriGroup, SWT.NONE);
		sameUriDetails.setLayout(new GridLayout(4, false));
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(sameUriDetails);
		Label commonUriLabel = new Label(sameUriDetails, SWT.NONE);
		commonUriLabel.setText(UIText.FetchUriPage_UriLabel);
		commonUriText = new Text(sameUriDetails, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(commonUriText);
		changeCommonUri = new Button(sameUriDetails, SWT.PUSH);
		changeCommonUri.setText(UIText.FetchUriPage_ChangeUriButton);
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
		deleteCommonUri.setText(UIText.FetchUriPage_DeleteUriButton);
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

		Group refSpecGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(refSpecGroup);
		refSpecGroup.setText(UIText.FetchUriPage_RefSpecGroupHeader);
		refSpecGroup.setLayout(new GridLayout(5, false));

		Label refSpecLabel = new Label(refSpecGroup, SWT.NONE);
		refSpecLabel.setText(UIText.FetchUriPage_RefSpecLabel);
		GridDataFactory.fillDefaults().span(5, 1).applyTo(refSpecLabel);

		specViewer = new TableViewer(refSpecGroup, SWT.BORDER | SWT.MULTI);
		specViewer.setContentProvider(ArrayContentProvider.getInstance());
		GridDataFactory.fillDefaults().span(5, 1).grab(true, true).applyTo(
				specViewer.getTable());

		addRefSpec = new Button(refSpecGroup, SWT.PUSH);
		addRefSpec.setText(UIText.FetchUriPage_AddSpecButton);
		addRefSpec.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				RefSpecDialog dlg = new RefSpecDialog(getShell(), repository,
						config, false);
				if (dlg.open() == Window.OK) {
					config.addFetchRefSpec(dlg.getSpec());
				}
				updateControls();
			}
		});

		final Button changeRefSpec = new Button(refSpecGroup, SWT.PUSH);
		changeRefSpec.setText(UIText.FetchUriPage_ChangeSpecButton);
		changeRefSpec.setEnabled(false);
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
		final Button deleteRefSpec = new Button(refSpecGroup, SWT.PUSH);
		deleteRefSpec.setText(UIText.FetchUriPage_DeleteSpecButton);
		deleteRefSpec.setEnabled(false);
		deleteRefSpec.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Object spec : ((IStructuredSelection) specViewer
						.getSelection()).toArray()) {
					config.removeFetchRefSpec((RefSpec) spec);

				}
				updateControls();
			}
		});

		final Button copySpec = new Button(refSpecGroup, SWT.PUSH);
		copySpec.setText(UIText.FetchUriPage_CopySpecButton);
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
		pasteSpec.setText(UIText.FetchUriPage_PasteSpecButton);
		pasteSpec.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Clipboard clipboard = new Clipboard(getShell().getDisplay());
				try {
					String content = (String) clipboard
							.getContents(TextTransfer.getInstance());
					if (content == null) {
						MessageDialog.openConfirm(getShell(),
								UIText.FetchUriPage_NothingToPasteWindowTitle,
								UIText.FetchUriPage_EmptyClipboardMessage);
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
												UIText.FetchUriPage_InvalidRefWindowTitle,
												NLS
														.bind(
																UIText.FetchUriPage_InvalidRefMessage,
																spec.toString())))
							config.addPushRefSpec(spec);

						updateControls();
					} catch (IllegalArgumentException ex) {
						MessageDialog.openError(getShell(),
								UIText.FetchUriPage_NotARefSpecWindowTitle,
								UIText.FetchUriPage_NotARefSpecMessage);
					}
				} finally {
					clipboard.dispose();
				}
			}
		});

		addRefSpecAdvanced = new Button(refSpecGroup, SWT.PUSH);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL)
				.span(3, 1).applyTo(addRefSpecAdvanced);

		addRefSpecAdvanced.setText(UIText.FetchUriPage_EditAdvancedButton);
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

		dryRun = new Button(main, SWT.PUSH);
		dryRun.setText(UIText.FetchUriPage_DryRunButton);
		dryRun.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					getContainer().run(false, true,
							new IRunnableWithProgress() {
								public void run(IProgressMonitor monitor)
										throws InvocationTargetException,
										InterruptedException {
									FetchConfiguredRemoteAction fetchOp = new FetchConfiguredRemoteAction(
											repository,
											config,
											Activator
													.getDefault()
													.getPreferenceStore()
													.getInt(
															UIPreferences.REMOTE_CONNECTION_TIMEOUT));
									fetchOp.setDryRun(true);
									try {
										fetchOp.execute(monitor);
									} catch (CoreException ce) {
										throw new InvocationTargetException(ce);
									}

									FetchOperationResult result = fetchOp
											.getOperationResult();
									FetchResultDialog dlg = new FetchResultDialog(
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

		fetchUponFinish = new Button(main, SWT.CHECK);
		fetchUponFinish.setText(UIText.FetchUriPage_FetchAfterFinishCheckbox);
		fetchUponFinish.setSelection(true);
		updateControls();

		setControl(main);
	}

	/**
	 * @return whether finish should also push
	 */
	public boolean shouldFetchUponFinish() {
		return fetchUponFinish.getSelection();
	}

	private void updateControls() {
		boolean anyUri = false;
		if (!config.getURIs().isEmpty()) {
			commonUriText.setText(config.getURIs().get(0).toPrivateString());
			anyUri = true;
		} else {
			commonUriText.setText(""); //$NON-NLS-1$
		}
		specViewer.setInput(config.getFetchRefSpecs());

		addRefSpec.setEnabled(anyUri);
		addRefSpecAdvanced.setEnabled(anyUri);
		dryRun.setEnabled(anyUri);

		setPageComplete(!config.getURIs().isEmpty());
	}

	/**
	 * @return the config
	 */
	public RemoteConfig getRemoteConfig() {
		return config;
	}
}
