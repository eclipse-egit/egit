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
package org.eclipse.egit.ui.internal.fetch;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.notifications.AbstractNotificationPopup;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.FetchResult;

/**
 * A non-blocking, auto-dismissing notification shown after a successful fetch
 * operation when the user has enabled the notification preference. Uses Eclipse's
 * {@link AbstractNotificationPopup} for proper fade animation and positioning.
 */
public class FetchResultNotification extends AbstractNotificationPopup {

	private static final long DELAY_CLOSE_MS = 5000;

	private final Repository repository;

	private final FetchResult result;

	private final String source;

	/**
	 * @param parent
	 *            the parent shell (used to obtain the Display)
	 * @param repository
	 *            the repository that was fetched
	 * @param result
	 *            the fetch result
	 * @param source
	 *            display string identifying the remote source
	 */
	public FetchResultNotification(Shell parent, Repository repository,
			FetchResult result, String source) {
		super(parent.getDisplay());
		this.repository = repository;
		this.result = result;
		this.source = source;
		setDelayClose(DELAY_CLOSE_MS);
		setFadingEnabled(true);
	}

	@Override
	protected void initializeBounds() {
		super.initializeBounds(); // computes and applies the correct size
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		if (window != null) {
			Rectangle wb = window.getShell().getBounds();
			Rectangle nb = getShell().getBounds();
			getShell().setLocation(wb.x + wb.width - nb.width,
					wb.y + wb.height - nb.height);
		}
	}

	@Override
	protected String getPopupShellTitle() {
		return NLS.bind(UIText.FetchResultNotification_Title, source);
	}

	@Override
	protected void createContentArea(Composite parent) {
		GridLayoutFactory.fillDefaults().margins(4, 0).applyTo(parent);

		Label body = new Label(parent, SWT.WRAP);
		int updateCount = result.getTrackingRefUpdates().size();
		if (updateCount == 0) {
			body.setText(UIText.FetchResultNotification_UpToDate);
		} else {
			body.setText(NLS.bind(UIText.FetchResultNotification_RefsUpdated,
					Integer.valueOf(updateCount)));
		}
		GridDataFactory.fillDefaults().grab(true, false).hint(280, SWT.DEFAULT)
				.applyTo(body);

		Link details = new Link(parent, SWT.NONE);
		details.setText(
				"<a>" + UIText.FetchResultNotification_Details + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		details.addListener(SWT.Selection, e -> {
			close();
			Shell activeShell = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getShell();
			FetchResultDialog dialog = new FetchResultDialog(activeShell,
					repository, result, source);
			dialog.open();
		});
		GridDataFactory.fillDefaults().applyTo(details);
	}
}
