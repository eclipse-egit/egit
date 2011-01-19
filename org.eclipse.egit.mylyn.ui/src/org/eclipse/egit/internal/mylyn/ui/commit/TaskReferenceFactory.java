/*******************************************************************************
 * Copyright (c) 2011 Benjamin Muskalla and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Benjamin Muskalla <benjamin.muskalla@tasktop.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn.ui.commit;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.egit.ui.internal.synchronize.model.GitModelCommit;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.mylyn.internal.team.ui.LinkedTaskInfo;
import org.eclipse.mylyn.team.ui.AbstractTaskReference;

/**
 * Adapter factory to bridge between Mylyn and EGit domain models.
 */
public class TaskReferenceFactory implements IAdapterFactory {
	private static final Class<?>[] ADAPTER_TYPES = new Class[] { AbstractTaskReference.class };

	@SuppressWarnings({ "rawtypes" })
	public Class[] getAdapterList() {
		return ADAPTER_TYPES;
	}

	@SuppressWarnings("rawtypes")
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (!AbstractTaskReference.class.equals(adapterType))
			return null;

		return adaptFromComment(adaptableObject);
	}

	private AbstractTaskReference adaptFromComment(Object element) {
		String comment;
		RevCommit commit = getCommitForElement(element);
		if(commit != null) {
			comment = commit.getFullMessage();
		} else {
			return null;
		}
		return new LinkedTaskInfo(null, null, null, comment);
	}

	private static RevCommit getCommitForElement(Object element) {
		RevCommit commit = null;
		if (element instanceof RevCommit) {
			commit = (RevCommit) element;
		} else if (element instanceof GitModelCommit) {
			GitModelCommit modelCommit = (GitModelCommit) element;
			commit= modelCommit.getBaseCommit();
		}
		return commit;
	}

}
