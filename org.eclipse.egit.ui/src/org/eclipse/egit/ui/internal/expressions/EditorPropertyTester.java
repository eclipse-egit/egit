/*******************************************************************************
 * Copyright (C) 2022 Thomas Wolf <twolf@apache.org> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.expressions;

import org.eclipse.jface.text.revisions.IRevisionRulerColumn;
import org.eclipse.jface.text.source.LineNumberChangeRulerColumn;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;

/**
 * Property tester for the active editor, if any.
 * <p>
 * Supports one property test "canShowRevisions", which is {@code true} if the
 * active editor is a text editor with a line number column capable of showing
 * revision information. The property test takes an optional argument
 * "notAlready", in which case the test is only {@code true} if the ruler column
 * additionally does not already show revision information.
 * </p>
 */
public class EditorPropertyTester extends AbstractPropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		return computeResult(expectedValue,
				internalTest(receiver, property, args));
	}

	private boolean internalTest(Object receiver, String property,
			Object[] args) {
		if ("canShowRevisions".equals(property)) { //$NON-NLS-1$
			Object editor = receiver;
			if (editor instanceof MultiPageEditorPart) {
				editor = ((MultiPageEditorPart) editor).getSelectedPage();
			}
			if (!(editor instanceof AbstractDecoratedTextEditor)) {
				return false;
			}
			AbstractDecoratedTextEditor activeEditor = (AbstractDecoratedTextEditor) editor;
			IRevisionRulerColumn column = activeEditor
					.getAdapter(IRevisionRulerColumn.class);
			if (column == null) {
				return false;
			}
			if (args != null && args.length == 1
					&& "notAlready".equals(args[0].toString()) //$NON-NLS-1$
					&& (column instanceof LineNumberChangeRulerColumn)
					&& ((LineNumberChangeRulerColumn) column)
							.isShowingRevisionInformation()) {
				return false;
			}
			return true;
		}
		return false;
	}

}
