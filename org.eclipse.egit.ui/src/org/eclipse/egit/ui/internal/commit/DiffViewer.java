/*******************************************************************************
 *  Copyright (c) 2011, 2013 GitHub Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    Tobias Pfeifer (SAP AG) - customizable font and color for the first header line - https://bugs.eclipse.org/397723
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import static org.eclipse.egit.ui.internal.UIPreferences.THEME_DiffAddBackgroundColor;
import static org.eclipse.egit.ui.internal.UIPreferences.THEME_DiffAddForegroundColor;
import static org.eclipse.egit.ui.internal.UIPreferences.THEME_DiffHeadlineBackgroundColor;
import static org.eclipse.egit.ui.internal.UIPreferences.THEME_DiffHeadlineFont;
import static org.eclipse.egit.ui.internal.UIPreferences.THEME_DiffHeadlineForegroundColor;
import static org.eclipse.egit.ui.internal.UIPreferences.THEME_DiffHunkBackgroundColor;
import static org.eclipse.egit.ui.internal.UIPreferences.THEME_DiffHunkForegroundColor;
import static org.eclipse.egit.ui.internal.UIPreferences.THEME_DiffRemoveBackgroundColor;
import static org.eclipse.egit.ui.internal.UIPreferences.THEME_DiffRemoveForegroundColor;

import org.eclipse.egit.ui.internal.commit.DiffStyleRangeFormatter.DiffStyleRange;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ColorDescriptor;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.DeviceResourceManager;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.LineBackgroundEvent;
import org.eclipse.swt.custom.LineBackgroundListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.themes.IThemeManager;

/**
 * Source viewer to display one or more file differences using standard editor
 * colors and fonts preferences.
 */
public class DiffViewer extends SourceViewer {

	private DiffStyleRangeFormatter formatter;

	private DeviceResourceManager colors = new DeviceResourceManager(PlatformUI
			.getWorkbench().getDisplay());

	private LineNumberRulerColumn lineNumberRuler;

	private Color hunkBackgroundColor;

	private Color hunkForegroundColor;

	private Color addBackgroundColor;

	private Color addForegroundColor;

	private Color removeBackgroundColor;

	private Color removeForegroundColor;

	private Color headlineBackgroundColor;

	private Color headlineForegroundColor;

	private Font headlineFont;

	private IPropertyChangeListener themeListener = new IPropertyChangeListener() {

		public void propertyChange(PropertyChangeEvent event) {
			String property = event.getProperty();
			if (IThemeManager.CHANGE_CURRENT_THEME.equals(property)
					|| THEME_DiffAddBackgroundColor.equals(property)
					|| THEME_DiffAddForegroundColor.equals(property)
					|| THEME_DiffHunkBackgroundColor.equals(property)
					|| THEME_DiffHunkForegroundColor.equals(property)
					|| THEME_DiffHeadlineBackgroundColor.equals(property)
					|| THEME_DiffHeadlineForegroundColor.equals(property)
					|| THEME_DiffHeadlineFont.equals(property)
					|| THEME_DiffRemoveBackgroundColor.equals(property)
					|| THEME_DiffRemoveForegroundColor.equals(property)) {
				refreshDiffColors();
				refreshDiffFonts();
				refreshStyleRanges();
			}
		}
	};

	private IPropertyChangeListener editorPrefListener = new IPropertyChangeListener() {

		public void propertyChange(PropertyChangeEvent event) {
			styleViewer();
		}
	};

	/**
	 * @param parent
	 * @param ruler
	 * @param styles
	 */
	public DiffViewer(Composite parent, IVerticalRuler ruler, int styles) {
		super(parent, ruler, styles);
		setDocument(new Document());
		SourceViewerDecorationSupport support = new SourceViewerDecorationSupport(
				this, null, null, EditorsUI.getSharedTextColors());
		support.setCursorLinePainterPreferenceKeys(
				AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE,
				AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE_COLOR);
		support.install(EditorsUI.getPreferenceStore());
		if (ruler instanceof CompositeRuler) {
			lineNumberRuler = new LineNumberRulerColumn();
			((CompositeRuler) ruler).addDecorator(0, lineNumberRuler);
		}
		initListeners();
		getControl().addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				EditorsUI.getPreferenceStore().removePropertyChangeListener(
						editorPrefListener);
				PlatformUI.getWorkbench().getThemeManager()
						.removePropertyChangeListener(themeListener);
				colors.dispose();
			}
		});
		refreshDiffColors();
		refreshDiffFonts();
		styleViewer();
	}

	private void refreshDiffFonts() {
		FontRegistry reg = PlatformUI.getWorkbench().getThemeManager()
				.getCurrentTheme().getFontRegistry();
		this.headlineFont = reg.get(THEME_DiffHeadlineFont);
	}

	private void refreshDiffColors() {
		ColorRegistry reg = PlatformUI.getWorkbench().getThemeManager()
				.getCurrentTheme().getColorRegistry();
		this.addBackgroundColor = reg.get(THEME_DiffAddBackgroundColor);
		this.addForegroundColor = reg.get(THEME_DiffAddForegroundColor);
		this.removeBackgroundColor = reg.get(THEME_DiffRemoveBackgroundColor);
		this.removeForegroundColor = reg.get(THEME_DiffRemoveForegroundColor);
		this.hunkBackgroundColor = reg.get(THEME_DiffHunkBackgroundColor);
		this.hunkForegroundColor = reg.get(THEME_DiffHunkForegroundColor);
		this.headlineBackgroundColor = reg.get(THEME_DiffHeadlineBackgroundColor);
		this.headlineForegroundColor = reg.get(THEME_DiffHeadlineForegroundColor);
	}

	private void initListeners() {
		PlatformUI.getWorkbench().getThemeManager()
				.addPropertyChangeListener(this.themeListener);
		EditorsUI.getPreferenceStore().addPropertyChangeListener(
				this.editorPrefListener);
		getTextWidget().addLineBackgroundListener(new LineBackgroundListener() {

			public void lineGetBackground(LineBackgroundEvent event) {
				StyledText text = getTextWidget();
				if (event.lineOffset < text.getCharCount()) {
					StyleRange style = text
							.getStyleRangeAtOffset(event.lineOffset);
					if (style instanceof DiffStyleRange)
						event.lineBackground = ((DiffStyleRange) style).lineBackground;
				}
			}
		});
	}

	private ColorDescriptor createEditorColorDescriptor(String key) {
		return ColorDescriptor.createFrom(PreferenceConverter.getColor(
				EditorsUI.getPreferenceStore(), key));
	}

	private Color getEditorColor(String key) {
		return (Color) colors.get(createEditorColorDescriptor(key));
	}

	private void styleViewer() {
		IPreferenceStore store = EditorsUI.getPreferenceStore();
		Color foreground = null;
		if (!store
				.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT))
			foreground = getEditorColor(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND);

		Color background = null;
		if (!store
				.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT))
			background = getEditorColor(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);

		Color selectionForeground = null;
		if (!store
				.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND_SYSTEM_DEFAULT))
			selectionForeground = getEditorColor(AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND);

		Color selectionBackground = null;
		if (!store
				.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND_SYSTEM_DEFAULT))
			selectionBackground = getEditorColor(AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND);

		StyledText text = getTextWidget();
		text.setForeground(foreground);
		text.setBackground(background);
		text.setSelectionForeground(selectionForeground);
		text.setSelectionBackground(selectionBackground);
		text.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
		if (lineNumberRuler != null) {
			lineNumberRuler.setFont(text.getFont());
			lineNumberRuler.setForeground(foreground);
			lineNumberRuler.setBackground(background);
		}
	}

	/** Refresh style ranges */
	public void refreshStyleRanges() {
		DiffStyleRange[] ranges = formatter != null ? formatter.getRanges()
				: new DiffStyleRange[0];
		for (DiffStyleRange range : ranges)
			switch (range.diffType) {
			case ADD:
				range.foreground = addForegroundColor;
				range.lineBackground = addBackgroundColor;
				break;
			case REMOVE:
				range.foreground = removeForegroundColor;
				range.lineBackground = removeBackgroundColor;
				break;
			case HUNK:
				range.foreground = hunkForegroundColor;
				range.lineBackground = hunkBackgroundColor;
				break;
			case HEADLINE:
				range.font = headlineFont;
				range.foreground = headlineForegroundColor;
				range.lineBackground = headlineBackgroundColor;
				break;
			default:
				break;
			}
		getTextWidget().setStyleRanges(ranges);
	}

	/** @param formatter */
	public void setFormatter(DiffStyleRangeFormatter formatter) {
		this.formatter = formatter;
		refreshStyleRanges();
	}

}
