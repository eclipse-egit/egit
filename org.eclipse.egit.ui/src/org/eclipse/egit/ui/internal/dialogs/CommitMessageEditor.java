/*******************************************************************************
 * Copyright (C) 2012, Robert Pofuk <rpofuk@gmail.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

/**
 * Interface for creating commit message view.
 *
 */
public interface CommitMessageEditor {

	/**
	 * Returns the commit message after processing row message text
	 *
	 * @return commit message
	 */
	public String getCommitMessage();

	/**
	 * @return raw message text
	 */
	public String getText();

	/**
	 * Set raw message text
	 *
	 * @param text
	 *
	 */
	public void setText(String text);

	/**
	 * Interface for message change event
	 *
	 */


	/**
	 * @return result of set Focus operation
	 *
	 */
	public boolean setFocus();


	/**
	 * @return is component enabled
	 */
	public boolean isEnabled();



	/**
	 * @return min hight of widget
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
	public Control getWidget();

	/**
	 * @param enabled
	 */
	public void setEnabled(boolean enabled);


	/**
	 * @param listener
	 */
	public void addVerifyKeyListener(VerifyKeyListener listener);

	/**
	 * @param iCommitMessageChangeListener
	 */
	public void addValueChangeListener(
			CommitMessageChangeListener iCommitMessageChangeListener);

	/**
	 * @author WN00079745
	 *
	 */
	public interface CommitMessageChangeListener

	{

		/**
		 * Invoked when commit message is changed
		 */
		public void commitMessageChanged();
	}
}
