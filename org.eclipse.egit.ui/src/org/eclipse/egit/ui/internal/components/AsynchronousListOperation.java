/*******************************************************************************
 * Copyright (C) 2018, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.ListRemoteOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.CancelableFuture;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;

/**
 * A {@link ListRemoteOperation} that is run asynchronously as a
 * {@link CancelableFuture}.
 *
 * @param <T>
 *            result type
 */
public abstract class AsynchronousListOperation<T>
		extends CancelableFuture<Collection<T>> {

	private final Repository repository;

	private final String uriText;

	private ListRemoteOperation listOp;

	/**
	 * Creates a new {@link AsynchronousListOperation}.
	 *
	 * @param repository
	 *            local repository for which to run the operation
	 * @param uriText
	 *            upstream URI
	 */
	public AsynchronousListOperation(Repository repository, String uriText) {
		this.repository = repository;
		this.uriText = uriText;
	}

	@Override
	protected String getJobTitle() {
		return MessageFormat.format(
				UIText.AsynchronousRefProposalProvider_FetchingRemoteRefsMessage,
				uriText);
	}

	@Override
	protected void prepareRun() throws InvocationTargetException {
		try {
			listOp = new ListRemoteOperation(repository, new URIish(uriText),
					Activator.getDefault().getPreferenceStore()
							.getInt(UIPreferences.REMOTE_CONNECTION_TIMEOUT));
		} catch (URISyntaxException e) {
			throw new InvocationTargetException(e);
		}
	}

	@Override
	protected void run(IProgressMonitor monitor)
			throws InterruptedException, InvocationTargetException {
		listOp.run(monitor);
		set(convert(listOp.getRemoteRefs()));
	}

	/**
	 * Transforms the {@link Ref}s obtained into the final objects. May just
	 * return the input if the generic type T is Ref, or may post-process the
	 * results as appropriate.
	 *
	 * @param refs
	 *            obtained from the upstream repository
	 * @return final result
	 */
	protected abstract Collection<T> convert(Collection<Ref> refs);

	@Override
	protected void done() {
		listOp = null;
	}

}
