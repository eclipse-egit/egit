/*******************************************************************************
 * Copyright (C) 2010, Ketan Padegaonkar <KetanPadegaonkar@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.test;

import org.eclipse.egit.ui.prefpages.configuration.GlobalConfigurationPageTest;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewBranchHandlingTest;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewRemoteHandlingTest;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewRepoHandlingTest;
import org.eclipse.egit.ui.view.repositories.GitRepositoriesViewTest;
import org.eclipse.egit.ui.wizards.clone.GitCloneWizardTest;
import org.eclipse.egit.ui.wizards.share.SharingWizardTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses( { GitCloneWizardTest.class,//
		GlobalConfigurationPageTest.class, //
		SharingWizardTest.class, //
		GitRepositoriesViewBranchHandlingTest.class,//
		GitRepositoriesViewRepoHandlingTest.class,//
		GitRepositoriesViewRemoteHandlingTest.class,//
		GitRepositoriesViewTest.class //
})
public class AllTests {
	// empty class, don't need anything here
}
