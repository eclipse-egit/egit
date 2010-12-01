/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.core.securestorage.UserPasswordCredentials;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Configure a fetch URI or a list of push URIs
 */
public class ConfigureUriPage extends WizardPage {
	private final boolean myFetchMode;

	Text uriText;

	TableViewer tv;

	private URIish myUri;

	private final List<URIish> myUris = new ArrayList<URIish>();

	private final RemoteConfig myConfig;

	private UserPasswordCredentials credentials;

	/**
	 * @param fetchMode
	 * @param remoteConfig
	 *
	 */
	public ConfigureUriPage(boolean fetchMode, RemoteConfig remoteConfig) {
		super(ConfigureUriPage.class.getName());

		myFetchMode = fetchMode;
		myConfig = remoteConfig;

		if (fetchMode) {
			setTitle(UIText.ConfigureUriPage_ConfigureFetch_pagetitle);
			setMessage(UIText.ConfigureUriPage_FetchPageMessage);
		} else {
			setTitle(UIText.ConfigureUriPage_ConfigurePush_pagetitle);
			setMessage(UIText.ConfigureUriPage_PushPageMessage);
		}
	}

	/**
	 * Sets the URI (only useful for push use case)
	 *
	 * @param uri
	 */
	public void setURI(URIish uri) {
		myUri = uri;
		uriText.setText(uri.toPrivateString());
		checkPage();
	}

	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		if (myFetchMode) {
			main.setLayout(new GridLayout(3, false));
			// we only use the first URI
			Label uriLabel = new Label(main, SWT.NONE);
			uriLabel.setText(UIText.ConfigureUriPage_FetchUri_label);
			uriLabel.setToolTipText(UIText.ConfigureUriPage_UriTooltip);
			uriText = new Text(main, SWT.BORDER);
			// manual entry is dangerous, as the validate may wait forever
			uriText.setEnabled(false);

			Button change = new Button(main, SWT.PUSH);
			change.setText(UIText.ConfigureUriPage_Change_button);
			change.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					SelectUriWizard slwz = new SelectUriWizard(false, uriText
							.getText());
					WizardDialog dlg = new WizardDialog(getShell(), slwz);
					if (dlg.open() == Window.OK) {
						URIish uri = slwz.getUri();
						credentials = slwz.getCredentials();
						uriText.setText(uri.toPrivateString());
						checkPage();
					}
				}

			});

			if (myConfig != null && !myConfig.getURIs().isEmpty()) {
				uriText.setText(myConfig.getURIs().get(0).toPrivateString());
				checkPage();
			} else {
				setPageComplete(false);
			}

			GridDataFactory.fillDefaults().grab(true, false).applyTo(uriText);

		} else {
			main.setLayout(new GridLayout(2, false));

			Label uriLabel = new Label(main, SWT.NONE);
			uriLabel.setText(UIText.ConfigureUriPage_FetchUri_label);
			uriLabel.setToolTipText(UIText.ConfigureUriPage_UriTooltip);
			uriText = new Text(main, SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(uriText);
			// push mode, display only
			uriText.setEnabled(false);

			Label pushLabel = new Label(main, SWT.NONE);
			pushLabel.setText(UIText.ConfigureUriPage_PushUriLabel);
			pushLabel.setToolTipText(UIText.ConfigureUriPage_PushUriTooltip);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(pushLabel);

			tv = new TableViewer(main);

			GridDataFactory.fillDefaults().span(2, 1).grab(true, true).applyTo(
					tv.getTable());

			tv.setLabelProvider(new LabelProvider());
			tv.setContentProvider(ArrayContentProvider.getInstance());

			Composite buttonBar = new Composite(main, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(buttonBar);
			buttonBar.setLayout(new RowLayout());
			Button add = new Button(buttonBar, SWT.PUSH);
			add.setText(UIText.ConfigureUriPage_Add_button);

			add.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					SelectUriWizard selectWizard = new SelectUriWizard(false);
					WizardDialog dlg = new WizardDialog(getShell(),
							selectWizard);
					if (dlg.open() == Window.OK) {
						URIish uri = selectWizard.getUri();
						if (uri.equals(myUri) || myUris.contains(uri)) {
							String message = NLS
									.bind(
											UIText.ConfigureUriPage_DuplicateUriMessage,
											uri.toPrivateString());
							MessageDialog.openInformation(getShell(),
									UIText.ConfigureUriPage_DuplicateUriTitle,
									message);
							return;
						}
						credentials = selectWizard.getCredentials();
						myUris.add(uri);
						tv.setInput(myUris);
						checkPage();
					}
				}

			});

			final Button remove = new Button(buttonBar, SWT.PUSH);
			remove.setText(UIText.ConfigureUriPage_Remove_button);
			remove.setEnabled(false);

			remove.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					for (Object o : ((IStructuredSelection) tv.getSelection())
							.toArray())
						myUris.remove(o);
					tv.setInput(myUris);
					checkPage();
				}

			});

			tv.addSelectionChangedListener(new ISelectionChangedListener() {

				public void selectionChanged(SelectionChangedEvent event) {
					remove.setEnabled(!tv.getSelection().isEmpty());

				}
			});

			// to enable keyboard-only operation, let's set the selection upon
			// traverse
			tv.getControl().addTraverseListener(new TraverseListener() {
				public void keyTraversed(TraverseEvent e) {
					if (tv.getTable().getSelection().length == 0) {
						if (tv.getTable().getItemCount() > 0) {
							tv.setSelection(new StructuredSelection(tv
									.getTable().getItem(0)));
						}
					}
				}
			});

			if (myConfig != null) {
				if (!myConfig.getURIs().isEmpty()) {
					myUri = myConfig.getURIs().get(0);
					uriText.setText(myUri.toPrivateString());
				}
				for (URIish uri : myConfig.getPushURIs())
					myUris.add(uri);
				tv.setInput(myUris);
				checkPage();
			} else {
				setPageComplete(false);
			}
		}

		Dialog.applyDialogFont(main);
		setControl(main);
	}

	private void checkPage() {
		try {
			setErrorMessage(null);

			if (myFetchMode) {
				if (uriText.getText().equals("")) { //$NON-NLS-1$
					setErrorMessage(UIText.ConfigureUriPage_MissingUri_message);
					return;
				}
				try {
					myUri = new URIish(uriText.getText());
				} catch (URISyntaxException e) {
					setErrorMessage(UIText.ConfigureUriPage_ParsingProblem_message);
					return;
				}

			} else {
				if (myUri == null && myUris.isEmpty()) {
					setErrorMessage(UIText.ConfigureUriPage_MissingUris_message);
					return;
				}
			}

		} finally {
			setPageComplete(getErrorMessage() == null);
		}
	}

	private static final class LabelProvider extends BaseLabelProvider
			implements ITableLabelProvider {

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			return ((URIish) element).toPrivateString();
		}

	}

	/**
	 * @return the (fetch) URI
	 */
	public URIish getUri() {
		if (myFetchMode) {
			return myUri;
		}
		throw new IllegalStateException();
	}

	/**
	 * @return the (push) URIs
	 */
	public List<URIish> getUris() {
		if (myFetchMode) {
			throw new IllegalStateException();
		}
		return myUris;
	}

	/**
	 * @return all URIs
	 */
	public List<URIish> getAllUris() {
		if (myFetchMode) {
			throw new IllegalStateException();
		}
		List<URIish> uris = new ArrayList<URIish>();
		uris.addAll(myUris);
		if (uris.isEmpty()) {
			uris.add(myUri);
		}
		return uris;
	}

	/**
	 * @return credentials
	 */
	public UserPasswordCredentials getCredentials() {
		return credentials;
	}
}
