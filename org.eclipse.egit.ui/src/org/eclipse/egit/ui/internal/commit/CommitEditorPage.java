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

import java.text.MessageFormat;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.dialogs.SpellcheckableMessageArea;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.egit.ui.internal.history.FileDiffLabelProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Commit editor page class displaying author, committer, parent commits,
 * message, and file information in form sections.
 */
public class CommitEditorPage extends FormPage {

	/**
	 * Abbreviated length of parent id links displayed
	 */
	public static final int PARENT_LENGTH = 20;

	/**
	 * Create commit editor page
	 *
	 * @param editor
	 */
	public CommitEditorPage(FormEditor editor) {
		this(editor, "commitPage", UIText.CommitEditorPage_Title); //$NON-NLS-1$
	}

	/**
	 * Create commit editor page
	 *
	 * @param editor
	 * @param id
	 * @param title
	 */
	public CommitEditorPage(FormEditor editor, String id, String title) {
		super(editor, id, title);
	}

	private void hookExpansionGrabbing(final Section section) {
		section.addExpansionListener(new ExpansionAdapter() {

			public void expansionStateChanged(ExpansionEvent e) {
				((GridData) section.getLayoutData()).grabExcessVerticalSpace = e
						.getState();
				getManagedForm().reflow(true);
			}
		});
	}

	private void createHeaderArea(Composite parent, FormToolkit toolkit) {
		RevCommit commit = getCommit().getRevCommit();
		Composite top = toolkit.createComposite(parent);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(top);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(top);

		Composite userArea = toolkit.createComposite(top);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(userArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(userArea);

		PersonIdent author = commit.getAuthorIdent();
		if (author != null) {
			Text authorText = toolkit.createText(userArea, MessageFormat
					.format(UIText.CommitEditorPage_LabelAuthor,
							author.getName(), author.getWhen()));
			GridDataFactory.fillDefaults().span(1, 1).applyTo(authorText);
		}

		PersonIdent committer = commit.getCommitterIdent();
		if (committer != null && !committer.equals(author)) {
			Text committerText = toolkit.createText(userArea, MessageFormat
					.format(UIText.CommitEditorPage_LabelCommitter,
							committer.getName(), committer.getWhen()));
			committerText.setFont(parent.getFont());
			GridDataFactory.fillDefaults().span(1, 1).applyTo(committerText);
		}

		int count = commit.getParentCount();
		if (count > 0) {
			Composite parents = toolkit.createComposite(top);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(parents);
			GridDataFactory.fillDefaults().grab(false, false).applyTo(parents);

			for (int i = 0; i < count; i++) {
				final RevCommit parentCommit = commit.getParent(i);
				toolkit.createLabel(parents,
						UIText.CommitEditorPage_LabelParent).setForeground(
						toolkit.getColors().getColor(IFormColors.TB_TOGGLE));
				final Hyperlink link = toolkit
						.createHyperlink(parents,
								parentCommit.abbreviate(PARENT_LENGTH).name(),
								SWT.NONE);
				link.addHyperlinkListener(new HyperlinkAdapter() {

					public void linkActivated(HyperlinkEvent e) {
						try {
							CommitEditor.open(new RepositoryCommit(getCommit()
									.getRepository(), parentCommit));
							if ((e.getStateMask() & SWT.MOD1) != 0)
								getEditor().close(false);
						} catch (PartInitException e1) {
							Activator.logError(
									"Error opening commit editor", e1);//$NON-NLS-1$
						}
					}
				});
			}
		}
	}

	private void createDescriptionArea(Composite parent, FormToolkit toolkit) {
		Section description = toolkit.createSection(parent,
				ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE
						| ExpandableComposite.EXPANDED);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(description);
		description.setText(UIText.CommitEditorPage_SectionMessage);

		String content = getCommit().getRevCommit().getFullMessage();
		SpellcheckableMessageArea textContent = new SpellcheckableMessageArea(
				description, content) {

			@Override
			protected IAdaptable getDefaultTarget() {
				return new PlatformObject() {
					public Object getAdapter(Class adapter) {
						return Platform.getAdapterManager().getAdapter(
								getEditorInput(), adapter);
					}
				};
			}

		};
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 80)
				.applyTo(textContent);
		textContent.getTextWidget().setEditable(false);
		description.setClient(textContent);

		hookExpansionGrabbing(description);
	}

	private void createFilesArea(Composite parent, FormToolkit toolkit) {
		Section files = toolkit.createSection(parent,
				ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE
						| ExpandableComposite.EXPANDED);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(files);

		TableViewer viewer = new TableViewer(toolkit.createTable(files,
				SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER));
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 80)
				.applyTo(viewer.getControl());
		viewer.setLabelProvider(new FileDiffLabelProvider());
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		files.setClient(viewer.getControl());

		RepositoryCommit commit = getCommit();
		FileDiff[] diffs = commit.getDiffs();
		viewer.setInput(diffs);
		files.setText(MessageFormat.format(
				UIText.CommitEditorPage_SectionFiles,
				Integer.valueOf(diffs.length)));

		hookExpansionGrabbing(files);
	}

	private RepositoryCommit getCommit() {
		return (RepositoryCommit) getEditor()
				.getAdapter(RepositoryCommit.class);
	}

	/**
	 * @see org.eclipse.ui.forms.editor.FormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	protected void createFormContent(IManagedForm managedForm) {
		Composite body = managedForm.getForm().getBody();
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(body);

		FormToolkit toolkit = managedForm.getToolkit();

		createHeaderArea(body, toolkit);
		createDescriptionArea(body, toolkit);
		createFilesArea(body, toolkit);
	}
}
