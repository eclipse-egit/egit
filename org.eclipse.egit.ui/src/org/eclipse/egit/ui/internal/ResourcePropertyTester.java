/*******************************************************************************
 * Copyright (c) 2011 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mathias Kinzler (SAP AG) - initial implementation
 *    Dariusz Luksza <dariusz@luksza.org> - add 'isSafe' implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal;

import static org.eclipse.jgit.lib.RepositoryState.SAFE;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.internal.trace.GitTraceLocation;

/**
 * Resource-based property tester
 */
public class ResourcePropertyTester extends PropertyTester {

	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		boolean value = internalTest(receiver, property, args, expectedValue);
		boolean trace = GitTraceLocation.PROPERTIESTESTER.isActive();
		if (trace)
			GitTraceLocation
					.getTrace()
					.trace(GitTraceLocation.PROPERTIESTESTER.getLocation(),
							"prop "	+ property + " of " + receiver + " = " + value + ", expected = " + expectedValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		return value;
	}

	private boolean internalTest(Object receiver, String property, Object[] args,
			Object expectedValue) {
		if (!(receiver instanceof IResource))
			return false;
		IResource res = (IResource) receiver;
		if ("isShared".equals(property)) { //$NON-NLS-1$
			RepositoryMapping mapping = RepositoryMapping.getMapping(res
					.getProject());
			return mapping != null && mapping.getRepository() != null;
		} else if ("isSafe".equals(property)) { //$NON-NLS-1$
			RepositoryMapping mapping = RepositoryMapping.getMapping(res
					.getProject());
			return mapping != null
					&& SAFE == mapping.getRepository().getRepositoryState();
		} else if ("isContainer".equals(property)) { //$NON-NLS-1$
			int type = res.getType();
			return type == IResource.FOLDER || type == IResource.PROJECT;
		}
		return false;
	}

}
