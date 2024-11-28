import SwiftUI
import secp256k1

struct ContentView: View {
  //	let greet = Greeting().greet()

  var body: some View {
    Text("greet")
      .task {
        do {
          guard let cookie = HTTPCookieStorage.shared.cookies?.first(where: { $0.name == "user-identity"}) else {
            Task {
              do {
                try await getAuthCookie()
                if let cookie = HTTPCookieStorage.shared.cookies?.first(where: { $0.name == "user-identity"}) {
                  try await extractIdentity(from: cookie)
                }
              } catch {
                print(error)
              }
            }
            return
          }
          try await extractIdentity(from: cookie)
        } catch {
          print("Error: \(error)")
        }
      }
  }

  func getAuthCookie() async throws {
    let privateKey = try secp256k1.Signing.PrivateKey(format: .uncompressed)
    let publicKeyData = privateKey.publicKey.dataRepresentation

    let xData = publicKeyData[1...32].base64URLEncodedString()
    let yData = publicKeyData[33...64].base64URLEncodedString()
    let dData = privateKey.dataRepresentation.base64URLEncodedString()

    // Step 3: Create the JWK
    let jwk: [String: Any] = [
      "kty": "EC",
      "crv": "secp256k1",
      "x": xData,
      "y": yData,
      "d": dData
    ]
    if let jsonData = try? JSONSerialization.data(withJSONObject: jwk, options: .prettyPrinted),
       let jsonString = String(data: jsonData, encoding: .utf8) {
      let jwkEcKey = try get_jwk_ec_key(jsonString)
      let identity = try get_secp256k1_identity(jwkEcKey)
      print("Ec Key: \(jwkEcKey), identity: \(String(describing: identity))")
    }
    let payload: [String: Any] = [
      "anonymous_identity": jwk
    ]

    let jsonData = try JSONSerialization.data(withJSONObject: payload, options: [])

    guard let url = URL(string: "https://yral.com/api/set_anonymous_identity_cookie") else {
      print("Invalid URL")
      return
    }

    var request = URLRequest(url: url)
    request.httpMethod = "POST"
    request.addValue("application/json", forHTTPHeaderField: "Content-Type")
    request.httpBody = jsonData

    let task = URLSession.shared.dataTask(with: request) { _, response, error in
      if let error = error {
        print("Error:", error)
        return
      }
      guard let httpResponse = response as? HTTPURLResponse else { return }
      if httpResponse.statusCode == 200 {
        if let cookies = HTTPCookieStorage.shared.cookies(for: url),
           let userIdentity = cookies.first(where: { $0.name == "user-identity"}) {
          HTTPCookieStorage.shared.setCookie(userIdentity)
        } else {
          print("No cookies found for this URL")
        }
      }
    }
    task.resume()
  }

  func extractIdentity(from cookie: HTTPCookie) async throws {
    guard let url = URL(string: "https://yral.com/api/extract_identity") else {
      print("Invalid URL")
      return
    }
    var request = URLRequest(url: url)
    let cookieHeader = "\(cookie.name)=\(cookie.value)"
    request.setValue(cookieHeader, forHTTPHeaderField: "Cookie")
    request.httpMethod = "POST"
    request.addValue("application/json", forHTTPHeaderField: "Content-Type")
    request.httpBody = Data("{}".utf8)

    let task = URLSession.shared.dataTask(with: request) { data, response, error in
      if let error = error {
        print("Error:", error)
        return
      }
      guard response is HTTPURLResponse else { return }
      if let data = data {
        Task {
          try await handleExtractIdentityResponse(from: data)
        }
      }
    }
    task.resume()
  }

  func handleExtractIdentityResponse(from data: Data) async throws {
    try data.withUnsafeBytes { (buffer: UnsafeRawBufferPointer) in
      if buffer.count > 0 {
        let uint8Buffer = buffer.bindMemory(to: UInt8.self)
        let wire = try delegated_identity_wire_from_bytes(uint8Buffer)
        let delegatedIdentity = try delegated_identity_from_bytes(uint8Buffer)
        Task {
          try await deployCanister(with: wire, identity: delegatedIdentity)
        }
      } else {
        print("Received empty data.")
      }
    }
  }

  func deployCanister(with wire: DelegatedIdentityWire, identity: DelegatedIdentity) async throws {
    let canistersWrapper = try await authenticate_with_network(wire, nil)
    let principal = get_canister_principal(canistersWrapper)
    let service = try Service(principal, identity)
    let result = try await service.get_last_access_time()
    let systemTime = extract_time_as_double(result)
    print(systemTime)
  }
}

struct ContentView_Previews: PreviewProvider {
  static var previews: some View {
    ContentView()
  }
}
