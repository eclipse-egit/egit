/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.lang.reflect.Field;

import org.eclipse.egit.ui.UIUtils;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
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
	 */
	public HeaderText(Form form, String text) {
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

			TextViewer titleViewer = new TextViewer(titleRegion, SWT.READ_ONLY);
			titleViewer.setDocument(new Document(text));

			titleLabel = titleViewer.getTextWidget();
			titleLabel.setForeground(heading.getForeground());
			titleLabel.setFont(heading.getFont());
			titleLabel.addFocusListener(new FocusAdapter() {
				public void focusLost(FocusEvent e) {
					titleLabel.setSelection(0);
				}
			});

			Point size = titleLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			Image emptyImage = new Image(heading.getDisplay(), size.x, size.y);
			UIUtils.hookDisposal(titleLabel, emptyImage);
			busyLabel.setImage(emptyImage);

			busyLabel.addControlListener(new ControlAdapter() {
				public void controlMoved(ControlEvent e) {
					updateSizeAndLocations();
				}
			});
			titleLabel.moveAbove(busyLabel);
			titleRegion.addControlListener(new ControlAdapter() {
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
}
