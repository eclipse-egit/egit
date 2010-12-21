package org.eclipse.egit.ui.internal.push;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.repository.SelectUriWizard;
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
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
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

	private RemoteConfig config;

	private Text commonUriText;

	private Button pushUris;

	private Button sameUri;

	private TableViewer uriViewer;

	private TableViewer specViewer;

	private Button changeCommonUri;

	/**
	 * @param repository
	 */
	protected PushUriPage(Repository repository) {
		super(PushUriPage.class.getName());
		this.repository = repository;
	}

	public void createControl(Composite parent) {
		final Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		Group uriGroup = new Group(main, SWT.SHADOW_ETCHED_IN);
		uriGroup.setLayout(new GridLayout(1, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(uriGroup);
		uriGroup.setText("URI"); //$NON-NLS-1$ TODO
		sameUri = new Button(uriGroup, SWT.RADIO);
		sameUri.setEnabled(false);
		sameUri.setText("Use same URI for fetch and push"); //$NON-NLS-1$

		final Composite sameUriDetails = new Composite(uriGroup, SWT.NONE);
		sameUriDetails.setLayout(new GridLayout(4, false));
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(sameUriDetails);
		Label commonUriLabel = new Label(sameUriDetails, SWT.NONE);
		commonUriLabel.setText("URI:"); //$NON-NLS-1$
		commonUriText = new Text(sameUriDetails, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(commonUriText);
		changeCommonUri = new Button(sameUriDetails, SWT.PUSH);
		changeCommonUri.setText("Change..."); //$NON-NLS-1$
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
		deleteCommonUri.setText("Delete"); //$NON-NLS-1$
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
		pushUris.setText("Use separate URIs for push"); //$NON-NLS-1$
		pushUris.setEnabled(false);

		final Composite pushUriDetails = new Composite(uriGroup, SWT.NONE);
		pushUriDetails.setLayout(new GridLayout(3, false));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(pushUriDetails);
		uriViewer = new TableViewer(pushUriDetails, SWT.BORDER | SWT.MULTI);
		GridDataFactory.fillDefaults().grab(true, true).span(3, 1).applyTo(
				uriViewer.getTable());
		uriViewer.setContentProvider(ArrayContentProvider.getInstance());
		Button addUri = new Button(pushUriDetails, SWT.PUSH);
		addUri.setText("Add..."); //$NON-NLS-1$
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
		changeUri.setText("Change..."); //$NON-NLS-1$
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
		deleteUri.setText("Delete"); //$NON-NLS-1$
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
		refSpecGroup.setText("RefSpec"); //$NON-NLS-1$
		refSpecGroup.setLayout(new GridLayout(3, false));
		specViewer = new TableViewer(refSpecGroup, SWT.BORDER | SWT.MULTI);
		specViewer.setContentProvider(ArrayContentProvider.getInstance());
		GridDataFactory.fillDefaults().span(3, 1).grab(true, true).applyTo(
				specViewer.getTable());

		Button addRefSpec = new Button(refSpecGroup, SWT.PUSH);
		addRefSpec.setText("Add..."); //$NON-NLS-1$ TODO
		addRefSpec.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				RefSpecDialog dlg = new RefSpecDialog(getShell());
				if (dlg.open() == Window.OK) {
					config.addPushRefSpec(dlg.getSpec());
				}
				updateControls();
			}
		});
		final Button changeRefSpec = new Button(refSpecGroup, SWT.PUSH);
		changeRefSpec.setText("Change..."); //$NON-NLS-1$ TODO
		changeRefSpec.setEnabled(false);
		changeRefSpec.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				RefSpec oldSpec = (RefSpec) ((IStructuredSelection) specViewer
						.getSelection()).getFirstElement();
				RefSpecDialog dlg = new RefSpecDialog(getShell(), oldSpec);
				if (dlg.open() == Window.OK) {
					config.removePushRefSpec(oldSpec);
					config.addPushRefSpec(dlg.getSpec());
				}
				updateControls();
			}
		});
		final Button deleteRefSpec = new Button(refSpecGroup, SWT.PUSH);
		deleteRefSpec.setText("Delete"); //$NON-NLS-1$ TODO
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

		specViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				changeRefSpec.setEnabled(((IStructuredSelection) specViewer
						.getSelection()).size() == 1);
				deleteRefSpec.setEnabled(!((IStructuredSelection) specViewer
						.getSelection()).isEmpty());
			}
		});

		Button dryRun = new Button(main, SWT.PUSH);
		dryRun.setText("Dry-Run"); //$NON-NLS-1$
		dryRun.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					getContainer().run(false, true,
							new IRunnableWithProgress() {
								public void run(IProgressMonitor monitor)
										throws InvocationTargetException,
										InterruptedException {
									PushConfiguredRemoteOperation op = new PushConfiguredRemoteOperation(
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

		setControl(main);
	}

	/**
	 * @param name
	 *            the name to use
	 */
	public void setRemoteName(String name) {
		setMessage("In order to use a Remote for push, either a URI or a set of push URIs must be configured"); //$NON-NLS-1$
		List<RemoteConfig> configs;
		try {
			configs = RemoteConfig.getAllRemoteConfigs(repository.getConfig());
			for (RemoteConfig conf : configs) {
				if (conf.getName().equals(name)) {
					config = conf;
					break;
				}
			}
		} catch (URISyntaxException e) {
			config = null;
		}
		if (config == null) {
			try {
				config = new RemoteConfig(repository.getConfig(), name);
			} catch (URISyntaxException e) {
				Activator.handleError(e.getMessage(), e, true);
				setErrorMessage(e.getMessage());
			}
		}
		if (config == null) {
			return;
		}
		setTitle(NLS.bind(
				"Configure Push URIs for Remote \"{0}\"", config.getName())); //$NON-NLS-1$ TODO
		updateControls();
	}

	private void updateControls() {
		List<URIish> pushUrisList = config.getPushURIs();
		uriViewer.setInput(pushUrisList);
		if (!config.getURIs().isEmpty()) {
			commonUriText.setText(config.getURIs().get(0).toPrivateString());
		} else {
			commonUriText.setText(""); //$NON-NLS-1$
		}

		if (pushUrisList.isEmpty()) {
			sameUri.setSelection(!config.getURIs().isEmpty());
			pushUris.setSelection(false);
		} else {
			sameUri.setSelection(false);
			pushUris.setSelection(true);
		}
		specViewer.setInput(config.getPushRefSpecs());
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
