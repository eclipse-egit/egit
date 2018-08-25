/*******************************************************************************
 * Copyright (c) 2011, 2017 Chris Aniszczyk <caniszczyk@gmail.com> and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Chris Aniszczyk <caniszczyk@gmail.com> - initial implementation
 *   EclipseSource - Filtered Viewer
 *   Thomas Wolf <thomas.wolf@paranor.ch> - deferred loading
 *******************************************************************************/
package org.eclipse.egit.ui.internal.reflog;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.swt.widgets.Control;
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
	 * Serializes concurrent attempts to load the reflog.
	 */
	private static class ReflogSchedulingRule implements ISchedulingRule {

		private final File gitDir;

		public ReflogSchedulingRule(File gitDir) {
			this.gitDir = gitDir;
		}

		@Override
		public boolean contains(ISchedulingRule rule) {
			if (rule instanceof ReflogSchedulingRule) {
				return Objects.equals(gitDir,
						((ReflogSchedulingRule) rule).gitDir);
			}
			return false;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return rule instanceof ReflogSchedulingRule;
		}

	}

	private static final WorkbenchAdapter ERROR_ELEMENT = new WorkbenchAdapter() {

		@Override
		public String getLabel(Object object) {
			return UIText.ReflogView_ErrorOnLoad;
		}
	};


	/**
	 * Input class for this content provider.
	 */
	public static class ReflogInput extends WorkbenchAdapter
			implements IDeferredWorkbenchAdapter {

		private final Repository repository;

		private final String ref;

		private final ISchedulingRule rule;

		private ReflogItem[] refLog;

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
			this.rule = new ReflogSchedulingRule(repository.getDirectory());
		}

		/**
		 * Get repository
		 *
		 * @return repository
		 */
		public Repository getRepository() {
			return repository;
		}

		/**
		 * Retrieves the ref.
		 *
		 * @return the ref
		 */
		public String getRef() {
			return ref;
		}

		@Override
		public Object[] getChildren(Object o) {
			if (refLog != null) {
				return refLog;
			}
			return null;
		}

		@Override
		public void fetchDeferredChildren(Object object,
				IElementCollector collector, IProgressMonitor monitor) {
			if (refLog != null) {
				return; // Already loaded.
			}
			try (Git git = new Git(repository);
					RevWalk walk = new RevWalk(repository)) {
				refLog = git.reflog().setRef(ref).call().stream()
						.map(entry -> {
							String commitMessage = null;
							try {
								commitMessage = walk
										.parseCommit(entry.getNewId())
										.getShortMessage();
							} catch (IOException e) {
								// Ignore here
							}
							return new ReflogItem(ReflogInput.this, entry,
									commitMessage);
						})
						.toArray(ReflogItem[]::new);
				collector.add(refLog, monitor);
			} catch (Exception e) {
				Activator.logError("Error running reflog command", e); //$NON-NLS-1$
				collector.add(ERROR_ELEMENT, monitor);
			}
		}

		@Override
		public boolean isContainer() {
			return true;
		}

		@Override
		public ISchedulingRule getRule(Object object) {
			return rule;
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
			loader = new DeferredBatchLoader((AbstractTreeViewer) viewer);
			loader.addUpdateCompleteListener(new JobChangeAdapter() {

				@Override
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {
						// Force a selection event
						viewer.setSelection(viewer.getSelection());
					}
				}
			});
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
			Object[] knownChildren = ((ReflogInput) parentElement)
					.getChildren(parentElement);
			if (knownChildren != null) {
				return knownChildren;
			}
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

	/**
	 * A variant of {@link DeferredTreeContentManager} that doesn't use a
	 * separate UI job to fill in the tree. With UI jobs, it's simply impossible
	 * to know what has already been added when there are several loading jobs.
	 * For our use case (load the whole reflog, then add it to the tree) a
	 * {@link org.eclipse.swt.widgets.Display#syncExec(Runnable) syncExec()} is
	 * sufficient.
	 */
	private static class DeferredBatchLoader
			extends DeferredTreeContentManager {

		private AbstractTreeViewer viewer;

		public DeferredBatchLoader(AbstractTreeViewer viewer) {
			super(viewer);
			this.viewer = viewer;
		}

		/**
		 * Add child nodes, removing the error element if appropriate. Contrary
		 * to the super implementation, this does <em>not</em> use a UI job but
		 * a simple {@link org.eclipse.swt.widgets.Display#syncExec(Runnable)
		 * syncExec()}.
		 *
		 * @param parent
		 *            to add the {@code children} to
		 * @param children
		 *            to add to the {@code parent}
		 * @param monitor
		 *            is ignored
		 */
		@Override
		protected void addChildren(Object parent, Object[] children,
				IProgressMonitor monitor) {
			Control control = viewer.getControl();
			if (control == null || control.isDisposed()) {
				return;
			}
			control.getDisplay().syncExec(() -> {
				if (!control.isDisposed()) {
					try {
						control.setRedraw(false);
						if (children.length != 1
								|| children[0] != ERROR_ELEMENT) {
							viewer.remove(ERROR_ELEMENT);
						}
						viewer.add(parent, children);
					} finally {
						control.setRedraw(true);
					}
				}
			});
		}
	}
}
