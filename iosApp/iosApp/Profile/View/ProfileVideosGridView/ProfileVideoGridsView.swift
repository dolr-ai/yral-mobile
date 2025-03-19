//
//  ProfileVideoGridsView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 17/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct ProfileVideo: Identifiable {
  let id: UUID = UUID()
  let thumbnailURL: URL?
  let likesCount: Int
}

struct ProfileVideosGridView: View {
  let videos: [ProfileVideo]
  var onDelete: ((ProfileVideo) -> Void)?
  var onVideoTapped: ((ProfileVideo) -> Void)?

  private let columns = [
    GridItem(.flexible(), spacing: Constants.gridItemSpacing),
    GridItem(.flexible(), spacing: Constants.gridItemSpacing)
  ]

  var body: some View {
    LazyVGrid(columns: columns, spacing: Constants.verticalSpacing) {
      ForEach(videos) { video in
        ZStack(alignment: .bottomLeading) {
          VideoThumbnailView(video: video)
            .onTapGesture {
              onVideoTapped?(video)
            }

          HStack {
            Text("\(video.likesCount)")
              .foregroundColor(.white)
              .font(.footnote)
              .padding(.leading, Constants.buttonHorizontalPadding)
            Spacer()
            Button { onDelete?(video) }
            label: {
              Image(Constants.deleteImageName)
            }
            .padding(.trailing, Constants.buttonHorizontalPadding)
          }
          .padding(.bottom, Constants.bottomPadding)
        }
        .background(Color.gray.opacity(0.3))
        .cornerRadius(Constants.thumbnailCornerRadius)
      }
    }
  }
}

extension ProfileVideosGridView {
  enum Constants {
    static let gridItemSpacing: CGFloat = 16.0
    static let verticalSpacing: CGFloat = 16.0

    static let thumbnailCornerRadius: CGFloat = 8.0
    static let buttonHorizontalPadding: CGFloat = 12.0
    static let bottomPadding: CGFloat = 12.0

    static let deleteImageName = "delete_video_profile"
  }
}

struct VideoThumbnailView: View {
  let video: ProfileVideo

  var body: some View {
    GeometryReader { proxy in
      if let url = video.thumbnailURL {
        AsyncImage(url: url) { phase in
          switch phase {
          case .empty:
            ProgressView()
              .frame(width: proxy.size.width, height: proxy.size.height)
          case .success(let image):
            image
              .resizable()
              .scaledToFill()
              .frame(width: proxy.size.width, height: proxy.size.height)
              .clipped()
          case .failure:
            Color.gray
              .overlay(Text("Failed to load").foregroundColor(.white))
          @unknown default:
            EmptyView()
          }
        }
      } else {
        Color.gray
          .overlay(Text("No Thumbnail").foregroundColor(.white))
      }
    }
    .aspectRatio(Constants.thumbnailAspectRatio, contentMode: .fit)
  }
}

extension VideoThumbnailView {
  enum Constants {
    static let thumbnailAspectRatio: CGFloat = 0.75
  }
}
