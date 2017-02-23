/*******************************************************************************
 * Copyright (C) 2017 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.text.MessageFormat;

import org.eclipse.egit.ui.UIUtils;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchCommandConstants;

/**
 * A component for text fields that are intended for branch name input. When a
 * blank is typed, it is automatically replaced by an underscore. Adds content
 * assist that offers a normalized branch name as replacement if the current
 * text does not form a valid ref. Characters that are definitely invalid
 * trigger automatic content assist.
 */
public class BranchNameNormalizer {

	private static final String UNDERSCORE = "_"; //$NON-NLS-1$

	private static final String REGEX_BLANK = "\\h|\\v"; //$NON-NLS-1$

	private static final char[] BRANCH_NAME_NORMALIZER_ACTIVATION_CHARS = "\\~^:?[*@<>|\"" //$NON-NLS-1$
			.toCharArray();

	private final ControlDecoration decorator;

	private boolean visible;

	/**
	 * Creates a new {@link BranchNameNormalizer}.
	 *
	 * @param text
	 *            {@link Text} to operate on
	 * @param tooltipText
	 *            to show on the bulb decorator
	 */
	public BranchNameNormalizer(Text text, String tooltipText) {
		KeyStroke stroke = UIUtils.getKeystrokeOfBestActiveBindingFor(
				IWorkbenchCommandConstants.EDIT_CONTENT_ASSIST);
		if (stroke == null) {
			stroke = KeyStroke.getInstance(SWT.MOD1, ' ');
		}
		decorator = UIUtils.addBulbDecorator(text,
				MessageFormat.format(tooltipText, stroke.format()));
		decorator.hide();
		ContentProposalAdapter proposer = new ContentProposalAdapter(text,
				new TextContentAdapter(),
				(c, p) -> {
					if (c.isEmpty() || Repository
							.isValidRefName(Constants.R_HEADS + c)) {
						return null;
					}
					String normalized = Repository.normalizeBranchName(c);
					if (normalized.isEmpty()) {
						return new ContentProposal[0];
					}
					return new ContentProposal[] {
							new ContentProposal(normalized) };
				}, stroke, BRANCH_NAME_NORMALIZER_ACTIVATION_CHARS);
		proposer.setProposalAcceptanceStyle(
				ContentProposalAdapter.PROPOSAL_REPLACE);
		text.addVerifyListener(
				e -> e.text = e.text.replaceAll(REGEX_BLANK, UNDERSCORE));
		text.addModifyListener(e -> {
			String input = text.getText();
			boolean doProposeCorrection = !input.isEmpty()
					&& !Repository.isValidRefName(Constants.R_HEADS + input);
			setVisible(doProposeCorrection);
		});
	}

	/**
	 * Determines whether the decorator is visible.
	 *
	 * @return {@code true} if the decorator is visible.
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * Changes the visibility of the decorator.
	 *
	 * @param visible
	 *            {@code true} to show the decorator; {@code false} otherwise
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
		if (visible) {
			decorator.show();
		} else {
			decorator.hide();
		}
	}

}
