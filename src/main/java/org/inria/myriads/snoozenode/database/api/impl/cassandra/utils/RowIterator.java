package org.inria.myriads.snoozenode.database.api.impl.cassandra.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import me.prettyprint.cassandra.model.IndexedSlicesQuery;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Row Iterator.
 * 
 * @author msimonin
 *
 */
public class RowIterator implements Iterable<Row<String, String, String>>
{
    
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(RowIterator.class);
    
    /** internal iterator.*/
    private MyQueryIterator queryIterator_;
    
    /**
     * Constructor.
     */
    public RowIterator()
    {
        queryIterator_ = new MyQueryIterator();
    }
    
    /**
     * execute the request.
     */
    public void execute()
    {
        queryIterator_.oneStep();
    }

    /**
     * 
     * Sets the keyspace.
     * 
     * @param keyspace      Keyspace.
     * @return  this.
     */
    public RowIterator setKeyspace(Keyspace keyspace)
    {
        queryIterator_.setKeyspace(keyspace);
        return this;
    }

    /**
     * 
     * Sets the column family.
     * 
     * @param columnFamily  column family.
     * @return  this.
     */
    public RowIterator setColumnFamily(String columnFamily)
    {
        queryIterator_.setColumnFamily(columnFamily);
        return this;
    }

    /**
     * 
     * Sets keys.
     *
     * @param start     key start.
     * @param end       key end.
     * @return this.
     */
    public RowIterator setKeys(String start, String end)
    {
        queryIterator_.setKeys(start, end);
        return this;
    }
    
    /**
     * 
     * Sets row Count.
     * 
     * @param rowCount  row count.
     * @return this.
     */
    public RowIterator setRowCount(int rowCount)
    {
        queryIterator_.setRowCount(rowCount);
        return this;
    }

    /**
     * 
     * Sets column range.
     * 
     * @param start     start
     * @param end       end
     * @return  this
     */
    public RowIterator setColumnRange(String start, String end)
    {
        queryIterator_.setColumnRange(start, end);
        return this;
    }

    /**
     * 
     * Sets reversed.
     * 
     * @param reversed      reversed order?.
     * @return this.
     */
    public RowIterator setReversed(boolean reversed)
    {
        queryIterator_.setReversed(reversed);
        return this;
    }

    /**
     * 
     *  Sets column count. 
     * 
     * @param columnCount   column count.
     * @return this.
     */
    public RowIterator setColumnCount(int columnCount)
    {
        queryIterator_.setColumnCount(columnCount);
        return this;
    }

    /**
     * 
     * Sets limit.
     * 
     * @param limit limit
     * @return  this.
     */
    public RowIterator setLimit(int limit)
    {
        queryIterator_.setLimit(limit);
        return this;
    }

    
    /**
     * 
     * Add equal expression.
     * 
     * @param columnName    column name
     * @param expression    equal expression
     * @return this.
     */
    public RowIterator addEqualsExpression(String columnName, String expression)
    {
        queryIterator_.addEqualsExpression(columnName, expression);
        return this;
    }
    
    /**
     * 
     * excludes rows.
     * 
     * @param excludes  keys to exclude.
     * @return  this.
     */
    public RowIterator addExcludedRows(String ... excludes)
    {
        queryIterator_.addExcludedRows(excludes);
        return this;
    }
    
    @Override
    public Iterator<Row<String, String, String>> iterator()
    {
        return queryIterator_;
    }

    /**
     * 
     * Intern iterator.
     * 
     * @author msimonin
     *
     */
    public class MyQueryIterator implements Iterator<Row<String, String, String>>
    {
        /** Keyspace.*/
        private Keyspace keyspace_;
        
        /** Row start. */
        private String start_;
        
        /** Row end. */
        private String end_;
        
        /** Reversed. */
        private boolean reversed_;

        /** Column family. */
        private String columnFamily_;

        /** Count. */
        private int count_;

        /** Row Count. */
        private int rowCount_;

        /** Column first.*/
        private String columnFirst_;
        
        /** Column end. */
        private String columnEnd_;

        /** Rows. */
        private OrderedRows<String, String, String> rows_;
        
        /** rowIterator. */
        private Iterator<Row<String, String, String>> rowIterator_;

        /** Limit. */
        private int limit_;
        
        /** Limit to reach. */
        private int toLimit_;
        
        /** Newt row.*/
        private Row<String, String, String> nextRow_;
        
        /** is the query indexed ?.*/
        private boolean isIndexed_;
        
        /** Index name.*/
        private List<String> indexNames_;
        
        /** Index expressions.*/
        private List<String> indexExpressions_;
        
        /** Row to exclude.*/
        private List<String> toExclude_;

        
        /**
         * Constructor.
         */
        public MyQueryIterator()
        {
            setRowCount(100);
            setColumnCount(100);
            setKeys("", "");
            setColumnRange("", "");
            setLimit(-1);
            setReversed(false);
            setIsIndexed(false);
            indexNames_ = new ArrayList<String>();
            indexExpressions_ = new ArrayList<String>();
            toExclude_ = new ArrayList<String>();
        }
        
        /**
         * 
         * Add rows to exclude.
         * 
         * @param excludes  rows to exclude.
         */
        public void addExcludedRows(String[] excludes)
        {
            for (String exclude :excludes)
            {
                toExclude_.add(exclude);
            }
            
        }

        /**
         * Perform one step in the iteration.
         */
        private void oneStep()
        {
         
            QueryResult<OrderedRows<String, String, String>> result;
            if (!isIndexed_)
            {
                RangeSlicesQuery<String, String, String> rangeSlicesQuery = HFactory
                    .createRangeSlicesQuery(
                            keyspace_,
                            StringSerializer.get(),
                            StringSerializer.get(),
                            StringSerializer.get())
                    .setColumnFamily(columnFamily_)
                    .setKeys(start_, end_)
                    .setRowCount(rowCount_)
                    .setRange(columnFirst_, columnEnd_, reversed_, count_);
            
                result = rangeSlicesQuery.execute();
                rows_ = result.get();
            }
            else
            {
                IndexedSlicesQuery<String, String, String> indexedSlicesQuery =  HFactory
                        .createIndexedSlicesQuery(
                                keyspace_,
                                StringSerializer.get(),
                                StringSerializer.get(),
                                StringSerializer.get())
                        .setColumnFamily(columnFamily_)
                        .setStartKey(start_)
                        .setRowCount(rowCount_)
                        .setRange(columnFirst_, columnEnd_, reversed_, count_);
                
                
                for (int i = 0; i < indexNames_.size(); i++)
                {
                    indexedSlicesQuery.addEqualsExpression(indexNames_.get(i), indexExpressions_.get(i));
                }
                
                result = indexedSlicesQuery.execute();
                rows_ = result.get();
            }
            
            rowIterator_ = rows_.iterator();
        }
        
        /**
         * 
         * Add equals expression.
         * 
         * @param columnName    Column name
         * @param expression    Expression
         */
        public void addEqualsExpression(String columnName, String expression)
        {
            indexNames_.add(columnName);
            indexExpressions_.add(expression);
            isIndexed_ = true;
        }

        /**
         * 
         * Sets the keyspace.
         * 
         * @param keyspace  The keyspace.
         */
        public void setKeyspace(Keyspace keyspace)
        {
            keyspace_ = keyspace;
        }

        /**
         * 
         * Sets the column family.
         *  
         * @param columnFamily  The column family.
         */
        public void setColumnFamily(String columnFamily)
        {
            columnFamily_ = columnFamily;
        }

        /**
         * 
         * Sets keys.
         *
         * @param start     Start key.
         * @param end       End key.
         */
        public void setKeys(String start, String end)
        {
            start_ = start;
            end_ = end;
        }
        
        /**
         * 
         * Sets rowCount.
         * 
         * @param rowCount  the rowCount.
         */
        public void setRowCount(int rowCount)
        {
            rowCount_ = rowCount;
        }

        /**
         * 
         * Sets the column range.
         * 
         * @param start         Column start.
         * @param end           Column End.
         */
        public void setColumnRange(String start, String end)
        {
            columnFirst_ = start;
            columnEnd_ = end;
        }

        /**
         * 
         * Sets reversed.
         * 
         * @param reversed      reversed order.
         */
        public void setReversed(boolean reversed)
        {
            reversed_ = reversed;
        }

        /**
         * 
         * Sets the column count.
         * 
         * @param columnCount   The column count.
         */
        public void setColumnCount(int columnCount)
        {
            count_ = columnCount;
        }

        /**
         * 
         * Sets limit.
         * 
         * @param limit     The limit.
         */
        public void setLimit(int limit)
        {
            limit_ = limit;
            toLimit_ = limit; 
        }

        @Override
        public boolean hasNext()
        {
            
            if (limit_ > 0 && toLimit_ <= 0)
            {
                return false;
            }
            
            while (true)
            {
                
                while (rowIterator_.hasNext())
                {
                    nextRow_ = rowIterator_.next();
                    if (nextRow_.getColumnSlice().getColumns().isEmpty() || toExclude_.contains(nextRow_.getKey()))
                    {
                        continue;
                    }
                    else
                    {
                        return true;
                    }
                }
                
                if (rows_.getCount() < rowCount_)
                {
                   return false;
                }

                start_ = rows_.peekLast().getKey();
                oneStep();
                rowIterator_.next();
             }

        }

        @Override
        public Row<String, String, String> next()
        {
            toLimit_--;
            return nextRow_;
        }

        
        /**
         * @return the isIndexed
         */
        public boolean getIsIndexed()
        {
            return isIndexed_;
        }

        /**
         * @param isIndexed the isIndexed to set
         */
        public void setIsIndexed(boolean isIndexed)
        {
            isIndexed_ = isIndexed;
        }

        @Override
        public void remove()
        {
            
        }

    }

}
