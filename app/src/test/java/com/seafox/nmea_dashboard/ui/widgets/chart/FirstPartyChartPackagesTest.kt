package com.seafox.nmea_dashboard.ui.widgets.chart

import com.seafox.nmea_dashboard.data.BillingCatalog
import com.seafox.nmea_dashboard.data.EntitlementSnapshot
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FirstPartyChartPackagesTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun premiumPackRequiresLicenseWhenNotOwned() {
        val pkg = FirstPartyChartPackages.offlinePackages(
            entitlementSnapshot = EntitlementSnapshot(),
        ).single()

        assertEquals(BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PACK_ID, pkg.id)
        assertEquals(FirstPartyChartPackages.PROVIDER_ID, pkg.providerId)
        assertEquals(ChartPackageLicenseStatus.licenseRequired, pkg.licenseStatus)
        assertEquals(ChartPackageValidationStatus.unknown, pkg.validationStatus)
        assertNull(pkg.localPath)
    }

    @Test
    fun ownedPremiumPackIsLicensedButIncompleteUntilPackageExists() {
        val pkg = FirstPartyChartPackages.offlinePackages(
            entitlementSnapshot = EntitlementSnapshot(
                ownedChartPackIds = setOf(BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PACK_ID),
            ),
        ).single()

        assertEquals(ChartPackageLicenseStatus.licensed, pkg.licenseStatus)
        assertEquals(ChartPackageValidationStatus.incomplete, pkg.validationStatus)
        assertTrue(ChartProviderCapability.rasterTiles in pkg.capabilities)
        assertTrue(ChartProviderCapability.offlinePackages in pkg.capabilities)
    }

    @Test
    fun ownedPremiumPackWithLocalPathIsRenderablePackageCandidate() {
        val pkg = FirstPartyChartPackages.offlinePackages(
            entitlementSnapshot = EntitlementSnapshot(
                ownedChartPackIds = setOf(BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PACK_ID),
            ),
            localPathsByPackageId = mapOf(
                BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PACK_ID to "/app/seaCHART/premium/de-coast.mbtiles",
            ),
        ).single()

        assertEquals(ChartPackageLicenseStatus.licensed, pkg.licenseStatus)
        assertEquals(ChartPackageValidationStatus.valid, pkg.validationStatus)
        assertEquals("/app/seaCHART/premium/de-coast.mbtiles", pkg.localPath)
    }

    @Test
    fun discoversInstalledPremiumPackFromCanonicalFileName() {
        val root = temporaryFolder.newFolder("premium")
        val installed = File(root, "seafox-premium-de-coast.mbtiles").apply {
            writeText("not-empty")
        }

        val paths = FirstPartyChartPackages.discoverLocalPaths(listOf(root))

        assertEquals(
            installed.absolutePath,
            paths[BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PACK_ID],
        )
    }

    @Test
    fun expiredSnapshotMarksPremiumPackExpired() {
        val pkg = FirstPartyChartPackages.offlinePackages(
            entitlementSnapshot = EntitlementSnapshot(
                ownedChartPackIds = setOf(BillingCatalog.SEAFOX_PREMIUM_DE_COAST_PACK_ID),
                validUntilEpochMs = 1_000L,
            ),
            nowEpochMs = 1_001L,
        ).single()

        assertEquals(ChartPackageLicenseStatus.expired, pkg.licenseStatus)
        assertEquals(ChartPackageValidationStatus.unknown, pkg.validationStatus)
        assertEquals(1_000L, pkg.expiresAtEpochMs)
    }
}
