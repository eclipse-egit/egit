/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Daniel Megert <daniel_megert@ch.ibm.com> - Added context menu to the Commit Editor's header text
 *****************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.lang.reflect.Field;

import org.eclipse.egit.ui.UIUtils;
import org.eclipse.egit.ui.internal.ClipboardUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.internal.forms.widgets.BusyIndicator;
import org.eclipse.ui.internal.forms.widgets.FormHeading;
import org.eclipse.ui.internal.forms.widgets.TitleRegion;

/**
 * Header text class to render selectable text instead of a label on the form
 * heading.
 *
 * Portions of this code were lifted from the Mylyn TaskEditor class that
 * applies a similar technique.
 */
@SuppressWarnings("restriction")
public class HeaderText {

	private StyledText titleLabel;

	private BusyIndicator busyLabel;

	/**
	 * @param form
	 * @param text
	 *            the displayed text
	 * @param sha1String
	 *            string form of the SHA-1, in lower case hexadecimal
	 */
	public HeaderText(Form form, String text, String sha1String) {
		try {
			FormHeading heading = (FormHeading) form.getHead();
			heading.setBusy(true);
			heading.setBusy(false);

			Field field = FormHeading.class.getDeclaredField("titleRegion"); //$NON-NLS-1$
			field.setAccessible(true);
			TitleRegion titleRegion = (TitleRegion) field.get(heading);

			for (Control child : titleRegion.getChildren())
				if (child instanceof BusyIndicator) {
					busyLabel = (BusyIndicator) child;
					break;
				}
			if (busyLabel == null)
				throw new IllegalArgumentException();

			final TextViewer titleViewer = new TextViewer(titleRegion, SWT.READ_ONLY);
			titleViewer.setDocument(new Document(text));

			titleLabel = titleViewer.getTextWidget();
			titleLabel.setForeground(heading.getForeground());
			titleLabel.setFont(heading.getFont());
			titleLabel.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					titleLabel.setSelection(0);
					Event selectionEvent = new Event();
					selectionEvent.x = 0;
					selectionEvent.y = 0;
					titleLabel.notifyListeners(SWT.Selection, selectionEvent);
				}
			});
			createContextMenu(titleLabel, sha1String);

			Point size = titleLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			Image emptyImage = new Image(heading.getDisplay(), size.x, size.y);
			UIUtils.hookDisposal(titleLabel, emptyImage);
			busyLabel.setImage(emptyImage);

			busyLabel.addControlListener(new ControlAdapter() {
				@Override
				public void controlMoved(ControlEvent e) {
					updateSizeAndLocations();
				}
			});
			titleLabel.moveAbove(busyLabel);
			titleRegion.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(ControlEvent e) {
					updateSizeAndLocations();
				}
			});
			updateSizeAndLocations();
		} catch (NoSuchFieldException e) {
			form.setText(text);
		} catch (IllegalArgumentException e) {
			form.setText(text);
		} catch (IllegalAccessException e) {
			form.setText(text);
		}
	}

	private void updateSizeAndLocations() {
		if (busyLabel == null || busyLabel.isDisposed())
			return;
		if (titleLabel == null || titleLabel.isDisposed())
			return;
		Point size = titleLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		int y = (titleLabel.getParent().getSize().y - size.y) / 2;
		titleLabel.setBounds(busyLabel.getLocation().x, y, size.x, size.y);
	}

	private static void createContextMenu(final StyledText styledText, final String sha1String) {
		Menu menu = new Menu(styledText);

		final MenuItem copySHA1MenuItem = new MenuItem(menu, SWT.PUSH);
		copySHA1MenuItem.setText(UIText.Header_contextMenu_copy_SHA1);
		final Shell shell = styledText.getShell();
		copySHA1MenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				ClipboardUtils.copySha1ToClipboard(sha1String, shell);
			}
		});

		final MenuItem copyMenuItem = new MenuItem(menu, SWT.PUSH);
		copyMenuItem.setText(UIText.Header_contextMenu_copy);
		copyMenuItem.setEnabled(false);
		copyMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				styledText.copy();
			}
		});
		styledText.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				copyMenuItem.setEnabled(styledText.getSelectionCount() > 0);
			}
		});

		styledText.setMenu(menu);
	}

	/**
	 * Retrieves the {@link Control} used for the title.
	 *
	 * @return the {@link Control}, or {@code null} if none
	 */
	public Control getControl() {
		return titleLabel;
	}
}
