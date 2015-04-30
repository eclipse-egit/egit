/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.ui.internal.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 *
 */
public class GitFlowHandlerUtil {
	/**
	 * @param event
	 * @return Selected GitFlowRepository
	 */
	public static GitFlowRepository getRepository(ExecutionEvent event) {
		IStructuredSelection selection = (IStructuredSelection) HandlerUtil
				.getCurrentSelection(event);
		PlatformObject firstElement = (PlatformObject) selection
				.getFirstElement();
		//TODO: on Mars this leads to an "unnecessary cast to Repository" error.
		// Use Utils.getAdapter() to fix this. See commit 47cbc54b48f
		Repository repository = (Repository) firstElement
				.getAdapter(Repository.class);
		if (repository == null) {
			return null;
		}
		return new GitFlowRepository(repository);
	}

}
