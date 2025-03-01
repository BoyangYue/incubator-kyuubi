/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.kyuubi.engine.trino

import org.apache.kyuubi.KyuubiSQLException

class TrinoStatementSuite extends WithTrinoContainerServer {

  test("test query") {
    val trinoStatement = TrinoStatement(TrinoContext(httpClient, session), kyuubiConf, "select 1")
    val schema = trinoStatement.getColumns
    val resultSet = trinoStatement.execute()

    assert(schema.size === 1)
    assert(schema(0).getName === "_col0")

    assert(resultSet.toIterator.hasNext)
    assert(resultSet.toIterator.next() === List(1))

    val trinoStatement2 =
      TrinoStatement(TrinoContext(httpClient, session), kyuubiConf, "show schemas")
    val schema2 = trinoStatement2.getColumns
    val resultSet2 = trinoStatement2.execute()

    assert(schema2.size === 1)
    assert(resultSet2.toIterator.hasNext)
  }

  test("test update session") {
    val trinoStatement = TrinoStatement(TrinoContext(httpClient, session), kyuubiConf, "select 1")
    val schema2 = trinoStatement.getColumns

    assert(schema2.size === 1)
    assert(schema2(0).getName === "_col0")
    assert(this.schema === trinoStatement.getCurrentDatabase)

    val trinoStatement2 = TrinoStatement(TrinoContext(httpClient, session), kyuubiConf, "use sf1")
    trinoStatement2.execute()

    assert("sf1" === trinoStatement2.getCurrentDatabase)
  }

  test("test exception") {
    val trinoStatement = TrinoStatement(TrinoContext(httpClient, session), kyuubiConf, "use kyuubi")
    val e1 = intercept[KyuubiSQLException](trinoStatement.execute())
    assert(e1.getMessage.contains("Schema does not exist: tpch.kyuubi"))
  }
}
