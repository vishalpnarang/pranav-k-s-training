package Keycloak.ImplementKeycloak.Service;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class SchemaConnectionProvider implements MultiTenantConnectionProvider {

    private final DataSource dataSource;

    public SchemaConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    // 🟢 Main logic to switch schema
    @Override
    public Connection getConnection(Object tenantIdentifier) throws SQLException {
        System.out.println("Switching to schema: " + tenantIdentifier);
        Connection connection = getAnyConnection();
        String schema = tenantIdentifier.toString();
        connection.createStatement().execute("USE " + schema);
        return connection;
    }

    @Override
    public void releaseConnection(Object tenantIdentifier, Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class unwrapType) {
        return false; // No unwrapping supported
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null; // Not unwrappable
    }
}
