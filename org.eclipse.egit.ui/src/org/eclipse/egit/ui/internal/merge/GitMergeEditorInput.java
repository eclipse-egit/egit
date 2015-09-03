/*******************************************************************************
 * Copyright (C) 2010, 2014 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.merge;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.internal.job.RuleUtil;
import org.eclipse.egit.core.internal.merge.DirCacheResourceVariantTreeProvider;
import org.eclipse.egit.core.internal.merge.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.internal.merge.GitResourceVariantTreeProvider;
import org.eclipse.egit.core.internal.merge.LogicalModels;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.internal.storage.WorkingTreeFileRevision;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.revision.EditableRevision;
import org.eclipse.egit.ui.internal.revision.FileRevisionTypedElement;
import org.eclipse.egit.ui.internal.revision.GitCompareFileRevisionEditorInput.EmptyTypedElement;
import org.eclipse.egit.ui.internal.revision.LocalFileRevision;
import org.eclipse.egit.ui.internal.revision.LocationEditableRevision;
import org.eclipse.egit.ui.internal.revision.ResourceEditableRevision;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.NotIgnoredFilter;
import org.eclipse.jgit.treewalk.filter.OrTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.IO;
import org.eclipse.jgit.util.RawParseUtils;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberMergeContext;
import org.eclipse.team.core.subscribers.SubscriberResourceMappingContext;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.ui.mapping.ISynchronizationCompareAdapter;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;

/**
 * A Git-specific {@link CompareEditorInput}
 */
public class GitMergeEditorInput extends CompareEditorInput {
	private static final String LABELPATTERN = "{0} - {1}"; //$NON-NLS-1$

	private static final Image FOLDER_IMAGE = PlatformUI.getWorkbench()
			.getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

	private static final Image PROJECT_IMAGE = PlatformUI.getWorkbench()
			.getSharedImages().getImage(SharedImages.IMG_OBJ_PROJECT);

	private final boolean useWorkspace;

	private final IPath[] locations;

	/**
	 * @param useWorkspace
	 *            if <code>true</code>, use the workspace content (i.e. the
	 *            Git-merged version) as "left" content, otherwise use HEAD
	 *            (i.e. the previous, non-merged version)
	 * @param locations
	 *            as selected by the user
	 */
	public GitMergeEditorInput(boolean useWorkspace, IPath... locations) {
		super(new CompareConfiguration());
		this.useWorkspace = useWorkspace;
		this.locations = locations;
		CompareConfiguration config = getCompareConfiguration();
		config.setLeftEditable(true);
	}

	@Override
	public Object getAdapter(Class adapter) {
		if ((adapter == IFile.class || adapter == IResource.class)
				&& isUIThread()) {
			Object selectedEdition = getSelectedEdition();
			if (selectedEdition instanceof DiffNode) {
				DiffNode diffNode = (DiffNode) selectedEdition;
				ITypedElement element = diffNode.getLeft();
				if (element instanceof ResourceEditableRevision) {
					ResourceEditableRevision resourceRevision = (ResourceEditableRevision) element;
					return resourceRevision.getFile();
				}
			}
		}
		return super.getAdapter(adapter);
	}

	private static boolean isUIThread() {
		return Display.getCurrent() != null;
	}

	@Override
	protected Object prepareInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		monitor.beginTask(UIText.GitMergeEditorInput_CheckingResourcesTaskName,
				IProgressMonitor.UNKNOWN);

		// Make sure all resources belong to the same repository
		final Map<Repository, Collection<String>> pathsByRepository = ResourceUtil
				.splitPathsByRepository(Arrays.asList(locations));
		if (pathsByRepository.size() != 1)
			throw new InvocationTargetException(new IllegalStateException(
					UIText.RepositoryAction_multiRepoSelection));
		final Repository repository = pathsByRepository.keySet().iterator()
				.next();
		final List<String> filterPaths = new ArrayList<String>(
				pathsByRepository.get(repository));

		checkCanceled(monitor);

		// The merge drivers have done their job of putting the necessary
		// information in the index
		// Read that info and provide it to the file-specific comparators
		RevWalk rw = null;
		try {
			rw = new RevWalk(repository);

			// get the "right" side (MERGE_HEAD for merge, ORIG_HEAD for rebase)
			final RevCommit rightCommit = getRightCommit(rw, repository);

			// we need the HEAD, also to determine the common ancestor
			final RevCommit headCommit = getLeftCommit(rw, repository);

			// try to obtain the common ancestor
			RevCommit ancestorCommit = getCommonAncestor(rw, rightCommit,
					headCommit);

			checkCanceled(monitor);

			// set the labels
			setLabels(repository, rightCommit, headCommit, ancestorCommit);

			final ICompareInput input = prepareCompareInput(repository,
					filterPaths, monitor);
			if (input != null)
				return input;

			checkCanceled(monitor);
			return buildDiffContainer(repository, headCommit, ancestorCommit,
					filterPaths, rw, monitor);
		} catch (IOException e) {
			throw new InvocationTargetException(e);
		} finally {
			if (rw != null)
				rw.dispose();
			monitor.done();
		}
	}

	private void checkCanceled(IProgressMonitor monitor)
			throws InterruptedException {
		if (monitor.isCanceled())
			throw new InterruptedException();
	}

	/**
	 * Even if there is a single file involved in this operation, it may have a
	 * custom comparator or merger defined. This will be found through its
	 * specific ISynchronizationCompareAdapter.
	 * <p>
	 * If there are multiple files involved, we need them all to be part of the
	 * same logical model. Otherwise, we can't be sure that multiple
	 * ISynchronizationCompareAdapter aren't interested in the different files,
	 * and thus cannot show a valid 'aggregate' compare editor.
	 * </p>
	 * <p>
	 * Then again, even if the multiple files involved are all part of
	 * single-file models (i.e. none of them is a part of a larger logical
	 * model, and we have as many models involved as there are files), we cannot
	 * show them all within the same compare editor input. Comparing the files
	 * and determining conflicts is the job of the
	 * ISynchronizationCompareAdapter(s), <u>not</u> ours. If we cannot reliably
	 * find the appropriate compare adapter, we should not try and compare the
	 * files ourselves. The user will have to manually open the merge tool on
	 * each individual logical model.
	 * </p>
	 * <p>
	 * Since we cannot determine the logical model of a file that is not in the
	 * workspace, this will fall back to the 'old' merge tool (we compute the
	 * diffs ourselves without consideration for the file type) iff there are
	 * <u>only</u> such files in the locations set.
	 * </p>
	 *
	 * @param repository
	 *            Repository within which the compared files are located.
	 * @param filterPaths
	 *            repository-relative paths of the resources we are comparing.
	 * @param monitor
	 *            Monitor on which to report progress to the user.
	 * @return The useable compare input.
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	private ICompareInput prepareCompareInput(Repository repository,
			List<String> filterPaths, IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		try {
			final GitResourceVariantTreeProvider variantTreeProvider = new DirCacheResourceVariantTreeProvider(
					repository, useWorkspace);
			final Subscriber subscriber = new GitResourceVariantTreeSubscriber(
					variantTreeProvider);
			checkCanceled(monitor);

			final Set<IProject> projects = new LinkedHashSet<IProject>();
			for (IResource root : subscriber.roots())
				projects.add(root.getProject());

			// Compute the set of IResources involved in this operation.
			// This will be cut short if we find that at least one resource is
			// in the workspace and at least one resource is not.
			final Set<IResource> resourcesInOperation = new LinkedHashSet<IResource>();
			boolean outOfWS = false;
			for (IPath path : locations) {
				boolean foundMatchInWS = false;
				final Iterator<IProject> projectIterator = projects.iterator();
				while (!foundMatchInWS && projectIterator.hasNext()) {
					final IProject project = projectIterator.next();
					final IPath projectLocation = project.getLocation();
					if (projectLocation.equals(path)) {
						resourcesInOperation
								.addAll(getConflictingFilesFrom(project));
						foundMatchInWS = true;
					} else if (project.getLocation().isPrefixOf(path)) {
						final IResource resource = ResourceUtil
								.getResourceForLocation(path);
						if (resource instanceof IContainer)
							resourcesInOperation
									.addAll(getConflictingFilesFrom((IContainer) resource));
						else
							resourcesInOperation.add(resource);
						foundMatchInWS = true;
					}
				}
				if (!foundMatchInWS) {
					if (!resourcesInOperation.isEmpty())
						// no need to go any further : we have both files in the
						// workspace and files outside of it
						break;
					else
						// for now, all paths are out of the ws
						outOfWS = true;
				} else if (outOfWS)
					// There was a match in the workspace for this one
					// yet at least one path before that was out of the ws
					break;
			}

			checkCanceled(monitor);

			if (!resourcesInOperation.isEmpty() && outOfWS) {
				// At least one resource is in the workspace while at least one
				// is out of it.
				// We cannot reliably tell whether they are related enough to be
				// in the same compare editor.
				throw new InvocationTargetException(new IllegalStateException(
						UIText.GitMergeEditorInput_OutOfWSResources));
			} else if (resourcesInOperation.isEmpty()) {
				// All resources are out of the workspace.
				// Fall back to the workspace-unaware "prepareDiffInput"
			} else {
				final RemoteResourceMappingContext remoteMappingContext = new SubscriberResourceMappingContext(
						subscriber, true);
				// Make sure that all of the compared resources are either
				// - all part of the same model, or
				// - not part of any model.
				Set<IResource> model = null;
				for (IResource comparedResource : resourcesInOperation) {
					model = LogicalModels.discoverModel(comparedResource,
							remoteMappingContext);
					if (model.isEmpty()) {
						// not part of any model... carry on
					} else {
						if (!model.containsAll(resourcesInOperation)) {
							// These resources belong to multiple different
							// models.
							// The merge tool needs to be launched manually on
							// each distinct logical model.
							throw new RuntimeException(
									UIText.GitMergeEditorInput_MultipleModels);
						} else {
							// No use going further : we know these resource all
							// belong to the same model.
							break;
						}
					}
				}

				final ISynchronizationCompareAdapter compareAdapter = LogicalModels
						.findAdapter(model,
								ISynchronizationCompareAdapter.class);
				if (compareAdapter != null) {
					final Set<ResourceMapping> allMappings = LogicalModels
							.getResourceMappings(model, remoteMappingContext);

					checkCanceled(monitor);

					final ISynchronizationContext synchronizationContext = prepareSynchronizationContext(
							repository, subscriber, allMappings,
							remoteMappingContext);
					final Object modelObject = allMappings.iterator().next()
							.getModelObject();
					if (compareAdapter.hasCompareInput(synchronizationContext,
							modelObject))
						return compareAdapter.asCompareInput(
								synchronizationContext, modelObject);
					else {
						// This compare adapter does not know about our model
						// object
					}
				} else {
					// There isn't a specific compare adapter for this logical
					// model. Fall back to default.
				}
			}
		} catch (IOException e) {
			throw new InvocationTargetException(e);
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
		return null;
	}

	private Set<IResource> getConflictingFilesFrom(IContainer container)
			throws IOException {
		final Set<IResource> conflictingResources = new LinkedHashSet<IResource>();
		final RepositoryMapping mapping = RepositoryMapping
				.getMapping(container);
		if (mapping == null) {
			return conflictingResources;
		}
		final IndexDiffCacheEntry indexDiffCacheEntry = org.eclipse.egit.core.Activator
				.getDefault().getIndexDiffCache()
				.getIndexDiffCacheEntry(mapping.getRepository());
		if (indexDiffCacheEntry == null) {
			return conflictingResources;
		}
		final IndexDiffData indexDiffData = indexDiffCacheEntry.getIndexDiff();
		if (indexDiffData != null) {
			final IPath containerPath = container.getLocation();
			final File workTree = mapping.getWorkTree();
			if (workTree != null) {
				final IPath workDirPrefix = new Path(
						workTree.getCanonicalPath());
				for (String conflicting : indexDiffData.getConflicting()) {
					final IPath resourcePath = workDirPrefix
							.append(conflicting);
					if (containerPath.isPrefixOf(resourcePath)) {
						final IPath containerRelativePath = resourcePath
								.removeFirstSegments(containerPath
										.segmentCount());
						conflictingResources.add(container
								.getFile(containerRelativePath));
					}
				}
			}
		}
		return conflictingResources;
	}

	private ISynchronizationContext prepareSynchronizationContext(
			final Repository repository, Subscriber subscriber,
			Set<ResourceMapping> allModelMappings,
			RemoteResourceMappingContext mappingContext)
			throws CoreException, OperationCanceledException,
			InterruptedException {
		final ResourceMapping[] mappings = allModelMappings
				.toArray(new ResourceMapping[allModelMappings.size()]);

		final ISynchronizationScopeManager manager = new SubscriberScopeManager(subscriber.getName(), mappings, subscriber, mappingContext, true) {
			@Override
			public ISchedulingRule getSchedulingRule() {
				return RuleUtil.getRule(repository);
			}
		};
		manager.initialize(new NullProgressMonitor());

		final ISynchronizationContext context = new GitSynchronizationContext(
				subscriber, manager);
		// Wait for the asynchronous scope expanding to end (started from the
		// initialization of our synchronization context)
		Job.getJobManager().join(context, new NullProgressMonitor());

		return context;
	}

	private RevCommit getRightCommit(RevWalk revWalk, Repository repository)
			throws InvocationTargetException {
		try {
			String target;
			if (repository.getRepositoryState().equals(RepositoryState.MERGING))
				target = Constants.MERGE_HEAD;
			else if (repository.getRepositoryState().equals(
					RepositoryState.CHERRY_PICKING))
				target = Constants.CHERRY_PICK_HEAD;
			else if (repository.getRepositoryState().equals(
					RepositoryState.REBASING_INTERACTIVE))
				target = readFile(repository.getDirectory(),
						RebaseCommand.REBASE_MERGE + File.separatorChar
								+ RebaseCommand.STOPPED_SHA);
			else
				target = Constants.ORIG_HEAD;
			ObjectId mergeHead = repository.resolve(target);
			if (mergeHead == null)
				throw new IOException(NLS.bind(
						UIText.ValidationUtils_CanNotResolveRefMessage, target));
			return revWalk.parseCommit(mergeHead);
		} catch (IOException e) {
			throw new InvocationTargetException(e);
		}
	}

	private RevCommit getLeftCommit(RevWalk revWalk, Repository repository)
			throws InvocationTargetException {
		try {
			ObjectId head = repository.resolve(Constants.HEAD);
			if (head == null)
				throw new IOException(NLS.bind(
						UIText.ValidationUtils_CanNotResolveRefMessage,
						Constants.HEAD));
			return revWalk.parseCommit(head);
		} catch (IOException e) {
			throw new InvocationTargetException(e);
		}
	}

	private RevCommit getCommonAncestor(RevWalk revWalk, RevCommit rightCommit,
			RevCommit leftCommit) {
		List<RevCommit> startPoints = new ArrayList<RevCommit>();
		revWalk.setRevFilter(RevFilter.MERGE_BASE);
		startPoints.add(rightCommit);
		startPoints.add(leftCommit);
		try {
			revWalk.markStart(startPoints);
			return revWalk.next();
		} catch (Exception e) {
			return null;
		}
	}

	private void setLabels(Repository repository, RevCommit rightCommit,
			RevCommit leftCommit, RevCommit ancestorCommit)
			throws InvocationTargetException {
		CompareConfiguration config = getCompareConfiguration();
		config.setRightLabel(NLS.bind(LABELPATTERN,
				rightCommit.getShortMessage(),
				CompareUtils.truncatedRevision(rightCommit.name())));

		if (!useWorkspace)
			config.setLeftLabel(NLS.bind(LABELPATTERN,
					leftCommit.getShortMessage(),
					CompareUtils.truncatedRevision(leftCommit.name())));
		else
			config.setLeftLabel(UIText.GitMergeEditorInput_WorkspaceHeader);

		if (ancestorCommit != null)
			config.setAncestorLabel(NLS.bind(LABELPATTERN,
					ancestorCommit.getShortMessage(),
					CompareUtils.truncatedRevision(ancestorCommit.name())));

		// set title and icon
		final String fullBranch;
		try {
			fullBranch = repository.getFullBranch();
		} catch (IOException e) {
			throw new InvocationTargetException(e);
		}
		setTitle(NLS.bind(
				UIText.GitMergeEditorInput_MergeEditorTitle,
				new Object[] {
						Activator.getDefault().getRepositoryUtil()
								.getRepositoryName(repository),
						rightCommit.getShortMessage(), fullBranch }));
	}

	@Override
	protected void contentsCreated() {
		super.contentsCreated();
		// select the first conflict
		getNavigator().selectChange(true);
	}

	@Override
	protected void handleDispose() {
		super.handleDispose();
		// we do NOT dispose the images, as these are shared
	}

	private IDiffContainer buildDiffContainer(Repository repository,
			RevCommit headCommit,
			RevCommit ancestorCommit, List<String> filterPaths, RevWalk rw,
			IProgressMonitor monitor) throws IOException, InterruptedException {

		monitor.setTaskName(UIText.GitMergeEditorInput_CalculatingDiffTaskName);
		IDiffContainer result = new DiffNode(Differencer.CONFLICTING);

		try (TreeWalk tw = new TreeWalk(repository)) {
			int dirCacheIndex = tw.addTree(new DirCacheIterator(repository
					.readDirCache()));
			int fileTreeIndex = tw.addTree(new FileTreeIterator(repository));
			int repositoryTreeIndex = tw.addTree(rw.parseTree(repository
					.resolve(Constants.HEAD)));

			// skip ignored resources
			NotIgnoredFilter notIgnoredFilter = new NotIgnoredFilter(
					fileTreeIndex);
			// filter by selected resources
			if (filterPaths.size() > 1) {
				List<TreeFilter> suffixFilters = new ArrayList<TreeFilter>();
				for (String filterPath : filterPaths)
					suffixFilters.add(PathFilter.create(filterPath));
				TreeFilter otf = OrTreeFilter.create(suffixFilters);
				tw.setFilter(AndTreeFilter.create(otf, notIgnoredFilter));
			} else if (filterPaths.size() > 0) {
				String path = filterPaths.get(0);
				if (path.length() == 0)
					tw.setFilter(notIgnoredFilter);
				else
					tw.setFilter(AndTreeFilter.create(PathFilter.create(path),
							notIgnoredFilter));
			} else
				tw.setFilter(notIgnoredFilter);

			tw.setRecursive(true);

			while (tw.next()) {
				if (monitor.isCanceled())
					throw new InterruptedException();
				String gitPath = tw.getPathString();
				monitor.setTaskName(gitPath);

				FileTreeIterator fit = tw.getTree(fileTreeIndex,
						FileTreeIterator.class);
				if (fit == null)
					continue;

				DirCacheIterator dit = tw.getTree(dirCacheIndex,
						DirCacheIterator.class);

				final DirCacheEntry dirCacheEntry = dit == null ? null : dit
						.getDirCacheEntry();

				boolean conflicting = dirCacheEntry != null
						&& dirCacheEntry.getStage() > 0;

				AbstractTreeIterator rt = tw.getTree(repositoryTreeIndex,
						AbstractTreeIterator.class);

				// compare local file against HEAD to see if it was modified
				boolean modified = rt != null
						&& !fit.getEntryObjectId()
								.equals(rt.getEntryObjectId());

				// if this is neither conflicting nor changed, we skip it
				if (!conflicting && !modified)
					continue;

				ITypedElement right;
				if (conflicting) {
					GitFileRevision revision = GitFileRevision.inIndex(
							repository, gitPath, DirCacheEntry.STAGE_3);
					String encoding = CompareCoreUtils.getResourceEncoding(
							repository, gitPath);
					right = new FileRevisionTypedElement(revision, encoding);
				} else
					right = CompareUtils.getFileRevisionTypedElement(gitPath,
							headCommit, repository);

				// can this really happen?
				if (right instanceof EmptyTypedElement)
					continue;

				IFileRevision rev;
				// if the file is not conflicting (as it was auto-merged)
				// we will show the auto-merged (local) version

				Path repositoryPath = new Path(repository.getWorkTree()
						.getAbsolutePath());
				IPath location = repositoryPath
						.append(fit.getEntryPathString());
				IFile file = ResourceUtil.getFileForLocation(location);
				if (!conflicting || useWorkspace) {
					if (file != null)
						rev = new LocalFileRevision(file);
					else
						rev = new WorkingTreeFileRevision(location.toFile());
				} else {
					rev = GitFileRevision.inIndex(repository, gitPath,
							DirCacheEntry.STAGE_2);
				}

				IRunnableContext runnableContext = getContainer();
				if (runnableContext == null)
					runnableContext = PlatformUI.getWorkbench().getProgressService();

				EditableRevision leftEditable;
				if (file != null)
					leftEditable = new ResourceEditableRevision(rev, file,
							runnableContext);
				else
					leftEditable = new LocationEditableRevision(rev, location,
							runnableContext);
				// make sure we don't need a round trip later
				try {
					leftEditable.cacheContents(monitor);
				} catch (CoreException e) {
					throw new IOException(e.getMessage());
				}

				int kind = Differencer.NO_CHANGE;
				if (conflicting)
					kind = Differencer.CONFLICTING;
				else if (modified)
					kind = Differencer.PSEUDO_CONFLICT;

				IDiffContainer fileParent = getFileParent(result,
						repositoryPath, file, location);

				ITypedElement anc;
				if (ancestorCommit != null)
					anc = CompareUtils.getFileRevisionTypedElement(gitPath,
							ancestorCommit, repository);
				else
					anc = null;
				// we get an ugly black icon if we have an EmptyTypedElement
				// instead of null
				if (anc instanceof EmptyTypedElement)
					anc = null;
				// create the node as child
				new DiffNode(fileParent, kind, anc, leftEditable, right);
			}
			return result;
		}
	}

	private IDiffContainer getFileParent(IDiffContainer root,
			IPath repositoryPath, IFile file, IPath location) {
		int projectSegment = -1;
		String projectName = null;
		if (file != null) {
			IProject project = file.getProject();
			IPath projectLocation = project.getLocation();
			if (projectLocation != null) {
				IPath projectPath = project.getLocation().makeRelativeTo(
						repositoryPath);
				projectSegment = projectPath.segmentCount() - 1;
				projectName = project.getName();
			}
		}

		IPath path = location.makeRelativeTo(repositoryPath);
		IDiffContainer child = root;
		for (int i = 0; i < path.segmentCount() - 1; i++) {
			if (i == projectSegment)
				child = getOrCreateChild(child, projectName, true);
			else
				child = getOrCreateChild(child, path.segment(i), false);
		}
		return child;
	}

	private DiffNode getOrCreateChild(IDiffContainer parent, final String name,
			final boolean projectMode) {
		for (IDiffElement child : parent.getChildren()) {
			if (child.getName().equals(name)) {
				return ((DiffNode) child);
			}
		}
		DiffNode child = new DiffNode(parent, Differencer.NO_CHANGE) {

			@Override
			public String getName() {
				return name;
			}

			@Override
			public Image getImage() {
				if (projectMode)
					return PROJECT_IMAGE;
				else
					return FOLDER_IMAGE;
			}
		};
		return child;
	}

	private String readFile(File directory, String fileName) throws IOException {
		byte[] content = IO.readFully(new File(directory, fileName));
		// strip off the last LF
		int end = content.length;
		while (0 < end && content[end - 1] == '\n')
			end--;
		return RawParseUtils.decode(content, 0, end);
	}

	private static class GitSynchronizationContext extends
			SubscriberMergeContext {
		public GitSynchronizationContext(Subscriber subscriber,
				ISynchronizationScopeManager scopeManager) {
			super(subscriber, scopeManager);
			initialize();
		}

		public void markAsMerged(IDiff node, boolean inSyncHint,
				IProgressMonitor monitor) throws CoreException {
			// Won't be used as a merging context
		}

		public void reject(IDiff diff, IProgressMonitor monitor)
				throws CoreException {
			// Won't be used as a merging context
		}

		@Override
		protected void makeInSync(IDiff diff, IProgressMonitor monitor)
				throws CoreException {
			// Won't be used as a merging context
		}
	}
}
