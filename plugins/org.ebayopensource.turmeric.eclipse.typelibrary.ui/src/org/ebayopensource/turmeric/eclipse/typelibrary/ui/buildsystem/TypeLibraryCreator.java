/*******************************************************************************
 * Copyright (c) 2006-2010 eBay Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/
package org.ebayopensource.turmeric.eclipse.typelibrary.ui.buildsystem;

import java.util.HashSet;
import java.util.Set;

import org.ebayopensource.turmeric.eclipse.buildsystem.TypeDepMarshaller;
import org.ebayopensource.turmeric.eclipse.buildsystem.core.TypeLibraryBuildSystemConfigurer;
import org.ebayopensource.turmeric.eclipse.buildsystem.services.SOAResourceCreator;
import org.ebayopensource.turmeric.eclipse.buildsystem.utils.ProjectPropertiesFileUtil;
import org.ebayopensource.turmeric.eclipse.codegen.utils.CodegenInvoker;
import org.ebayopensource.turmeric.eclipse.core.model.typelibrary.TypeLibraryParamModel;
import org.ebayopensource.turmeric.eclipse.core.resources.constants.SOAProjectConstants.SupportedProjectType;
import org.ebayopensource.turmeric.eclipse.repositorysystem.core.GlobalRepositorySystem;
import org.ebayopensource.turmeric.eclipse.repositorysystem.core.SOAGlobalRegistryAdapter;
import org.ebayopensource.turmeric.eclipse.resources.model.SOAProjectEclipseMetadata;
import org.ebayopensource.turmeric.eclipse.typelibrary.builders.TypeLibraryBuilderUtils;
import org.ebayopensource.turmeric.eclipse.typelibrary.codegen.model.GenTypeCreateTypeLibrary;
import org.ebayopensource.turmeric.eclipse.typelibrary.resources.model.SOATypeLibraryMetadata;
import org.ebayopensource.turmeric.eclipse.typelibrary.resources.model.SOATypeLibraryProject;
import org.ebayopensource.turmeric.eclipse.typelibrary.utils.TypeLibraryUtil;
import org.ebayopensource.turmeric.eclipse.utils.plugin.ProgressUtil;
import org.ebayopensource.turmeric.eclipse.utils.plugin.WorkspaceUtil;
import org.ebayopensource.turmeric.tools.library.SOATypeRegistry;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;


/**
 * Type Library Creation Point.
 * 
 * @author smathew
 */
public class TypeLibraryCreator {

	/**
	 * Creates the type library.
	 *
	 * @param model the model
	 * @param monitor the monitor
	 * @throws Exception the exception
	 */
	public static void createTypeLibrary(TypeLibraryParamModel model,
			IProgressMonitor monitor) throws Exception {

		// Eclipse Metadata
		SOAProjectEclipseMetadata eclipseMetadata = SOAProjectEclipseMetadata
				.create(model.getTypeLibraryName(), model.getWorkspaceRoot());

		// Creates the SOA related metadata
		SOATypeLibraryMetadata metadata = SOATypeLibraryMetadata.create(model);
		ProgressUtil.progressOneStep(monitor);

		// Creating SOA Type Library Project
		SOATypeLibraryProject typeLibraryProject = SOATypeLibraryProject
				.createSOAProjectTypeLibrary(eclipseMetadata, metadata);

		Set<String> requiredLibraries = new HashSet<String>();
		requiredLibraries.addAll(GlobalRepositorySystem.instanceOf().getActiveRepositorySystem()
				.getActiveOrganizationProvider().getDefaultDependencies(SupportedProjectType.TYPE_LIBRARY));

		typeLibraryProject.setRequiredLibraries(requiredLibraries);

		// Project Creation starts here
		IProject project = SOAResourceCreator.createProject(
				eclipseMetadata, monitor);
		// Folder Creation
		SOAResourceCreator.createFolders(project, typeLibraryProject, monitor);
		//Creates the pref file for RIDE
		ProjectPropertiesFileUtil.createPrefsFile(typeLibraryProject.getProject(), monitor);
		// Create the type reference xml.
		TypeDepMarshaller.createDefaultDepXml(typeLibraryProject, monitor);

		// call BuildSystem
		GlobalRepositorySystem.instanceOf().getActiveRepositorySystem()
				.getProjectConfigurer().initializeTypeLibProject(
						typeLibraryProject, metadata.getVersion(), monitor);

		// Configure
		TypeLibraryBuildSystemConfigurer.configure(typeLibraryProject, monitor);

		// CodegenCall.
		callCodegen(project, model);
		WorkspaceUtil.refresh(project);

		SOAGlobalRegistryAdapter registryAdapter = SOAGlobalRegistryAdapter.getInstance();
		SOATypeRegistry typeRegistry = registryAdapter.getGlobalRegistry();
		if (typeRegistry == null) {
			typeRegistry = registryAdapter.getGlobalRegistry();
		}
		typeRegistry.addTypeLibraryToRegistry(
				TypeLibraryUtil.getTypeLibraryType(project));

	}

	private static void callCodegen(IProject project,
			TypeLibraryParamModel typeLibraryModel) throws Exception {
		GenTypeCreateTypeLibrary genTypeCreateTypeLibrary = new GenTypeCreateTypeLibrary();
		genTypeCreateTypeLibrary.setProjectRoot(project.getLocation()
				.toString());
		genTypeCreateTypeLibrary.setLibraryName(typeLibraryModel
				.getTypeLibraryName());
		genTypeCreateTypeLibrary.setLibraryVersion(typeLibraryModel
				.getVersion());
		genTypeCreateTypeLibrary.setLibraryCategory(typeLibraryModel
				.getCategory());
		genTypeCreateTypeLibrary.setLibNamespace(typeLibraryModel.getNamespace());
		CodegenInvoker codegenInvoker = TypeLibraryBuilderUtils
				.initForTypeLib(project);
		codegenInvoker.execute(genTypeCreateTypeLibrary);

	}

}
