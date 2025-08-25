//
//  AIVideoRepositoryProtocol.swift
//  iosApp
//
//  Created by Samarth Paboowal on 18/08/25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Foundation

protocol AIVideoRepositoryProtocol {
  func getProviders() async -> Result<AIVideoProviderMetaResponse, AIVideoProviderError>
  func getRateLimitStatus() async -> Result<RateLimitStatus, RateLimitStatusError>
  func generateVideo(for request: GenerateVideoMetaRequest) async -> Result<GenerateVideoResponse, GenerateVideoError>
  func getGenerateVideoStatus(for counter: UInt64) async -> Result<String, GenerateVideoStatusError>
  func uploadVideo(with url: String) async -> Result<Void, UploadAIVideoError>
}
