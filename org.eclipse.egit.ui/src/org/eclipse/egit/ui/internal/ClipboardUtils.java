/*******************************************************************************
 * Copyright (C) 2018, Michael Keppler <michael.keppler@gmx.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;

/**
 * Utility for copying to the system clipboard.
 */
public class ClipboardUtils {
	private ClipboardUtils() {
		// utility class
	}

	/**
	 * Copy given SHA1 to the clipboard. Ask for retry on error.
	 *
	 * @param sha1
	 * @param shell
	 */
	public static void copySha1ToClipboard(String sha1, Shell shell) {
		Clipboard clipboard = new Clipboard(shell.getDisplay());
		try {
			clipboard.setContents(new String[] { sha1 },
					new Transfer[] { TextTransfer.getInstance() });
		} catch (SWTError ex) {
			if (ex.code != DND.ERROR_CANNOT_SET_CLIPBOARD) {
				throw ex;
			}
			String title = UIText.Header_copy_SHA1_error_title;
			String message = UIText.Header_copy_SHA1_error_message;
			if (MessageDialog.openQuestion(shell, title, message)) {
				copySha1ToClipboard(sha1, shell);
			}
		} finally {
			clipboard.dispose();
		}
	}
}
