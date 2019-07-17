/*******************************************************************************
 * Copyright (C) 2010, Dariusz Luksza <dariusz@luksza.org>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Assert;
import org.eclipse.egit.core.internal.Utils;
import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

/**
 * This is an extended version of {@link Combo} widget with is specialized in
 * displaying commits and simplifying selection process.
 *
 * It is integrated with {@link ContentProposalAdapter} that helps select
 * preferred tag by user. To get activate proposal provider simply just start
 * writing commit SHA-1 or part of commit's message first line
 */
public class CommitCombo extends Composite {

	private final List<ComboCommitEnt> commits;

	private final Combo combo;

	private static class ComboCommitEnt {

		private final String message;

		private final ObjectId objectId;

		public ComboCommitEnt(ObjectId objecId, String message) {
			this.objectId = objecId;
			this.message = message;
		}

	}

	private class CommitContentProposalProvider implements
			IContentProposalProvider {

		@Override
		public IContentProposal[] getProposals(String contents, int position) {
			List<IContentProposal> list = new ArrayList<>();
			Pattern pattern = Pattern.compile(contents,
					Pattern.CASE_INSENSITIVE);
			for (int i = 0; i < commits.size(); i++) {
				String message = commits.get(i).message;
				if (message.length() >= contents.length()
						&& pattern.matcher(message).find()) {
					list.add(makeContentProposal(message));
				}
			}
			return list.toArray(new IContentProposal[0]);
		}

		/*
		 * Make an IContentProposal for showing the specified String.
		 */
		private IContentProposal makeContentProposal(final String proposal) {
			return new IContentProposal() {
				@Override
				public String getContent() {
					return proposal;
				}

				@Override
				public String getDescription() {
					return null;
				}

				@Override
				public String getLabel() {
					return null;
				}

				@Override
				public int getCursorPosition() {
					return proposal.length();
				}
			};
		}
	}

	/**
	 * Constructs a new instance of this class given its parent and a style
	 * value describing its behavior and appearance.
	 *
	 * @param parent
	 *            a widget which will be the parent of the new instance (cannot
	 *            be null)
	 * @param style
	 *            the SWT style bits
	 */
	public CommitCombo(Composite parent, int style) {
		super(parent, style);

		combo = new Combo(this, SWT.DROP_DOWN);
		commits = new ArrayList<>();

		setLayout(GridLayoutFactory.swtDefaults().create());
		setLayoutData(GridDataFactory.fillDefaults().create());

		GridData totalLabelData = new GridData();
		totalLabelData.horizontalAlignment = SWT.FILL;
		totalLabelData.grabExcessHorizontalSpace = true;
		combo.setLayoutData(totalLabelData);
		combo.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				if (null == getValue())
					combo.setText(""); //$NON-NLS-1$
			}
		});

		UIUtils
				.addBulbDecorator(combo,
						UIText.CommitCombo_showSuggestedCommits);

		ContentProposalAdapter adapter = new ContentProposalAdapter(combo,
				new ComboContentAdapter(), new CommitContentProposalProvider(),
				null, null);
		adapter.setPropagateKeys(true);
		adapter
				.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	}

	/**
	 * Add a {@link RevCommit} to widget.
	 *
	 * @param revCommit
	 */
	public void add(RevCommit revCommit) {
		Assert.isNotNull(revCommit);
		checkWidget();

		String shortSha1 = Utils.getShortObjectId(revCommit);
		String message = shortSha1 + ": " + revCommit.getShortMessage(); //$NON-NLS-1$
		combo.add(message);
		commits.add(new ComboCommitEnt(revCommit.getId(), message));
	}

	/**
	 * Returns value of SHA-1 for selected commit.
	 *
	 * @param index
	 *            index of item in check box
	 * @return SHA-1 of selected commit
	 */
	public ObjectId getItem(int index) {
		checkWidget();

		if (!(0 <= index && index < commits.size())) {
			SWT.error(SWT.ERROR_INVALID_RANGE);
		}
		return commits.get(index).objectId;
	}

	/**
	 * @return the number of items
	 */
	public int getItemCount() {
		return commits.size();
	}

	/**
	 * @return index of selected element
	 */
	public int getSelectedIndex() {
		int selectionIndex = combo.getSelectionIndex();
		if (selectionIndex == -1)
			selectionIndex = combo.indexOf(combo.getText());
		return selectionIndex;
	}

	/**
	 * @return SHA-1 of selected commit
	 */
	public ObjectId getValue() {
		int selectionIndex = getSelectedIndex();
		return -1 != selectionIndex ? getItem(selectionIndex) : null;
	}

	/**
	 * Selects the item with is associated with given <code>objectId</code>
	 *
	 * @param objectId
	 */
	public void setSelectedElement(ObjectId objectId) {
		if (objectId == null) {
			return;
		}

		for (int i = 0; i < commits.size(); i++)
			if (objectId.equals(commits.get(i).objectId)) {
				combo.select(i);
				break;
			}
	}

	@Override
	public void setEnabled(boolean enabled) {
		combo.setEnabled(enabled);
		super.setEnabled(enabled);
	}

	/**
	 * Sets the selection in the receiver's text field to an empty selection
	 * starting just before the first character. If the text field is editable,
	 * this has the effect of placing the i-beam at the start of the text.
	 */
	public void clearSelection() {
		combo.clearSelection();
		combo.setText(""); //$NON-NLS-1$
	}

}
