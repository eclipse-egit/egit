/*******************************************************************************
 * Copyright (C) 2012, Matthias Sohn <matthias.sohn@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.internal.op;

import java.io.IOException;
import java.text.ParseException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.internal.Activator;
import org.eclipse.egit.core.internal.EclipseGitProgressTransformer;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.internal.storage.file.GC;

/**
 * Operation to garbage collect a git repository
 */
public class GarbageCollectOperation implements IEGitOperation {

	private Repository repository;

	/**
	 * @param repository the repository to garbage collect
	 */
	public GarbageCollectOperation(Repository repository) {
		this.repository = repository;
	}

	/**
	 * Execute garbage collection
	 */
	public void execute(IProgressMonitor monitor) throws CoreException {
		GC gc = new GC((FileRepository) repository);
		gc.setProgressMonitor(new EclipseGitProgressTransformer(
				monitor));
		try {
			gc.gc();
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.getPluginId(), e.getMessage(), e));
		} catch (ParseException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					Activator.getPluginId(), e.getMessage(), e));
		}
	}

	public ISchedulingRule getSchedulingRule() {
		return RuleUtil.getRule(repository);
	}

}
