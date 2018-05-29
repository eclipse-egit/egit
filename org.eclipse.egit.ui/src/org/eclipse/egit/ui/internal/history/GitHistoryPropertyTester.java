/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.File;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.ui.internal.expressions.AbstractPropertyTester;
import org.eclipse.team.ui.history.IHistoryPage;
import org.eclipse.team.ui.history.IHistoryView;

/**
 * A {@link PropertyTester} specific to the git history page. Offers the
 * following tests:
 * <dl>
 * <dt>IHistoryView.isSingleFileHistory</dt>
 * <dd><code>true</code> if the active part is the history view, and the active
 * page is the git history page, and the page is filtered to a single file. The
 * expected <code>value</code> "resource" matches only if that single file is an
 * {@link IResource}, and likewise the <code>value</code> "file" matches only if
 * the single file is a {@link File}. Otherwise the test is <code>true</code> in
 * either case.</dd>
 * </dl>
 */
public class GitHistoryPropertyTester extends AbstractPropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		if ("isSingleFileHistory".equals(property)) { //$NON-NLS-1$
			GitHistoryPage page = getGitHistoryPage(receiver);
			if (page == null) {
				return false;
			}
			Object single = page.getInputInternal().getSingleFile();
			if (expectedValue instanceof String) {
				if (expectedValue.equals("resource")) { //$NON-NLS-1$
					return single instanceof IResource;
				} else if (expectedValue.equals("file")) { //$NON-NLS-1$
					return single instanceof File;
				}
			} else {
				return computeResult(expectedValue, single != null);
			}
		}
		return false;
	}

	private GitHistoryPage getGitHistoryPage(Object receiver) {
		if (!(receiver instanceof IHistoryView)) {
			return null;
		}
		IHistoryPage page = ((IHistoryView) receiver).getHistoryPage();
		if (page instanceof GitHistoryPage) {
			return (GitHistoryPage) page;
		}
		return null;
	}

}
