/*******************************************************************************
 * Copyright (C) 2014, Andreas Hermann <a.v.hermann@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import static java.util.Arrays.asList;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.history.CommitFileDiffViewer;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Stash viewer page class displaying author, committer, parent commits,
 * message, and file information in form sections.
 */
public class StashEditorPage extends CommitEditorPage {

	/**
	 * Index of the parent commit which contains staged changes.
	 */
	public static final int PARENT_COMMIT_STAGED = 1;

	/**
	 * Index of the parent commit which contains untracked changes.
	 */
	public static final int PARENT_COMMIT_UNTRACKED = 2;

	private Section stagedDiffSection;

	private CommitFileDiffViewer stagedDiffViewer;

	/**
	 * Create stash viewer page
	 *
	 * @param editor
	 */
	public StashEditorPage(FormEditor editor) {
		super(editor, "stashPage", UIText.CommitEditorPage_Title); //$NON-NLS-1$
	}

	@Override
	String getParentCommitLabel(int i) {
		switch (i) {
		case 0:
			return UIText.StashEditorPage_LabelParent0;
		case 1:
			return UIText.StashEditorPage_LabelParent1;
		case 2:
			return UIText.StashEditorPage_LabelParent2;
		default:
			throw new IllegalStateException("Unexpected parent with index" + i); //$NON-NLS-1$
		}
	}

	@Override
	void createTagsArea(Composite parent, FormToolkit toolkit,
			int span) {
		// tags do not apply to stash commits
	}

	@Override
	void createChangesArea(Composite displayArea, FormToolkit toolkit) {
		createDiffArea(displayArea, toolkit, 2);
		createIndexArea(displayArea, toolkit, 2);
	}

	private void createIndexArea(Composite parent,
			FormToolkit toolkit, int span) {
		String sectionTitle = MessageFormat.format(
				UIText.StashEditorPage_StagedChanges, Integer.valueOf(0));
		stagedDiffSection = createSection(parent, toolkit, sectionTitle, span,
				ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE
						| ExpandableComposite.EXPANDED);
		Composite unstagedChangesArea = createSectionClient(
				stagedDiffSection, toolkit);

		stagedDiffViewer = new CommitFileDiffViewer(unstagedChangesArea,
				getSite(), SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL
						| SWT.FULL_SELECTION | toolkit.getBorderStyle());
		Control control = stagedDiffViewer.getControl();
		control.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(control);
		addToFocusTracking(control);
		stagedDiffViewer.setContentProvider(ArrayContentProvider
				.getInstance());
		stagedDiffViewer.setTreeWalk(getCommit().getRepository(), null);

		updateSectionClient(stagedDiffSection, unstagedChangesArea, toolkit);
	}

	@Override
	void loadSections() {
		RepositoryCommit commit = getCommit();
		Job refreshJob = new Job(MessageFormat.format(
				UIText.CommitEditorPage_JobName, commit.getRevCommit().name())) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final FileDiff[] unstagedDiffs = getUnstagedDiffs();
				final FileDiff[] indexDiffs = getStagedDiffs();

				final ScrolledForm form = getManagedForm().getForm();
				if (UIUtils.isUsable(form))
					form.getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							if (!UIUtils.isUsable(form))
								return;
							fillDiffs(unstagedDiffs);
							fillStagedDiffs(indexDiffs);
							form.reflow(true);
							form.layout(true, true);
						}
					});

				return Status.OK_STATUS;
			}
		};
		refreshJob.setRule(this);
		refreshJob.schedule();
	}

	/**
	 * @return diffs for staged changes
	 */
	protected FileDiff[] getStagedDiffs() {
		List<FileDiff> stagedDiffsResult = new ArrayList<>();
		if (getCommit().getRevCommit().getParentCount() > 1) {
			RevCommit stagedCommit = getCommit().getRevCommit().getParent(
					PARENT_COMMIT_STAGED);
			FileDiff[] stagedDiffs = new RepositoryCommit(getCommit()
					.getRepository(), stagedCommit).getDiffs();
			stagedDiffsResult.addAll(asList(stagedDiffs));
		}
		return stagedDiffsResult
				.toArray(new FileDiff[stagedDiffsResult.size()]);
	}

	/**
	 * @return diffs for unstaged and untracked changes
	 */
	protected FileDiff[] getUnstagedDiffs() {
		List<FileDiff> unstagedDiffs = new ArrayList<>();

		RevCommit stagedCommit = getCommit().getRevCommit().getParent(
				PARENT_COMMIT_STAGED);
		List<FileDiff> workingDirDiffs = asList(getCommit().getDiffs(
				stagedCommit));
		unstagedDiffs.addAll(workingDirDiffs);

		if (getCommit().getRevCommit().getParentCount() > 2) {
			RevCommit untrackedCommit = getCommit().getRevCommit().getParent(
					PARENT_COMMIT_UNTRACKED);
			FileDiff[] untrackedDiffs = new RepositoryCommit(getCommit()
					.getRepository(), untrackedCommit).getDiffs();
			unstagedDiffs.addAll(asList(untrackedDiffs));
		}

		return unstagedDiffs.toArray(new FileDiff[unstagedDiffs.size()]);
	}

	private void fillStagedDiffs(FileDiff[] diffs) {
		if (diffs == null)
			return;
		stagedDiffViewer.setInput(diffs);
		stagedDiffSection.setText(MessageFormat.format(
				UIText.StashEditorPage_StagedChanges,
				Integer.valueOf(diffs.length)));
		setSectionExpanded(stagedDiffSection, diffs.length != 0);
	}

	@Override
	String getDiffSectionTitle(Integer numChanges) {
		return MessageFormat.format(UIText.StashEditorPage_UnstagedChanges,
				numChanges);
	}

}
