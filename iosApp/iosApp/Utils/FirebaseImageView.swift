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
        ProgressView("Loading…")
      }
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .onAppear {
      loadImage()
    }
  }

  private func loadImage() {
    guard !isLoading else { return }
    isLoading = true

    let ref = Storage.storage().reference(withPath: path)
    ref.getData(maxSize: Constants.maxBytes, completion: { data, error in
      guard let data = data, error == nil else {
        DispatchQueue.main.async {
          self.isLoading = false
        }
        return
      }

      DispatchQueue.main.async {
        self.imageData = data
        self.isLoading = false
      }
    })
  }
}

extension FirebaseImageView {
  enum Constants {
    static let maxBytes: Int64 = 4 * 1024 * 1024
  }
}
