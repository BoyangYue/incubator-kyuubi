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

package org.apache.kyuubi

import org.apache.kyuubi.config.KyuubiConf
import org.apache.kyuubi.config.KyuubiConf._
import org.apache.kyuubi.config.KyuubiConf.FrontendProtocols.FrontendProtocol
import org.apache.kyuubi.ha.HighAvailabilityConf.{HA_ZK_AUTH_TYPE, HA_ZK_QUORUM}
import org.apache.kyuubi.ha.client.ZooKeeperAuthTypes
import org.apache.kyuubi.server.KyuubiServer
import org.apache.kyuubi.zookeeper.{EmbeddedZookeeper, ZookeeperConf}

trait WithKyuubiServer extends KyuubiFunSuite {

  protected val conf: KyuubiConf

  protected val frontendProtocols: Seq[FrontendProtocol] =
    FrontendProtocols.THRIFT_BINARY :: Nil

  private var zkServer: EmbeddedZookeeper = _
  protected var server: KyuubiServer = _

  override def beforeAll(): Unit = {
    conf.set(FRONTEND_PROTOCOLS, frontendProtocols.map(_.toString))
    conf.set(FRONTEND_THRIFT_BINARY_BIND_PORT, 0)
    conf.set(FRONTEND_REST_BIND_PORT, 0)
    conf.set(FRONTEND_MYSQL_BIND_PORT, 0)

    zkServer = new EmbeddedZookeeper()
    conf.set(ZookeeperConf.ZK_CLIENT_PORT, 0)
    val zkData = Utils.createTempDir()
    conf.set(ZookeeperConf.ZK_DATA_DIR, zkData.toString)
    zkServer.initialize(conf)
    zkServer.start()
    conf.set(HA_ZK_QUORUM, zkServer.getConnectString)
    conf.set(HA_ZK_AUTH_TYPE, ZooKeeperAuthTypes.NONE.toString)

    conf.set("spark.ui.enabled", "false")
    conf.setIfMissing("spark.sql.catalogImplementation", "in-memory")
    conf.setIfMissing(ENGINE_CHECK_INTERVAL, 3000L)
    conf.setIfMissing(ENGINE_IDLE_TIMEOUT, 10000L)
    // TODO KYUUBI #745
    conf.setIfMissing(ENGINE_INIT_TIMEOUT, 300000L)
    server = KyuubiServer.startServer(conf)
    super.beforeAll()
    Thread.sleep(1500)
  }

  override def afterAll(): Unit = {

    if (server != null) {
      server.stop()
      server = null
    }

    if (zkServer != null) {
      zkServer.stop()
      zkServer = null
    }
    super.afterAll()
  }

  protected def getJdbcUrl: String = s"jdbc:hive2://${server.frontendServices.head.connectionUrl}/;"
}
