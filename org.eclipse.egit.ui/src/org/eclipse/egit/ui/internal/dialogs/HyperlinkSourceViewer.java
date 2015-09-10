/*******************************************************************************
 * Copyright (C) 2015 Thomas Wolf <thomas.wolf@paranor.ch>.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.dialogs;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.HyperlinkDetectorDescriptor;

/**
 * A {@link SourceViewer} that automatically reacts to changes in the
 * hyperlinking preferences.
 */
public class HyperlinkSourceViewer extends SourceViewer {
	// The default SourceViewer doesn't do this and instead AbstractTextEditor
	// has code that does all that. For our uses,it is much more convenient if
	// the viewer itself handles this.

	private Configuration configuration;

	private Set<String> preferenceKeysForEnablement;

	private Set<String> preferenceKeysForActivation;

	private IPropertyChangeListener hyperlinkChangeListener;

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
		super(parent, ruler, styles);
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
	}

	@Override
	public void configure(SourceViewerConfiguration config) {
		super.configure(config);
		if (config instanceof Configuration) {
			configuration = (Configuration) config;
			configurePreferenceKeys();
			// Install a listener
			hyperlinkChangeListener = new IPropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					String property = event.getProperty();
					if (preferenceKeysForEnablement.contains(property)) {
						resetHyperlinkDetectors();
						final Control control = getControl();
						if (control != null && !control.isDisposed()) {
							control.getDisplay().asyncExec(new Runnable() {
								@Override
								public void run() {
									if (!control.isDisposed()) {
										refresh();
									}
								}
							});
						}
					} else if (preferenceKeysForActivation.contains(property)) {
						resetHyperlinkDetectors();
					}
				}
			};
			EditorsUI.getPreferenceStore()
					.addPropertyChangeListener(hyperlinkChangeListener);
		} else {
			configuration = null;
			hyperlinkChangeListener = null;
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
		Map targets = configuration.getHyperlinkDetectorTargets(this);
		for (HyperlinkDetectorDescriptor desc : EditorsUI
				.getHyperlinkDetectorRegistry()
				.getHyperlinkDetectorDescriptors()) {
			if (targets.keySet().contains(desc.getTargetId())) {
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

	@Override
	public void unconfigure() {
		super.unconfigure();
		if (hyperlinkChangeListener != null) {
			EditorsUI.getPreferenceStore()
					.removePropertyChangeListener(hyperlinkChangeListener);
		}
		preferenceKeysForEnablement = null;
		preferenceKeysForActivation = null;
	}

	/**
	 * Identical to {@link TextSourceViewerConfiguration}.
	 * <p>
	 * Subclassed to make {@link #getHyperlinkDetectorTargets(ISourceViewer)}
	 * visible to {@link HyperlinkSourceViewer}.
	 * </p>
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

		@Override
		protected Map getHyperlinkDetectorTargets(ISourceViewer sourceViewer) {
			// Just so that we have visibility on this in the enclosing class.
			return super.getHyperlinkDetectorTargets(sourceViewer);
		}
	}
}
