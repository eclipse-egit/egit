/*******************************************************************************
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.themes.ColorUtil;

/**
 * A {@link LineNumberRulerColumn} specialized for a viewer showing a
 * {@link DiffDocument}: it can display logical line numbers corresponding to
 * the shown diff hunks (both old and new), or alternatively the physical line
 * numbers of the unified diff itself.
 */
public class OldNewLogicalLineNumberRulerColumn extends LineNumberRulerColumn {

	/** Container for assembling the actual line number columns. */
	private final CompositeRuler composite = new CompositeRuler(0);
	// Must not have any gap, otherwise mouse wheel scrolling won't work when
	// the mouse cursor happens to be exactly on the gap.

	/** Standard physical line numbers for plain display. */
	private final LineNumberRulerColumn plainLines = new LineNumberRulerColumn() {

		@Override
		public Control createControl(CompositeRuler parentRuler,
				Composite parentControl) {
			return addMenuListener(
					super.createControl(parentRuler, parentControl));
		}
	};

	/**
	 * Ruler for the old line numbers; draws a vertical line on its right edge
	 * in order to get a visual separation of old and new line numbers.
	 * <p>
	 * Note that the rulers are always on the left side of the viewer, even in
	 * an RTL layout. They are also always ordered as given by their indices in
	 * a CompositeRuler -- there is no logic anywhere that would flip the order
	 * when the layout direction is changed. Thus drawing the line always on the
	 * right edge of the left ruler is correct in all cases.
	 * </p>
	 */
	private final LineNumberRulerColumn oldLines = new LogicalLineNumberRulerColumn(
			DiffEntry.Side.OLD) {

		private ResourceManager resourceManager;

		private Color lineColor;

		@Override
		public Control createControl(CompositeRuler parentRuler,
				Composite parentControl) {
			return addMenuListener(
					super.createControl(parentRuler, parentControl));
		}

		@Override
		public int getWidth() {
			// Add space for the line plus one empty pixel on each side of the
			// line.
			return super.getWidth() + 3;
		}

		@Override
		protected void paintLine(int line, int y, int lineHeight, GC gc,
				Display display) {
			super.paintLine(line, y, lineHeight, gc, display);
			// Draw a vertical line to separate old numbers from the new ones.
			int x = super.getWidth() + 1;
			if (lineColor == null) {
				if (resourceManager == null) {
					resourceManager = new LocalResourceManager(
							JFaceResources.getResources());
				}
				lineColor = resourceManager.createColor(
						ColorUtil.blend(gc.getForeground().getRGB(),
								gc.getBackground().getRGB(), 60));
			}
			Color foreground = gc.getForeground();
			// Needs to redraw the whole line each time, otherwise it'll have
			// gaps when word-wrapping is on. There doesn't seem to be any hook
			// available to hook into drawing after all line numbers have been
			// drawn.
			Rectangle bounds = super.getControl().getBounds();
			gc.setForeground(lineColor);
			gc.drawLine(x, 0, x, bounds.height);
			gc.setForeground(foreground);
		}

		@Override
		public void setForeground(Color foreground) {
			lineColor = null;
			super.setForeground(foreground);
		}

		@Override
		public void setBackground(Color background) {
			lineColor = null;
			super.setBackground(background);
		}

		@Override
		protected void handleDispose() {
			super.handleDispose();
			if (resourceManager != null) {
				resourceManager.dispose();
				resourceManager = null;
			}
			lineColor = null;
		}
	};

	/** Ruler for the new line numbers. */
	private final LineNumberRulerColumn newLines = new LogicalLineNumberRulerColumn(
			DiffEntry.Side.NEW) {

		@Override
		public Control createControl(CompositeRuler parentRuler,
				Composite parentControl) {
			return addMenuListener(
					super.createControl(parentRuler, parentControl));
		}
	};

	/**
	 * Current display mode. If {@code true}, showing only physical line
	 * numbers, otherwise showing both old and new logical line numbers.
	 */
	private boolean plain;

	/**
	 * We need our own listener for SWT.MenuDetect. The framework does propagate
	 * the parent's listener to children, but our own CompositeRuler then
	 * propagates its own to _its_ children. And that one looks for a menu set
	 * on its own control. However, the text editor framework sets the menu only
	 * on the outer CompositeRuler, and thus the context menu will not appear on
	 * our own columns unless we set a MenuDetect listener ourselves.
	 */
	private Listener menuListener;

	/**
	 * Creates a new {@link OldNewLogicalLineNumberRulerColumn} showing both old
	 * and new logical line numbers.
	 */
	public OldNewLogicalLineNumberRulerColumn() {
		this(false);
	}

	/**
	 * Creates a new {@link OldNewLogicalLineNumberRulerColumn}.
	 *
	 * @param plain
	 *            {@code true} to show physical line numbers only, or
	 *            {@code false} to show both old and new logical line numbers.
	 */
	public OldNewLogicalLineNumberRulerColumn(boolean plain) {
		this.plain = plain;
		if (!plain) {
			composite.addDecorator(0, oldLines);
			composite.addDecorator(1, newLines);
		} else {
			composite.addDecorator(0, plainLines);
		}
	}

	@Override
	public void setModel(IAnnotationModel model) {
		composite.setModel(model);
	}

	@Override
	public void redraw() {
		composite.immediateUpdate();
	}

	@Override
	public Control createControl(CompositeRuler parentRuler,
			Composite parentControl) {
		menuListener = (event) -> {
			if (event.type == SWT.MenuDetect) {
				Menu contextMenu = parentControl.getMenu();
				if (contextMenu != null) {
					contextMenu.setLocation(event.x, event.y);
					contextMenu.setVisible(true);
				}
			}
		};
		return composite.createControl(parentControl,
				parentRuler.getTextViewer());
	}

	private Control addMenuListener(Control control) {
		if (menuListener != null) {
			control.addListener(SWT.MenuDetect, menuListener);
		}
		return control;
	}

	@Override
	public Control getControl() {
		return composite.getControl();
	}

	@Override
	public int getWidth() {
		return composite.getWidth();
	}

	@Override
	public void setFont(Font font) {
		plainLines.setFont(font);
		oldLines.setFont(font);
		newLines.setFont(font);
	}

	@Override
	public void setForeground(Color foreground) {
		super.setForeground(foreground);
		plainLines.setForeground(foreground);
		oldLines.setForeground(foreground);
		newLines.setForeground(foreground);
	}

	@Override
	public void setBackground(Color background) {
		super.setBackground(background);
		plainLines.setBackground(background);
		oldLines.setBackground(background);
		newLines.setBackground(background);
	}

	/**
	 * Tells whether the column is currently plain, showing only physical line
	 * numbers.
	 *
	 * @return {@code true} if only physical line numbers are shown;
	 *         {@code false} if old and new logical line numbers are shown
	 */
	public boolean isPlain() {
		return plain;
	}

	/**
	 * Switches the mode of the {@link OldNewLogicalLineNumberRulerColumn}.
	 *
	 * @param plain
	 *            {@code true} to show only physical line numbers; {@code false}
	 *            to show old and new logical line numbers.
	 */
	public void setPlain(boolean plain) {
		if (this.plain != plain) {
			this.plain = plain;
			if (!plain) {
				composite.removeDecorator(plainLines);
				composite.addDecorator(0, oldLines);
				composite.addDecorator(1, newLines);
			} else {
				composite.removeDecorator(oldLines);
				composite.removeDecorator(newLines);
				composite.addDecorator(0, plainLines);
			}
		}
	}
}
