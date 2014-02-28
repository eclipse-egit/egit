/*******************************************************************************
 * Copyright (c) 2015 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Laurent Goubet <laurent.goubet@obeo.fr> - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.core.internal.merge;

import static org.eclipse.jgit.treewalk.TreeWalk.T_BASE;
import static org.eclipse.jgit.treewalk.TreeWalk.T_FILE;
import static org.eclipse.jgit.treewalk.TreeWalk.T_INDEX;
import static org.eclipse.jgit.treewalk.TreeWalk.T_OURS;
import static org.eclipse.jgit.treewalk.TreeWalk.T_THEIRS;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.storage.TreeParserResourceVariant;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.dircache.DirCacheBuildIterator;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeResult;
import org.eclipse.jgit.merge.RecursiveMerger;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.core.mapping.IResourceMappingMerger;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberMergeContext;
import org.eclipse.team.core.subscribers.SubscriberResourceMappingContext;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;

/**
 * This extends the recursive merger in order to take into account specific
 * mergers provided by the Team
 * {@link org.eclipse.core.resources.mapping.ModelProvider model providers}.
 * <p>
 * The Recursive Merger handles files one-by-one, calling file-specific merge
 * drivers for each. On the opposite, this strategy can handle bigger sets of
 * files at once, delegating the merge to the files' model. As such,
 * file-specific merge drivers may not be called from this strategy if that file
 * is part of a larger model.
 * </p>
 * <p>
 * Any file that is <b>not</b> part of a model, which model cannot be
 * determined, or which model does not specify a custom merger, will be handled
 * as it would by the RecursiveMerger.
 * </p>
 */
public class RecursiveModelMerger extends RecursiveMerger {
	/**
	 * This will be populated during the course of the RecursiveMappingMergers'
	 * executions. These files have been cleanly merged and we should thus make
	 * sure the DirCacheBuilder takes their latest working directory version
	 * before committing.
	 */
	private final Set<String> makeInSync = new LinkedHashSet<String>();

	/**
	 * keeps track of the files we've already merged. Since we iterate one file
	 * at a time but may merge multiple files at once when they are part of the
	 * same model, this will help us avoid merging the same file or model twice.
	 */
	private final Set<String> handledPaths = new HashSet<String>();

	/**
	 * Default recursive model merger.
	 *
	 * @param db
	 * @param inCore
	 */
	public RecursiveModelMerger(Repository db, boolean inCore) {
		super(db, inCore);
	}

	@Override
	protected boolean mergeTreeWalk(TreeWalk treeWalk, boolean ignoreConflicts)
			throws IOException {
		final GitResourceVariantTreeProvider variantTreeProvider = new TreeWalkResourceVariantTreeProvider(
				getRepository(), treeWalk);
		final GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(
				variantTreeProvider);
		final RemoteResourceMappingContext remoteMappingContext = new SubscriberResourceMappingContext(
				subscriber, true);

		try {
			refreshRoots(subscriber.roots());
		} catch (CoreException e) {
			// We cannot be sure that Team and/or the merger implementations
			// will properly handle unrefreshed files. Fall back to merging
			// without workspace awareness.
			Activator.logError(CoreText.RecursiveModelMerger_RefreshError, e);
			return super.mergeTreeWalk(treeWalk, ignoreConflicts);
		}

		// Eager lookup for the logical models to avoid issues in case we
		// iterate over a file that does not exist locally before the rest of
		// its logical model.
		final LogicalModels logicalModels = new LogicalModels();
		logicalModels.build(variantTreeProvider.getKnownResources(),
				remoteMappingContext);

		// We are done with the setup. We can now iterate over the tree walk and
		// either delegate to the logical model's merger if any or fall back to
		// standard git merging. Basically, any file that is not a part of a
		// logical model that defines its own specific merger will be handled as
		// it would by the RecursiveMerger.
		while (treeWalk.next()) {
			final int modeBase = treeWalk.getRawMode(T_BASE);
			final int modeOurs = treeWalk.getRawMode(T_OURS);
			final int modeTheirs = treeWalk.getRawMode(T_THEIRS);
			if (modeBase == 0 && modeOurs == 0 && modeTheirs == 0) {
				// untracked
				continue;
			}
			final String path = treeWalk.getPathString();
			if (handledPaths.contains(path)) {
				// This one has been handled as a result of a previous model
				// merge. Simply make sure we use its latest content if it is
				// not in conflict.
				if (treeWalk.isSubtree() && enterSubtree)
					treeWalk.enterSubtree();
				if (!unmergedPaths.contains(path))
					makeInSync.add(path);
				continue;
			}

			final int nonZeroMode = modeBase != 0 ? modeBase
					: modeOurs != 0 ? modeOurs : modeTheirs;
			final IResource resource = ResourceUtil
					.getResourceHandleForLocation(getRepository(), path,
							FileMode.fromBits(nonZeroMode) == FileMode.TREE);
			Set<IResource> logicalModel = logicalModels.getModel(resource);

			IResourceMappingMerger modelMerger = null;
			if (logicalModel != null) {
				try {
					// We need to refresh because new resources may have been added
					refreshRoots(subscriber.roots());
					modelMerger = LogicalModels.findAdapter(logicalModel,
							IResourceMappingMerger.class);
				} catch (CoreException e) {
					Activator.logError(
							CoreText.RecursiveModelMerger_AdaptError, e);
					// ignore this model and fall back to default
					if (!fallBackToDefaultMerge(treeWalk, ignoreConflicts)) {
						cleanUp();
						return false;
					}
				}
			}
			if (modelMerger != null) {
				enterSubtree = true;
				if (!new ModelMerge(this, subscriber, remoteMappingContext,
						path, logicalModel, modelMerger).run()) {
					return false;
				}
				if (treeWalk.isSubtree()) {
					enterSubtree = true;
				}
			} else if (!fallBackToDefaultMerge(treeWalk, ignoreConflicts)) {
				cleanUp();
				return false;
			}
			if (treeWalk.isSubtree() && enterSubtree) {
				treeWalk.enterSubtree();
			}
		}
		if (!makeInSync.isEmpty()) {
			indexModelMergedFiles();
		}
		return true;
	}

	private boolean fallBackToDefaultMerge(TreeWalk treeWalk,
			boolean ignoreConflicts) throws MissingObjectException,
			IncorrectObjectTypeException, CorruptObjectException, IOException {
		boolean hasWorkingTreeIterator = tw.getTreeCount() > T_FILE;
		return processEntry(
				treeWalk.getTree(T_BASE, CanonicalTreeParser.class),
				treeWalk.getTree(T_OURS, CanonicalTreeParser.class),
				treeWalk.getTree(T_THEIRS, CanonicalTreeParser.class),
				treeWalk.getTree(T_INDEX, DirCacheBuildIterator.class),
				hasWorkingTreeIterator ? treeWalk.getTree(T_FILE,
						WorkingTreeIterator.class) : null, ignoreConflicts);
	}

	/**
	 * Add files modified by model mergers to the index.
	 *
	 * @throws CorruptObjectException
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 */
	private void indexModelMergedFiles() throws CorruptObjectException,
			MissingObjectException, IncorrectObjectTypeException, IOException {
		try (final TreeWalk syncingTreeWalk = new TreeWalk(getRepository())) {
			syncingTreeWalk.addTree(new DirCacheIterator(dircache));
			syncingTreeWalk.addTree(new FileTreeIterator(getRepository()));
			syncingTreeWalk.setRecursive(true);
			syncingTreeWalk.setFilter(PathFilterGroup
					.createFromStrings(makeInSync));
			String lastAdded = null;
			while (syncingTreeWalk.next()) {
				String path = syncingTreeWalk.getPathString();
				if (path.equals(lastAdded))
					continue;

				WorkingTreeIterator workingTree = syncingTreeWalk.getTree(1,
						WorkingTreeIterator.class);
				DirCacheIterator dirCache = syncingTreeWalk.getTree(0,
						DirCacheIterator.class);
				if (dirCache == null && workingTree != null
						&& workingTree.isEntryIgnored()) {
					// nothing to do on this one
				} else if (workingTree != null) {
					if (dirCache == null || dirCache.getDirCacheEntry() == null
							|| !dirCache.getDirCacheEntry().isAssumeValid()) {
						final DirCacheEntry dce = new DirCacheEntry(path);
						final FileMode mode = workingTree
								.getIndexFileMode(dirCache);
						dce.setFileMode(mode);

						if (FileMode.GITLINK != mode) {
							dce.setLength(workingTree.getEntryLength());
							dce.setLastModified(workingTree
									.getEntryLastModified());
							InputStream is = workingTree.openEntryStream();
							try {
								dce.setObjectId(getObjectInserter()
										.insert(Constants.OBJ_BLOB,
												workingTree
														.getEntryContentLength(),
												is));
							} finally {
								is.close();
							}
						} else {
							dce.setObjectId(workingTree.getEntryObjectId());
						}
						builder.add(dce);
						lastAdded = path;
					} else {
						builder.add(dirCache.getDirCacheEntry());
					}
				} else if (dirCache != null
						&& FileMode.GITLINK == dirCache.getEntryFileMode()) {
					builder.add(dirCache.getDirCacheEntry());
				}
			}
		}
	}

	private static String getRepoRelativePath(IResource file) {
		final RepositoryMapping mapping = RepositoryMapping.getMapping(file);
		return mapping.getRepoRelativePath(file);
	}

	/**
	 * On many aspects, team relies on the refreshed state of the workspace
	 * files, notably to determine if a file is in sync or not. Since we could
	 * have been called for a rebase, rebase that checked out a new commit
	 * without refreshing the workspace afterwards, team could see "in-sync"
	 * files even though they no longer exist in the workspace. This should be
	 * called before any merging takes place to make sure all files concerned by
	 * this merge operation are refreshed beforehand.
	 *
	 * @param resources
	 *            The set of resource roots to refresh.
	 * @throws CoreException
	 *             Thrown whenever we fail at refreshing one of the resources or
	 *             its children.
	 */
	private void refreshRoots(IResource[] resources) throws CoreException {
		for (IResource root : resources) {
			root.refreshLocal(IResource.DEPTH_INFINITE,
					new NullProgressMonitor());
		}
	}

	private void markConflict(String filePath, DirCacheBuilder cacheBuilder,
			TreeParserResourceVariant baseVariant,
			TreeParserResourceVariant ourVariant,
			TreeParserResourceVariant theirVariant) {
		add(filePath, cacheBuilder, baseVariant, DirCacheEntry.STAGE_1);
		add(filePath, cacheBuilder, ourVariant, DirCacheEntry.STAGE_2);
		add(filePath, cacheBuilder, theirVariant, DirCacheEntry.STAGE_3);
	}

	private void add(String path, DirCacheBuilder cacheBuilder,
			TreeParserResourceVariant variant, int stage) {
		if (variant != null && !FileMode.TREE.equals(variant.getRawMode())) {
			DirCacheEntry e = new DirCacheEntry(path, stage);
			e.setFileMode(FileMode.fromBits(variant.getRawMode()));
			e.setObjectId(variant.getObjectId());
			e.setLastModified(0);
			e.setLength(0);
			cacheBuilder.add(e);
		}
	}

	private static class ModelMerge {
		private final RecursiveModelMerger merger;

		private final GitResourceVariantTreeSubscriber subscriber;

		private final RemoteResourceMappingContext remoteMappingContext;

		private final String path;

		private final Set<IResource> logicalModel;

		private final IResourceMappingMerger modelMerger;

		public ModelMerge(RecursiveModelMerger merger,
				GitResourceVariantTreeSubscriber subscriber,
				RemoteResourceMappingContext remoteMappingContext, String path,
				Set<IResource> logicalModel, IResourceMappingMerger modelMerger) {
			this.merger = merger;
			this.subscriber = subscriber;
			this.remoteMappingContext = remoteMappingContext;
			this.path = path;
			this.logicalModel = logicalModel;
			this.modelMerger = modelMerger;
		}

		private boolean run() throws CorruptObjectException, IOException {
			try {
				final IMergeContext mergeContext = prepareMergeContext();
				final IStatus status = modelMerger.merge(mergeContext,
						new NullProgressMonitor());
				registerHandledFiles(mergeContext, status);
			} catch (CoreException e) {
				Activator.logError(e.getMessage(), e);
				merger.cleanUp();
				return false;
			} catch (OperationCanceledException e) {
				final String message = NLS
						.bind(CoreText.RecursiveModelMerger_ScopeInitializationInterrupted,
								path);
				Activator.logError(message, e);
				merger.cleanUp();
				return false;
			}
			return true;
		}

		private void registerHandledFiles(final IMergeContext mergeContext,
				final IStatus status) throws TeamException {
			for (IResource handledFile : logicalModel) {
				final String filePath = getRepoRelativePath(handledFile);
				merger.modifiedFiles.add(filePath);
				merger.handledPaths.add(filePath);

				// The merge failed. If some parts of the model were
				// auto-mergeable, the model merger told us so through
				// GitMergeContext#markAsMerged() (stored within #makeInSync).
				// All other components of the logical model should be marked as
				// conflicts.
				if (status.getSeverity() != IStatus.OK
						&& !merger.makeInSync.contains(filePath)) {
					merger.unmergedPaths.add(filePath);
					merger.mergeResults.put(filePath, new MergeResult<RawText>(
							Collections.<RawText> emptyList()));
					final TreeParserResourceVariant baseVariant = (TreeParserResourceVariant) subscriber
							.getBaseTree().getResourceVariant(handledFile);
					final TreeParserResourceVariant ourVariant = (TreeParserResourceVariant) subscriber
							.getSourceTree().getResourceVariant(handledFile);
					final TreeParserResourceVariant theirVariant = (TreeParserResourceVariant) subscriber
							.getRemoteTree().getResourceVariant(handledFile);
					merger.markConflict(filePath, merger.builder, baseVariant,
							ourVariant, theirVariant);
				} else if (mergeContext.getDiffTree().getDiff(handledFile) == null) {
					// If no diff, the model merger does... nothing
					// Make sure this file will be added to the index.
					merger.makeInSync.add(filePath);
				}
			}
		}

		/**
		 * Create and initialize the merge context for the given model.
		 *
		 * @return An initialized merge context for the given model.
		 * @throws CoreException
		 *             Thrown if we cannot initialize the scope for this merge
		 *             context.
		 * @throws OperationCanceledException
		 *             Thrown if the user cancelled the initialization.
		 */
		private IMergeContext prepareMergeContext() throws CoreException,
				OperationCanceledException {
			final Set<ResourceMapping> allMappings = LogicalModels
					.getResourceMappings(logicalModel, remoteMappingContext);
			final ResourceMapping[] mappings = allMappings
					.toArray(new ResourceMapping[allMappings.size()]);

			final ISynchronizationScopeManager manager = new SubscriberScopeManager(
					subscriber.getName(), mappings, subscriber,
					remoteMappingContext, true) {
				@Override
				public ISchedulingRule getSchedulingRule() {
					return RuleUtil.getRule(merger.getRepository());
				}
			};
			manager.initialize(new NullProgressMonitor());

			final IMergeContext context = new GitMergeContext(merger,
					subscriber, manager);
			// Wait for the asynchronous scope expanding to end (started from
			// the initialization of our merge context)
			waitForScope(context);

			return context;
		}

		private void waitForScope(IMergeContext context) {
			// The UILockListener might prevent us from properly joining.
			boolean joined = false;
			while (!joined) {
				try {
					Job.getJobManager()
							.join(context, new NullProgressMonitor());
					joined = true;
				} catch (InterruptedException e) {
					// Some other UI threads were trying to run. Let the
					// syncExecs do their jobs and re-try to join on ours.
				}
			}
		}
	}

	private static class GitMergeContext extends SubscriberMergeContext {

		private final RecursiveModelMerger merger;

		/**
		 * Create and initialize a merge context for the given subscriber.
		 *
		 * @param merger
		 *            the merger
		 * @param subscriber
		 *            the subscriber.
		 * @param scopeManager
		 *            the scope manager.
		 */
		public GitMergeContext(RecursiveModelMerger merger,
				Subscriber subscriber, ISynchronizationScopeManager scopeManager) {
			super(subscriber, scopeManager);
			this.merger = merger;
			initialize();
		}

		public void markAsMerged(IDiff node, boolean inSyncHint,
				IProgressMonitor monitor) throws CoreException {
			final IResource resource = getDiffTree().getResource(node);
			merger.addSyncPath(resource);
		}

		public void reject(IDiff diff, IProgressMonitor monitor)
				throws CoreException {
			// Empty implementation
		}

		protected void makeInSync(IDiff diff, IProgressMonitor monitor)
				throws CoreException {
			final IResource resource = getDiffTree().getResource(diff);
			merger.addSyncPath(resource);
		}
	}

	private void addSyncPath(IResource resource) {
		makeInSync.add(getRepoRelativePath(resource));
	}
}
