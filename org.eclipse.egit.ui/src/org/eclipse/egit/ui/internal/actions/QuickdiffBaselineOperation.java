/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.decorators.GitQuickDiffProvider;
import org.eclipse.jgit.lib.Repository;

/**
 * UI operation to change the git quickdiff baseline
 */
public class QuickdiffBaselineOperation extends AbstractRevObjectOperation {

	private final String baseline;

	/**
	 * Construct a QuickdiffBaselineOperation for changing quickdiff baseline
	 * @param repository
	 *
	 * @param baseline
	 */
	QuickdiffBaselineOperation(final Repository repository, final String baseline) {
		super(repository);
		this.baseline = baseline;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.egit.core.op.IEGitOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(IProgressMonitor monitor) throws CoreException {
		try {
			GitQuickDiffProvider.setBaselineReference(repository, baseline);
		} catch (IOException e) {
			Activator
					.logError(
							UIText.QuickdiffBaselineOperation_baseline,
							e);
		}
	}

	public ISchedulingRule getSchedulingRule() {
		return null;
	}

}
