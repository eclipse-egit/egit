/*******************************************************************************
 * Copyright (C) 2016 Thomas Wolf <thomas.wolf@paranor.ch>.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.util.Map;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.SubActionBars;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * An {@link IEditorActionBarContributor} for the {@link CommitEditor} that
 * provides and properly activates and deactivates a
 * {@link TextEditorActionContributor} for any page that is an
 * {@link ITextEditor}.
 */
public class CommitEditorActionBarContributor
		extends MultiPageEditorActionBarContributor {

	private TextEditorActionContributor textActionContributor = new TextEditorActionContributor();

	private SubActionBars textEditorBars;

	private IFormPage currentPage;

	@Override
	public void init(IActionBars bars) {
		super.init(bars);
		textEditorBars = new SubActionBars(bars);
		textActionContributor.init(textEditorBars);
	}

	@Override
	public void dispose() {
		textActionContributor.dispose();
		textEditorBars.dispose();
		super.dispose();
	}

	@Override
	public void setActivePage(IEditorPart activeEditor) {
		IFormPage formerPage = currentPage;
		if (activeEditor instanceof IFormPage) {
			currentPage = (IFormPage) activeEditor;
		} else {
			currentPage = null;
		}
		if (formerPage != null && !formerPage.isEditor()
				&& currentPage != null && !currentPage.isEditor()) {
			getActionBars().updateActionBars();
			return;
		}
		boolean isTextEditor = currentPage instanceof ITextEditor;
		if (isTextEditor && currentPage == formerPage) {
			return;
		}
		getTextEditorActionContributor().setActiveEditor(currentPage);
		updateTextEditorContributions(isTextEditor);
		getActionBars().updateActionBars();
	}

	private void updateTextEditorContributions(boolean activate) {
		IActionBars rootBars = getActionBars();
		rootBars.clearGlobalActionHandlers();
		if (activate) {
			textEditorBars.activate();
			Map<?, ?> handlers = textEditorBars.getGlobalActionHandlers();
			if (handlers != null) {
				for (Map.Entry<?, ?> entry : handlers.entrySet()) {
					Object key = entry.getKey();
					Object value = entry.getValue();
					if (key instanceof String && value instanceof IAction) {
						rootBars.setGlobalActionHandler((String) key,
								(IAction) value);
					}
				}
			}
		} else {
			textEditorBars.deactivate();
		}
	}

	/**
	 * Gets the nested contributor for {@link ITextEditor}s.
	 *
	 * @return the {@link IEditorActionBarContributor}
	 */
	public IEditorActionBarContributor getTextEditorActionContributor() {
		return textActionContributor;
	}
}
