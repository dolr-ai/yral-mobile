//
//  UploadViewModel.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 04/03/25.
//  Copyright © 2025 orgName. All rights reserved.
//
import Foundation

enum UploadViewState {
  case initalized
  case loading
  case success
  case failure(Error)
}

enum UploadViewEvent {
  case uploadPressed
  case uploadProgressUpdated(Double)
}

class UploadViewModel: ObservableObject {
  let getUploadEndpointUseCase: GetUploadEndpointUseCase
  var uploadEndpointResponse: UploadEndpointResponse!

  @Published var state: UploadViewState = .initalized
  @Published var event: UploadViewEvent?

  init(getUploadEndpointUseCase: GetUploadEndpointUseCase) {
    self.getUploadEndpointUseCase = getUploadEndpointUseCase
  }

  func getUploadEndpoint() async {
    let result = await getUploadEndpointUseCase.execute()
    switch result {
    case .success(let success):
      self.uploadEndpointResponse = success
    case .failure(let failure):
      print(failure)
    }
  }
}
