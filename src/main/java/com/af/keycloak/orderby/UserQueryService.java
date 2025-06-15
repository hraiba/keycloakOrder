package com.af.keycloak.orderby;

import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.UserEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

public class UserQueryService {
    private static final Logger logger = Logger.getLogger(UserQueryService.class);

    private final KeycloakSession session;
    private final EntityManager entityManager;

    public UserQueryService(KeycloakSession session) {
        this.session = session;
        this.entityManager = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        logger.info("UserQueryService initialized with session: " + session);
    }

    public List<UserModel> getUsersWithORdering(RealmModel realm,
            List<SortField> sortFields,
            int first, int max) {
        try {
            String jpql = buildOrderByQuery(realm.getId(), sortFields);
            logger.infof("Extending query: %s", jpql);
            TypedQuery<UserEntity> query = entityManager.createQuery(jpql, UserEntity.class);
            query.setFirstResult(first);
            query.setMaxResults(max);
            List<UserEntity> userEntities = query.getResultList();

            return userEntities.stream()
                    .map(entity -> session.users().getUserById(realm, entity.getId()))
                    .filter(user -> user != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.errorf("Error retrieving users with ordering", e.getMessage());
            logger.warn("Falling back to standard user query without ordering ");
            // Fallback to standard user query without ordering
            // return session.users().getusers(realm, first, max);
            throw new RuntimeException("Error retrieving users with ordering: " + e.getMessage(), e);
        }
    }

    private String buildOrderByQuery(String realmId, List<SortField> sortFields) {
        StringBuilder jpql = new StringBuilder();
        ;
        jpql
                .append("SELECT u FROM UserEntity u WHERE u.realmId = '")
                .append(realmId)
                .append("'");
        if (!sortFields.isEmpty()) {
            jpql.append(" ORDER BY ");

            for (int i = 0; i < sortFields.size(); i++) {
                if (i > 0) {
                    jpql.append(", ");
                }
                SortField sortField = sortFields.get(i);
                String entityField = mapToEntityField(sortField.field);
                jpql.append("u.")
                        .append(entityField)
                        .append(" ")
                        .append(sortField.direction.toUpperCase());
            }
        }
        return jpql.toString();
    }

    private String mapToEntityField(String apiFieldName) {
        switch (apiFieldName) {
            case "username":
                return "username";
            case "email":
                return "email";
            case "firstName":
                return "firstName";
            case "lastName":
                return "lastName";
            case "enabled":
                return "enabled";
            case "createdTimestamp":
                return "createdTimestamp";
            default:
                // Fallback to username if unknown field
                logger.warnf("Unknown sort field: %s, falling back to username", apiFieldName);
                return "username";
        }
    }

    public long getUserCount(RealmModel realm) {
        try {
            String jpql = "SELECT COUNT(u) FROM UserEntity u WHERE u.realmId = :realmId";
            TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
            query.setParameter("realmId", realm.getId());
            return query.getSingleResult();
        } catch (Exception e) {
            logger.errorf("Error retrieving user count: %s", e.getMessage());
            return 0; // Return 0 in case of error
        }
    }

   public static class SortField {
        public final String field;
        public final String direction;
        
        public SortField(String field, String direction) {
            this.field = field;
            this.direction = direction;
        }
        
        @Override
        public String toString() {
            return field + ":" + direction;
        }
    }
}
