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

  var body: some View {
    Group {
      if let imageData, let uiImage = UIImage(data: imageData) {
        Image(uiImage: uiImage)
          .resizable()
          .scaledToFill()
      } else {
        Image(ImageResource(name: Constants.defaultImage, bundle: .main))
          .resizable()
          .scaledToFit()
      }
    }
    .onAppear {
      loadImage()
    }
  }

  private func loadImage() {
    guard let url else { return }
    URLSession.shared.dataTask(with: url) { data, _, error in
      guard let data, error == nil else { return }
      DispatchQueue.main.async {
        self.imageData = data
      }
    }.resume()
  }
}

extension URLImage {
  public enum Constants {
    static let defaultImage = "default_profile"
  }
}
