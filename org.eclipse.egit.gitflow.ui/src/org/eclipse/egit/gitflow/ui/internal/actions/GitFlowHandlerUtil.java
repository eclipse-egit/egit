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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.jdt.annotation.Nullable;
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
	public static @Nullable GitFlowRepository getRepository(ExecutionEvent event) {
		IStructuredSelection selection = (IStructuredSelection) HandlerUtil
				.getCurrentSelection(event);
		Object firstElement = selection.getFirstElement();
		if (!(firstElement instanceof IAdaptable)) {
			return null;
		}
		Repository repository = Utils.getAdapter((IAdaptable) firstElement,
				Repository.class);
		if (repository == null) {
			IResource resource = Utils.getAdapter((IAdaptable) firstElement,
					IResource.class);
			if (resource != null) {
				repository = Utils.getAdapter(resource, Repository.class);
			}
		}
		if (repository == null) {
			return null;
		}
		return new GitFlowRepository(repository);
	}

}
