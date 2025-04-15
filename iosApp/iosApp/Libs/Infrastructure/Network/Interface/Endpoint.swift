//
//  Endpoint.swift
//  iosApp
//
//  Created by Sarvesh Sharma on 11/12/24.
//  Copyright © 2024 orgName. All rights reserved.
//
import Foundation

enum Tranport {
  case http
  case grpc
}

enum HTTPMethod: String {
  case get
  case post
  case put
  case delete = "DELETE"
}

struct Endpoint {
  let transport: Tranport
  let name: String

  // HTTP fields
  let baseURL: URL?
  let path: String?
  let httpMethod: HTTPMethod?
  let headers: [String: String]?
  let queryItems: [URLQueryItem]?
  let httpBody: Data?

  // grpc fields
  let serviceName: String?
  let methodName: String?
  let grpcRequestData: Data?

  // HTTP initializer
  init(http name: String,
       baseURL: URL,
       path: String,
       method: HTTPMethod,
       queryItems: [URLQueryItem]? = nil,
       headers: [String: String]? = nil,
       body: Data? = nil) {
    self.transport = .http
    self.name = name
    self.baseURL = baseURL
    self.path = path
    self.httpMethod = method
    self.queryItems = queryItems
    self.headers = headers
    self.httpBody = body

    // Unused for HTTP
    self.serviceName = nil
    self.methodName = nil
    self.grpcRequestData = nil
  }

  // gRPC initializer
  init(grpc name: String,
       serviceName: String,
       methodName: String,
       requestData: Data) {
    self.transport = .grpc
    self.name = name
    self.serviceName = serviceName
    self.methodName = methodName
    self.grpcRequestData = requestData

    // Unused for gRPC
    self.baseURL = nil
    self.path = nil
    self.httpMethod = nil
    self.queryItems = nil
    self.headers = nil
    self.httpBody = nil
  }
}
