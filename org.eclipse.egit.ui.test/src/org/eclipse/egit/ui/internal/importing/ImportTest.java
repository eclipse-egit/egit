/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.importing;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.widgetIsEnabled;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.junit.Test;
/**
 * Tests for importing plug-ins from a repository
 */
public class ImportTest extends LocalRepositoryTestCase {

	@Test
	public void importFromMainMenu() throws InterruptedException {
		openImportPluginsAndFragmentsWizard();
		bot.shell("Import Plug-ins and Fragments").activate();

		// "Import Plug-ins and Fragments" page
		bot.radioInGroup("The active target platform", "Import From").click();
		bot.radioInGroup("Select from all plug-ins and fragments found at the specified location", "Plug-ins and Fragments to Import").click();
		bot.radioInGroup("Projects from a repository", "Import As").click();
		bot.button(IDialogConstants.NEXT_LABEL).click();
		bot.waitUntil(widgetIsEnabled(bot.button(IDialogConstants.BACK_LABEL)), 5000);

		// "Selection" page
		bot.textInGroup("Filter Available Plug-ins and Fragments", 0).setText("org.eclipse.compare");
		waitInUI();
		bot.table().select(0);
		bot.button("Add ->").click();
		bot.button(IDialogConstants.NEXT_LABEL).click();

		// "Import Projects from Git" page
		assertTrue(bot.radio("Import from master").isEnabled());
		assertEquals(1, bot.table().rowCount());
		assertFalse(bot.button(IDialogConstants.NEXT_LABEL).isEnabled());
		assertTrue(bot.button(IDialogConstants.FINISH_LABEL).isEnabled());

		// TODO: proceed with import
	}

	private void openImportPluginsAndFragmentsWizard() {
		bot.menu("File").menu("Import...").click();
		bot.shell("Import").activate();
		bot.tree().expandNode("Plug-in Development").select("Plug-ins and Fragments");
		bot.button(IDialogConstants.NEXT_LABEL).click();
	}
}
