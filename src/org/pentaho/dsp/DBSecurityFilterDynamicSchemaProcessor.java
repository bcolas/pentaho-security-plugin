/**
 * COPYRIGHT (C) 2014 Pentaho. All Rights Reserved.
 * THE SOFTWARE PROVIDED IN THIS SAMPLE IS PROVIDED "AS IS" AND PENTAHO AND ITS 
 * LICENSOR MAKE NO WARRANTIES, WHETHER EXPRESS, IMPLIED, OR STATUTORY REGARDING 
 * OR RELATING TO THE SOFTWARE, ITS DOCUMENTATION OR ANY MATERIALS PROVIDED BY 
 * PENTAHO TO LICENSEE.  PENTAHO AND ITS LICENSORS DO NOT WARRANT THAT THE 
 * SOFTWARE WILL OPERATE UNINTERRUPTED OR THAT THEY WILL BE FREE FROM DEFECTS OR 
 * THAT THE SOFTWARE IS DESIGNED TO MEET LICENSEE'S BUSINESS REQUIREMENTS.  PENTAHO 
 * AND ITS LICENSORS HEREBY DISCLAIM ALL OTHER WARRANTIES, INCLUDING, WITHOUT 
 * LIMITATION, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE, TITLE AND NONINFRINGMENT.  IN ADDITION, THERE IS NO MAINTENANCE OR SUPPORT 
 * INCLUDED WITH THIS SAMPLE OF ANY NATURE WHATSOEVER, INCLUDING, BUT NOT LIMITED TO, 
 * HELP-DESK SERVICES. 
 */
package org.pentaho.dsp;

import java.io.InputStream;
import java.lang.String;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import mondrian.olap.Util;
import mondrian.spi.impl.FilterDynamicSchemaProcessor;

import org.pentaho.platform.api.engine.*;          // This is for IPentahoSession (-api jar)
import org.pentaho.platform.engine.core.system.*;  // This is for PentahoSessionHolder (-core jar)
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;


public class DBSecurityFilterDynamicSchemaProcessor extends FilterDynamicSchemaProcessor {
	private static final Logger LOGGER = Logger.getLogger(DBSecurityFilterDynamicSchemaProcessor.class);
	
	private static Hashtable<String, String> _SecurityFilters = null;
	
	static {
		Transaction tx = null;
		_SecurityFilters = new Hashtable<String, String>();
		try {
			Configuration config = new Configuration().configure();
			SessionFactory sessionFactory = config.buildSessionFactory();
			Session session = sessionFactory.getCurrentSession();
			tx = session.beginTransaction();
			List<DBSecurityBasedFilter> list = session.createQuery("from DBSecurityBasedFilter").list(); 
			Iterator<DBSecurityBasedFilter> it = list.iterator(); 
			while(it.hasNext()) 
			{ 
				DBSecurityBasedFilter dspItem = (DBSecurityBasedFilter)it.next(); 
				LOGGER.debug("load DSP item "+dspItem.getParamVar()+" - " + dspItem.getSessionVar());
				_SecurityFilters.put(dspItem.getParamVar(), dspItem.getSessionVar());
			} 
			tx.commit();
		}
		catch (Throwable ex) { 
			if (tx != null) tx.rollback();
			LOGGER.error("Initialization failed." + ex); 
		} 
	}

	@Override public String filter(String schemaUrl, Util.PropertyList connectInfo, InputStream stream) throws Exception {
		String schema = super.filter(schemaUrl, connectInfo, stream);

		IPentahoSession session = PentahoSessionHolder.getSession();
		
		Matcher matcher = Pattern.compile("STEF#([^#]*)#").matcher(schema);
		StringBuffer newSchema = new StringBuffer();
		Enumeration<String> keys = _SecurityFilters.keys();
		if (_SecurityFilters.size()>0) {
			while (matcher.find()) {
				String clause = matcher.group(1);
				while (keys.hasMoreElements()) {
					String key=keys.nextElement();
					String value=_SecurityFilters.get(key);
					LOGGER.debug("replace "+key+" with " + session.getAttribute(value));
				
					matcher.appendReplacement(newSchema, clause.replaceAll(key, (String) session.getAttribute(value)));
				}
			}
			return matcher.appendTail(newSchema).toString();
		}
		else {
			LOGGER.debug("Full access - strip MT clause from schema");
			return schema.replaceAll("STEF#[^#]*#", "1=1");
		}
	}
}
