/*******************************************************************************
 * Copyright (C) 2009, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2010, Ketan Padegaonkar <KetanPadegaonkar@gmail.com>
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.performance;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public abstract class EGitTestCase {

	protected static final SWTWorkbenchBot bot = new SWTWorkbenchBot();
	protected static final TestUtil util = new TestUtil();
	private static volatile boolean welcomePageClosed = false;

	@BeforeClass
	public static void closeWelcomePage() {
		if (welcomePageClosed)
			return;
		try {
			bot.viewByTitle("Welcome").close();
		} catch (WidgetNotFoundException e) {
			// somebody else probably closed it, lets not feel bad about it.
		} finally {
			welcomePageClosed = true;
		}
	}

	@After
	public void resetWorkbench() {
		new Eclipse().reset();
	}

	public static void waitForWorkspaceRefresh() {
		WorkspaceRefreshHook wrh = new WorkspaceRefreshHook();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(wrh);

		try {
			bot.waitUntil(wrh, 120000);
		} finally {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(wrh);
		}
	}

	private static class WorkspaceRefreshHook extends DefaultCondition
			implements IResourceChangeListener {
		private boolean state = false;

		public void resourceChanged(IResourceChangeEvent event) {
			if (event.getType() == IResourceChangeEvent.POST_CHANGE)
				state = true;
		}

		public String getFailureMessage() {
			return "Failed waiting for workspace refresh.";
		}

		public boolean test() throws Exception {
			return state;
		}
	}

}
