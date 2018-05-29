/*******************************************************************************
 * Copyright (C) 2013 Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.test;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.WidgetResult;
import org.eclipse.swtbot.swt.finder.widgets.AbstractSWTBot;

/**
 * Like SWTBotTableColumn, but for a tree. This implementation was contributed
 * to SWTBot, see bug 413401. Due to SWTBot depending on hamcrest 1.3 and
 * hamcrest 1.3 not being available in an Orbit R-build, we can't use it yet.
 * TODO: But as soon as we update to a newer SWTBot, this should be removed.
 */
public class SWTBotTreeColumn extends AbstractSWTBot<TreeColumn> {

	private final Tree parent;

	public static SWTBotTreeColumn getColumn(final Tree tree, final int index) {
		TreeColumn treeColumn = UIThreadRunnable.syncExec(tree.getDisplay(),
				new WidgetResult<TreeColumn>() {
					@Override
					public TreeColumn run() {
						return tree.getColumn(index);
					}
				});
		return new SWTBotTreeColumn(treeColumn);
	}

	public SWTBotTreeColumn(final TreeColumn w) throws WidgetNotFoundException {
		super(w);
		parent = UIThreadRunnable.syncExec(new WidgetResult<Tree>() {
			@Override
			public Tree run() {
				return w.getParent();
			}
		});
	}

	/**
	 * Clicks the item.
	 */
	@Override
	public SWTBotTreeColumn click() {
		waitForEnabled();
		notify(SWT.Selection);
		notify(SWT.MouseUp, createMouseEvent(0, 0, 1, SWT.BUTTON1, 1), parent);
		return this;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
