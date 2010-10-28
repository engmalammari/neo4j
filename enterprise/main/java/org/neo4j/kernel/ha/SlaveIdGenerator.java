/**
 * Copyright (c) 2002-2010 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.neo4j.kernel.ha;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.kernel.CommonFactories;
import org.neo4j.kernel.IdGeneratorFactory;
import org.neo4j.kernel.IdType;
import org.neo4j.kernel.ha.zookeeper.ZooKeeperException;
import org.neo4j.kernel.impl.nioneo.store.IdGenerator;
import org.neo4j.kernel.impl.nioneo.store.IdRange;
import org.neo4j.kernel.impl.nioneo.store.NeoStore;

public class SlaveIdGenerator implements IdGenerator
{
    private static final long VALUE_REPRESENTING_NULL = -1;
    
    public static class SlaveIdGeneratorFactory implements IdGeneratorFactory
    {
        private final Broker broker;
        private final ResponseReceiver receiver;
        private final Map<IdType, IdGenerator> generators = new HashMap<IdType, IdGenerator>();
        private final IdGeneratorFactory localFactory =
                CommonFactories.defaultIdGeneratorFactory();

        public SlaveIdGeneratorFactory( Broker broker, ResponseReceiver receiver )
        {
            this.broker = broker;
            this.receiver = receiver;
        }
        
        public IdGenerator open( String fileName, int grabSize, IdType idType, long highestIdInUse )
        {
            IdGenerator localIdGenerator = localFactory.open( fileName, grabSize,
                    idType, highestIdInUse );
            IdGenerator generator = new SlaveIdGenerator( idType, highestIdInUse, broker, receiver,
                    localIdGenerator );
            generators.put( idType, generator );
            return generator;
        }
        
        public void create( String fileName )
        {
            localFactory.create( fileName );
        }

        public IdGenerator get( IdType idType )
        {
            return generators.get( idType );
        }
        
        public void updateIdGenerators( NeoStore store )
        {
            store.updateIdGenerators();
        }
    };
    
    private final Broker broker;
    private final ResponseReceiver receiver;
    private volatile long highestIdInUse;
    private volatile long defragCount;
    private IdRangeIterator idQueue = new IdRangeIterator( new IdRange( new long[0], 0, 0 ) );
    private final IdType idType;
    private final IdGenerator localIdGenerator;

    public SlaveIdGenerator( IdType idType, long highestIdInUse, Broker broker,
            ResponseReceiver receiver, IdGenerator localIdGenerator )
    {
        this.idType = idType;
        this.highestIdInUse = highestIdInUse;
        this.broker = broker;
        this.receiver = receiver;
        this.localIdGenerator = localIdGenerator;
    }

    public void close()
    {
        this.localIdGenerator.close();
    }

    public void freeId( long id )
    {
    }

    public long getHighId()
    {
        return this.highestIdInUse;
    }

    public long getNumberOfIdsInUse()
    {
        return this.highestIdInUse - this.defragCount;
    }

    public synchronized long nextId()
    {
        try
        {
            long nextId = nextLocalId();
            if ( nextId == VALUE_REPRESENTING_NULL )
            {
                // If we dont have anymore grabbed ids from master, grab a bunch 
                IdAllocation allocation = broker.getMaster().first().allocateIds( idType );
                nextId = storeLocally( allocation );
            }
            return nextId;
        }
        catch ( ZooKeeperException e )
        {
            receiver.newMaster( null, e );
            throw e;
        }
        catch ( HaCommunicationException e )
        {
            receiver.newMaster( null, e );
            throw e;
        }
    }
    
    public IdRange nextIdBatch( int size )
    {
        throw new UnsupportedOperationException( "Should never be called" );
    }

    private long storeLocally( IdAllocation allocation )
    {
        this.highestIdInUse = allocation.getHighestIdInUse();
        this.defragCount = allocation.getDefragCount();
        this.idQueue = new IdRangeIterator( allocation.getIdRange() );
        updateLocalIdGenerator();
        return idQueue.next();
    }

    private void updateLocalIdGenerator()
    {
        long localHighId = this.localIdGenerator.getHighId();
        if ( this.highestIdInUse > localHighId )
        {
            this.localIdGenerator.setHighId( this.highestIdInUse );
        }
    }

    private long nextLocalId()
    {
        return this.idQueue.next();
    }

    public void setHighId( long id )
    {
        this.highestIdInUse = id;
        this.localIdGenerator.setHighId( id );
    }
    
    public long getDefragCount()
    {
        return this.defragCount;
    }
    
    private static class IdRangeIterator
    {
        private int position = 0;
        private final long[] defrag;
        private final long start;
        private final int length;

        IdRangeIterator( IdRange idRange )
        {
            this.defrag = idRange.getDefragIds();
            this.start = idRange.getRangeStart();
            this.length = idRange.getRangeLength();
        }
        
        long next()
        {
            try
            {
                if ( position < defrag.length )
                {
                    return defrag[position];
                }
                else
                {
                    int offset = position - defrag.length;
                    return ( offset < length ) ? ( start + offset ) : VALUE_REPRESENTING_NULL;
                }
            }
            finally
            {
                ++position;
            }
        }
    }
}
