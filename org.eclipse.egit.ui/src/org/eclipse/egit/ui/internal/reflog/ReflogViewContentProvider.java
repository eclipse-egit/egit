/*******************************************************************************
 * Copyright (c) 2011, 2017 Chris Aniszczyk <caniszczyk@gmail.com> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Chris Aniszczyk <caniszczyk@gmail.com> - initial implementation
 *   EclipseSource - Filtered Viewer
 *   Thomas Wolf <thomas.wolf@paranor.ch> - deferred loading
 *******************************************************************************/
package org.eclipse.egit.ui.internal.reflog;

import java.util.Collection;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ReflogEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.progress.DeferredTreeContentManager;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;

/**
 * A content provider for reflog entries given a repository and a ref.
 */
public class ReflogViewContentProvider implements ITreeContentProvider {

	private DeferredTreeContentManager loader;

	private Object currentInput;

	/**
	 * Input class for this content provider
	 */
	public static class ReflogInput extends WorkbenchAdapter
			implements IDeferredWorkbenchAdapter {

		private final Repository repository;

		private final String ref;

		private Collection<ReflogEntry> refLog;

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
		 * @return repository
		 */
		public Repository getRepository() {
			return repository;
		}

		@Override
		public Object[] getChildren(Object o) {
			if (refLog != null) {
				return refLog.toArray();
			}
			return super.getChildren(o);
		}

		@Override
		public void fetchDeferredChildren(Object object,
				IElementCollector collector, IProgressMonitor monitor) {
			try (Git git = new Git(repository)) {
				refLog = git.reflog().setRef(ref).call();
				collector.add(refLog.toArray(), monitor);
			} catch (Exception e) {
				Activator.logError("Error running reflog command", e); //$NON-NLS-1$
				collector.add(new ErrorElement(), monitor);
			}
		}

		@Override
		public boolean isContainer() {
			return true;
		}

		@Override
		public ISchedulingRule getRule(Object object) {
			return null;
		}
	}

	private static class ErrorElement extends WorkbenchAdapter {

		@Override
		public String getLabel(Object object) {
			return UIText.ReflogView_ErrorOnLoad;
		}
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (oldInput != null && loader != null) {
			loader.cancel(oldInput);
		}
		currentInput = newInput;
		if (viewer instanceof AbstractTreeViewer && newInput != null) {
			loader = new DeferredTreeContentManager(
					(AbstractTreeViewer) viewer);
		}
	}

	@Override
	public void dispose() {
		if (currentInput != null && loader != null) {
			loader.cancel(currentInput);
		}
		currentInput = null;
		loader = null;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof ReflogInput && loader != null) {
			return loader.getChildren(parentElement);
		}
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return false;
	}
}
