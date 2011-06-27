/*******************************************************************************
 * Copyright (c) 2011, Chris Aniszczyk <caniszczyk@gmail.com> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Chris Aniszczyk <caniszczyk@gmail.com> - initial implementation
 *   EclipseSource - Filtered Viewer
 *******************************************************************************/
package org.eclipse.egit.ui.internal.reflog;

import java.util.Collection;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.ReflogEntry;

/**
 * A content provider for reflog entries given a repository
 */
public class ReflogViewContentProvider implements
		ITreeContentProvider {

	public Object[] getElements(Object inputElement) {
		if(inputElement instanceof Repository) {
			Repository repository = (Repository) inputElement;
			Git git = new Git(repository);
			try {
				Collection<ReflogEntry> entries = git.reflog().call();
				return entries.toArray(new ReflogEntry[entries.size()]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public void inputChanged(Viewer viewer, Object oldInput,
			Object newInput) {
		// TODO
	}

	public void dispose() {
		// Do nothing
	}

	public Object[] getChildren(Object parentElement) {
		return null;
	}

	public Object getParent(Object element) {
		return null;
	}

	public boolean hasChildren(Object element) {
		return false;
	}
}
