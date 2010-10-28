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

import javax.transaction.Transaction;

import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.ha.zookeeper.ZooKeeperException;
import org.neo4j.kernel.impl.transaction.TxFinishHook;

public class SlaveTxRollbackHook implements TxFinishHook
{
    private final Broker broker;
    private final ResponseReceiver receiver;

    public SlaveTxRollbackHook( Broker broker, ResponseReceiver receiver )
    {
        this.broker = broker;
        this.receiver = receiver;
    }
    
    public boolean hasAnyLocks( Transaction tx )
    {
        return ((AbstractGraphDatabase) receiver).getConfig().getLockReleaser().hasLocks( tx );
    }

    public void finishTransaction( int eventIdentifier )
    {
        try
        {
            receiver.receive( broker.getMaster().first().finishTransaction(
                    receiver.getSlaveContext( eventIdentifier ) ) );
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
}
