/*******************************************************************************
 * Copyright (c) 2012 Kamil Sobon <kam.sobon@gmail.com>.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 *    Kamil Sobon - initial implementation
 *******************************************************************************/

package org.eclipse.egit.ui.internal.preferences;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.IPersonProvider;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

/**
 * Provides list of person from preference store.
 */
public final class PreferenceStorePersonProvider implements IPersonProvider {

	/** Root element of DOM structure where users are stored */
	public static final String ROOT_ELEMENT = "users"; //$NON-NLS-1$

	/** Name of the element where user's details are stored */
	public static final String USER_ELEMENT = "user"; //$NON-NLS-1$

	/** Name of the element where user's full name is stored */
	public static final String USER_NAME_ELEMENT = "name"; //$NON-NLS-1$

	/** Name of the element where user's login is stored */
	public static final String USER_LOGIN_ELEMENT = "login"; //$NON-NLS-1$

	private static final PreferenceStorePersonProvider INSTANCE = new PreferenceStorePersonProvider();

	/**
	 * Returns shared instance of {@link PreferenceStorePersonProvider}.
	 *
	 * @return shared instance of {@link PreferenceStorePersonProvider}
	 */
	public static PreferenceStorePersonProvider getInstance() {
		return INSTANCE;
	}

	private PreferenceStorePersonProvider() {
	}

	public Collection<Person> getPeople() {
		IPreferenceStore preferenceStore = Activator.getDefault()
				.getPreferenceStore();

		List<Person> result = new ArrayList<Person>();

		// Obtain stored users (users are stored in XML format)
		String usersRaw = preferenceStore
				.getString(GerritUsersPreferencePage.USERS_PREFERENCE_KEY);

		// Process users if there is some content
		if (usersRaw != null && usersRaw.length() > 0) {
			try {
				XMLMemento memento = XMLMemento
						.createReadRoot(new StringReader(usersRaw));
				for (IMemento userMemento : memento.getChildren(USER_ELEMENT)) {
					result.add(new Person(userMemento
							.getString(USER_LOGIN_ELEMENT), userMemento
							.getString(USER_NAME_ELEMENT)));
				}
			} catch (WorkbenchException e) {
				Activator.handleError(
						UIText.PreferenceStorePersonProvider_ExceptionMessage,
						e, true);
			} catch (IllegalArgumentException e) {
				// Handle situation when 'login' field is null
				Activator.handleError(
						UIText.PreferenceStorePersonProvider_ExceptionMessage,
						e, false);
			}
		}

		return result;
	}
}
