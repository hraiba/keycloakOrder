package com.af.keycloak.userorderby;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.jboss.logging.Logger;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.*;
import java.util.stream.Stream;

public class DatabaseOrderByUserProvider implements UserStorageProvider, UserQueryProvider {
    
    private static final Logger logger = Logger.getLogger(DatabaseOrderByUserProvider.class);
    private final KeycloakSession session;
    private final ComponentModel model;
    
    public DatabaseOrderByUserProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
    }
    
    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        String orderBy = params.get("orderby");
        
        // Always use database-level query to avoid loading all users into memory
        logger.infof("Executing database-level query with orderby: %s", orderBy);
        return executeOrderedQuery(realm, params, firstResult, maxResults, orderBy);
    }
    
    private Stream<UserModel> executeOrderedQuery(RealmModel realm, Map<String, String> params, 
                                                 Integer firstResult, Integer maxResults, String orderBy) {
        try {
            EntityManager em = session.getProvider(org.keycloak.connections.jpa.JpaConnectionProvider.class).getEntityManager();
            
            StringBuilder jpql = new StringBuilder("SELECT u FROM UserEntity u WHERE u.realmId = :realmId");
            Map<String, Object> queryParams = new HashMap<>();
            queryParams.put("realmId", realm.getId());
            
            // Add search conditions
            addSearchConditions(jpql, queryParams, params);
            
            // Add ORDER BY clause - apply default ordering if no orderBy provided
            if (orderBy != null && isValidOrderByParam(orderBy)) {
                addOrderByClause(jpql, orderBy);
            } else {
                // Default ordering by username to ensure consistent results
                jpql.append(" ORDER BY u.username ASC");
            }
            
            Query query = em.createQuery(jpql.toString());
            queryParams.forEach(query::setParameter);
            
            if (firstResult != null) query.setFirstResult(firstResult);
            if (maxResults != null) query.setMaxResults(maxResults);
            
            @SuppressWarnings("unchecked")
            List<Object> results = query.getResultList();
            
            return results.stream()
                    .map(entity -> session.users().getUserById(realm, 
                        ((org.keycloak.models.jpa.entities.UserEntity) entity).getId()))
                    .filter(Objects::nonNull);
                    
        } catch (Exception e) {
            logger.errorf("Database orderby query failed: %s", e.getMessage());
            // Return empty stream instead of falling back to memory-heavy operation
            return Stream.empty();
        }
    }
    
    private void addSearchConditions(StringBuilder jpql, Map<String, Object> queryParams, Map<String, String> params) {
        String search = params.get("search");
        if (search != null && !search.trim().isEmpty()) {
            jpql.append(" AND (LOWER(u.username) LIKE :search OR LOWER(u.firstName) LIKE :search OR LOWER(u.lastName) LIKE :search OR LOWER(u.email) LIKE :search)");
            queryParams.put("search", "%" + search.toLowerCase() + "%");
        }
    }
    
    private void addOrderByClause(StringBuilder jpql, String orderBy) {
        String field = orderBy.toLowerCase().trim();
        boolean descending = false;
        
        if (field.endsWith(" desc")) {
            descending = true;
            field = field.substring(0, field.lastIndexOf(" ")).trim();
        } else if (field.endsWith(" asc")) {
            field = field.substring(0, field.lastIndexOf(" ")).trim();
        }
        
        String column = mapFieldToColumn(field);
        jpql.append(" ORDER BY ").append(column);
        jpql.append(descending ? " DESC" : " ASC");
    }
    
    private String mapFieldToColumn(String field) {
        switch (field) {
            case "username":
                return "u.username";
            case "firstname":
                return "u.firstName";
            case "lastname":
                return "u.lastName";
            case "email":
                return "u.email";
            case "createdtimestamp":
                return "u.createdTimestamp";
            case "enabled":
                return "u.enabled";
            default:
                return "u.username";
        }
    }
    
    private boolean isValidOrderByParam(String orderBy) {
        if (orderBy == null || orderBy.trim().isEmpty()) {
            return false;
        }
        
        Set<String> allowedFields = Set.of(
            "username", "firstname", "lastname", "email", 
            "createdtimestamp", "enabled"
        );
        
        String field = orderBy.toLowerCase().trim();
        if (field.endsWith(" desc") || field.endsWith(" asc")) {
            field = field.substring(0, field.lastIndexOf(" ")).trim();
        }
        
        return allowedFields.contains(field);
    }
    
    @Override
    public void close() {
        // Cleanup resources if needed
    }
    
    // Delegate other required methods to default implementation
    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params) {
        return searchForUserStream(realm, params, null, null);
    }
    
    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        Map<String, String> params = new HashMap<>();
        if (search != null) params.put("search", search);
        return searchForUserStream(realm, params, firstResult, maxResults);
    }
    
    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search) {
        return searchForUserStream(realm, search, null, null);
    }
    
    @Override
    public int getUsersCount(RealmModel realm) {
        try {
            EntityManager em = session.getProvider(org.keycloak.connections.jpa.JpaConnectionProvider.class).getEntityManager();
            Query query = em.createQuery("SELECT COUNT(u) FROM UserEntity u WHERE u.realmId = :realmId");
            query.setParameter("realmId", realm.getId());
            return ((Long) query.getSingleResult()).intValue();
        } catch (Exception e) {
            logger.errorf("Failed to get users count: %s", e.getMessage());
            return 0;
        }
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult,
            Integer maxResults) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getGroupMembersStream'");
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'searchForUserByUserAttributeStream'");
    }
}