/*******************************************************************************
 * Copyright (C) 2010, Thorsten Kamann <thorsten@kamann.info>
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
 * This interface must be implemented to be a commit message provider. A commit
 * message provider provides the complete or a fragment of a commit message.
 * This message will be added to the text field in the {@link CommitDialog}. <br/>
 *
 * @see CommitDialog
 */
public interface ICommitMessageProvider {

	/**
	 * @param resources
	 * @return the message the CommitDialog should use as default message or
	 * <code>null</code> if this provider cannot provide a commit message
	 */
	public String getMessage(IResource[] resources);
}
