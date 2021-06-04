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
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.contentmergeviewer.ContentMergeViewer;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.egit.core.RepositoryUtil;
import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.egit.core.internal.efs.EgitFileSystem;
import org.eclipse.egit.core.internal.storage.GitFileRevision;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.util.RevCommitUtils;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.revision.EditableRevision;
import org.eclipse.egit.ui.internal.revision.FileRevisionTypedElement;
import org.eclipse.egit.ui.internal.revision.GitCompareFileRevisionEditorInput.EmptyTypedElement;
import org.eclipse.egit.ui.internal.revision.ResourceEditableRevision;
import org.eclipse.egit.ui.internal.synchronize.compare.LocalNonWorkspaceTypedElement;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.Viewer;
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
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.ui.PlatformUI;

/**
 * A Git-specific {@link CompareEditorInput} for merging conflicting files.
 */
@SuppressWarnings("restriction")
public class GitMergeEditorInput extends AbstractGitMergeEditorInput {

	private static final String LABELPATTERN = "{0} - {1}"; //$NON-NLS-1$

	private final MergeInputMode mode;

	private final boolean useWorkspace;

	private final boolean useOurs;

	private CompareEditorInputViewerAction toggleCurrentChanges;

	/**
	 * Creates a new {@link GitMergeEditorInput}.
	 *
	 * @param mode
	 *            defining what to use as input for the logical left side
	 * @param locations
	 *            as selected by the user
	 */
	public GitMergeEditorInput(MergeInputMode mode, IPath... locations) {
		super(null, locations);
		this.useWorkspace = !MergeInputMode.STAGE_2.equals(mode);
		this.useOurs = MergeInputMode.MERGED_OURS.equals(mode);
		this.mode = mode;
		CompareConfiguration config = getCompareConfiguration();
		config.setLeftEditable(true);
	}

	@Override
	public int hashCode() {
		return 31 * super.hashCode() + Objects.hash(mode);
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj)) {
			return false;
		}
		GitMergeEditorInput other = (GitMergeEditorInput) obj;
		return mode == other.mode;
	}

	@Override
	protected void initActions(ToolBarManager manager, Viewer newViewer,
			ICompareInput input) {
		super.initActions(manager, newViewer, input);
		setToggleCurrentChangesAction(manager, newViewer, input);
	}

	private void setToggleCurrentChangesAction(ToolBarManager manager,
			Viewer newViewer, ICompareInput input) {
		boolean isApplicable = newViewer instanceof ContentMergeViewer
				&& input instanceof MergeDiffNode
				&& input.getAncestor() != null;
		setAction(manager, newViewer, isApplicable,
				ToggleCurrentChangesAction.COMMAND_ID,
				create -> {
					if (toggleCurrentChanges == null && create) {
						toggleCurrentChanges = new ToggleCurrentChangesAction(
								UIText.GitMergeEditorInput_ToggleCurrentChangesLabel,
								this);
						toggleCurrentChanges
								.setId(ToggleCurrentChangesAction.COMMAND_ID);
					}
					return toggleCurrentChanges;
				});
	}

	@Override
	protected void disposeActions() {
		if (toggleCurrentChanges != null) {
			toggleCurrentChanges.dispose();
			toggleCurrentChanges = null;
		}
		super.disposeActions();
	}

	@Override
	protected Object buildInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		RevWalk rw = null;
		try {
			Repository repo = getRepository();

			rw = new RevWalk(repo);

			// get the "right" side
			final RevCommit rightCommit;
			try {
				rightCommit = RevCommitUtils.getTheirs(repo, rw);
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

			CompareConfiguration config = getCompareConfiguration();
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
				if (!MergeInputMode.WORKTREE.equals(mode)) {
					// Do not suppress any changes on the left if the input is
					// the possibly pre-merged working tree version. Conflict
					// markers exist only on the left; they would not be shown
					// as differences, and are then too easy to miss.
					config.setChangeIgnored(
							config.isMirrored() ? RangeDifference.RIGHT
									: RangeDifference.LEFT,
							true);
					config.setChangeIgnored(RangeDifference.ANCESTOR, true);
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
				return buildDiffContainer(repo, headCommit, ancestorCommit, rw,
						monitor);
			} catch (IOException e) {
				throw new InvocationTargetException(e);
			}
		} finally {
			if (rw != null) {
				rw.dispose();
			}
		}
	}

	@SuppressWarnings("unused")
	private IDiffContainer buildDiffContainer(Repository repository,
			RevCommit headCommit, RevCommit ancestorCommit, RevWalk rw,
			IProgressMonitor monitor)
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
			Collection<String> filterPaths = getFilterPaths();
			if (!filterPaths.isEmpty()) {
				if (filterPaths.size() > 1) {
					tw.setFilter(
							PathFilterGroup.createFromStrings(filterPaths));
				} else {
					String path = filterPaths.iterator().next();
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
						String filter = (useDiff3Style ? 'O' : 'o')
								+ Integer.toString(conflictMarkerSize);
						URI uri = EgitFileSystem.createURI(repository, gitPath,
								"WORKTREE:" + filter); //$NON-NLS-1$
						Charset rscEncoding = null;
						if (file != null) {
							if (encoding == null) {
								encoding = CompareCoreUtils
										.getResourceEncoding(file);
							}
							try {
								rscEncoding = Charset.forName(encoding);
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
							item = new LocalNonWorkspaceTypedElement(repository,
									location);
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
					kind = Differencer.CONFLICTING + Differencer.CHANGE;
				} else if (modified) {
					kind = Differencer.LEFT + Differencer.ADDITION;
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
				new MergeDiffNode(fileParent, kind, ancestor, left, right);
			}
			return result;
		} catch (URISyntaxException e) {
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
}
