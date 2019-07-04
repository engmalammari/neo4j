/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.runtime.debug

object DebugSupport {

  final val DEBUG_WORKERS = false
  final val DEBUG_QUERIES = false
  final val DEBUG_TRACKER = false
  final val DEBUG_LOCKS = false
  final val DEBUG_ERROR_HANDLING = false
  final val DEBUG_CURSORS = false
  final val DEBUG_PIPELINES = false

  def logWorker(str: => String): Unit = {
    if (DEBUG_WORKERS) {
      println(s"        $str")
    }
  }

  def logQueries(str: => String): Unit = {
    if (DEBUG_QUERIES) {
      println(s"        $str")
    }
  }

  def logTracker(str: => String): Unit = {
    if (DEBUG_TRACKER) {
      println(s"        $str")
    }
  }

  def logLocks(str: => String): Unit = {
    if (DEBUG_LOCKS) {
      println(s"        $str")
    }
  }

  def logErrorHandling(str: => String): Unit = {
    if (DEBUG_ERROR_HANDLING) {
      println(s"        $str")
    }
  }

  def logCursors(str: => String): Unit = {
    if (DEBUG_CURSORS) {
      println(s"        $str")
    }
  }

  def logPipelines(rows: => Seq[String]): Unit = {
    if (DEBUG_PIPELINES) {
      for (row <- rows) {
        println(s"       || $row")
      }
    }
  }
}
