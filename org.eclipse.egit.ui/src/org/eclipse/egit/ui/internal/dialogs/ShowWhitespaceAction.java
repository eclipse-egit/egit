/*******************************************************************************
 * Copyright (c) 2012, 2019 Tomasz Zarna (IBM), Robin Stocker, and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Thomas Wolf <thomas.wolf@paranor.ch> - Factored out of SpellcheckableMessageArea.
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.core.runtime.Assert;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IPainter;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.WhitespaceCharacterPainter;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.texteditor.AbstractTextEditor;

/**
 * An action to toggle showing whitespace in a an {@link ITextViewer}.
 */
public class ShowWhitespaceAction extends TextEditorPropertyAction {

	/**
	 * Creates a new {@link ShowWhitespaceAction}.
	 *
	 * @param viewer
	 *            to operate on
	 * @param initiallyOff
	 *            whether to show whitespace initially
	 */
	public ShowWhitespaceAction(ITextViewer viewer, boolean initiallyOff) {
		super(UIText.SpellcheckableMessageArea_showWhitespace, viewer,
				AbstractTextEditor.PREFERENCE_SHOW_WHITESPACE_CHARACTERS,
				initiallyOff);
	}

	private IPainter whitespaceCharPainter;

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getProperty();
		if (property == null) {
			return;
		}
		switch (property) {
		case AbstractTextEditor.PREFERENCE_SHOW_WHITESPACE_CHARACTERS:
		case AbstractTextEditor.PREFERENCE_SHOW_LEADING_SPACES:
		case AbstractTextEditor.PREFERENCE_SHOW_ENCLOSED_SPACES:
		case AbstractTextEditor.PREFERENCE_SHOW_TRAILING_SPACES:
		case AbstractTextEditor.PREFERENCE_SHOW_LEADING_IDEOGRAPHIC_SPACES:
		case AbstractTextEditor.PREFERENCE_SHOW_ENCLOSED_IDEOGRAPHIC_SPACES:
		case AbstractTextEditor.PREFERENCE_SHOW_TRAILING_IDEOGRAPHIC_SPACES:
		case AbstractTextEditor.PREFERENCE_SHOW_LEADING_TABS:
		case AbstractTextEditor.PREFERENCE_SHOW_ENCLOSED_TABS:
		case AbstractTextEditor.PREFERENCE_SHOW_TRAILING_TABS:
		case AbstractTextEditor.PREFERENCE_SHOW_CARRIAGE_RETURN:
		case AbstractTextEditor.PREFERENCE_SHOW_LINE_FEED:
		case AbstractTextEditor.PREFERENCE_WHITESPACE_CHARACTER_ALPHA_VALUE:
			synchronizeWithPreference();
			break;
		default:
			break;
		}
	}

	@Override
	protected final String getPreferenceKey() {
		return AbstractTextEditor.PREFERENCE_SHOW_WHITESPACE_CHARACTERS;
	}

	@Override
	protected void toggleState(boolean checked) {
		if (checked) {
			installPainter();
		} else {
			uninstallPainter();
		}
	}

	/**
	 * Installs the painter on the viewer.
	 */
	private void installPainter() {
		Assert.isTrue(whitespaceCharPainter == null);
		ITextViewer v = getTextViewer();
		if (v instanceof ITextViewerExtension2) {
			IPreferenceStore store = getStore();
			whitespaceCharPainter = new WhitespaceCharacterPainter(v,
					store.getBoolean(
							AbstractTextEditor.PREFERENCE_SHOW_LEADING_SPACES),
					store.getBoolean(
							AbstractTextEditor.PREFERENCE_SHOW_ENCLOSED_SPACES),
					store.getBoolean(
							AbstractTextEditor.PREFERENCE_SHOW_TRAILING_SPACES),
					store.getBoolean(
							AbstractTextEditor.PREFERENCE_SHOW_LEADING_IDEOGRAPHIC_SPACES),
					store.getBoolean(
							AbstractTextEditor.PREFERENCE_SHOW_ENCLOSED_IDEOGRAPHIC_SPACES),
					store.getBoolean(
							AbstractTextEditor.PREFERENCE_SHOW_TRAILING_IDEOGRAPHIC_SPACES),
					store.getBoolean(
							AbstractTextEditor.PREFERENCE_SHOW_LEADING_TABS),
					store.getBoolean(
							AbstractTextEditor.PREFERENCE_SHOW_ENCLOSED_TABS),
					store.getBoolean(
							AbstractTextEditor.PREFERENCE_SHOW_TRAILING_TABS),
					store.getBoolean(
							AbstractTextEditor.PREFERENCE_SHOW_CARRIAGE_RETURN),
					store.getBoolean(
							AbstractTextEditor.PREFERENCE_SHOW_LINE_FEED),
					store.getInt(
							AbstractTextEditor.PREFERENCE_WHITESPACE_CHARACTER_ALPHA_VALUE));
			((ITextViewerExtension2) v).addPainter(whitespaceCharPainter);
		}
	}

	/**
	 * Remove the painter from the viewer.
	 */
	private void uninstallPainter() {
		if (whitespaceCharPainter == null) {
			return;
		}
		ITextViewer v = getTextViewer();
		if (v instanceof ITextViewerExtension2) {
			((ITextViewerExtension2) v).removePainter(whitespaceCharPainter);
		}
		whitespaceCharPainter.deactivate(true);
		whitespaceCharPainter = null;
	}
}
