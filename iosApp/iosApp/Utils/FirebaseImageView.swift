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
  let fallbackImage: String?
  @State private var imageData: Data?
  @State private var isLoading: Bool = false

  init(path: String, fallbackImage: String? = nil) {
    self.path = path
    self.fallbackImage = fallbackImage
    _imageData = State(initialValue: YralCache.shared.data(forPath: path))
  }

  var body: some View {
    ZStack(alignment: .center) {
      if let data = imageData, let uiImage = UIImage(data: data) {
        Image(uiImage: uiImage)
          .resizable()
          .scaledToFill()
      } else if isLoading {
        ProgressView()
      } else if let fallbackImageForSmiley = fallbackImage {
        Text(fallbackImageForSmiley)
          .font(.system(size: Constants.fallbackImageSize))
      }
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .onAppear {
      Task {
        await loadImage()
      }
    }
  }

  @MainActor
  private func loadImage() async {
    if let cachedImageData = YralCache.shared.data(forPath: path) {
      imageData = cachedImageData
      return
    }

    guard !isLoading else { return }
    isLoading = true

    do {
      let ref = Storage.storage().reference(withPath: path)
      let data = try await ref.data(maxSize: Constants.maxBytes)
      imageData = data
      YralCache.shared.store(data, forPath: path)
    } catch {
      print("Firebase image download failed: \(error.localizedDescription)")
    }

    isLoading = false
  }
}

extension FirebaseImageView {
  enum Constants {
    static let maxBytes: Int64 = 4 * 1024 * 1024
    static let fallbackImageSize = 36.0
  }
}
