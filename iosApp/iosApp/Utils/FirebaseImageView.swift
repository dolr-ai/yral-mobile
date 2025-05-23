//
//  FirebaseImageView.swift
//  iosApp
//
//  Created by Samarth Paboowal on 28/04/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import FirebaseStorage

struct FirebaseImageView: View {
  let path: String
  @State private var imageData: Data?
  @State private var isLoading: Bool = false

  var body: some View {
    ZStack(alignment: .center) {
      if let data = imageData, let uiImage = UIImage(data: data) {
        Image(uiImage: uiImage)
          .resizable()
          .scaledToFill()
          .transition(.opacity.animation(.easeIn))
      } else if isLoading {
        ProgressView()
      }
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .task {
      await loadImage()
    }
  }

  @MainActor
  private func loadImage() async {
    if let cachedImageData = ImageCache.shared.data(forPath: path) {
      imageData = cachedImageData
      return
    }

    guard !isLoading else { return }
    isLoading = true

    do {
      let ref = Storage.storage().reference(withPath: path)
      let data = try await ref.data(maxSize: Constants.maxBytes)
      imageData = data
      ImageCache.shared.store(data, forPath: path)
    } catch {
      print("Firebase image download failed: \(error.localizedDescription)")
    }

    isLoading = false
  }
}

extension FirebaseImageView {
  enum Constants {
    static let maxBytes: Int64 = 4 * 1024 * 1024
  }
}
