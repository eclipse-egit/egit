/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Creates a branch based on another branch or on a commit.
 */
public class CreateBranchWizard extends Wizard {
	/**
	 * @param repository
	 *            the repository
	 * @param baseBranch
	 *            the base branch, may be null
	 */
	public CreateBranchWizard(Repository repository, Ref baseBranch) {
		myPage = new CreateBranchPage(repository, baseBranch);
		setWindowTitle(UIText.CreateBranchWizard_NewBranchTitle);
	}

	/**
	 * @param repository
	 *            the repository
	 * @param baseCommit
	 *            the base commit, must not be null
	 */
	public CreateBranchWizard(Repository repository, RevCommit baseCommit) {
		myPage = new CreateBranchPage(repository, baseCommit);
		setWindowTitle(UIText.CreateBranchWizard_NewBranchTitle);
	}

	private CreateBranchPage myPage;

	@Override
	public void addPages() {
		addPage(myPage);
	}

	@Override
	public boolean performFinish() {
		try {
			getContainer().run(false, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					CreateBranchPage cp = (CreateBranchPage) getPages()[0];
					try {
						cp.createBranch(monitor);
					} catch (CoreException ce) {
						throw new InvocationTargetException(ce);
					} catch (IOException ioe) {
						throw new InvocationTargetException(ioe);
					}
				}
			});
		} catch (InvocationTargetException ite) {
			Activator.handleError(UIText.CreateBranchWizard_CreationFailed, ite
					.getCause(), true);
			return false;
		} catch (InterruptedException ie) {
			// ignore here
		}
		return true;
	}
}
