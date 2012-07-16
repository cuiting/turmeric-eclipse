/*******************************************************************************
 * Copyright (c) 2006-2010 eBay Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/
package org.ebayopensource.turmeric.eclipse.repositorysystem.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.ebayopensource.turmeric.eclipse.repositorysystem.RepositorySystemActivator;
import org.ebayopensource.turmeric.eclipse.repositorysystem.preferences.core.PreferenceConstants;
import org.ebayopensource.turmeric.eclipse.repositorysystem.preferences.core.PreferenceReader;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;


/**
 * The Class GlobalRepositorySystem.
 *
 * @author smathew
 * 
 * This is the Global access point for all other systems to query and set the
 * repo systems. The extension point are processed and are kept inside a local
 * storage and the processing happens only once per plugin activation
 * @see ISOARepositorySystem
 */
public class GlobalRepositorySystem {

	/** The available repository systems. */
	List<ISOARepositorySystem> availableRepositorySystems;

	private ISOARepositorySystem activeRepositorySystem = null;

	private static final GlobalRepositorySystem globalRepositorySystem = new GlobalRepositorySystem();

	// singleton(simulated one, not as per book) to make the extension
	// processing execute once
	private GlobalRepositorySystem() {
		availableRepositorySystems = new ArrayList<ISOARepositorySystem>();
		final IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry
				.getExtensionPoint(RepositorySystemActivator.PLUGIN_ID
						+ ".repositorysystem");
		if (extensionPoint != null) {
			IExtension[] extensions = extensionPoint.getExtensions();
			boolean foundV3 = false;
			for (final IExtension extension : extensions) {
				for (final IConfigurationElement element : extension
						.getConfigurationElements()) {
					ISOARepositorySystem repositorySystem;
					try {
						repositorySystem = (ISOARepositorySystem) element
								.createExecutableExtension("RepositorySystem");
						availableRepositorySystems.add(repositorySystem);
						if (PreferenceConstants._PREF_DEFAULT_REPOSITORY_SYSTEM.equals(repositorySystem.getId())) {
							foundV3 = true;
						}
					} catch (CoreException e) {
						// The SOA logger is not available yet, need an altertinave way to report the error
						e.printStackTrace();
					}
				}
			}
			if (availableRepositorySystems.isEmpty() == true) {
				throw new RuntimeException("Can not find any repository system for extension point->" 
						+ extensionPoint.getUniqueIdentifier());
			}
			if (foundV3 == false && availableRepositorySystems.isEmpty() == false) {
				//the default V3 plugin is not installed, we should use the first available repo system
				IEclipsePreferences prefs = RepositorySystemActivator.getDefault().getPreferences();
				prefs.put(PreferenceConstants.PREF_REPOSITORY_SYSTEM, availableRepositorySystems.get(0).getId());
			}
			//SOALogger.setBuildSystemName(systemID);
		}

	}

	/**
	 * Instance of.
	 *
	 * @return the global repository system
	 */
	public static GlobalRepositorySystem instanceOf() {
		return globalRepositorySystem;
	}

	/**
	 * Gets the active repository system.
	 *
	 * @return the active repository system
	 */
	public ISOARepositorySystem getActiveRepositorySystem() {
		if (activeRepositorySystem == null) {
			final String systemID = PreferenceReader.getCurrentRepositorySystemId();
			//SOALogger.setBuildSystemName(systemID);
			activeRepositorySystem = GlobalRepositorySystem.instanceOf()
					.getRepositorySystem(systemID);
		}
		return activeRepositorySystem;
	}

	/**
	 * Sets the active repository system.
	 *
	 * @param activeRepositorySystem the new active repository system
	 */
	public void setActiveRepositorySystem(
			ISOARepositorySystem activeRepositorySystem) {
		this.activeRepositorySystem = activeRepositorySystem;
	}

	/**
	 * Gets the available repository systems.
	 *
	 * @return the available repository systems
	 */
	public List<ISOARepositorySystem> getAvailableRepositorySystems() {
		return availableRepositorySystems;
	}

	/**
	 * Sets the available repository systems.
	 *
	 * @param availableRepositorySystems the new available repository systems
	 */
	public void setAvailableRepositorySystems(
			List<ISOARepositorySystem> availableRepositorySystems) {
		this.availableRepositorySystems = availableRepositorySystems;
	}

	/**
	 * Not a null safe method.
	 *
	 * @param id the id
	 * @return the repository system
	 */
	public ISOARepositorySystem getRepositorySystem(String id) {
		for (ISOARepositorySystem system : availableRepositorySystems) {
			if (StringUtils.equalsIgnoreCase(id, system.getId())) {
				return system;
			}
		}
		return null;
	}

}
