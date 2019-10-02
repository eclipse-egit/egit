/*******************************************************************************
 * Copyright (C) 2013 Robin Stocker <robin@nibor.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.common;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;

import java.util.List;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.eclipse.ui.IEditorReference;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class CompareEditorTester {

	private final SWTBotEditor editor;

	public static CompareEditorTester forTitleContaining(String title) {
		SWTWorkbenchBot bot = new SWTWorkbenchBot();
		SWTBotEditor editor = bot.editor(new CompareEditorTitleMatcher(title));
		// Ensure that both StyledText widgets are enabled
		SWTBotStyledText styledText = editor.toTextEditor().getStyledText();
		bot.waitUntil(Conditions.widgetIsEnabled(styledText));
		return new CompareEditorTester(editor);
	}

	private CompareEditorTester(SWTBotEditor editor) {
		this.editor = editor;
	}

	public SWTBotStyledText getLeftEditor() {
		return getNonAncestorEditor(0);
	}

	public SWTBotStyledText getRightEditor() {
		return getNonAncestorEditor(1);
	}

	public boolean isDirty() {
		return editor.isDirty();
	}

	public void save() {
		editor.save();
	}

	public void close() {
		editor.close();
	}

	// Needed because sometimes the ancestor is also shown as a styled text. An
	// alternative solution would be to click on "Hide Ancestor Pane", but that
	// is slower because it waits if the toggle is named "Show Ancestor Pane".
	private SWTBotStyledText getNonAncestorEditor(int index) {
		List<StyledText> texts = editor.bot().getFinder()
				.findControls(widgetOfType(StyledText.class));
		if (texts.size() == 2)
			return new SWTBotStyledText(texts.get(index));
		else if (texts.size() == 3)
			return new SWTBotStyledText(texts.get(index + 1));
		else
			throw new IllegalStateException(
					"Expected compare editor to contain 2 or 3 styled text widgets, but was "
							+ texts.size());
	}

	private static class CompareEditorTitleMatcher extends
			BaseMatcher<IEditorReference> {

		private final String titleSubstring;

		public CompareEditorTitleMatcher(String titleSubstring) {
			this.titleSubstring = titleSubstring;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("Compare editor that title contains text: "
					+ titleSubstring);
		}

		@Override
		public boolean matches(Object item) {
			if (item instanceof IEditorReference) {
				IEditorReference editor = (IEditorReference) item;
				String id = editor.getId();
				String title = editor.getTitle();
				return id.equals("org.eclipse.compare.CompareEditor")
						&& title.contains(titleSubstring);
			}

			return false;
		}
	}
}
