//
//  EventBus.swift
//  iosApp
//
//  Created by Samarth Paboowal on 21/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Combine

final class EventBus: ObservableObject {
  let finishUploadingVideo = PassthroughSubject<Void, Never>()
}
