/*******************************************************************************
 * Copyright (c) 2006-2010 eBay Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/
package org.ebayopensource.turmeric.eclipse.errorlibrary.properties;

import org.ebayopensource.turmeric.eclipse.core.logging.SOALogger;
import org.ebayopensource.turmeric.eclipse.utils.plugin.JDTUtil;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;



/**
 * @author yayu
 * @since 1.0.0
 */
public class Activator extends Plugin {
	/**
	 * 
	 */
	public static final String PLUGIN_ID = "org.ebayopensource.turmeric.eclipse.errorlibrary.properties";

	// The shared instance
	private static Activator plugin;

	/**
	 * {@inheritDoc}
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		StringBuffer buf = new StringBuffer();
    	buf.append("SOAPlugin.start - ");
    	buf.append(JDTUtil.getBundleInfo(context.getBundle(), SOALogger.DEBUG));
        SOALogger.getLogger().info(buf);
	}

	/**
	 * {@inheritDoc}
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}
	
	/**
	 * Returns the shared instance.
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
}
