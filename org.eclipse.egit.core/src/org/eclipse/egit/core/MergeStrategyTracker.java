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
package org.eclipse.egit.core;

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
import org.eclipse.core.runtime.IRegistryEventListener;
import org.eclipse.core.runtime.Platform;
import org.eclipse.egit.core.internal.CoreText;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * An OSGi component maintaining the known merge strategies.
 */
@Component
public class MergeStrategyTracker {

	// TODO: investigate using an IExtensionTracker for this?

	private static final String EXTENSION_POINT = "org.eclipse.egit.core.mergeStrategy"; //$NON-NLS-1$

	private IExtensionRegistry registry;

	private MergeStrategyRegistryListener mergeStrategyRegistryListener;

	@Reference
	void setExtensionRegistry(IExtensionRegistry registry) {
		this.registry = registry;
	}

	@Reference
	void setWorkspace(@SuppressWarnings("unused") IWorkspace workspace) {
		// Nothing. But this code instantiates extension classes; we have no way
		// of knowing what they need. This reference makes this component be
		// activated only once the workspace is available, which hopefully is
		// good enough.
	}

	@Activate
	void start() {
		mergeStrategyRegistryListener = new MergeStrategyRegistryListener(
				registry);
		Platform.getExtensionRegistry()
				.addListener(mergeStrategyRegistryListener, EXTENSION_POINT);
	}

	@Deactivate
	void shutDown() {
		if (mergeStrategyRegistryListener != null) {
			registry.removeListener(mergeStrategyRegistryListener);
			mergeStrategyRegistryListener = null;
		}
	}

	private static class MergeStrategyRegistryListener
			implements IRegistryEventListener {

		private Map<String, MergeStrategyDescriptor> strategies;

		private MergeStrategyRegistryListener(IExtensionRegistry registry) {
			strategies = new LinkedHashMap<>();
			IConfigurationElement[] elements = registry
					.getConfigurationElementsFor(EXTENSION_POINT);
			if (loadMergeStrategies(elements)) {
				Activator.getDefault().setMergeStrategies(getStrategies());
			}
		}

		private Collection<MergeStrategyDescriptor> getStrategies() {
			return Collections.unmodifiableCollection(strategies.values());
		}

		@Override
		public void added(IExtension[] extensions) {
			boolean changed = false;
			for (IExtension extension : extensions) {
				changed |= loadMergeStrategies(
						extension.getConfigurationElements());
			}
			if (changed) {
				Activator.getDefault().setMergeStrategies(getStrategies());
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
				Activator.getDefault().setMergeStrategies(getStrategies());
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
				Activator.logError(NLS.bind(CoreText.MergeStrategy_MissingName,
						strategy.getClass()), null);
				result = false;
			} else if (strategies.containsKey(name)) {
				// Other strategy already registered for this name
				Activator
						.logError(NLS.bind(CoreText.MergeStrategy_DuplicateName,
								new Object[] { name,
										strategies.get(name).getImplementedBy(),
										strategy.getClass() }),
								null);
				result = false;
			} else if (MergeStrategy.get(name) != null
					&& MergeStrategy.get(name) != strategy) {
				// The name is reserved by a core JGit strategy, and the
				// provided instance is not that of JGit
				Activator.logError(NLS.bind(CoreText.MergeStrategy_ReservedName,
						new Object[] { name, MergeStrategy.get(name).getClass(),
								strategy.getClass() }),
						null);
				result = false;
			}
			return result;
		}
	}
}
