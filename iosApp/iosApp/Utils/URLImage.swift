//
//  URLImage.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/01/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import SwiftUI

struct URLImage: View {
  let url: URL?
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
      } else {
        Image(ImageResource(name: Constants.defaultImage, bundle: .main))
          .resizable()
          .scaledToFit()
      }
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .onAppear {
      loadImage()
    }
  }

  private func loadImage() {
    guard let url = url else { return }
    isLoading = true
    URLSession.shared.dataTask(with: url) { data, _, error in
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
    }.resume()
  }
}

extension URLImage {
  enum Constants {
    static let defaultImage = "default_profile"
  }
}
