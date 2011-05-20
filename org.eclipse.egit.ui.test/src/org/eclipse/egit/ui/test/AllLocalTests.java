/*******************************************************************************
 * Copyright (C) 2010, Ketan Padegaonkar <KetanPadegaonkar@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.test;

import org.eclipse.egit.ui.httpauth.PushTest;
import org.eclipse.egit.ui.internal.dialogs.SpellcheckableMessageAreaTest;
import org.eclipse.egit.ui.operations.GitScopeUtilTest;
import org.eclipse.egit.ui.prefpages.configuration.GlobalConfigurationPageTest;
import org.eclipse.egit.ui.test.history.HistoryViewTest;
import org.eclipse.egit.ui.test.team.actions.AllTeamActionTests;
import org.eclipse.egit.ui.test.trace.TraceConfigurationDialogTest;
import org.eclipse.egit.ui.view.repositories.AllRepositoriesViewTests;
import org.eclipse.egit.ui.view.synchronize.SynchronizeViewGitChangeSetModelTest;
import org.eclipse.egit.ui.view.synchronize.SynchronizeViewWorkspaceModelTest;
import org.eclipse.egit.ui.wizards.clone.GitCloneWizardHttpTest;
import org.eclipse.egit.ui.wizards.clone.GitCloneWizardTest;
import org.eclipse.egit.ui.wizards.share.SharingWizardTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses( { AllRepositoriesViewTests.class, 	//
		GlobalConfigurationPageTest.class,			//
		GitCloneWizardTest.class,                   //
		GitCloneWizardHttpTest.class,               //
		SharingWizardTest.class,					//
		AllTeamActionTests.class,                   //
		HistoryViewTest.class,                      //
		PushTest.class,
 GitScopeUtilTest.class,
		SpellcheckableMessageAreaTest.class,
		TraceConfigurationDialogTest.class,
		SynchronizeViewWorkspaceModelTest.class,
		SynchronizeViewGitChangeSetModelTest.class })
public class AllLocalTests {
	// empty class, don't need anything here
}
