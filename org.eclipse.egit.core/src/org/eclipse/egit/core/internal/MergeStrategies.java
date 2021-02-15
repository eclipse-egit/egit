/*******************************************************************************
 * Copyright (c) 2021 Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Thomas Wolf -- factored out of Activator
 *******************************************************************************/
package org.eclipse.egit.core.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IRegistryEventListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.GitCorePreferences;
import org.eclipse.egit.core.MergeStrategyDescriptor;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.util.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * Global registry of merge strategies that are registered via an extension
 * point.
 */
public class MergeStrategies {

	/** Extension point ID for merge strategies. */
	public static final String EXTENSION_POINT = "org.eclipse.egit.core.mergeStrategy"; //$NON-NLS-1$

	private static volatile Loader loader;

	private MergeStrategies() {
		// No instantiation.
	}

	// Note that we must not wait (via Job.join() or otherwise) until the loader
	// job is done. In normal circumstances, none of the public methods here
	// should be called that early -- getPreferredMergeStrategy() is used by git
	// operations, and getRegisteredMergeStrategies() by a preference page. The
	// loader job is scheduled very early and normally has finished long before
	// either method is called. Waiting for the job might lead to deadlocks;
	// some calls are in workspace operations, and we have absolutely no control
	// over what instantiating a MergeStrategy in the job might do. So we must
	// not block in any way. Instead, the methods return empty results if called
	// before the loader job is done.

	/**
	 * Provides the 3-way merge strategy to use according to the user's
	 * preferences. The preferred merge strategy is JGit's default merge
	 * strategy unless the user has explicitly chosen a different strategy among
	 * the registered strategies.
	 *
	 * @return The {@link MergeStrategy} to use, can be {@code null}, in which
	 *         case the default merge strategy should be used as defined by
	 *         JGit.
	 */
	public static MergeStrategy getPreferredMergeStrategy() {
		String key = Platform.getPreferencesService().getString(
				Activator.PLUGIN_ID,
				GitCorePreferences.core_preferredMergeStrategy, null, null);
		if (!StringUtils.isEmptyOrNull(key)
				&& !GitCorePreferences.core_preferredMergeStrategy_Default
						.equals(key)) {
			Loader loadJob = loader;
			boolean jobDone = loadJob != null && loadJob.isDone();
			MergeStrategy result = MergeStrategy.get(key);
			if (result != null) {
				return result;
			}
			if (jobDone) {
				Activator.logError(MessageFormat.format(
						CoreText.Activator_invalidPreferredMergeStrategy, key),
						null);
			}
		}
		return null;
	}

	/**
	 * Provides all currently registered {@link MergeStrategies}.
	 *
	 * @return a collection of all registered strategies; may be empty
	 */
	@NonNull
	public static Collection<MergeStrategyDescriptor> getRegisteredMergeStrategies() {
		Loader loadJob = loader;
		if (loadJob != null && loadJob.isDone()) {
			return loadJob.getRegisteredMergeStrategies();
		}
		return Collections.emptyList();
	}

	/**
	 * An OSGi component for loading and maintaining the known merge strategies.
	 */
	@Component
	public static class Loader extends Job {

		private IExtensionRegistry registry;

		private volatile MergeStrategyRegistryListener mergeStrategyRegistryListener;

		private volatile boolean done;

		/**
		 * Creates a new {@link Loader}. Instantiated via OSGi DS.
		 */
		public Loader() {
			super(CoreText.MergeStrategy_LoaderJob);
			setSystem(true);
			setUser(false);
		}

		@Reference
		void setExtensionRegistry(IExtensionRegistry registry) {
			this.registry = registry;
		}

		@Reference
		void setWorkspace(@SuppressWarnings("unused") IWorkspace workspace) {
			// Nothing. But this code instantiates extension classes; we have no
			// way of knowing what they need. This reference makes this
			// component be activated only once the workspace is available,
			// which hopefully is good enough.
		}

		boolean isDone() {
			return done;
		}

		Collection<MergeStrategyDescriptor> getRegisteredMergeStrategies() {
			MergeStrategyRegistryListener listener = mergeStrategyRegistryListener;
			if (listener != null) {
				return listener.getCurrentStrategies();
			}
			return Collections.emptyList();
		}

		@Activate
		void start() {
			schedule();
			loader = this;
		}

		@Deactivate
		void shutDown() {
			loader = null;
			cancel();
			try {
				join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if (mergeStrategyRegistryListener != null) {
				registry.removeListener(mergeStrategyRegistryListener);
				mergeStrategyRegistryListener = null;
			}
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				MergeStrategyRegistryListener listener = new MergeStrategyRegistryListener(
						registry);
				if (!monitor.isCanceled()) {
					registry.addListener(listener,
							MergeStrategies.EXTENSION_POINT);
					mergeStrategyRegistryListener = listener;
					return Status.OK_STATUS;
				}
			} finally {
				done = true;
				monitor.done();
			}
			return Status.CANCEL_STATUS;
		}
	}

	private static class MergeStrategyRegistryListener
			implements IRegistryEventListener {

		private Map<String, MergeStrategyDescriptor> strategies = new LinkedHashMap<>();

		private volatile Collection<MergeStrategyDescriptor> currentStrategies = Collections
				.emptyList();

		private MergeStrategyRegistryListener(IExtensionRegistry registry) {
			IConfigurationElement[] elements = registry
					.getConfigurationElementsFor(
							MergeStrategies.EXTENSION_POINT);
			if (loadMergeStrategies(elements)) {
				currentStrategies = getStrategies();
			}
		}

		private Collection<MergeStrategyDescriptor> getStrategies() {
			// Create a copy to prevent concurrent access.
			return new ArrayList<>(strategies.values());
		}

		@NonNull
		public Collection<MergeStrategyDescriptor> getCurrentStrategies() {
			return currentStrategies;
		}

		@Override
		public void added(IExtension[] extensions) {
			boolean changed = false;
			for (IExtension extension : extensions) {
				changed |= loadMergeStrategies(
						extension.getConfigurationElements());
			}
			if (changed) {
				currentStrategies = getStrategies();
			}
		}

		@Override
		public void added(IExtensionPoint[] extensionPoints) {
			// Nothing to do here
		}

		@Override
		public void removed(IExtension[] extensions) {
			boolean changed = false;
			for (IExtension extension : extensions) {
				for (IConfigurationElement element : extension
						.getConfigurationElements()) {
					try {
						Object ext = element.createExecutableExtension("class"); //$NON-NLS-1$
						if (ext instanceof MergeStrategy) {
							MergeStrategy strategy = (MergeStrategy) ext;
							changed |= strategies
									.remove(strategy.getName()) != null;
						}
					} catch (CoreException e) {
						Activator.logError(CoreText.MergeStrategy_UnloadError,
								e);
					}
				}
			}
			if (changed) {
				currentStrategies = getStrategies();
			}
		}

		@Override
		public void removed(IExtensionPoint[] extensionPoints) {
			// Nothing to do here
		}

		private boolean loadMergeStrategies(IConfigurationElement[] elements) {
			boolean changed = false;
			for (IConfigurationElement element : elements) {
				try {
					Object ext = element.createExecutableExtension("class"); //$NON-NLS-1$
					if (ext instanceof MergeStrategy) {
						MergeStrategy strategy = (MergeStrategy) ext;
						String name = element.getAttribute("name"); //$NON-NLS-1$
						if (name == null || name.isEmpty()) {
							name = strategy.getName();
						}
						if (canRegister(name, strategy)) {
							if (MergeStrategy.get(name) == null) {
								MergeStrategy.register(name, strategy);
							}
							strategies.put(name,
									new MergeStrategyDescriptor(name,
											element.getAttribute("label"), //$NON-NLS-1$
											strategy.getClass()));
							changed = true;
						}
					}
				} catch (CoreException e) {
					Activator.logError(CoreText.MergeStrategy_LoadError, e);
				}
			}
			return changed;
		}

		/**
		 * Checks whether it's possible to register the provided strategy with
		 * the given name
		 *
		 * @param name
		 *            Name to use to register the strategy
		 * @param strategy
		 *            Strategy to register
		 * @return <code>true</code> if the name is neither null nor empty, no
		 *         other strategy is already register for the same name, and the
		 *         name is not one of the core JGit strategies. If the given
		 *         name is that of a core JGit strategy, the method will return
		 *         <code>true</code> only if the strategy is the matching JGit
		 *         strategy for that name.
		 */
		private boolean canRegister(String name, MergeStrategy strategy) {
			boolean result = true;
			if (name == null || name.isEmpty()) {
				// name is mandatory
				Activator.logError(
						MessageFormat.format(CoreText.MergeStrategy_MissingName,
								strategy.getClass()),
						null);
				result = false;
			} else if (strategies.containsKey(name)) {
				// Other strategy already registered for this name
				Activator.logError(MessageFormat.format(
						CoreText.MergeStrategy_DuplicateName, name,
						strategies.get(name).getImplementedBy(),
						strategy.getClass()), null);
				result = false;
			} else if (MergeStrategy.get(name) != null
					&& MergeStrategy.get(name) != strategy) {
				// The name is reserved by a core JGit strategy, and the
				// provided instance is not that of JGit
				Activator.logError(MessageFormat.format(
						CoreText.MergeStrategy_ReservedName, name,
						MergeStrategy.get(name).getClass(),
						strategy.getClass()), null);
				result = false;
			}
			return result;
		}
	}
}
