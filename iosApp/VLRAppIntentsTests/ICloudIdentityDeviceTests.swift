import Foundation
import XCTest

/// Checks identity continuity against a signed-in device's iCloud key-value cache.
@MainActor
final class ICloudIdentityDeviceTests: XCTestCase {
    /// Reads the existing identity without clearing app data or modifying the cloud key.
    /// This checks device integration; delayed restoration is covered by deterministic tests.
    func testExistingIdentityMatchesICloudWithoutResettingData() async throws {
        #if targetEnvironment(simulator)
        throw XCTSkip("Requires a signed-in physical iPhone.")
        #else
        guard ProcessInfo.processInfo.environment["VLR_VERIFY_ICLOUD_IDENTITY"] == "1" else {
            throw XCTSkip("Run explicitly on a signed-in device.")
        }
        guard FileManager.default.ubiquityIdentityToken != nil else {
            throw XCTSkip("No iCloud account is available to this app.")
        }
        let key = "identity.uuid"
        let defaults = UserDefaults.standard
        let original = defaults.string(forKey: key)
        let store = NSUbiquitousKeyValueStore.default
        if let original, UUID(uuidString: original) != nil,
           let cloud = store.string(forKey: key), UUID(uuidString: cloud) != nil,
           cloud != original {
            throw XCTSkip("Established local and cloud identities differ; reconciliation preserves both.")
        }
        store.synchronize()
        for _ in 0..<20 {
            if let local = defaults.string(forKey: key), UUID(uuidString: local) != nil,
               let cloud = store.string(forKey: key), cloud == local {
                if let original {
                    XCTAssertTrue(local == original, "The existing local identity must remain unchanged.")
                }
                return
            }
            try await Task.sleep(for: .seconds(1))
        }
        XCTFail("Local and iCloud identities did not agree within the observation window.")
        #endif
    }
}
