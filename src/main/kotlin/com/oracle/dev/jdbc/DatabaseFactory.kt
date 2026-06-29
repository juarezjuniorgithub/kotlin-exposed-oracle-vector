package com.oracle.dev.jdbc

import oracle.ucp.admin.UniversalConnectionPoolManagerImpl
import oracle.ucp.jdbc.PoolDataSource
import oracle.ucp.jdbc.PoolDataSourceFactory
import org.jetbrains.exposed.v1.jdbc.Database

object DatabaseFactory {
    fun connect(config: AppConfig): PoolDataSource {
        val dataSource = PoolDataSourceFactory.getPoolDataSource().apply {
            connectionFactoryClassName = "oracle.jdbc.pool.OracleDataSource"
            url = config.jdbcUrl
            user = config.username
            password = config.password
            connectionPoolName = config.poolName
            initialPoolSize = config.initialPoolSize
            minPoolSize = config.minPoolSize
            maxPoolSize = config.maxPoolSize
        }

        Database.connect(dataSource)
        return dataSource
    }

    fun destroyPool(dataSource: PoolDataSource) {
        UniversalConnectionPoolManagerImpl
            .getUniversalConnectionPoolManager()
            .destroyConnectionPool(dataSource.connectionPoolName)
    }
}
