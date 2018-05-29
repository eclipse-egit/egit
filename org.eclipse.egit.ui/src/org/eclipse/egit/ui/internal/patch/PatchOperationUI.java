/*******************************************************************************
 * Copyright (C) 2011-2012, Tomasz Zarna <Tomasz.Zarna@pl.ibm.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.patch;

import static org.eclipse.jgit.lib.Repository.stripWorkDir;

import java.util.Collection;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.op.CreatePatchOperation;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.history.GitCreatePatchWizard;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

/**
 * The UI wrapper for {@link CreatePatchOperation}
 */
public class PatchOperationUI {
	private IWorkbenchPart part;

	private Repository repository;

	private RevCommit commit;

	private Collection<? extends IResource> resources;

	private PatchOperationUI(IWorkbenchPart part, Repository repo) {
		this.part = part;
		this.repository = repo;
	}

	private PatchOperationUI(IWorkbenchPart part, Repository repo,
			RevCommit commit) {
		this(part, repo);
		this.commit = commit;
	}

	private PatchOperationUI(IWorkbenchPart part, Repository repo,
			Collection<? extends IResource> resources) {
		this(part, repo);
		this.resources = resources;
	}

	/**
	 * Create an operation for creating a patch for a specific commit.
	 *
	 * @param part
	 *            the part
	 * @param commit
	 *            a commit
	 * @param repo
	 *            the repository
	 * @return the {@link PatchOperationUI}
	 */
	public static PatchOperationUI createPatch(IWorkbenchPart part,
			RevCommit commit, Repository repo) {
		return new PatchOperationUI(part, repo, commit);
	}

	/**
	 * Create an operation for creating a patch for change made relative to the
	 * index.
	 *
	 * @param part
	 *            the part
	 * @param repo
	 *            the repository
	 * @param resources
	 *            collection of {@link IResource}s
	 * @return the {@link PatchOperationUI}
	 */
	public static PatchOperationUI createPatch(IWorkbenchPart part,
			Repository repo, Collection<? extends IResource> resources) {
		return new PatchOperationUI(null, repo, resources);
	}

	/**
	 * Starts the operation asynchronously
	 */
	public void start() {
		if (commit != null) {
			GitCreatePatchWizard.run(getShell(), commit, repository, null);
			return;
		} else

		if (isWorkingTreeClean()) {
			MessageDialog.openInformation(getShell(),
					UIText.GitCreatePatchAction_cannotCreatePatch,
					UIText.GitCreatePatchAction_workingTreeClean);
			return;
		}
		GitCreatePatchWizard.run(getShell(), null, repository, resources);
	}

	private boolean isWorkingTreeClean() {
		IndexDiffCache diffCache = org.eclipse.egit.core.Activator.getDefault()
				.getIndexDiffCache();
		if (diffCache != null) {
			IndexDiffCacheEntry diffCacheEntry = diffCache
					.getIndexDiffCacheEntry(
					repository);
			if (diffCacheEntry == null) {
				return true;
			}
			IndexDiffData diffData = diffCacheEntry.getIndexDiff();
			if (diffData != null) {
				Set<String> modified = diffData.getModified();
				Set<String> untracked = diffData.getUntracked();
				Set<String> missing = diffData.getMissing();
				for (IResource resource : resources) {
					String repoRelativePath = makeRepoRelative(resource);
					if (containsPrefix(modified, repoRelativePath))
						return false;
					if (containsPrefix(untracked, repoRelativePath))
						return false;
					if (containsPrefix(missing, repoRelativePath))
						return false;
				}
			}
		}
		return true;
	}

	private String makeRepoRelative(IResource res) {
		return stripWorkDir(repository.getWorkTree(), res.getLocation()
				.toFile());
	}

	private boolean containsPrefix(Set<String> collection, String prefix) {
		for (String path : collection)
			if (path.startsWith(prefix))
				return true;
		return false;
	}

	private Shell getShell() {
		return getShell(part);
	}

	private static Shell getShell(IWorkbenchPart part) {
		if (part != null)
			return part.getSite().getShell();
		return PlatformUI.getWorkbench().getDisplay().getActiveShell();
	}
}
