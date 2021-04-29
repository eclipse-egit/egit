/*******************************************************************************
 * Copyright (C) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.merge;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.contentmergeviewer.ContentMergeViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

/**
 * An {@link Action} targeted at a particular {@link ContentMergeViewer} in a
 * comparison defined by a {@link CompareEditorInput}. The action listens to
 * changes in the {@link org.eclipse.compare.CompareConfiguration
 * CompareConfiguration}, mainly to be able to adapt to changes regarding
 * mirroring.
 */
public abstract class CompareEditorInputViewerAction extends Action
		implements IWorkbenchAction, IPropertyChangeListener {

	private final CompareEditorInput comparison;

	private ContentMergeViewer viewer;

	/**
	 * Creates a new {@link CompareEditorInputViewerAction}.
	 *
	 * @param name
	 *            of the action
	 * @param comparison
	 *            the action is for
	 */
	protected CompareEditorInputViewerAction(String name,
			CompareEditorInput comparison) {
		super(name);
		this.comparison = comparison;
		comparison.getCompareConfiguration().addPropertyChangeListener(this);
	}

	/**
	 * Creates a new {@link CompareEditorInputViewerAction}.
	 *
	 * @param name
	 *            of the action
	 * @param style
	 *            of the action
	 * @param comparison
	 *            the action is for
	 */
	protected CompareEditorInputViewerAction(String name, int style,
			CompareEditorInput comparison) {
		super(name, style);
		this.comparison = comparison;
		comparison.getCompareConfiguration().addPropertyChangeListener(this);
	}

	@Override
	public void dispose() {
		getInput().getCompareConfiguration().removePropertyChangeListener(this);
	}

	/**
	 * Retrieves the {@link CompareEditorInput} this action was created for.
	 *
	 * @return the {@link CompareEditorInput}
	 */
	protected CompareEditorInput getInput() {
		return comparison;
	}

	/**
	 * Set the {@link ContentMergeViewer} this action shall target.
	 *
	 * @param viewer
	 *            to set, if {@code null}, the action is disabled
	 */
	public void setViewer(ContentMergeViewer viewer) {
		this.viewer = viewer;
		if (viewer == null) {
			super.setEnabled(false);
		}
	}

	/**
	 * Retrieves the {@link ContentMergeViewer} this action currently targets.
	 *
	 * @return the viewer, or {@code null} if none
	 */
	protected ContentMergeViewer getViewer() {
		return viewer;
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled && viewer != null);
	}
}
