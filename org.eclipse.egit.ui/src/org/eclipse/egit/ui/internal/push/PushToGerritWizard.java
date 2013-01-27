/*******************************************************************************
 * Copyright (c) 2012 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Repository;

/**
 * Wizard for pushing the current HEAD to Gerrit
 */
public class PushToGerritWizard extends Wizard {
	private final Repository repository;

	PushToGerritPage page;

	/**
	 * @param repository
	 *            the repository
	 */
	public PushToGerritWizard(Repository repository) {
		Assert.isNotNull(repository);
		this.repository = repository;
		setNeedsProgressMonitor(true);
		setHelpAvailable(false);
		setWindowTitle(UIText.PushToGerritWizard_Title);
	}

	@Override
	public void addPages() {
		page = new PushToGerritPage(repository);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		try {
			getContainer().run(false, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					page.doPush(monitor);
				}
			});
		} catch (InvocationTargetException e) {
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}
}
