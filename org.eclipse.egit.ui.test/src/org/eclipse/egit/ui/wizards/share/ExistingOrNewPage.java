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
package org.eclipse.egit.ui.wizards.share;

import static org.junit.Assert.assertEquals;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;

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

	public void assertContents(String project, String path, String repository,
			String newRepoPath) {
		assertContents(new Row[] { new Row(project, path, repository) },
				newRepoPath);
	}

	public void assertContents(Row[] rows, String newRepoPath) {
		assertEquals(rows.length, bot.tree().rowCount());
		for (int i = 0; i < rows.length; i++) {
			assertEquals(rows[i].getProject(), bot.tree().cell(i, 0));
			assertEquals(rows[i].getPath(), bot.tree().cell(i, 1));
			assertEquals(rows[i].getRepository(), bot.tree().cell(i, 2));
		}
		assertEquals(newRepoPath, bot.text().getText());
	}

	public static class Row {
		private String project;

		private String path;

		private String repository;

		public Row(String project, String path, String repository) {
			this.project = project;
			this.path = path;
			this.repository = repository;
		}

		public String getProject() {
			return project;
		}

		public String getPath() {
			return path;
		}

		public String getRepository() {
			return repository;
		}

	}

}
