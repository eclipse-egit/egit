/*******************************************************************************
 * Copyright (c) 2014 Vadim Dmitriev and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
}
