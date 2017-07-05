/*******************************************************************************
 * Copyright (C) 2015, 2016 Thomas Wolf <thomas.wolf@paranor.ch>.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetectorExtension;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetectorExtension2;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.IVerticalRulerExtension;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.HyperlinkDetectorDescriptor;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.eclipse.ui.texteditor.spelling.SpellingService;

/**
 * A {@link ProjectionViewer} that automatically reacts to changes in the
 * hyperlinking, spellchecking, and color preferences.
 */
public class HyperlinkSourceViewer extends ProjectionViewer {
	// The default SourceViewer doesn't do this and instead AbstractTextEditor
	// has code that does all that. For our uses it is much more convenient if
	// the viewer itself handles this.
	//
	// Note: although ProjectionViewer is marked as noextend, there are already
	// a number of subclasses.

	private final Map<String, Color> customColors = new HashMap<>();

	private Configuration configuration;

	private Set<String> preferenceKeysForEnablement;

	private Set<String> preferenceKeysForActivation;

	private IPropertyChangeListener hyperlinkChangeListener;

	private IPropertyChangeListener editorPropertyChangeListener;

	private IPropertyChangeListener jFacePropertyChangeListener;

	/**
	 * Constructs a new source viewer. The vertical ruler is initially visible.
	 * The viewer has not yet been initialized with a source viewer
	 * configuration.
	 *
	 * @param parent
	 *            the parent of the viewer's control
	 * @param ruler
	 *            the vertical ruler used by this source viewer
	 * @param styles
	 *            the SWT style bits for the viewer's control,
	 */
	public HyperlinkSourceViewer(Composite parent, IVerticalRuler ruler,
			int styles) {
		this(parent, ruler, null, false, styles);
	}

	/**
	 * Constructs a new source viewer. The vertical ruler is initially visible.
	 * The overview ruler visibility is controlled by the value of
	 * <code>showAnnotationsOverview</code>. The viewer has not yet been
	 * initialized with a source viewer configuration.
	 *
	 * @param parent
	 *            the parent of the viewer's control
	 * @param verticalRuler
	 *            the vertical ruler used by this source viewer
	 * @param overviewRuler
	 *            the overview ruler
	 * @param showAnnotationsOverview
	 *            {@code true} if the overview ruler should be visible,
	 *            {@code false} otherwise
	 * @param styles
	 *            the SWT style bits for the viewer's control,
	 */
	public HyperlinkSourceViewer(Composite parent, IVerticalRuler verticalRuler,
			IOverviewRuler overviewRuler, boolean showAnnotationsOverview,
			int styles) {
		super(parent, verticalRuler, overviewRuler, showAnnotationsOverview,
				styles);
		setColors();
		editorPropertyChangeListener = event -> handleEditorPreferencesChange(
				event);
		EditorsUI.getPreferenceStore()
				.addPropertyChangeListener(editorPropertyChangeListener);
		jFacePropertyChangeListener = event -> handleJFacePreferencesChange(
				event);
		JFacePreferences.getPreferenceStore()
				.addPropertyChangeListener(jFacePropertyChangeListener);
	}

	@Override
	public void configure(SourceViewerConfiguration config) {
		super.configure(config);
		if (config instanceof Configuration) {
			configuration = (Configuration) config;
			configurePreferenceKeys();
			// Install a listener
			hyperlinkChangeListener = event -> {
				String property = event.getProperty();
				if (preferenceKeysForEnablement.contains(property)) {
					resetHyperlinkDetectors();
					async(() -> refresh());
				} else if (preferenceKeysForActivation.contains(property)) {
					resetHyperlinkDetectors();
				}
			};
			EditorsUI.getPreferenceStore()
					.addPropertyChangeListener(hyperlinkChangeListener);
		} else {
			configuration = null;
			hyperlinkChangeListener = null;
		}
	}

	@Override
	protected void handleDispose() {
		if (hyperlinkChangeListener != null) {
			EditorsUI.getPreferenceStore()
					.removePropertyChangeListener(hyperlinkChangeListener);
			hyperlinkChangeListener = null;
		}
		if (editorPropertyChangeListener != null) {
			EditorsUI.getPreferenceStore()
					.removePropertyChangeListener(editorPropertyChangeListener);
			editorPropertyChangeListener = null;
		}
		if (jFacePropertyChangeListener != null) {
			JFacePreferences.getPreferenceStore()
					.removePropertyChangeListener(jFacePropertyChangeListener);
			jFacePropertyChangeListener = null;
		}
		for (Color color : customColors.values()) {
			if (color != null) {
				color.dispose();
			}
		}
		customColors.clear();
		super.handleDispose();
	}

	@Override
	public void refresh() {
		// Don't lose the annotation model, if there is one!
		// (The super implementation ignores it.)
		setDocument(getDocument(), getAnnotationModel());
	}

	/**
	 * Executes the given {@link Runnable} via
	 * {@link Display#asyncExec(Runnable)} if the viewer still exists.
	 *
	 * @param runnable
	 *            to perform some operation in the UI thread.
	 */
	protected void async(Runnable runnable) {
		Control control = getControl();
		if (control != null && !control.isDisposed()) {
			control.getDisplay().asyncExec(() -> {
				if (!control.isDisposed()) {
					runnable.run();
				}
			});
		}
	}

	/**
	 * Handle a change in EditorsUI preferences. The default implementation
	 * handles spell-checking enablement changes, and foreground/background
	 * color changes. May be overridden, but the subclass should invoke super.
	 *
	 * @param event
	 *            describing the property change.
	 */
	protected void handleEditorPreferencesChange(PropertyChangeEvent event) {
		switch (event.getProperty()) {
		case SpellingService.PREFERENCE_SPELLING_ENABLED:
			boolean isEnabled = EditorsUI.getPreferenceStore()
					.getBoolean(SpellingService.PREFERENCE_SPELLING_ENABLED);
			updateSpellChecking(isEnabled);
			break;
		case AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND:
		case AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT:
		case AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND:
		case AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT:
		case AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND:
		case AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND_SYSTEM_DEFAULT:
		case AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND:
		case AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND_SYSTEM_DEFAULT:
			async(() -> setColors());
			break;
		default:
			break;
		}
	}

	/**
	 * Handle a change in EditorsUI preferences. The default implementation
	 * handles hyperlink coloring changes. May be overridden, but the subclass
	 * should invoke super.
	 *
	 * @param event
	 *            describing the property change.
	 */
	protected void handleJFacePreferencesChange(PropertyChangeEvent event) {
		if (JFacePreferences.HYPERLINK_COLOR.equals(event.getProperty())) {
			async(() -> invalidateTextPresentation());
		}
	}

	/**
	 * Update the font of this viewer, trying to maintain the selection and the
	 * top index. Note that when this viewer is used in an
	 * {@link AbstractTextEditor}, the editor takes care of this and this method
	 * should not be called.
	 *
	 * @param font
	 *            to set
	 */
	protected void setFont(Font font) {
		// See AbstractTextEditor.setFont()
		StyledText styledText = getTextWidget();
		IVerticalRuler verticalRuler = getVerticalRuler();
		if (getDocument() != null) {
			ISelectionProvider provider = getSelectionProvider();
			ISelection selection = provider.getSelection();
			int topIndex = getTopIndex();

			Control parent = getControl();
			parent.setRedraw(false);
			styledText.setFont(font);
			if (verticalRuler instanceof IVerticalRulerExtension) {
				IVerticalRulerExtension e = (IVerticalRulerExtension) verticalRuler;
				e.setFont(font);
			}
			provider.setSelection(selection);
			setTopIndex(topIndex);
			if (parent instanceof Composite) {
				Composite composite = (Composite) parent;
				composite.layout(true);
			}
			parent.setRedraw(true);
		} else {
			styledText.setFont(font);
			if (verticalRuler instanceof IVerticalRulerExtension) {
				IVerticalRulerExtension e = (IVerticalRulerExtension) verticalRuler;
				e.setFont(font);
			}
		}
	}

	private void updateSpellChecking(boolean isEnabled) {
		// See TextEditor.handlePreferenceStoreChanged.
		this.unconfigure();
		this.configure(configuration);
		if (!isEnabled) {
			SpellingProblem.removeAll(this, null);
		}
	}

	private void configurePreferenceKeys() {
		preferenceKeysForEnablement = new HashSet<>();
		preferenceKeysForActivation = new HashSet<>();
		// Global settings (master switch)
		preferenceKeysForEnablement
				.add(AbstractTextEditor.PREFERENCE_HYPERLINKS_ENABLED);
		preferenceKeysForActivation
				.add(AbstractTextEditor.PREFERENCE_HYPERLINK_KEY_MODIFIER);
		// All applicable individual hyperlink detectors settings.
		Set<?> targets = configuration.getHyperlinkDetectorTargets(this)
				.keySet();
		for (HyperlinkDetectorDescriptor desc : EditorsUI
				.getHyperlinkDetectorRegistry()
				.getHyperlinkDetectorDescriptors()) {
			if (targets.contains(desc.getTargetId())) {
				preferenceKeysForEnablement.add(desc.getId());
				preferenceKeysForActivation.add(desc.getId()
						+ HyperlinkDetectorDescriptor.STATE_MASK_POSTFIX);
			}
		}
	}

	private void resetHyperlinkDetectors() {
		IHyperlinkDetector[] detectors = configuration
				.getHyperlinkDetectors(this);
		int stateMask = configuration.getHyperlinkStateMask(this);
		setHyperlinkDetectors(detectors, stateMask);
	}

	private void setColors() {
		IPreferenceStore store = EditorsUI.getPreferenceStore();
		StyledText styledText = getTextWidget();
		setColor(styledText, store,
				AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND,
				store.getBoolean(
						AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT));
		setColor(styledText, store,
				AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND,
				store.getBoolean(
						AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT));
		setColor(styledText, store,
				AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND,
				store.getBoolean(
						AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND_SYSTEM_DEFAULT));
		setColor(styledText, store,
				AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND,
				store.getBoolean(
						AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND_SYSTEM_DEFAULT));
	}

	private void setColor(StyledText styledText, IPreferenceStore store,
			String key, boolean useDefault) {
		Color newColor = useDefault ? null
				: createColor(styledText.getDisplay(), store, key);
		switch (key) {
		case AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND:
			styledText.setForeground(newColor);
			break;
		case AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND:
			styledText.setBackground(newColor);
			break;
		case AbstractTextEditor.PREFERENCE_COLOR_SELECTION_FOREGROUND:
			styledText.setSelectionForeground(newColor);
			break;
		case AbstractTextEditor.PREFERENCE_COLOR_SELECTION_BACKGROUND:
			styledText.setSelectionBackground(newColor);
			break;
		default:
			return;
		}
		Color oldColor = customColors.remove(key);
		if (oldColor != null) {
			oldColor.dispose();
		}
		customColors.put(key, newColor);
	}

	private Color createColor(Display display, IPreferenceStore store,
			String key) {
		RGB rgb = null;
		if (store.contains(key)) {
			if (store.isDefault(key)) {
				rgb = PreferenceConverter.getDefaultColor(store, key);
			} else {
				rgb = PreferenceConverter.getColor(store, key);
			}
			if (rgb != null) {
				return new Color(display, rgb);
			}
		}
		return null;
	}

	@Override
	public void unconfigure() {
		super.unconfigure();
		if (hyperlinkChangeListener != null) {
			EditorsUI.getPreferenceStore()
					.removePropertyChangeListener(hyperlinkChangeListener);
			hyperlinkChangeListener = null;
		}
		preferenceKeysForEnablement = null;
		preferenceKeysForActivation = null;
	}

	/**
	 * A {@link TextSourceViewerConfiguration} for use in conjunction with
	 * {@link HyperlinkSourceViewer}. Includes support for opening hyperlinks on
	 * the configured modifier key, or also when no key is pressed (i.e., on
	 * {@link SWT#NONE}) if
	 * {@link TextSourceViewerConfiguration#getHyperlinkStateMask(ISourceViewer)
	 * getHyperlinkStateMask(ISourceViewer)} returns {@link SWT#NONE}
	 */
	public static class Configuration extends TextSourceViewerConfiguration {

		/**
		 * Creates a new configuration.
		 */
		public Configuration() {
			super();
		}

		/**
		 * Creates a new configuration and initializes it with the given
		 * preference store.
		 *
		 * @param preferenceStore
		 *            the preference store used to initialize this configuration
		 */
		public Configuration(IPreferenceStore preferenceStore) {
			super(preferenceStore);
		}

		/**
		 * Returns the hyperlink detectors which are to be used to detect
		 * hyperlinks in the given source viewer. This implementation uses
		 * {@link #internalGetHyperlinkDetectors(ISourceViewer)} to get the
		 * hyperlink detectors.
		 * <p>
		 * Sets up the hyperlink detectors such that they are active on both
		 * {@link SWT#NONE} and on the configured modifier key combination if
		 * the viewer is configured to open hyperlinks on direct click, i.e., if
		 * {@link TextSourceViewerConfiguration#getHyperlinkStateMask(ISourceViewer)
		 * getHyperlinkStateMask(ISourceViewer)} returns {@link SWT#NONE}.
		 * </p>
		 *
		 * @param sourceViewer
		 *            the {@link ISourceViewer} to be configured by this
		 *            configuration
		 * @return an array with {@link IHyperlinkDetector}s or {@code null} if
		 *         no hyperlink support should be installed
		 */
		@Override
		public final IHyperlinkDetector[] getHyperlinkDetectors(
				ISourceViewer sourceViewer) {
			IHyperlinkDetector[] detectors = internalGetHyperlinkDetectors(
					sourceViewer);
			if (detectors != null && detectors.length > 0
					&& getHyperlinkStateMask(sourceViewer) == SWT.NONE) {
				// Duplicate them all with a detector that is active on SWT.NONE
				int defaultMask = getConfiguredDefaultMask();
				IHyperlinkDetector[] newDetectors = new IHyperlinkDetector[detectors.length
						* 2];
				int j = 0;
				for (IHyperlinkDetector original : detectors) {
					if (original instanceof IHyperlinkDetectorExtension2) {
						int mask = ((IHyperlinkDetectorExtension2) original)
								.getStateMask();
						if (mask == -1) {
							newDetectors[j++] = new FixedMaskHyperlinkDetector(
									original, defaultMask);
							if (defaultMask != SWT.NONE) {
								newDetectors[j++] = new NoMaskHyperlinkDetector(
										original);
							}
						} else {
							newDetectors[j++] = original;
							if (mask != SWT.NONE) {
								newDetectors[j++] = new NoMaskHyperlinkDetector(
										original);
							}
						}
					} else {
						newDetectors[j++] = original;
					}
				}
				IHyperlinkDetector[] result = new IHyperlinkDetector[j];
				System.arraycopy(newDetectors, 0, result, 0, j);
				return result;
			}
			return detectors;
		}

		/**
		 * Collects the {@link IHyperlinkDetector}s for the given
		 * {@link ISourceViewer}.
		 *
		 * @param sourceViewer
		 *            to get the detectors for
		 * @return the hyperlink detectors, or {@code null} if none shall be
		 *         installed
		 * @see TextSourceViewerConfiguration#getHyperlinkDetectors(ISourceViewer)
		 */
		protected @Nullable IHyperlinkDetector[] internalGetHyperlinkDetectors(
				ISourceViewer sourceViewer) {
			return super.getHyperlinkDetectors(sourceViewer);
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Map getHyperlinkDetectorTargets(ISourceViewer sourceViewer) {
			// TODO: use generified signature once EGit's base dependency is
			// Eclipse 4.5.
			// Just so that we have visibility on this in the enclosing class.
			return super.getHyperlinkDetectorTargets(sourceViewer);
		}

	}

	private static abstract class InternalHyperlinkDetector
			implements IHyperlinkDetector, IHyperlinkDetectorExtension,
			IHyperlinkDetectorExtension2 {

		protected IHyperlinkDetector delegate;

		protected InternalHyperlinkDetector(IHyperlinkDetector delegate) {
			this.delegate = delegate;
		}

		@Override
		public final IHyperlink[] detectHyperlinks(ITextViewer textViewer,
				IRegion region, boolean canShowMultipleHyperlinks) {
			return delegate.detectHyperlinks(textViewer, region,
					canShowMultipleHyperlinks);
		}

		@Override
		public final void dispose() {
			if (delegate instanceof IHyperlinkDetectorExtension) {
				((IHyperlinkDetectorExtension) delegate).dispose();
			}
		}
	}

	private static class FixedMaskHyperlinkDetector
			extends InternalHyperlinkDetector {

		private final int mask;

		protected FixedMaskHyperlinkDetector(IHyperlinkDetector delegate,
				int mask) {
			super(delegate);
			this.mask = mask;
		}

		@Override
		public int getStateMask() {
			return mask;
		}
	}

	/**
	 * An {@link IHyperlinkDetector} that activates when no modifier key is
	 * pressed. It's protected so that the {@link HyperlinkTokenScanner} can see
	 * it and can avoid calling those for syntax coloring since they are
	 * duplicates of some other hyperlink detector anyway.
	 */
	protected static class NoMaskHyperlinkDetector
			extends FixedMaskHyperlinkDetector {

		private NoMaskHyperlinkDetector(IHyperlinkDetector delegate) {
			// Private to allow instantiation only within this compilation unit
			super(delegate, SWT.NONE);
		}

	}

	private static int getConfiguredDefaultMask() {
		int mask = computeStateMask(EditorsUI.getPreferenceStore().getString(
				AbstractTextEditor.PREFERENCE_HYPERLINK_KEY_MODIFIER));
		if (mask == -1) {
			// Fallback
			mask = EditorsUI.getPreferenceStore().getInt(
					AbstractTextEditor.PREFERENCE_HYPERLINK_KEY_MODIFIER_MASK);
		}
		return mask;
	}

	// The preference for
	// AbstractTextEditor.PREFERENCE_HYPERLINK_KEY_MODIFIER_MASK is not
	// recomputed when the user changes preferences. Therefore, we have to
	// use the AbstractTextEditor.PREFERENCE_HYPERLINK_KEY_MODIFIER and
	// translate that to a state mask explicitly. Code below copied from
	// org.eclipse.ui.internal.editors.text.HyperlinkDetectorsConfigurationBlock.

	/**
	 * Maps the localized modifier name to a code in the same manner as
	 * {@link org.eclipse.jface.action.Action#findModifier
	 * Action.findModifier()}.
	 *
	 * @param modifierName
	 *            the modifier name
	 * @return the SWT modifier bit, or {@code 0} if no match was found
	 */
	private static final int findLocalizedModifier(String modifierName) {
		if (modifierName == null) {
			return 0;
		}

		if (modifierName
				.equalsIgnoreCase(Action.findModifierString(SWT.CTRL))) {
			return SWT.CTRL;
		}
		if (modifierName
				.equalsIgnoreCase(Action.findModifierString(SWT.SHIFT))) {
			return SWT.SHIFT;
		}
		if (modifierName.equalsIgnoreCase(Action.findModifierString(SWT.ALT))) {
			return SWT.ALT;
		}
		if (modifierName
				.equalsIgnoreCase(Action.findModifierString(SWT.COMMAND))) {
			return SWT.COMMAND;
		}

		return 0;
	}

	/**
	 * Computes the state mask for the given modifier string.
	 *
	 * @param modifiers
	 *            the string with the modifiers, separated by '+', '-', ';', ','
	 *            or '.'
	 * @return the state mask or {@code -1} if the input is invalid
	 */
	private static final int computeStateMask(String modifiers) {
		if (modifiers == null) {
			return -1;
		}

		if (modifiers.length() == 0) {
			return SWT.NONE;
		}

		int stateMask = 0;
		StringTokenizer modifierTokenizer = new StringTokenizer(modifiers,
				",;.:+-* "); //$NON-NLS-1$
		while (modifierTokenizer.hasMoreTokens()) {
			int modifier = findLocalizedModifier(modifierTokenizer.nextToken());
			if (modifier == 0 || (stateMask & modifier) == modifier) {
				return -1;
			}
			stateMask = stateMask | modifier;
		}
		return stateMask;
	}

}
