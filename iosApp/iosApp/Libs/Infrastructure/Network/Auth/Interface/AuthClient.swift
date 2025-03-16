import Foundation

protocol AuthClient {
  var identity: DelegatedIdentity? { get }
  var principal: Principal? { get }
  var principalString: String? { get }
  func initialize() async throws
  func refreshAuthIfNeeded(using cookie: HTTPCookie) async throws
  func generateNewDelegatedIdentity() throws -> DelegatedIdentity
  func generateNewDelegatedIdentityWireOneHour() throws -> DelegatedIdentityWire
}
