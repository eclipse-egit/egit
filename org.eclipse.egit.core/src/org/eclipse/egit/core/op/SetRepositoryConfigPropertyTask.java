/*******************************************************************************
 * Copyright (C) 2012, Stefan Lay <stefan.lay@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.op.CloneOperation.PostCloneTask;
import org.eclipse.jgit.lib.Repository;

/**
 * Sets config properties for a repository
 */
public class SetRepositoryConfigPropertyTask implements PostCloneTask {

	private final String section;
	private final String subsection;
	private final String name;
	private final String value;

	/**
	 * @param section
	 * @param subsection
	 * @param name
	 * @param value
	 */
	public SetRepositoryConfigPropertyTask(String section, String subsection, String name, String value) {
		this.section = section;
		this.subsection = subsection;
		this.name = name;
		this.value = value;
	}

	@Override
	public void execute(Repository repository, IProgressMonitor monitor)
			throws CoreException {
		try {
			repository.getConfig().setString(section, subsection, name, value);
			repository.getConfig().save();
		} catch (IOException e) {
			throw new CoreException(Activator.error(e.getMessage(), e));
		}
	}

}
