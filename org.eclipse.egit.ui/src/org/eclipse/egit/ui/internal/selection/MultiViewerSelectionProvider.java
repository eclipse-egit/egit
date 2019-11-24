/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.selection;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;

/**
 * A {@link IPostSelectionProvider} for views with several viewers, tracking
 * focus changes among the viewers to supply the correct selection.
 */
public class MultiViewerSelectionProvider extends AbstractSelectionProvider {

	private final List<Viewer> viewers = new ArrayList<>();

	private final Listener focusHook = event -> {
		if (event.type == SWT.FocusIn && event.widget instanceof Control) {
			focusChanged((Control) event.widget);
		}
	};

	private final ISelectionChangedListener selectionHook = this::selectionChanged;

	private final ISelectionChangedListener postSelectionHook = this::postSelectionChanged;

	private Viewer currentViewer;

	/**
	 * Creates a new {@link MultiViewerSelectionProvider} for the given viewers.
	 * The first viewer given is assumed to be the one that's focused initially.
	 *
	 * @param providers
	 *            that contribute to this selection provider
	 */
	public MultiViewerSelectionProvider(Viewer... providers) {
		Assert.isLegal(providers != null && providers.length > 0);
		for (Viewer viewer : providers) {
			Assert.isLegal(viewer != null);
			viewers.add(viewer);
			viewer.getControl().addListener(SWT.FocusIn, focusHook);
			viewer.addSelectionChangedListener(selectionHook);
			if (viewer instanceof IPostSelectionProvider) {
				((IPostSelectionProvider) viewer)
						.addPostSelectionChangedListener(postSelectionHook);
			}
			if (currentViewer == null) {
				currentViewer = viewer;
			}
		}
	}

	private void focusChanged(Control control) {
		for (Viewer viewer : viewers) {
			if (control == viewer.getControl()) {
				if (viewer != currentViewer) {
					currentViewer = viewer;
					fireSelectionChanged(getSelectionListeners());
					fireSelectionChanged(getPostSelectionListeners());
				}
				return;
			}
		}
	}

	private void selectionChanged(SelectionChangedEvent event) {
		if (event.getSelectionProvider() == currentViewer) {
			fireSelectionChanged(getSelectionListeners());
		}
	}

	private void postSelectionChanged(SelectionChangedEvent event) {
		if (event.getSelectionProvider() == currentViewer) {
			fireSelectionChanged(getPostSelectionListeners());
		}
	}

	@Override
	public ISelection getSelection() {
		if (currentViewer != null) {
			return currentViewer.getSelection();
		}
		return StructuredSelection.EMPTY;
	}

	@Override
	public void setSelection(ISelection selection) {
		if (currentViewer != null) {
			currentViewer.setSelection(selection);
		}
	}
}
