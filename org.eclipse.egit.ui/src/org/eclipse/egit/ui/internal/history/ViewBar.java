/*******************************************************************************
 * Copyright (C) 2016 Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import org.eclipse.jface.action.CoolBarManager;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.SubContributionManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;

/**
 * A (typically small) bar that when {@link #setVisible(boolean) shown} appears
 * at the top of the view containing the control its bound to.
 */
public abstract class ViewBar {

	private final Listener listener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			switch (event.type) {
			case SWT.Dispose:
			case SWT.Close:
			case SWT.Hide:
				setVisible(false);
				break;
			case SWT.Resize:
				if (control.getShell() != parentShell) {
					if (!shell.setParent(control.getShell())) {
						// Moving the part between windows... can't re-parent
						// our shell.
						setVisible(false);
						break;
					} else {
						parentShell = control.getShell();
					}
				}
				// This is an optimization: repositioning our shell only on
				// a subsequent paint event on our anchor control gives much
				// less visual lag than doing so right away now.
				control.addListener(SWT.Paint, listener);
				break;
			case SWT.Paint:
				control.removeListener(SWT.Paint, listener);
				move();
				break;
			case SWT.Move:
				move();
				break;
			default:
				break;
			}
		}
	};

	private IPartListener2 partListener = new IPartListener2() {

		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			// Nothing
		}

		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {
			// Nothing
		}

		@Override
		public void partClosed(IWorkbenchPartReference partRef) {
			if (partRef.getPart(false) == workbenchPart) {
				setVisible(false);
			}
		}

		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) {
			// Nothing
		}

		@Override
		public void partOpened(IWorkbenchPartReference partRef) {
			// Nothing
		}

		@Override
		public void partHidden(IWorkbenchPartReference partRef) {
			if (partRef.getPart(false) == workbenchPart) {
				setVisible(false);
			}
		}

		@Override
		public void partVisible(IWorkbenchPartReference partRef) {
			// Nothing
		}

		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) {
			// Nothing
		}
	};

	private final Control control;

	private Layout layout;

	private Shell shell;

	private Shell parentShell;

	private Point originalSize;

	private boolean visible;

	private IPartService partService;

	private IWorkbenchPart workbenchPart;

	/**
	 * Creates a new {@link ViewBar} for the view containing the given control.
	 *
	 * @param control
	 *            the control to which the bar is bound
	 * @param workbenchPart
	 *            to which the bar is bound
	 */
	public ViewBar(Control control, IWorkbenchPart workbenchPart) {
		this.control = control;
		this.workbenchPart = workbenchPart;
		partService = workbenchPart.getSite().getService(IPartService.class);
	}

	private void addListeners() {
		removeListeners();
		control.addListener(SWT.Dispose, listener);
		control.addListener(SWT.Hide, listener);
		control.addListener(SWT.Close, listener);
		control.addListener(SWT.Resize, listener);
		control.addListener(SWT.Move, listener);
		partService.addPartListener(partListener);
	}

	private void removeListeners() {
		if (!control.isDisposed()) {
			control.removeListener(SWT.Dispose, listener);
			control.removeListener(SWT.Hide, listener);
			control.removeListener(SWT.Close, listener);
			control.removeListener(SWT.Resize, listener);
			control.removeListener(SWT.Paint, listener);
			control.removeListener(SWT.Move, listener);
		}
		partService.removePartListener(partListener);
	}

	/**
	 * Shows the {@link ViewBar} at coordinates (0,0) in the view.
	 */
	protected void show() {
		Shell newShell = new Shell(control.getShell(),
				SWT.DOUBLE_BUFFERED | SWT.NO_TRIM | SWT.TOOL);
		if (layout == null) {
			layout = new FillLayout();
		}
		newShell.setLayout(layout);

		shell = newShell;
		parentShell = control.getShell();

		visible = true;
		createContent(newShell);
		addListeners();
		newShell.pack();
		originalSize = newShell.getSize();
		aboutToShow(move());
		newShell.setVisible(true);
	}

	/**
	 * Set the {@link Layout} to use for the contents. If no layout is set, the
	 * {@link ViewBar} will use a horizontal {@link FillLayout}.
	 *
	 * @param layout
	 *            to use
	 */
	public void setLayout(Layout layout) {
		this.layout = layout;
	}

	/**
	 * Retrieves the {@link Layout} used.
	 *
	 * @return the layout, or {@code null} if none set.
	 */
	public Layout getLayout() {
		return layout;
	}

	/**
	 * Resizes the bar according to its contents.
	 */
	public void resize() {
		if (shell != null && !shell.isDisposed()) {
			shell.layout(true, true);
			shell.pack(true);
			originalSize = shell.getSize();
			move();
		}
	}

	/**
	 * Focuses the {@link ViewBar}.
	 *
	 * @return {@code true} if the control got focus, {@code false} if it was
	 *         unable to.
	 */
	public boolean setFocus() {
		if (shell != null && !shell.isDisposed()) {
			boolean result = shell.setFocus();
			return result;
		}
		return false;
	}

	/**
	 * Invoked just before the bar is shown. The default implementation does
	 * nothing.
	 *
	 * @param bounds
	 *            of the bar, in display coordinates
	 */
	protected void aboutToShow(Rectangle bounds) {
		// Nothing
	}

	/**
	 * Converts view-relative coordinates to display coordinates and may also
	 * change the extent of the bounds.
	 *
	 * @param bounds
	 *            of the bar to be displayed, with the location relative to the
	 *            {@link #getParent()} control
	 * @return a new {@link Rectangle} in display coordinates, possibly with
	 *         changed extent.
	 */
	protected Rectangle clip(Rectangle bounds) {
		int x = bounds.x;
		int y = bounds.y;
		// TODO: following resizes lags.
		// TODO: proper way to get main control of containing view.
		// The bar shall appear at the top of the view, irrespective of
		// whether there is a toolbar and whether it is next to the tabs
		// or below the tabs.
		Control anchor = getParent();
		while (!(anchor.getParent() instanceof CTabFolder)) {
			anchor = anchor.getParent();
		}
		final CTabFolder folder = (CTabFolder) anchor.getParent();
		final int tabHeight = folder.getTabHeight();
		IContributionManager manager;
		if (workbenchPart instanceof IViewPart) {
			manager = ((IViewPart) workbenchPart).getViewSite().getActionBars()
					.getToolBarManager();
		} else {
			manager = ((IEditorPart) workbenchPart).getEditorSite()
					.getActionBars()
					.getToolBarManager();
		}
		while (manager instanceof SubContributionManager) {
			manager = ((SubContributionManager) manager).getParent();
		}
		Control tbc;
		if (manager instanceof ToolBarManager) {
			tbc = ((ToolBarManager) manager).getControl();
		} else if (manager instanceof CoolBarManager) {
			tbc = ((CoolBarManager) manager).getControl();
		} else {
			tbc = null;
		}
		Point topLeft;
		boolean inToolbar = false;
		if (tbc != null) {
			if (tbc.toDisplay(0, 0).y < folder.toDisplay(0, 0).y + tabHeight) {
				// Toolbar is next to the tabs
				topLeft = anchor.toDisplay(x, y);
			} else {
				// Toolbar is below tabs: use top of toolbar as top of
				// bar.
				Point p = anchor.toDisplay(x, y);
				Point q = tbc.toDisplay(x, y);
				topLeft = new Point(p.x, q.y);
				inToolbar = true;
			}
		} else {
			topLeft = anchor.toDisplay(x, y);
		}
		Rectangle result = new Rectangle(topLeft.x, topLeft.y, bounds.width,
				bounds.height);
		// Limit width to that of the anchor.
		Rectangle clipTo = anchor.getBounds();
		if (anchor.getParent() != null) {
			Point displayLocation = anchor.getParent()
					.toDisplay(new Point(clipTo.x, clipTo.y));
			clipTo = new Rectangle(displayLocation.x, displayLocation.y,
					clipTo.width, clipTo.height);
		}
		if (inToolbar) {
			clipTo.y = topLeft.y;
		}
		return clipTo.intersection(result);
	}

	/**
	 * Retrieve the {@link Control} the {@link ViewBar} is bound to.
	 *
	 * @return the control
	 */
	protected Control getParent() {
		return control;
	}

	/**
	 * Determines whether the {@link ViewBar} is visible.
	 *
	 * @return {@code true} if it is visible, {@code false} otherwise
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * Shows or hides the {@link ViewBar}. Showing an already visible bar has no
	 * effect, nor does hiding an already hidden bar.
	 *
	 * @param visible
	 *            whether to show or hide the bar
	 */
	public void setVisible(boolean visible) {
		if (visible != isVisible()) {
			if (visible) {
				show();
			} else {
				hide();
			}
		}
	}

	/**
	 * Creates the content area of the the {@link ViewBar}.
	 *
	 * @param parent
	 *            the parent of the content area
	 * @return the content area created
	 */
	protected abstract Composite createContent(Composite parent);

	/**
	 * Hide the {@link ViewBar}.
	 */
	protected void hide() {
		visible = false;
		if (shell != null && !shell.isDisposed()) {
			removeListeners();
			shell.close();
			shell.dispose();
		}
		shell = null;
		parentShell = null;
	}

	private Rectangle move() {
		Rectangle bounds = new Rectangle(0, 0, originalSize.x, originalSize.y);
		bounds = control.getDisplay().getBounds().intersection(clip(bounds));
		shell.setBounds(bounds);
		return bounds;
	}

}
