package org.inria.myriads.snoozenode.database.api.impl.cassandra.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.inria.myriads.snoozenode.database.api.impl.cassandra.GroupManagerCassandraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.prettyprint.cassandra.model.IndexedSlicesQuery;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;

public class RowIterator implements Iterable<Row<String,String,String>>
{
    
    /** Logger. */
    private static final Logger log_ = LoggerFactory.getLogger(RowIterator.class);
    
    private MyQueryIterator queryIterator_;
    
    public RowIterator()
    {
        queryIterator_ = new MyQueryIterator();
    }
    
    public void execute()
    {
        queryIterator_.oneStep();
    }

    public RowIterator setKeyspace(Keyspace keyspace)
    {
        queryIterator_.setKeyspace(keyspace);
        return this;
    }

    public RowIterator setColumnFamily(String columnFamily)
    {
        queryIterator_.setColumnFamily(columnFamily);
        return this;
    }

    public RowIterator setKeys(String start, String end)
    {
        queryIterator_.setKeys(start, end);
        return this;
    }
    
    public RowIterator setRowCount(int rowCount)
    {
        queryIterator_.setRowCount(rowCount);
        return this;
    }

    public RowIterator setColumnRange(String start, String end)
    {
        queryIterator_.setColumnRange(start, end);
        return this;
    }

    public RowIterator setReversed(boolean reversed)
    {
        queryIterator_.setReversed(reversed);
        return this;
    }

    public RowIterator setColumnCount(int columnCount)
    {
        queryIterator_.setColumnCount(columnCount);
        return this;
    }

    public RowIterator setLimit(int limit)
    {
        queryIterator_.setLimit(limit);
        return this;
    }


    public RowIterator addEqualsExpression(String columnName, String expression)
    {
        queryIterator_.addEqualsExpression(columnName, expression);
        return this;
    }
    
    public RowIterator addExcludedRows(String ... excludes)
    {
        queryIterator_.addExcludedRows(excludes);
        return this;
    }
    
    public Iterator<Row<String, String, String>> iterator()
    {
        return queryIterator_;
    }

    public class MyQueryIterator implements Iterator<Row<String, String, String>>
    {
        
        private Keyspace keyspace_;
        
        private String start_;
        
        private String end_;
        
        private boolean reversed_;

        private String columnFamily_;

        private int count_;

        private int rowCount_;

        private String columnFirst_;
        
        private String columnEnd_;

        private OrderedRows<String, String, String> rows_;
        
        private Iterator<Row<String,String,String>> rowIterator_;

        private int limit_;
        
        private int toLimit_;
        
        private Row<String,String,String>  nextRow_ ;
        
        private boolean isIndexed_ ; 
        
        private List<String> indexNames_;
        
        private List<String> indexExpressions_;
        
        private List<String> toExclude_;

        public MyQueryIterator()
        {
            setRowCount(100);
            setColumnCount(100);
            setKeys("","");
            setColumnRange("","");
            setLimit(-1);
            setReversed(false);
            setIsIndexed(false);
            indexNames_ = new ArrayList<String>();
            indexExpressions_ = new ArrayList<String>();
            toExclude_ = new ArrayList<String>();
        }
        
        public void addExcludedRows(String[] excludes)
        {
            for (String exclude :excludes)
            {
                toExclude_.add(exclude);
            }
            
        }

        private void oneStep()
        {
         
            QueryResult<OrderedRows<String, String, String>> result;
            if (! isIndexed_)
            {
                RangeSlicesQuery<String, String, String> rangeSlicesQuery = HFactory
                    .createRangeSlicesQuery(keyspace_, StringSerializer.get(), StringSerializer.get(), StringSerializer.get())
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
                        .createIndexedSlicesQuery(keyspace_, StringSerializer.get(), StringSerializer.get(), StringSerializer.get())
                        .setColumnFamily(columnFamily_)
                        .setStartKey(start_)
                        .setRowCount(rowCount_)
                        .setRange(columnFirst_, columnEnd_, reversed_, count_);
                
                
                for (int i=0; i<indexNames_.size();i++)
                {
                    indexedSlicesQuery.addEqualsExpression(indexNames_.get(i), indexExpressions_.get(i));
                }
                
                result = indexedSlicesQuery.execute();
                rows_ = result.get();
            }
            
            rowIterator_ = rows_.iterator();
        }
        
        public void addEqualsExpression(String columnName, String expression)
        {
            indexNames_.add(columnName);
            indexExpressions_.add(expression);
            isIndexed_ = true;
        }

        public void setKeyspace(Keyspace keyspace)
        {
            keyspace_ = keyspace;
        }

        public void setColumnFamily(String columnFamily)
        {
            columnFamily_ = columnFamily;
        }

        public void setKeys(String start, String end)
        {
            start_ = start;
            end_ = end;
        }
        
        public void setRowCount(int rowCount)
        {
            rowCount_ = rowCount;
        }

        public void setColumnRange(String start, String end)
        {
            columnFirst_ = start;
            columnEnd_ = end;
        }

        public void setReversed(boolean reversed)
        {
            reversed_ = reversed;
        }

        public void setColumnCount(int columnCount)
        {
            count_ = columnCount;
        }

        public void setLimit(int limit)
        {
            limit_ = limit;
            toLimit_ = limit; 
        }

        public boolean hasNext()
        {
            
            if (limit_ >0 && toLimit_ <= 0)
            {
                return false;
            }
            
            while(true)
            {
                
                while(rowIterator_.hasNext())
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
                
                if (rows_.getCount()< rowCount_)
                {
                   return false;
                }

                start_ = rows_.peekLast().getKey();
                oneStep();
                rowIterator_.next();
             }

        }

        public Row<String,String,String> next()
        {
            toLimit_ -- ;
            return nextRow_;
        }

        public void remove()
        {
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

    }

}
