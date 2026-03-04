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
package org.eclipse.egit.ui.internal.push;

import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.notifications.AbstractNotificationPopup;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * A non-blocking, auto-dismissing notification shown after a successful push
 * operation when the user has enabled the notification preference. Uses
 * Eclipse's {@link AbstractNotificationPopup} for proper fade animation and
 * positioning.
 */
public class PushResultNotification extends AbstractNotificationPopup {

	private static final long DELAY_CLOSE_MS = 5000;

	private final Repository repository;

	private final PushOperationResult result;

	private final String destination;

	private final boolean showConfigure;

	private final @NonNull PushMode pushMode;

	/**
	 * @param parent
	 *            the parent shell (used to obtain the Display)
	 * @param repository
	 *            the repository that was pushed
	 * @param result
	 *            the push result
	 * @param destination
	 *            display string identifying the remote destination
	 * @param showConfigure
	 *            whether to show the configure button in the detail dialog
	 * @param pushMode
	 *            the push mode used
	 */
	public PushResultNotification(Shell parent, Repository repository,
			PushOperationResult result, String destination,
			boolean showConfigure, @NonNull PushMode pushMode) {
		super(parent.getDisplay());
		setParentShell(parent);
		this.repository = repository;
		this.result = result;
		this.destination = destination;
		this.showConfigure = showConfigure;
		this.pushMode = pushMode;
		setDelayClose(DELAY_CLOSE_MS);
		setFadingEnabled(true);
	}

	@Override
	protected String getPopupShellTitle() {
		return NLS.bind(UIText.PushResultNotification_Title, destination);
	}

	@Override
	protected void createContentArea(Composite parent) {
		GridLayoutFactory.fillDefaults().margins(4, 0).applyTo(parent);

		Label body = new Label(parent, SWT.WRAP);
		int updated = 0;
		int rejected = 0;
		boolean upToDate = true;
		boolean hasErrors = false;

		for (URIish uri : result.getURIs()) {
			String errorMessage = result.getErrorMessage(uri);
			if (errorMessage != null && !errorMessage.isEmpty()) {
				hasErrors = true;
				continue;
			}
			for (RemoteRefUpdate update : result.getPushResult(uri)
					.getRemoteUpdates()) {
				Status status = update.getStatus();
				if (status == Status.OK) {
					updated++;
					upToDate = false;
				} else if (isRejected(status)) {
					rejected++;
					upToDate = false;
				} else if (status != Status.UP_TO_DATE) {
					upToDate = false;
				}
			}
		}

		if (rejected > 0 || hasErrors) {
			body.setText(UIText.PushResultNotification_PushRejected);
		} else if (upToDate) {
			body.setText(UIText.PushResultNotification_UpToDate);
		} else {
			body.setText(NLS.bind(UIText.PushResultNotification_RefsUpdated,
					Integer.valueOf(updated)));
		}

		GridDataFactory.fillDefaults().grab(true, false).hint(280, SWT.DEFAULT)
				.applyTo(body);

		Link details = new Link(parent, SWT.NONE);
		details.setText(
				"<a>" + UIText.PushResultNotification_Details + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		details.addListener(SWT.Selection, e -> {
			close();
			Shell activeShell = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getShell();
			PushResultDialog dialog = new PushResultDialog(activeShell,
					repository, result, destination, false, pushMode);
			dialog.showConfigureButton(showConfigure);
			dialog.open();
		});
		GridDataFactory.fillDefaults().applyTo(details);
	}

	private boolean isRejected(Status status) {
		switch (status) {
		case REJECTED_NODELETE:
		case REJECTED_NONFASTFORWARD:
		case REJECTED_OTHER_REASON:
		case REJECTED_REMOTE_CHANGED:
			return true;
		default:
			return false;
		}
	}
}
