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
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Stash viewer page class displaying author, committer, parent commits,
 * message, and file information in form sections.
 */
public class StashEditorPage extends CommitEditorPage {

	private static final int PARENT_COMMIT_WORKING_DIR = 1;

	private static final int PARENT_COMMIT_UNTRACKED = 2;

	private Section unstagedDiffSection;

	private CommitFileDiffViewer unstagedDiffViewer;

	/**
	 * Create stash viewer page
	 *
	 * @param editor
	 */
	public StashEditorPage(FormEditor editor) {
		super(editor, "stashPage", UIText.CommitEditorPage_Title); //$NON-NLS-1$
	}

	@Override
	void createTagsArea(Composite parent, FormToolkit toolkit,
			int span) {
		// tags do not apply to stash commits
	}

	@Override
	void createChangesArea(Composite displayArea, FormToolkit toolkit) {
		createUnstagedChangesArea(displayArea, toolkit, 2);
		createIndexChangesArea(displayArea, toolkit, 2);
	}

	private void createUnstagedChangesArea(Composite parent,
			FormToolkit toolkit, int span) {
		unstagedDiffSection = createSection(parent, toolkit, span);
		String sectionTitle = MessageFormat.format(
				UIText.StashEditorPage_UnstagedChanges, Integer.valueOf(0));
		unstagedDiffSection.setText(sectionTitle);
		Composite unstagedChangesArea = createSectionClient(
				unstagedDiffSection, toolkit);

		unstagedDiffViewer = new CommitFileDiffViewer(unstagedChangesArea,
				getSite(), SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL
						| SWT.FULL_SELECTION | toolkit.getBorderStyle());
		unstagedDiffViewer.getTable().setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TREE_BORDER);
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 80)
				.applyTo(unstagedDiffViewer.getControl());
		unstagedDiffViewer.setContentProvider(ArrayContentProvider
				.getInstance());
		unstagedDiffViewer.setTreeWalk(getCommit().getRepository(), null);

		updateSectionClient(unstagedDiffSection, unstagedChangesArea, toolkit);
	}

	@Override
	void loadSections() {
		RepositoryCommit commit = getCommit();
		Job refreshJob = new Job(MessageFormat.format(
				UIText.CommitEditorPage_JobName, commit.getRevCommit().name())) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final FileDiff[] indexDiffs = getCommit().getDiffs();
				final FileDiff[] unstagedDiffs = getUnstagedDiffs();

				final ScrolledForm form = getManagedForm().getForm();
				if (UIUtils.isUsable(form))
					form.getDisplay().syncExec(new Runnable() {
						public void run() {
							if (!UIUtils.isUsable(form))
								return;
							fillIndexDiffs(indexDiffs);
							fillUnstagedDiffs(unstagedDiffs);
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
	 * @return diffs for unstaged and untracked changes in case of stash commit
	 */
	protected FileDiff[] getUnstagedDiffs() {
		List<FileDiff> unstagedDiffs = new ArrayList<FileDiff>();
		if (getCommit().getRevCommit().getParentCount() > 1) {
			RevCommit workingDirCommit = getCommit().getRevCommit().getParent(
					PARENT_COMMIT_WORKING_DIR);
			FileDiff[] workingDirDiffs = new RepositoryCommit(getCommit()
					.getRepository(), workingDirCommit).getDiffs();
			unstagedDiffs.addAll(asList(workingDirDiffs));
		}
		if (getCommit().getRevCommit().getParentCount() > 2) {
			RevCommit untrackedCommit = getCommit().getRevCommit().getParent(
					PARENT_COMMIT_UNTRACKED);
			unstagedDiffs.addAll(asList(new RepositoryCommit(getCommit()
					.getRepository(), untrackedCommit).getDiffs()));
		}

		return unstagedDiffs.toArray(new FileDiff[0]);
	}

	private void fillUnstagedDiffs(FileDiff[] diffs) {
		if (diffs == null)
			return;
		unstagedDiffViewer.setInput(diffs);
		unstagedDiffSection.setText(MessageFormat.format(
				UIText.StashEditorPage_UnstagedChanges,
				Integer.valueOf(diffs.length)));
	}

	@Override
	String getIndexDiffSectionTitle(Integer numChanges) {
		return MessageFormat.format(UIText.StashEditorPage_StagedChanges,
				numChanges);
	}

}
