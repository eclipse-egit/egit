/*******************************************************************************
 * Copyright (c) 2012 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.wizards.clone;

import org.eclipse.egit.ui.internal.provisional.wizards.GitRepositoryInfo;
import org.eclipse.egit.ui.internal.provisional.wizards.IRepositorySearchResult;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class TestRepositorySearchPage extends WizardPage implements
		IRepositorySearchResult {

	public TestRepositorySearchPage() {
		super(TestRepositorySearchPage.class.getName());
		setTitle("Find Repository");
		setMessage("Do the needful");
	}

	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		new Label(main, SWT.NULL).setText("Search for Repos");
		setControl(main);
	}

	@Override
	public GitRepositoryInfo getGitRepositoryInfo() {
			return new GitRepositoryInfo(
					"http://egit.eclipse.org/r/p/egit-training");
	}

}
