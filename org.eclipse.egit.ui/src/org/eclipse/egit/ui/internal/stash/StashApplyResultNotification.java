/*******************************************************************************
 * Copyright (C) 2026 vogella GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.stash;

import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.notifications.NotificationPopup;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * A non-blocking, auto-dismissing notification shown after a successful stash
 * apply operation.
 */
public class StashApplyResultNotification {

	/**
	 * Opens a notification popup for a successful stash apply.
	 *
	 * @param shell
	 *            the parent shell
	 * @param repository
	 *            the repository
	 * @param commit
	 *            the stashed commit that was applied
	 */
	public static void open(Shell shell, Repository repository,
			RevCommit commit) {
		NotificationPopup.forShell(shell)
				.title(UIText.StashApplyResultNotification_Title, true)
				.content(parent -> {
					GridLayoutFactory.fillDefaults().margins(4, 0)
							.applyTo(parent);
					Label body = new Label(parent, SWT.WRAP);
					body.setText(NLS.bind(
							UIText.StashApplyResultNotification_Message,
							Utils.getShortObjectId(commit),
							RepositoryUtil.INSTANCE
									.getRepositoryName(repository)));
					GridDataFactory.fillDefaults().grab(true, false)
							.hint(280, SWT.DEFAULT).applyTo(body);
					return parent;
				})
				.open();
	}
}
