import SwiftUI
import secp256k1

struct ContentView: View {
  //	let greet = Greeting().greet()

  var body: some View {
    Text("greet")
      .task {
        do {
          guard let cookie = HTTPCookieStorage.shared.cookies?.first(where: { $0.name == "user-identity"}) else { return }
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
      if let ec_key = get_jwk_ec_key(jsonString) {
        let identity = get_secp256k1_identity(ec_key)
        print("Ec Key: \(ec_key), identity: \(identity)")
      }
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

    let task = URLSession.shared.dataTask(with: request) { data, response, error in
      if let error = error {
        print("Error:", error)
        return
      }
      guard let httpResponse = response as? HTTPURLResponse else { return }
      if httpResponse.statusCode == 200, let data = data, let responseString = String(data: data, encoding: .utf8) {
        if let cookies = HTTPCookieStorage.shared.cookies(for: url), let userIdentity = cookies.first(where: { $0.name == "user-identity"}) {
          HTTPCookieStorage.shared.setCookie(userIdentity)
        } else {
          print("No cookies found for this URL")
        }
      }
    }
    task.resume()
  }

  func extractIdentity(from cookie: HTTPCookie) async throws {
    do {
      guard let url = URL(string: "https://yral.com/api/extract_identity") else {
        print("Invalid URL")
        return
      }
      var request = URLRequest(url: url)
      let cookieHeader = "\(cookie.name)=\(cookie.value)"
      request.setValue(cookieHeader, forHTTPHeaderField: "Cookie")
      request.httpMethod = "POST"
      request.addValue("application/json", forHTTPHeaderField: "Content-Type")
      request.httpBody = "{}".data(using: .utf8)

      let task = URLSession.shared.dataTask(with: request) { data, response, error in
        if let error = error {
          print("Error:", error)
          return
        }
        guard let httpResponse = response as? HTTPURLResponse else { return }
        if let data = data, let responseString = String(data: data, encoding: .utf8) {
          Task {
            try await handleExtractIdentityResponse(from: data)
          }
          do {
          } catch {
            print(error)
          }
        }
      }
      task.resume()
    }
  }

  func handleExtractIdentityResponse(from data: Data) async throws {
    try data.withUnsafeBytes { (buffer: UnsafeRawBufferPointer) in
      if buffer.count > 0 {
        let uint8Buffer = buffer.bindMemory(to: UInt8.self)
        let delegatedIdentity = try delegated_identity_from_bytes(uint8Buffer)
        print(delegatedIdentity)
      } else {
        print("Received empty data.")
      }
    }
  }

}

struct ContentView_Previews: PreviewProvider {
  static var previews: some View {
    ContentView()
  }
}
