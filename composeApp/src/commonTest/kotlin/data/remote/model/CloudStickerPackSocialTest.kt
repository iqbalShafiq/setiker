package data.remote.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CloudStickerPackSocialTest {

    private fun samplePack(
        isLiked: Boolean? = null,
        isSaved: Boolean? = null
    ) = CloudStickerPack(
        id = "pack-1",
        ownerId = "owner-1",
        name = "Test",
        visibility = "PUBLIC",
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z",
        isLiked = isLiked,
        isSaved = isSaved,
        liked = isLiked,
        saved = isSaved
    )

    @Test
    fun `applySocialUpdate preserves liked when save response omits liked`() {
        val pack = samplePack(isLiked = true, isSaved = false)

        val updated = pack.applySocialUpdate(
            PackSocialStateData(saved = true, saveCount = 1)
        )

        assertTrue(updated.userHasLiked())
        assertTrue(updated.userHasSaved())
    }

    @Test
    fun `applySocialUpdate preserves saved when like response omits saved`() {
        val pack = samplePack(isLiked = false, isSaved = true)

        val updated = pack.applySocialUpdate(
            PackSocialStateData(liked = true, likeCount = 3)
        )

        assertTrue(updated.userHasLiked())
        assertTrue(updated.userHasSaved())
    }

    @Test
    fun `withOptimisticLike toggles liked and count`() {
        val pack = samplePack(isLiked = false, isSaved = false)

        val liked = pack.withOptimisticLike(liked = true)
        assertTrue(liked.userHasLiked())
        assertEquals(1, liked.likeCount)

        val unliked = liked.withOptimisticLike(liked = false)
        assertFalse(unliked.userHasLiked())
        assertEquals(0, unliked.likeCount)
    }

    @Test
    fun `withOptimisticFollowOwner toggles following and owner follower count`() {
        val pack = samplePack().copy(
            owner = CloudOwner(id = "owner-1", followerCount = 10),
            isFollowingOwner = false
        )

        val followed = pack.withOptimisticFollowOwner(following = true)
        assertTrue(followed.userIsFollowingOwner())
        assertEquals(11, followed.owner?.followerCount)

        val unfollowed = followed.withOptimisticFollowOwner(following = false)
        assertFalse(unfollowed.userIsFollowingOwner())
        assertEquals(10, unfollowed.owner?.followerCount)
    }

    @Test
    fun `applySocialUpdate can clear liked when api explicitly returns false`() {
        val pack = samplePack(isLiked = true, isSaved = false)

        val updated = pack.applySocialUpdate(
            PackSocialStateData(liked = false, likeCount = 0)
        )

        assertFalse(updated.userHasLiked())
    }
}
