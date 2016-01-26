/*******************************************************************************
 * Copyright (C) 2012, Robert Pofuk <rpofuk@gmail.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui;

import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

/**
 * Interface for creating commit message view.
 *
 */
public interface ICommitMessageEditor {

	/**
	 * Returns the commit message after processing row message text
	 *
	 * @return commit message
	 */
	public String getCommitMessage();

	/**
	 * Returns unformated message text
	 *
	 * @return raw message text
	 */
	public String getText();

	/**
	 * Set raw message text. Sets unformatted message text
	 *
	 * @param text
	 *            of commit message
	 *
	 */
	public void setText(String text);

	/**
	 * @return result of set Focus operation
	 * @see Control#setFocus()
	 */
	public boolean setFocus();

	/**
	 * @return is component enabled
	 * @see Control#isEnabled()
	 */
	public boolean isEnabled();

	/**
	 *
	 * Return minimum allowed height for the widget
	 *
	 * @return minimum height of widget
	 */
	public int getMinHeight();

	/**
	 *
	 * @param key
	 * @param value
	 */
	public void setData(String key, Object value);

	/**
	 *
	 * @param layoutData
	 */
	public void setLayoutData(Object layoutData);

	/**
	 * @return widget size
	 */
	public Point getSize();

	/**
	 * @return widget for message
	 */
	public Control getCommentWidget();

	/**
	 * @return entire widget
	 */
	public Control getEditorWidget();

	/**
	 * @param enabled
	 */
	public void setEnabled(boolean enabled);

	/**
	 * Add listener for a key press. This can be used for catching shortcut
	 * events i.e. ctrl+enter. All listeners should be invoked when key is
	 * pressed
	 *
	 * @param listener
	 *            to be invoked when key is pressed
	 */
	public void addVerifyKeyListener(VerifyKeyListener listener);

	/**
	 * Add change listener. All listeners should be invoked when message is
	 * changed
	 *
	 * @param messageChangeListener
	 *            to be invoked when message is changed
	 */
	public void addCommitMessageChangeListener(
			CommitMessageChangeListener messageChangeListener);

	/**
	 * Interface of message change listener
	 *
	 */
	public interface CommitMessageChangeListener {

		/**
		 * Invoked when commit message is changed
		 */
		public void commitMessageChanged();
	}
}
