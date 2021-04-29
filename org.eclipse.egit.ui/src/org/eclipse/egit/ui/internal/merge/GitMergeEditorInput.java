/*******************************************************************************
 * Copyright (C) 2010, 2021 Mathias Kinzler <mathias.kinzler@sap.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.merge;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.RevUtils;
import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.efs.EgitFileSystem;
import org.eclipse.egit.core.internal.efs.HiddenResources;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCache;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffCacheEntry;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.revision.EditableRevision;
import org.eclipse.egit.ui.internal.revision.FileRevisionTypedElement;
import org.eclipse.egit.ui.internal.revision.GitCompareFileRevisionEditorInput.EmptyTypedElement;
import org.eclipse.egit.ui.internal.revision.ResourceEditableRevision;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jgit.api.MergeCommand.ConflictStyle;
import org.eclipse.jgit.attributes.Attribute;
import org.eclipse.jgit.attributes.Attributes;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEditor.PathEdit;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ui.synchronize.EditableSharedDocumentAdapter.ISharedDocumentAdapterListener;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;

/**
 * A Git-specific {@link CompareEditorInput} for merging conflicting files.
 */
@SuppressWarnings("restriction")
public class GitMergeEditorInput extends CompareEditorInput {
	private static final String LABELPATTERN = "{0} - {1}"; //$NON-NLS-1$

	private static final Image FOLDER_IMAGE = PlatformUI.getWorkbench()
			.getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

	private static final Image PROJECT_IMAGE = PlatformUI.getWorkbench()
			.getSharedImages().getImage(SharedImages.IMG_OBJ_PROJECT);

	private final boolean useWorkspace;

	private final boolean useOurs;

	private final IPath[] locations;

	private List<IFile> toDelete;

	/**
	 * Creates a new {@link GitMergeEditorInput}.
	 *
	 * @param mode
	 *            defining what to use as input for the logical left side
	 * @param locations
	 *            as selected by the user
	 */
	public GitMergeEditorInput(MergeInputMode mode, IPath... locations) {
		super(new CompareConfiguration());
		this.useWorkspace = !MergeInputMode.STAGE_2.equals(mode);
		this.useOurs = MergeInputMode.MERGED_OURS.equals(mode);
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
				IResource resource = null;
				if (element instanceof HiddenResourceTypedElement) {
					resource = ((HiddenResourceTypedElement) element)
							.getRealFile();
				}
				if (resource == null && element instanceof IResourceProvider) {
					resource = ((IResourceProvider) element).getResource();
				}
				if (resource != null && adapter.isInstance(resource)) {
					return resource;
				}
			}
		}
		return super.getAdapter(adapter);
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
		// We do NOT dispose the images, as these are shared.
		//
		// We need to remove the temporary resources. A CompareEditorInput is
		// supposed to be the very last thing that is disposed in a compare
		// viewer, but this is not always true. If content merge viewers add
		// additional widgets, for instance for the Java structure comparison,
		// we're suddenly no longer the last item to be disposed. The various
		// viewers (left, right, structure, and so on) are all disposed when
		// their widgets are disposed. Widget disposal happens recursively
		// top-down on the UI thread, so an asyncExec should be safe here to
		// ensure that we remove the files only once everything else has been
		// disposed of. If we delete temporary resources before all viewers had
		// disconnected the Document, some might not disconnect because
		// SharedDocumentAdapter.getDocumentKey() returns null if the file has
		// been deleted. If this happens the framework will find that still
		// connected document the next time this resource is opened and show
		// that instead of the true resource contents. This is wrong and is very
		// annoying if this cached document is dirty: one can open only this
		// dirty version from then on, until the next restart of Eclipse.
		PlatformUI.getWorkbench().getDisplay().asyncExec(this::cleanUp);
	}

	private void cleanUp() {
		if (toDelete == null || toDelete.isEmpty()) {
			return;
		}
		List<IFile> toClean = toDelete;
		toDelete = null;
		// Don't clean up if the workbench is shutting down; we would exit with
		// unsaved workspace changes. Instead, EGit core cleans the project on
		// start.
		Job job = new Job(UIText.GitMergeEditorInput_ResourceCleanupJobName) {

			@Override
			public boolean shouldSchedule() {
				return super.shouldSchedule()
						&& !PlatformUI.getWorkbench().isClosing();
			}

			@Override
			public boolean shouldRun() {
				return super.shouldRun()
						&& !PlatformUI.getWorkbench().isClosing();
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IWorkspaceRunnable remove = m -> {
					SubMonitor progress = SubMonitor.convert(m, toClean.size());
					for (IFile tmp : toClean) {
						if (PlatformUI.getWorkbench().isClosing()) {
							return;
						}
						try {
							tmp.delete(true, progress.newChild(1));
						} catch (CoreException e) {
							// Ignore
						}
					}
				};
				try {
					ResourcesPlugin.getWorkspace().run(remove, null,
							IWorkspace.AVOID_UPDATE, monitor);
				} catch (CoreException e) {
					return e.getStatus();
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.setUser(false);
		job.schedule();
	}

	private static boolean isUIThread() {
		return Display.getCurrent() != null;
	}

	@Override
	protected Object prepareInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		// make sure all resources belong to the same repository
		RevWalk rw = null;
		try {
			monitor.beginTask(
					UIText.GitMergeEditorInput_CheckingResourcesTaskName,
					IProgressMonitor.UNKNOWN);

			Map<Repository, Collection<String>> pathsByRepository = ResourceUtil
					.splitPathsByRepository(Arrays.asList(locations));
			if (pathsByRepository.size() != 1) {
				throw new InvocationTargetException(
						new IllegalStateException(
								UIText.RepositoryAction_multiRepoSelection));
			}
			Entry<Repository, Collection<String>> entry = pathsByRepository
					.entrySet().iterator().next();
			Repository repo = entry.getKey();
			List<String> filterPaths = new ArrayList<>(entry.getValue());

			if (monitor.isCanceled())
				throw new InterruptedException();

			rw = new RevWalk(repo);

			// get the "right" side
			final RevCommit rightCommit;
			try {
				rightCommit = RevUtils.getTheirs(repo, rw);
			} catch (IOException e) {
				throw new InvocationTargetException(e);
			}

			// we need the HEAD, also to determine the common
			// ancestor
			final RevCommit headCommit;
			try {
				ObjectId head = repo.resolve(Constants.HEAD);
				if (head == null)
					throw new IOException(NLS.bind(
							CoreText.ValidationUtils_CanNotResolveRefMessage,
							Constants.HEAD));
				headCommit = rw.parseCommit(head);
			} catch (IOException e) {
				throw new InvocationTargetException(e);
			}

			final String fullBranch;
			try {
				fullBranch = repo.getFullBranch();
			} catch (IOException e) {
				throw new InvocationTargetException(e);
			}

			// try to obtain the common ancestor
			RevCommit ancestorCommit = null;
			boolean unknownAncestor = false;
			switch (repo.getRepositoryState()) {
			case CHERRY_PICKING:
			case REBASING_INTERACTIVE:
			case REBASING_MERGE:
				if (rightCommit.getParentCount() == 1) {
					try {
						ancestorCommit = rw
								.parseCommit(rightCommit.getParent(0));
					} catch (IOException e) {
						unknownAncestor = true;
					}
				} else {
					// Cherry-pick of a merge commit -- git doesn't record the
					// mainline index anywhere, so we don't know which parent
					// was taken.
					unknownAncestor = true;
				}
				break;
			default:
				List<RevCommit> startPoints = new ArrayList<>();
				rw.setRevFilter(RevFilter.MERGE_BASE);
				startPoints.add(rightCommit);
				startPoints.add(headCommit);
				try {
					rw.markStart(startPoints);
					ancestorCommit = rw.next();
				} catch (Exception e) {
					// Ignore; ancestor remains null
				}
				break;
			}

			if (monitor.isCanceled()) {
				throw new InterruptedException();
			}
			// set the labels
			CompareConfiguration config = getCompareConfiguration();
			config.setRightLabel(NLS.bind(LABELPATTERN, rightCommit
					.getShortMessage(), CompareUtils.truncatedRevision(rightCommit.name())));

			if (!useWorkspace) {
				config.setLeftLabel(NLS.bind(LABELPATTERN, headCommit
						.getShortMessage(), CompareUtils.truncatedRevision(headCommit.name())));
			} else if (useOurs) {
				config.setLeftLabel(
						UIText.GitMergeEditorInput_WorkspaceOursHeader);
			} else {
				config.setLeftLabel(UIText.GitMergeEditorInput_WorkspaceHeader);
			}
			if (ancestorCommit != null) {
				config.setAncestorLabel(NLS.bind(LABELPATTERN, ancestorCommit
						.getShortMessage(), CompareUtils.truncatedRevision(ancestorCommit.name())));
			} else if (unknownAncestor) {
				config.setAncestorLabel(NLS.bind(
						UIText.GitMergeEditorInput_AncestorUnknownHeader,
						CompareUtils.truncatedRevision(rightCommit.name())));
			}
			// set title and icon
			setTitle(NLS.bind(UIText.GitMergeEditorInput_MergeEditorTitle,
					new Object[] {
							RepositoryUtil.getInstance()
									.getRepositoryName(repo),
							rightCommit.getShortMessage(), fullBranch }));

			// build the nodes
			try {
				return buildDiffContainer(repo, headCommit,
						ancestorCommit, filterPaths, rw, monitor);
			} catch (IOException e) {
				throw new InvocationTargetException(e);
			}
		} finally {
			if (rw != null)
				rw.dispose();
			monitor.done();
		}
	}

	@SuppressWarnings("unused")
	private IDiffContainer buildDiffContainer(Repository repository,
			RevCommit headCommit, RevCommit ancestorCommit,
			List<String> filterPaths, RevWalk rw, IProgressMonitor monitor)
			throws IOException, InterruptedException {

		monitor.setTaskName(UIText.GitMergeEditorInput_CalculatingDiffTaskName);
		IDiffContainer result = new DiffNode(Differencer.CONFLICTING);

		ConflictStyle style = null;
		try (TreeWalk tw = new TreeWalk(repository)) {
			int dirCacheIndex = tw.addTree(new DirCacheIterator(repository
					.readDirCache()));
			FileTreeIterator fIter = new FileTreeIterator(repository);
			int fileTreeIndex = tw.addTree(fIter);
			fIter.setDirCacheIterator(tw, dirCacheIndex);
			int repositoryTreeIndex = tw.addTree(rw.parseTree(repository
					.resolve(Constants.HEAD)));

			// filter by selected resources
			if (!filterPaths.isEmpty()) {
				if (filterPaths.size() > 1) {
					tw.setFilter(
							PathFilterGroup.createFromStrings(filterPaths));
				} else {
					String path = filterPaths.get(0);
					if (!path.isEmpty()) {
						tw.setFilter(PathFilterGroup.createFromStrings(path));
					}
				}
			}
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
				String encoding = null;
				if (conflicting) {
					GitFileRevision revision = GitFileRevision.inIndex(
							repository, gitPath, DirCacheEntry.STAGE_3);
					encoding = CompareCoreUtils.getResourceEncoding(repository,
							gitPath);
					right = new FileRevisionTypedElement(revision, encoding);
				} else {
					right = CompareUtils.getFileRevisionTypedElement(gitPath,
							headCommit, repository);
				}
				// can this really happen?
				if (right instanceof EmptyTypedElement) {
					continue;
				}
				ITypedElement left;
				IFileRevision rev;
				// if the file is not conflicting (as it was auto-merged)
				// we will show the auto-merged (local) version

				Path repositoryPath = new Path(repository.getWorkTree()
						.getAbsolutePath());
				IPath location = repositoryPath.append(gitPath);
				assert location != null;
				IFile file = ResourceUtil.getFileForLocation(location, false);
				boolean useWorkingTree = !conflicting || useWorkspace;
				if (!useWorkingTree && conflicting && dirCacheEntry != null) {
					// Normal conflict stages have a zero timestamp. If it's not
					// zero, we marked it below when the content was saved to
					// the working tree file in an earlier merge editor.
					useWorkingTree = !Instant.EPOCH
							.equals(dirCacheEntry.getLastModifiedInstant());
				}
				if (useWorkingTree) {
					boolean useOursFilter = conflicting && useOurs;
					int conflictMarkerSize = 7; // Git default
					if (useOursFilter) {
						Attributes attributes = tw.getAttributes();
						useOursFilter = attributes.canBeContentMerged();
						if (useOursFilter) {
							Attribute markerSize = attributes
									.get("conflict-marker-size"); //$NON-NLS-1$
							if (markerSize != null && Attribute.State.CUSTOM
									.equals(markerSize.getState())) {
								try {
									conflictMarkerSize = Integer
											.parseUnsignedInt(
													markerSize.getValue());
								} catch (NumberFormatException e) {
									// Ignore
								}
							}
						}
					}
					LocalResourceTypedElement item;
					if (useOursFilter) {
						if (style == null) {
							style = repository.getConfig().getEnum(
									ConfigConstants.CONFIG_MERGE_SECTION, null,
									ConfigConstants.CONFIG_KEY_CONFLICTSTYLE,
									ConflictStyle.MERGE);
						}
						boolean useDiff3Style = ConflictStyle.DIFF3
								.equals(style);
						String mode = (useDiff3Style ? 'O' : 'o')
								+ Integer.toString(conflictMarkerSize);
						URI uri = EgitFileSystem.createURI(repository, gitPath,
								"WORKTREE:" + mode); //$NON-NLS-1$
						Charset rscEncoding = null;
						if (file != null) {
							String encodingName = CompareCoreUtils
									.getResourceEncoding(file);
							try {
								rscEncoding = Charset.forName(encodingName);
							} catch (IllegalArgumentException e) {
								// Ignore here; use default.
							}
						}
						item = createWithHiddenResource(
								uri, tw.getNameString(), file, rscEncoding);
						if (file != null) {
							item.setSharedDocumentListener(
									new LocalResourceSaver(item) {

										@Override
										protected void save()
												throws CoreException {
											super.save();
											file.refreshLocal(
													IResource.DEPTH_ZERO, null);
										}
									});
						} else {
							item.setSharedDocumentListener(
									new LocalResourceSaver(item));
						}
					} else {
						if (file != null) {
							item = new LocalResourceTypedElement(file);
						} else {
							item = createWithHiddenResource(
									location.toFile().toURI(),
									tw.getNameString(), null, null);
						}
						item.setSharedDocumentListener(
								new LocalResourceSaver(item));
					}
					left = item;
				} else {
					IFile rsc = file != null ? file
							: createHiddenResource(location.toFile().toURI(),
									tw.getNameString(), null);
					assert rsc != null;
					// Stage 2 from index with backing IResource
					rev = GitFileRevision.inIndex(repository, gitPath,
							DirCacheEntry.STAGE_2);
					IRunnableContext runnableContext = getContainer();
					if (runnableContext == null) {
						runnableContext = PlatformUI.getWorkbench()
								.getProgressService();
						assert runnableContext != null;
					}
					left = new ResourceEditableRevision(rev, rsc,
							runnableContext);
					// 'left' saves to the working tree. Update the index entry
					// with the current time. Normal conflict stages have a
					// timestamp of zero, so this is a non-invasive fully
					// compatible way to mark this conflict stage so that the
					// next time we do take the file contents.
					((EditableRevision) left).addContentChangeListener(
							source -> updateIndexTimestamp(repository,
									gitPath));
					// make sure we don't need a round trip later
					try {
						((EditableRevision) left).cacheContents(monitor);
					} catch (CoreException e) {
						throw new IOException(e.getMessage(), e);
					}
				}

				int kind = Differencer.NO_CHANGE;
				if (conflicting) {
					kind = Differencer.CONFLICTING;
				} else if (modified) {
					kind = Differencer.PSEUDO_CONFLICT;
				}
				IDiffContainer fileParent = getFileParent(result,
						repositoryPath, file, location);

				ITypedElement ancestor = null;
				if (ancestorCommit != null) {
					ancestor = CompareUtils.getFileRevisionTypedElement(gitPath,
							ancestorCommit, repository);
					// we get an ugly black icon if we have an EmptyTypedElement
					// instead of null
					if (ancestor instanceof EmptyTypedElement) {
						ancestor = null;
					}
				} else if (conflicting) {
					GitFileRevision revision = GitFileRevision.inIndex(
							repository, gitPath, DirCacheEntry.STAGE_1);
					if (encoding == null) {
						encoding = CompareCoreUtils
								.getResourceEncoding(repository, gitPath);
					}
					ancestor = new FileRevisionTypedElement(revision, encoding);
				}
				// create the node as child
				new DiffNode(fileParent, kind, ancestor, left, right);
			}
			return result;
		} catch (URISyntaxException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	private LocalResourceTypedElement createWithHiddenResource(URI uri,
			String name, IFile file, Charset encoding) throws IOException {
		IFile tmp = createHiddenResource(uri, name, encoding);
		return new HiddenResourceTypedElement(tmp, file);
	}

	private IFile createHiddenResource(URI uri, String name, Charset encoding)
			throws IOException {
		try {
			IFile tmp = HiddenResources.INSTANCE.createFile(uri, name, encoding,
					null);
			if (toDelete == null) {
				toDelete = new ArrayList<>();
			}
			toDelete.add(tmp);
			return tmp;
		} catch (CoreException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	private void updateIndexTimestamp(Repository repository, String gitPath) {
		DirCache cache = null;
		try {
			cache = repository.lockDirCache();
			DirCacheEditor editor = cache.editor();
			editor.add(new PathEdit(gitPath) {

				private boolean done;

				@Override
				public void apply(DirCacheEntry ent) {
					if (!done && ent.getStage() > 0) {
						ent.setLastModified(Instant.now());
						done = true;
					}
				}
			});
			editor.commit();
		} catch (IOException e) {
			Activator.logError(MessageFormat.format(
					UIText.GitMergeEditorInput_ErrorUpdatingIndex, gitPath), e);
		} finally {
			if (cache != null) {
				cache.unlock();
			}
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

	@Override
	public boolean canRunAsJob() {
		return true;
	}

	private static class LocalResourceSaver
			implements ISharedDocumentAdapterListener {

		LocalResourceTypedElement element;

		public LocalResourceSaver(LocalResourceTypedElement element) {
			this.element = element;
		}

		protected void save() throws CoreException {
			element.saveDocument(true, null);
			refreshIndexDiff();
		}

		private void refreshIndexDiff() {
			IResource resource = element.getResource();
			if (resource != null && HiddenResources.INSTANCE
					.isHiddenProject(resource.getProject())) {
				String gitPath = null;
				Repository repository = null;
				URI uri = resource.getLocationURI();
				if (EFS.SCHEME_FILE.equals(uri.getScheme())) {
					IPath location = new Path(uri.getSchemeSpecificPart());
					repository = ResourceUtil.getRepository(location);
					if (repository != null) {
						location = ResourceUtil.getRepositoryRelativePath(
								location, repository);
						if (location != null) {
							gitPath = location.toPortableString();
						}
					}
				} else {
					repository = HiddenResources.INSTANCE.getRepository(uri);
					if (repository != null) {
						gitPath = HiddenResources.INSTANCE.getGitPath(uri);
					}
				}
				if (gitPath != null && repository != null) {
					IndexDiffCacheEntry indexDiffCacheForRepository = IndexDiffCache
							.getInstance().getIndexDiffCacheEntry(repository);
					if (indexDiffCacheForRepository != null) {
						indexDiffCacheForRepository.refreshFiles(
								Collections.singletonList(gitPath));
					}
				}
			}
		}

		@Override
		public void handleDocumentConnected() {
			// Nothing
		}

		@Override
		public void handleDocumentDisconnected() {
			// Nothing
		}

		@Override
		public void handleDocumentFlushed() {
			try {
				save();
			} catch (CoreException e) {
				Activator.handleStatus(e.getStatus(), true);
			}
		}

		@Override
		public void handleDocumentDeleted() {
			// Nothing
		}

		@Override
		public void handleDocumentSaved() {
			// Nothing
		}
	}

	private static class HiddenResourceTypedElement
			extends LocalResourceTypedElement {

		private final IFile realFile;

		public HiddenResourceTypedElement(IFile file, IFile realFile) {
			super(file);
			this.realFile = realFile;
		}

		public IFile getRealFile() {
			return realFile;
		}
	}
}
