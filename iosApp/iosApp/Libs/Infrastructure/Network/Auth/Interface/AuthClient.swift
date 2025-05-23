import Foundation

protocol AuthClient {
  var identity: DelegatedIdentity? { get }
  var canisterPrincipal: Principal? { get }
  var canisterPrincipalString: String? { get }
  var userPrincipal: Principal? { get }
  var userPrincipalString: String? { get }
  func initialize() async throws
  func refreshAuthIfNeeded(using cookie: HTTPCookie) async throws
  func generateNewDelegatedIdentity() throws -> DelegatedIdentity
  func generateNewDelegatedIdentityWireOneHour() throws -> DelegatedIdentityWire
}
