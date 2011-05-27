/*******************************************************************************
 * Copyright (c) 2010, SAP AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Lay (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

public class ExistingOrNewPage {

	private static final SWTWorkbenchBot bot = new SWTWorkbenchBot();

	@SuppressWarnings("boxing")
	public void assertEnabling(boolean createRepository, boolean textField,
			boolean finish) {
		assertEquals(createRepository, bot.button("Create Repository")
				.isEnabled());
		assertEquals(textField, bot.text().isEnabled());
		assertEquals(finish, bot.button("Finish").isEnabled());
	}

	public void assertContents(boolean selected, String project, String path, String repository,
			String newRepoPath) {
		assertContents(new Row[] { new Row(selected, project, path, repository) },
				newRepoPath);
	}

	public void assertContents(Row[] rows, String newRepoPath) {
		assertContents(rows);
		assertEquals(newRepoPath, bot.text().getText());
	}

	@SuppressWarnings("boxing")
	private void assertContents(Row[] rows) {
		assertEquals(rows.length, bot.tree().rowCount());
		for (int i = 0; i < rows.length; i++) {
			assertEquals(rows[i].isSelected(), bot.tree().getAllItems()[i].isChecked());
			assertEquals(rows[i].getProject(), bot.tree().cell(i, 0));
			assertEquals(rows[i].getPath(), bot.tree().cell(i, 1));
			assertEquals(rows[i].getRepository(), bot.tree().cell(i, 2));
			SWTBotTreeItem subteeItems = bot.tree().getAllItems()[i];
			Row[] subrows = rows[i].getSubrows();
			if (subrows != null) {
				assertEquals("Row " + i + " is a tree:", subrows.length, subteeItems.getItems().length);
				assertNotNull("Rows " + i + " is not a tree", subteeItems.getItems());
				for (int j = 0; j < subrows.length; ++j) {
					Row r = subrows[j];
					assertEquals(r.isSelected(), subteeItems.getItems()[j].isChecked());
					assertEquals(r.getProject(), subteeItems.cell(j, 0));
					assertEquals(r.getPath(), subteeItems.cell(j, 1));
					assertEquals(r.getRepository(), subteeItems.cell(j, 2));
				}
			} else
				assertEquals("Row " + i + " is a tree:", 0, subteeItems.getItems().length);
		}
	}

	public static class Row {
		private final boolean selected;

		private String project;

		private String path;

		private String repository;

		private final Row[] subrows;

		public Row(boolean selected, String project, String path, String repository) {
			this(selected, project, path, repository, null);
		}

		public Row(boolean selected, String project, String path, String repository, Row[] subrows) {
			this.selected = selected;
			this.project = project;
			this.path = path;
			this.repository = repository;
			if (subrows != null) {
				this.subrows = new Row[subrows.length];
				System.arraycopy(subrows, 0, this.subrows, 0, subrows.length);
			} else
				this.subrows = null;
		}

		String getProject() {
			return project;
		}

		String getPath() {
			return path;
		}

		String getRepository() {
			return repository;
		}

		Row[] getSubrows() {
			return subrows;
		}

		boolean isSelected() {
			return selected;
		}
	}

}
