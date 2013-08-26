package org.inria.myriads.snoozenode.database.api.impl.cassandra.utils;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

import org.inria.myriads.snoozenode.database.api.impl.cassandra.CassandraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.Rows;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.MultigetSliceQuery;
import me.prettyprint.hector.api.query.QueryResult;

public final class CassandraUtils
{
    /** Logger. */
    protected static final Logger log_ = LoggerFactory.getLogger(CassandraRepository.class);
    
    /** Cluster column family. */
    public static  String CLUSTER = "Test Cluster";
    
    /** Keyspace column family. */
    public static  String KEYSPACE = "snooze";
    
    /** virtual Machines column family. */
    public static  String VIRTUALMACHINES_CF = "virtualmachines";
    
    /** virtual Machines column family. */
    public static  String VIRTUALMACHINES_MONITORING_CF = "virtualmachines_monitorings";
    
    /** Groupmanagers column family. */
    public static  String GROUPMANAGERS_CF = "groupmanagers";
    /** localcontrollers column family. */
    public static  String LOCALCONTROLLERS_CF = "localcontrollers";
    
    /** localcontrollers monitoring column family. */
    public static  String LOCALCONTROLLERS_MAPPING_CF = "localcontrollers_mappings";
    
    /** groupmanagers column family. */
    public static  String GROUPMANAGERS_MONITORING_CF = "groupmanagers_monitorings";
    
    /** ippools column family. */
    public static  String IPSPOOL_CF = "ipspools";
    
    /** Ips row key in IPSPOOL_CF*/
    public static final String IPS_ROW_KEY = "0";
    
    /** byte[] true.*/
    public static final byte[] byteTrue = {1};
    
    /** byte[] false.*/
    public static final byte[] byteFalse = {0};
    
    /** string true.*/
    public static final String stringTrue = new String(byteTrue, Charset.forName("UTF-8"));
    
    /** string false.*/
    public static final String stringFalse = new String(byteFalse, Charset.forName("UTF-8"));
    
    /**
     * Hide Constructor.
     */
    public CassandraUtils()
    {
        throw new UnsupportedOperationException();
    }
    
    
    /**
     * 
     * Add a column in a specific column family
     * 
     * @param rowKey
     * @param columnFamily
     * @return
     */
    public static boolean addStringColumn(Keyspace keyspace, String rowKey, String columnFamily, String name, String value)
    {
        try{
            Mutator<String> mutator = HFactory.createMutator(keyspace, StringSerializer.get());
            mutator.addInsertion(rowKey, columnFamily, HFactory.createStringColumn(name, value));
            mutator.execute();
        }
        catch (Exception e)
        {
            log_.error("Unable to add this column to the column family " + columnFamily);
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    /**
     * 
     * Checks if a specific row exist.
     * 
     * @param columnFamily
     * @param rowKey
     * @return
     */
    public static boolean checkForRow(Keyspace keyspace, String columnFamily, String rowKey)
    {
//        RowIterator rowQueryIterator = new RowIterator(
//                keyspace, CassandraUtils.IPSPOOL_CF,
//                CassandraUtils.IPS_ROW_KEY, // start
//                CassandraUtils.IPS_ROW_KEY, // end
//                1); // rows to fecth 
        RowIterator rowIterator = new RowIterator();
        rowIterator.setKeyspace(keyspace)
                   .setColumnFamily(columnFamily)
                   .setKeys(rowKey, rowKey);
        rowIterator.execute();
        
        Iterator<Row<String, String, String>> rowsIterator = rowIterator.iterator();
        if (!rowsIterator.hasNext() || !rowsIterator.next().getKey().equals(rowKey))
        {
            log_.debug(String.format("Row key %s doesn't exist in %s ", rowKey, columnFamily));
            return false;
        }
        return true;
    }
    
    
    /**
     * 
     * Drop list of keys from a column family.
     * 
     * @param list                  List of keys
     * @param columnFamily          Column family to remove from
     */
    public static boolean drop(Keyspace keyspace, List<String> list, String columnFamily)
    {
        
        log_.debug(String.format("Removing %d keys from %s", list.size(), columnFamily));
        try
        {
            Mutator<String> mutator = HFactory.createMutator(keyspace, StringSerializer.get());
            mutator.addDeletion(list, columnFamily);
            mutator.execute();
        }
        catch(Exception exception)
        {
            log_.error(String.format("Unable to remove the keys from %s", columnFamily));
            return false;
        }
        
        return true;
    }           
    
    /**
     * 
     * Unassign a list of row.
     * I hope it will handle more than 100 rows ! 
     * 
     * @param keys
     * @param columnFamily
     */
    public static boolean unassignNodes(Keyspace keyspace, List<String> keys, String columnFamily)
    {
        try
        {
            MultigetSliceQuery<String, String, String> multigetSliceQuery =
                    HFactory.createMultigetSliceQuery(keyspace, StringSerializer.get(), StringSerializer.get(), StringSerializer.get());
            multigetSliceQuery.setKeys(keys);
            multigetSliceQuery.setColumnFamily(columnFamily);
            multigetSliceQuery.setRange(null, null, false, 100); 
            
            QueryResult<Rows<String, String, String>> result = multigetSliceQuery.execute();
            Mutator<String> mutator = HFactory.createMutator(keyspace, StringSerializer.get());
            for (Row<String, String, String> row  : result.get())
            {
                ColumnSlice<String, String> columnSlice = row.getColumnSlice() ; 
                if (columnSlice.getColumns().isEmpty()) 
                {
                  //tombstone
                  continue;
                }
                
                for (HColumn<String, String>  column : row.getColumnSlice().getColumns())
                {
                    mutator.addInsertion(
                            row.getKey(), 
                            columnFamily, 
                            HFactory.createColumn(column.getName(), column.getValue(), 67, column.getNameSerializer(), column.getValueSerializer()));
                }
                mutator.addInsertion(row.getKey(), columnFamily, HFactory.createColumn("isAssigned", false, 67, StringSerializer.get(), BooleanSerializer.get()));
              }
              mutator.execute();
        }
        catch(Exception exception)
        {
            log_.error("Unable to unassign the nodes");
            exception.printStackTrace();
            return false;
        }
        return true;
    }

    
    public static void unassignNodes(Keyspace keyspace, String columnFamily)
    {
        log_.debug("Unassign all the group managers");

        RowIterator rowIterator = new RowIterator();
        rowIterator
            .setKeyspace(keyspace)
            .setColumnFamily(columnFamily);
        rowIterator.execute();
        
        Mutator<String> mutator = HFactory.createMutator(keyspace, StringSerializer.get());
        for (Row<String,String,String> row : rowIterator)
        {
          ColumnSlice<String, String> columnSlice = row.getColumnSlice();
          HColumn<String, String> isGroupLeaderColumn = columnSlice.getColumnByName("isGroupLeader");
          if (isGroupLeaderColumn != null && isGroupLeaderColumn.getValue().equals(CassandraUtils.stringTrue))
          {
              log_.debug("DELETION of previous GL");
              mutator.addDeletion(row.getKey(), columnFamily);
              continue;
          }
          for (HColumn<String, String>  column : row.getColumnSlice().getColumns())
          {
              mutator.addInsertion(
                      row.getKey(), 
                      columnFamily, 
                      HFactory.createColumn(column.getName(), column.getValue(), 67, column.getNameSerializer(), column.getValueSerializer()));
          }
          mutator.addInsertion(row.getKey(), columnFamily, HFactory.createColumn("isAssigned", false, 67, StringSerializer.get(), BooleanSerializer.get()));
        }
        mutator.execute();
    }
     
        
//        int row_count = 100;
//
//        String lastKey = null;
//        Mutator<String> mutator = HFactory.createMutator(keyspace, StringSerializer.get());
//        while (true) {
//            RowIterator rowQueryIterator = new RowIterator(
//                    keyspace, columnFamily,
//                    lastKey, // start
//                    null, // end
//                    row_count); // rows to fetch 
//            
//            @SuppressWarnings("unchecked")
//            Iterator<Row<String, String, String>> rowsIterator = rowQueryIterator.iterator();
//            
//            if (lastKey != null && rowsIterator != null) rowsIterator.next();   
//
//            while (rowsIterator.hasNext()) {
//              Row<String, String, String> row = rowsIterator.next();
//              lastKey = row.getKey();
//              
//              ColumnSlice<String, String> columnSlice = row.getColumnSlice() ; 
//              if (columnSlice.getColumns().isEmpty()) 
//              {
//                //tombstone
//                continue;
//              }
//              
//              HColumn<String, String> isGroupLeaderColumn = columnSlice.getColumnByName("isGroupLeader");
//              if (isGroupLeaderColumn != null && isGroupLeaderColumn.getValue().equals(CassandraUtils.stringTrue))
//              {
//                  log_.debug("DELETION of previous GL");
//                  mutator.addDeletion(row.getKey(), columnFamily);
//                  continue;
//              }
//              for (HColumn<String, String>  column : row.getColumnSlice().getColumns())
//              {
//                  mutator.addInsertion(
//                          row.getKey(), 
//                          columnFamily, 
//                          HFactory.createColumn(column.getName(), column.getValue(), 67, column.getNameSerializer(), column.getValueSerializer()));
//              }
//              mutator.addInsertion(row.getKey(), columnFamily, HFactory.createColumn("isAssigned", false, 67, StringSerializer.get(), BooleanSerializer.get()));
//            }
//            mutator.execute();
//
//            if (rowQueryIterator.getCount() < row_count)
//                break;
//        }
//        
//    }
    
}


