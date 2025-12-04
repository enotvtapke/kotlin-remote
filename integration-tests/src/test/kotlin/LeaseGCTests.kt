import kotlinx.remote.classes.RemoteInstancesPool
import kotlinx.remote.classes.lease.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for the lease-based garbage collection components.
 * 
 * These tests verify the LeaseManager and LeaseRenewalClient logic
 * without using the remote plugin (to avoid compiler conflicts).
 */
class LeaseGCTests {
    
    @BeforeEach
    fun beforeEach() {
        // Configure LeaseManager with short durations for testing
        LeaseManager.configure(LeaseConfig(
            leaseDurationMs = 500,
            cleanupIntervalMs = 100,
            gracePeriodMs = 100
        ))
        LeaseManager.clear()
        LeaseRenewalClient.clear()
    }
    
    @AfterEach
    fun afterEach() {
        LeaseManager.stopCleanupJob()
        LeaseRenewalClient.stopRenewalJob()
        LeaseManager.clear()
        LeaseRenewalClient.clear()
    }
    
    @Test
    fun `LeaseManager creates and tracks leases`() {
        // Create a lease
        val leaseInfo = LeaseManager.createLease(1L)
        
        assertNotNull(leaseInfo)
        assertEquals(1L, leaseInfo.instanceId)
        assertTrue(leaseInfo.expirationTimeMs > 0)
        assertEquals(1, LeaseManager.leaseCount())
        assertTrue(LeaseManager.hasActiveLease(1L))
    }
    
    @Test
    fun `LeaseManager renews leases`() {
        // Add instance to pool (required for renewal)
        RemoteInstancesPool.instances[1L] = "test"
        
        // Create a lease
        val initialLease = LeaseManager.createLease(1L)
        
        // Wait a bit
        Thread.sleep(50)
        
        // Renew the lease
        val response = LeaseManager.renewLeases(LeaseRenewalRequest(listOf(1L)))
        
        assertEquals(1, response.renewedLeases.size)
        assertEquals(0, response.failedIds.size)
        assertTrue(response.renewedLeases[0].expirationTimeMs >= initialLease.expirationTimeMs)
    }
    
    @Test
    fun `LeaseManager fails to renew non-existent leases`() {
        // Try to renew a non-existent lease
        val response = LeaseManager.renewLeases(LeaseRenewalRequest(listOf(999L)))
        
        assertEquals(0, response.renewedLeases.size)
        assertEquals(1, response.failedIds.size)
        assertEquals(999L, response.failedIds[0])
    }
    
    @Test
    fun `LeaseManager releases leases`() {
        // Create a lease
        LeaseManager.createLease(1L)
        assertTrue(LeaseManager.hasActiveLease(1L))
        
        // Release the lease
        LeaseManager.releaseLeases(LeaseReleaseRequest(listOf(1L)))
        
        // Lease should no longer be active
        assertFalse(LeaseManager.hasActiveLease(1L))
    }
    
    @Test
    fun `LeaseManager cleans up expired instances`() {
        // Add an instance and create a lease
        RemoteInstancesPool.instances[1L] = "test instance"
        LeaseManager.createLease(1L)
        
        assertTrue(RemoteInstancesPool.hasInstance(1L))
        assertTrue(LeaseManager.hasActiveLease(1L))
        
        // Wait for lease to expire
        Thread.sleep(700) // lease duration (500) + grace period (100) + buffer
        
        // Cleanup expired instances
        val cleanedUp = LeaseManager.cleanupExpiredInstances()
        
        assertTrue(cleanedUp > 0)
        assertFalse(RemoteInstancesPool.hasInstance(1L))
        assertFalse(LeaseManager.hasActiveLease(1L))
    }
    
    @Test
    fun `LeaseRenewalClient tracks stubs`() {
        // Register a stub ID
        LeaseRenewalClient.registerStubId(1L)
        
        assertTrue(LeaseRenewalClient.isTracking(1L))
        assertEquals(1, LeaseRenewalClient.trackedCount())
        
        // Clear
        LeaseRenewalClient.clear()
        
        assertEquals(0, LeaseRenewalClient.trackedCount())
        assertFalse(LeaseRenewalClient.isTracking(1L))
    }
    
    @Test
    fun `LeaseConfig has sensible defaults`() {
        val config = LeaseConfig()
        
        assertEquals(30_000L, config.leaseDurationMs)
        assertEquals(10_000L, config.cleanupIntervalMs)
        assertEquals(5_000L, config.gracePeriodMs)
    }
    
    @Test
    fun `LeaseRenewalClientConfig has sensible defaults`() {
        val config = LeaseRenewalClientConfig()
        
        assertEquals(10_000L, config.renewalIntervalMs)
    }
    
    @Test
    fun `multiple leases can be managed independently`() {
        // Create multiple leases
        LeaseManager.createLease(1L)
        LeaseManager.createLease(2L)
        LeaseManager.createLease(3L)
        
        assertEquals(3, LeaseManager.leaseCount())
        assertTrue(LeaseManager.hasActiveLease(1L))
        assertTrue(LeaseManager.hasActiveLease(2L))
        assertTrue(LeaseManager.hasActiveLease(3L))
        
        // Release one
        LeaseManager.releaseLeases(LeaseReleaseRequest(listOf(2L)))
        
        assertTrue(LeaseManager.hasActiveLease(1L))
        assertFalse(LeaseManager.hasActiveLease(2L))
        assertTrue(LeaseManager.hasActiveLease(3L))
    }
    
    @Test
    fun `client ID is tracked in leases`() {
        val clientId = "test-client"
        val leaseInfo = LeaseManager.createLease(1L, clientId)
        
        assertEquals(clientId, leaseInfo.clientId)
    }
}
