/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.synchronize;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.internal.actions.CommitAction;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

class CommitOperation extends SynchronizeModelOperation {

	private ISynchronizePageConfiguration configuration;

	CommitOperation(ISynchronizePageConfiguration configuration,
			IDiffElement[] elements) {
		super(configuration, elements);
		this.configuration = configuration;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		final CommitAction action = new CommitAction();
		action.selectionChanged(null, new StructuredSelection(getSyncInfoSet()
				.getSyncInfos()));

		configuration.getSite().getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
				// ideally the code in CommitAction should be refactored so it can be
				// consumed in better ways
				action.run(null);
			}
		});
	}

}
