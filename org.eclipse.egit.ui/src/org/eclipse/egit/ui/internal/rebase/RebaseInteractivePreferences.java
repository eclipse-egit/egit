/*******************************************************************************
 * Copyright (c) 2014 Vadim Dmitriev and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Vadim Dmitriev - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Provides convenient methods to get/set interactive rebase preferences.
 */
public class RebaseInteractivePreferences {

	private static IPreferenceStore getPreferencesStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	/**
	 * @return if commit display order is set to reversed (historical order).
	 */
	public static boolean isOrderReversed() {
		return getPreferencesStore().getBoolean(
				UIPreferences.REBASE_INTERACTIVE_ORDER_REVERSE);
	}

	/**
	 * Sets commit display order.
	 *
	 * @param reversed
	 *            {@code false} - display commits in execution order,
	 *            {@code true} - display commits in historical order.
	 */
	public static void setOrderReversed(boolean reversed) {
		getPreferencesStore().setValue(
				UIPreferences.REBASE_INTERACTIVE_ORDER_REVERSE, reversed);
	}

	/**
	 * @return if the view should react on selection changes.
	 */
	public static boolean isReactOnSelection() {
		return getPreferencesStore().getBoolean(
				UIPreferences.REBASE_INTERACTIVE_SYNC_SELECTION);
	}

	/**
	 * Sets if the view should react on selection changes.
	 *
	 * @param react
	 *            {@code true} - should react, {@code false} - should ignore.
	 */
	public static void setReactOnSelection(boolean react) {
		getPreferencesStore().setValue(
				UIPreferences.REBASE_INTERACTIVE_SYNC_SELECTION, react);
	}
}
