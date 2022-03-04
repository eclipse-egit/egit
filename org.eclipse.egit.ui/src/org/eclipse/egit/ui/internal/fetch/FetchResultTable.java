/*******************************************************************************
 * Copyright (c) 2009, 2022 Shawn O. Pearce and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.egit.core.UnitOfWork;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CommonUtils;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.WorkbenchStyledLabelProvider;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.lib.AbbrevConfig;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.notes.NoteMap;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.eclipse.jgit.util.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.OpenAndLinkWithEditorHelper;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.IWorkbenchAdapter3;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.progress.DeferredTreeContentManager;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Component displaying table with results of fetch operation.
 */
public class FetchResultTable {

	private static class CommitAdapter extends RepositoryCommit
			implements IDeferredWorkbenchAdapter, ISchedulingRule {

		private volatile FileDiff[] diffs;

		CommitAdapter(Repository repository, RevCommit commit) {
			super(repository, commit);
		}

		@Override
		public Object[] getChildren(Object o) {
			return diffs;
		}

		@Override
		public void fetchDeferredChildren(Object object,
				IElementCollector collector, IProgressMonitor monitor) {
			if (diffs != null) {
				return;
			}
			diffs = getDiffs();
			collector.add(diffs, monitor);
		}

		@Override
		public boolean isContainer() {
			return true;
		}

		@Override
		public ISchedulingRule getRule(Object object) {
			return this;
		}

		@Override
		public boolean contains(ISchedulingRule rule) {
			return this == rule;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return this == rule;
		}
	}

	private class FetchResultAdapter extends WorkbenchAdapter
			implements IDeferredWorkbenchAdapter, ISchedulingRule {

		private final TrackingRefUpdate update;

		private volatile Object[] children;

		private boolean loadingTriggered;

		public FetchResultAdapter(TrackingRefUpdate update) {
			this.update = update;
		}

		@Override
		public String getLabel(Object object) {
			return getStyledText(object).getString();
		}

		@Override
		public ImageDescriptor getImageDescriptor(Object object) {
			switch (update.getResult()) {
			case IO_FAILURE:
			case LOCK_FAILURE:
			case REJECTED_CURRENT_BRANCH:
			case REJECTED:
				return PlatformUI.getWorkbench().getSharedImages()
						.getImageDescriptor(ISharedImages.IMG_OBJS_ERROR_TSK);
			case FORCED:
				if (isPruned()) {
					ImageDescriptor icon = UIIcons.BRANCH;
					if (update.getLocalName().startsWith(Constants.R_TAGS))
						icon = UIIcons.TAG;
					if (update.getLocalName().startsWith(Constants.R_NOTES))
						icon = UIIcons.NOTE;
					return new DecorationOverlayIcon(icon,
							UIIcons.OVR_STAGED_REMOVE, IDecoration.TOP_RIGHT);
				}
				// else
				//$FALL-THROUGH$
			case RENAMED:
			case FAST_FORWARD:
				if (update.getRemoteName().startsWith(Constants.R_HEADS))
					return UIIcons.BRANCH;
				if (update.getLocalName().startsWith(Constants.R_TAGS))
					return UIIcons.TAG;
				if (update.getLocalName().startsWith(Constants.R_NOTES))
					return UIIcons.NOTE;
				break;
			case NEW:
				if (update.getRemoteName().startsWith(Constants.R_HEADS))
					return UIIcons.CREATE_BRANCH;
				if (update.getLocalName().startsWith(Constants.R_TAGS))
					return UIIcons.CREATE_TAG;
				if (update.getLocalName().startsWith(Constants.R_NOTES))
					return UIIcons.NOTE;
				break;
			default:
				break;
			}
			return super.getImageDescriptor(object);
		}

		private void addCommits(StyledString styled, String separator) {
			styled.append('[', StyledString.DECORATIONS_STYLER);
			styled.append(safeAbbreviate(update.getNewObjectId()),
					StyledString.DECORATIONS_STYLER);
			styled.append(separator, StyledString.DECORATIONS_STYLER);
			styled.append(safeAbbreviate(update.getOldObjectId()),
					StyledString.DECORATIONS_STYLER);
			styled.append(']', StyledString.DECORATIONS_STYLER);

			Object[] commits = getChildren(this);
			if (commits != null) {
				styled.append(
						MessageFormat.format(
								UIText.FetchResultTable_counterCommits,
								Integer.valueOf(commits.length)),
						StyledString.COUNTER_STYLER);
			} else if (!loadingTriggered) {
				// Careful here. We're creating the label, but the deferred
				// loading job calls getLabel again to include it in the job
				// name. So do this only once, otherwise we get an endless
				// recursion.
				loadingTriggered = true;
				((ITreeContentProvider) treeViewer.getContentProvider())
						.getChildren(this);
			}
		}

		@Override
		public Object[] getChildren(Object object) {
			return children;
		}

		@Override
		public void fetchDeferredChildren(Object object,
				IElementCollector collector, IProgressMonitor monitor) {
			if (children != null) {
				return;
			}
			switch (update.getResult()) {
			case FORCED:
				if (isPruned()) {
					children = NO_CHILDREN;
					return;
				}
				break;
			case FAST_FORWARD:
				break;
			default:
				children = NO_CHILDREN;
				return;
			}
			try (RevWalk walk = new RevWalk(repo)) {
				walk.setRetainBody(true);
				walk.markStart(walk.parseCommit(update.getNewObjectId()));
				walk.markUninteresting(
						walk.parseCommit(update.getOldObjectId()));
				List<CommitAdapter> commits = new ArrayList<>();
				for (RevCommit commit : walk) {
					if (monitor.isCanceled()) {
						break;
					}
					commits.add(new CommitAdapter(repo, commit));
				}
				children = commits.toArray();
				collector.add(children, monitor);
			} catch (IOException e) {
				Activator.logError("Error parsing commits from fetch result", //$NON-NLS-1$
						e);
				children = NO_CHILDREN;
			}
		}


		/**
		 * Shorten ref name
		 *
		 * @param ref
		 * @return shortened ref name
		 */
		protected String shortenRef(final String ref) {
			return NoteMap.shortenRefName(Repository.shortenRefName(ref));
		}

		@Override
		public StyledString getStyledText(Object object) {
			StyledString styled = new StyledString();
			final String remote = update.getRemoteName();
			final String local = update.getLocalName();
			styled.append(shortenRef(remote));
			styled.append(" : ", StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
			styled.append(shortenRef(local), StyledString.QUALIFIER_STYLER);
			styled.append(' ');
			switch (update.getResult()) {
			case LOCK_FAILURE:
				styled.append(UIText.FetchResultTable_statusLockFailure,
						StyledString.DECORATIONS_STYLER);
				break;
			case IO_FAILURE:
				styled.append(UIText.FetchResultTable_statusIOError,
						StyledString.DECORATIONS_STYLER);
				break;
			case NEW:
				if (remote.startsWith(Constants.R_HEADS))
					styled.append(UIText.FetchResultTable_statusNewBranch,
							StyledString.DECORATIONS_STYLER);
				else if (local.startsWith(Constants.R_TAGS))
					styled.append(UIText.FetchResultTable_statusNewTag,
							StyledString.DECORATIONS_STYLER);
				else
					styled.append(UIText.FetchResultTable_statusNew,
							StyledString.DECORATIONS_STYLER);
				break;
			case FORCED:
				if (isPruned())
					styled.append(UIText.FetchResultTable_statusPruned,
							StyledString.DECORATIONS_STYLER);
				else
					addCommits(styled, "..."); //$NON-NLS-1$
				break;
			case FAST_FORWARD:
				addCommits(styled, ".."); //$NON-NLS-1$
				break;
			case REJECTED:
				styled.append(UIText.FetchResultTable_statusRejected,
						StyledString.DECORATIONS_STYLER);
				break;
			case NO_CHANGE:
				styled.append(UIText.FetchResultTable_statusUpToDate,
						StyledString.DECORATIONS_STYLER);
				break;
			default:
				break;
			}
			return styled;
		}

		private boolean isPruned() {
			return update.getNewObjectId().equals(ObjectId.zeroId());
		}

		boolean isRemoteBranch(String branchName) {
			return update.getRemoteName().equals(branchName);
		}

		String getName() {
			String result = update.getRemoteName();
			if (StringUtils.isEmptyOrNull(result)) {
				result = update.getLocalName();
			}
			return result;
		}

		@Override
		public boolean isContainer() {
			switch (update.getResult()) {
			case FORCED:
				return !isPruned();
			case FAST_FORWARD:
				return true;
			default:
				return false;
			}
		}

		@Override
		public ISchedulingRule getRule(Object object) {
			return this;
		}

		@Override
		public boolean contains(ISchedulingRule rule) {
			return this == rule;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return this == rule;
		}
	}

	private final Composite treePanel;

	private final TreeViewer treeViewer;

	private Repository repo;

	private String remoteBranchOfCurrentBranch;

	private int oidLength = Constants.OBJECT_ID_ABBREV_STRING_LENGTH;

	/**
	 * Creates a new {@link FetchResultTable} under the given parent.
	 *
	 * @param parent
	 *            {@link Composite} to create the table in
	 */
	@SuppressWarnings("unused")
	public FetchResultTable(final Composite parent) {
		treePanel = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(treePanel);
		treeViewer = new TreeViewer(treePanel);
		treeViewer.setUseHashlookup(true);
		treeViewer.setAutoExpandLevel(1);

		addToolbar(treePanel);

		final IStyledLabelProvider styleProvider = new WorkbenchStyledLabelProvider() {

			@Override
			public StyledString getStyledText(Object element) {
				if (element instanceof IWorkbenchAdapter3) {
					return ((IWorkbenchAdapter3) element)
							.getStyledText(element);
				}
				return super.getStyledText(element);
			}
		};
		treeViewer.setLabelProvider(new DelegatingStyledCellLabelProvider(
				styleProvider) {

			@Override
			public String getToolTipText(final Object element) {
				if (element instanceof FetchResultAdapter) {
					switch (((FetchResultAdapter) element).update.getResult()) {
					case FAST_FORWARD:
						return UIText.FetchResultTable_statusDetailFastForward;
					case FORCED:
					case REJECTED:
						return UIText.FetchResultTable_statusDetailNonFastForward;
					case IO_FAILURE:
						return UIText.FetchResultTable_statusDetailIOError;
					case LOCK_FAILURE:
						return UIText.FetchResultTable_statusDetailCouldntLock;
					default:
						return super.getToolTipText(element);
					}
				}
				return super.getToolTipText(element);
			}

		});
		treeViewer.setComparator(new ViewerComparator() {

			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				// top level: branches and tags
				if (e1 instanceof FetchResultAdapter
						&& e2 instanceof FetchResultAdapter) {
					FetchResultAdapter f1 = (FetchResultAdapter) e1;
					FetchResultAdapter f2 = (FetchResultAdapter) e2;

					// currently checked out branch comes before other branches
					if (f1.isRemoteBranch(remoteBranchOfCurrentBranch)) {
						return -1;
					}
					if (f2.isRemoteBranch(remoteBranchOfCurrentBranch)) {
						return 1;
					}

					// otherwise sort by name
					return CommonUtils.STRING_ASCENDING_COMPARATOR
							.compare(f1.getName(), f2.getName());
				}

				// nested inside of branches: don't change commit order
				if (e1 instanceof CommitAdapter
						&& e2 instanceof CommitAdapter) {
					return 0;
				}

				// nested inside commits: sort by path
				if (e1 instanceof FileDiff && e2 instanceof FileDiff) {
					FileDiff f1 = (FileDiff) e1;
					FileDiff f2 = (FileDiff) e2;
					return f1.getPath().compareTo(f2.getPath());
				}

				return super.compare(viewer, e1, e2);
			}

		});
		ColumnViewerToolTipSupport.enableFor(treeViewer);
		final Tree tree = treeViewer.getTree();
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);

		treeViewer.setContentProvider(new BaseWorkbenchContentProvider() {

			private DeferredTreeContentManager loader = new DeferredTreeContentManager(
					treeViewer) {

				@Override
				protected void addChildren(Object parentItem, Object[] children,
						IProgressMonitor monitor) {
					WorkbenchJob updateJob = new WorkbenchJob(
							UIText.FetchResultTable_addingChildren) {

						@Override
						public IStatus runInUIThread(
								IProgressMonitor updateMonitor) {
							// Cancel the job if the tree viewer got closed
							if (treeViewer.getControl().isDisposed()
									|| updateMonitor.isCanceled()) {
								return Status.CANCEL_STATUS;
							}
							treeViewer.add(parentItem, children);
							// Update parent label to get the count
							treeViewer.update(parentItem, null);
							// If this is the top item in the viewer, expand it
							if (children.length > 0) {
								int topItems = tree.getItemCount();
								if (topItems > 0 && parentItem == tree
										.getItem(0).getData()) {
									treeViewer.expandToLevel(parentItem, 1,
											true);
								}
							}
							return Status.OK_STATUS;
						}
					};
					updateJob.setSystem(true);
					updateJob.schedule();
				}
			};

			private Object currentInput;

			@Override
			public void dispose() {
				if (loader != null && currentInput != null) {
					loader.cancel(currentInput);
				}
				currentInput = null;
				loader = null;
				super.dispose();
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				if (loader != null && oldInput != null) {
					loader.cancel(oldInput);
				}
				currentInput = newInput;
				super.inputChanged(viewer, oldInput, newInput);
			}

			@Override
			public Object[] getElements(Object inputElement) {
				if (!(inputElement instanceof FetchResult)) {
					return new Object[0];
				}
				FetchResult result = (FetchResult) inputElement;
				return result.getTrackingRefUpdates().stream()
						.map(FetchResultAdapter::new).toArray();
			}

			@Override
			public Object[] getChildren(Object element) {
				Object[] children = super.getChildren(element);
				if (children != null) {
					return children;
				}
				if (loader == null) {
					return new Object[0];
				}
				return loader.getChildren(element);
			}

			@Override
			public boolean hasChildren(Object element) {
				if (element instanceof IDeferredWorkbenchAdapter) {
					return ((IDeferredWorkbenchAdapter) element).isContainer();
				}
				return super.hasChildren(element);
			}
		});

		new OpenAndLinkWithEditorHelper(treeViewer) {

			@Override
			protected void linkToEditor(ISelection selection) {
				// Not supported
			}

			@Override
			protected void open(ISelection selection, boolean activate) {
				handleOpen(selection, OpenStrategy.activateOnOpen());
			}

			@Override
			protected void activate(ISelection selection) {
				handleOpen(selection, true);
			}

			private void handleOpen(ISelection selection, boolean activateOnOpen) {
				if (selection instanceof IStructuredSelection) {
					for (Object element : (IStructuredSelection) selection) {
						if (element instanceof RepositoryCommit) {
							CommitEditor.openQuiet((RepositoryCommit) element, activateOnOpen);
						}
					}
				}
			}
		};
	}

	private void addToolbar(Composite parent) {
		ToolBar toolbar = new ToolBar(parent, SWT.VERTICAL);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(toolbar);
		UIUtils.addExpansionItems(toolbar, treeViewer);
	}

	/**
	 * Sets the {@link FetchResult} to show.
	 *
	 * @param db
	 *            {@link Repository} the fetch was done in
	 * @param fetchResult
	 *            {@link FetchResult} describing the outcome of the fetch
	 *            operation
	 */
	public void setData(final Repository db, final FetchResult fetchResult) {
		treeViewer.setSelection(StructuredSelection.EMPTY);
		treeViewer.setInput(null);
		repo = db;
		UnitOfWork.execute(db, () -> {
			try {
				oidLength = AbbrevConfig.parseFromConfig(db).get();
			} catch (InvalidConfigurationException e) {
				Activator.logError(e.getLocalizedMessage(), e);
				oidLength = Constants.OBJECT_ID_ABBREV_STRING_LENGTH;
			}
			try {
				String branch = repo.getBranch();
				if (branch != null) {
					remoteBranchOfCurrentBranch = repo.getConfig().getString(
							ConfigConstants.CONFIG_BRANCH_SECTION, branch,
							ConfigConstants.CONFIG_KEY_MERGE);
				}
			} catch (IOException e) {
				remoteBranchOfCurrentBranch = null;
			}
		});
		treeViewer.setInput(fetchResult);
	}

	private String safeAbbreviate(ObjectId objectId) {
		return objectId.name().substring(0, oidLength);
	}

	/**
	 * Retrieves the top-level {@link Control} of this {@link FetchResultTable}.
	 *
	 * @return the control
	 */
	public Control getControl() {
		return treePanel;
	}

}
