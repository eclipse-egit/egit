/*******************************************************************************
 * Copyright (C) 2009, Alex Blewitt <alex.blewitt@gmail.com>
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2012, 2013 Robin Stocker <robin@nibor.org>
 * Copyright (C) 2015, Stephan Hackstedt <stephan.hackstedt@googlemail.com>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.IteratorService;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
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

	@Override
	public void execute(IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor,
				CoreText.IgnoreOperation_taskName, 3);
		try {
			Map<IPath, Collection<String>> perFolder = getFolderMap(
					progress.newChild(1));
			if (perFolder == null) {
				return;
			}
			perFolder = pruneFolderMap(perFolder, progress.newChild(1));
			if (perFolder == null) {
				return;
			}

			updateGitIgnores(perFolder, progress.newChild(1));
		} catch (CoreException e) {
			throw e;
		} catch (Exception e) {
			throw new CoreException(Activator.error(
					CoreText.IgnoreOperation_error, e));
		}
	}

	/**
	 * @return true if a gitignore file outside the workspace was changed. In
	 *         this case the caller may need to perform manual UI refreshes
	 *         because there was no ResourceChanged event.
	 */
	public boolean isGitignoreOutsideWSChanged() {
		return gitignoreOutsideWSChanged;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return schedulingRule;
	}

	private Map<IPath, Collection<String>> getFolderMap(
			IProgressMonitor monitor) {
		SubMonitor progress = SubMonitor.convert(monitor, paths.size());
		Map<IPath, Collection<String>> result = new HashMap<>();
		for (IPath path : paths) {
			if (progress.isCanceled()) {
				return null;
			}
			IPath parent = path.removeLastSegments(1);
			Collection<String> values = result.computeIfAbsent(parent,
					key -> new LinkedHashSet<>());
			values.add(path.lastSegment());
			progress.worked(1);
		}
		return result;
	}

	private Map<IPath, Collection<String>> pruneFolderMap(
			Map<IPath, Collection<String>> perFolder, IProgressMonitor monitor)
			throws IOException {
		SubMonitor progress = SubMonitor.convert(monitor, perFolder.size());
		for (Map.Entry<IPath, Collection<String>> entry : perFolder
				.entrySet()) {
			pruneFolder(entry.getKey(), entry.getValue(), progress.newChild(1));
			if (progress.isCanceled()) {
				return null;
			}
		}
		return perFolder;
	}

	private void pruneFolder(IPath folder, Collection<String> files,
			IProgressMonitor monitor)
			throws IOException {
		if (files.isEmpty()) {
			return;
		}
		Repository repository = Activator.getDefault().getRepositoryCache()
				.getRepository(folder);
		if (repository == null || repository.isBare()) {
			files.clear();
			return;
		}
		WorkingTreeIterator treeIterator = IteratorService
				.createInitialIterator(repository);
		if (treeIterator == null) {
			files.clear();
			return;
		}
		IPath repoRelativePath = folder.makeRelativeTo(
				new Path(repository.getWorkTree().getAbsolutePath()));
		if (repoRelativePath.equals(folder)) {
			files.clear();
			return;
		}
		Collection<String> repoRelative = new HashSet<>(files.size());
		for (String file : files) {
			repoRelative.add(repoRelativePath.append(file).toPortableString());
		}
		// Remove all entries,then re-add only those found during the tree walk
		// that are not ignored already
		files.clear();
		try (TreeWalk walk = new TreeWalk(repository)) {
			walk.addTree(treeIterator);
			walk.setFilter(PathFilterGroup.createFromStrings(repoRelative));
			while (walk.next()) {
				if (monitor.isCanceled()) {
					return;
				}
				WorkingTreeIterator workingTreeIterator = walk.getTree(0,
						WorkingTreeIterator.class);
				if (repoRelative.contains(walk.getPathString())) {
					if (!workingTreeIterator.isEntryIgnored()) {
						files.add(walk.getNameString());
					}
				} else if (workingTreeIterator.getEntryFileMode()
						.equals(FileMode.TREE)) {
					walk.enterSubtree();
				}
			}
		}
	}

	private void updateGitIgnores(Map<IPath, Collection<String>> perFolder,
			IProgressMonitor monitor) throws CoreException, IOException {
		SubMonitor progress = SubMonitor.convert(monitor, perFolder.size() * 2);
		for (Map.Entry<IPath, Collection<String>> entry : perFolder
				.entrySet()) {
			if (progress.isCanceled()) {
				return;
			}
			IContainer container = ResourceUtil
					.getContainerForLocation(entry.getKey(), false);
			if (container instanceof IWorkspaceRoot) {
				container = null;
			}
			Collection<String> files = entry.getValue();
			if (files.isEmpty()) {
				progress.worked(1);
				continue;
			}
			StringBuilder builder = new StringBuilder();
			for (String file : files) {
				builder.append('/').append(file);
				boolean isDirectory = false;
				IResource resource = container != null
						? container.findMember(file) : null;
				if (resource != null) {
					isDirectory = resource.getType() != IResource.FILE;
				} else {
					isDirectory = entry.getKey().append(file).toFile()
							.isDirectory();
				}
				if (isDirectory) {
					builder.append('/');
				}
				builder.append('\n');
			}
			progress.worked(1);
			if (progress.isCanceled()) {
				return;
			}
			addToGitIgnore(container, entry.getKey(), builder.toString(),
					progress.newChild(1));
		}
	}

	private void addToGitIgnore(IContainer container, IPath parent,
			String entry, IProgressMonitor monitor)
			throws CoreException, IOException {
		SubMonitor progress = SubMonitor.convert(monitor, 1);
		if (container == null) {
			// .gitignore outside of workspace
			Repository repository = Activator.getDefault().getRepositoryCache()
					.getRepository(parent);
			if (repository == null || repository.isBare()) {
				String message = NLS.bind(
						CoreText.IgnoreOperation_parentOutsideRepo,
						parent.toOSString(), null);
				IStatus status = Activator.error(message, null);
				throw new CoreException(status);
			}
			IPath gitIgnorePath = parent.append(Constants.GITIGNORE_FILENAME);
			IPath repoPath = new Path(repository.getWorkTree()
					.getAbsolutePath());
			if (!repoPath.isPrefixOf(gitIgnorePath)) {
				String message = NLS.bind(
						CoreText.IgnoreOperation_parentOutsideRepo,
						parent.toOSString(), repoPath.toOSString());
				IStatus status = Activator.error(message, null);
				throw new CoreException(status);
			}
			File gitIgnore = new File(gitIgnorePath.toOSString());
			updateGitIgnore(gitIgnore, entry);
			// no resource change event when updating .gitignore outside
			// workspace => trigger manual decorator refresh
			gitignoreOutsideWSChanged = true;
		} else {
			// .gitignore is in workspace
			IFile gitignore = container.getFile(new Path(
					Constants.GITIGNORE_FILENAME));
			String toAdd = getEntry(gitignore.getLocation().toFile(), entry);
			ByteArrayInputStream entryBytes = asStream(toAdd);
			if (gitignore.exists()) {
				gitignore.appendContents(entryBytes, true, true,
						progress.newChild(1));
			} else {
				gitignore.create(entryBytes, true, progress.newChild(1));
			}
		}
	}

	private boolean prependNewline(File file) throws IOException {
		boolean prepend = false;
		long length = file.length();
		if (length > 0) {
			try (RandomAccessFile raf = new RandomAccessFile(file, "r")) { //$NON-NLS-1$
				// Read the last byte and see if it is a newline
				ByteBuffer buffer = ByteBuffer.allocate(1);
				FileChannel channel = raf.getChannel();
				channel.position(length - 1);
				if (channel.read(buffer) > 0) {
					buffer.rewind();
					prepend = buffer.get() != '\n';
				}
			}
		}
		return prepend;
	}

	private String getEntry(File file, String entry) throws IOException {
		return prependNewline(file) ? '\n' + entry : entry;
	}

	private void updateGitIgnore(File gitIgnore, String entry)
			throws CoreException {
		try {
			String ignoreLine = entry;
			if (!gitIgnore.exists()) {
				if (!gitIgnore.createNewFile()) {
					String error = NLS.bind(
							CoreText.IgnoreOperation_creatingFailed,
							gitIgnore.getAbsolutePath());
					throw new CoreException(Activator.error(error, null));
				}
			} else {
				ignoreLine = getEntry(gitIgnore, ignoreLine);
			}

			try (FileOutputStream os = new FileOutputStream(gitIgnore, true)) {
				os.write(ignoreLine.getBytes(StandardCharsets.UTF_8));
			}
		} catch (IOException e) {
			String error = NLS.bind(CoreText.IgnoreOperation_updatingFailed,
					gitIgnore.getAbsolutePath());
			throw new CoreException(Activator.error(error, e));
		}
	}

	private ByteArrayInputStream asStream(String entry) {
		return new ByteArrayInputStream(
				entry.getBytes(StandardCharsets.UTF_8));
	}

	private ISchedulingRule calcSchedulingRule() {
		return RuleUtil.getRuleForContainers(paths);
	}
}
