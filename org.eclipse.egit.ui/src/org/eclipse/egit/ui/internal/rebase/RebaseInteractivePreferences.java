package org.eclipse.egit.ui.internal.rebase;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;

/**
 * @author Vadim
 *
 */
public class RebaseInteractivePreferences {
	/**
	 * @return 123
	 */
	public static boolean isOrderReversed() {
		return Activator.getDefault().getPreferenceStore()
				.getBoolean(UIPreferences.REBASE_INTERACTIVE_ORDER_INVERSE);
	}

	/**
	 * @param reversed
	 */
	public static void setOrderReversed(boolean reversed) {
		Activator
				.getDefault()
				.getPreferenceStore()
				.setValue(UIPreferences.REBASE_INTERACTIVE_ORDER_INVERSE,
						reversed);
	}
}
