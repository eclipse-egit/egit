/*******************************************************************************
 * Copyright (C) 2012, Robert Pofuk <rpofuk@gmail.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.internal.mylyn.ui.commit;

import org.eclipse.egit.ui.ICommitMessageEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * Test implemetation for Mylyn commit message editor
 *
 */
public class MylynTaskCommitMessageEditor extends Composite
		implements ICommitMessageEditor {

	/**
	 * @param parent
	 * @param style
	 */
	public MylynTaskCommitMessageEditor(Composite parent, int style) {
		super(parent, style);

		RowLayout rowLayout = new RowLayout();
		rowLayout.spacing = 15;
		rowLayout.marginWidth = 15;
		rowLayout.marginHeight = 15;

		this.setLayout(rowLayout);

		Label label = new Label(this, SWT.NONE);
		label.setText("Demo implementation"); //$NON-NLS-1$
	}

	@Override
	public String getCommitMessage() {
		// TODO Auto-generated method stub
		return ""; //$NON-NLS-1$
	}

	@Override
	public String getText() {
		// TODO Auto-generated method stub
		return ""; //$NON-NLS-1$
	}

	@Override
	public void setText(String text) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getMinHeight() {
		return 20;
	}

	@Override
	public Control getCommentWidget() {
		return this;
	}

	@Override
	public void addVerifyKeyListener(VerifyKeyListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addValueChangeListener(
			CommitMessageChangeListener iCommitMessageChangeListener) {
		// TODO Auto-generated method stub

	}

	@Override
	public Control getEditorWidget() {
		return this;
	}

}
