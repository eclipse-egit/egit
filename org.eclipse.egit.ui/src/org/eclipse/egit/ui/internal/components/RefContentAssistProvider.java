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
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.ListRemoteOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardContainer;
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
	private List<Ref> destinationRefs;
	private List<Ref> sourceRefs;
	private final Shell shell;
	private final IWizardContainer container;
	private final Repository repo;
	private URIish uri;

	/**
	 * @param repo the repository
	 * @param uri the uri to fetch branches from
	 * @param shell the shell used to attach progress dialogs to.
	 */
	public RefContentAssistProvider(Repository repo, URIish uri, Shell shell) {
		this.repo = repo;
		this.uri = uri;
		this.shell = shell;
		this.container = null;
	}

	/**
	 * @param repo
	 *            the repository
	 * @param uri
	 *            the uri to fetch branches from
	 * @param container
	 *            used to attach progress dialogs to.
	 */
	public RefContentAssistProvider(Repository repo, URIish uri,
			IWizardContainer container) {
		this.repo = repo;
		this.uri = uri;
		this.shell = null;
		this.container = container;
	}

	/**
	 * @param source whether we want proposals for the source or the destination of the operation
	 * @param pushMode whether the operation is a push or a fetch
	 * @return a list of all refs for the given mode.
	 */
	public List<Ref> getRefsForContentAssist(boolean source, boolean pushMode) {
		if (source) {
			if (sourceRefs != null)
				return sourceRefs;
		} else if (destinationRefs != null)
			return destinationRefs;

		List<Ref> result = new ArrayList<>();
		try {
			boolean local = pushMode == source;
			if (!local) {
				final ListRemoteOperation lop = new ListRemoteOperation(repo,
						uri,
						Activator.getDefault().getPreferenceStore().getInt(
								UIPreferences.REMOTE_CONNECTION_TIMEOUT));
				IRunnableWithProgress runnable = new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {
						monitor.beginTask(
								UIText.RefSpecDialog_GettingRemoteRefsMonitorMessage,
								IProgressMonitor.UNKNOWN);
						lop.run(monitor);
						monitor.done();
					}
				};
				if (shell != null) {
					new ProgressMonitorDialog(shell).run(true, true, runnable);
				} else {
					container.run(true, true, runnable);
				}
				for (Ref ref : lop.getRemoteRefs())
					if (ref.getName().startsWith(Constants.R_HEADS)
							|| (!pushMode && ref.getName().startsWith(
									Constants.R_TAGS)))
						result.add(ref);

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
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			Activator.handleError(cause.getMessage(), cause, true);
			return result;
		} catch (Exception e) {
			Activator.handleError(e.getMessage(), e, true);
			return result;
		}
		Collections.sort(result, CommonUtils.REF_ASCENDING_COMPARATOR);
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
