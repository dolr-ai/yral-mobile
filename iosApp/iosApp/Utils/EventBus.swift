//
//  EventBus.swift
//  iosApp
//
//  Created by Samarth Paboowal on 21/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Combine

final class EventBus: ObservableObject {
  let startPlayingTapped = PassthroughSubject<Void, Never>()
  let finishUploadingVideo = PassthroughSubject<Void, Never>()
  let playGamesToEarnMoreTapped = PassthroughSubject<Void, Never>()
  let walletTapped = PassthroughSubject<Void, Never>()
  let updatedUsername = PassthroughSubject<String, Never>()
  let dismissFullScreenCoverWallet = PassthroughSubject<Void, Never>()
  let dismissFullScreenCoverProfile = PassthroughSubject<Void, Never>()
}
