/*******************************************************************************
 * Copyright (c) 2006-2010 eBay Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/
package org.ebayopensource.turmeric.eclipse.repositorysystem.core;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ebayopensource.turmeric.common.config.LibraryType;
import org.ebayopensource.turmeric.eclipse.core.logging.SOALogger;
import org.ebayopensource.turmeric.eclipse.core.resources.constants.SOATypeLibraryConstants;
import org.ebayopensource.turmeric.eclipse.resources.model.AssetInfo;
import org.ebayopensource.turmeric.eclipse.soatools.Activator;
import org.ebayopensource.turmeric.eclipse.utils.classloader.SOAPluginClassLoader;
import org.ebayopensource.turmeric.eclipse.utils.collections.ListUtil;
import org.ebayopensource.turmeric.eclipse.utils.plugin.WorkspaceUtil;
import org.ebayopensource.turmeric.tools.library.RegistryUpdateDetails;
import org.ebayopensource.turmeric.tools.library.SOAGlobalRegistryFactory;
import org.ebayopensource.turmeric.tools.library.SOATypeRegistry;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.Bundle;

/**
 * The Class SOAGlobalRegistryAdapter.
 *
 * @author smathew
 * 
 * This class adapts the SOA Registry from Tools to Plugin.
 * 
 * The changes to SOA Tools API should only affect this class.
 */
public class SOAGlobalRegistryAdapter {

	private static SOATypeRegistry soaTypeRegistry = null;
	private static Set<String> typeLibNamesForSOATools;
	private static Set<File> typeLibLocationsForSOATools;
	private static SOAPluginClassLoader typeLibclassLoader;
	private static final SOALogger logger = SOALogger.getLogger();
	private static final SOAGlobalRegistryAdapter registryAdapter = new SOAGlobalRegistryAdapter();
	
	private SOAGlobalRegistryAdapter() {
		
	}

	/**
	 * Returns the global singleton instance representing a type registry. A
	 * progress monitor would be displayed if called from a UI thread Otherwise
	 * this willbe a silent operation
	 *
	 * @return single instance of SOAGlobalRegistryAdapter
	 */
	
	public static SOAGlobalRegistryAdapter getInstance() {
		return registryAdapter;
	}
	
	/**
	 * Gets the global registry.
	 *
	 * @return the global registry
	 * @throws Exception the exception
	 */
	public SOATypeRegistry getGlobalRegistry() throws Exception {
		if (soaTypeRegistry == null) {
			long startTime = System.currentTimeMillis();
			synchronized (SOAGlobalRegistryAdapter.class) {
				if (soaTypeRegistry == null) {
//					monitor.beginTask(
//							"Initializing SOA Type Registry...", 100);
//					monitor.internalWorked(10);
					final ClassLoader originalClassLoader = Thread
							.currentThread().getContextClassLoader();
					
					try {
						init();
//						monitor.worked(20);
						Thread thread = Thread.currentThread();
						ClassLoader loader = thread.getContextClassLoader();
						thread.setContextClassLoader(SOAGlobalRegistryFactory.class.getClassLoader());
						SOATypeRegistry typeReg = GlobalRepositorySystem
						.instanceOf().getActiveRepositorySystem()
						.getTypeRegistryBridge().getSOATypeRegistry();
						thread.setContextClassLoader(loader);

//						monitor.worked(40);
						typeLibclassLoader.setPluginBundles(
								(GlobalRepositorySystem
										.instanceOf().getActiveRepositorySystem()
										.getTypeRegistryBridge().getPluginBundles()));
//						monitor.worked(10);
						Thread.currentThread().setContextClassLoader(
								typeLibclassLoader);
//						monitor.worked(10);
						List<RegistryUpdateDetails> libraries = 
							typeReg.populateRegistryWithTypeLibrariesDetailed(ListUtil.arrayList(
								typeLibNamesForSOATools));
						if (libraries != null) {
							for (RegistryUpdateDetails details : libraries) {
								if (details.isUpdateSucess() == false) {
									logger.warning("Invalid type library->", 
											details.getLibraryName(), ". Detailed Error: ", details.getMessage());
								}
							}
						}
//						monitor.worked(10);
						soaTypeRegistry = typeReg;
					} catch (Exception e) {
						logger.error(e);
						
					} finally {
						Thread.currentThread().setContextClassLoader(
								originalClassLoader);
//						monitor.done();
					}
					
//					Job job = new GlobalRegistryJob("Retrieve Global Registry");
//					job.setUser(true);
//					job.setPriority(Job.INTERACTIVE);
//					job.schedule();
//					
//					try {
////						if (Display.getCurrent() == null) {
////							// non-UI thread
////							runnable.run(ProgressUtil.getDefaultMonitor(null));
////						} else {
////							final IProgressService service = PlatformUI
////							.getWorkbench().getProgressService();
////							service.run(false, false, runnable);
////						}
//					    int cnt = 0;
//						while ((job.getState() == Job.RUNNING || job.getState() == Job.WAITING)) {
//					   // while (cnt < 15 && soaTypeRegistry == null) {
//							logger.warning("SOA types registry not initialized yet, sleeping..."+cnt);
//							if(job.getState()==Job.WAITING){
//								logger.warning("waiting");
//								
//							}
//							else if (job.getState()==Job.RUNNING){
//								logger.warning("running");
//							}
//							Thread.sleep(1000);
//							cnt++;
//						}
//					} finally {
//						if (SOALogger.DEBUG) {
//							long duration = System.currentTimeMillis() - startTime;
//							logger.info("Time taken for initializing SOA global type registry is ", 
//									duration, " ms.");
//						}
//					}
				}
			}
		}
	
		return soaTypeRegistry;
	}


	/**
	 * Codegen requires the type library names to create the registry object.
	 * This API takes the typeLibrary names, find the location out, build a
	 * class loader and pass the type lib names to codegen to populate the
	 * registry.
	 *
	 * @param typelibNames the typelib names
	 * @throws Exception the exception
	 */
	public void populateRegistry(String... typelibNames)
			throws Exception {
		getGlobalRegistry();

		ClassLoader originalClassLoader = Thread.currentThread()
				.getContextClassLoader();
		try {
			init();
			ArrayList<Bundle> bundles = new ArrayList<Bundle>();
			bundles.add(Activator.getDefault().getBundle());
			typeLibclassLoader.setPluginBundles(bundles);
			if (SOALogger.DEBUG) {
				logger.debug("In populate Registry URLs are:",
						typeLibclassLoader.getM_classPathURLs());
			}

			Thread.currentThread().setContextClassLoader(typeLibclassLoader);
			final List<RegistryUpdateDetails> libraries = 
				soaTypeRegistry.populateRegistryWithTypeLibrariesDetailed(ListUtil.arrayList(
						typelibNames));
			if (libraries != null) {
				for (RegistryUpdateDetails details : libraries) {
					if (details.isUpdateSucess() == false) {
						logger.warning("Invalid type library->", 
								details.getLibraryName(), ". Detailed Error: ", details.getMessage());
					}
				}
			}
		} finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}

	}

	/**
	 * To make sure that we give user an option to refresh his registry. reason
	 * being there is no extension point in the ebay Build plugin now. Its under
	 * development
	 */
	public void invalidateRegistry() {
		soaTypeRegistry = null;
	}

	/**
	 * Sets up the classpath based on the repo system jar info and lib info.
	 *
	 * @throws Exception the exception
	 */
	public void init() throws Exception {
		typeLibNamesForSOATools = new HashSet<String>();
		typeLibLocationsForSOATools = new HashSet<File>();
		// adding the jars
		ITypeRegistryBridge typeRegistryBridge = GlobalRepositorySystem
				.instanceOf().getActiveRepositorySystem()
				.getTypeRegistryBridge();
		final List<AssetInfo> typeLibs = typeRegistryBridge.getAllLatestTypeLibraries();
		if (typeLibs != null) {
			for (AssetInfo assetInfo : typeLibs) {
				// Filtering the jars if we have a project here in workspace
				if (!WorkspaceUtil.getProject(assetInfo.getName())
						.isAccessible()) {
					// add the typlib Name String
					typeLibNamesForSOATools.add(assetInfo.getName());
					// add the jar files
					Set<File> fileSet = assetInfo.getFiles(true);
					typeLibLocationsForSOATools.addAll(fileSet);
				}
			}
		}

		populateClassLoader();
	}
	
	/**
	 * Refresh type dependency in soa type registry.
	 *
	 * @param typeLibraryName the type library name
	 * @throws CoreException the core exception
	 * @throws Exception the exception
	 */
	public void refreshTypeDependencyInSOATypeRegistry(String typeLibraryName) throws CoreException, Exception{
		populateClassLoader();
		ClassLoader current = Thread.currentThread().getContextClassLoader();
		try{
			Thread.currentThread().setContextClassLoader(typeLibclassLoader);
			GlobalRepositorySystem.instanceOf().getActiveRepositorySystem().getTypeRegistryBridge()
			.processTypeDepXMLFile(typeLibraryName);
		}finally{
			Thread.currentThread().setContextClassLoader(current);
		}
	}

	private void populateClassLoader() throws CoreException, Exception {
		Set<URL> urlsSet = new HashSet<URL>();
		for (File file : typeLibLocationsForSOATools) {
			urlsSet.add(file.toURI().toURL());
		}
		// adding type lib projects in workspace
		for (IProject project : WorkspaceUtil.getAllProjectsInWorkSpace()) {
			if (project.isAccessible()
					&& project.hasNature("com.ebay.tools.soa.typelibrary.TypeLibraryProjectNature")) {
				urlsSet.add(project.getFolder(
						SOATypeLibraryConstants.FOLDER_GEN_META_SRC)
						.getLocation().toFile().toURI().toURL());
				urlsSet.add(project.getFolder(
						SOATypeLibraryConstants.FOLDER_META_SRC).getLocation()
						.toFile().toURI().toURL());
				typeLibNamesForSOATools.add(project.getName());
			}
		}
		if (SOALogger.DEBUG) {
			for (URL url : urlsSet) {
				logger.debug("populate classloader" + url);
			}
		}
		typeLibclassLoader = new SOAPluginClassLoader("SOATools", urlsSet
				.toArray(new URL[0]));

	}
	
	/**
	 * Adds the type to registry.
	 *
	 * @param libraryType the library type
	 * @throws Exception the exception
	 */
	public void addTypeToRegistry(LibraryType libraryType) throws Exception {
		populateClassLoader();
		ClassLoader current = Thread.currentThread().getContextClassLoader();
		try{
			Thread.currentThread().setContextClassLoader(typeLibclassLoader);
			GlobalRepositorySystem.instanceOf().getActiveRepositorySystem().getTypeRegistryBridge()
			.getSOATypeRegistry().addTypeToRegistry(libraryType);
		}finally{
			Thread.currentThread().setContextClassLoader(current);
		}
	}
	
	private class GlobalRegistryJob extends Job {

		public GlobalRegistryJob(String name) {
			super(name);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask(
							"Initializing SOA Type Registry...", 100);
					monitor.internalWorked(10);
					final ClassLoader originalClassLoader = Thread
							.currentThread().getContextClassLoader();
					
					try {
						init();
						monitor.worked(20);
						Thread thread = Thread.currentThread();
						ClassLoader loader = thread.getContextClassLoader();
						thread.setContextClassLoader(SOAGlobalRegistryFactory.class.getClassLoader());
						SOATypeRegistry typeReg = GlobalRepositorySystem
						.instanceOf().getActiveRepositorySystem()
						.getTypeRegistryBridge().getSOATypeRegistry();
						thread.setContextClassLoader(loader);

						monitor.worked(40);
						typeLibclassLoader.setPluginBundles(
								(GlobalRepositorySystem
										.instanceOf().getActiveRepositorySystem()
										.getTypeRegistryBridge().getPluginBundles()));
						monitor.worked(10);
						Thread.currentThread().setContextClassLoader(
								typeLibclassLoader);
						monitor.worked(10);
						List<RegistryUpdateDetails> libraries = 
							typeReg.populateRegistryWithTypeLibrariesDetailed(ListUtil.arrayList(
								typeLibNamesForSOATools));
						if (libraries != null) {
							for (RegistryUpdateDetails details : libraries) {
								if (details.isUpdateSucess() == false) {
									logger.warning("Invalid type library->", 
											details.getLibraryName(), ". Detailed Error: ", details.getMessage());
								}
							}
						}
						monitor.worked(10);
						soaTypeRegistry = typeReg;
					} catch (Exception e) {
						logger.error(e);
						monitor.done();
						return Status.CANCEL_STATUS;
					} finally {
						Thread.currentThread().setContextClassLoader(
								originalClassLoader);
						monitor.done();
					}
			
			return Status.OK_STATUS;
		}
		
	}

}
