/*******************************************************************************
 * Copyright (C) 2017, Stefan Rademacher <stefan.rademacher@tk.de>
 *
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.internal.dialogs.CommitDialog;

/**
 * This interface must be implemented to be a commit message provider, that does
 * not only provide the message itself, but also a caret position within this
 * message. This message will be added to the text field in the
 * {@link CommitDialog}. <br/>
 *
 * @see ICommitMessageProvider
 * @see CommitDialog
 */
public interface ICommitMessageProvider2 extends ICommitMessageProvider {

	/**
	 * Unlike {@link #getMessage(IResource[])}, this method provides a way to
	 * retrieve not only a commit message but also a caret position within the
	 * message.
	 *
	 * @param resources
	 *            the selected resources, when this method is called.
	 *
	 * @return an object, containing the commit message and the caret position
	 *         within the message
	 */
	CommitMessageWithCaretPosition getCommitMessageWithPosition(
			IResource[] resources);

}
