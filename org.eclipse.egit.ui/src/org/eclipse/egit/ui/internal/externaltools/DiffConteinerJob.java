/*******************************************************************************
 * Copyright (C) 2015, Andre Bossert <anb0s@anbos.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.externaltools;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.internal.merge.GitMergeEditorInput;

/**
 * @author anb0s@anbos.de
 *
 */
public class DiffConteinerJob extends Job {

	private IDiffContainer diffCont = null;

	private GitMergeEditorInput gitMergeInput = null;

	/**
	 * @param name
	 *            the Job name
	 * @param input
	 *            the input
	 */
	public DiffConteinerJob(String name, GitMergeEditorInput input) {
		super(name);
		this.gitMergeInput = input;
	}

	/**
	 * @return the diff container
	 */
	public IDiffContainer getDiffContainer() {
		return diffCont;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			diffCont = gitMergeInput.getDiffContainer(monitor);
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
		return Status.OK_STATUS;
	}

}
