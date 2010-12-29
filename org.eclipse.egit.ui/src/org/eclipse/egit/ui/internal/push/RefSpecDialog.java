/*******************************************************************************
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.ListRemoteOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Add or edit a RefSpec
 */
public class RefSpecDialog extends TitleAreaDialog {
	private final boolean pushMode;

	private final Repository repo;

	private final RemoteConfig config;

	private RefSpec spec;

	private Text sourceText;

	private Text destinationText;

	private Button forceButton;

	private Text specString;

	private boolean autoSuggestDestination;

	/**
	 * @param parentShell
	 * @param repository
	 * @param config
	 * @param push
	 */
	public RefSpecDialog(Shell parentShell, Repository repository,
			RemoteConfig config, boolean push) {
		super(parentShell);
		this.repo = repository;
		this.config = config;
		this.pushMode = push;
		this.autoSuggestDestination = !pushMode;
		setHelpAvailable(false);
	}

	/**
	 * @param parentShell
	 * @param repository
	 * @param config
	 * @param spec
	 *            the {@link RefSpec} to edit
	 * @param push
	 */
	public RefSpecDialog(Shell parentShell, Repository repository,
			RemoteConfig config, RefSpec spec, boolean push) {
		this(parentShell, repository, config, push);
		this.spec = spec;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(UIText.RefSpecDialog_WindowTitle);
	}

	@Override
	public void create() {
		super.create();
		if (pushMode) {
			setTitle(UIText.RefSpecDialog_PushTitle);
			setMessage(UIText.RefSpecDialog_PushMessage);
		} else {
			setTitle(UIText.RefSpecDialog_FetchTitle);
			setMessage(UIText.RefSpecDialog_FetchMessage);
		}
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);
		main.setLayout(new GridLayout(3, false));
		Label sourceLabel = new Label(main, SWT.NONE);
		sourceLabel.setText(UIText.RefSpecDialog_SourceLabel);
		sourceText = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true,
				false).applyTo(sourceText);
		if (spec != null && spec.getSource() != null)
			sourceText.setText(spec.getSource());

		Button browseSource = new Button(main, SWT.PUSH);
		browseSource.setText(UIText.RefSpecDialog_BrowseSourceButton);
		browseSource.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doBrowse(true);
			}
		});

		if (!pushMode) {
			final Button autoSuggest = new Button(main, SWT.CHECK);
			GridDataFactory.fillDefaults().span(3, 1).applyTo(autoSuggest);
			autoSuggest.setText(UIText.RefSpecDialog_AutoSuggestCheckbox);
			autoSuggest.setSelection(autoSuggestDestination);
			autoSuggest.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					autoSuggestDestination = autoSuggest.getSelection();
				}
			});
		}

		Label destinationLabel = new Label(main, SWT.NONE);
		destinationLabel.setText(UIText.RefSpecDialog_DestinationLabel);
		destinationText = new Text(main, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true,
				false).applyTo(destinationText);
		if (spec != null && spec.getDestination() != null)
			destinationText.setText(spec.getDestination());

		Button browseDestination = new Button(main, SWT.PUSH);
		browseDestination.setText(UIText.RefSpecDialog_BrowseDestinationButton);
		browseDestination.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doBrowse(false);
			}
		});

		forceButton = new Button(main, SWT.CHECK);
		forceButton.setText(UIText.RefSpecDialog_ForceUpdateCheckbox);
		GridDataFactory.fillDefaults().span(3, 1).applyTo(forceButton);
		if (spec != null)
			forceButton.setSelection(spec.isForceUpdate());

		Label stringLabel = new Label(main, SWT.NONE);
		stringLabel.setText(UIText.RefSpecDialog_SpecificationLabel);
		specString = new Text(main, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(
				specString);
		if (spec != null)
			specString.setText(spec.toString());

		sourceText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateSource();
			}
		});
		destinationText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateDestination();
			}
		});
		forceButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (getSpec().isForceUpdate() == forceButton.getSelection())
					return;
				setSpec(getSpec().setForceUpdate(forceButton.getSelection()));
			}

		});
		applyDialogFont(main);
		return main;
	}

	private void updateSource() {
		setErrorMessage(null);
		try {
			RefSpec current = getSpec();
			if (sourceText.getText().length() > 0) {
				if (sourceText.getText().equals(current.getSource()))
					return;
				updateSpec();
				if (!pushMode) {
					if (autoSuggestDestination) {
						String name = sourceText.getText();
						if (name.startsWith(Constants.R_HEADS))
							name = name.substring(Constants.R_HEADS.length());
						else if (name.startsWith(Constants.R_TAGS))
							name = name.substring(Constants.R_TAGS.length());
						destinationText.setText(Constants.R_REMOTES
								+ config.getName() + '/' + name);
					}
				}
			} else {
				if (current.getSource() != null
						&& current.getSource().length() > 0)
					updateSpec();
			}
		} catch (IllegalStateException ex) {
			setErrorMessage(ex.getMessage());
		}
	}

	private void updateDestination() {
		setErrorMessage(null);
		try {
			RefSpec current = getSpec();
			if (destinationText.getText().length() > 0) {
				if (destinationText.getText().equals(current.getDestination()))
					return;
				updateSpec();
			} else {
				if (current.getDestination() != null
						&& current.getDestination().length() > 0)
					updateSpec();
			}
		} catch (IllegalStateException ex) {
			setErrorMessage(ex.getMessage());
		}
	}

	private void updateSpec() {
		this.spec = new RefSpec();
		try {
			this.spec = this.spec.setSource(sourceText.getText());
			this.spec = this.spec.setDestination(destinationText.getText());
		} catch (IllegalStateException e) {
			setErrorMessage(e.getMessage());
		}
		this.specString.setText(this.spec.toString());
	}

	/**
	 * @return the spec
	 */
	public RefSpec getSpec() {
		if (this.spec == null)
			this.spec = new RefSpec();
		return this.spec;

	}

	private void setSpec(RefSpec spec) {
		this.spec = spec;
		String newSourceText = spec.getSource() != null ? spec.getSource() : ""; //$NON-NLS-1$
		String newDestinationText = spec.getDestination() != null ? spec
				.getDestination() : ""; //$NON-NLS-1$
		String newStringText = spec.toString();
		if (!sourceText.getText().equals(newSourceText)) {
			sourceText.setText(newSourceText);
		}
		if (!destinationText.getText().equals(newDestinationText)) {
			destinationText.setText(newDestinationText);
		}
		if (!specString.getText().equals(newStringText)) {
			specString.setText(newStringText);
		}
		forceButton.setSelection(spec.isForceUpdate());
	}

	private void doBrowse(boolean source) {
		try {
			Set<Ref> proposals = new HashSet<Ref>();
			boolean local = pushMode == source;
			if (!local) {
				URIish uriToCheck;
				if (pushMode)
					uriToCheck = config.getPushURIs().get(0);
				else
					uriToCheck = config.getURIs().get(0);
				final ListRemoteOperation lop = new ListRemoteOperation(repo,
						uriToCheck,
						Activator.getDefault().getPreferenceStore().getInt(
								UIPreferences.REMOTE_CONNECTION_TIMEOUT));
				try {
					new ProgressMonitorDialog(getShell()).run(false, true,
							new IRunnableWithProgress() {

								public void run(IProgressMonitor monitor)
										throws InvocationTargetException,
										InterruptedException {
									monitor
											.beginTask(
													UIText.RefSpecDialog_GettingRemoteRefsMonitorMessage,
													IProgressMonitor.UNKNOWN);
									lop.run(monitor);
									monitor.done();
								}
							});
					for (Ref ref : lop.getRemoteRefs()) {
						if (ref.getName().startsWith(Constants.R_HEADS))
							proposals.add(ref);
					}
				} catch (IllegalStateException e) {
					setErrorMessage(e.getMessage());
					return;
				} catch (InvocationTargetException e) {
					setErrorMessage(e.getMessage());
					return;
				} catch (InterruptedException e) {
					return;
				}
			} else {
				if (pushMode)
					for (Ref ref : repo.getRefDatabase().getRefs(
							RefDatabase.ALL).values()) {
						if (ref.getName().startsWith(Constants.R_REMOTES)) {
							continue;
						}
						proposals.add(ref);
					}
				else
					for (Ref ref : repo.getRefDatabase().getRefs(
							Constants.R_REMOTES).values()) {
						proposals.add(ref);
					}
			}

			BrowseRefDialog dlg = new BrowseRefDialog(getShell(), repo,
					proposals);
			if (dlg.open() == Window.OK) {
				if (source)
					sourceText.setText(dlg.getRef().getName());
				else
					destinationText.setText(dlg.getRef().getName());
			}
		} catch (Exception ioe) {
			setErrorMessage(ioe.getMessage());
		}
	}
}
