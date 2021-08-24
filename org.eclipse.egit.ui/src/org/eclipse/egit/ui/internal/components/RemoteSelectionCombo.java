/*******************************************************************************
 * Copyright (C) 2007, 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2010, Matthias Sohn <matthias.sohn@sap.com>
 * Copyright (C) 2012, Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.push.AddRemoteWizard;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

/**
 * A reusable composite containing a combo for selecting a remote from a given
 * list of remote configs.
 */
public class RemoteSelectionCombo extends Composite {

	/**
	 * Interface for listening to selection changes.
	 */
	public static interface IRemoteSelectionListener {
		/**
		 * @param remoteConfig
		 *            the remote which has been selected
		 */
		void remoteSelected(RemoteConfig remoteConfig);
	}

	/**
	 * Type of remote selection
	 */
	public static enum SelectionType {
		/**
		 * Shows the fetch URI
		 */
		FETCH,
		/**
		 * Shows the push URI if available
		 */
		PUSH
	}

	private static final int REMOTE_CONFIG_TEXT_MAX_LENGTH = 80;

	private SelectionType selectionType;

	private final Combo remoteCombo;

	private List<IRemoteSelectionListener> selectionListeners = new ArrayList<>();

	private List<RemoteConfig> remoteConfigs;

	private Repository enableAddNewRemote;

	private RemoteConfig lastSelection;

	private RemoteConfig newlyCreatedConfig;

	/**
	 * Create the widget.
	 *
	 * @param parent
	 *            the parent composite
	 * @param style
	 * @param selectionType
	 *            type of remote selection (fetch or push)
	 */
	public RemoteSelectionCombo(Composite parent, int style,
			SelectionType selectionType) {
		super(parent, style);

		this.selectionType = selectionType;

		setLayout(new FillLayout());

		remoteCombo = new Combo(this, SWT.READ_ONLY | SWT.DROP_DOWN);
		remoteCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (remoteCombo.getSelectionIndex() < remoteConfigs.size()) {
					RemoteConfig remoteConfig = getSelectedRemote();
					remoteSelected(remoteConfig);
				} else {
					showNewRemoteDialog();
				}
			}
		});
	}

	/**
	 * Set the available items.
	 *
	 * @param remoteConfigs
	 * @param enableAddNewRemote
	 * @return the initially selected remote config, defaults to the origin
	 *         remote if there is one
	 */
	public RemoteConfig setItems(List<RemoteConfig> remoteConfigs,
			Repository enableAddNewRemote) {
		this.remoteConfigs = remoteConfigs;
		this.enableAddNewRemote = enableAddNewRemote;

		final String items[] = new String[remoteConfigs.size()
				+ (enableAddNewRemote != null ? 1 : 0)];
		int i = 0;
		for (final RemoteConfig rc : remoteConfigs) {
			items[i++] = getTextForRemoteConfig(rc);
		}
		if (enableAddNewRemote != null) {
			items[items.length
					- 1] = UIText.RemoteSelectionCombole_addNewRemote;
		}

		remoteCombo.setItems(items);
		RemoteConfig defaultRemoteConfig = getDefaultRemoteConfig();
		setSelectedRemote(defaultRemoteConfig);

		return defaultRemoteConfig;
	}

	/**
	 * Set the available items.
	 *
	 * @param remoteConfigs
	 * @return the initially selected remote config, defaults to the origin
	 *         remote if there is one
	 */
	public RemoteConfig setItems(List<RemoteConfig> remoteConfigs) {
		return setItems(remoteConfigs, null);
	}

	private void showNewRemoteDialog() {
		AddRemoteWizard wizard = new AddRemoteWizard(enableAddNewRemote);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		int result = dialog.open();
		if (result == Window.OK) {
			try {
				RemoteConfig newConfig = configureNewRemote(
						wizard.getRemoteName(), wizard.getUri());
				addAndSelectNewConfig(newConfig);
			} catch (IOException | URISyntaxException ex) {
				MessageDialog.openError(wizard.getShell(),
						UIText.RemoteSelectionCombo_couldNotCreateNewRemote_title,
						UIText.RemoteSelectionCombo_couldNotCreateNewRemote_message);
				Activator.logError(
						UIText.RemoteSelectionCombo_couldNotCreateNewRemote_title,
						ex);
			}
		} else {
			setSelectedRemote(lastSelection);
		}
	}

	private void addAndSelectNewConfig(RemoteConfig newConfig) {
		remoteConfigs = new ArrayList<>(remoteConfigs);
		remoteConfigs.add(newConfig);
		newlyCreatedConfig = newConfig;
		setItems(remoteConfigs);
	}

	private RemoteConfig configureNewRemote(String remoteName, URIish uri)
			throws URISyntaxException, IOException {
		StoredConfig config = enableAddNewRemote.getConfig();
		RemoteConfig remoteConfig = new RemoteConfig(config, remoteName);
		remoteConfig.addURI(uri);
		RefSpec defaultFetchSpec = new RefSpec().setForceUpdate(true)
				.setSourceDestination(Constants.R_HEADS + "*", //$NON-NLS-1$
						Constants.R_REMOTES + remoteName + "/*"); //$NON-NLS-1$
		remoteConfig.addFetchRefSpec(defaultFetchSpec);
		remoteConfig.update(config);
		config.save();
		return remoteConfig;
	}

	/**
	 * Adds a selection listener.
	 *
	 * @param selectionListener
	 */
	public void addRemoteSelectionListener(
			IRemoteSelectionListener selectionListener) {
		selectionListeners.add(selectionListener);
	}

	/**
	 * @return the currently selected remote config, or null
	 */
	public RemoteConfig getSelectedRemote() {
		final int idx = remoteCombo.getSelectionIndex();
		if (remoteConfigs != null && idx != -1) {
			return remoteConfigs.get(idx);
		}
		return null;
	}

	/**
	 * Set the selected remote
	 *
	 * @param remoteConfig
	 *            config to set, must be one of those passed to
	 *            {@link #setItems(List)}
	 */
	public void setSelectedRemote(RemoteConfig remoteConfig) {
		int index = remoteConfigs.indexOf(remoteConfig);
		if (index != -1) {
			remoteCombo.select(index);
			lastSelection = remoteConfig;
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		remoteCombo.setEnabled(enabled);
	}

	private RemoteConfig getDefaultRemoteConfig() {
		if (remoteConfigs == null || remoteConfigs.isEmpty())
			return null;
		if (newlyCreatedConfig != null) {
			return newlyCreatedConfig;
		}
		for (final RemoteConfig rc : remoteConfigs)
			if (Constants.DEFAULT_REMOTE_NAME.equals(rc.getName()))
				return rc;
		return remoteConfigs.get(0);
	}

	private String getTextForRemoteConfig(final RemoteConfig rc) {
		final StringBuilder sb = new StringBuilder(rc.getName());
		sb.append(": "); //$NON-NLS-1$
		boolean first = true;
		List<URIish> uris;
		if (selectionType == SelectionType.FETCH)
			uris = rc.getURIs();
		else {
			uris = rc.getPushURIs();
			// if no push URIs are defined, use fetch URIs instead
			if (uris.isEmpty())
				uris = rc.getURIs();
		}

		for (final URIish u : uris) {
			final String uString = u.toString();
			if (first)
				first = false;
			else {
				sb.append(", "); //$NON-NLS-1$
				if (sb.length() + uString.length() > REMOTE_CONFIG_TEXT_MAX_LENGTH) {
					sb.append("..."); //$NON-NLS-1$
					break;
				}
			}
			sb.append(uString);
		}
		return sb.toString();
	}

	private void remoteSelected(RemoteConfig remoteConfig) {
		this.lastSelection = remoteConfig;
		for (IRemoteSelectionListener listener : selectionListeners) {
			listener.remoteSelected(remoteConfig);
		}
	}

}
