package net.mafiascum.jdbc;

import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import net.mafiascum.util.QueryUtil;
import net.mafiascum.web.misc.DataObjectWithIntID;
import net.mafiascum.web.misc.DataObjectWithLongID;
import net.mafiascum.web.misc.DataObjectWithShortID;

public class StoreDataObjectSQLBuilder extends StoreSQLBuilder {

  public StoreDataObjectSQLBuilder(String tableName) {
    
    super(tableName);
  }
  
private static final Logger logger = Logger.getLogger(StoreDataObjectSQLBuilder.class.getName());
  public void execute(Statement statement, IsNewDataObject dataObject) throws SQLException {
    logger.info("Query: " + (dataObject.isNew() ? generateInsert() : generateUpdate()));
    statement.executeUpdate(dataObject.isNew() ? generateInsert() : generateUpdate());
  }
  
  public int execute(Statement statement, DataObjectWithShortID dataObject) throws SQLException {
    
    if(dataObject.isNew()) {
      
      statement.executeUpdate(generateInsert());
      dataObject.setId((short)QueryUtil.get().getLastInsertedID(statement));
    }
    else
      statement.executeUpdate(generateUpdate());
    
    return dataObject.getId();
  }
  
  public int execute(Statement statement, DataObjectWithIntID dataObject) throws SQLException {
    
    if(dataObject.isNew()) {
      
      statement.executeUpdate(generateInsert());
      dataObject.setId(QueryUtil.get().getLastInsertedID(statement));
    }
    else
      statement.executeUpdate(generateUpdate());
    
    return dataObject.getId();
  }
  
  public long execute(Statement statement, DataObjectWithLongID dataObject) throws SQLException {
    
    if(dataObject.isNew()) {
      
      statement.executeUpdate(generateInsert());
      dataObject.setId(QueryUtil.get().getLastInsertedLongID(statement));
    }
    else
      statement.executeUpdate(generateUpdate());
    
    return dataObject.getId();
  }
}
