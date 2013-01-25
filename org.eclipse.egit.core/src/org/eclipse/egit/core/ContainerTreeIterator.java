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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator.FileEntry;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.WorkingTreeOptions;
import org.eclipse.jgit.util.FS;

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
		node = base;
		init(entries(false));
		initRootIterator(repository);
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
		node = root;
		init(entries(false));
		initRootIterator(repository);
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
		this(p, base, false);
	}

	private ContainerTreeIterator(final WorkingTreeIterator p,
			final IContainer base, final boolean hasInheritedResourceFilters) {
		super(p);
		node = base;
		init(entries(hasInheritedResourceFilters));
	}

	@Override
	public AbstractTreeIterator createSubtreeIterator(ObjectReader reader)
			throws IncorrectObjectTypeException, IOException {
		if (FileMode.TREE.equals(mode)) {
			if (current() instanceof ResourceEntry) {
				ResourceEntry resourceEntry = (ResourceEntry) current();
				return new ContainerTreeIterator(this,
						(IContainer) resourceEntry.rsrc,
						resourceEntry.hasInheritedResourceFilters);
			} else if (current() instanceof FileEntry) {
				FileEntry fileEntry = (FileEntry) current();
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				return new AdaptableFileTreeIterator(this, fileEntry.getFile(), root);
			} else {
				throw new IllegalStateException("Unknown entry type: " + current()); //$NON-NLS-1$
			}
		} else
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

	private Entry[] entries(final boolean hasInheritedResourceFilters) {
		final IResource[] resources;
		try {
			resources = node.members(IContainer.INCLUDE_HIDDEN);
		} catch (CoreException err) {
			return EOF;
		}

		List<Entry> entries = new ArrayList<Entry>(resources.length);

		boolean inheritableResourceFilter = addFilteredEntries(
				hasInheritedResourceFilters, resources, entries);

		for (IResource resource : resources)
			entries.add(new ResourceEntry(resource, inheritableResourceFilter));

		return entries.toArray(new Entry[entries.size()]);
	}

	/**
	 * Add entries for filtered resources.
	 *
	 * @param hasInheritedResourceFilters
	 *            true if resource filters of parents could be active, false
	 *            otherwise
	 * @param memberResources
	 *            the resources returned from members() that do not have to be
	 *            added as entries again
	 * @param entries
	 *            where entries should be added to
	 * @return true if we now have resource filters that are inherited, false if
	 *         there are no resource filters which are inherited.
	 */
	private boolean addFilteredEntries(
			final boolean hasInheritedResourceFilters,
			final IResource[] memberResources, final List<Entry> entries) {
		// Inheritable resource filters must be propagated.
		boolean inheritableResourceFilter = hasInheritedResourceFilters;
		IResourceFilterDescription[] filters;
		try {
			filters = node.getFilters();
		} catch (CoreException e) {
			// Should not happen, but assume we have no filters then.
			filters = new IResourceFilterDescription[] {};
		}

		if (filters.length != 0 || hasInheritedResourceFilters) {
			if (!inheritableResourceFilter) {
				for (IResourceFilterDescription filter : filters) {
					boolean inheritable = (filter.getType() & IResourceFilterDescription.INHERITABLE) != 0;
					if (inheritable)
						inheritableResourceFilter = true;
				}
			}

			Set<File> resourceEntries = new HashSet<File>();
			for (IResource resource : memberResources) {
				IPath location = resource.getLocation();
				if (location != null)
					resourceEntries.add(location.toFile());
			}

			IPath containerLocation = node.getLocation();
			if (containerLocation != null) {
				File folder = containerLocation.toFile();
				File[] children = folder.listFiles();
				for (File child : children) {
					if (resourceEntries.contains(child))
						continue;

					IPath childLocation = new Path(child.getAbsolutePath());
					IWorkspaceRoot root = node.getWorkspace().getRoot();
					IContainer container = root.getContainerForLocation(childLocation);
					// Check if the container is accessible in the workspace.
					// This may seem strange, as it was not returned from
					// members() above, but it's the case for nested projects
					// that are filtered directly.
					if (container != null && container.isAccessible())
						// Resource filters does not cross the non-member line
						// -> stop inheriting resource filter here (false)
						entries.add(new ResourceEntry(container, false));
					else
						entries.add(new FileEntry(child, FS.DETECTED));
				}
			}
		}
		return inheritableResourceFilter;
	}

	/**
	 * Wrapper for a resource in the Eclipse workspace
	 */
	static public class ResourceEntry extends Entry {
		final IResource rsrc;
		final boolean hasInheritedResourceFilters;

		private final FileMode mode;

		private long length = -1;

		ResourceEntry(final IResource f, final boolean hasInheritedResourceFilters) {
			rsrc = f;
			this.hasInheritedResourceFilters = hasInheritedResourceFilters;

			switch (f.getType()) {
			case IResource.FILE:
				File file = asFile();
				if (FS.DETECTED.supportsExecute() && file != null
						&& FS.DETECTED.canExecute(file))
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
			if (length < 0)
				if (rsrc instanceof IFile) {
					File file = asFile();
					if (file != null)
						length = file.length();
					else
						length = 0;
				} else
					length = 0;
			return length;
		}

		@Override
		public long getLastModified() {
			return rsrc.getLocalTimeStamp();
		}

		@Override
		public InputStream openInputStream() throws IOException {
			if (rsrc.getType() == IResource.FILE)
				try {
					return ((IFile) rsrc).getContents(true);
				} catch (CoreException err) {
					final IOException ioe = new IOException(err.getMessage());
					ioe.initCause(err);
					throw ioe;
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

		/**
		 * @return file of the resource or null
		 */
		private File asFile() {
			return ContainerTreeIterator.asFile(rsrc);
		}
	}

	private static File asFile(IResource resource) {
		final IPath location = resource.getLocation();
		return location != null ? location.toFile() : null;
	}

	protected byte[] idSubmodule(Entry e) {
		File nodeFile = asFile(node);
		if (nodeFile != null)
			return idSubmodule(nodeFile, e);
		return super.idSubmodule(e);
	}
}
