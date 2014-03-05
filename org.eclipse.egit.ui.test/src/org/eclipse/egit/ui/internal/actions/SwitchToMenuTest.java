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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.services.IServiceLocator;
import org.junit.Before;
import org.junit.Test;

public class SwitchToMenuTest extends LocalRepositoryTestCase {

	private SwitchToMenu switchToMenu;

	private ISelectionService selectionService;

	@Before
	public void setUp() throws Exception {
		switchToMenu = new SwitchToMenu();
		selectionService = mock(ISelectionService.class);
		IServiceLocator serviceLocator = mock(IServiceLocator.class);
		when(serviceLocator.getService(ISelectionService.class)).thenReturn(
				selectionService);
		switchToMenu.initialize(serviceLocator);
	}

	@Test
	public void emptySelection() {
		when(selectionService.getSelection()).thenReturn(new EmptySelection());

		MenuItem[] items = fillMenu();

		assertEquals(0, items.length);
	}

	@Test
	public void selectionNotAdaptableToRepository() {
		when(selectionService.getSelection()).thenReturn(
				new StructuredSelection(new Object()));

		MenuItem[] items = fillMenu();

		assertEquals(0, items.length);
	}

	@Test
	public void selectionWithProj1() throws Exception {
		createProjectAndCommitToRepository();
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(PROJ1);
		when(selectionService.getSelection()).thenReturn(
				new StructuredSelection(project));

		MenuItem[] items = fillMenu();

		assertEquals(6, items.length);
		assertEquals(UIText.SwitchToMenu_NewBranchMenuLabel, items[0].getText());
		assertEquals(SWT.SEPARATOR, items[1].getStyle());
		assertEquals("master", items[2].getText());
		assertEquals("stable", items[3].getText());
		assertEquals(SWT.SEPARATOR, items[4].getStyle());
		assertEquals(UIText.SwitchToMenu_OtherMenuLabel, items[5].getText());
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

		MenuItem[] items = fillMenu();

		assertEquals(24, items.length);
		assertEquals(UIText.SwitchToMenu_NewBranchMenuLabel, items[0].getText());
		assertEquals(SWT.SEPARATOR,  items[1].getStyle());
		assertEquals("change/0",  items[2].getText());
		assertEquals("change/1",  items[3].getText());
		assertEquals("change/10",  items[4].getText());
		assertEquals("change/11",  items[5].getText());
		assertEquals("change/12",  items[6].getText());
		assertEquals("change/13",  items[7].getText());
		assertEquals("change/14",  items[8].getText());
		assertEquals("change/15",  items[9].getText());
		assertEquals("change/16",  items[10].getText());
		assertEquals("change/17",  items[11].getText());
		assertEquals("change/18",  items[12].getText());
		assertEquals("change/19",  items[13].getText());
		assertEquals("change/2",  items[14].getText());
		assertEquals("change/3",  items[15].getText());
		assertEquals("change/4",  items[16].getText());
		assertEquals("change/5",  items[17].getText());
		assertEquals("change/6",  items[18].getText());
		assertEquals("change/7",  items[19].getText());
		assertEquals("change/8",  items[20].getText());
		assertEquals("change/9",  items[21].getText());
		// "master" and "stable" didn't make it
		assertEquals(SWT.SEPARATOR,  items[22].getStyle());
		assertEquals(UIText.SwitchToMenu_OtherMenuLabel, items[23].getText());
	}

	private MenuItem[] fillMenu() {
		final MenuItem[][] items = new MenuItem[1][];
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				Menu menu = new Menu(new Shell(Display.getDefault()));
				switchToMenu.fill(menu, 0 /* index */);
				items[0] = menu.getItems();
			}
		});
		return items[0];
	}

	private static class EmptySelection implements ISelection {
		public boolean isEmpty() {
			return true;
		}
	}
}
