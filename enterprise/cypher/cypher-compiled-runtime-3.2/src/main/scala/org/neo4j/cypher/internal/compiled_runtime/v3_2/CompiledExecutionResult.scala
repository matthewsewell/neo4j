/*
 * Copyright (c) 2002-2017 "Neo Technology,"
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
package org.neo4j.cypher.internal.compiled_runtime.v3_2

import java.util

import org.neo4j.cypher.internal.compiled_runtime.v3_2.executionplan.GeneratedQueryExecution
import org.neo4j.cypher.internal.compiler.v3_2._
import org.neo4j.cypher.internal.compiler.v3_2.executionplan.{Provider, READ_ONLY, StandardInternalExecutionResult}
import org.neo4j.cypher.internal.compiler.v3_2.planDescription.InternalPlanDescription
import org.neo4j.cypher.internal.compiler.v3_2.spi.{InternalResultVisitor, QueryContext}
import org.neo4j.cypher.internal.frontend.v3_2.ProfilerStatisticsNotReadyException

/**
 * Main class for compiled execution results, implements everything in InternalExecutionResult
 * except `javaColumns` and `accept` which delegates to the injected compiled code.
 */
class CompiledExecutionResult(taskCloser: TaskCloser,
                              context: QueryContext,
                              compiledCode: GeneratedQueryExecution,
                              description: Provider[InternalPlanDescription])
  extends StandardInternalExecutionResult(context, Some(taskCloser))
  with StandardInternalExecutionResult.IterateByAccepting {

  compiledCode.setCompletable(this)

  // *** Delegate to compiled code
  def executionMode: ExecutionMode = compiledCode.executionMode()

  override def javaColumns: util.List[String] = compiledCode.javaColumns()

  override def accept[EX <: Exception](visitor: InternalResultVisitor[EX]): Unit =
    compiledCode.accept(visitor)

  override def executionPlanDescription(): InternalPlanDescription = {
    if (!taskCloser.isClosed) {
      completed(success = false)
      throw new ProfilerStatisticsNotReadyException
    }
    else
      compiledCode.executionPlanDescription()
  }

  override def queryStatistics() = InternalQueryStatistics()

  //TODO delegate to compiled code once writes are being implemented
  override def executionType = READ_ONLY
}
