/******************************************************************************
 *  Copyright (c) 2011, 2016 GitHub Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Marc-Andre Laperle (Ericsson) - Set the input to null when not visible
 *    Thomas Wolf <thomas.wolf@paranor.ch> - preference-based date formatting
 *****************************************************************************/
package org.eclipse.egit.ui.internal.blame;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.egit.core.internal.CompareCoreUtils;
import org.eclipse.egit.core.internal.job.JobUtil;
import org.eclipse.egit.core.internal.storage.CommitFileRevision;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.JobFamilies;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.CompareUtils;
import org.eclipse.egit.ui.internal.PreferenceBasedDateFormatter;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.blame.BlameOperation.BlameHistoryPageInput;
import org.eclipse.egit.ui.internal.blame.BlameRevision.Diff;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.DiffDocument;
import org.eclipse.egit.ui.internal.commit.DiffStyleRangeFormatter;
import org.eclipse.egit.ui.internal.commit.DiffViewer;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.egit.ui.internal.history.HistoryPageInput;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;

/**
 * Annotation information control
 */
public class BlameInformationControl extends AbstractInformationControl
		implements IInformationControlExtension2 {

	private IInformationControlCreator creator;

	private IVerticalRulerInfo rulerInfo;

	private BlameInformationControl hoverInformationControl;

	private BlameRevision revision;

	private ScrolledComposite scrolls;

	private Composite displayArea;

	private Label commitLabel;

	private Label authorLabel;

	private Label committerLabel;

	private StyledText messageText;

	/**
	 * 0-based line number
	 */
	private int revisionRulerLineNumber;

	private Composite diffComposite;

	private Link showAnnotationsLink;

	private SelectionAdapter showAnnotationsLinkSelectionAdapter;

	/**
	 * Create the information control for showing details on hover.
	 *
	 * @param parentShell
	 * @param creator
	 * @param rulerInfo
	 */
	public BlameInformationControl(Shell parentShell,
			IInformationControlCreator creator, IVerticalRulerInfo rulerInfo) {
		super(parentShell, false);
		this.creator = creator;
		this.rulerInfo = rulerInfo;
		create();
	}

	/**
	 * Create the enriched information control that is shown when moving the
	 * mouse over the control that was shown on hover (making it sticky).
	 *
	 * @param parentShell
	 * @param hoverInformationControl
	 *            the control that was used on hover (used for getting the
	 *            correct line that was hovered)
	 */
	public BlameInformationControl(Shell parentShell,
			BlameInformationControl hoverInformationControl) {
		// Make resizable and have a bottom bar for moving around
		super(parentShell, new ToolBarManager());
		this.hoverInformationControl = hoverInformationControl;
		create();
	}

	@Override
	public IInformationControlCreator getInformationPresenterControlCreator() {
		return this.creator;
	}

	@Override
	public boolean hasContents() {
		return true;
	}

	@Override
	protected void createContent(Composite parent) {
		scrolls = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		scrolls.setExpandHorizontal(true);
		scrolls.setExpandVertical(true);
		displayArea = new Composite(scrolls, SWT.NONE);
		scrolls.setContent(displayArea);
		displayArea.setForeground(parent.getForeground());
		displayArea.setBackground(parent.getBackground());
		displayArea.setBackgroundMode(SWT.INHERIT_FORCE);
		GridLayoutFactory.swtDefaults().equalWidth(true).applyTo(displayArea);

		Composite commitHeader = new Composite(displayArea, SWT.NONE);
		commitHeader.setLayout(GridLayoutFactory.fillDefaults().numColumns(3)
				.create());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(commitHeader);

		commitLabel = new Label(commitHeader, SWT.READ_ONLY);
		commitLabel.setFont(JFaceResources.getBannerFont());

		Link openCommitLink = new Link(commitHeader, SWT.NONE);
		openCommitLink.setText(UIText.BlameInformationControl_OpenCommitLink);
		openCommitLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openCommit();
			}
		});

		Link showInHistoryLink = new Link(commitHeader, SWT.NONE);
		showInHistoryLink.setText(UIText.BlameInformationControl_ShowInHistoryLink);
		showInHistoryLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showCommitInHistory();
			}
		});

		authorLabel = new Label(displayArea, SWT.NONE);
		authorLabel.setForeground(parent.getForeground());
		authorLabel.setBackground(parent.getBackground());
		authorLabel.setFont(UIUtils.getItalicFont(JFaceResources.DEFAULT_FONT));
		GridDataFactory.fillDefaults().grab(true, false).applyTo(authorLabel);

		committerLabel = new Label(displayArea, SWT.NONE);
		committerLabel.setForeground(parent.getForeground());
		committerLabel.setBackground(parent.getBackground());
		committerLabel.setFont(UIUtils
				.getItalicFont(JFaceResources.DEFAULT_FONT));
		GridDataFactory.fillDefaults().grab(true, false)
				.applyTo(committerLabel);

		Label separator = new Label(displayArea, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(separator);

		messageText = new StyledText(displayArea, SWT.NONE);
		messageText.setForeground(parent.getForeground());
		messageText.setBackground(parent.getBackground());
		messageText.setEditable(false);
		messageText.setFont(UIUtils
				.getFont(UIPreferences.THEME_CommitMessageFont));
		GridDataFactory.fillDefaults().grab(true, true).applyTo(messageText);
	}

	@Override
	public Point computeSizeHint() {
		Point computed = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

		Point constraints = getSizeConstraints();
		if (constraints == null)
			return computed;

		Point constrainedSize = getShell().computeSize(constraints.x,
				SWT.DEFAULT, true);
		int width = Math.min(computed.x, constrainedSize.x);
		int height = Math.max(computed.y, constrainedSize.y);
		return new Point(width, height);
	}

	private void setControlVisible(Control control, boolean visible) {
		control.setVisible(visible);
		((GridData) control.getLayoutData()).exclude = !visible;
	}

	@Override
	public void setInput(Object input) {
		if (input == null) {
			// Make sure we don't hold a reference to this when nothing is
			// shown, it can be big
			this.revision = null;
			if (showAnnotationsLink != null) {
				// This listener can also hold a reference because of a final
				// parameter, see createDiffLinkAndText(final RevCommit
				// parent...)
				if (!showAnnotationsLink.isDisposed())
					showAnnotationsLink
							.removeSelectionListener(showAnnotationsLinkSelectionAdapter);
				showAnnotationsLink = null;
				showAnnotationsLinkSelectionAdapter = null;
			}
			return;
		}

		this.revision = (BlameRevision) input;

		// Remember line number that was hovered over when the input was set.
		// Used for showing the diff hunk of this line instead of the full diff.
		if (rulerInfo != null)
			revisionRulerLineNumber = rulerInfo
					.getLineOfLastMouseButtonActivity();

		RevCommit commit = this.revision.getCommit();

		String linkText = MessageFormat.format(
				UIText.BlameInformationControl_Commit, commit.abbreviate(7)
						.name());
		commitLabel.setText(linkText);

		PreferenceBasedDateFormatter dateFormatter = PreferenceBasedDateFormatter
				.create();
		PersonIdent author = commit.getAuthorIdent();
		if (author != null) {
			setControlVisible(authorLabel, true);
			authorLabel.setText(MessageFormat.format(
					UIText.BlameInformationControl_Author, author.getName(),
					author.getEmailAddress(),
					dateFormatter.formatDate(author)));
		} else
			setControlVisible(authorLabel, false);

		PersonIdent committer = commit.getCommitterIdent();
		setControlVisible(authorLabel, author != null);
		if (committer != null && !committer.equals(author)) {
			setControlVisible(committerLabel, true);
			committerLabel.setText(MessageFormat.format(
					UIText.BlameInformationControl_Committer,
					committer.getName(), committer.getEmailAddress(),
					dateFormatter.formatDate(committer)));
		} else
			setControlVisible(committerLabel, false);

		messageText.setText(commit.getFullMessage());

		createDiffs();

		displayArea.layout();
		scrolls.setMinSize(displayArea.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	private void createDiffs() {
		if (diffComposite != null)
			diffComposite.dispose();

		RevCommit commit = revision.getCommit();
		if (commit.getParentCount() == 0)
			return;

		createDiffComposite();

		for (int i = 0; i < commit.getParentCount(); i++) {
			RevCommit parent = commit.getParent(i);
			createDiff(parent);
		}
	}

	private void createDiffComposite() {
		diffComposite = new Composite(displayArea, SWT.NONE);
		diffComposite.setLayoutData(GridDataFactory.fillDefaults()
				.grab(true, true).create());
		diffComposite.setLayout(GridLayoutFactory.fillDefaults().create());
	}

	private void createDiff(RevCommit parent) {
		Diff diff = revision.getDiffToParent(parent);
		if (diff != null) {
			try {
				createDiffLinkAndText(parent, diff);
			} catch (IOException e) {
				String msg = "Error creating diff in blame information control for commit " //$NON-NLS-1$
						+ parent.toObjectId();
				Activator.logError(msg, e);
			}
		}
	}

	private void createDiffLinkAndText(final RevCommit parent, final Diff diff)
			throws IOException {
		String parentId = parent.toObjectId().abbreviate(7).name();
		String parentMessage = parent.getShortMessage();

		EditList interestingDiff = getInterestingDiff(diff.getEditList());

		final Integer parentLine;
		if (!interestingDiff.isEmpty())
			parentLine = Integer.valueOf(interestingDiff.get(0).getBeginA());
		else
			parentLine = null;

		Composite header = new Composite(diffComposite, SWT.NONE);
		header.setLayout(GridLayoutFactory.fillDefaults().numColumns(2)
				.create());

		Label diffHeaderLabel = new Label(header, SWT.NONE);
		diffHeaderLabel.setText(NLS.bind(
				UIText.BlameInformationControl_DiffHeaderLabel, parentId,
				parentMessage));

		showAnnotationsLink = new Link(header, SWT.NONE);
		showAnnotationsLink
				.setText(UIText.BlameInformationControl_ShowAnnotationsLink);
		showAnnotationsLinkSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				blameParent(parent, diff, parentLine);
			}
		};
		showAnnotationsLink
				.addSelectionListener(showAnnotationsLinkSelectionAdapter);

		DiffViewer diffText = new DiffViewer(diffComposite, null, SWT.NONE);
		diffText.configure(
				new DiffViewer.Configuration(EditorsUI.getPreferenceStore()));
		diffText.getControl().setLayoutData(
				GridDataFactory.fillDefaults().grab(true, true).create());

		DiffDocument document = new DiffDocument();
		DiffStyleRangeFormatter diffFormatter = new DiffStyleRangeFormatter(
				document);
		diffFormatter.setContext(1);
		diffFormatter.setRepository(revision.getRepository());
		diffFormatter.format(interestingDiff, diff.getOldText(),
				diff.getNewText());

		try (ObjectReader reader = revision.getRepository().newObjectReader()) {
			DiffEntry diffEntry = CompareCoreUtils.getChangeDiffEntry(
					revision.getRepository(), revision.getSourcePath(),
					revision.getCommit(), parent, reader);
			if (diffEntry != null) {
				FileDiff fileDiff = new FileDiff(revision.getCommit(),
						diffEntry);
				document.setDefault(revision.getRepository(), fileDiff);
			}
		}
		document.connect(diffFormatter);
		diffText.setDocument(document);
	}

	private EditList getInterestingDiff(EditList fullDiff) {
		int hoverLineNumber = getHoverLineNumber();
		Integer sourceLine = revision.getSourceLine(hoverLineNumber);

		if (sourceLine == null)
			// Fall back to whole diff
			return fullDiff;

		int line = sourceLine.intValue();
		EditList interestingDiff = new EditList(1);
		for (Edit edit : fullDiff) {
			if (line >= edit.getBeginB() && line <= edit.getEndB())
				interestingDiff.add(edit);
		}
		return interestingDiff;
	}

	/**
	 * @return 0-based line number of hover
	 */
	private int getHoverLineNumber() {
		// If this is the enriched control, we have to use the line number of
		// the original hover control, otherwise the line number may have
		// changed if the mouse moved over another area.
		if (hoverInformationControl != null)
			return hoverInformationControl.getHoverLineNumber();
		else
			return revisionRulerLineNumber;
	}

	private void openCommit() {
		try {
			getShell().dispose();
			CommitEditor.open(new RepositoryCommit(revision.getRepository(),
					revision.getCommit()));
		} catch (PartInitException pie) {
			Activator.logError(pie.getLocalizedMessage(), pie);
		}
	}

	private void showCommitInHistory() {
		getShell().dispose();
		IHistoryView part;
		try {
			part = (IHistoryView) PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage()
					.showView(IHistoryView.VIEW_ID);
		} catch (PartInitException e) {
			Activator.logError(e.getLocalizedMessage(), e);
			return;
		}

		if (part == null)
			return;

		Repository repository = revision.getRepository();
		if (!repository.isBare()) {
			String sourcePath = revision.getSourcePath();
			File file = new File(repository.getWorkTree(), sourcePath);
			BlameHistoryPageInput input = new BlameHistoryPageInput(repository,
					revision.getCommit(), file);
			part.showHistoryFor(input);
		} else {
			HistoryPageInput input = new BlameHistoryPageInput(repository,
					revision.getCommit());
			part.showHistoryFor(input);
		}
	}

	private void blameParent(RevCommit parent, Diff diff, Integer sourceLine) {
		try {
			String path = diff.getOldPath();
			IFileRevision rev = CompareUtils.getFileRevision(path, parent,
					revision.getRepository(), null);
			int line = sourceLine == null ? -1 : sourceLine.intValue();
			if (rev instanceof CommitFileRevision) {
				IWorkbenchPage page = PlatformUI.getWorkbench()
						.getActiveWorkbenchWindow().getActivePage();
				BlameOperation operation = new BlameOperation(
						(CommitFileRevision) rev, getShell(),
						page, line);
				JobUtil.scheduleUserJob(operation,
						UIText.ShowBlameHandler_JobName, JobFamilies.BLAME);
			}
		} catch (IOException e) {
			Activator.logError(UIText.ShowBlameHandler_errorMessage, e);
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (!visible)
			setInput(null);
	}
}
