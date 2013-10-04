package org.inria.myriads.snoozenode.database.api.impl.cassandra.utils;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

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

import org.inria.myriads.snoozenode.database.api.impl.cassandra.CassandraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * Cassandra utils.
 * 
 * @author msimonin
 *
 */
public final class CassandraUtils
{

    /** Cluster column family. */
    public static final String CLUSTER = "Test Cluster";
    
    /** Keyspace column family. */
    public static final String KEYSPACE = "snooze";
    
    /** virtual Machines column family. */
    public static final String VIRTUALMACHINES_CF = "virtualmachines";
    
    /** virtual Machines column family. */
    public static final String VIRTUALMACHINES_MONITORING_CF = "virtualmachines_monitorings";
    
    /** Groupmanagers column family. */
    public static final String GROUPMANAGERS_CF = "groupmanagers";
    
    /** localcontrollers column family. */
    public static final String LOCALCONTROLLERS_CF = "localcontrollers";
    
    /** localcontrollers monitoring column family. */
    public static  final String LOCALCONTROLLERS_MAPPING_CF = "localcontrollers_mappings";
    
    /** groupmanagers column family. */
    public static  final String GROUPMANAGERS_MONITORING_CF = "groupmanagers_monitorings";
    
    /** ippools column family. */
    public static final String IPSPOOL_CF = "ipspools";
    
    /** Ips row key in IPSPOOL_CF.*/
    public static final String IPS_ROW_KEY = "0";
    
    /** byte[] true.*/
    public static final byte[] byteTrue = {1};
    
    /** byte[] false.*/
    public static final byte[] byteFalse = {0};
    
    /** string true.*/
    public static final String stringTrue = new String(byteTrue, Charset.forName("UTF-8"));
    
    /** string false.*/
    public static final String stringFalse = new String(byteFalse, Charset.forName("UTF-8"));
    
    /** Logger. */
    protected static final Logger log_ = LoggerFactory.getLogger(CassandraRepository.class);
    
    /**
     * Hide Constructor.
     */
    private CassandraUtils()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * 
     * Add a column in a specific column family.
     * 
     * @param keyspace          Keyspace.
     * @param rowKey            RowKey.
     * @param columnFamily      ColumnFamily.
     * @param name              Name.
     * @param value             Value.
     * @return  true iff everything is ok.
     */
    public static boolean addStringColumn(
            Keyspace keyspace,
            String rowKey,
            String columnFamily,
            String name,
            String value)
    {
        try
        {
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
     * @param keyspace          Keyspace.
     * @param columnFamily      ColumnFamily.
     * @param rowKey            RowKey.
     * @return true iff everything is ok.
     */
    public static boolean checkForRow(Keyspace keyspace, String columnFamily, String rowKey)
    {
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
     * @param keyspace          Keyspace.
     * @param list              List.
     * @param columnFamily      ColumnFamily.
     * @return true iff everything is ok.
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
        catch (Exception exception)
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
     * @param keyspace          Keyspace.
     * @param keys              Keys.
     * @param columnFamily      ColumnFamily.
     * @return  true iff everything ok.
     */
    public static boolean unassignNodes(Keyspace keyspace, List<String> keys, String columnFamily)
    {
        try
        {
            MultigetSliceQuery<String, String, String> multigetSliceQuery =
                    HFactory.createMultigetSliceQuery(
                            keyspace, 
                            StringSerializer.get(), 
                            StringSerializer.get(), 
                            StringSerializer.get());
            
            multigetSliceQuery.setKeys(keys);
            multigetSliceQuery.setColumnFamily(columnFamily);
            multigetSliceQuery.setRange(null, null, false, 100); 
            
            QueryResult<Rows<String, String, String>> result = multigetSliceQuery.execute();
            Mutator<String> mutator = HFactory.createMutator(keyspace, StringSerializer.get());
            for (Row<String, String, String> row  : result.get())
            {
                ColumnSlice<String, String> columnSlice = row.getColumnSlice();
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
                            HFactory.createColumn(
                                    column.getName(),
                                    column.getValue(), 
                                    67, 
                                    column.getNameSerializer(),
                                    column.getValueSerializer()));
                }
                mutator.addInsertion(
                        row.getKey(),
                        columnFamily,
                        HFactory.createColumn("isAssigned",
                                false,
                                67,
                                StringSerializer.get(), BooleanSerializer.get()));
              }
              mutator.execute();
        }
        catch (Exception exception)
        {
            log_.error("Unable to unassign the nodes");
            exception.printStackTrace();
            return false;
        }
        return true;
    }

    
    /**
     * 
     * Unassign nodes from a column family.
     * 
     * @param keyspace          The keyspace
     * @param columnFamily      The column family
     */
    public static void unassignNodes(Keyspace keyspace, String columnFamily)
    {
        log_.debug("Unassign all the rows from " + keyspace.getKeyspaceName());

        RowIterator rowIterator = new RowIterator();
        rowIterator
            .setKeyspace(keyspace)
            .setColumnFamily(columnFamily);
        rowIterator.execute();
        
        Mutator<String> mutator = HFactory.createMutator(keyspace, StringSerializer.get());
        for (Row<String, String, String> row : rowIterator)
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
                      HFactory.createColumn(
                              column.getName(), 
                              column.getValue(), 
                              67, 
                              column.getNameSerializer(), 
                              column.getValueSerializer()));
          }
          mutator.addInsertion(
                  row.getKey(),
                  columnFamily,
                  HFactory.createColumn(
                          "isAssigned",
                          false,
                          67,
                          StringSerializer.get(),
                          BooleanSerializer.get()));
        }
        mutator.execute();
    }
     
        

    
}


