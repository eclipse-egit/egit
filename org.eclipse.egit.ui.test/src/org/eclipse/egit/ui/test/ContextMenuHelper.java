/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stefan Seelmann - initial implementation posted to
 *    http://www.eclipse.org/forums/index.php?t=msg&th=11863&start=2
 *******************************************************************************/
package org.eclipse.egit.ui.test;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withMnemonic;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.results.WidgetResult;
import org.eclipse.swtbot.swt.finder.widgets.AbstractSWTBot;
import org.hamcrest.Matcher;

public class ContextMenuHelper {

	/**
	 * Clicks the context menu matching the text, executing the action
	 * synchronously (blocking until the action completes).
	 * <p>
	 * This should be used if the action requires no more UI interaction.
	 *
	 * @param bot
	 *
	 * @param texts
	 *            the text on the context menu.
	 * @throws WidgetNotFoundException
	 *             if the widget is not found.
	 * @throws SWTException
	 *             if the menu item is disabled (the root cause being an
	 *             {@link IllegalStateException})
	 */
	public static void clickContextMenuSync(final AbstractSWTBot<?> bot,
			final String... texts) {
		clickContextMenuWithRetry(bot, true, texts);
	}

	/**
	 * Clicks the context menu matching the text, executing the action
	 * asynchronously (non-blocking).
	 * <p>
	 * This should only be used when further UI interaction is part of the
	 * action, e.g. a dialog or wizard. Using
	 * {@link #clickContextMenuSync(AbstractSWTBot, String...)} is preferred in
	 * other cases.
	 *
	 * @param bot
	 *
	 * @param texts
	 *            the text on the context menu.
	 * @throws WidgetNotFoundException
	 *             if the widget is not found.
	 * @throws SWTException
	 *             if the menu item is disabled (the root cause being an
	 *             {@link IllegalStateException})
	 */
	public static void clickContextMenu(final AbstractSWTBot<?> bot,
			final String... texts) {
		clickContextMenuWithRetry(bot, false, texts);
	}

	private static void clickContextMenuWithRetry(final AbstractSWTBot<?> bot,
			final boolean sync, final String... texts) {
		int failCount = 0;
		int maxFailCount = 4;
		long sleepTime = 250;
		while (failCount <= maxFailCount)
			try {
				clickContextMenuInternal(bot, sync, texts);
				if (failCount > 0)
					System.out.println("Retrying clickContextMenu succeeded");
				break;
			} catch (WidgetNotFoundException e) {
				failCount++;
				if (failCount > maxFailCount) {
					System.out.println("clickContextMenu failed " + failCount
							+ " times");
					throw e;
				}
				System.out.println("clickContextMenu failed. Retrying in "
						+ sleepTime + " ms");
				try {
					Thread.sleep(sleepTime);
					sleepTime *= 2;
				} catch (InterruptedException e1) {
					// empty
				}
			}
	}

	private static void clickContextMenuInternal(final AbstractSWTBot<?> bot,
			final boolean sync, final String... texts) {
		// set focus on current widget and let the UI process events
		bot.setFocus();
		TestUtil.processUIEvents();

		// show
		final MenuItem menuItem = UIThreadRunnable
				.syncExec(new WidgetResult<MenuItem>() {
					@Override
					public MenuItem run() {
						MenuItem theItem = getMenuItem(bot, texts);
						if (theItem != null && !theItem.isEnabled())
							throw new IllegalStateException(
									"Menu item is diabled");

						return theItem;
					}
				});
		if (menuItem == null)
			throw new WidgetNotFoundException("Could not find menu: "
					+ Arrays.asList(texts));

		// click
		click(menuItem, sync);

		// hide
		UIThreadRunnable.syncExec(new VoidResult() {
			@Override
			public void run() {
				if (menuItem.isDisposed())
					return; // menu already gone
				hide(menuItem.getParent());
			}
		});
	}

	public static boolean contextMenuItemExists(final AbstractSWTBot<?> bot,
			final String... texts) {
		final MenuItem menuItem = UIThreadRunnable
				.syncExec(new WidgetResult<MenuItem>() {
					@Override
					public MenuItem run() {
						return getMenuItem(bot, texts);
					}
				});
		return menuItem != null;
	}

	private static MenuItem getMenuItem(final AbstractSWTBot<?> bot,
			final String... texts) {
		MenuItem theItem = null;
		// try three times to get the menu item
		for (int i = 0; i < 3; i++) {
			Control control = (Control) bot.widget;
			// for dynamic menus, we need to issue this event
			control.notifyListeners(SWT.MenuDetect, new Event());
			Menu menu = control.getMenu();
			for (String text : texts) {
				Matcher<?> matcher = allOf(instanceOf(MenuItem.class),
						withMnemonic(text));
				theItem = show(menu, matcher);
				if (theItem != null)
					menu = theItem.getMenu();
				else {
					hide(menu);
					break;
				}
			}
			if (theItem != null)
				break;
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		return theItem;
	}

	/**
	 * Checks if the context menu matching the text is enabled
	 *
	 * @param bot
	 *
	 * @param texts
	 *            the text on the context menu.
	 * @return true if the context menu is enabled
	 * @throws WidgetNotFoundException
	 *             if the widget is not found.
	 */
	public static boolean isContextMenuItemEnabled(final AbstractSWTBot<?> bot,
			final String... texts) {

		final AtomicBoolean enabled = new AtomicBoolean(false);
		// show
		final MenuItem menuItem = UIThreadRunnable
				.syncExec(new WidgetResult<MenuItem>() {
					@Override
					public MenuItem run() {
						MenuItem theItem = getMenuItem(bot, texts);
						if (theItem != null && theItem.isEnabled())
							enabled.set(true);
						return theItem;
					}
				});
		if (menuItem == null)
			throw new WidgetNotFoundException("Could not find menu: "
					+ Arrays.asList(texts));
		// hide
		UIThreadRunnable.syncExec(new VoidResult() {
			@Override
			public void run() {
				if (menuItem.isDisposed())
					return; // menu already gone
				hide(menuItem.getParent());
			}
		});
		return enabled.get();
	}

	private static MenuItem show(final Menu menu, final Matcher<?> matcher) {
		if (menu != null) {
			menu.notifyListeners(SWT.Show, new Event());
			MenuItem[] items = menu.getItems();
			for (final MenuItem menuItem : items)
				if (matcher.matches(menuItem))
					return menuItem;
			menu.notifyListeners(SWT.Hide, new Event());
		}
		return null;
	}

	private static void click(final MenuItem menuItem, boolean sync) {
		final Event event = new Event();
		event.time = (int) System.currentTimeMillis();
		event.widget = menuItem;
		event.display = menuItem.getDisplay();
		event.type = SWT.Selection;

		VoidResult toExecute = new VoidResult() {
			@Override
			public void run() {
				menuItem.notifyListeners(SWT.Selection, event);
			}
		};
		if (sync)
			UIThreadRunnable.syncExec(menuItem.getDisplay(), toExecute);
		else
			UIThreadRunnable.asyncExec(menuItem.getDisplay(), toExecute);
	}

	private static void hide(final Menu menu) {
		menu.notifyListeners(SWT.Hide, new Event());
		if (menu.getParentMenu() != null)
			hide(menu.getParentMenu());
	}
}
