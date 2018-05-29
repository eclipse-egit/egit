/*******************************************************************************
 * Copyright (c) 2010, 2015 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Chris Aniszczyk <caniszczyk@gmail.com> - initial implementation
 *    Lars Vogel <Lars.Vogel@vogella.com> - Bug 462866
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * A FilteredCheckboxTree implementation to be used internally in EGit code.  This tree stores
 * all the tree elements internally, and keeps the check state in sync.  This way, even if an
 * element is filtered, the caller can get and set the checked state.
 */
public class FilteredCheckboxTree extends FilteredTree {

	private static final long FILTER_DELAY = 400;

	FormToolkit fToolkit;
	CachedCheckboxTreeViewer checkboxViewer;

	/**
	 * Constructor that creates a tree with preset style bits and a CachedContainerCheckedTreeViewer for the tree.
	 *
	 * @param parent parent composite
	 * @param toolkit optional toolkit to create UI elements with, required if the tree is being created in a form editor
	 */
	public FilteredCheckboxTree(Composite parent, FormToolkit toolkit) {
		this(parent, toolkit, SWT.NONE);
	}

	/**
	 * Constructor that creates a tree with preset style bits and a CachedContainerCheckedTreeViewer for the tree.
	 *
	 * @param parent parent composite
	 * @param toolkit optional toolkit to create UI elements with, required if the tree is being created in a form editor
	 * @param treeStyle
	 */
	public FilteredCheckboxTree(Composite parent, FormToolkit toolkit, int treeStyle) {
		this(parent, toolkit, treeStyle, new PatternFilter());
	}

	/**
	 * Constructor that creates a tree with preset style bits and a CachedContainerCheckedTreeViewer for the tree.
	 *
	 * @param parent parent composite
	 * @param toolkit optional toolkit to create UI elements with, required if the tree is being created in a form editor
	 * @param treeStyle
	 * @param filter pattern filter to use in the filter control
	 */
	public FilteredCheckboxTree(Composite parent, FormToolkit toolkit, int treeStyle, PatternFilter filter) {
		super(parent, treeStyle, filter, true);
		fToolkit = toolkit;
	}

	@Override
	protected TreeViewer doCreateTreeViewer(Composite actParent, int style) {
		int treeStyle = style | SWT.CHECK | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER;
		Tree tree = null;
		if (fToolkit != null) {
			tree = fToolkit.createTree(actParent, treeStyle);
		} else {
			tree = new Tree(actParent, treeStyle);
		}

		checkboxViewer = new CachedCheckboxTreeViewer(tree);
		return checkboxViewer;
	}

	/*
	 * Overridden to hook a listener on the job and set the deferred content
	 * provider to synchronous mode before a filter is done.
	 *
	 */
	@Override
	protected WorkbenchJob doCreateRefreshJob() {
		WorkbenchJob filterJob = super.doCreateRefreshJob();
		filterJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							if (checkboxViewer.getTree().isDisposed())
								return;
							checkboxViewer.restoreLeafCheckState();
						}
					});
				}
			}
		});
		return filterJob;
	}

	@Override
	protected Text doCreateFilterText(Composite actParent) {
		// Overridden so the text gets create using the toolkit if we have one
		Text parentText = super.doCreateFilterText(actParent);
		if (fToolkit != null) {
			int style = parentText.getStyle();
			parentText.dispose();
			return fToolkit.createText(actParent, null, style);
		}
		return parentText;
	}

	@Override
	protected String getFilterString() {
		String filterString = super.getFilterString();
		if (!filterText.getText().equals(initialText)
				&& filterString.indexOf("*") != 0 //$NON-NLS-1$
				&& filterString.indexOf("?") != 0 //$NON-NLS-1$
				&& filterString.indexOf(".") != 0) {//$NON-NLS-1$
			filterString = "*" + filterString; //$NON-NLS-1$
		}
		return filterString;
	}

	/**
	 * Clears the filter
	 */
	public void clearFilter() {
		getPatternFilter().setPattern(null);
		setFilterText(getInitialText());
		textChanged();
	}

	/**
	 * @return The checkbox treeviewer
	 */
	public CachedCheckboxTreeViewer getCheckboxTreeViewer() {
		return checkboxViewer;
	}

	@Override
	protected long getRefreshJobDelay() {
		return FILTER_DELAY;
	}
}
