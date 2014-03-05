/******************************************************************************
 *  Copyright (c) 2014 Tasktop Technologies.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Tomasz Zarna (Tasktop) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.egit.ui.common.LocalRepositoryTestCase;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.services.IServiceLocator;
import org.junit.Before;
import org.junit.Test;

public class SwitchToMenuTest extends LocalRepositoryTestCase {

	private SwitchToMenu switchToMenu;

	private ISelectionService selectionService;

	private Menu menu;

	@Before
	public void setUp() throws Exception {

		switchToMenu = new SwitchToMenu();
		selectionService = mock(ISelectionService.class);
		IServiceLocator serviceLocator = mock(IServiceLocator.class);
		when(serviceLocator.getService(ISelectionService.class)).thenReturn(
				selectionService);
		switchToMenu.initialize(serviceLocator);

		menu = new Menu(new Shell());
	}

	@Test
	public void emptySelection() {
		when(selectionService.getSelection()).thenReturn(new EmptySelection());

		switchToMenu.fill(menu, 0 /* index */);

		assertEquals(0, menu.getItemCount());
	}

	@Test
	public void selectionNotAdaptableToRepository() {
		when(selectionService.getSelection()).thenReturn(
				new StructuredSelection(new Object()));

		switchToMenu.fill(menu, 0 /* index */);

		assertEquals(0, menu.getItemCount());
	}

	@Test
	public void selectionWithProj1() throws Exception {
		createProjectAndCommitToRepository();
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);
		when(selectionService.getSelection()).thenReturn(
				new StructuredSelection(project));

		switchToMenu.fill(menu, 0 /* index */);

		assertEquals(6, menu.getItemCount());
		assertEquals(UIText.SwitchToMenu_NewBranchMenuLabel, menu.getItem(0)
				.getText());
		assertEquals(SWT.SEPARATOR, menu.getItem(1).getStyle());
		assertEquals("master", menu.getItem(2).getText());
		assertEquals("stable", menu.getItem(3).getText());
		assertEquals(SWT.SEPARATOR, menu.getItem(4).getStyle());
		assertEquals(UIText.SwitchToMenu_OtherMenuLabel, menu.getItem(5)
				.getText());
	}

	@Test
	public void selectionWithRepositoryHavingOver20Branches() throws Exception {
		Repository repo = lookupRepository(createProjectAndCommitToRepository());
		for (int i = 0; i < SwitchToMenu.MAX_NUM_MENU_ENTRIES; i++) {
			createBranch(repo, "refs/heads/change/" + i);
		}
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);
		when(selectionService.getSelection()).thenReturn(
				new StructuredSelection(project));

		switchToMenu.fill(menu, 0 /* index */);

		assertEquals(24, menu.getItemCount());
		assertEquals(UIText.SwitchToMenu_NewBranchMenuLabel, menu.getItem(0)
				.getText());
		assertEquals(SWT.SEPARATOR, menu.getItem(1).getStyle());
		assertEquals("change/0", menu.getItem(2).getText());
		assertEquals("change/1", menu.getItem(3).getText());
		assertEquals("change/10", menu.getItem(4).getText());
		assertEquals("change/11", menu.getItem(5).getText());
		assertEquals("change/12", menu.getItem(6).getText());
		assertEquals("change/13", menu.getItem(7).getText());
		assertEquals("change/14", menu.getItem(8).getText());
		assertEquals("change/15", menu.getItem(9).getText());
		assertEquals("change/16", menu.getItem(10).getText());
		assertEquals("change/17", menu.getItem(11).getText());
		assertEquals("change/18", menu.getItem(12).getText());
		assertEquals("change/19", menu.getItem(13).getText());
		assertEquals("change/2", menu.getItem(14).getText());
		assertEquals("change/3", menu.getItem(15).getText());
		assertEquals("change/4", menu.getItem(16).getText());
		assertEquals("change/5", menu.getItem(17).getText());
		assertEquals("change/6", menu.getItem(18).getText());
		assertEquals("change/7", menu.getItem(19).getText());
		assertEquals("change/8", menu.getItem(20).getText());
		assertEquals("change/9", menu.getItem(21).getText());
		// "master" and "stable" didn't make it
		assertEquals(SWT.SEPARATOR, menu.getItem(22).getStyle());
		assertEquals(UIText.SwitchToMenu_OtherMenuLabel, menu.getItem(23)
				.getText());
	}

	private static class EmptySelection implements ISelection {
		public boolean isEmpty() {
			return true;
		}
	}
}
