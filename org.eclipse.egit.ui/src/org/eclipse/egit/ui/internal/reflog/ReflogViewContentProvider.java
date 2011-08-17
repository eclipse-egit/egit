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

import org.eclipse.core.runtime.Assert;
import org.eclipse.egit.ui.Activator;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ReflogCommand;
import org.eclipse.jgit.lib.Repository;

/**
 * A content provider for reflog entries given a repository
 */
public class ReflogViewContentProvider implements ITreeContentProvider {

	/**
	 * Input class for this content provider
	 */
	public static class ReflogInput {

		private final Repository repository;

		private final String ref;

		/**
		 * Create input with non-null repository and non-null ref
		 *
		 * @param repository
		 * @param ref
		 */
		public ReflogInput(Repository repository, String ref) {
			Assert.isNotNull(repository, "Repository cannot be null"); //$NON-NLS-1$
			Assert.isNotNull(ref, "Ref cannot be null"); //$NON-NLS-1$
			this.repository = repository;
			this.ref = ref;
		}

		/**
		 * Get repository
		 *
		 * @return repositoyr
		 */
		public Repository getRepository() {
			return repository;
		}

		/**
		 * Get ref
		 *
		 * @return ref
		 */
		public String getRef() {
			return ref;
		}
	}

	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof ReflogInput) {
			ReflogInput input = (ReflogInput) inputElement;
			ReflogCommand command = new Git(input.repository).reflog();
			command.setRef(input.ref);
			try {
				return command.call().toArray();
			} catch (Exception e) {
				Activator.logError("Error running reflog command", e); //$NON-NLS-1$
			}
		}
		return new Object[0];
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
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
