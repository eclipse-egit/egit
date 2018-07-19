/*******************************************************************************
 *  Copyright (c) 2011, 2012 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Daniel Megert <daniel_megert@ch.ibm.com> - handle RevisionSyntaxException
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Comparator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.GitLabelProvider;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.search.CommitSearchPage;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Constants;
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

	private static class CommitLabelProvider extends GitLabelProvider {

		@Override
		public String getText(Object element) {
			return getStyledText(element).getString();
		}

		@Override
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
		setDetailsLabelProvider(new GitLabelProvider() {
			@Override
			public Image getImage(Object element) {
				if (element instanceof RepositoryCommit) {
					RepositoryCommit commit = (RepositoryCommit) element;
					return super.getImage(commit.getRepository());
				}
				return super.getImage(element);
			}

			@Override
			public String getText(Object element) {
				if (element instanceof RepositoryCommit) {
					RepositoryCommit commit = (RepositoryCommit) element;
					return super.getText(commit.getRepository());
				}
				return super.getText(element);
			}
		});
		setInitialPattern(Constants.HEAD, FULL_SELECTION);
	}

	@Override
	protected Control createExtendedContentArea(Composite parent) {
		Composite displayArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(displayArea);
		Link link = new Link(displayArea, SWT.NONE);
		link.setText(UIText.CommitSelectionDialog_LinkSearch);
		link.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				close();
				NewSearchUI.openSearchDialog(PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow(), CommitSearchPage.ID);
			}

		});
		return displayArea;
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		IDialogSettings section = settings
				.getSection(COMMIT_SELECTION_DIALOG_SECTION);
		if (section == null)
			section = settings.addNewSection(COMMIT_SELECTION_DIALOG_SECTION);
		return section;
	}

	@Override
	protected IStatus validateItem(Object item) {
		return Status.OK_STATUS;
	}

	@Override
	protected ItemsFilter createFilter() {
		return new ItemsFilter() {

			@Override
			public boolean isSubFilter(ItemsFilter filter) {
				return false;
			}

			@Override
			public boolean matchItem(Object item) {
				return true;
			}

			@Override
			public boolean isConsistentItem(Object item) {
				return true;
			}
		};
	}

	@Override
	protected Comparator getItemsComparator() {
		return new Comparator<RepositoryCommit>() {

			@Override
			public int compare(RepositoryCommit o1, RepositoryCommit o2) {
				int compare = o1.getRepositoryName().compareToIgnoreCase(
						o2.getRepositoryName());
				if (compare == 0)
					compare = o1.getRevCommit().compareTo(o2.getRevCommit());
				return compare;
			}
		};
	}

	private Repository[] getRepositories() {
		return org.eclipse.egit.core.Activator.getDefault()
				.getRepositoryCache().getAllRepositories();
	}

	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider,
			ItemsFilter itemsFilter, IProgressMonitor progressMonitor)
			throws CoreException {
		String pattern = itemsFilter.getPattern();
		Repository[] repositories = getRepositories();
		SubMonitor progress = SubMonitor.convert(progressMonitor,
				repositories.length);
		progress.setTaskName(UIText.CommitSelectionDialog_TaskSearching);
		for (Repository repository : repositories) {
			try {
				ObjectId commitId;
				if (ObjectId.isId(pattern))
					commitId = ObjectId.fromString(pattern);
				else
					commitId = repository.resolve(itemsFilter.getPattern());
				if (commitId != null) {
					try (RevWalk walk = new RevWalk(repository)) {
						walk.setRetainBody(true);
						RevCommit commit = walk.parseCommit(commitId);
						contentProvider.add(
								new RepositoryCommit(repository, commit),
								itemsFilter);
					}
				}
			} catch (RevisionSyntaxException ignored) {
				// Ignore and advance
			} catch (IOException ignored) {
				// Ignore and advance
			}
			progress.worked(1);
		}
	}

	@Override
	public String getElementName(Object item) {
		return labelProvider.getText(item);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				UIText.CommitSelectionDialog_ButtonOK, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

}
