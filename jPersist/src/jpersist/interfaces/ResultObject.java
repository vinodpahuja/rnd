/**
 * Copyright (C) 2006 - present Software Sensation Inc.  
 * All Rights Reserved.
 *
 * This file is part of jPersist.
 *
 * jPersist is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the accompanying license 
 * for more details.
 *
 * You should have received a copy of the license along with jPersist; if not, 
 * go to http://www.softwaresensation.com and download the latest version.
 */

package jpersist.interfaces;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import jpersist.Database;
import jpersist.JPersistException;
import jpersist.Result;

/**
 * When an interface is being used as a proxy to a database (also known as database to interface casting), 
 * this interface can be extended by the interface to include jpersist.Database functionality.
 */

public interface ResultObject<T> extends ListIterator<T>, Iterable<T>
  {
    void afterLast() throws JPersistException;
    void beforeFirst() throws JPersistException;
    Object castToInterface(Class interfaceClass, Object... handlers) throws JPersistException;
    void close() throws JPersistException;
    void deleteRow() throws JPersistException;
    boolean first() throws JPersistException;
    String getClose() throws JPersistException;
    <C> C getColumnValue(String columnName) throws JPersistException;
    <C> C getColumnValue(Class<C> returnType, String columnName) throws JPersistException;
    <C> C getColumnValue(int columnIndex) throws JPersistException;
    <C> C getColumnValue(Class<C> returnType, int columnIndex) throws JPersistException;
    Database getDatabase() throws JPersistException;
    boolean getMoreResults() throws JPersistException;
    boolean getMoreResults(int doWhatWithCurrent) throws JPersistException;
    ResultSet getResultSet() throws JPersistException;
    Statement getStatement() throws JPersistException;
    boolean hasNext();
    boolean hasPrevious();
    void insertRow() throws JPersistException;
    boolean isAfterLast() throws JPersistException;
    boolean isBeforeFirst() throws JPersistException;
    boolean isClosed();
    boolean isFirst() throws JPersistException;
    boolean isLast() throws JPersistException;
    Iterator<T> iterator();
    boolean last() throws JPersistException;
    void loadAssociations(Object object) throws JPersistException;
    <C> C loadObject(Class<C> cs) throws JPersistException;
    <C> C loadObject(Class<C> cs, boolean loadAssociations) throws JPersistException;
    <C> C loadObject(C object) throws JPersistException;
    <C> C loadObject(C object, boolean loadAssociations) throws JPersistException;
    <C> Collection<C> loadObjects(Collection<C> collection, Class<C> cs) throws JPersistException;
    <C> Collection<C> loadObjects(Collection<C> collection, Class<C> cs, boolean loadAssociations) throws JPersistException;
    void moveToCurrentRow() throws JPersistException;
    void moveToInsertRow() throws JPersistException;
    T next();
    T next(boolean loadAssociations);
    <C> C next(C object);
    <C> C next(C object, boolean loadAssociations);
    int nextIndex();
    T previous();
    T previous(boolean loadAssociations);
    <C> C previous(C object);
    <C> C previous(C object, boolean loadAssociations);
    int previousIndex();
    void refreshRow() throws JPersistException;
    void remove();
    boolean rowDeleted() throws JPersistException;
    boolean rowInserted() throws JPersistException;
    boolean rowUpdated() throws JPersistException;
    Result setClass(Class<T> cs);
    void setClosed(boolean true_only) throws JPersistException;
    void setColumnValue(String columnName, Object object) throws JPersistException;
    void setColumnValue(int columnIndex, Object object) throws JPersistException;
    void setFetchDirection(int direction) throws JPersistException;
    void updateRow() throws JPersistException;
  }