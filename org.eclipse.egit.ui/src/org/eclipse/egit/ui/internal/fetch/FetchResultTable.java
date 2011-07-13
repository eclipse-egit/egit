package org.eclipse.egit.ui.internal.fetch;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.WorkbenchStyledLabelProvider;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.notes.NoteMap;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchContentProvider;

/**
 * Component displaying table with results of fetch operation.
 */
class FetchResultTable {

	private class FetchResultAdapter extends WorkbenchAdapter {

		private final TrackingRefUpdate update;

		private Object[] children;

		public FetchResultAdapter(TrackingRefUpdate update) {
			this.update = update;
		}

		public String getLabel(Object object) {
			return getStyledText(object).getString();
		}

		public ImageDescriptor getImageDescriptor(Object object) {
			switch (update.getResult()) {
			case IO_FAILURE:
			case LOCK_FAILURE:
			case REJECTED_CURRENT_BRANCH:
			case REJECTED:
				return PlatformUI.getWorkbench().getSharedImages()
						.getImageDescriptor(ISharedImages.IMG_OBJS_ERROR_TSK);
			case FORCED:
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
				return super.getImageDescriptor(object);
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

			styled.append(MessageFormat.format(
					UIText.FetchResultTable_counterCommits,
					Integer.valueOf(getChildren(this).length)),
					StyledString.COUNTER_STYLER);
		}

		public Object[] getChildren(Object object) {
			if (children != null)
				return children;

			switch (update.getResult()) {
			case FORCED:
			case FAST_FORWARD:
				RevWalk walk = new RevWalk(reader);
				try {
					walk.setRetainBody(true);
					walk.markStart(walk.parseCommit(update.getNewObjectId()));
					walk.markUninteresting(walk.parseCommit(update
							.getOldObjectId()));
					List<RepositoryCommit> commits = new ArrayList<RepositoryCommit>();
					for (RevCommit commit : walk)
						commits.add(new RepositoryCommit(repo, commit));
					children = commits.toArray();
					break;
				} catch (IOException e) {
					Activator.logError(
							"Error parsing commits from fetch result", e); //$NON-NLS-1$
				} finally {
					walk.release();
				}
				//$FALL-THROUGH$
			default:
				children = super.getChildren(object);
			}
			return children;
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
	}

	private final Composite treePanel;

	private final TreeViewer treeViewer;

	private Repository repo;

	private ObjectReader reader;

	private Map<ObjectId, String> abbrevations;

	FetchResultTable(final Composite parent) {
		treePanel = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(treePanel);
		treeViewer = new TreeViewer(treePanel);
		treeViewer.setAutoExpandLevel(2);

		addToolbar(treePanel);

		final IStyledLabelProvider styleProvider = new WorkbenchStyledLabelProvider() {

			public StyledString getStyledText(Object element) {
				// TODO Replace with use of IWorkbenchAdapter3 when is no longer
				// supported
				if (element instanceof FetchResultAdapter)
					return ((FetchResultAdapter) element)
							.getStyledText(element);
				if (element instanceof RepositoryCommit)
					return ((RepositoryCommit) element).getStyledText(element);

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
		treeViewer.setSorter(new ViewerSorter() {

			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof FetchResultAdapter
						&& e2 instanceof FetchResultAdapter) {
					FetchResultAdapter f1 = (FetchResultAdapter) e1;
					FetchResultAdapter f2 = (FetchResultAdapter) e2;
					if (f1.getChildren(f1).length > 0
							&& f2.getChildren(f2).length == 0)
						return 1;
					if (f1.getChildren(f1).length == 0
							&& f2.getChildren(f2).length > 0)
						return -1;

					return f1.getLabel(f1).compareToIgnoreCase(f2.getLabel(f2));
				}

				// Leave commits order alone
				if (e1 instanceof RepositoryCommit
						&& e2 instanceof RepositoryCommit)
					return 0;

				return super.compare(viewer, e1, e2);
			}

		});
		ColumnViewerToolTipSupport.enableFor(treeViewer);
		final Tree tree = treeViewer.getTree();
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);
		tree.setLinesVisible(true);

		treePanel.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (reader != null)
					reader.release();
			}
		});

		treeViewer.setContentProvider(new WorkbenchContentProvider() {

			public Object[] getElements(Object inputElement) {
				if (inputElement == null)
					return new FetchResultAdapter[0];

				final FetchResult result = (FetchResult) inputElement;
				TrackingRefUpdate[] updates = result.getTrackingRefUpdates()
						.toArray(new TrackingRefUpdate[0]);
				FetchResultAdapter[] elements = new FetchResultAdapter[updates.length];
				for (int i = 0; i < elements.length; i++)
					elements[i] = new FetchResultAdapter(updates[i]);
				return elements;
			}

			public Object[] getChildren(Object element) {
				if (element instanceof RepositoryCommit)
					return ((RepositoryCommit) element).getDiffs();
				return super.getChildren(element);
			}

		});

		treeViewer.addOpenListener(new IOpenListener() {

			public void open(OpenEvent event) {
				ISelection selection = event.getSelection();
				if (selection instanceof IStructuredSelection)
					for (Object element : ((IStructuredSelection) selection)
							.toArray())
						if (element instanceof RepositoryCommit)
							CommitEditor.openQuiet((RepositoryCommit) element);
			}
		});
	}

	private void addToolbar(Composite parent) {
		ToolBar toolbar = new ToolBar(parent, SWT.VERTICAL);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(toolbar);
		UIUtils.addExpandsionItems(toolbar, treeViewer);
	}

	void setData(final Repository db, final FetchResult fetchResult) {
		treeViewer.setInput(null);
		repo = db;
		reader = db.newObjectReader();
		abbrevations = new HashMap<ObjectId, String>();
		treeViewer.setInput(fetchResult);
	}

	private String safeAbbreviate(ObjectId id) {
		String abbrev = abbrevations.get(id);
		if (abbrev == null) {
			try {
				abbrev = reader.abbreviate(id).name();
			} catch (IOException cannotAbbreviate) {
				abbrev = id.name();
			}
			abbrevations.put(id, abbrev);
		}
		return abbrev;
	}

	Control getControl() {
		return treePanel;
	}

}
