import SwiftUI
import secp256k1

struct ContentView: View {
  //	let greet = Greeting().greet()

  var body: some View {
    Text("greet")
      .task {
        do {
          try await getAuthCookie()
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
      if let httpResponse = response as? HTTPURLResponse {
        print("Response status code:", httpResponse.statusCode)
      }
      if let data = data, let responseString = String(data: data, encoding: .utf8) {
        print("Response data:", responseString)
      }
    }
    task.resume()
  }
}

struct ContentView_Previews: PreviewProvider {
  static var previews: some View {
    ContentView()
  }
}
