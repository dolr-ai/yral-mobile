//
//  ProfileVideoGridsView.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 17/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI

struct ProfileVideosGridView: View {
  let videos: [ProfileVideoInfo]
  var onDelete: ((ProfileVideoInfo) -> Void)?
  var onVideoTapped: ((ProfileVideoInfo) -> Void)?

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
              .font(Constants.likeTextFont)
              .foregroundColor(Constants.likeTextColor)
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

    static let likeTextFont = YralFont.pt14.medium.swiftUIFont
    static let likeTextColor = YralColor.grey50.swiftUIColor
  }
}

struct VideoThumbnailView: View {
  let video: ProfileVideoInfo

  var body: some View {
    GeometryReader { proxy in
      AsyncImage(url: video.thumbnailUrl) { phase in
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
    }
    .aspectRatio(Constants.thumbnailAspectRatio, contentMode: .fit)
  }
}

extension VideoThumbnailView {
  enum Constants {
    static let thumbnailAspectRatio: CGFloat = 0.75
  }
}
