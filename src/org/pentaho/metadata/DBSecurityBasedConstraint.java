package org.pentaho.metadata;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.commons.lang.builder.HashCodeBuilder;

@Entity
public class DBSecurityBasedConstraint implements Serializable {

	private static final long serialVersionUID = 6704030226936887937L;
	
	@Id
	private String queryModel;
	@Id
	private String tableName;
	@Id
	private String sessionVar;
	private String whereClause;
	
	public void setQueryModel(String iQueryModel) {
		queryModel=iQueryModel;
	}
	
	public String getQueryModel() {
		return queryModel;
	}
	
	public void setTableName(String iTableName) {
		tableName=iTableName;
	}
	
	public String getTableName() {
		return tableName;
	}
	
	public void setSessionVar(String iSessionVar) {
		sessionVar=iSessionVar;
	}
	
	public String getSessionVar() {
		return sessionVar;
	}
	
	public void setWhereClause(String iWhereClause) {
		whereClause=iWhereClause;
	}
	
	public String getWhereClause() {
		return whereClause;
	}
	
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if((obj == null) || (obj.getClass() != this.getClass()))
			return false;
		// object must be Test at this point
		DBSecurityBasedConstraint sc = (DBSecurityBasedConstraint)obj;
			return (this.queryModel.equals(sc.queryModel) &&
					this.tableName.equals(sc.tableName) &&
					this.sessionVar.equals(sc.sessionVar));
		}

	public int hashCode () {
		return new HashCodeBuilder().
				append(getQueryModel()).
				append(getTableName()).
				append(getSessionVar()).
				toHashCode();
	}
}
