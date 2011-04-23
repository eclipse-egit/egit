/*******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.egit.ui.internal.commit;

import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.commit.DiffStyleRangeFormatter.DiffStyleRange;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ColorDescriptor;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.DeviceResourceManager;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.themes.IThemeManager;

/**
 * Viewer to display one or more file differences using standard editor colors
 * and fonts preferences.
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

	private IPropertyChangeListener themeListener = new IPropertyChangeListener() {

		public void propertyChange(PropertyChangeEvent event) {
			String property = event.getProperty();
			if (IThemeManager.CHANGE_CURRENT_THEME.equals(property)
					|| UIPreferences.THEME_DiffAddBackgroundColor
							.equals(property)
					|| UIPreferences.THEME_DiffAddForegroundColor
							.equals(property)
					|| UIPreferences.THEME_DiffHunkBackgroundColor
							.equals(property)
					|| UIPreferences.THEME_DiffHunkForegroundColor
							.equals(property)
					|| UIPreferences.THEME_DiffRemoveBackgroundColor
							.equals(property)
					|| UIPreferences.THEME_DiffRemoveForegroundColor
							.equals(property)) {
				refreshDiffColors();
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
		styleViewer();
	}

	private void refreshDiffColors() {
		ColorRegistry colorRegistry = PlatformUI.getWorkbench()
				.getThemeManager().getCurrentTheme().getColorRegistry();
		this.addBackgroundColor = colorRegistry
				.get(UIPreferences.THEME_DiffAddBackgroundColor);
		this.addForegroundColor = colorRegistry
				.get(UIPreferences.THEME_DiffAddForegroundColor);
		this.removeBackgroundColor = colorRegistry
				.get(UIPreferences.THEME_DiffRemoveBackgroundColor);
		this.removeForegroundColor = colorRegistry
				.get(UIPreferences.THEME_DiffRemoveForegroundColor);
		this.hunkBackgroundColor = colorRegistry
				.get(UIPreferences.THEME_DiffHunkBackgroundColor);
		this.hunkForegroundColor = colorRegistry
				.get(UIPreferences.THEME_DiffHunkForegroundColor);
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

	/**
	 * Refresh style ranges
	 *
	 * @return this viewer
	 */
	public DiffViewer refreshStyleRanges() {
		StyledText text = getTextWidget();
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
			default:
				break;
			}
		text.setStyleRanges(ranges);
		return this;
	}

	/**
	 * Set diff style range formatter
	 *
	 * @param formatter
	 * @return this viewer
	 */
	public DiffViewer setFormatter(DiffStyleRangeFormatter formatter) {
		this.formatter = formatter;
		refreshStyleRanges();
		return this;
	}

}
