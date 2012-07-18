/*******************************************************************************
 * Copyright (C) 2011, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2012, Markus Duft <markus.duft@salomon.at>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.op.ListRemoteOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.widgets.Shell;

/**
 * Contains functionality required to calculate content assist data for {@link Ref}s
 */
public class RefContentAssistProvider {

	private class FetchPushDestinationRefsJob extends Job {
		List<Ref> result;

		public FetchPushDestinationRefsJob() {
			super(UIText.RefSpecDialog_GettingRemoteRefsMonitorMessage);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Ref> tempResult;
			try {
				tempResult = new ArrayList<Ref>();
				ListRemoteOperation lop = new ListRemoteOperation(repo,
						uri,
						Activator.getDefault().getPreferenceStore().getInt(
								UIPreferences.REMOTE_CONNECTION_TIMEOUT));

				lop.run(monitor);
				for (Ref ref : lop.getRemoteRefs())
					if (ref.getName().startsWith(Constants.R_HEADS)
							|| (!pushMode && ref.getName().startsWith(
									Constants.R_TAGS)))
						tempResult.add(ref);
			} catch (InvocationTargetException e) {
				return new Status(IStatus.ERROR, Activator.getPluginId(), e.getLocalizedMessage(), e);
			} catch (InterruptedException e) {
				return Status.CANCEL_STATUS;
			}

			this.result= tempResult;
			return Status.OK_STATUS;
		}
	}

	private List<Ref> destinationRefs;
	private List<Ref> sourceRefs;
	private volatile FetchPushDestinationRefsJob fetchJob;

	private final Repository repo;
	private final URIish uri;
	private final boolean pushMode;
	private final Shell shell;

	/**
	 * @param repo the repository
	 * @param uri the uri to fetch branches from
	 * @param pushMode <code>true</code> if the operation is a push, <code>false</code> for a fetch
	 * @param shell the shell used to attach progress dialogs to.
	 */
	public RefContentAssistProvider(Repository repo, URIish uri, boolean pushMode, Shell shell) {
		this.repo = repo;
		this.uri = uri;
		this.shell = shell;
		this.pushMode = pushMode;
	}

	/**
	 * Pre-fetches remote refs for content assist in the background.
	 * <p>
	 * This method should be called if the <code>pushMode</code> passed to the
	 * constructor is != the <code>source</code> that will likely be passed to
	 * <code>getRefsForContentAssist(source)</code> later.
	 * <p>
	 * This method returns immediately.
	 *
	 * @return the scheduled Job
	 */
	public Job fetchPushDestinationRefsForContentAssist() {
		if (fetchJob != null)
			return fetchJob;

		fetchJob= new FetchPushDestinationRefsJob();
		fetchJob.schedule();
		return fetchJob;
	}

	/**
	 * @param source whether we want proposals for the source or the destination of the operation
	 * @return a list of all refs for the given mode.
	 */
	public List<Ref> getRefsForContentAssist(boolean source) {
		if (source) {
			if (sourceRefs != null)
				return sourceRefs;
		} else if (destinationRefs != null)
			return destinationRefs;

		List<Ref> result = new ArrayList<Ref>();
		try {
			boolean local = pushMode == source;
			if (!local) {
				if (fetchJob == null || fetchJob.getResult() != Status.OK_STATUS) {
					new ProgressMonitorDialog(shell).run(false, true,
							new IRunnableWithProgress() {

								public void run(IProgressMonitor monitor)
										throws InvocationTargetException,
										InterruptedException {
									monitor
											.beginTask(
													UIText.RefSpecDialog_GettingRemoteRefsMonitorMessage,
													IProgressMonitor.UNKNOWN);
									fetchPushDestinationRefsForContentAssist().join();
									monitor.done();
								}
							});
				}
				IStatus jobResult = fetchJob.getResult();
				if (jobResult != null && jobResult.matches(IStatus.ERROR)) {
					Throwable e = jobResult.getException();
					if (e instanceof RuntimeException) {
						throw (RuntimeException) e;
					} else if (e instanceof Exception) {
						Activator.handleError(e.getMessage(), e, true);
						return result;
					}
				}
				result = fetchJob.result;

			} else if (pushMode)
				for (Ref ref : repo.getRefDatabase().getRefs(
						RefDatabase.ALL).values()) {
					if (ref.getName().startsWith(Constants.R_REMOTES))
						continue;
					result.add(ref);
				}
			else
				for (Ref ref : repo.getRefDatabase().getRefs(
						Constants.R_REMOTES).values())
					result.add(ref);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			Activator.handleError(e.getMessage(), e, true);
			return result;
		}
		if (source)
			sourceRefs = result;
		else
			destinationRefs = result;
		return result;
	}

	/**
	 * @return the associated current repository.
	 */
	public Repository getRepository() {
		return repo;
	}

	/**
	 * @return the associated remote config to fetch branches from.
	 */
	public URIish getRemoteURI() {
		return uri;
	}
}
