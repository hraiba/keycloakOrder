package com.af.keycloak.orderby;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import com.af.keycloak.orderby.UserQueryService.SortField;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

public class OrderByResource {
    private final KeycloakSession session;

    public OrderByResource(KeycloakSession session) {
        this.session = session;
    }

    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrderedUsers(
            @QueryParam("orderby") String orderBy,
            @QueryParam("first") @DefaultValue("0") Integer first,
            @QueryParam("max") @DefaultValue("100") Integer max) {
        try {
            if (!isAuthorized()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Unauthorized"))
                        .build();
            }

            RealmModel realm = session.getContext().getRealm();
            if (realm == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Realm not found"))
                        .build();
            }

            List<UserQueryService.SortField> sortFields = parseOrderByParameter(orderBy);
            List<UserModel> users = getUsersWithORdering(realm, sortFields, first, max);
            List<Map<String, Object>> userList = users.stream()
                    .map(this::convertUserToMap)
                    .collect(Collectors.toList());
            Map<String, Object> response = new HashMap<>();
            response.put("users", userList);
            response.put("count", userList.size());
            response.put("first", first);
            response.put("max", max);
            response.put("orderBy", orderBy);
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Internal server error: " + e.getMessage()))
                    .build();
        }
    }

    private boolean isAuthorized() {
        return true;
    }

    private List<SortField> parseOrderByParameter(String orderBy) {
        List<SortField> sortFields = new ArrayList<>();
        if (orderBy == null || orderBy.trim().isEmpty()) {
            sortFields.add(new UserQueryService.SortField("username", "asc"));
            return sortFields;
        }

        String[] fields = orderBy.split(",");
        for (String field : fields) {
            field = field.trim();
            String fieldName;
            String direction = "asc"; // Default direction
            if (field.contains(":")) {
                String[] parts = field.split(":");
                fieldName = parts[0].trim();
                direction = parts[1].trim().toLowerCase();

                if (!direction.equals("asc") && !direction.equals("desc")) {
                    direction = "asc"; // Default to ascending if invalid direction
                }
            } else {
                fieldName = field;
            }

            if (isValidSortField(fieldName)) {
                sortFields.add(new SortField(fieldName, direction));
            }
        }

        if (sortFields.isEmpty()) {
            sortFields.add(new UserQueryService.SortField("username", "asc"));
        }

        return sortFields;
    }

    private boolean isValidSortField(String fieldName) {
        return Set.of(
                "username",
                "email",
                "firstName",
                "lastName",
                "createdTimestamp").contains(fieldName);
    }

    private List<UserModel> getUsersWithORdering(RealmModel realm, List<SortField> fields,
    int first, int max){
        UserQueryService queryService = new UserQueryService(session);
        return queryService.getUsersWithORdering(realm, fields, first, max);
    }
    
    private Map<String, Object> convertUserToMap(UserModel user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail());
        userMap.put("firstName", user.getFirstName());
        userMap.put("lastName", user.getLastName());
        userMap.put("createdTimestamp", user.getCreatedTimestamp());
        return userMap;
    }

}
