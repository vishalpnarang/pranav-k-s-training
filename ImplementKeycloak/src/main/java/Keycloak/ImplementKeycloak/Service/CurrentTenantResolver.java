package Keycloak.ImplementKeycloak.Service;

import Keycloak.ImplementKeycloak.Model.TenantContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

public class CurrentTenantResolver implements CurrentTenantIdentifierResolver {

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenantId = TenantContext.getTenant();
        return (tenantId != null) ? tenantId : "keycloak";
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
