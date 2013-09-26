/*******************************************************************************
 * Copyright (c) 2013 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Pfeifer (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import java.util.List;

import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler;
import org.eclipse.jgit.lib.RebaseTodoLine;

/**
 * Singleton {@link InteractiveHandler}
 */
public enum RebaseInteracitveHandler implements InteractiveHandler {
	/**
	 * Commonly used {@link InteractiveHandler} for (interactive) rebase
	 */
	INSTANCE;

	public String modifyCommitMessage(String commit) {
		// TODO show a CommitMessageDialog to change the commit message
		return commit;
	}

	public void prepareSteps(List<RebaseTodoLine> steps) {
		// do not change list of steps here. Instead change the list via
		// writeRebaseTodoFile of class Repository
	}
}