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
package org.ebayopensource.turmeric.eclipse.resources.model;


/**
 * The Class AbstractSOAMetadata.
 *
 * @author yayu
 */
public abstract class AbstractSOAMetadata {

	/**
	 * Instantiates a new abstract soa metadata.
	 */
	public AbstractSOAMetadata() {
		super();
	}
	
	//	public abstract IFile getMetadataFile();

	/**
	 * Gets the metadata file name.
	 *
	 * @return the MetaDataFileName as a string.
	 */
	public abstract String getMetadataFileName();
	public abstract String getSuperVersion();

}
