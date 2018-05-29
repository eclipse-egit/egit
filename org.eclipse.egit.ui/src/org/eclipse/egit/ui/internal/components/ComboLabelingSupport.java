/*******************************************************************************
 * Copyright (C) 2008, 2012 Marek Zawirski <marek.zawirski@gmail.com> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;

/**
 * Support class for Combo, extending its functionality to differentiate between
 * item label and item content.
 * <p>
 * This implementation takes {@link IContentProposal} instances as data source.
 * <p>
 * Use {@link #getContent()} instead of {@link Combo#getText()} to get the
 * current item content (instead of label).
 */
public class ComboLabelingSupport {
	private final Combo combo;

	private List<? extends IContentProposal> proposals;

	/**
	 * Installs labeling support on provided combo. setItems method of combo
	 * shouldn't be called manually after that installation.
	 * <p>
	 * Support class is initialized with empty proposals list.
	 *
	 * @param combo
	 *            target combo to install on.
	 * @param selectionListener
	 *            listener that is notified when content is filled after label
	 *            being clicked. May be null.
	 */
	public ComboLabelingSupport(final Combo combo,
			final SelectionListener selectionListener) {
		this.combo = combo;

		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final int i = combo.getSelectionIndex();
				if (i != -1 && i < proposals.size()) {
					combo.setText(proposals.get(i).getContent());
					if (selectionListener != null)
						selectionListener.widgetSelected(e);
				}
			}
		});
		setProposals(Collections.<IContentProposal> emptyList());
	}

	/**
	 * Sets input data for combo.
	 * <p>
	 * Proposals are set in provided order.
	 *
	 * @param proposals
	 *            model of input data.
	 */
	public void setProposals(final List<? extends IContentProposal> proposals) {
		this.proposals = proposals;

		final String[] itemsLabels = new String[proposals.size()];
		int i = 0;
		for (final IContentProposal p : proposals)
			itemsLabels[i++] = p.getLabel();
		combo.setItems(itemsLabels);
	}

	/**
	 * @return the content of the selected item, or just the text if it does not
	 *         match a proposal
	 */
	public String getContent() {
		String text = combo.getText();
		for (final IContentProposal p : proposals)
			if (text.equals(p.getLabel()))
				return p.getContent();
		return text;
	}
}
