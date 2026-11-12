package io.github.aulteran.driveloom.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class ImagesTest {
    @Test fun `images are sampled down to stay near the edge limit`() {
        assertEquals(1, Images.sampleSize(1200, 800, 2048))
        assertEquals(1, Images.sampleSize(4000, 3000, 2048))
        assertEquals(2, Images.sampleSize(4096, 3072, 2048))
        assertEquals(4, Images.sampleSize(12000, 9000, 2048))
        assertEquals(1, Images.sampleSize(0, 0, 2048))
    }
}
