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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.notifications.AbstractNotificationPopup;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.PushResult;
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

	private static final Pattern URL_PATTERN = Pattern
			.compile("(https?://\\S+)"); //$NON-NLS-1$

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

		String pullRequestUrl = extractPullRequestUrl();
		if (pullRequestUrl != null) {
			Link prLink = new Link(parent, SWT.NONE);
			prLink.setText("<a>" //$NON-NLS-1$
					+ UIText.PushResultNotification_CreatePullRequest
					+ "</a>"); //$NON-NLS-1$
			prLink.addListener(SWT.Selection, e -> {
				try {
					URL url = URI.create(pullRequestUrl).toURL();
					PlatformUI.getWorkbench().getBrowserSupport()
							.getExternalBrowser()
							.openURL(url);
				} catch (Exception ex) {
					Activator.logError(ex.getMessage(), ex);
				}
			});
			GridDataFactory.fillDefaults().applyTo(prLink);
		}

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

	/**
	 * Extracts a pull/merge request creation URL from the push result
	 * messages. Remote servers like GitHub, GitLab, and Bitbucket include
	 * such URLs in the push response when a new branch is pushed.
	 *
	 * @return the URL string, or {@code null} if none was found
	 */
	private String extractPullRequestUrl() {
		for (URIish uri : result.getURIs()) {
			PushResult pushResult = result.getPushResult(uri);
			if (pushResult == null) {
				continue;
			}
			String messages = pushResult.getMessages();
			if (messages == null || messages.isEmpty()) {
				continue;
			}
			// Look for URLs that indicate PR/MR creation
			Matcher matcher = URL_PATTERN.matcher(messages);
			while (matcher.find()) {
				String url = matcher.group(1);
				if (isPullRequestUrl(url)) {
					return url;
				}
			}
		}
		return null;
	}

	private boolean isPullRequestUrl(String url) {
		try {
			URI.create(url).toURL();
		} catch (MalformedURLException e) {
			return false;
		}
		String lower = url.toLowerCase();
		return lower.contains("/pull/new/") //$NON-NLS-1$
				|| lower.contains("/merge_requests/new") //$NON-NLS-1$
				|| lower.contains("/pull-requests/create") //$NON-NLS-1$
				|| lower.contains("/-/merge_requests/create"); //$NON-NLS-1$
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
