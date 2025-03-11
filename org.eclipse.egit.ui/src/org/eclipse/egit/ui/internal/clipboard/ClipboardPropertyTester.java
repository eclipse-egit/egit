/*******************************************************************************
 * Copyright (C) 2024 OPCoach and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0`
 *
 * Contributors:
 *    Olivier Prouvost <olivier.prouvost@opcoach.com> - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clipboard;

import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.clone.GitUrlChecker;
import org.eclipse.egit.ui.internal.expressions.AbstractPropertyTester;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

/**
 * Property Tester used to test the clipboard content. Does it contain a GIT URL
 * and is outside a styled text area (cell editor) ? Used for the paste active
 * when condition.
 */
public class ClipboardPropertyTester extends AbstractPropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {

		boolean value = internalTest(property);
		if (GitTraceLocation.CLIPBOARD.isActive()) {
			String clipboardText = getClipboardTextContent();
			Control ctrl = getFocusedControl();
			GitTraceLocation.getTrace().trace(
					GitTraceLocation.CLIPBOARD.getLocation(),
					"prop " + property + " The clipboard text value is " //$NON-NLS-1$ //$NON-NLS-2$
							+ (clipboardText == null
									? " No text value in clipboard " //$NON-NLS-1$
									: "'" + clipboardText + "'") //$NON-NLS-1$ //$NON-NLS-2$
							+ " = " + value //$NON-NLS-1$
							+ ((ctrl == null) ? "" //$NON-NLS-1$
									: ", current Control class = " //$NON-NLS-1$
											+ ctrl.getClass().getName())
							+ ", expected = " + expectedValue); //$NON-NLS-1$
		}
		return computeResult(expectedValue, value);
	}

	private boolean internalTest(String property) {
		if (!"containsGitURL".equals(property)) { //$NON-NLS-1$
			return false;
		}
		if (!isClipboarMonitoringEnabled()) {
			return false;
		}
		String content = getClipboardTextContent();
		if (content != null) {

			String sanitized = GitUrlChecker.sanitizeAsGitUrl(content);

			if (GitUrlChecker.isValidGitUrl(sanitized)) {
				// The clipboard is valid for GIT paste only if the current
				// swt control is not a Text or a StyledText (ie : in a
				// CellEditor)
				Control c = getFocusedControl();
				return (c != null) && !(c instanceof StyledText)
						&& !(c instanceof Text);
			}
		}
		return false;
	}

	/**
	 * Tells whether "Monitor clipboard for repository url" is currently enabled
	 *
	 * @return {@code true} if monitoring is enabled, {@code false} otherwise.
	 */
	public static boolean isClipboarMonitoringEnabled() {
		return Platform.getPreferencesService().getBoolean(Activator.PLUGIN_ID,
				UIPreferences.ENABLE_CLIPBOARD_MONITORING, false, null);
	}

	/**
	 * Extract the text value in the clipboard.
	 *
	 * @return null if nothing in clipboard
	 */
	private @Nullable String getClipboardTextContent() {
		Clipboard clipboard = new Clipboard(null);
		Object content = clipboard.getContents(TextTransfer.getInstance());
		clipboard.dispose();
		return content == null ? null : content.toString();
	}

	/**
	 * Get the current focused control
	 *
	 * @return the current focused control or null
	 */
	private Control getFocusedControl() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}

		return (display != null) ? display.getFocusControl() : null;
	}

}
