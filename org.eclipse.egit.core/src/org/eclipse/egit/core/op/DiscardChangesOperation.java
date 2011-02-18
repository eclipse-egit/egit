/*******************************************************************************
 * Copyright (C) 2010, Jens Baumgart <jens.baumgart@sap.com>
 * Copyright (C) 2010, Roland Grunberg <rgrunber@redhat.com>
 * Copyright (C) 2011, Ilya Ivanov <ilya.ivanov@intland.com>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Code extracted from org.eclipse.egit.ui.internal.actions.DiscardChangesAction
 * and reworked.
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.internal.util.ProjectUtil;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheCheckout;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEditor.PathEdit;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.osgi.util.NLS;

/**
 * The operation discards changes on a set of resources. In case of a folder
 * resource all file resources in the sub tree are processed.
 * Untracked files are ignored.
 */
public class DiscardChangesOperation implements IEGitOperation {

	IResource[] files;

	ISchedulingRule schedulingRule;

	/**
	 * Resource replacing variants
	 */
	public static enum ReplaceType {
		/**
		 * Using file version in Index
		 */
		INDEX,

		/**
		 * Using file version in HEAD revision
		 */
		HEAD
	}

	private ReplaceType type;

	/**
	 * Construct a {@link DiscardChangesOperation} object.
	 *
	 * By default, files are replaced with what's in the index
	 *
	 * @param files
	 */
	public DiscardChangesOperation(IResource[] files) {
		this.files = files;
		this.type = ReplaceType.INDEX;
		schedulingRule = calcRefreshRule(files);
	}

	/**
	 * Construct a {@link DiscardChangesOperation} object.
	 *
	 * @param type
	 * @param files
	 */
	public DiscardChangesOperation(ReplaceType type, IResource[] files) {
		this.type = type;
		this.files = files;
		schedulingRule = calcRefreshRule(files);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.egit.core.op.IEGitOperation#getSchedulingRule()
	 */
	public ISchedulingRule getSchedulingRule() {
		return schedulingRule;
	}

	private static ISchedulingRule calcRefreshRule(IResource[] resources) {
		List<ISchedulingRule> rules = new ArrayList<ISchedulingRule>();
		IResourceRuleFactory ruleFactory = ResourcesPlugin.getWorkspace()
		.getRuleFactory();
		for (IResource resource : resources) {
			ISchedulingRule rule = ruleFactory.refreshRule(resource);
			if (rule != null)
				rules.add(rule);
		}
		if (rules.size() == 0)
			return null;
		else
			return new MultiRule(rules.toArray(new IResource[rules.size()]));
	}

	public void execute(IProgressMonitor m) throws CoreException {
		IProgressMonitor monitor;
		if (m == null)
			monitor = new NullProgressMonitor();
		else
			monitor = m;
		IWorkspaceRunnable action = new IWorkspaceRunnable() {
			public void run(IProgressMonitor actMonitor) throws CoreException {
				discardChanges(actMonitor);
			}
		};
		ResourcesPlugin.getWorkspace().run(action, getSchedulingRule(),
				IWorkspace.AVOID_UPDATE, monitor);
	}

	private void discardChanges(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(CoreText.DiscardChangesOperation_discardingChanges, 2);
		boolean errorOccured = false;
		List<IResource> allFiles = new ArrayList<IResource>();
		// find all files
		for (IResource res : files) {
			allFiles.addAll(getAllMembers(res));
		}
		for (IResource res : allFiles) {
			Repository repo = getRepository(res);
			if (repo == null) {
				IStatus status = Activator.error(
						CoreText.DiscardChangesOperation_repoNotFound, null);
				throw new CoreException(status);
			}
			try {
				discardChange(res, repo);
			} catch (IOException e) {
				errorOccured = true;
				String message = NLS.bind(
						CoreText.DiscardChangesOperation_discardFailed, res
						.getFullPath());
				Activator.logError(message, e);
			}
		}
		monitor.worked(1);
		try {
			ProjectUtil.refreshResources(files, new SubProgressMonitor(monitor,
					1));
		} catch (CoreException e) {
			errorOccured = true;
			Activator.logError(CoreText.DiscardChangesOperation_refreshFailed,
					e);
		}
		monitor.worked(1);
		monitor.done();
		if (errorOccured) {
			IStatus status = Activator.error(
					CoreText.DiscardChangesOperation_discardFailedSeeLog, null);
			throw new CoreException(status);
		}
	}

	private static Repository getRepository(IResource resource) {
		IProject project = resource.getProject();
		RepositoryMapping repositoryMapping = RepositoryMapping
		.getMapping(project);
		if (repositoryMapping != null)
			return repositoryMapping.getRepository();
		else
			return null;
	}

	/**
	 * @param res
	 * @param repository
	 * @throws IOException
	 */
	public void discardChange(IResource res, Repository repository) throws IOException {
		String resRelPath = RepositoryMapping.getMapping(res)
		.getRepoRelativePath(res);
		DirCache dc = repository.lockDirCache();
		switch (type) {
		case INDEX:
			try {
				DirCacheEntry entry = dc.getEntry(resRelPath);
				if (entry != null) {
					File file = new File(res.getLocationURI());
					DirCacheCheckout.checkoutEntry(repository, file, entry);
				}
			} finally {
				dc.unlock();
			}
			break;
		case HEAD:
			File f = new File(res.getLocationURI());

			RevCommit commit = new RevWalk(repository).parseCommit(repository.getRef(
					Constants.HEAD).getObjectId());

			TreeWalk w = TreeWalk.forPath(repository, resRelPath, commit.getTree());

			if (w == null)
				return;	// git doesn't know such resource path

			final ObjectId blobId = w.getObjectId(0);
			ObjectLoader ol = repository.open(blobId);
			final long blobSize = ol.getSize();

			File parentDir = f.getParentFile();
			File tmpFile = File.createTempFile("._" + f.getName(), null, parentDir); //$NON-NLS-1$
			FileOutputStream channel = new FileOutputStream(tmpFile);
			try {
				ol.copyTo(channel);
			} finally {
				channel.close();
			}

			if (!tmpFile.renameTo(f)) {
				// Rename failed. Let's delete the file and try again
				FileUtils.delete(f);
				if (!tmpFile.renameTo(f))
					throw new IOException(CoreText.ReplaceWithHeadOperation_WritingError + f.getPath());
			}

			try {
				DirCacheEditor dcEdit = dc.editor();

				dcEdit.add(new PathEdit(resRelPath) {
					public void apply(DirCacheEntry ent) {
						ent.setObjectId(blobId);
						ent.setLength(blobSize);
					}
				});
				dcEdit.commit();
			} catch (Exception e) {
				throw new IOException(CoreText.ReplaceWithHeadOperation_IndexError + f.getPath());
			} finally {
				dc.unlock();
			}
			break;
		default:
			break;
		}
	}

	/**
	 * @param res
	 *            an IResource
	 * @return An ArrayList with all members of this IResource of arbitrary
	 *         depth. This will return just the argument res if it is a file.
	 */
	private ArrayList<IResource> getAllMembers(IResource res) {
		ArrayList<IResource> ret = new ArrayList<IResource>();
		if (res.getLocation().toFile().isFile()) {
			ret.add(res);
		} else {
			getAllMembersHelper(res, ret);
		}
		return ret;
	}

	private void getAllMembersHelper(IResource res, ArrayList<IResource> ret) {
		if (res instanceof IContainer) {
			ArrayList<IResource> tmp = new ArrayList<IResource>();
			IContainer cont = (IContainer) res;
			try {
				for (IResource r : cont.members()) {
					if (r.getLocation().toFile().isFile()) {
						tmp.add(r);
					} else {
						getAllMembersHelper(r, tmp);
					}
				}
			} catch (CoreException e) {
				// thrown by members()
				// ignore children in case parent resource no longer accessible
				return;
			}

			ret.addAll(tmp);
		}
	}

}
