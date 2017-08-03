/*******************************************************************************
 * Copyright (C) 2011, Bernard Leach <leachbj@bouncycastle.org>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 * Copyright (C) 2014, Axel Richard <axel.richard@obeo.fr>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Andre Bossert <anb0s@anbos.de> - Cleaning up the DecoratableResourceAdapter
 *******************************************************************************/
package org.eclipse.egit.ui.internal.staging;

import java.util.EnumSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.internal.decorators.IDecoratableResource;
import org.eclipse.egit.ui.internal.decorators.IProblemDecoratable;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.lib.Repository;


/**
 * A staged/unstaged entry in the table
 */
public class StagingEntry extends PlatformObject
		implements IProblemDecoratable, IDecoratableResource {

	/**
	 * State of the node
	 */
	public static enum State {
		/** in index, not in HEAD */
		ADDED(EnumSet.of(Action.UNSTAGE)),

		/** changed in index compared to HEAD */
		CHANGED(EnumSet.of(Action.REPLACE_WITH_HEAD_REVISION, Action.UNSTAGE)),

		/** removed from index, but in HEAD */
		REMOVED(EnumSet.of(Action.REPLACE_WITH_HEAD_REVISION, Action.UNSTAGE)),

		/** in index (unchanged), missing from working tree */
		MISSING(EnumSet.of(Action.REPLACE_WITH_HEAD_REVISION, Action.STAGE)),

		/** changed in index compared to HEAD, missing from working tree */
		MISSING_AND_CHANGED(EnumSet.of(Action.REPLACE_WITH_FILE_IN_GIT_INDEX,
				Action.REPLACE_WITH_HEAD_REVISION, Action.STAGE)),

		/** modified in working tree compared to index */
		MODIFIED(EnumSet.of(Action.REPLACE_WITH_HEAD_REVISION, Action.STAGE,
				Action.ASSUME_UNCHANGED, Action.UNTRACK)),

		/** modified in working tree compared to index, changed in index compared to HEAD */
		MODIFIED_AND_CHANGED(EnumSet.of(Action.REPLACE_WITH_FILE_IN_GIT_INDEX, Action.REPLACE_WITH_HEAD_REVISION, Action.STAGE)),

		/** modified in working tree compared to index, added in index (not in HEAD) */
		MODIFIED_AND_ADDED(EnumSet.of(Action.REPLACE_WITH_FILE_IN_GIT_INDEX, Action.STAGE)),

		/** not ignored, and not in the index */
		UNTRACKED(EnumSet.of(Action.STAGE, Action.DELETE, Action.IGNORE)),

		/** in conflict */
		CONFLICTING(EnumSet.of(Action.REPLACE_WITH_FILE_IN_GIT_INDEX,
				Action.REPLACE_WITH_HEAD_REVISION, Action.STAGE,
				Action.LAUNCH_MERGE_TOOL, Action.REPLACE_WITH_OURS_THEIRS_MENU));

		private final Set<Action> availableActions;

		private State(Set<Action> availableActions) {
			this.availableActions = availableActions;
		}

		/**
		 * @return set of available actions for the current state
		 */
		public Set<Action> getAvailableActions() {
			return availableActions;
		}
	}

	/**
	 * Possible actions available on a staging entry.
	 */
	enum Action {
		REPLACE_WITH_FILE_IN_GIT_INDEX,
		REPLACE_WITH_HEAD_REVISION,
		STAGE,
		UNSTAGE,
		DELETE,
		IGNORE,
		LAUNCH_MERGE_TOOL,
		REPLACE_WITH_OURS_THEIRS_MENU,
		ASSUME_UNCHANGED,
		UNTRACK
	}

	private final Repository repository;
	private final State state;
	private final String path;

	private boolean fileLoaded;

	private IFile file;

	private String name;

	private StagingFolderEntry parent;

	private boolean submodule;

	private boolean symlink;

	/**
	 * @param repository
	 *            repository for this entry
	 * @param state
	 * @param path
	 *            repo-relative path for this entry
	 */
	public StagingEntry(Repository repository, State state, String path) {
		this.repository = repository;
		this.state = state;
		this.path = path;
	}

	/**
	 * @param submodule
	 */
	public void setSubmodule(final boolean submodule) {
		this.submodule = submodule;
	}

	/**
	 * @return true if submodule, false otherwise
	 */
	public boolean isSubmodule() {
		return submodule;
	}

	/**
	 * @param symlink
	 */
	public void setSymlink(boolean symlink) {
		this.symlink = symlink;
	}

	/**
	 * @return true if symlink, false otherwise
	 */
	public boolean isSymlink() {
		return symlink;
	}

	/**
	 * @return the repo-relative path for this file
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return the repo-relative path of the parent
	 */
	public IPath getParentPath() {
		return new Path(path).removeLastSegments(1);
	}

	/**
	 * @return the repository for this node
	 */
	public Repository getRepository() {
		return repository;
	}

	/**
	 * @return the state for this node
	 */
	public State getState() {
		return state;
	}

	Set<Action> getAvailableActions() {
		return state.getAvailableActions();
	}

	/**
	 * @return the file corresponding to the entry, if it exists in the
	 *         workspace, null otherwise.
	 */
	public IFile getFile() {
		if (!fileLoaded) {
			fileLoaded = true;
			file = ResourceUtil.getFileForLocation(repository, path, false);
		}
		return file;
	}

	/**
	 * @return the location (path) of the entry
	 */
	@NonNull
	public IPath getLocation() {
		IPath absolutePath = new Path(repository.getWorkTree().getAbsolutePath()).append(path);
		return absolutePath;
	}

	/**
	 * @return parent StagingFolderEntry
	 */
	public StagingFolderEntry getParent() {
		return parent;
	}

	/**
	 * @param parent
	 *            StagingFolderEntry
	 */
	public void setParent(StagingFolderEntry parent) {
		this.parent = parent;
	}

	@Override
	public int getProblemSeverity() {
		IFile f = getFile();
		if (f == null)
			return SEVERITY_NONE;

		try {
			return f.findMaxProblemSeverity(IMarker.PROBLEM, true,
					IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			return SEVERITY_NONE;
		}
	}

	@Override
	public int getType() {
		return IResource.FILE;
	}

	@Override
	public String getName() {
		if (name == null) {
			IPath parsed = Path.fromOSString(getPath());
			name = parsed.lastSegment();
		}
		return name;
	}

	@Override
	public String getRepositoryName() {
		return null;
	}

	@Override
	public String getBranch() {
		return null;
	}

	@Override
	public String getBranchStatus() {
		return null;
	}

	@Override
	public String getCommitMessage() {
		return null;
	}

	@Override
	public boolean isTracked() {
		return state != State.UNTRACKED;
	}

	@Override
	public boolean isIgnored() {
		return false;
	}

	@Override
	public boolean isDirty() {
		return state == State.MODIFIED || state == State.MODIFIED_AND_CHANGED
				|| state == State.MODIFIED_AND_ADDED;
	}

	@Override
	public boolean isMissing() {
		return state == State.MISSING || state == State.MISSING_AND_CHANGED;
	}

	@Override
	public boolean hasUnstagedChanges() {
		return !isTracked() || isDirty() || isMissing() || hasConflicts();
	}

	@Override
	public StagingState getStagingState() {
		switch (state) {
		case ADDED:
			return StagingState.ADDED;
		case CHANGED:
			return StagingState.MODIFIED;
		case REMOVED:
			return StagingState.REMOVED;
		case MISSING:
		case MISSING_AND_CHANGED:
			return StagingState.REMOVED;
		default:
			return StagingState.NOT_STAGED;
		}
	}

	@Override
	public boolean isStaged() {
		return getStagingState() != StagingState.NOT_STAGED;
	}

	@Override
	public boolean hasConflicts() {
		return state == State.CONFLICTING;
	}

	@Override
	public boolean isAssumeUnchanged() {
		return false;
	}

	@Override
	public String toString() {
		return "StagingEntry[" + state + ' ' + path + ']'; //$NON-NLS-1$
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StagingEntry other = (StagingEntry) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (state != other.state)
			return false;
		return true;
	}

	@Override
	public boolean isRepositoryContainer() {
		return false;
	}
}
