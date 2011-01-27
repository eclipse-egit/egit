/*******************************************************************************
 * Copyright (C) 2008, Google Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.WorkingTreeOptions;
import org.eclipse.jgit.util.FS;
import org.eclipse.team.core.Team;

/**
 * Adapts an Eclipse {@link IContainer} for use in a <code>TreeWalk</code>.
 * <p>
 * This iterator converts an Eclipse IContainer object into something that a
 * TreeWalk instance can iterate over in parallel with any other Git tree data
 * structure, such as another working directory tree from outside of the
 * workspace or a stored tree from a Repository object database.
 * <p>
 * Modification times provided by this iterator are obtained from the cache
 * Eclipse uses to track external resource modification. This can be faster, but
 * requires the user refresh their workspace when external modifications take
 * place. This is not really a concern as it is common practice to need to do a
 * workspace refresh after externally modifying a file.
 *
 * @see org.eclipse.jgit.treewalk.TreeWalk
 */
public class ContainerTreeIterator extends WorkingTreeIterator {

	private static final QualifiedName FILE_LENGTH_KEY = new QualifiedName(
			Activator.getPluginId(), "fileLength"); //$NON-NLS-1$

	private static String computePrefix(final IContainer base) {
		final RepositoryMapping rm = RepositoryMapping.getMapping(base);
		if (rm == null)
			throw new IllegalArgumentException(
					"Not in a Git project: " + base);  //$NON-NLS-1$
		return rm.getRepoRelativePath(base);
	}

	private final IContainer node;

	/**
	 * Construct a new iterator from a container in the workspace.
	 * <p>
	 * The iterator will support traversal over the named container, but only if
	 * it is contained within a project which has the Git repository provider
	 * connected and this resource is mapped into a Git repository. During the
	 * iteration the paths will be automatically generated to match the proper
	 * repository paths for this container's children.
	 *
	 * @param repository
	 *            repository the given base is mapped to
	 * @param base
	 *            the part of the workspace the iterator will walk over.
	 */
	public ContainerTreeIterator(final Repository repository, final IContainer base) {
		super(computePrefix(base), repository.getConfig().get(WorkingTreeOptions.KEY));
		registerRCL();
		node = base;
		init(entries());
	}

	/**
	 * Construct a new iterator from the workspace root.
	 * <p>
	 * The iterator will support traversal over workspace projects that have
	 * a Git repository provider connected and is mapped into a Git repository.
	 * During the iteration the paths will be automatically generated to match
	 * the proper repository paths for this container's children.
	 *
	 * @param repository
	 *            repository the given base is mapped to
	 * @param root
	 *            the workspace root to walk over.
	 */
	public ContainerTreeIterator(final Repository repository, final IWorkspaceRoot root) {
		super("", repository.getConfig().get(WorkingTreeOptions.KEY));  //$NON-NLS-1$
		registerRCL();
		node = root;
		init(entries());
	}

	/**
	 * Construct a new iterator from a container in the workspace, with a given
	 * parent iterator.
	 * <p>
	 * The iterator will support traversal over the named container, but only if
	 * it is contained within a project which has the Git repository provider
	 * connected and this resource is mapped into a Git repository. During the
	 * iteration the paths will be automatically generated to match the proper
	 * repository paths for this container's children.
	 *
	 * @param p
	 *            the parent iterator we were created from.
	 * @param base
	 *            the part of the workspace the iterator will walk over.
	 */
	public ContainerTreeIterator(final WorkingTreeIterator p,
			final IContainer base) {
		super(p);
		registerRCL();
		node = base;
		init(entries());
	}

	@Override
	public AbstractTreeIterator createSubtreeIterator(ObjectReader reader)
			throws IncorrectObjectTypeException, IOException {
		if (FileMode.TREE.equals(mode))
			return new ContainerTreeIterator(this,
					(IContainer) ((ResourceEntry) current()).rsrc);
		else
			throw new IncorrectObjectTypeException(ObjectId.zeroId(),
					Constants.TYPE_TREE);
	}

	/**
	 * Get the ResourceEntry for the current entry.
	 *
	 * @return the current entry
	 */
	public ResourceEntry getResourceEntry() {
		return (ResourceEntry) current();
	}

	private Entry[] entries() {
		final IResource[] all;
		try {
			all = node.members(IContainer.INCLUDE_HIDDEN);
		} catch (CoreException err) {
			return EOF;
		}

		final Entry[] r = new Entry[all.length];
		for (int i = 0; i < r.length; i++)
			r[i] = new ResourceEntry(all[i]);
		return r;
	}

	@Override
	public boolean isEntryIgnored() throws IOException {
		return super.isEntryIgnored() ||
			isEntryIgnoredByTeamProvider(getResourceEntry().getResource());
	}

	private boolean isEntryIgnoredByTeamProvider(IResource resource) {
		if (resource.getType() == IResource.ROOT)
			return false;
		if (Team.isIgnoredHint(resource))
			return true;
		return isEntryIgnoredByTeamProvider(resource.getParent());

	}

	/**
	 * Wrapper for a resource in the Eclipse workspace
	 */
	static public class ResourceEntry extends Entry {
		final IResource rsrc;

		private final FileMode mode;

		private long length = -1;

		ResourceEntry(final IResource f) {
			rsrc = f;

			switch (f.getType()) {
			case IResource.FILE:
				if (FS.DETECTED.supportsExecute()
						&& FS.DETECTED.canExecute(asFile()))
					mode = FileMode.EXECUTABLE_FILE;
				else
					mode = FileMode.REGULAR_FILE;
				break;
			case IResource.PROJECT:
			case IResource.FOLDER: {
				final IContainer c = (IContainer) f;
				if (c.findMember(Constants.DOT_GIT) != null)
					mode = FileMode.GITLINK;
				else
					mode = FileMode.TREE;
				break;
			}
			default:
				mode = FileMode.MISSING;
				break;
			}
		}

		@Override
		public FileMode getMode() {
			return mode;
		}

		@Override
		public String getName() {
			if (rsrc.getType() == IResource.PROJECT)
				return rsrc.getLocation().lastSegment();
			else
				return rsrc.getName();
		}

		@Override
		public long getLength() {
			if (length < 0) {
				if (rsrc.getType() == IResource.FILE) {
					Long fileLength = null;
					try {
						fileLength = (Long) rsrc
								.getSessionProperty(FILE_LENGTH_KEY);
					} catch (CoreException e) {
						// Ignore
					}
					if (fileLength != null) {
						length = fileLength.longValue();
					} else {
						length = asFile().length();
						try {
							rsrc.setSessionProperty(FILE_LENGTH_KEY, new Long(
									length));
						} catch (CoreException e) {
							// Ignore
						}
					}
				} else {
					length = 0;
				}
			}
			return length;
		}

		@Override
		public long getLastModified() {
			return rsrc.getLocalTimeStamp();
		}

		@Override
		public InputStream openInputStream() throws IOException {
			if (rsrc.getType() == IResource.FILE) {
				try {
					return ((IFile) rsrc).getContents(true);
				} catch (CoreException err) {
					final IOException ioe = new IOException(err.getMessage());
					ioe.initCause(err);
					throw ioe;
				}
			}
			throw new IOException("Not a regular file: " + rsrc);  //$NON-NLS-1$
		}

		/**
		 * Get the underlying resource of this entry.
		 *
		 * @return the underlying resource
		 */
		public IResource getResource() {
			return rsrc;
		}

		private File asFile() {
			return ((IFile) rsrc).getLocation().toFile();
		}
	}

	private static final IResourceChangeListener rcl = new RCL();

	private static class RCL implements IResourceChangeListener {

		// Remove 'fileLength' property for changed resources
		public void resourceChanged(IResourceChangeEvent event) {
			try {
				event.getDelta().accept(new IResourceDeltaVisitor() {

					public boolean visit(IResourceDelta delta)
							throws CoreException {
						final IResource resource = delta.getResource();
						if (resource != null
								&& resource.getType() == IResource.FILE)
							try {
								resource.setSessionProperty(FILE_LENGTH_KEY,
										null);
							} catch (CoreException e) {
								// Ignore
							}
						return true;
					}
				}, true);
			} catch (CoreException e) {
				// Ignore
			}
		}
	}

	private static void registerRCL() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(rcl,
				IResourceChangeEvent.POST_CHANGE);
	}
}
