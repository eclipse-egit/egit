/*******************************************************************************
 * Copyright (C) 2007, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.op.DisconnectProviderOperation;
import org.eclipse.egit.core.op.IEGitOperation;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator;

/**
 *	Action to disassociate a project from its Git repository.
 *
 *  @see DisconnectProviderOperation
 */
public class Disconnect extends AbstractResourceOperationAction {
	protected IEGitOperation createOperation(final List<IResource> sel) {
		List<IProject> projects = new ArrayList<IProject>();
		for(IResource resource:sel)
			projects.add((IProject) resource);
		return new DisconnectProviderOperation(projects);
	}

	protected void postOperation() {
		GitLightweightDecorator.refresh();
	}

	@Override
	protected String getJobName() {
		return UIText.Disconnect_disconnect;
	}
}
