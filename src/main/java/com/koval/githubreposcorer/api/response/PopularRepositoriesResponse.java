package com.koval.githubreposcorer.api.response;

import java.util.List;

/**
 * Wrapper object for caching popular repositories.
 *
 * Why this exists:
 * - Spring Cache + Redis serializes data as JSON
 * - Generic types like List<PopularRepositoryResponse> lose type information
 *   during deserialization and become List<LinkedHashMap>
 * - This causes ClassCastException on cache hits
 *
 * By wrapping the list into a concrete type, we ensure that
 * Jackson can correctly deserialize cached values back into
 * the expected structure.
 */
public record PopularRepositoriesResponse(
    List<PopularRepositoryResponse> items
) {}
