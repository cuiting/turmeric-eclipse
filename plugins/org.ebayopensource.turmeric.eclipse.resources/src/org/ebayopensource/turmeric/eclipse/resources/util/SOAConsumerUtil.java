/*******************************************************************************
 * Copyright (c) 2006-2010 eBay Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/
/**
 * 
 */
package org.ebayopensource.turmeric.eclipse.resources.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.ebayopensource.turmeric.eclipse.core.logging.SOALogger;
import org.ebayopensource.turmeric.eclipse.core.resources.constants.SOAProjectConstants;
import org.ebayopensource.turmeric.eclipse.resources.model.AssetInfo;
import org.ebayopensource.turmeric.eclipse.resources.model.ISOAConsumerProject;
import org.ebayopensource.turmeric.eclipse.resources.model.ISOAConsumerProject.SOAClientConfig;
import org.ebayopensource.turmeric.eclipse.resources.model.ISOAConsumerProject.SOAClientEnvironment;
import org.ebayopensource.turmeric.eclipse.resources.model.ISOAProject.SOAProjectSourceDirectory;
import org.ebayopensource.turmeric.eclipse.resources.model.SOAConsumerProject;
import org.ebayopensource.turmeric.eclipse.resources.model.SOAIntfMetadata;
import org.ebayopensource.turmeric.eclipse.utils.collections.ListUtil;
import org.ebayopensource.turmeric.eclipse.utils.io.PropertiesFileUtil;
import org.ebayopensource.turmeric.eclipse.utils.lang.StringUtil;
import org.ebayopensource.turmeric.eclipse.utils.plugin.EclipseMessageUtils;
import org.ebayopensource.turmeric.eclipse.utils.plugin.JDTUtil;
import org.ebayopensource.turmeric.eclipse.utils.plugin.WorkspaceUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.jdom.JDOMException;


/**
 * The Class SOAConsumerUtil.
 *
 * @author yayu
 * @since 1.0.0
 */
public final class SOAConsumerUtil {
	private static final SOALogger logger = SOALogger.getLogger();
	/**
	 * 
	 */
	private SOAConsumerUtil() {
		super();
	}

	/**
	 * Removes the client config files.
	 *
	 * @param project the project
	 * @param monitor the monitor
	 * @param serviceNames the service names
	 * @throws CoreException the core exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void removeClientConfigFiles(final IProject project,
			IProgressMonitor monitor, final String... serviceNames)
			throws CoreException, IOException {
		IFolder parentFolder = project
				.getFolder(SOAConsumerProject.META_SRC_ClIENT_CONFIG);
		if (parentFolder != null && parentFolder.exists()
				&& serviceNames != null) {
			final String clientName = getClientName(project);
			parentFolder = parentFolder.getFolder(clientName);
			if (parentFolder.exists()) {
				for (IResource envResource : parentFolder.members()) {
					if (envResource.exists()) {
						IFolder envFolder = (IFolder)envResource;
						for (final String serviceName : serviceNames) {
							final IFolder folder = envFolder.getFolder(serviceName);
							if (folder != null && folder.exists()) {
								final File file = new File(folder.getLocation().toString());
								FileUtils.deleteDirectory(file);
								// folder.delete(true, new NullProgressMonitor()); TODO
							}
						}
					}
				}
			}
			parentFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}
	}
	
	/**
	 * The Class ModifyConsumerIDResult.
	 */
	public static class ModifyConsumerIDResult {
		
		/** The client configs. */
		final List<SOAClientConfig> clientConfigs = new ArrayList<SOAClientConfig>();
		
		/** The old client configs. */
		final List<SOAClientConfig> oldClientConfigs = new ArrayList<SOAClientConfig>();
		private Properties properties = null;
		
		/**
		 * Gets the client configs.
		 *
		 * @return the client configs
		 */
		public List<SOAClientConfig> getClientConfigs() {
			return clientConfigs;
		}
		
		/**
		 * Gets the old client configs.
		 *
		 * @return the old client configs
		 */
		public List<SOAClientConfig> getOldClientConfigs() {
			return oldClientConfigs;
		}
		
		/**
		 * Gets the properties.
		 *
		 * @return the properties
		 */
		public Properties getProperties() {
			return properties;
		}
	}
	
	/**
	 * Update consumer id.
	 *
	 * @param consumerId the consumer id
	 * @param consumerProject the consumer project
	 * @return the modify consumer id result
	 * @throws CoreException the core exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JDOMException the jDOM exception
	 */
	public static ModifyConsumerIDResult updateConsumerId(String consumerId, IProject consumerProject) 
	throws CoreException, IOException, JDOMException {
		final Properties props = SOAConsumerUtil.loadConsumerProperties(consumerProject);
		final String conId = props.getProperty(SOAProjectConstants.PROPS_KEY_CONSUMER_ID);
		if (consumerId.equals(conId) == false && props.containsKey(SOAProjectConstants.PROPS_KEY_SCPP_VERSION)) {
			//the consumer ID being entered and the version of scpp is new.
			
			final ModifyConsumerIDResult result = new ModifyConsumerIDResult();
			result.properties = props;
			//modify the consumer-id tag in the CC.xml files as well
			for (IFile configFile : SOAConsumerUtil.getClientConfigFiles(consumerProject).values()) {
				final SOAClientConfig clientConfig = SOAClientConfigUtil.parseClientConfig(configFile);
				clientConfig.setConsumerId(consumerId);
				result.clientConfigs.add(clientConfig);
				if (clientConfig.getInvocationUseCase() != null) {
					//old client config
					result.oldClientConfigs.add(clientConfig);
				}
			}
			return result;
		}
		return null;
	}
	
	/**
	 * Removes the environments.
	 *
	 * @param project the project
	 * @param monitor the monitor
	 * @param envrionmentNames the envrionment names
	 * @throws CoreException the core exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void removeEnvironments(final IProject project, 
			IProgressMonitor monitor, final String... envrionmentNames)
	throws CoreException, IOException {
		IFolder parentFolder = project
		.getFolder(SOAConsumerProject.META_SRC_ClIENT_CONFIG);
		if (parentFolder != null && parentFolder.exists()
				&& envrionmentNames != null) {
			final String clientName = getClientName(project);
			if (clientName == null)
				return;
			parentFolder = parentFolder.getFolder(clientName);
			if (parentFolder.exists()) {
				for (final String envName : envrionmentNames) {
					final IFolder folder = parentFolder.getFolder(envName);
					if (folder != null && folder.exists()) {
						final File file = new File(folder.getLocation().toString());
						FileUtils.deleteDirectory(file);
						// folder.delete(true, new NullProgressMonitor()); TODO
					}
				}
			}
			parentFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}
	}
	
	/**
	 * Gets the consumed service list.
	 *
	 * @param project the project
	 * @return the consumed service list
	 * @throws CoreException the core exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static List<String> getConsumedServiceList(IProject project) throws CoreException, IOException {
		final List<String> result = new ArrayList<String>();
		final List<EnvironmentItem> items = SOAConsumerUtil.getClientConfigStructure(project);
		if (items.isEmpty() == false) {
			result.addAll(items.get(0).getServices());
		}
		return result;
	}

	/**
	 * <p>
	 * Get all client config files of the underlying consumer project.
	 * The key would be the environment name, and the values would be the client
	 * config files belong to this enviroment.</p>
	 * 
	 * <p>Note. this method would be able to handle both old and new dir structure for
	 * ClientConfig.xml files. For the old dir structure, the environment name would
	 * be <b>default</b>.</p>
	 *
	 * @param project the project
	 * @return all client configs of the underlying consumer project
	 * @throws CoreException the core exception
	 */
	public static Map<SOAClientEnvironment, IFile> getClientConfigFiles(final IProject project)
			throws CoreException {
		final Map<SOAClientEnvironment, IFile> result = new LinkedHashMap
		<SOAClientEnvironment, IFile>();
		final IFolder folder = getClientConfigClientFolder(project);
		if (folder != null && folder.exists()) {
			if (isOldClientConfigDirStructure(project) == false) {
				//the new client config dir structure
				for (final IResource res : folder.members()) {
					if (res instanceof IFolder) {
						//string environment name
						String envName = res.getName();
						for (IResource clientFolder : ((IFolder)res).members()) {
							if (clientFolder instanceof IFolder &&((IFolder) clientFolder)
									.getFile(ISOAConsumerProject.FILE_ClIENT_CONFIG).isAccessible()) {
								//the sub folder contains valid ClientConfig.xml file
								result.put(new SOAClientEnvironment(envName, clientFolder.getName()), 
										((IFolder) clientFolder)
										.getFile(ISOAConsumerProject.FILE_ClIENT_CONFIG));
							}
						}
					}
				}
			} else {
				//the OLD client config dir structure
				Map<String, String> svcClientMap = new ConcurrentHashMap<String, String>();;
				try {
					svcClientMap = SOAConsumerUtil
					.getMappedServiceNamesFromPropsFile(project);
				} catch (IOException e) {
					throw new CoreException(EclipseMessageUtils.createErrorStatus(e));
				}
				for (final IResource clientFolder : folder.getParent().members()) {
					if (clientFolder instanceof IFolder) {
						//service name
						if (((IFolder) clientFolder)
								.getFile(ISOAConsumerProject.FILE_ClIENT_CONFIG).isAccessible()) {
							//the sub folder contains valid ClientConfig.xml file
							final String clientFolderName = clientFolder.getName();
							final String serviceName = svcClientMap.containsKey(
									clientFolderName) ? 
									svcClientMap.get(clientFolderName) : clientFolderName;
							result.put(new SOAClientEnvironment(SOAProjectConstants.DEFAULT_CLIENT_CONFIG_ENVIRONMENT,
									serviceName), 
									((IFolder) clientFolder)
									.getFile(ISOAConsumerProject.FILE_ClIENT_CONFIG));
						}
					}
				}
			}
		}

		return result;
	}

	/**
	 * Save impl metadata props.
	 *
	 * @param consumerProject the consumer project
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws CoreException the core exception
	 */
	public static void saveImplMetadataProps(
			final SOAConsumerProject consumerProject) throws IOException,
			CoreException {
		Properties properties = loadConsumerProperties(consumerProject.getProject());
		PropertiesFileUtil.writeToFile(properties, consumerProject.getProject()
				.getFile(SOAProjectConstants.PROPS_FILE_SERVICE_CONSUMER),
				SOAProjectConstants.PROPS_COMMENTS);
	}

	/**
	 * Load client configs.
	 *
	 * @param consumerProject the consumer project
	 * @throws Exception the exception
	 */
	public static void loadClientConfigs(
			final SOAConsumerProject consumerProject) throws Exception {
		final Map<SOAClientEnvironment, IFile> clientConfigFiles = getClientConfigFiles(consumerProject
				.getProject());
		final Set<String> serviceNames = new LinkedHashSet<String>();
		for (SOAClientEnvironment cliEnv : clientConfigFiles.keySet()) {
			final IFile configFile = clientConfigFiles.get(cliEnv);
			final SOAClientConfig config = SOAClientConfigUtil
			.parseClientConfig(configFile);
			consumerProject.getClientConfigs().put(cliEnv, config);
			serviceNames.add(cliEnv.getServiceName());
		}
		
		fillMetadata(consumerProject, ListUtil.arrayList(serviceNames));
	}

	/**
	 * Fill metadata.
	 *
	 * @param consumerProject the consumer project
	 * @param serviceNames the service names
	 * @return the sOA consumer project
	 * @throws Exception the exception
	 */
	public static SOAConsumerProject fillMetadata(
			final SOAConsumerProject consumerProject,
			final List<String> serviceNames) throws Exception {
		final List<SOAProjectSourceDirectory> entries = new ArrayList<SOAProjectSourceDirectory>();
		for (final IClasspathEntry entry : JDTUtil.rawClasspath(consumerProject
				.getProject(), false)) {
			if (entry == null
					|| entry.getEntryKind() != IClasspathEntry.CPE_SOURCE)
				continue;
			final String path = entry.getPath().removeFirstSegments(1)
					.toString();
			entries.add(new SOAProjectSourceDirectory(path));
		}
		consumerProject.setSourceDirectories(entries);

		if (serviceNames != null) {
			if (consumerProject instanceof SOAConsumerProject) {
				((SOAConsumerProject) consumerProject).getMetadata()
						.setServiceNames(serviceNames);
			}

			for (final String serviceName : serviceNames) {
				final IProject intfProject = WorkspaceUtil
						.getProject(serviceName);
				if (intfProject != null && intfProject.isAccessible()) {
					SOAIntfMetadata intfMetadata = SOAServiceUtil
							.getSOAIntfMetadata(SOAServiceUtil
									.getSOAEclipseMetadata(intfProject));
					intfMetadata.setServiceName(serviceName);
					SOAIntfUtil.fillMetadata(intfProject, intfMetadata);
					consumerProject.getRequiredServices().put(serviceName,
							intfMetadata);
				}
			}

		}
		return consumerProject;

	}
	
	/**
	 * Gets the client config. 
	 * @param project
	 * @param serviceName
	 * @return
	 * @throws CoreException 
	 * @throws IOException 
	 * @deprecated This is only used for the old dir structure which 
	 * does not have environment in the path. Please use another overloaded 
	 * version which has environmentName as input option.
	 */
	public static IFile getClientConfig(final IProject project,
			final String serviceName) throws CoreException, IOException {
		return getClientConfig(project, null, serviceName);
	}

	/**
	 * Gets the client config.
	 *
	 * @param project the project
	 * @param environmentName null means loading client config from old dir structure
	 * @param serviceName the service name
	 * @return the client config
	 * @throws CoreException the core exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static IFile getClientConfig(final IProject project,
			final String environmentName, final String serviceName) throws CoreException, IOException {
		if (project == null || serviceName == null)
			return null;
		if (StringUtils.isBlank(environmentName)) {
			//loading old client config
			logger.warning("Loading client config from deprecated dir structure->" + project);
			return project.getFile(StringUtil.toString(
					SOAConsumerProject.META_SRC_ClIENT_CONFIG, serviceName, WorkspaceUtil.PATH_SEPERATOR,
					ISOAConsumerProject.FILE_ClIENT_CONFIG));
		} else {
			String clientName = getClientName(project);
			if (StringUtils.isNotBlank(clientName)) {
				return project.getFile(StringUtil.toString(
						SOAConsumerProject.META_SRC_ClIENT_CONFIG, 
						clientName, WorkspaceUtil.PATH_SEPERATOR,
						environmentName, WorkspaceUtil.PATH_SEPERATOR, 
						serviceName, WorkspaceUtil.PATH_SEPERATOR,
						ISOAConsumerProject.FILE_ClIENT_CONFIG));
			}
		}
		return null;
	}

	/**
	 * Load client config.
	 *
	 * @param project the project
	 * @param environmentName the environment name
	 * @param serviceName the service name
	 * @return the sOA client config
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws JDOMException the jDOM exception
	 * @throws CoreException the core exception
	 */
	public static SOAClientConfig loadClientConfig(final IProject project,
			final String environmentName, final String serviceName) 
	throws IOException, JDOMException, CoreException {
		final IFile clientConfigFile = getClientConfig(project, environmentName, serviceName);
		if (clientConfigFile != null && clientConfigFile.isAccessible()) {
			return SOAClientConfigUtil.parseClientConfig(clientConfigFile);
		}
		return null;
	}

	/**
	 * Gets the consumer properties file.
	 *
	 * @param project the project
	 * @return the consumer properties file
	 */
	public static IFile getConsumerPropertiesFile(IProject project) {
		return project.getFile(SOAProjectConstants.PROPS_FILE_SERVICE_CONSUMER);
	}
	
	/**
	 * Load consumer properties.
	 *
	 * @param project the project
	 * @return the properties
	 * @throws CoreException the core exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static Properties loadConsumerProperties(IProject project) 
	throws CoreException, IOException {
		final Properties properties = new Properties();
		InputStream ins = null;
		try {
			ins = getConsumerPropertiesFile(project).getContents();
			properties.load(ins);
		} finally {
			IOUtils.closeQuietly(ins);
		}
		return properties;
	}
	
	/**
	 * Gets the env mapper from consumer properties.
	 *
	 * @param project the project
	 * @return the env mapper from consumer properties
	 * @throws CoreException the core exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static String getEnvMapperFromConsumerProperties(IProject project) 
	throws CoreException, IOException {
		return StringUtils.trim(loadConsumerProperties(project).getProperty(
				SOAProjectConstants.PROPS_ENV_MAPPER));
	}

	/**
	 * This function returns the mapped value. Lets say the client config folder
	 * name is "a" and if a has an entry in the the properties file map,then
	 * that entry is returned otherwise a. the format of the String in the props
	 * file is {[a,b],[d,e]}
	 *
	 * @param project the project
	 * @param clientNames the client names
	 * @return the mapped service names from props file
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws CoreException the core exception
	 */
	public static Map<String, String> getMappedServiceNamesFromPropsFile(
			IProject project, String... clientNames) throws IOException,
			CoreException {
		final Map<String, String> svcClientNameMap = new ConcurrentHashMap<String, String>();
		// This is a hot fix for consumers. Impls wont have this props file
		// This is not a very nice approach. Impls also should have
		// consumer properties if its a consumer.
		IFile consumerPropsFile = getConsumerPropertiesFile(project);
		if (consumerPropsFile != null && consumerPropsFile.isAccessible()) {
			//this should be a consumer project
			final Properties properties = loadConsumerProperties(project);
			Object objMapStr = properties
			.get(SOAProjectConstants.PROPS_KEY_CONSUMER_SVC_CLIENT_NAME_MAPPING);
			if (objMapStr != null && objMapStr instanceof String
					&& StringUtils.isNotEmpty((String) objMapStr)) {
				String strMap = (String) objMapStr;
				strMap = StringUtils.substringBetween(strMap, "{", "}");
				for (String tempStr : StringUtils.substringsBetween(strMap,
						"[", "]")) {
					String[] tempArr = StringUtils.split(tempStr,
							SOAProjectConstants.DELIMITER_COMMA);
					svcClientNameMap.put(tempArr[0], tempArr[1]);
				}
			}
		}
		if (clientNames != null) {
			for (String clientName : clientNames) {
				String svcName = svcClientNameMap.get(clientName);
				if (StringUtils.isEmpty(svcName)) {
					svcClientNameMap.put(clientName, clientName);
				}
			}
		}
		return svcClientNameMap;
	}
	
	/**
	 * Gets the service client name.
	 *
	 * @param project the project
	 * @return the service client name
	 * @throws CoreException the core exception
	 */
	public static String getServiceClientName(IProject project) throws CoreException {
		if (isOldClientConfigDirStructure(project) == true) {
			return project.getName();
		} else {
			final IFolder parentFolder = project.getFolder(SOAConsumerProject.META_SRC_ClIENT_CONFIG);
			if (parentFolder != null && parentFolder.exists() && parentFolder.members().length == 1) {
				//the correct directory structure
				return parentFolder.members()[0].getName();
			} else if (project
					.getFile(SOAProjectConstants.PROPS_FILE_SERVICE_IMPL).isAccessible()){
				//this is an impl project that has not consumed any services yet
				//this is a dirty fix, but we could not get the service name at this point
				final String serviceName = StringUtils.removeEnd(project.getName(), 
						SOAProjectConstants.IMPL_PROJECT_SUFFIX);
				return serviceName + SOAProjectConstants.CLIENT_PROJECT_SUFFIX;
			}
		}
		return project.getName();
	}
	
	/**
	 * Starting from SOA v2.3.0, we have deployed a new multi client config directory strucutre.
	 * The new path format would be <b>meta-src/META-INF/soa/client/config/<client name>/<environment>/<service name>/ClientConfig.xml</b>
	 *
	 * @param project the project
	 * @return True is old client config directory structure or False otherwise
	 * @throws CoreException the core exception
	 * @since 1.0.0
	 */
	public static boolean isOldClientConfigDirStructure(IProject project) throws CoreException {
		final IFolder parentFolder = project.getFolder(SOAConsumerProject.META_SRC_ClIENT_CONFIG);
		if (parentFolder != null && parentFolder.exists()) {
			for (final IResource res : parentFolder.members()) {
				if (res instanceof IFolder) {
					IFile file = ((IFolder) res).getFile(ISOAConsumerProject.FILE_ClIENT_CONFIG);
					if (file.exists() == true) {
						logger.warning("Project ", project, 
								" is using the old deprecated directory structure for client config->", 
								file.getLocation());
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Gets the client config structure.
	 *
	 * @param project the project
	 * @param availableServices the available services
	 * @return the client config structure
	 * @throws CoreException the core exception
	 */
	public static List<EnvironmentItem> getClientConfigStructure(IProject project, 
			List<String> availableServices) throws CoreException{
		final List<EnvironmentItem> result = getClientConfigEnvironments(project, null);
		for (EnvironmentItem item : result) {
			item.setServices(availableServices);
		}
		return result;
	}
	
	/**
	 * Gets the client config structure.
	 *
	 * @param project the project
	 * @return the client config structure
	 * @throws CoreException the core exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static List<EnvironmentItem> getClientConfigStructure(IProject project) 
	throws CoreException, IOException {
		return getClientConfigStructure(project, (IProgressMonitor)null);
	}
	
	/**
	 * Gets the client environment list.
	 *
	 * @param project the project
	 * @param monitor the monitor
	 * @return the client environment list
	 * @throws CoreException the core exception
	 */
	public static List<String> getClientEnvironmentList(IProject project, 
			IProgressMonitor monitor) throws CoreException {
		final List<String> clients = new ArrayList<String>();
		if (project == null)
			return clients;
		final IFolder clientConfigFolder = project
		.getFolder(SOAConsumerProject.META_SRC_ClIENT_CONFIG);
		//lets do a refresh to ensure we can have the latest change,
		//because the user might have modified the file in an external text
		//editor
		clientConfigFolder.refreshLocal(IResource.DEPTH_ONE, monitor);
		if (clientConfigFolder.isAccessible() == false) {
			logger.warning("Client config folder is not accessible->", clientConfigFolder.getLocation());
			return clients;
		}
		
		if (isOldClientConfigDirStructure(project) == true) {
			//the old dir structure
			clients.add(SOAProjectConstants.DEFAULT_CLIENT_CONFIG_ENVIRONMENT);
		} else {
			for (final IResource res : clientConfigFolder.members()) {
				if (res instanceof IFolder) {
					//this should be the client name
					for (final IResource folder : ((IFolder)res).members()) {
						if (folder instanceof IFolder) {
							//this should be the environment
							clients.add(folder.getName());
						}
					}
				}
			}
		}
		return clients;
	}
	
	/**
	 * Gets the client config environments.
	 *
	 * @param project the project
	 * @param monitor the monitor
	 * @return the client config environments
	 * @throws CoreException the core exception
	 */
	public static List<EnvironmentItem> getClientConfigEnvironments(IProject project, 
			IProgressMonitor monitor) 
	throws CoreException {
		final List<EnvironmentItem> clients = new ArrayList<EnvironmentItem>();
		for (String envName : getClientEnvironmentList(project, monitor)) {
			clients.add(new EnvironmentItem(envName));
		}
		return clients;
	}
	
	/**
	 * Gets the client name.
	 *
	 * @param project the project
	 * @return the client name
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws CoreException the core exception
	 */
	public static String getClientName(IProject project) throws IOException, CoreException {
		final IFile file = getConsumerPropertiesFile(project);
		//the default name is same as the project name
		String clientName = project.getName();
		if (file.exists() == true) {
			final Properties props = loadConsumerProperties(project);
			clientName = StringUtils.trim(props.getProperty(SOAProjectConstants.PROPS_KEY_CLIENT_NAME, clientName));
		} else {
			logger.warning(
					"service_consumer_project.properties file is missing, so use the project name instead->", 
					project.getName());
		}
		return clientName;
	}
	
	public static boolean isZeroConfigEnabled(IProject project) throws IOException, CoreException {
		final IFile file = getConsumerPropertiesFile(project);
		//the default name is same as the project name
		boolean isZeroConfig = Boolean.FALSE;
		if (file.exists() == true) {
			final Properties props = loadConsumerProperties(project);
			isZeroConfig = Boolean.valueOf(StringUtils.trim(
					props.getProperty(SOAProjectConstants.PROPS_SUPPORT_ZERO_CONFIG, 
							Boolean.FALSE.toString())));
		} else {
			logger.warning(
			"service_consumer_project.properties file is missing, so use the support_zero_config=false instead");
		}
		return isZeroConfig;

	}

	/**
	 * Gets the client config client folder.
	 *
	 * @param project the project
	 * @return the client config client folder
	 * @throws CoreException the core exception
	 */
	public static IFolder getClientConfigClientFolder(IProject project) throws CoreException {
		final IFolder clientConfigFolder = project
		.getFolder(SOAConsumerProject.META_SRC_ClIENT_CONFIG);
		//lets do a refresh to ensure we can have the latest change,
		//because the user might have modified the file in an external text
		//editor
		clientConfigFolder.refreshLocal(IResource.DEPTH_ONE, null);
		if (clientConfigFolder.isAccessible() == false) {
			logger.warning("Client config folder is not accessible->", clientConfigFolder.getLocation());
			return null;
		}

		for (final IResource res : clientConfigFolder.members()) {
			if (res instanceof IFolder) {
				//this should be the client name
				return (IFolder)res;
			}
		}
		return null;
	}
	
	/**
	 * Clone environment.
	 *
	 * @param project the project
	 * @param existingEnvName the existing env name
	 * @param newEnvName the new env name
	 * @param monitor the monitor
	 * @throws CoreException the core exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void cloneEnvironment(IProject project, String existingEnvName, 
			String newEnvName, IProgressMonitor monitor) throws CoreException, IOException {
		final IFolder clientFolder = getClientConfigClientFolder(project);
		if (clientFolder != null) {
			IFolder srcFolder = clientFolder.getFolder(existingEnvName);
			if (srcFolder.exists()) {
				IFolder targetFolder = clientFolder.getFolder(newEnvName);
				FileUtils.copyDirectory(srcFolder.getLocation().toFile(), 
						targetFolder.getLocation().toFile(), true);
				monitor.worked(10);
				clientFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			}
		}
	}
	
	/**
	 * Add an admin name into the consumer_project.props file to indicate that the base
	 * consumer should not be generated in the consumer project.
	 *
	 * @param project the project
	 * @param addedAdminNames the added admin names
	 * @param removedAdminNames the removed admin names
	 * @param monitor the monitor
	 * @throws CoreException the core exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void modifyNotGenerateBaseConsumers(final IProject project, 
			final Collection<String> addedAdminNames, final Collection<String> removedAdminNames, 
			IProgressMonitor monitor) throws CoreException, IOException {
		final Properties props = loadConsumerProperties(project);
		final List<String> adminNames = ListUtil.arrayList(StringUtils.split(
				props.getProperty(SOAProjectConstants.PROPS_NOT_GENERATE_BASE_CONSUMER, ""), 
				SOAProjectConstants.DELIMITER_COMMA));
		for (String adminName : addedAdminNames) {
			if (adminNames.contains(adminName) == false) {
				adminNames.add(adminName);
			} else {
				logger.warning(
						"Admin name already exist in the not_genreate_base_consumer propery->", 
						adminName);
			}
		}
		
		for (String adminName : removedAdminNames) {
			if (adminNames.contains(adminName) == true) {
				adminNames.remove(adminName);
			} else {
				logger.debug(
						"The admin name does not exist in the not_genreate_base_consumer property->", 
						adminName);
			}
		}
		
		props.setProperty(SOAProjectConstants.PROPS_NOT_GENERATE_BASE_CONSUMER, 
				StringUtils.join(adminNames, SOAProjectConstants.DELIMITER_COMMA));
		savePropsFileForConsumer(project, props, monitor);
	}
	
	/**
	 * Save props file for consumer.
	 *
	 * @param project the project
	 * @param props the props
	 * @param monitor the monitor
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws CoreException the core exception
	 */
	public static void savePropsFileForConsumer(IProject project, final Properties props, 
			IProgressMonitor monitor) throws IOException, CoreException {
		final IFile file = SOAConsumerUtil.getConsumerPropertiesFile(project);
		OutputStream output = null;
		try {
			output = new ByteArrayOutputStream();
			props.store(output, SOAProjectConstants.PROPS_COMMENTS);
			WorkspaceUtil.writeToFile(output.toString(), file, monitor);
		} finally {
			IOUtils.closeQuietly(output);
		}
	}
	
	/**
	 * Gets the client config structure.
	 *
	 * @param project the project
	 * @param monitor the monitor
	 * @return the client config structure
	 * @throws CoreException the core exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static List<EnvironmentItem> getClientConfigStructure(IProject project, 
			IProgressMonitor monitor) 
	throws CoreException, IOException {
		final List<EnvironmentItem> clients = getClientConfigEnvironments(project, monitor);
		if (clients.isEmpty())
			return clients;
		
		IFolder clientConfigFolder = project
		.getFolder(SOAConsumerProject.META_SRC_ClIENT_CONFIG);
		
		if (isOldClientConfigDirStructure(project) == true) {
			//the old dir structure
			EnvironmentItem item = clients.get(0);
			for (final IResource res : clientConfigFolder.members()) {
				if (res instanceof IFolder) {
					// each service should have a corresponding folder in here
					// using its service name
					String svcName = res.getName();
					item.addService(svcName);
				}
			}
		} else {
			final String clientName = getClientName(project);
			clientConfigFolder = clientConfigFolder.getFolder(clientName);
			if (clientConfigFolder.exists() == false) {
				logger.warning("Client Config folder is missing->", clientConfigFolder.getLocation());
				return clients;
			}
			for (final EnvironmentItem item : clients) {
				for (final IResource child : clientConfigFolder.
						getFolder(item.getName()).members()) {
					//this should be the service names
					if (child instanceof IFolder) {
						final IFolder folder = (IFolder) child;
						final IFile clientFile = folder.getFile(
								ISOAConsumerProject.FILE_ClIENT_CONFIG);
						if (clientFile.isAccessible() == true) {
							item.addService(folder.getName());
							if (clients.contains(item) == false) {
								//we only add the environment if and 
								//only if it contains valid ClientConfig.xml files
								clients.add(item);
							}
						}
					}
				}
			}
		}
		return clients;
	}
	
	/**
	 * The Class EnvironmentItem.
	 */
	public static class EnvironmentItem {
		private String name;
		private List<String> services = new ArrayList<String>();
		private Map<String, AssetInfo> serviceData = new ConcurrentHashMap<String, AssetInfo>();
		
		/**
		 * Instantiates a new environment item.
		 *
		 * @param name the name
		 */
		public EnvironmentItem(String name) {
			super();
			this.name = name;
		}
		
		/**
		 * Gets the name.
		 *
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		
		/**
		 * Sets the name.
		 *
		 * @param name the new name
		 */
		public void setName(String name) {
			this.name = name;
		}
		
		/**
		 * Gets the services.
		 *
		 * @return the services
		 */
		public List<String> getServices() {
			return services;
		}
		
		/**
		 * Removes the service.
		 *
		 * @param service the service
		 */
		public void removeService(String service) {
			services.remove(service);
			serviceData.remove(service);
		}
		
		/**
		 * Adds the service.
		 *
		 * @param service the service
		 * @return true, if successful
		 */
		public boolean addService(String service) {
			return services.add(service);
		}
		
		/**
		 * Sets the services.
		 *
		 * @param services the new services
		 */
		public void setServices(List<String> services) {
			this.services = services;
		}
		
		/**
		 * Gets the service data.
		 *
		 * @param serviceName the service name
		 * @return the service data
		 */
		public AssetInfo getServiceData(String serviceName) {
			return serviceData.get(serviceName);
		}
		
		/**
		 * Gets the service data.
		 *
		 * @return the service data
		 */
		public Map<String, AssetInfo> getServiceData() {
			return serviceData;
		}
		
		/**
		 * Sets the service data.
		 *
		 * @param serviceData the service data
		 */
		public void setServiceData(Map<String, AssetInfo> serviceData) {
			this.serviceData = serviceData;
		}
		
		/**
		 * Adds the service data.
		 *
		 * @param svcData the svc data
		 */
		public void addServiceData(AssetInfo svcData) {
			if (services.contains(svcData.getName()) == false)
				services.add(svcData.getName());
			
			serviceData.put(svcData.getName(), svcData);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return this.name;
		}
	}
}
