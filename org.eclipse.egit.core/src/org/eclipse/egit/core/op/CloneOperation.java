/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.core.op;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.egit.core.CoreText;
import org.eclipse.egit.core.EclipseGitProgressTransformer;
import org.eclipse.jgit.dircache.DirCacheCheckout;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.osgi.util.NLS;

/**
 * Clones a repository from a remote location to a local location.
 */
public class CloneOperation {
	private final URIish uri;

	private final boolean allSelected;

	private final Collection<Ref> selectedBranches;

	private final File workdir;

	private final File gitdir;

	private final Ref ref;

	private final String remoteName;

	private final int timeout;

	private FileRepository local;

	private RemoteConfig remoteConfig;

	private FetchResult fetchResult;

	private CredentialsProvider credentialsProvider;

	/**
	 * Create a new clone operation.
	 *
	 * @param uri
	 *            remote we should fetch from.
	 * @param allSelected
	 *            true when all branches have to be fetched (indicates wildcard
	 *            in created fetch refspec), false otherwise.
	 * @param selectedBranches
	 *            collection of branches to fetch. Ignored when allSelected is
	 *            true.
	 * @param workdir
	 *            working directory to clone to. The directory may or may not
	 *            already exist.
	 * @param ref
	 *            ref to be checked out after clone.
	 * @param remoteName
	 *            name of created remote config as source remote (typically
	 *            named "origin").
	 * @param timeout timeout in seconds
	 */
	public CloneOperation(final URIish uri, final boolean allSelected,
			final Collection<Ref> selectedBranches, final File workdir,
			final Ref ref, final String remoteName, int timeout) {
		this.uri = uri;
		this.allSelected = allSelected;
		this.selectedBranches = selectedBranches;
		this.workdir = workdir;
		this.gitdir = new File(workdir, Constants.DOT_GIT);
		this.ref = ref;
		this.remoteName = remoteName;
		this.timeout = timeout;
	}

	/**
	 * Sets a credentials provider
	 * @param credentialsProvider
	 */
	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	/**
	 * @param pm
	 *            the monitor to be used for reporting progress and responding
	 *            to cancellation. The monitor is never <code>null</code>
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	public void run(final IProgressMonitor pm)
			throws InvocationTargetException, InterruptedException {
		final IProgressMonitor monitor;
		if (pm == null)
			monitor = new NullProgressMonitor();
		else
			monitor = pm;

		try {
			monitor.beginTask(NLS.bind(CoreText.CloneOperation_title, uri),
					5000);
			try {
				doInit(new SubProgressMonitor(monitor, 100));
				doFetch(new SubProgressMonitor(monitor, 4000));
				doCheckout(new SubProgressMonitor(monitor, 900));
			} finally {
				closeLocal();
			}
		} catch (final Exception e) {
			try {
				FileUtils.delete(workdir, FileUtils.RECURSIVE);
			} catch (IOException ioe) {
				throw new InvocationTargetException(ioe);
			}
			if (monitor.isCanceled())
				throw new InterruptedException();
			else
				throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}
	}


	/**
	 * @return The git directory which will contain the repository
	 */
	public File getGitDir() {
		return gitdir;
	}

	private void closeLocal() {
		if (local != null) {
			local.close();
			local = null;
		}
	}

	private void doInit(final IProgressMonitor monitor)
			throws URISyntaxException, IOException {
		monitor.setTaskName(CoreText.CloneOperation_initializingRepository);

		local = new FileRepository(gitdir);
		local.create();

		if (ref != null && ref.getName().startsWith(Constants.R_HEADS)) {
			final RefUpdate head = local.updateRef(Constants.HEAD);
			head.disableRefLog();
			head.link(ref.getName());
		}

		FileBasedConfig config = local.getConfig();
		remoteConfig = new RemoteConfig(config, remoteName);
		remoteConfig.addURI(uri);

		final String dst = Constants.R_REMOTES + remoteConfig.getName();
		RefSpec wcrs = new RefSpec();
		wcrs = wcrs.setForceUpdate(true);
		wcrs = wcrs.setSourceDestination(Constants.R_HEADS
				+ "*", dst + "/*"); //$NON-NLS-1$ //$NON-NLS-2$

		if (allSelected) {
			remoteConfig.addFetchRefSpec(wcrs);
		} else {
			for (final Ref selectedRef : selectedBranches)
				if (wcrs.matchSource(selectedRef))
					remoteConfig.addFetchRefSpec(wcrs.expandFromSource(selectedRef));
		}

		// we're setting up for a clone with a checkout
		config.setBoolean(
				ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_BARE, false);

		remoteConfig.update(config);

		// branch is like 'Constants.R_HEADS + branchName', we need only
		// the 'branchName' part
		if (ref != null && ref.getName().startsWith(Constants.R_HEADS)) {
			String branchName = ref.getName().substring(Constants.R_HEADS.length());

			// setup the default remote branch for branchName
			config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_REMOTE, remoteName);
			config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_MERGE, ref.getName());
		}
		config.save();
	}

	private void doFetch(final IProgressMonitor monitor)
			throws NotSupportedException, TransportException {
		final Transport tn = Transport.open(local, remoteConfig);
		if (credentialsProvider != null)
			tn.setCredentialsProvider(credentialsProvider);
		tn.setTimeout(this.timeout);
		try {
			final EclipseGitProgressTransformer pm;
			pm = new EclipseGitProgressTransformer(monitor);
			fetchResult = tn.fetch(pm, null);
		} finally {
			tn.close();
		}
	}

	private void doCheckout(final IProgressMonitor monitor) throws IOException {
		if (ref == null)
			return;
		final Ref head = fetchResult.getAdvertisedRef(ref.getName());
		if (head == null || head.getObjectId() == null)
			return;

		final RevWalk rw = new RevWalk(local);
		final RevCommit mapCommit;
		try {
			mapCommit = rw.parseCommit(head.getObjectId());
		} finally {
			rw.release();
		}

		final RefUpdate u;

		boolean detached = !head.getName().startsWith(Constants.R_HEADS);
		u = local.updateRef(Constants.HEAD, detached);
		u.setNewObjectId(mapCommit.getId());
		u.forceUpdate();

		monitor.setTaskName(CoreText.CloneOperation_checkingOutFiles);
		DirCacheCheckout dirCacheCheckout = new DirCacheCheckout(
				local, null, local.lockDirCache(), mapCommit.getTree());
		dirCacheCheckout.setFailOnConflict(true);
		boolean result = dirCacheCheckout.checkout();
		if (!result)
			// this should never happen when writing in an empty folder
			throw new IOException("Internal error occurred on checking out files"); //$NON-NLS-1$
		monitor.setTaskName(CoreText.CloneOperation_writingIndex);
	}
}
