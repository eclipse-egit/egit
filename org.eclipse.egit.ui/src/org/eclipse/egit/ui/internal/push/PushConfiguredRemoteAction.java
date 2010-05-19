/*******************************************************************************
 * Copyright (c) 2010 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.push;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.PushOperation;
import org.eclipse.egit.core.op.PushOperationResult;
import org.eclipse.egit.core.op.PushOperationSpecification;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Push to a remote as configured
 */
public class PushConfiguredRemoteAction {

	private final Repository repository;

	private final String remoteName;

	/**
	 * The default constructor
	 *
	 * @param repository
	 *            a {@link Repository}
	 * @param remoteName
	 *            the name of a remote as configured for fetching
	 */
	public PushConfiguredRemoteAction(Repository repository, String remoteName) {
		this.repository = repository;
		this.remoteName = remoteName;
	}

	/**
	 * Runs this action
	 * <p>
	 *
	 * @param shell
	 *            a shell may be null; if provided, a pop up will be displayed
	 *            indicating the fetch result
	 * @param dryRun
	 *
	 */
	public void run(final Shell shell, boolean dryRun) {
		RemoteConfig config;
		PushOperationSpecification spec;
		Exception pushException = null;
		final PushOperation op;
		try {
			config = new RemoteConfig(repository.getConfig(), remoteName);
			if (config.getPushURIs().isEmpty()) {
				throw new IOException(NLS.bind(
						UIText.PushConfiguredRemoteAction_NoUrisMessage,
						remoteName));
			}
			final Collection<RefSpec> pushSpecs = config.getPushRefSpecs();
			if (pushSpecs.isEmpty()) {
				throw new IOException(NLS.bind(
						UIText.PushConfiguredRemoteAction_NoSpecDefined,
						remoteName));
			}
			final Collection<RemoteRefUpdate> updates = Transport
					.findRemoteRefUpdatesFor(repository, pushSpecs, null);
			if (updates.isEmpty()) {
				throw new IOException(
						NLS.bind(
								UIText.PushConfiguredRemoteAction_NoUpdatesFoundMessage,
								remoteName));
			}

			spec = new PushOperationSpecification();
			for (final URIish uri : config.getPushURIs())
				spec.addURIRefUpdates(uri,
						ConfirmationPage.copyUpdates(updates));

			op = new PushOperation(repository, spec, dryRun, config);

		} catch (URISyntaxException e) {
			pushException = e;
			return;
		} catch (IOException e) {
			pushException = e;
			return;
		} finally {
			if (pushException != null)
				Activator.handleError(pushException.getMessage(),
						pushException, shell != null);
		}

		final Job job = new Job(
				"Push to " + repository.getDirectory().getParentFile().getName() + " - " + remoteName) { //$NON-NLS-1$ //$NON-NLS-2$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					op.run(monitor);
					final PushOperationResult res = op.getOperationResult();
					if (shell != null) {
						PlatformUI.getWorkbench().getDisplay().asyncExec(
								new Runnable() {
									public void run() {
										final Dialog dialog = new PushResultDialog(
												shell, repository, res,
												repository.getDirectory()
														.getParentFile()
														.getName()
														+ " - " + remoteName); //$NON-NLS-1$
										dialog.open();
									}
								});
					}
					return Status.OK_STATUS;
				} catch (InvocationTargetException e) {
					return new Status(IStatus.ERROR, Activator.getPluginId(), e
							.getCause().getMessage(), e);
				}
			}

		};

		job.setUser(true);
		job.schedule();
	}
}
