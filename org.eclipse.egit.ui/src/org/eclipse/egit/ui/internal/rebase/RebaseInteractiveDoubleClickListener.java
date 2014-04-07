/*******************************************************************************
 * Copyright (C) 2014, Konrad Kügler <swamblumat-eclipsebugs@yahoo.de> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.rebase;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.egit.core.internal.rebase.RebaseInteractivePlan.PlanElement;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

final class RebaseInteractiveDoubleClickListener implements
		IDoubleClickListener {

	private final RebaseInteractiveView rebaseInteractiveView;

	RebaseInteractiveDoubleClickListener(
			RebaseInteractiveView rebaseInteractiveView) {
		this.rebaseInteractiveView = rebaseInteractiveView;
	}

	public void doubleClick(DoubleClickEvent event) {
		PlanElement element = (PlanElement) ((IStructuredSelection) event
				.getSelection()).getFirstElement();
		if (element == null)
			return;

		RepositoryCommit commit = loadCommit(element.getCommit());
		if (commit != null)
			CommitEditor.openQuiet(commit);
	}

	private RepositoryCommit loadCommit(AbbreviatedObjectId abbreviatedObjectId) {
		if (abbreviatedObjectId != null) {
			RevWalk walk = new RevWalk(this.rebaseInteractiveView.currentRepository);
			try {
				Collection<ObjectId> resolved = walk.getObjectReader()
						.resolve(abbreviatedObjectId);
				if (resolved.size() == 1) {
					RevCommit commit = walk.parseCommit(resolved
							.iterator().next());
					return new RepositoryCommit(this.rebaseInteractiveView.currentRepository, commit);
				}
			} catch (IOException e) {
				return null;
			} finally {
				walk.release();
			}
		}
		return null;
	}
}