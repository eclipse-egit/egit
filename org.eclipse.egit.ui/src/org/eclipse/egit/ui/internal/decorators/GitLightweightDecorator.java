/*******************************************************************************
 * Copyright (C) 2007, 2013 IBM Corporation and others.
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Robin Rosenberg <robin.rosenberg@dewire.com>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2008, Google Inc.
 * Copyright (C) 2008, Tor Arne Vestbø <torarnv@gmail.com>
 * Copyright (C) 2011, Dariusz Luksza <dariusz@luksza.org>
 * Copyright (C) 2011, Christian Halstrick <christian.halstrick@sap.com>
 * Copyright (C) 2015, IBM Corporation (Dani Megert <daniel_megert@ch.ibm.com>)
 * Copyright (C) 2016, 2020 Thomas Wolf <thomas.wolf@paranor.ch>
 * Copyright (C) 2016, Stefan Dirix <sdirix@eclipsesource.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Andre Bossert <anb0s@anbos.de> - Cleaning up the DecoratableResourceAdapter
 *******************************************************************************/

package org.eclipse.egit.ui.internal.decorators;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.egit.core.internal.SafeRunnable;
import org.eclipse.egit.core.internal.indexdiff.IndexDiffData;
import org.eclipse.egit.core.internal.util.ExceptionCollector;
import org.eclipse.egit.core.project.GitProjectData;
import org.eclipse.egit.core.project.RepositoryMappingChangeListener;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.resources.IResourceState.StagingState;
import org.eclipse.egit.ui.internal.resources.ResourceStateFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.osgi.util.NLS;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.ui.IContributorResourceAdapter;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

/**
 * Supplies annotations for displayed resources
 *
 * This decorator provides annotations to indicate the status of each resource
 * when compared to <code>HEAD</code>, as well as the index in the relevant
 * repository.
 */
public class GitLightweightDecorator extends GitDecorator
		implements IPropertyChangeListener {

	/**
	 * Property constant pointing back to the extension point id of the
	 * decorator
	 */
	public static final String DECORATOR_ID = "org.eclipse.egit.ui.internal.decorators.GitLightweightDecorator"; //$NON-NLS-1$

	/**
	 * Collector for keeping the error view from filling up with exceptions
	 */
	private static final ExceptionCollector EXCEPTION_COLLECTOR = new ExceptionCollector(
			UIText.Decorator_exceptionMessageCommon, Activator.getPluginId(),
			IStatus.ERROR, Activator.getDefault().getLog());

	/** ID of the new symbolic font for trees and tables since Eclipse 4.17. */
	private static final String TREE_TABLE_FONT = "org.eclipse.ui.workbench.TREE_TABLE_FONT"; //$NON-NLS-1$

	private static final List<String> FONT_IDS = Arrays.asList(TREE_TABLE_FONT,
			UIPreferences.THEME_UncommittedChangeFont,
			UIPreferences.THEME_IgnoredResourceFont);

	private static final List<String> COLOR_IDS = Arrays.asList(
		UIPreferences.THEME_UncommittedChangeBackgroundColor,
			UIPreferences.THEME_UncommittedChangeForegroundColor,
			UIPreferences.THEME_IgnoredResourceBackgroundColor,
			UIPreferences.THEME_IgnoredResourceForegroundColor);

	private final ChangeTrackingColorsAndFonts resources;

	private final DecorationHelper helper;

	private RepositoryMappingChangeListener mappingChangeListener = changed -> fireLabelEvent();

	/**
	 * Constructs a new Git resource decorator
	 */
	public GitLightweightDecorator() {
		// This is an optimization to ensure that while decorating our fonts and
		// colors are pre-created and decoration can occur without having to syncExec.
		resources = new ChangeTrackingColorsAndFonts();
		helper = new DecorationHelper(
				Activator.getDefault().getPreferenceStore(), resources);
		resources.addListener(this::postLabelEvent);
		TeamUI.addPropertyChangeListener(this);
		Activator.addPropertyChangeListener(this);

		GitProjectData.addRepositoryChangeListener(mappingChangeListener);
	}

	@Override
	public void dispose() {
		super.dispose();
		resources.dispose();
		TeamUI.removePropertyChangeListener(this);
		Activator.removePropertyChangeListener(this);
		GitProjectData.removeRepositoryChangeListener(mappingChangeListener);
		mappingChangeListener = null;
	}

	/**
	 * This method should only be called by the decorator thread.
	 *
	 * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object,
	 *      org.eclipse.jface.viewers.IDecoration)
	 */
	@Override
	public void decorate(Object element, IDecoration decoration) {
		// Don't decorate if UI plugin is not running
		if (Activator.getDefault() == null) {
			return;
		}

		// Don't decorate if the workbench is not running or the workspace is
		// not yet ready or already shut down
		if (!PlatformUI.isWorkbenchRunning()
				|| ResourcesPlugin.getWorkspace() == null) {
			return;
		}

		final IResource resource = getResource(element);
		try {
			if (resource == null) {
				decorateResourceMapping(element, decoration);
			} else {
				decorateResource(resource, decoration);
			}
		} catch (CoreException e) {
			handleException(resource, e);
		} catch (Exception e) {
			handleException(resource, new CoreException(Activator
					.createErrorStatus(NLS.bind(UIText.Decorator_exceptionMessage, resource), e)));
		}
	}

	/**
	 * Decorates a single resource (i.e. a project).
	 *
	 * @param resource the resource to decorate
	 * @param decoration the decoration
	 */
	private void decorateResource(@NonNull IResource resource,
			IDecoration decoration) {
		if (resource.getType() == IResource.ROOT || !resource.isAccessible()) {
			return;
		}
		IndexDiffData indexDiffData = ResourceStateFactory.getInstance()
				.getIndexDiffDataOrNull(resource);

		if (indexDiffData == null) {
			return;
		}
		IDecoratableResource decoratableResource = new DecoratableResourceAdapter(
				indexDiffData, resource);
		helper.decorate(decoration, decoratableResource);
	}

	/**
	 * Decorates a resource mapping (i.e. a Working Set).
	 *
	 * @param element the element for which the decoration was initially called
	 * @param decoration the decoration
	 * @throws CoreException
	 */
	private void decorateResourceMapping(Object element, IDecoration decoration) throws CoreException {
		@SuppressWarnings("restriction")
		ResourceMapping mapping = Utils.getResourceMapping(element);
		if (mapping == null) {
			return;
		}

		boolean isWorkingSet = mapping.getModelObject() instanceof IWorkingSet;

		DecoratableResourceGroup decoRes;
		try {
			if (isWorkingSet) {
				decoRes = new DecoratableWorkingSet(mapping);
			} else {
				decoRes = new DecoratableResourceMapping(mapping);
			}
		} catch (IOException e) {
			throw new CoreException(Activator.createErrorStatus(
					NLS.bind(UIText.Decorator_exceptionMessage, element), e));
		}

		if (!decoRes.hasSharedResources()) {
			return;
		}

		/*
		 *  don't render question marks on working sets. !isTracked() can have two reasons:
		 *   1) nothing is tracked.
		 *   2) no indexDiff for the contained projects ready yet.
		 *  in both cases, don't do anything to not pollute the display of the sets.
		 */
		if (!decoRes.isTracked() && isWorkingSet) {
			return;
		}

		helper.decorate(decoration, decoRes);
	}

	/**
	 * A repository of colors and fonts.
	 */
	public static interface ColorsAndFonts {

		/**
		 * Retrieves the {@link Color} identified by {@code id}.
		 *
		 * @param id
		 *            of the color to get
		 * @return the color, or {@code null} if none
		 */
		Color getColor(String id);

		/**
		 * Retrieves the {@link Font} identified by {@code id}.
		 *
		 * @param id
		 *            of the font to get
		 * @return the font, or {@code null} if none
		 */
		Font getFont(String id);

		/**
		 * Retrieves the default font.
		 *
		 * @return the font
		 */
		Font getDefaultFont();

		/**
		 * Retrieves the default background color.
		 *
		 * @return the {@link RGB} of the color
		 */
		RGB getDefaultBackground();

	}

	/**
	 * A font and color cache that tracks changes to the fonts and colors used
	 * by Git decoration and that fires a notification when they have changed.
	 * <p>
	 * The main idea behind caching colors and fonts is to do load them on the
	 * UI thread once, and then access them from the decorators background
	 * thread. Otherwise the decorator would need to syncExec(), which kind of
	 * obviates running in the background.
	 * </p>
	 */
	public static class ChangeTrackingColorsAndFonts implements ColorsAndFonts {

		private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

		private final IPropertyChangeListener themeListener = event -> {
			final String prop = event.getProperty();
			if (prop == null) {
				return;
			}
			switch (prop) {
			case IThemeManager.CHANGE_CURRENT_THEME:
			case TREE_TABLE_FONT:
			case UIPreferences.THEME_UncommittedChangeBackgroundColor:
			case UIPreferences.THEME_UncommittedChangeFont:
			case UIPreferences.THEME_UncommittedChangeForegroundColor:
			case UIPreferences.THEME_IgnoredResourceFont:
			case UIPreferences.THEME_IgnoredResourceBackgroundColor:
			case UIPreferences.THEME_IgnoredResourceForegroundColor:
				reload();
				break;
			default:
				break;
			}
		};

		private volatile Map<String, Object> colorsOrFonts = new HashMap<>();

		private volatile RGB defaultBackground;

		/**
		 * Creates a new {@link ChangeTrackingColorsAndFonts} object.
		 */
		public ChangeTrackingColorsAndFonts() {
			PlatformUI.getWorkbench().getThemeManager().getCurrentTheme()
					.addPropertyChangeListener(themeListener);
			reload();
		}

		private void reload() {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(() -> {
				Map<String, Object> newResources = new HashMap<>();
				ITheme theme = PlatformUI.getWorkbench().getThemeManager()
						.getCurrentTheme();
				for (String actColor : COLOR_IDS) {
					newResources.put(actColor,
							theme.getColorRegistry().get(actColor));
				}
				for (String actFont : FONT_IDS) {
					newResources.put(actFont,
							theme.getFontRegistry().get(actFont));
				}
				newResources.put(JFaceResources.DEFAULT_FONT,
						theme.getFontRegistry().defaultFont());
				colorsOrFonts = newResources;
				defaultBackground = display
						.getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB();
			});
			notifyListeners();
		}

		private void notifyListeners() {
			for (Runnable listener : listeners) {
				SafeRunnable.run(listener::run);
			}
		}

		/**
		 * Register a listener to be called when the colors or fonts have
		 * changed. A no-op if the listener is already registered.
		 *
		 * @param runnable
		 *            to invoke
		 */
		public void addListener(Runnable runnable) {
			listeners.addIfAbsent(runnable);
		}

		/**
		 * Unregister a previously registered listener. A no-op if the listener
		 * is not currently registered.
		 *
		 * @param runnable
		 *            to unregister
		 */
		public void removeListener(Runnable runnable) {
			listeners.remove(runnable);
		}

		/**
		 * Disposes internal resources, removes all listeners, and stops
		 * reacting on color or font changes.
		 */
		public void dispose() {
			listeners.clear();
			PlatformUI.getWorkbench().getThemeManager().getCurrentTheme()
					.removePropertyChangeListener(themeListener);
		}

		@Override
		public Color getColor(String id) {
			Color color = (Color) colorsOrFonts.get(id);
			if (color != null && color.isDisposed()) {
				return null;
			}
			return color;
		}

		@Override
		public Font getFont(String id) {
			Font font = (Font) colorsOrFonts.get(id);
			if (font != null && font.isDisposed()) {
				return null;
			}
			return font;
		}

		@Override
		public Font getDefaultFont() {
			return getFont(JFaceResources.DEFAULT_FONT);
		}

		@Override
		public RGB getDefaultBackground() {
			return defaultBackground;
		}

	}

	/**
	 * Helper class for doing resource decoration, based on the given
	 * preferences
	 *
	 * Used for real-time decoration, as well as in the decorator preview
	 * preferences page
	 */
	public static class DecorationHelper {

		/** */
		public static final String BINDING_RESOURCE_NAME = "name"; //$NON-NLS-1$

		/** */
		public static final String BINDING_BRANCH_NAME = "branch"; //$NON-NLS-1$

		/** */
		public static final String BINDING_BRANCH_STATUS = "branch_status"; //$NON-NLS-1$

		/** */
		public static final String BINDING_REPOSITORY_NAME = "repository"; //$NON-NLS-1$

		/** */
		public static final String BINDING_SHORT_MESSAGE = "short_message"; //$NON-NLS-1$

		/** */
		public static final String BINDING_DIRTY_FLAG = "dirty"; //$NON-NLS-1$

		/** */
		public static final String BINDING_STAGED_FLAG = "staged"; //$NON-NLS-1$

		/** */
		public static final String FILE_FORMAT_DEFAULT="{dirty:>} {name}"; //$NON-NLS-1$

		/** */
		public static final String FOLDER_FORMAT_DEFAULT = "{dirty:>} {name}"; //$NON-NLS-1$

		/** */
		public static final String PROJECT_FORMAT_DEFAULT = "{dirty:>} {name} [{repository }{branch}{ branch_status}]"; //$NON-NLS-1$

		/** */
		public static final String SUBMODULE_FORMAT_DEFAULT = "{dirty:>} {name} [{branch}{ branch_status}]{ short_message}"; //$NON-NLS-1$

		private IPreferenceStore store;

		private ColorsAndFonts resources;

		/**
		 * Define a cached image descriptor which only creates the image data
		 * once
		 */
		private static class CachedImageDescriptor extends ImageDescriptor {
			private final ImageDescriptor descriptor;

			private ImageData data;

			public CachedImageDescriptor(ImageDescriptor descriptor) {
				this.descriptor = descriptor;
			}

			@Override
			public ImageData getImageData() {
				if (data == null) {
					data = descriptor.getImageData();
				}
				return data;
			}
		}

		/** Image for a resource being tracked by Git. */
		protected static final ImageDescriptor TRACKED_IMAGE;

		/** Image for a resource not being tracked by Git. */
		protected static final ImageDescriptor UNTRACKED_IMAGE;

		/** Image for a resource in the index but not yet committed. */
		protected static final ImageDescriptor STAGED_IMAGE;

		/** Image for a resource added to index but not yet committed. */
		protected static final ImageDescriptor STAGED_ADDED_IMAGE;

		/** Image for a resource removed from the index but not yet committed. */
		protected static final ImageDescriptor STAGED_REMOVED_IMAGE;

		/** Image for a tracked resource with a merge conflict. */
		protected static final ImageDescriptor CONFLICT_IMAGE;

		/** Image for a tracked resource in which we want to ignore changes. */
		protected static final ImageDescriptor ASSUME_UNCHANGED_IMAGE;

		/** Image for a tracked resource that is dirty. */
		protected static final ImageDescriptor DIRTY_IMAGE;

		static {
			TRACKED_IMAGE = new CachedImageDescriptor(TeamImages
					.getImageDescriptor(ISharedImages.IMG_CHECKEDIN_OVR));
			UNTRACKED_IMAGE = new CachedImageDescriptor(UIIcons.OVR_UNTRACKED);
			STAGED_IMAGE = new CachedImageDescriptor(UIIcons.OVR_STAGED);
			STAGED_ADDED_IMAGE = new CachedImageDescriptor(UIIcons.OVR_STAGED_ADD);
			STAGED_REMOVED_IMAGE = new CachedImageDescriptor(
					UIIcons.OVR_STAGED_REMOVE);
			CONFLICT_IMAGE = new CachedImageDescriptor(UIIcons.OVR_CONFLICT);
			ASSUME_UNCHANGED_IMAGE = new CachedImageDescriptor(UIIcons.OVR_ASSUMEUNCHANGED);
			DIRTY_IMAGE = new CachedImageDescriptor(UIIcons.OVR_DIRTY);
		}

		/**
		 * Constructs a decorator using the rules from the given
		 * {@code preferencesStore}.
		 * <p>
		 * Intended to be used in the GitDecoratorPreferencesPage only. The
		 * decorator assumes that colors, fonts, and the theme do not change
		 * while it is in use.
		 * </p>
		 *
		 * @param preferencesStore
		 *            the preferences store with the preferred decorator rules
		 */
		public DecorationHelper(IPreferenceStore preferencesStore) {
			this(preferencesStore, new ColorsAndFonts() {

				private final ITheme current = PlatformUI.getWorkbench()
						.getThemeManager().getCurrentTheme();

				private volatile RGB defaultBackground;

				@Override
				public Color getColor(String id) {
					return current.getColorRegistry().get(id);
				}

				@Override
				public Font getFont(String id) {
					return current.getFontRegistry().get(id);
				}

				@Override
				public Font getDefaultFont() {
					return current.getFontRegistry().defaultFont();
				}

				@Override
				public RGB getDefaultBackground() {
					if (defaultBackground == null) {
						Display display = PlatformUI.getWorkbench()
								.getDisplay();
						display.syncExec(() -> {
							defaultBackground = display
									.getSystemColor(SWT.COLOR_LIST_BACKGROUND)
									.getRGB();
						});
					}
					return defaultBackground;
				}
			});
		}

		/**
		 * Creates a new {@link DecorationHelper} based on the given
		 * {@link IPreferenceStore} using the given {@link ColorsAndFonts}.
		 *
		 * @param preferencesStore
		 *            to get decoration settings from
		 * @param resources
		 *            to get colors and fonts from
		 */
		public DecorationHelper(IPreferenceStore preferencesStore,
				ColorsAndFonts resources) {
			store = preferencesStore;
			this.resources = resources;
		}

		/**
		 * Decorates the given <code>decoration</code> based on the state of the
		 * given <code>resource</code>, using the preferences passed when
		 * constructing this decoration helper.
		 *
		 * @param decoration
		 *            the decoration to decorate
		 * @param resource
		 *            the resource to retrieve state from
		 */
		public void decorate(IDecoration decoration,
				IDecoratableResource resource) {
			decorateFontAndColour(decoration, resource);

			if (resource.isIgnored())
				return;

			decorateText(decoration, resource);
			decorateIcons(decoration, resource);
		}

		private void decorateFontAndColour(IDecoration decoration,
				IDecoratableResource resource) {
			Color bc = null;
			Color fc = null;
			Font f = null;
			if (resource.isIgnored()) {
				bc = resources.getColor(
						UIPreferences.THEME_IgnoredResourceBackgroundColor);
				fc = resources.getColor(
						UIPreferences.THEME_IgnoredResourceForegroundColor);
				f = resources.getFont(
						UIPreferences.THEME_IgnoredResourceFont);
			} else if (!resource.isTracked()
					|| resource.isDirty()
					|| resource.isStaged()) {
				bc = resources.getColor(
						UIPreferences.THEME_UncommittedChangeBackgroundColor);
				fc = resources.getColor(
						UIPreferences.THEME_UncommittedChangeForegroundColor);
				f = resources.getFont(
						UIPreferences.THEME_UncommittedChangeFont);
			}
			if (bc != null) {
				setBackgroundColor(decoration, bc);
			}
			if (fc != null) {
				decoration.setForegroundColor(fc);
			}
			if (f == null
					|| isSameFont(f, resources.getDefaultFont())) {
				// Try the TREE_TABLE_FONT new in Eclipse 4.17
				Font treeTableFont = resources.getFont(TREE_TABLE_FONT);
				if (treeTableFont != null) {
					f = treeTableFont;
				}
			}
			if (f != null) {
				decoration.setFont(f);
			}
		}

		private boolean isSameFont(@NonNull Font a, Font b) {
			return a.equals(b) //
					|| (b != null
							&& Arrays.equals(a.getFontData(), b.getFontData()));
		}

		private void setBackgroundColor(IDecoration decoration, Color color) {
			// In case the color is not changed from the default, do not set the
			// background because it paints over things from the theme such as
			// alternating line colors (see bug 412183).
			if (!color.getRGB().equals(resources.getDefaultBackground()))
				decoration.setBackgroundColor(color);
		}

		private void decorateText(IDecoration decoration,
				IDecoratableResource resource) {
			String format = ""; //$NON-NLS-1$
			switch (resource.getType()) {
			default:
			case IResource.FILE:
				format = store
						.getString(UIPreferences.DECORATOR_FILETEXT_DECORATION);
				break;
			case IResource.FOLDER:
			case DecoratableResourceMapping.RESOURCE_MAPPING:
				if (resource.isRepositoryContainer()) {
					// Use the submodule formatting if it's a submodule or
					// nested repository root
					format = store.getString(
							UIPreferences.DECORATOR_SUBMODULETEXT_DECORATION);
				} else {
					format = store.getString(
							UIPreferences.DECORATOR_FOLDERTEXT_DECORATION);
				}
				break;
			case DecoratableWorkingSet.WORKING_SET:
				// working sets will use the project formatting but only if the
				// repo and branch is available
				if (resource.getRepositoryName() != null
						&& resource.getBranch() != null)
					format = store
							.getString(UIPreferences.DECORATOR_PROJECTTEXT_DECORATION);
				else
					format = store
							.getString(UIPreferences.DECORATOR_FOLDERTEXT_DECORATION);
				break;
			case IResource.PROJECT:
				format = store
						.getString(UIPreferences.DECORATOR_PROJECTTEXT_DECORATION);
				break;
			}

			Map<String, String> bindings = new HashMap<>();
			bindings.put(BINDING_RESOURCE_NAME, resource.getName());
			bindings.put(BINDING_REPOSITORY_NAME, resource.getRepositoryName());
			bindings.put(BINDING_BRANCH_NAME, resource.getBranch());
			bindings.put(BINDING_BRANCH_STATUS, resource.getBranchStatus());
			bindings.put(BINDING_DIRTY_FLAG, resource.isDirty() ? ">" : null); //$NON-NLS-1$
			bindings.put(BINDING_STAGED_FLAG, resource.isStaged() ? "*" : null); //$NON-NLS-1$
			bindings.put(BINDING_SHORT_MESSAGE, resource.getCommitMessage());
			decorate(decoration, format, bindings);
		}

		private void decorateIcons(IDecoration decoration,
				IDecoratableResource resource) {
			ImageDescriptor overlay = null;

			if (resource.isTracked()) {
				if (store.getBoolean(UIPreferences.DECORATOR_SHOW_TRACKED_ICON))
					overlay = TRACKED_IMAGE;

				if (store
						.getBoolean(UIPreferences.DECORATOR_SHOW_ASSUME_UNCHANGED_ICON)
						&& resource.isAssumeUnchanged())
					overlay = ASSUME_UNCHANGED_IMAGE;

				// Staged overrides tracked
				StagingState staged = resource.getStagingState();
				if (store.getBoolean(UIPreferences.DECORATOR_SHOW_STAGED_ICON)
						&& staged != StagingState.NOT_STAGED) {
					if (staged == StagingState.ADDED)
						overlay = STAGED_ADDED_IMAGE;
					else if (staged == StagingState.REMOVED)
						overlay = STAGED_REMOVED_IMAGE;
					else
						overlay = STAGED_IMAGE;
				}

				// Dirty overrides staged
				if(store
						.getBoolean(UIPreferences.DECORATOR_SHOW_DIRTY_ICON) && resource.isDirty()) {
					overlay = DIRTY_IMAGE;
				}

				// Conflicts override everything
				if (store
						.getBoolean(UIPreferences.DECORATOR_SHOW_CONFLICTS_ICON)
						&& resource.hasConflicts())
					overlay = CONFLICT_IMAGE;

			} else if (store
					.getBoolean(UIPreferences.DECORATOR_SHOW_UNTRACKED_ICON)) {
				overlay = UNTRACKED_IMAGE;
			}

			// Overlays can only be added once, so do it at the end
			decoration.addOverlay(overlay);
		}

		/**
		 * Decorates the given <code>decoration</code>, using the specified text
		 * <code>format</code>, and mapped using the variable bindings from
		 * <code>bindings</code>
		 *
		 * @param decoration
		 *            the decoration to decorate
		 * @param format
		 *            the format to base the decoration on
		 * @param bindings
		 *            the bindings between variables in the format and actual
		 *            values
		 */
		public static void decorate(IDecoration decoration, String format,
				Map<String, String> bindings) {
			StringBuilder prefix = new StringBuilder();
			StringBuilder suffix = new StringBuilder();
			StringBuilder output = prefix;

			int length = format.length();
			int start = -1;
			int end = length;
			while (true) {
				if ((end = format.indexOf('{', start)) > -1) {
					output.append(format.substring(start + 1, end));
					if ((start = format.indexOf('}', end)) > -1) {
						String key = format.substring(end + 1, start);
						String s;
						boolean spaceBefore = false;
						boolean spaceAfter = false;

						// Allow users to override the binding
						if (key.indexOf(':') > -1) {
							String[] keyAndBinding = key.split(":", 2); //$NON-NLS-1$
							key = keyAndBinding[0];
							if (keyAndBinding.length > 1
									&& bindings.get(key) != null)
								bindings.put(key, keyAndBinding[1]);
						} else {
							if (key.charAt(0) == ' ') {
								spaceBefore = true;
								key = key.substring(1);
							}
							if (key.charAt(key.length() - 1) == ' ') {
								spaceAfter = true;
								key = key.substring(0, key.length() - 1);
							}
						}


						// We use the BINDING_RESOURCE_NAME key to determine if
						// we are doing the prefix or suffix. The name isn't
						// actually part of either.
						if (key.equals(BINDING_RESOURCE_NAME)) {
							output = suffix;
							s = null;
						} else {
							s = bindings.get(key);
						}

						if (s != null) {
							if (spaceBefore)
								output.append(' ');
							output.append(s);
							if (spaceAfter)
								output.append(' ');
						} else {
							// Support removing prefix character if binding is
							// null
							int curLength = output.length();
							if (curLength > 0) {
								char c = output.charAt(curLength - 1);
								if (c == ':' || c == '@') {
									output.deleteCharAt(curLength - 1);
								}
							}
						}
					} else {
						output.append(format.substring(end, length));
						break;
					}
				} else {
					output.append(format.substring(start + 1, length));
					break;
				}
			}

			String prefixString = prefix.toString().replaceAll("^\\s+", ""); //$NON-NLS-1$ //$NON-NLS-2$
			if (prefixString.length() > 0)
				decoration.addPrefix(TextProcessor.process(prefixString,
						"()[].")); //$NON-NLS-1$
			String suffixString = suffix.toString().replaceAll("\\s+$", ""); //$NON-NLS-1$ //$NON-NLS-2$
			if (suffixString.length() > 0)
				decoration.addSuffix(TextProcessor.process(suffixString,
						"()[].")); //$NON-NLS-1$
		}
	}

	// -------- Refresh handling --------

	/**
	 * Perform a blanket refresh of all decorations
	 */
	public static void refresh() {
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> PlatformUI
				.getWorkbench().getDecoratorManager().update(DECORATOR_ID));
	}

	/**
	 * Callback for IPropertyChangeListener events
	 *
	 * If any of the relevant preferences has been changed we refresh all
	 * decorations (all projects and their resources).
	 *
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		final String prop = event.getProperty();
		if (prop == null) {
			return;
		}
		switch (prop) {
		case TeamUI.GLOBAL_IGNORES_CHANGED:
		case TeamUI.GLOBAL_FILE_TYPES_CHANGED:
		case Activator.DECORATORS_CHANGED:
			postLabelEvent();
			break;
		default:
			break;
		}
	}

	// -------- Helper methods --------

	private static IResource getResource(Object actElement) {
		Object element = actElement;
		if (element instanceof ResourceMapping) {
			element = ((ResourceMapping) element).getModelObject();
		}

		IResource resource = null;
		if (element instanceof IResource) {
			resource = (IResource) element;
		} else if (element instanceof IAdaptable) {
			final IAdaptable adaptable = (IAdaptable) element;
			resource = Adapters.adapt(adaptable, IResource.class);
			if (resource == null) {
				final IContributorResourceAdapter adapter = Adapters
						.adapt(adaptable, IContributorResourceAdapter.class);
				if (adapter != null)
					resource = adapter.getAdaptedResource(adaptable);
			}
		}

		return resource;
	}

	/**
	 * Handle exceptions that occur in the decorator. Exceptions are only logged
	 * for resources that are accessible (i.e. exist in an open project).
	 *
	 * @param resource
	 *            The resource that triggered the exception
	 * @param e
	 *            The exception that occurred
	 */
	private static void handleException(IResource resource, CoreException e) {
		if (resource == null || resource.isAccessible())
			EXCEPTION_COLLECTOR.handleException(e);
	}

	@Override
	protected String getName() {
		return UIText.GitLightweightDecorator_name;
	}
}