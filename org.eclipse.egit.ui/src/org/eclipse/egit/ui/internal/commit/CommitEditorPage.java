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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIText;
import org.eclipse.egit.ui.internal.dialogs.SpellcheckableMessageArea;
import org.eclipse.egit.ui.internal.history.CommitFileDiffViewer;
import org.eclipse.egit.ui.internal.history.FileDiff;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
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

	private static final String SIGNED_OFF_BY = "Signed-off-by: {0} <{1}>"; //$NON-NLS-1$

	/**
	 * Abbreviated length of parent id links displayed
	 */
	public static final int PARENT_LENGTH = 20;

	private LocalResourceManager resources = new LocalResourceManager(
			JFaceResources.getResources());

	private Composite tagLabelArea;

	private Section branchSection;

	private TableViewer branchViewer;

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
				getManagedForm().getForm().getBody().layout(true, true);
			}
		});
	}

	private Image getImage(ImageDescriptor descriptor) {
		return (Image) this.resources.get(descriptor);
	}

	private Section createSection(Composite parent, FormToolkit toolkit,
			int span) {
		Section section = toolkit.createSection(parent,
				ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE
						| ExpandableComposite.EXPANDED);
		GridDataFactory.fillDefaults().span(span, 1).grab(true, true)
				.applyTo(section);
		return section;
	}

	private Composite createSectionClient(Section parent, FormToolkit toolkit) {
		Composite client = toolkit.createComposite(parent);
		GridLayoutFactory.fillDefaults().extendedMargins(2, 2, 2, 2)
				.applyTo(client);
		return client;
	}

	private boolean isSignedOffBy(PersonIdent person) {
		RevCommit commit = getCommit().getRevCommit();
		return commit.getFullMessage().indexOf(getSignedOffByLine(person)) != -1;
	}

	private String getSignedOffByLine(PersonIdent person) {
		return MessageFormat.format(SIGNED_OFF_BY, person.getName(),
				person.getEmailAddress());
	}

	private String replaceSignedOffByLine(String message, PersonIdent person) {
		Pattern pattern = Pattern.compile(
				"^\\s*" + Pattern.quote(getSignedOffByLine(person)) //$NON-NLS-1$
						+ "\\s*$", Pattern.MULTILINE); //$NON-NLS-1$
		return pattern.matcher(message).replaceAll(""); //$NON-NLS-1$
	}

	private Composite createUserArea(Composite parent, FormToolkit toolkit,
			PersonIdent person, boolean author) {
		Composite userArea = toolkit.createComposite(parent);
		GridLayoutFactory.fillDefaults().spacing(2, 2).numColumns(3)
				.applyTo(userArea);

		Label userLabel = toolkit.createLabel(userArea, null);
		userLabel.setImage(getImage(author ? UIIcons.ELCL16_AUTHOR
				: UIIcons.ELCL16_COMMITTER));
		if (author)
			userLabel.setToolTipText(UIText.CommitEditorPage_TooltipAuthor);
		else
			userLabel.setToolTipText(UIText.CommitEditorPage_TooltipCommitter);

		boolean signedOff = isSignedOffBy(person);

		Text userText = new Text(userArea, SWT.FLAT | SWT.READ_ONLY);
		userText.setText(MessageFormat.format(
				author ? UIText.CommitEditorPage_LabelAuthor
						: UIText.CommitEditorPage_LabelCommitter, person
						.getName(), person.getEmailAddress(), person.getWhen()));
		toolkit.adapt(userText, false, false);
		userText.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);

		GridDataFactory.fillDefaults().span(signedOff ? 1 : 2, 1)
				.applyTo(userText);
		if (signedOff) {
			Label signedOffLabel = toolkit.createLabel(userArea, null);
			signedOffLabel.setImage(getImage(UIIcons.SIGNED_OFF));
			if (author)
				signedOffLabel
						.setToolTipText(UIText.CommitEditorPage_TooltipSignedOffByAuthor);
			else
				signedOffLabel
						.setToolTipText(UIText.CommitEditorPage_TooltipSignedOffByCommitter);
		}

		return userArea;
	}

	private void updateSectionClient(Section section, Composite client,
			FormToolkit toolkit) {
		hookExpansionGrabbing(section);
		toolkit.paintBordersFor(client);
		section.setClient(client);
	}

	private void createHeaderArea(Composite parent, FormToolkit toolkit,
			int span) {
		RevCommit commit = getCommit().getRevCommit();
		Composite top = toolkit.createComposite(parent);
		GridDataFactory.fillDefaults().grab(true, false).span(span, 1)
				.applyTo(top);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(top);

		Composite userArea = toolkit.createComposite(top);
		GridLayoutFactory.fillDefaults().spacing(2, 2).numColumns(1)
				.applyTo(userArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(userArea);

		PersonIdent author = commit.getAuthorIdent();
		if (author != null)
			createUserArea(userArea, toolkit, author, true);

		PersonIdent committer = commit.getCommitterIdent();
		if (committer != null && !committer.equals(author))
			createUserArea(userArea, toolkit, committer, false);

		int count = commit.getParentCount();
		if (count > 0) {
			Composite parents = toolkit.createComposite(top);
			GridLayoutFactory.fillDefaults().spacing(2, 2).numColumns(2)
					.applyTo(parents);
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

		createTagsArea(userArea, toolkit, 2);
	}

	private List<String> getTags() {
		RevCommit commit = getCommit().getRevCommit();
		Repository repository = getCommit().getRepository();
		List<String> tags = new ArrayList<String>();
		for (Ref tag : repository.getTags().values())
			if (commit.equals(repository.peel(tag).getPeeledObjectId()))
				tags.add(Repository.shortenRefName(tag.getName()));
		Collections.sort(tags);
		return tags;
	}

	private void createTagsArea(Composite parent, FormToolkit toolkit, int span) {
		Composite tagArea = toolkit.createComposite(parent);
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false)
				.applyTo(tagArea);
		GridDataFactory.fillDefaults().span(span, 1).grab(true, false)
				.applyTo(tagArea);
		toolkit.createLabel(tagArea, UIText.CommitEditorPage_LabelTags)
				.setForeground(
						toolkit.getColors().getColor(IFormColors.TB_TOGGLE));
		fillTags(tagArea, toolkit);
	}

	private void fillTags(Composite parent, FormToolkit toolkit) {
		if (tagLabelArea != null)
			tagLabelArea.dispose();
		tagLabelArea = toolkit.createComposite(parent);
		GridLayoutFactory.fillDefaults().spacing(1, 1).numColumns(4)
				.applyTo(tagLabelArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tagLabelArea);
		List<String> tags = getTags();
		for (String tag : tags) {
			CLabel tagLabel = new CLabel(tagLabelArea, SWT.NONE);
			toolkit.adapt(tagLabel, false, false);
			tagLabel.setImage(getImage(UIIcons.TAG));
			tagLabel.setText(tag);
		}
	}

	private void createMessageArea(Composite parent, FormToolkit toolkit,
			int span) {
		Section messageSection = createSection(parent, toolkit, span);
		Composite messageArea = createSectionClient(messageSection, toolkit);

		messageSection.setText(UIText.CommitEditorPage_SectionMessage);

		RevCommit commit = getCommit().getRevCommit();
		String message = commit.getFullMessage();

		PersonIdent author = commit.getAuthorIdent();
		if (author != null)
			message = replaceSignedOffByLine(message, author);
		PersonIdent committer = commit.getCommitterIdent();
		if (committer != null)
			message = replaceSignedOffByLine(message, committer);

		SpellcheckableMessageArea textContent = new SpellcheckableMessageArea(
				messageArea, message, SWT.NONE) {

			@Override
			protected IAdaptable getDefaultTarget() {
				return new PlatformObject() {
					public Object getAdapter(Class adapter) {
						return Platform.getAdapterManager().getAdapter(
								getEditorInput(), adapter);
					}
				};
			}

			protected void createMarginPainter() {
				// Disabled intentionally
			}

		};
		textContent.setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TEXT_BORDER);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 80).grab(true, true)
				.applyTo(textContent);
		textContent.getTextWidget().setEditable(false);

		updateSectionClient(messageSection, messageArea, toolkit);
	}

	private void createBranchesArea(Composite parent, FormToolkit toolkit,
			int span) {
		branchSection = createSection(parent, toolkit, span);
		Composite branchesArea = createSectionClient(branchSection, toolkit);

		branchViewer = new TableViewer(toolkit.createTable(branchesArea,
				SWT.V_SCROLL | SWT.H_SCROLL));
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 50)
				.applyTo(branchViewer.getControl());
		branchViewer.setSorter(new ViewerSorter());
		branchViewer.setLabelProvider(new LabelProvider() {

			public Image getImage(Object element) {
				return CommitEditorPage.this.getImage(UIIcons.BRANCH);
			}

			public String getText(Object element) {
				return Repository.shortenRefName(((Ref) element).getName());
			}

		});
		branchViewer.setContentProvider(ArrayContentProvider.getInstance());
		branchViewer.setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TREE_BORDER);

		fillBranches();

		updateSectionClient(branchSection, branchesArea, toolkit);
	}

	private void fillBranches() {
		Repository repository = getCommit().getRepository();
		RevCommit commit = getCommit().getRevCommit();
		RevWalk revWalk = new RevWalk(repository);
		List<Ref> result = new ArrayList<Ref>();
		try {
			Map<String, Ref> refsMap = new HashMap<String, Ref>();
			refsMap.putAll(repository.getRefDatabase().getRefs(
					Constants.R_HEADS));
			refsMap.putAll(repository.getRefDatabase().getRefs(
					Constants.R_REMOTES));
			for (Ref ref : refsMap.values()) {
				if (ref.isSymbolic())
					continue;
				RevCommit headCommit = revWalk.parseCommit(ref.getObjectId());
				RevCommit base = revWalk.parseCommit(commit);
				if (revWalk.isMergedInto(base, headCommit))
					result.add(ref);
			}
		} catch (IOException ignored) {
			// Ignored
		}

		branchViewer.setInput(result);
		branchSection.setText(MessageFormat.format(
				UIText.CommitEditorPage_SectionBranches,
				Integer.valueOf(result.size())));
	}

	private void createFilesArea(Composite parent, FormToolkit toolkit, int span) {
		Section files = createSection(parent, toolkit, span);
		Composite filesArea = createSectionClient(files, toolkit);
		GridLayout filesAreaLayout = (GridLayout) filesArea.getLayout();
		filesAreaLayout.marginLeft = 0;
		filesAreaLayout.marginRight = 0;
		filesAreaLayout.marginTop = 0;
		filesAreaLayout.marginBottom = 0;

		CommitFileDiffViewer viewer = new CommitFileDiffViewer(filesArea,
				getSite(), SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL
						| SWT.FULL_SELECTION | toolkit.getBorderStyle());
		// commit file diff viewer uses a nested composite with a stack layout
		// and so margins need to be applied to have form toolkit style borders
		toolkit.paintBordersFor(viewer.getTable().getParent());
		viewer.getTable().setData(FormToolkit.KEY_DRAW_BORDER,
				FormToolkit.TREE_BORDER);
		StackLayout viewerLayout = (StackLayout) viewer.getControl()
				.getParent().getLayout();
		viewerLayout.marginHeight = 2;
		viewerLayout.marginWidth = 2;
		GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 80)
				.applyTo(viewer.getTable().getParent());
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setTreeWalk(getCommit().getRepository(), null);

		FileDiff[] diffs = getCommit().getDiffs();
		viewer.setInput(diffs);
		files.setText(MessageFormat.format(
				UIText.CommitEditorPage_SectionFiles,
				Integer.valueOf(diffs.length)));

		updateSectionClient(files, filesArea, toolkit);
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
		body.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				resources.dispose();
			}
		});
		FillLayout bodyLayout = new FillLayout();
		bodyLayout.marginHeight = 5;
		bodyLayout.marginWidth = 5;
		body.setLayout(bodyLayout);

		FormToolkit toolkit = managedForm.getToolkit();

		Composite displayArea = toolkit.createComposite(body);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(displayArea);

		createHeaderArea(displayArea, toolkit, 2);
		createMessageArea(displayArea, toolkit, 2);
		createFilesArea(displayArea, toolkit, 1);
		createBranchesArea(displayArea, toolkit, 1);
	}

	/**
	 * Refresh the editor page
	 */
	public void refresh() {
		fillTags(tagLabelArea.getParent(), getManagedForm().getToolkit());
		fillBranches();
		getManagedForm().getForm().layout(true, true);
	}

}
