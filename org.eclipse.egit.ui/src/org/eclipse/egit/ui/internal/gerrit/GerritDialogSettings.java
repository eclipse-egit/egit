/*******************************************************************************
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.gerrit;

import java.util.List;

import org.eclipse.egit.core.internal.gerrit.GerritUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.fetch.FetchGerritChangePage;
import org.eclipse.egit.ui.internal.push.PushToGerritPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

/**
 * Helper class for managing the dialog setting of the
 * {@link FetchGerritChangePage} and {@link PushToGerritPage}.
 */
public final class GerritDialogSettings {

	/**
	 * Name of the {@link IDialogSettings} section for the
	 * {@link FetchGerritChangePage}.
	 */
	public static final String FETCH_FROM_GERRIT_SECTION = FetchGerritChangePage.class
			.getSimpleName();

	/**
	 * Name of the {@link IDialogSettings} section for the
	 * {@link PushToGerritPage}.
	 */
	public static final String PUSH_TO_GERRIT_SECTION = PushToGerritPage.class
			.getSimpleName();

	/**
	 * Repository suffix for storing the last used URI in a section.
	 */
	public static final String LAST_URI_SUFFIX = ".lastUri"; //$NON-NLS-1$

	private GerritDialogSettings() {
		// Utility class shall not be instantiated.
	}

	/**
	 * Updates dialog settings as appropriate. Called within the UI thread.
	 *
	 * @param repository
	 *            the {@code config} belongs to
	 * @param config
	 *            that was updated
	 */
	public static void updateRemoteConfig(Repository repository,
			RemoteConfig config) {
		if (repository == null || config == null) {
			return;
		}
		if (GerritUtil.isGerritFetch(config)) {
			updateGerritFetch(repository, config);
		}
		if (GerritUtil.isGerritPush(config)) {
			updateGerritPush(repository, config);
		}
	}

	/**
	 * Gets the specified section from the activator's {@link IDialogSettings}.
	 * Creates the section if it doesn't exist.
	 *
	 * @param id
	 *            of the section to get
	 * @return the section
	 */
	public static @NonNull IDialogSettings getSection(String id) {
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(id);
		if (section == null) {
			section = settings.addNewSection(id);
			if (section == null) {
				throw new NullPointerException(
						"IDialogSettings section could not be created"); //$NON-NLS-1$
			}
		}
		return section;
	}

	private static void updateGerritFetch(@NonNull Repository repository,
			@NonNull RemoteConfig config) {
		IDialogSettings section = getSection(FETCH_FROM_GERRIT_SECTION);
		String configured = section.get(repository + LAST_URI_SUFFIX);
		if (configured == null || configured.isEmpty()) {
			List<URIish> fetchUris = config.getURIs();
			if (!fetchUris.isEmpty()) {
				section.put(repository + LAST_URI_SUFFIX,
						fetchUris.get(0).toPrivateString());
			}
		}
	}

	private static void updateGerritPush(@NonNull Repository repository,
			@NonNull RemoteConfig config) {
		IDialogSettings section = getSection(PUSH_TO_GERRIT_SECTION);
		String configured = section.get(repository + LAST_URI_SUFFIX);
		if (configured == null || configured.isEmpty()) {
			List<URIish> pushUris = config.getPushURIs();
			if (!pushUris.isEmpty()) {
				section.put(repository + LAST_URI_SUFFIX,
						pushUris.get(0).toPrivateString());
			}
		}
	}
}
