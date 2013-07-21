/*******************************************************************************
 * Copyright (C) 2013 Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * Our own implementation until SWTBot provides this, see bug 413401. Inspired
 * by SWTBotTableColumn.
 */
public class SWTBotTreeColumn extends AbstractSWTBot<TreeColumn> {

	private final Tree parent;

	public static SWTBotTreeColumn getColumn(final Tree tree, final int index) {
		TreeColumn treeColumn = UIThreadRunnable.syncExec(tree.getDisplay(),
				new WidgetResult<TreeColumn>() {
					public TreeColumn run() {
						return tree.getColumn(index);
					}
				});
		return new SWTBotTreeColumn(treeColumn);
	}

	public SWTBotTreeColumn(final TreeColumn w) throws WidgetNotFoundException {
		super(w);
		parent = UIThreadRunnable.syncExec(new WidgetResult<Tree>() {
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