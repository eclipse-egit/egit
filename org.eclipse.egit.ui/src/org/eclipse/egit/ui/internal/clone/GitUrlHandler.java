/*******************************************************************************
 * Copyright (c) 2011, 2017 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Ian Pun - reimplemented to work with Git Cloning DND
 *******************************************************************************/
package org.eclipse.egit.ui.internal.clone;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.egit.ui.internal.repository.tree.command.CloneCommand;

/**
 * Handler for git URLs.
 *
 * @author Ian Pun
 *
 */
public abstract class GitUrlHandler {


	private static final String GIT = "github"; //$NON-NLS-1$

	/**
	 * @param url
	 * @return boolean
	 */
	public static boolean isPotentialSolution(String url) {
		return url != null && url.contains(GIT);
	}

	/**
	 * @param url
	 */
	public static void triggerClone(String url) {
		CloneCommand command = new CloneCommand(url);
		try {
			command.execute(new ExecutionEvent());
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
