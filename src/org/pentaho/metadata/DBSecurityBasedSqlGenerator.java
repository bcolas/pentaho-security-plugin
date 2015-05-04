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
 
package org.pentaho.metadata;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.Session; 
import org.hibernate.Transaction;
import org.pentaho.metadata.query.impl.sql.*;
import org.pentaho.pms.core.exception.PentahoMetadataException;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.pms.mql.dialect.SQLQueryModel;
import org.pentaho.metadata.query.model.Constraint;
import org.pentaho.metadata.query.model.Selection;
import org.pentaho.metadata.query.model.Order;
import org.pentaho.metadata.model.LogicalColumn;
import org.pentaho.metadata.model.LogicalModel;
import org.pentaho.metadata.model.LogicalTable;
import org.pentaho.di.core.database.DatabaseMeta; // kettle-db jar file
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;

public class DBSecurityBasedSqlGenerator extends SqlGenerator {
	private static final Logger LOGGER = Logger.getLogger(DBSecurityBasedSqlGenerator.class);
	
	private LogicalModel _Model;

	private static Hashtable<String, Hashtable> _SecurityConstraints = null;
	
	static {
		Transaction tx = null;
		try {
			_SecurityConstraints = new Hashtable<String, Hashtable>();
			Configuration config = new Configuration().configure();
			SessionFactory sessionFactory = config.buildSessionFactory();
			Session session = sessionFactory.getCurrentSession();
			tx = session.beginTransaction();
			List<DBSecurityBasedConstraint> list = session.createQuery("from DBSecurityBasedConstraint").list(); 
			Iterator<DBSecurityBasedConstraint> it = list.iterator(); 
			while(it.hasNext()) 
			{ 
				DBSecurityBasedConstraint scItem = (DBSecurityBasedConstraint)it.next(); 
				LOGGER.debug("load Security Constraints "+scItem.getQueryModel()+" - " 
														+ scItem.getTableName()+" - "
														+ scItem.getSessionVar()+" - "
														+ scItem.getWhereClause());
				if (_SecurityConstraints.containsKey(scItem.getQueryModel())) {
					Hashtable<String, Hashtable> h1 = _SecurityConstraints.get(scItem.getQueryModel());
					if (h1.containsKey(scItem.getTableName())) {
						Hashtable<String, String> h2 = h1.get(scItem.getTableName());
						h2.put(scItem.getWhereClause(), scItem.getSessionVar());
					}
					else {
						Hashtable<String, String> h2 = new Hashtable<String, String>();
						h2.put(scItem.getWhereClause(), scItem.getSessionVar());
						h1.put(scItem.getTableName(), h2);
					}
				}
				else
				{
					Hashtable<String, Hashtable> h1 = new Hashtable<String, Hashtable>();
					Hashtable<String, String> h2 = new Hashtable<String, String>();
					h2.put(scItem.getWhereClause(), scItem.getSessionVar());
					h1.put(scItem.getTableName(), h2);
					_SecurityConstraints.put(scItem.getQueryModel(), h1);
				}
				
			} 
			tx.commit();
		}
		catch (Throwable ex) {
			if (tx!=null) tx.rollback();
			LOGGER.error("Initialization failed." + ex); 
		} 
	}
	
	@Override
	protected MappedQuery getSQL(LogicalModel model, 
								List<Selection> selections, 
								List<Constraint> conditions, 
								List<Order> orderBy, 
								DatabaseMeta databaseMeta, 
								String locale,
								Map<String, Object> parameters,
								boolean genAsPreparedStatement,
								boolean disableDistinct, 
								int limit,
								Constraint securityConstraint) throws PentahoMetadataException {
		_Model = model;
		LOGGER.debug(">> Model : "+_Model.getId());
		return super.getSQL(model, selections, conditions, orderBy, databaseMeta, locale, parameters, genAsPreparedStatement, disableDistinct, limit, securityConstraint);
	}

	@Override
	protected void preprocessQueryModel(
			SQLQueryModel query,
			List<Selection> selections, 
			Map<LogicalTable, String> tableAliases,
			DatabaseMeta databaseMeta) {

		Set<LogicalTable> selectedLogicalTables = new HashSet<LogicalTable>();
		
		// Get the user's DOSSIER & LANGUE from the session
		IPentahoSession session = PentahoSessionHolder.getSession();
		if (session == null){
			LOGGER.debug("In preprocessQueryModel -- no session!!!!!");
		}
		
		LOGGER.debug("Model : "+_Model.getId());
		if (_SecurityConstraints.containsKey(_Model.getId())) {
			Hashtable lTableSecurityModel = (Hashtable) _SecurityConstraints.get(_Model.getId());
			
			if (selections != null && !selections.isEmpty()) {
				for (Selection selection : selections) {
					LogicalColumn column = selection.getLogicalColumn();
					LogicalTable table = column.getLogicalTable();
					//selectedLogicalTables.add(table);
					
					if (lTableSecurityModel.containsKey(table.getId())) {
						Hashtable lWhereClauses = (Hashtable) lTableSecurityModel.get(table.getId());
						Enumeration<String> keys = lWhereClauses.keys();
						while (keys.hasMoreElements()) {
							String lWhereClause=keys.nextElement();
							String lSessionVar = (String) lWhereClauses.get(lWhereClause);
							String lSessionValue = (String) session.getAttribute(lSessionVar);
							if (lSessionValue != null)
								query.addWhereFormula(lWhereClause.replace("%"+lSessionVar+"%", lSessionValue), "AND");
						}
					}
				}
			}
		}
	}

	@Override
	protected String processGeneratedSql(String sql) {
		LOGGER.debug("processGeneratedSql SQL is:"+ sql);
		return super.processGeneratedSql(sql);
	}
	
	
	
}
