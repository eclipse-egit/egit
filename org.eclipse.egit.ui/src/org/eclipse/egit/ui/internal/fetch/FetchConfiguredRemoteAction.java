package org.eclipse.egit.ui.internal.fetch;

import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Fetches from a remote as configured
 */
public class FetchConfiguredRemoteAction {

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
	public FetchConfiguredRemoteAction(Repository repository, String remoteName) {
		this.repository = repository;
		this.remoteName = remoteName;
	}

	/**
	 * Runs this action
	 * <p>
	 *
	 * @param shell
	 *            a shell may be null; if provided, a pop up will be displayed
	 *            indicating the fetch result; if Exceptions occur, these will
	 *            be displayed
	 *
	 */
	public void run(final Shell shell) {

		final RemoteConfig config;
		Exception prepareException = null;
		final Transport transport;
		try {
			config = new RemoteConfig(repository.getConfig(), remoteName);
			if (config.getURIs().isEmpty()) {
				throw new IOException(
						NLS
								.bind(
										UIText.FetchConfiguredRemoteAction_NoUrisDefinedMessage,
										remoteName));
			}
			if (config.getFetchRefSpecs().isEmpty()) {
				throw new IOException(
						NLS
								.bind(
										UIText.FetchConfiguredRemoteAction_NoSpecsDefinedMessage,
										remoteName));
			}
			transport = Transport.open(repository, config);
		} catch (URISyntaxException e) {
			prepareException = e;
			return;
		} catch (IOException e) {
			prepareException = e;
			return;
		} finally {
			if (prepareException != null)
				Activator.handleError(prepareException.getMessage(),
						prepareException, shell != null);
		}

		Job job = new Job(
				"Fetch from " + repository.getDirectory().getParentFile().getName() + " - " + remoteName) { //$NON-NLS-1$ //$NON-NLS-2$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final FetchResult result;
				Exception fetchException = null;
				try {
					result = transport.fetch(new EclipseGitProgressTransformer(
							monitor), config.getFetchRefSpecs());
				} catch (final NotSupportedException e) {
					fetchException = e;
					return new Status(IStatus.ERROR, Activator.getPluginId(),
							UIText.FetchWizard_fetchNotSupported, e);
				} catch (final TransportException e) {
					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;
					fetchException = e;
					return new Status(IStatus.ERROR, Activator.getPluginId(),
							UIText.FetchWizard_transportError, e);
				} finally {
					if (fetchException != null)
						Activator.handleError(fetchException.getMessage(),
								fetchException, shell != null);

				}
				if (shell != null) {
					PlatformUI.getWorkbench().getDisplay().asyncExec(
							new Runnable() {
								public void run() {
									Dialog dialog = new FetchResultDialog(
											shell, repository, result,
											repository.getDirectory()
													.getParentFile().getName()
													+ " - " + remoteName); //$NON-NLS-1$
									dialog.open();
								}
							});
				}
				return Status.OK_STATUS;
			}

		};

		job.setUser(true);
		job.schedule();
	}

}
