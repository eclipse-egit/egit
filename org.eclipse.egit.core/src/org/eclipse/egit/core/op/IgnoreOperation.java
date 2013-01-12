/*******************************************************************************
 * Copyright (C) 2009, Alex Blewitt <alex.blewitt@gmail.com>
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.osgi.util.NLS;

/**
 * IgnoreOperation adds resources to a .gitignore file
 *
 */
public class IgnoreOperation implements IEGitOperation {

	private final Collection<IPath> paths;

	private boolean gitignoreOutsideWSChanged;

	private ISchedulingRule schedulingRule;

	/**
	 * construct an IgnoreOperation
	 *
	 * @param paths
	 * @since 2.2
	 */
	public IgnoreOperation(Collection<IPath> paths) {
		this.paths = paths;
		gitignoreOutsideWSChanged = false;
		schedulingRule = calcSchedulingRule();
	}

	/**
	 * @param resources
	 * @deprecated use {@link #IgnoreOperation(Collection)}
	 */
	@Deprecated
	public IgnoreOperation(IResource[] resources) {
		paths = new ArrayList<IPath>(resources.length);
		for (IResource resource : resources) {
			IPath location = resource.getLocation();
			if (location != null)
				paths.add(location);
		}
	}

	public void execute(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(CoreText.IgnoreOperation_taskName, paths.size());
		try {
			for (IPath path : paths) {
				if (monitor.isCanceled())
					break;
				// TODO This is pretty inefficient; multiple ignores in
				// the same directory cause multiple writes.

				// NB This does the same thing in
				// DecoratableResourceAdapter, but neither currently
				// consult .gitignore
				if (!isIgnored(path))
					addIgnore(monitor, path);
				monitor.worked(1);
			}
			monitor.done();
		} catch (CoreException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(Activator.error(
					CoreText.IgnoreOperation_error, e));
		}
	}

	private boolean isIgnored(IPath path) throws IOException {
		RepositoryMapping mapping = RepositoryMapping.getMapping(path);
		Repository repository = mapping.getRepository();
		String repoRelativePath = mapping.getRepoRelativePath(path);
		TreeWalk walk = new TreeWalk(repository);
		walk.addTree(new FileTreeIterator(repository));
		walk.setFilter(PathFilter.create(repoRelativePath));
		while (walk.next()) {
			WorkingTreeIterator workingTreeIterator = walk.getTree(0,
					WorkingTreeIterator.class);
			if (walk.getPathString().equals(repoRelativePath)) {
				return workingTreeIterator.isEntryIgnored();
			}
			if (workingTreeIterator.getEntryFileMode().equals(FileMode.TREE))
				walk.enterSubtree();
		}
		return false;
	}

	/**
	 * @return true if a gitignore file outside the workspace was changed. In
	 *         this case the caller may need to perform manual UI refreshes
	 *         because there was no ResourceChanged event.
	 */
	public boolean isGitignoreOutsideWSChanged() {
		return gitignoreOutsideWSChanged;
	}

	public ISchedulingRule getSchedulingRule() {
		return schedulingRule;
	}

	private void addIgnore(IProgressMonitor monitor, IPath path)
			throws UnsupportedEncodingException, CoreException, IOException {
		IPath parent = path.removeLastSegments(1);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IContainer container = root.getContainerForLocation(parent);

		String entry = "/" + path.lastSegment() + "\n"; //$NON-NLS-1$  //$NON-NLS-2$

		if (container == null || container instanceof IWorkspaceRoot) {
			Repository repository = RepositoryMapping.getMapping(
					path).getRepository();
			// .gitignore is not accessible as resource
			IPath gitIgnorePath = parent.append(Constants.GITIGNORE_FILENAME);
			IPath repoPath = new Path(repository.getWorkTree()
					.getAbsolutePath());
			if (!repoPath.isPrefixOf(gitIgnorePath)) {
				String message = NLS.bind(
						CoreText.IgnoreOperation_parentOutsideRepo,
						path.toOSString(), repoPath.toOSString());
				IStatus status = Activator.error(message, null);
				throw new CoreException(status);
			}
			File gitIgnore = new File(gitIgnorePath.toOSString());
			updateGitIgnore(gitIgnore, entry);
			// no resource change event when updating .gitignore outside
			// workspace => trigger manual decorator refresh
			gitignoreOutsideWSChanged = true;
		} else {
			IFile gitignore = container.getFile(new Path(
					Constants.GITIGNORE_FILENAME));
			entry = getEntry(gitignore.getLocation().toFile(), entry);
			IProgressMonitor subMonitor = new SubProgressMonitor(monitor, 1);
			ByteArrayInputStream entryBytes = asStream(entry);
			if (gitignore.exists())
				gitignore.appendContents(entryBytes, true, true, subMonitor);
			else
				gitignore.create(entryBytes, true, subMonitor);
		}
	}

	private boolean prependNewline(File file) throws IOException {
		boolean prepend = false;
		long length = file.length();
		if (length > 0) {
			RandomAccessFile raf = new RandomAccessFile(file, "r"); //$NON-NLS-1$
			try {
				// Read the last byte and see if it is a newline
				ByteBuffer buffer = ByteBuffer.allocate(1);
				FileChannel channel = raf.getChannel();
				channel.position(length - 1);
				if (channel.read(buffer) > 0) {
					buffer.rewind();
					prepend = buffer.get() != '\n';
				}
			} finally {
				raf.close();
			}
		}
		return prepend;
	}

	private String getEntry(File file, String entry) throws IOException {
		return prependNewline(file) ? "\n" + entry : entry; //$NON-NLS-1$
	}

	private void updateGitIgnore(File gitIgnore, String entry)
			throws CoreException {
		try {
			String ignoreLine = entry;
			if (!gitIgnore.exists())
				if (!gitIgnore.createNewFile()) {
					String error = NLS.bind(
							CoreText.IgnoreOperation_creatingFailed,
							gitIgnore.getAbsolutePath());
					throw new CoreException(Activator.error(error, null));
				}
			else
				ignoreLine = getEntry(gitIgnore, ignoreLine);

			FileOutputStream os = new FileOutputStream(gitIgnore, true);
			try {
				os.write(ignoreLine.getBytes());
			} finally {
				os.close();
			}
		} catch (IOException e) {
			String error = NLS.bind(CoreText.IgnoreOperation_updatingFailed,
					gitIgnore.getAbsolutePath());
			throw new CoreException(Activator.error(error, e));
		}
	}

	private ByteArrayInputStream asStream(String entry)
			throws UnsupportedEncodingException {
		return new ByteArrayInputStream(
				entry.getBytes(Constants.CHARACTER_ENCODING));
	}

	private ISchedulingRule calcSchedulingRule() {
		return RuleUtil.getRuleForContainers(paths);
	}
}
