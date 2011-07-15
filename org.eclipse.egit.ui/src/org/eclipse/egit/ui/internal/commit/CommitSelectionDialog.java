/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Comparator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.search.CommitSearchPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

/**
 * Commit selection dialog
 */
public class CommitSelectionDialog extends FilteredItemsSelectionDialog {

	private static final String COMMIT_SELECTION_DIALOG_SECTION = "CommitSelectionDialogSection"; //$NON-NLS-1$

	private static class CommitLabelProvider extends LabelProvider implements
			IStyledLabelProvider {

		private Image commitImage = UIIcons.CHANGESET.createImage();

		public void dispose() {
			this.commitImage.dispose();
			super.dispose();
		}

		public Image getImage(Object element) {
			return this.commitImage;
		}

		public String getText(Object element) {
			return getStyledText(element).getString();
		}

		public StyledString getStyledText(Object element) {
			StyledString styled = new StyledString();
			if (element instanceof RepositoryCommit) {
				RepositoryCommit commit = (RepositoryCommit) element;
				styled.append(commit.abbreviate());
				styled.append(MessageFormat.format(
						UIText.CommitSelectionDialog_SectionMessage, commit
								.getRevCommit().getShortMessage()),
						StyledString.QUALIFIER_STYLER);
				styled.append(MessageFormat.format(
						UIText.CommitSelectionDialog_SectionRepo,
						commit.getRepositoryName()),
						StyledString.DECORATIONS_STYLER);
			} else if (element != null)
				styled.append(element.toString());
			return styled;
		}
	}

	private static class RepositoryLabelProvider extends LabelProvider {

		private Image repositoryImage = UIIcons.REPOSITORY.createImage();

		public void dispose() {
			this.repositoryImage.dispose();
			super.dispose();
		}

		public Image getImage(Object element) {
			return this.repositoryImage;
		}

		public String getText(Object element) {
			if (element instanceof RepositoryCommit)
				return ((RepositoryCommit) element).getRepositoryName();
			else if (element != null)
				return element.toString();
			else
				return ""; //$NON-NLS-1$
		}

	}

	private CommitLabelProvider labelProvider;

	/**
	 * Create commit selection dialog
	 *
	 * @param shell
	 * @param multi
	 */
	public CommitSelectionDialog(Shell shell, boolean multi) {
		super(shell, multi);
		setTitle(UIText.CommitSelectionDialog_Title);
		setMessage(UIText.CommitSelectionDialog_Message);
		labelProvider = new CommitLabelProvider();
		setListLabelProvider(labelProvider);
		setDetailsLabelProvider(new RepositoryLabelProvider());
	}

	protected Control createExtendedContentArea(Composite parent) {
		Composite displayArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(displayArea);
		Link link = new Link(displayArea, SWT.NONE);
		link.setText(UIText.CommitSelectionDialog_LinkSearch);
		link.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				close();
				NewSearchUI.openSearchDialog(PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow(), CommitSearchPage.ID);
			}

		});
		return displayArea;
	}

	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = settings
				.getSection(COMMIT_SELECTION_DIALOG_SECTION);
		if (section == null)
			section = settings.addNewSection(COMMIT_SELECTION_DIALOG_SECTION);
		return section;
	}

	protected IStatus validateItem(Object item) {
		return Status.OK_STATUS;
	}

	protected ItemsFilter createFilter() {
		return new ItemsFilter() {

			public boolean isSubFilter(ItemsFilter filter) {
				return false;
			}

			public boolean matchItem(Object item) {
				return true;
			}

			public boolean isConsistentItem(Object item) {
				return true;
			}
		};
	}

	protected Comparator getItemsComparator() {
		return new Comparator<RepositoryCommit>() {

			public int compare(RepositoryCommit o1, RepositoryCommit o2) {
				int compare = o1.getRepositoryName().compareToIgnoreCase(
						o2.getRepositoryName());
				if (compare == 0)
					compare = o1.getRevCommit().name()
							.compareTo(o2.getRevCommit().name());
				return compare;
			}
		};
	}

	private Repository[] getRepositories() {
		return org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache().getAllRepositories();
	}

	protected void fillContentProvider(AbstractContentProvider contentProvider,
			ItemsFilter itemsFilter, IProgressMonitor progressMonitor)
			throws CoreException {
		String pattern = itemsFilter.getPattern();
		Repository[] repositories = getRepositories();
		progressMonitor.beginTask(UIText.CommitSelectionDialog_TaskSearching,
				repositories.length);
		for (Repository repository : repositories) {
			try {
				ObjectId commitId;
				if (ObjectId.isId(pattern))
					commitId = ObjectId.fromString(pattern);
				else
					commitId = repository.resolve(itemsFilter.getPattern());
				if (commitId != null) {
					RevWalk walk = new RevWalk(repository);
					walk.setRetainBody(true);
					RevCommit commit = walk.parseCommit(commitId);
					contentProvider.add(
							new RepositoryCommit(repository, commit),
							itemsFilter);
				}
			} catch (IOException ignored) {
				// Ignore and advance
			}
			progressMonitor.worked(1);
		}
	}

	public String getElementName(Object item) {
		return labelProvider.getText(item);
	}

}
