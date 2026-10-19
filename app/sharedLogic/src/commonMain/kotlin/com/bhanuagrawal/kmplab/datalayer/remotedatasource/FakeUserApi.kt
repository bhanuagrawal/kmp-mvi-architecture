package com.bhanuagrawal.kmplab.datalayer.remotedatasource

import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Simulates a remote API serving 50 immutable seed users.
 *
 * IDs are chosen so they sort lexicographically BEFORE any locally-added user:
 *   API seed:   "0000000000001" .. "0000000000050"   (13-char zero-padded)
 *   Locally added (RegisterUsecase): "<currentTimeMillis>_<uuid>"  (e.g. "1716643200000_...")
 *
 * Since both are strings and '0' < '1' lexicographically, seed users always
 * sort first → the existing cursor query `WHERE userId > :cursor` works seamlessly
 * across both data sources.
 *
 * The API supports cursor-based pagination only. Add/Delete are NOT on the
 * server — those are local operations against Room.
 */
class FakeUserApi {

    data class ApiUser(
        val id: String,
        val username: String,
    )

    data class UsersPageResponse(
        val users: List<ApiUser>,
        val nextCursor: String?,   // null when no more pages
    )

    // 50 seed users with IDs in the SAME format as RegisterUsecase:
    //   "<timestamp><uuid-from-username>"
    //
    // Using a past base timestamp (Jan 1, 2020 = 1577836800000L) so seed IDs sort
    // BEFORE any locally-added user (whose timestamp will be the current time).
    // Each seed gets timestamp + n so they sort among themselves in insertion order.
    private val seedBaseTimestamp = 1577836800000L  // Jan 1 2020 UTC
    @OptIn(ExperimentalUuidApi::class)
    private val seedUsers: List<ApiUser> = (1..50).map { n ->
        val username = "seed_user_$n@example.com"
        val timestamp = seedBaseTimestamp + n
        val uuid = Uuid.random().toString()
        ApiUser(
            id = "$timestamp$uuid",
            username = username,
        )
    }

    /**
     * Cursor-based page fetch.
     *
     * @param cursor null = load from start. Otherwise return items strictly AFTER this id.
     * @param limit how many items to return at most.
     */
    suspend fun getUsersAfter(cursor: String?, limit: Int): UsersPageResponse {
        delay(1000)  // simulate network latency

        val startIndex = if (cursor == null) {
            0
        } else {
            // First index whose id is strictly greater than cursor.
            seedUsers.indexOfFirst { it.id > cursor }.let { idx ->
                if (idx == -1) seedUsers.size else idx
            }
        }

        val endIndex = (startIndex + limit).coerceAtMost(seedUsers.size)
        val page = if (startIndex >= seedUsers.size) {
            emptyList()
        } else {
            seedUsers.subList(startIndex, endIndex).toList()
        }

        val nextCursor = if (endIndex < seedUsers.size) page.lastOrNull()?.id else null


        val response =  UsersPageResponse(users = page, nextCursor = nextCursor)
        Logger.d(response.toString(), null, "getUsersAfter")
        return response
    }
}
