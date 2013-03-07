/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.blame;

import java.text.MessageFormat;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.commit.CommitEditor;
import org.eclipse.egit.ui.internal.commit.RepositoryCommit;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;

/**
 * Annotation information control
 */
public class BlameInformationControl extends AbstractInformationControl
		implements IInformationControlExtension2 {

	private IInformationControlCreator creator;

	private BlameRevision revision;

	private ScrolledComposite scrolls;

	private Composite displayArea;

	private StyledText commitLink;

	private Label authorLabel;

	private Label committerLabel;

	private StyledText messageText;

	/**
	 * @param parentShell
	 * @param isResizable
	 * @param creator
	 */
	public BlameInformationControl(Shell parentShell, boolean isResizable,
			IInformationControlCreator creator) {
		super(parentShell, isResizable);
		this.creator = creator;
		create();
	}

	public IInformationControlCreator getInformationPresenterControlCreator() {
		return this.creator;
	}

	public boolean hasContents() {
		return true;
	}

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

		commitLink = new StyledText(displayArea, SWT.READ_ONLY);
		commitLink.addMouseListener(new MouseAdapter() {

			public void mouseUp(MouseEvent e) {
				if (commitLink.getSelectionText().length() != 0)
					return;
				try {
					getShell().dispose();
					CommitEditor.open(new RepositoryCommit(revision
							.getRepository(), revision.getCommit()));
				} catch (PartInitException pie) {
					Activator.logError(pie.getLocalizedMessage(), pie);
				}
			}
		});
		commitLink.setFont(JFaceResources.getBannerFont());
		commitLink.setForeground(JFaceColors.getHyperlinkText(commitLink
				.getDisplay()));
		Cursor handCursor = new Cursor(commitLink.getDisplay(), SWT.CURSOR_HAND);
		UIUtils.hookDisposal(commitLink, handCursor);
		commitLink.setCursor(handCursor);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(commitLink);

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

	public void setInput(Object input) {
		this.revision = (BlameRevision) input;

		RevCommit commit = this.revision.getCommit();

		String linkText = MessageFormat.format(
				UIText.BlameInformationControl_Commit, commit.name());
		commitLink.setText(linkText);
		StyleRange styleRange = new StyleRange(0, linkText.length(), null, null);
		styleRange.underline = true;
		commitLink.setStyleRange(styleRange);

		PersonIdent author = commit.getAuthorIdent();
		if (author != null) {
			setControlVisible(authorLabel, true);
			authorLabel.setText(MessageFormat.format(
					UIText.BlameInformationControl_Author, author.getName(),
					author.getEmailAddress(), author.getWhen()));
		} else
			setControlVisible(authorLabel, false);

		PersonIdent committer = commit.getCommitterIdent();
		setControlVisible(authorLabel, author != null);
		if (committer != null && !committer.equals(author)) {
			setControlVisible(committerLabel, true);
			committerLabel.setText(MessageFormat.format(
					UIText.BlameInformationControl_Committer,
					committer.getName(), committer.getEmailAddress(),
					committer.getWhen()));
		} else
			setControlVisible(committerLabel, false);

		messageText.setText(commit.getFullMessage());

		scrolls.setMinSize(displayArea.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
}
