/*******************************************************************************
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.SubActionBars;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.MessagePage;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * A {@link ContentOutlinePage} for {@link MultiPageEditorPart}s. The
 * {@link org.eclipse.ui.views.contentoutline.ContentOutline ContentOutline}
 * view only automatically handles outline pages of top-level editors, but not
 * of nested editors. (It reacts only to part events, not to page change events,
 * and moreover the MultiPageEditorPart framework does not send events when
 * pages are added or removed.)
 * <p>
 * This class manages content outline pages for nested editors in its own
 * {@link PageBook}. Nested editors can provide their outline pages as usual by
 * adapting to {@link IContentOutlinePage}.
 * </p>
 */
public class MultiPageEditorContentOutlinePage extends ContentOutlinePage {

	private final MultiPageEditorPart editorPart;

	private final ISelectionChangedListener globalSelectionListener = //
			event -> fireSelectionChangedEvent(event);

	private final CopyOnWriteArrayList<ISelectionChangedListener> selectionListeners = new CopyOnWriteArrayList<>();

	private PageBook book;

	private MessagePage emptyPage;

	private IPage currentPage;

	private IPageChangedListener pageListener;

	private final Map<IEditorPart, IPage> pages = new HashMap<>();

	private final Map<IPage, SubActionBars> bars = new HashMap<>();

	/**
	 * Creates a new {@link MultiPageEditorContentOutlinePage} for the given
	 * top-level editor part. It will track page changes and create and manage
	 * outline pages for any nested {@link IEditorPart}s of that top-level
	 * editor part.
	 *
	 * @param editorPart
	 *            the outline page belongs to
	 */
	public MultiPageEditorContentOutlinePage(MultiPageEditorPart editorPart) {
		super();
		this.editorPart = editorPart;
	}

	@Override
	public void createControl(Composite parent) {
		book = new PageBook(parent, SWT.NONE);
		emptyPage = new MessagePage();
		emptyPage.createControl(book);
		emptyPage
				.setMessage(UIText.MultiPageEditorContentOutlinePage_NoOutline);
		Object activePage = editorPart.getSelectedPage();
		if (activePage instanceof IEditorPart) {
			showPage(createOutlinePage((IEditorPart) activePage));
		} else {
			currentPage = emptyPage;
			book.showPage(emptyPage.getControl());
		}
		pageListener = (event) -> {
			Object newPage = event.getSelectedPage();
			if (!(newPage instanceof IEditorPart)) {
				showPage(emptyPage);
				return;
			}
			IPage newOutlinePage = pages.get(newPage);
			if (newOutlinePage == null) {
				newOutlinePage = createOutlinePage((IEditorPart) newPage);
			}
			showPage(newOutlinePage);
		};
		editorPart.addPageChangedListener(pageListener);
	}

	@Override
	public void dispose() {
		if (pageListener != null) {
			editorPart.removePageChangedListener(pageListener);
			pageListener = null;
		}
		pages.clear();
		selectionListeners.clear();
		for (SubActionBars bar : bars.values()) {
			bar.dispose();
		}
		bars.clear();
		if (currentPage instanceof ISelectionProvider) {
			((ISelectionProvider) currentPage)
					.removeSelectionChangedListener(globalSelectionListener);
		}
		currentPage = null;
		if (book != null) {
			book.dispose();
			book = null;
		}
		if (emptyPage != null) {
			emptyPage.dispose();
			emptyPage = null;
		}
	}

	@Override
	public Control getControl() {
		return book;
	}

	@Override
	public void setFocus() {
		// See org.eclipse.ui.part.PageBookView
		book.setFocus();
		currentPage.setFocus();
	}

	@Override
	public void addSelectionChangedListener(
			ISelectionChangedListener listener) {
		selectionListeners.addIfAbsent(listener);
	}

	@Override
	public ISelection getSelection() {
		if (currentPage instanceof ISelectionProvider) {
			return ((ISelectionProvider) currentPage).getSelection();
		}
		return StructuredSelection.EMPTY;
	}

	@Override
	public void removeSelectionChangedListener(
			ISelectionChangedListener listener) {
		selectionListeners.remove(listener);
	}

	@Override
	public void setSelection(ISelection selection) {
		if (currentPage instanceof ISelectionProvider) {
			((ISelectionProvider) currentPage).setSelection(selection);
		}
	}

	private void showPage(IPage page) {
		if (page == null) {
			page = emptyPage;
		}
		if (currentPage == page) {
			return;
		}
		if (currentPage instanceof ISelectionProvider) {
			((ISelectionProvider) currentPage)
					.removeSelectionChangedListener(globalSelectionListener);
		}
		SubActionBars localBars = bars.get(currentPage);
		if (localBars != null) {
			localBars.deactivate();
		}
		currentPage = page;
		if (currentPage instanceof ISelectionProvider) {
			((ISelectionProvider) currentPage)
					.addSelectionChangedListener(globalSelectionListener);
		}
		localBars = bars.get(currentPage);
		Control control = page.getControl();
		if (control == null || control.isDisposed()) {
			page.createControl(book);
			page.setActionBars(localBars);
			control = page.getControl();
		}
		if (localBars != null) {
			localBars.activate();
		}
		getSite().getActionBars().updateActionBars();
		book.showPage(control);
		if (currentPage instanceof ISelectionProvider) {
			ISelection selection = ((ISelectionProvider) currentPage)
					.getSelection();
			fireSelectionChangedEvent(new SelectionChangedEvent(
					(ISelectionProvider) currentPage, selection));
		} else {
			fireSelectionChangedEvent(
					new SelectionChangedEvent(this, StructuredSelection.EMPTY));
		}
	}

	private IPage createOutlinePage(IEditorPart editor) {
		IContentOutlinePage outlinePage = AdapterUtils.adapt(editor,
				IContentOutlinePage.class);
		if (outlinePage == null) {
			pages.put(editor, emptyPage);
			return emptyPage;
		}
		pages.put(editor, outlinePage);
		if (outlinePage instanceof NestedContentOutlinePage) {
			((Page) outlinePage).init(getSite());
		}
		SubActionBars pageBars = new SubActionBars(getSite().getActionBars());
		bars.put(outlinePage, pageBars);
		return outlinePage;
	}

	private void fireSelectionChangedEvent(SelectionChangedEvent event) {
		for (ISelectionChangedListener listener : selectionListeners) {
			SafeRunnable.run(new SafeRunnable() {

				@Override
				public void run() {
					listener.selectionChanged(event);
				}
			});
		}
	}
}
