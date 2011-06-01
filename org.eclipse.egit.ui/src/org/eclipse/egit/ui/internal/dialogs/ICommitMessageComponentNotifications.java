/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

/**
 * Call back interface for handling notifications from a
 * {@link CommitMessageComponent}
 *
 */
public interface ICommitMessageComponentNotifications {

	/**
	 * The component host must update its signed off toggle
	 *
	 * @param selection
	 */
	void updateSignedOffToggleSelection(boolean selection);

	/**
	 * The component host must update its change id toggle
	 *
	 * @param selection
	 */
	void updateChangeIdToggleSelection(boolean selection);
}
