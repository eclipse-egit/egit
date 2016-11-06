/*******************************************************************************
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2007, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2014, Obeo
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.IResourceTree;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.errors.LockFailedException;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;

class GitMoveDeleteHook implements IMoveDeleteHook {
	private static final boolean I_AM_DONE = true;

	private static final boolean FINISH_FOR_ME = false;

	private final GitProjectData data;

	GitMoveDeleteHook(final GitProjectData d) {
		Assert.isNotNull(d);
		data = d;
	}

	@Override
	public boolean deleteFile(final IResourceTree tree, final IFile file,
			final int updateFlags, final IProgressMonitor monitor) {
		if (!org.eclipse.egit.core.Activator.autoStageDeletion()) {
			return false;
		}

		// Linked resources are not files, hence not tracked by git
		if (file.isLinked())
			return false;

		final boolean force = (updateFlags & IResource.FORCE) == IResource.FORCE;
		if (!force && !tree.isSynchronized(file, IResource.DEPTH_ZERO))
			return false;

		final RepositoryMapping map = RepositoryMapping.getMapping(file);
		if (map == null)
			return false;

		String repoRelativePath = map.getRepoRelativePath(file);
		IndexDiffCache indexDiffCache = Activator.getDefault()
				.getIndexDiffCache();
		IndexDiffCacheEntry indexDiffCacheEntry = indexDiffCache
				.getIndexDiffCacheEntry(map.getRepository());
		if (indexDiffCacheEntry == null) {
			return false;
		}
		IndexDiffData indexDiff = indexDiffCacheEntry.getIndexDiff();
		if (indexDiff != null) {
			if (indexDiff.getUntracked().contains(repoRelativePath))
				return false;
			if (indexDiff.getIgnoredNotInIndex().contains(repoRelativePath))
				return false;
		}
		if (!file.exists())
			return false;
		if (file.isDerived())
			return false;

		DirCache dirc = null;
		try {
			dirc = map.getRepository().lockDirCache();
			final int first = dirc.findEntry(repoRelativePath);
			if (first < 0) {
				dirc.unlock();
				return false;
			}

			final DirCacheBuilder edit = dirc.builder();
			if (first > 0)
				edit.keep(0, first);
			final int next = dirc.nextEntry(first);
			if (next < dirc.getEntryCount())
				edit.keep(next, dirc.getEntryCount() - next);
			if (!edit.commit())
				tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(),
						0, CoreText.MoveDeleteHook_operationError, null));
			tree.standardDeleteFile(file, updateFlags, monitor);
		} catch (LockFailedException e) {
			// FIXME The index is currently locked. This notably happens during
			// rebase operations. auto-staging deletions should be queued... and
			// the queued job will have to double-check whether the file has
			// truly been deleted or if it was only deleted to be replaced by
			// another version.
			// This hook only exists to automatically add changes to the index.
			// If the index is currently locked, do not accept the
			// responsibility of deleting the file, return false to tell the
			// workspace it can continue with the standard deletion. The user
			// will have to stage the deletion later on _if_ this was truly
			// needed, which won't happen for calls triggered by merge
			// operations from the merge strategies.
			Activator.getDefault().getLog()
					.log(new Status(IStatus.WARNING, Activator.getPluginId(),
							MessageFormat
									.format(CoreText.MoveDeleteHook_cannotAutoStageDeletion,
											file.getLocation())));
			return FINISH_FOR_ME;
		} catch (IOException e) {
			tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(), 0,
					CoreText.MoveDeleteHook_operationError, e));
		} finally {
			if (dirc != null)
				dirc.unlock();
		}
		return true;
	}

	@Override
	public boolean deleteFolder(final IResourceTree tree, final IFolder folder,
			final int updateFlags, final IProgressMonitor monitor) {
		// Deleting a GIT repository which is in use is a pretty bad idea. To
		// delete disconnect the team provider first.
		//
		if (data.isProtected(folder)) {
			return cannotModifyRepository(tree);
		} else {
			return FINISH_FOR_ME;
		}
	}

	@Override
	public boolean deleteProject(final IResourceTree tree,
			final IProject project, final int updateFlags,
			final IProgressMonitor monitor) {
		// TODO: Note that eclipse thinks folders are real, while
		// Git does not care.
		return FINISH_FOR_ME;
	}

	@Override
	public boolean moveFile(final IResourceTree tree, final IFile srcf,
			final IFile dstf, final int updateFlags,
			final IProgressMonitor monitor) {
		final boolean force = (updateFlags & IResource.FORCE) == IResource.FORCE;
		if (!force && !tree.isSynchronized(srcf, IResource.DEPTH_ZERO))
			return false;

		final RepositoryMapping srcm = RepositoryMapping.getMapping(srcf);
		if (srcm == null)
			return false;
		final RepositoryMapping dstm = RepositoryMapping.getMapping(dstf);

		DirCache sCache = null;
		try {
			sCache = srcm.getRepository().lockDirCache();
			final String sPath = srcm.getRepoRelativePath(srcf);
			final DirCacheEntry sEnt = sCache.getEntry(sPath);
			if (sEnt == null)
				return FINISH_FOR_ME;

			if (!sEnt.isMerged()) {
				tree.failed(new Status(IStatus.WARNING, Activator.getPluginId(),
						CoreText.MoveDeleteHook_unmergedFileError));
				return I_AM_DONE;
			}
			if (org.eclipse.egit.core.Activator.autoStageMoves()) {
				final DirCacheEditor sEdit = sCache.editor();
				sEdit.add(new DirCacheEditor.DeletePath(sEnt));
				if (dstm != null
						&& dstm.getRepository() == srcm.getRepository()) {
					final String dPath = srcm.getRepoRelativePath(dstf);
					sEdit.add(new DirCacheEditor.PathEdit(dPath) {

						@Override
						public void apply(final DirCacheEntry dEnt) {
							dEnt.copyMetaData(sEnt);
						}
					});
				}
				if (!sEdit.commit()) {
					tree.failed(new Status(IStatus.ERROR,
							Activator.getPluginId(), 0,
							CoreText.MoveDeleteHook_operationError, null));
				}
			}
			tree.standardMoveFile(srcf, dstf, updateFlags, monitor);
		} catch (IOException e) {
			tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(), 0,
					CoreText.MoveDeleteHook_operationError, e));
		} finally {
			if (sCache != null)
				sCache.unlock();
		}
		return I_AM_DONE;
	}

	@Override
	public boolean moveFolder(final IResourceTree tree, final IFolder srcf,
			final IFolder dstf, final int updateFlags,
			final IProgressMonitor monitor) {
		final boolean force = (updateFlags & IResource.FORCE) == IResource.FORCE;
		if (!force && !tree.isSynchronized(srcf, IResource.DEPTH_ZERO))
			return false;

		final RepositoryMapping srcm = RepositoryMapping.getMapping(srcf);
		if (srcm == null)
			return false;
		final RepositoryMapping dstm = RepositoryMapping.getMapping(dstf);

		try {
			final String sPath = srcm.getRepoRelativePath(srcf);
			if (dstm != null && dstm.getRepository() == srcm.getRepository()) {
				MoveResult result = null;
				if (org.eclipse.egit.core.Activator.autoStageMoves()) {
					final String dPath = srcm.getRepoRelativePath(dstf) + "/"; //$NON-NLS-1$
					result = moveIndexContent(dPath, srcm, sPath);
				} else {
					result = checkUnmergedPaths(srcm, sPath);
				}
				switch (result) {
				case SUCCESS:
					break;
				case FAILED:
					tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(),
							0, CoreText.MoveDeleteHook_operationError, null));
					return I_AM_DONE;
				case UNTRACKED:
					// we are not responsible for moving untracked files
					return FINISH_FOR_ME;
				case UNMERGED:
					tree.failed(new Status(IStatus.WARNING, Activator.getPluginId(),
							CoreText.MoveDeleteHook_unmergedFileInFolderError));
					return I_AM_DONE;
				}
			}
			tree.standardMoveFolder(srcf, dstf, updateFlags, monitor);
		} catch (IOException e) {
			tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(), 0,
					CoreText.MoveDeleteHook_operationError, e));
		}
		return true;
	}

	private void mapProject(final IProject source,
			final IProjectDescription description,
			final IProgressMonitor monitor, IPath gitDir) throws CoreException,
			TeamException {
		IProject destination = source.getWorkspace().getRoot()
				.getProject(description.getName());
		RepositoryMapping repositoryMapping = RepositoryMapping.create(destination, gitDir.toFile());
		if (repositoryMapping != null) {
			GitProjectData projectData = new GitProjectData(destination);
			projectData.setRepositoryMappings(Arrays.asList(repositoryMapping));
			projectData.store();
			GitProjectData.add(destination, projectData);
			RepositoryProvider.map(destination, GitProvider.class.getName());
			destination.refreshLocal(IResource.DEPTH_INFINITE,
					new SubProgressMonitor(monitor, 50));
		}
	}

	private boolean unmapProject(final IResourceTree tree, final IProject source) {
		// The Repository mapping does not support moving
		// projects, so just disconnect/reconnect for now
		try {
			RepositoryProvider.unmap(source);
		} catch (TeamException e) {
			tree.failed(new Status(IStatus.ERROR, Activator
					.getPluginId(), 0,
					CoreText.MoveDeleteHook_operationError, e));
					return true; // Do not let Eclipse complete the operation
		}
		return false;
	}

	@Override
	public boolean moveProject(final IResourceTree tree, final IProject source,
			final IProjectDescription description, final int updateFlags,
			final IProgressMonitor monitor) {
		final RepositoryMapping srcm = RepositoryMapping.getMapping(source);
		if (srcm == null)
			return false;
		IPath newLocation = null;
		if (description.getLocationURI() != null)
			newLocation = URIUtil.toPath(description.getLocationURI());
		else
			newLocation = source.getWorkspace().getRoot().getLocation()
					.append(description.getName());
		IPath sourceLocation = source.getLocation();
		// Prevent a serious error.
		if (sourceLocation.isPrefixOf(newLocation)
				&& sourceLocation.segmentCount() != newLocation.segmentCount()
				&& !"true".equals(System.getProperty("egit.assume_307140_fixed"))) { //$NON-NLS-1$//$NON-NLS-2$
			// Graceful handling of bug, i.e. refuse to destroy your code
			tree.failed(new Status(
					IStatus.ERROR,
					Activator.getPluginId(),
					0,
					"Cannot move project. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=307140 (not resolved in 3.7)", //$NON-NLS-1$
					null));
			return true;
		}
		File newLocationFile = newLocation.toFile();
		// check if new location is below the same repository
		Path workTree = new Path(srcm.getRepository().getWorkTree().getAbsolutePath());
		int matchingFirstSegments = workTree.matchingFirstSegments(newLocation);
		if (matchingFirstSegments == workTree.segmentCount()) {
			return moveProjectHelperMoveOnlyProject(tree, source, description, updateFlags,
					monitor, srcm, newLocationFile);
		} else {
			int dstAboveSrcRepo = newLocation.matchingFirstSegments(srcm.getGitDirAbsolutePath());
			int srcAboveSrcRepo = sourceLocation.matchingFirstSegments(srcm.getGitDirAbsolutePath());
			if (dstAboveSrcRepo > 0 && srcAboveSrcRepo > 0) {
				return moveProjectHelperMoveRepo(tree, source, description, updateFlags, monitor,
					srcm, newLocation, sourceLocation);
			} else {
				return FINISH_FOR_ME;
			}
		}
	}

	private boolean moveProjectHelperMoveOnlyProject(final IResourceTree tree,
			final IProject source, final IProjectDescription description,
			final int updateFlags, final IProgressMonitor monitor,
			final RepositoryMapping srcm, File newLocationFile) {
		final String sPath = srcm.getRepoRelativePath(source);
		final String absoluteWorkTreePath = srcm.getRepository().getWorkTree().getAbsolutePath();
		final String newLocationAbsolutePath = newLocationFile.getAbsolutePath();
		final String dPath;
		if (newLocationAbsolutePath.equals(absoluteWorkTreePath))
			dPath = ""; //$NON-NLS-1$
		else
			dPath = new Path(
					newLocationAbsolutePath.substring(absoluteWorkTreePath
							.length() + 1) + "/").toPortableString(); //$NON-NLS-1$
		try {
			IPath gitDir = srcm.getGitDirAbsolutePath();
			if (unmapProject(tree, source))
				return true;

			monitor.worked(100);
			MoveResult result = null;
			if (org.eclipse.egit.core.Activator.autoStageMoves()) {
				result = moveIndexContent(dPath, srcm, sPath);
			} else {
				result = checkUnmergedPaths(srcm, sPath);
			}
			switch (result) {
			case SUCCESS:
				break;
			case FAILED:
				tree.failed(new Status(IStatus.ERROR, Activator
						.getPluginId(), 0,
						CoreText.MoveDeleteHook_operationError, null));
				break;
			case UNTRACKED:
				// we are not responsible for moving untracked files
				return FINISH_FOR_ME;
			case UNMERGED:
				tree.failed(new Status(IStatus.WARNING, Activator.getPluginId(),
						CoreText.MoveDeleteHook_unmergedFileInFolderError));
				return I_AM_DONE;
			}

			tree.standardMoveProject(source, description, updateFlags,
					monitor);

			// Reconnect
			mapProject(
					source.getWorkspace().getRoot()
							.getProject(description.getName()),
					description, monitor, gitDir);
		} catch (IOException e) {
			tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(),
					0, CoreText.MoveDeleteHook_operationError, e));
		} catch (CoreException e) {
			tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(),
					0, CoreText.MoveDeleteHook_operationError, e));
		}
		return true;
	}

	private boolean moveProjectHelperMoveRepo(final IResourceTree tree, final IProject source,
			final IProjectDescription description, final int updateFlags,
			final IProgressMonitor monitor, final RepositoryMapping srcm,
			IPath newLocation, IPath sourceLocation) {
		// Moving repo, we need to unplug the previous location and
		// Re-plug it again with the new location.
		IPath gitDir = srcm.getGitDirAbsolutePath();
		if (unmapProject(tree, source)) {
			return true; // Error information in tree
		}

		monitor.worked(100);
		if (gitDir == null) {
			return true; // mapping on deleted container with relative path
		}
		IPath relativeGitDir = gitDir.makeRelativeTo(sourceLocation);
		tree.standardMoveProject(source, description, updateFlags,
				monitor);

		IPath newGitDir = newLocation.append(relativeGitDir);
		// Reconnect
		try {
			mapProject(source, description, monitor, newGitDir);
		} catch (CoreException e) {
			tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(),
					0, CoreText.MoveDeleteHook_operationError, e));
		}
		return true; // We're done with the move
	}

	enum MoveResult { SUCCESS, FAILED, UNTRACKED, UNMERGED }

	private MoveResult moveIndexContent(String dPath,
			final RepositoryMapping srcm, final String sPath) throws IOException {

		final DirCache sCache = srcm.getRepository().lockDirCache();
		try {
			final DirCacheEntry[] sEnt = sCache.getEntriesWithin(sPath);
			if (sEnt.length == 0) {
				sCache.unlock();
				return MoveResult.UNTRACKED;
			}

			final DirCacheEditor sEdit = sCache.editor();
			sEdit.add(new DirCacheEditor.DeleteTree(sPath));
			final int sPathLen = sPath.length() == 0 ? sPath.length() : sPath
					.length() + 1;
			for (final DirCacheEntry se : sEnt) {
				if (!se.isMerged())
					return MoveResult.UNMERGED;
				final String p = se.getPathString().substring(sPathLen);
				sEdit.add(new DirCacheEditor.PathEdit(dPath + p) {
					@Override
					public void apply(final DirCacheEntry dEnt) {
						dEnt.copyMetaData(se);
					}
				});
			}
			if (sEdit.commit())
				return MoveResult.SUCCESS;
			else
				return MoveResult.FAILED;
		} finally {
			if (sCache != null)
				sCache.unlock();
		}
	}

	private MoveResult checkUnmergedPaths(final RepositoryMapping srcm,
			final String sPath) throws IOException {
		final DirCache sCache = srcm.getRepository().lockDirCache();
		try {
			final DirCacheEntry[] sEnt = sCache.getEntriesWithin(sPath);
			if (sEnt.length == 0) {
				sCache.unlock();
				return MoveResult.UNTRACKED;
			}
			for (final DirCacheEntry se : sEnt) {
				if (!se.isMerged()) {
					return MoveResult.UNMERGED;
				}
			}
			return MoveResult.SUCCESS;
		} finally {
			if (sCache != null) {
				sCache.unlock();
			}
		}
	}

	private boolean cannotModifyRepository(final IResourceTree tree) {
		tree.failed(new Status(IStatus.ERROR, Activator.getPluginId(), 0,
				CoreText.MoveDeleteHook_cannotModifyFolder, null));
		return I_AM_DONE;
	}
}
