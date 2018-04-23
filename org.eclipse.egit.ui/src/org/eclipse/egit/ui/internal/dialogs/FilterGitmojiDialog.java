/*******************************************************************************
 * Copyright (C) 2018, Thibault Falque <thibault.falque@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.dialogs;

import java.util.Collection;

import org.eclipse.egit.core.internal.gitmoji.Gitmoji;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * A dialog for showing Gitmojis to the user.
 */
public class FilterGitmojiDialog extends ElementListSelectionDialog {

	/**
	 * Creates a list selection dialog with a list of Gitmojis.
	 *
	 * @param shell
	 *            the shell object
	 * @param gitmojis
	 *            the gitmojis to show
	 */
	public FilterGitmojiDialog(Shell shell, Collection<Gitmoji> gitmojis) {
		super(shell, new LabelProvider());
		setTitle("Choose your gitmoji"); //$NON-NLS-1$
		setMessage("Search your gitmoji"); //$NON-NLS-1$
		setElements(gitmojis.toArray());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.dialogs.AbstractElementListSelectionDialog#
	 * createFilterText(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Text createFilterText(Composite parent) {
		Text text = super.createFilterText(parent);
		Listener listener = new Listener() {
			@Override
			public void handleEvent(Event e) {
				fFilteredList.setFilter('*' + text.getText());
			}
		};
		text.addListener(SWT.Modify, listener);
		return text;
	}

}
