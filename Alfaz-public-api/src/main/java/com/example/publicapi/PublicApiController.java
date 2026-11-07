
package com.example.publicapi;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.*;

@RestController
@RequestMapping("/public-api")
public class PublicApiController {

    private final RestTemplate rest;

    @Value("${listing.service.url}")
    private String listingServiceUrl;

    @Value("${user.service.url}")
    private String userServiceUrl;

    public PublicApiController(RestTemplate rest) {
        this.rest = rest;
    }

    @GetMapping("/listings")
    public ResponseEntity<Map<String, Object>> getListings(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String userId) {

        UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl(listingServiceUrl + "/listings")
                .queryParam("pageNum", pageNum)
                .queryParam("pageSize", pageSize);
        if (userId != null && !userId.isBlank()) {
            uri.queryParam("userId", userId);
        }

        JsonNode body = rest.getForObject(uri.toUriString(), JsonNode.class);

        Map<Integer, JsonNode> userCache = new HashMap<>();
        List<Map<String, Object>> listings = new ArrayList<>();

        for (JsonNode l : body.path("listings")) {
            int uid = l.path("userId").asInt();
            if (!userCache.containsKey(uid)) {
                userCache.put(uid, fetchUser(uid));
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", l.path("id"));
            item.put("listingType", l.path("listingType"));
            item.put("price", l.path("price"));
            item.put("createdAt", l.path("createdAt"));
            item.put("updatedAt", l.path("updatedAt"));
            item.put("user", userCache.get(uid));
            listings.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("result", true);
        result.put("listings", listings);
        return ResponseEntity.ok(result);
    }

    public record CreateUserRequest(String name) {
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody CreateUserRequest req) {
        if (req == null || req.name() == null || req.name().isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "name is required");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("name", req.name());

        JsonNode resp = postForm(userServiceUrl + "/users", form);
        return ResponseEntity.ok(Map.of("user", resp.path("user")));
    }

    public record CreateListingRequest(Integer userId, String listingType, Integer price) {
    }

    @PostMapping("/listings")
    public ResponseEntity<Map<String, Object>> createListing(@RequestBody CreateListingRequest req) {
        if (req == null || req.userId() == null || req.listingType() == null || req.price() == null) {
            return error(HttpStatus.BAD_REQUEST, "userId, listingType and price are required");
        }
        if (!req.listingType().equals("rent") && !req.listingType().equals("sale")) {
            return error(HttpStatus.BAD_REQUEST, "listingType must be 'rent' or 'sale'");
        }
        if (req.price() <= 0) {
            return error(HttpStatus.BAD_REQUEST, "price must be greater than 0");
        }
        if (fetchUser(req.userId()) == null) {
            return error(HttpStatus.BAD_REQUEST, "user not found");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("userId", String.valueOf(req.userId()));
        form.add("listingType", req.listingType());
        form.add("price", String.valueOf(req.price()));

        JsonNode resp = postForm(listingServiceUrl + "/listings", form);
        return ResponseEntity.ok(Map.of("listing", resp.path("listing")));
    }

    private JsonNode fetchUser(int id) {
        try {
            JsonNode resp = rest.getForObject(userServiceUrl + "/users/" + id, JsonNode.class);
            return resp == null ? null : resp.path("user");
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    private JsonNode postForm(String url, MultiValueMap<String, String> form) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return rest.postForObject(url, new HttpEntity<>(form, headers), JsonNode.class);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("result", false, "errors", List.of(message)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> badJson(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid JSON body");
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Map<String, Object>> downstreamClientError(HttpClientErrorException e) {
        return error(HttpStatus.valueOf(e.getStatusCode().value()), e.getResponseBodyAsString());
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> serviceDown(ResourceAccessException e) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "a downstream service is unavailable");
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, Object>> downstreamError(RestClientException e) {
        return error(HttpStatus.BAD_GATEWAY, "downstream service error");
    }
}
