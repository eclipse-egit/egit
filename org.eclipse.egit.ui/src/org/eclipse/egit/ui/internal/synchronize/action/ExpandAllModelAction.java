/*******************************************************************************
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize.action;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

/**
 * 'Expand All' tool bar action
 */
public class ExpandAllModelAction extends SynchronizeModelAction {

	private static final class ExpandAllAction implements Runnable {
		private final ISynchronizePageConfiguration configuration;

		private ExpandAllAction(ISynchronizePageConfiguration configuration) {
			this.configuration = configuration;
		}

		@Override
		public void run() {
			Viewer viewer = configuration.getPage().getViewer();
			if (viewer == null || viewer.getControl().isDisposed()
					|| !(viewer instanceof AbstractTreeViewer)) {
				return;
			}
			UIUtils.expandAll(((AbstractTreeViewer) viewer));
		}
	}

	/**
	 * Creates 'Expand All' tool bar action
	 *
	 * @param text
	 * @param configuration
	 */
	public ExpandAllModelAction(String text,
			ISynchronizePageConfiguration configuration) {
		super(text, configuration);
	}

	@Override
	protected SynchronizeModelOperation getSubscriberOperation(
			final ISynchronizePageConfiguration configuration,
			IDiffElement[] elements) {
		return new SynchronizeModelOperation(configuration, elements) {

			@Override
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				configuration.getSite().getShell().getDisplay()
						.syncExec(new ExpandAllAction(configuration));
			}
		};
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		return true;
	}

}
