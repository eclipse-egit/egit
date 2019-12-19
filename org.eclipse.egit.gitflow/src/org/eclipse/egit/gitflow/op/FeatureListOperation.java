/*******************************************************************************
 * Copyright (C) 2015, Max Hohenegger <eclipse@hohenegger.eu>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.gitflow.op;

import static org.eclipse.egit.gitflow.Activator.error;
import static org.eclipse.jgit.lib.Constants.DEFAULT_REMOTE_NAME;
import static org.eclipse.jgit.lib.Constants.R_REMOTES;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.op.ListRemoteOperation;
import org.eclipse.egit.gitflow.GitFlowRepository;
import org.eclipse.egit.gitflow.internal.CoreText;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.osgi.util.NLS;

/**
 * List feature branches.
 */
public final class FeatureListOperation extends GitFlowOperation {
	private static final String FILE = "file:///"; //$NON-NLS-1$

	private static final String REMOTE_ORIGIN_FEATURE_PREFIX = R_REMOTES
			+ DEFAULT_REMOTE_NAME + SEP;

	private List<Ref> result = new ArrayList<>();

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
		SubMonitor progress = SubMonitor.convert(monitor, 2);
		String uriString = FILE
				+ repository.getRepository().getDirectory().getPath();
		try {
			operationResult = fetch(progress.newChild(1), timeout);

			URIish uri = new URIish(uriString);
			ListRemoteOperation listRemoteOperation = new ListRemoteOperation(
					repository.getRepository(), uri, timeout);
			listRemoteOperation.run(progress.newChild(1));
			Collection<Ref> remoteRefs = listRemoteOperation.getRemoteRefs();
			for (Ref ref : remoteRefs) {
				if (ref.getName().startsWith(
						REMOTE_ORIGIN_FEATURE_PREFIX
								+ repository.getConfig().getFeaturePrefix())) {
					result.add(ref);
				}
			}
		} catch (URISyntaxException e) {
			String message = NLS.bind(CoreText.FeatureListOperation_unableToParse, uriString);
			throw new CoreException(error(message, e));
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
	 * @return list of feature branches
	 */
	public List<Ref> getResult() {
		return result;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return null;
	}
}
