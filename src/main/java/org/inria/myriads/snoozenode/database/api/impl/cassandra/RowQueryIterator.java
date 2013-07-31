package org.inria.myriads.snoozenode.database.api.impl.cassandra;

import java.util.Iterator;

import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;

public class RowQueryIterator implements Iterable<Row<String,String,String>>
{
    private final Keyspace keyspace_;
    
    Iterator<Row<String, String, String>> rowsIterator_;
    
    private String start_;
    
    private String end_;
    
    private boolean reversed_;

    private String columnFamily_;

    private int count_;

    private int rowCount_;

    private int getCount_;

    private String columnFirst_;
    
    private String columnEnd_;
    
    RowQueryIterator(Keyspace keyspace, String columnFamily, String start, String end, int rowCount)
    {
        this(keyspace, columnFamily, start, end, rowCount, null, null, false, 100);
    }
    
    RowQueryIterator(Keyspace keyspace, 
                     String columnFamily, 
                     String start, 
                     String end,
                     int rowCount,  
                     String columnFirst, 
                     String columnEnd,
                     boolean reversed, 
                     int count)
    {
        keyspace_ = keyspace;
        columnFamily_ = columnFamily;
        start_ = start;
        end_ = end;
        rowCount_ = rowCount;
        columnFirst_ = columnFirst;
        columnEnd_ = columnEnd;
        reversed_ = reversed;
        count_ = count;
    
        
        RangeSlicesQuery<String, String, String> rangeSlicesQuery = HFactory
                .createRangeSlicesQuery(keyspace_, StringSerializer.get(), StringSerializer.get(), StringSerializer.get())
                .setColumnFamily(columnFamily)
                .setKeys(start_, end_)
                .setRowCount(rowCount_)
                .setRange(columnFirst_, columnEnd_, reversed_, count_);
                
        
        rangeSlicesQuery.execute();
        QueryResult<OrderedRows<String, String, String>> result = rangeSlicesQuery.execute();
        OrderedRows<String, String, String> rows = result.get();
        getCount_ = rows.getCount();
        rowsIterator_ = rows.iterator();
    }

    @Override
    public Iterator iterator()
    {
        return rowsIterator_;
    }

    public int getCount()
    {
        return getCount_;
    }
}
