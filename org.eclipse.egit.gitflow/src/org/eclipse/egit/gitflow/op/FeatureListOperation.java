/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.op.ListRemoteOperation;

import static org.eclipse.egit.gitflow.Activator.error;

import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.internal.CoreText;

import static org.eclipse.jgit.lib.Constants.*;

import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.URIish;

/**
 * List feature branches.
 */
@SuppressWarnings("restriction")
public final class FeatureListOperation extends GitFlowOperation {
	private static final String FILE = "file:///"; //$NON-NLS-1$

	private static final String REMOTE_ORIGIN_FEATURE_PREFIX = R_REMOTES
			+ DEFAULT_REMOTE_NAME + SEP;

	private List<Ref> result = new ArrayList<Ref>();

	private int timeout;

	private FetchResult operationResult;

	/**
	 * @param repository
	 * @param timeout
	 */
	public FeatureListOperation(GitFlowRepository repository, int timeout) {
		super(repository);
		this.timeout = timeout;
	}

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		String uriString = FILE
				+ repository.getRepository().getDirectory().getPath();
		try {
			operationResult = fetch(monitor);

			URIish uri = new URIish(uriString);
			ListRemoteOperation listRemoteOperation = new ListRemoteOperation(
					repository.getRepository(), uri, timeout);
			listRemoteOperation.run(monitor);
			Collection<Ref> remoteRefs = listRemoteOperation.getRemoteRefs();
			for (Ref ref : remoteRefs) {
				if (ref.getName().startsWith(
						REMOTE_ORIGIN_FEATURE_PREFIX
								+ repository.getFeaturePrefix())) {
					result.add(ref);
				}
			}
		} catch (URISyntaxException e) {
			throw new CoreException(error(
					CoreText.FeatureListOperation_unableToParse + uriString, e));
		} catch (InvocationTargetException e) {
			Throwable targetException = e.getTargetException();
			throw new CoreException(error(targetException.getMessage(),
					targetException));
		} catch (InterruptedException e) {
			throw new CoreException(error(e.getMessage(), e));
		}
	}

	/**
	 * @return result set after operation was executed
	 */
	public FetchResult getOperationResult() {
		return operationResult;
	}

	/**
	 * @return list of features
	 */
	public List<Ref> getResult() {
		return result;
	}
}
