/*******************************************************************************
 * Copyright (C) 2008, Marek Zawirski <marek.zawirski@gmail.com>
 * Copyright (C) 2011, Dariusz Luksza <dariusz.luksza@gmail.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.components;

import static org.eclipse.jface.resource.ImageDescriptor.createFromImageData;
import static org.eclipse.jface.resource.JFaceResources.getResources;

import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Label provider displaying native check boxes images for boolean values.
 * Label-image is centralized.
 * <p>
 * Concrete implementations must provide object to boolean mapping.
 * <p>
 * This implementation is actually workaround for lacking features in
 * TableViewer. It is based on (workaround) snippets&tricks found on Internet.
 */
public abstract class CheckboxLabelProvider extends CenteredImageLabelProvider {

	private static class CheckBoxImages {
		private final Image checkedEnabled;

		private final Image uncheckedEnabled;

		private final Image checkedDisabled;

		private final Image uncheckedDisabled;

		public CheckBoxImages(Image checkedEnabled, Image uncheckedEnabled,
				Image checkedDisabled, Image uncheckedDisabled) {
			this.checkedEnabled = checkedEnabled;
			this.uncheckedEnabled = uncheckedEnabled;
			this.checkedDisabled = checkedDisabled;
			this.uncheckedDisabled = uncheckedDisabled;
		}

	}

	private final CheckBoxImages checkBoxes;

	private final LocalResourceManager resourceManager;

	private static CheckBoxImages createCheckboxImage(
			ResourceManager resourceManager, Control control) {

		String checkboxhack = System.getProperty("egit.swt.checkboxhack"); //$NON-NLS-1$
		if (checkboxhack == null)
			if (Platform.getOS().equals(Platform.OS_MACOSX))
				checkboxhack = "hardwired"; //$NON-NLS-1$
			else
				checkboxhack = "screenshot"; //$NON-NLS-1$

		if ("hardwired".equals(checkboxhack)) //$NON-NLS-1$
			return new CheckBoxImages(
					UIIcons.CHECKBOX_ENABLED_CHECKED.createImage(),
					UIIcons.CHECKBOX_ENABLED_UNCHECKED.createImage(),
					UIIcons.CHECKBOX_DISABLED_CHECKED.createImage(),
					UIIcons.CHECKBOX_DISABLED_UNCHECKED.createImage());

		Shell shell = new Shell(control.getShell(), SWT.NO_TRIM);
		// Hopefully no platform uses exactly this color because we'll make
		// it transparent in the image.
		Color gray = resourceManager.createColor(new RGB(222, 223, 224));

		Composite composite = new Composite(shell, SWT.NONE);
		RowLayout layout = new RowLayout();
		layout.marginTop = 0;
		layout.marginLeft = 0;
		layout.marginBottom = 0;
		layout.marginRight = 0;
		layout.spacing = 0;
		composite.setLayout(layout);
		createButton(composite, gray, true, true);
		createButton(composite, gray, false, true);
		createButton(composite, gray, true, false);
		createButton(composite, gray, false, false);

		Point cSize = composite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		composite.setSize(cSize);
		shell.setBackground(gray);
		shell.setLocation(0, 0);
		shell.setSize(cSize);

		Display display = shell.getShell().getDisplay();

		shell.open();
		Image canvas = new Image(display, cSize.x, cSize.y);
		GC gc = new GC(canvas);
		composite.print(gc);

		int buttonX = cSize.x / 4;
		Image[] images = new Image[4];

		for (int i = 0; i < 4; i++) {
			Image image = new Image(display, buttonX, cSize.y);
			gc.copyArea(image, buttonX * i, 0);
			images[i] = getImage(resourceManager, gray, image);
		}

		canvas.dispose();
		gc.dispose();
		shell.close();

		return new CheckBoxImages(images[0], images[1], images[2], images[3]);
	}

	private static void createButton(Composite parent, Color bgColor,
			boolean checked, boolean enabled) {
		Button button = new Button(parent, SWT.CHECK);
		button.setSelection(checked);
		button.setEnabled(enabled);
		button.setBackground(bgColor);
		Point bSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		button.setSize(bSize);
	}

	private static Image getImage(ResourceManager rm, Color bgColor, Image img) {
		ImageData imageData = img.getImageData();
		imageData.transparentPixel = imageData.palette.getPixel(bgColor
				.getRGB());
		Image image = rm.createImage(createFromImageData(imageData));
		img.dispose();

		return image;
	}

	/**
	 * Create label provider for provided viewer.
	 *
	 * @param control
	 *            viewer where label provided is used.
	 */
	public CheckboxLabelProvider(final Control control) {
		resourceManager = new LocalResourceManager(getResources());
		checkBoxes = createCheckboxImage(resourceManager, control);
	}

	@Override
	protected Image getImage(final Object element) {
		if (isEnabled(element)) {
			if (isChecked(element))
				return checkBoxes.checkedEnabled;

			return checkBoxes.uncheckedEnabled;
		} else {
			if (isChecked(element))
				return checkBoxes.checkedDisabled;

			return checkBoxes.uncheckedDisabled;
		}
	}

	@Override
	public void dispose() {
		resourceManager.dispose();
		super.dispose();
	}

	/**
	 * @param element
	 *            element to provide label for.
	 * @return true if checkbox label should be checked for this element, false
	 *         otherwise.
	 */
	protected abstract boolean isChecked(Object element);

	/**
	 * Default implementation always return true.
	 *
	 * @param element
	 *            element to provide label for.
	 * @return true if checkbox label should be enabled for this element, false
	 *         otherwise.
	 */
	protected boolean isEnabled(final Object element) {
		return true;
	}
}
