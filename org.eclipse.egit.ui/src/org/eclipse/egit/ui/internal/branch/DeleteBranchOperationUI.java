/*******************************************************************************
 * Copyright (c) 2022 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.branch;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.core.op.DeleteBranchOperation;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.dialogs.UnmergedBranchDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.PlatformUI;

/**
 * UI wrapper for deleting branches in repositories.
 */
public class DeleteBranchOperationUI {

	/**
	 * Deletes the given branches in the given repositories, prompting the user
	 * whether unmerged branches should be deleted, too.
	 *
	 * @param toDelete
	 *            branches per repository to delete
	 * @throws InvocationTargetException
	 *             if the operation failed
	 */
	public static void deleteBranches(
			Map<Repository, ? extends Collection<Ref>> toDelete)
			throws InvocationTargetException {
		// Make sure the map is modifiable.
		Map<Repository, List<Ref>> map = new HashMap<>();
		int[] total = { 0 };
		toDelete.forEach((repo, list) -> {
			if (!list.isEmpty()) {
				map.put(repo, new ArrayList<>(list));
				total[0] += list.size();
			}
		});
		if (total[0] > 0) {
			deleteBranchesImpl(map, total[0]);
		}
	}

	/**
	 * Deletes the given branches in the given repository, prompting the user
	 * whether unmerged branches should be deleted, too.
	 *
	 * @param repository
	 *            the branches are in
	 * @param toDelete
	 *            branches to delete
	 * @throws InvocationTargetException
	 *             if the operation failed
	 */
	public static void deleteBranches(Repository repository,
			Collection<Ref> toDelete) throws InvocationTargetException {
		if (toDelete.isEmpty()) {
			return;
		}
		Map<Repository, List<Ref>> map = new HashMap<>();
		map.put(repository, new ArrayList<>(toDelete));
		deleteBranchesImpl(map, toDelete.size());
	}

	private static void deleteBranchesImpl(
			Map<Repository, List<Ref>> toDelete, int numberOfRefs)
			throws InvocationTargetException {
		List<Ref> unmergedRefs = new ArrayList<>();
		IShellProvider shell = PlatformUI.getWorkbench()
				.getModalDialogShellProvider();
		try {
			new ProgressMonitorDialog(shell.getShell()).run(true, true,
					monitor -> deleteBranches(toDelete, unmergedRefs, false,
							numberOfRefs, monitor));
		} catch (InterruptedException e) {
			// Cancelled
			return;
		}
		if (unmergedRefs.isEmpty()) {
			return;
		}
		// TODO: UnmergedBranchDialog should accept refs grouped by repository
		MessageDialog messageDialog = new UnmergedBranchDialog(shell.getShell(),
				unmergedRefs);
		if (messageDialog.open() != Window.OK) {
			return;
		}
		int remainingRefs = numberOfRefs - unmergedRefs.size();
		try {
			new ProgressMonitorDialog(shell.getShell()).run(true, true,
					monitor -> deleteBranches(toDelete, null, true,
							remainingRefs, monitor));
		} catch (InterruptedException e) {
			// Ignore
		}
	}

	private static void deleteBranches(Map<Repository, List<Ref>> refs,
			List<Ref> unmerged, boolean forceDeletionOfUnmergedBranches,
			int numberOfRefs, IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		try {
			SubMonitor progress = SubMonitor.convert(monitor,
					UIText.DeleteBranchCommand_DeletingBranchesProgress,
					numberOfRefs);
			Iterator<Entry<Repository, List<Ref>>> entries = refs.entrySet()
					.iterator();
			while (entries.hasNext()) {
				Entry<Repository, List<Ref>> entry = entries.next();
				List<Ref> toDelete = entry.getValue();
				try {
					deleteBranches(entry.getKey(), toDelete, unmerged,
							forceDeletionOfUnmergedBranches,
							progress.newChild(toDelete.size()));
					if (toDelete.isEmpty()) {
						entries.remove();
					}
				} catch (OperationCanceledException e) {
					throw new InterruptedException();
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		} finally {
			monitor.done();
		}
	}

	private static void deleteBranches(Repository repo, List<Ref> refs,
			List<Ref> unmerged, boolean forceDeletionOfUnmergedBranches,
			SubMonitor progress) throws CoreException {
		Iterator<Ref> toDelete = refs.iterator();
		while (toDelete.hasNext()) {
			if (progress.isCanceled()) {
				throw new OperationCanceledException();
			}
			Ref ref = toDelete.next();
			int result = deleteBranch(repo, ref,
					forceDeletionOfUnmergedBranches);
			if (result == DeleteBranchOperation.REJECTED_CURRENT) {
				throw new CoreException(Activator.createErrorStatus(
						UIText.DeleteBranchCommand_CannotDeleteCheckedOutBranch,
						null));
			} else if (result == DeleteBranchOperation.REJECTED_UNMERGED) {
				if (unmerged != null) {
					unmerged.add(ref);
				}
			} else {
				toDelete.remove();
				progress.worked(1);
			}
		}
	}

	private static int deleteBranch(final Repository repo, final Ref ref,
			boolean force) throws CoreException {
		DeleteBranchOperation op = new DeleteBranchOperation(repo, ref, force);
		op.execute(null);
		return op.getStatus();
	}
}
