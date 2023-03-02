/* tslint:disable */
/* eslint-disable */
/**
 * Halo Next API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 2.0.0
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import type { Configuration } from '../configuration'
import type { AxiosPromise, AxiosInstance, AxiosRequestConfig } from 'axios'
import globalAxios from 'axios'
// Some imports not used depending on template conditions
// @ts-ignore
import {
  DUMMY_BASE_URL,
  assertParamExists,
  setApiKeyToObject,
  setBasicAuthToObject,
  setBearerAuthToObject,
  setOAuthToObject,
  setSearchParams,
  serializeDataIfNeeded,
  toPathString,
  createRequestFunction,
} from '../common'
// @ts-ignore
import { BASE_PATH, COLLECTION_FORMATS, RequestArgs, BaseAPI, RequiredError } from '../base'
// @ts-ignore
import { SinglePage } from '../models'
// @ts-ignore
import { SinglePageList } from '../models'
/**
 * ContentHaloRunV1alpha1SinglePageApi - axios parameter creator
 * @export
 */
export const ContentHaloRunV1alpha1SinglePageApiAxiosParamCreator = function (configuration?: Configuration) {
  return {
    /**
     * Create content.halo.run/v1alpha1/SinglePage
     * @param {SinglePage} [singlePage] Fresh singlepage
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    createcontentHaloRunV1alpha1SinglePage: async (
      singlePage?: SinglePage,
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      const localVarPath = `/apis/content.halo.run/v1alpha1/singlepages`
      // use dummy base URL string because the URL constructor only accepts absolute URLs.
      const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL)
      let baseOptions
      if (configuration) {
        baseOptions = configuration.baseOptions
      }

      const localVarRequestOptions = { method: 'POST', ...baseOptions, ...options }
      const localVarHeaderParameter = {} as any
      const localVarQueryParameter = {} as any

      // authentication BasicAuth required
      // http basic authentication required
      setBasicAuthToObject(localVarRequestOptions, configuration)

      // authentication BearerAuth required
      // http bearer authentication required
      await setBearerAuthToObject(localVarHeaderParameter, configuration)

      localVarHeaderParameter['Content-Type'] = 'application/json'

      setSearchParams(localVarUrlObj, localVarQueryParameter)
      let headersFromBaseOptions = baseOptions && baseOptions.headers ? baseOptions.headers : {}
      localVarRequestOptions.headers = { ...localVarHeaderParameter, ...headersFromBaseOptions, ...options.headers }
      localVarRequestOptions.data = serializeDataIfNeeded(singlePage, localVarRequestOptions, configuration)

      return {
        url: toPathString(localVarUrlObj),
        options: localVarRequestOptions,
      }
    },
    /**
     * Delete content.halo.run/v1alpha1/SinglePage
     * @param {string} name Name of singlepage
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    deletecontentHaloRunV1alpha1SinglePage: async (
      name: string,
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      // verify required parameter 'name' is not null or undefined
      assertParamExists('deletecontentHaloRunV1alpha1SinglePage', 'name', name)
      const localVarPath = `/apis/content.halo.run/v1alpha1/singlepages/{name}`.replace(
        `{${'name'}}`,
        encodeURIComponent(String(name)),
      )
      // use dummy base URL string because the URL constructor only accepts absolute URLs.
      const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL)
      let baseOptions
      if (configuration) {
        baseOptions = configuration.baseOptions
      }

      const localVarRequestOptions = { method: 'DELETE', ...baseOptions, ...options }
      const localVarHeaderParameter = {} as any
      const localVarQueryParameter = {} as any

      // authentication BasicAuth required
      // http basic authentication required
      setBasicAuthToObject(localVarRequestOptions, configuration)

      // authentication BearerAuth required
      // http bearer authentication required
      await setBearerAuthToObject(localVarHeaderParameter, configuration)

      setSearchParams(localVarUrlObj, localVarQueryParameter)
      let headersFromBaseOptions = baseOptions && baseOptions.headers ? baseOptions.headers : {}
      localVarRequestOptions.headers = { ...localVarHeaderParameter, ...headersFromBaseOptions, ...options.headers }

      return {
        url: toPathString(localVarUrlObj),
        options: localVarRequestOptions,
      }
    },
    /**
     * Get content.halo.run/v1alpha1/SinglePage
     * @param {string} name Name of singlepage
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    getcontentHaloRunV1alpha1SinglePage: async (
      name: string,
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      // verify required parameter 'name' is not null or undefined
      assertParamExists('getcontentHaloRunV1alpha1SinglePage', 'name', name)
      const localVarPath = `/apis/content.halo.run/v1alpha1/singlepages/{name}`.replace(
        `{${'name'}}`,
        encodeURIComponent(String(name)),
      )
      // use dummy base URL string because the URL constructor only accepts absolute URLs.
      const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL)
      let baseOptions
      if (configuration) {
        baseOptions = configuration.baseOptions
      }

      const localVarRequestOptions = { method: 'GET', ...baseOptions, ...options }
      const localVarHeaderParameter = {} as any
      const localVarQueryParameter = {} as any

      // authentication BasicAuth required
      // http basic authentication required
      setBasicAuthToObject(localVarRequestOptions, configuration)

      // authentication BearerAuth required
      // http bearer authentication required
      await setBearerAuthToObject(localVarHeaderParameter, configuration)

      setSearchParams(localVarUrlObj, localVarQueryParameter)
      let headersFromBaseOptions = baseOptions && baseOptions.headers ? baseOptions.headers : {}
      localVarRequestOptions.headers = { ...localVarHeaderParameter, ...headersFromBaseOptions, ...options.headers }

      return {
        url: toPathString(localVarUrlObj),
        options: localVarRequestOptions,
      }
    },
    /**
     * List content.halo.run/v1alpha1/SinglePage
     * @param {number} [page] The page number. Zero indicates no page.
     * @param {number} [size] Size of one page. Zero indicates no limit.
     * @param {Array<string>} [labelSelector] Label selector for filtering.
     * @param {Array<string>} [fieldSelector] Field selector for filtering.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    listcontentHaloRunV1alpha1SinglePage: async (
      page?: number,
      size?: number,
      labelSelector?: Array<string>,
      fieldSelector?: Array<string>,
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      const localVarPath = `/apis/content.halo.run/v1alpha1/singlepages`
      // use dummy base URL string because the URL constructor only accepts absolute URLs.
      const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL)
      let baseOptions
      if (configuration) {
        baseOptions = configuration.baseOptions
      }

      const localVarRequestOptions = { method: 'GET', ...baseOptions, ...options }
      const localVarHeaderParameter = {} as any
      const localVarQueryParameter = {} as any

      // authentication BasicAuth required
      // http basic authentication required
      setBasicAuthToObject(localVarRequestOptions, configuration)

      // authentication BearerAuth required
      // http bearer authentication required
      await setBearerAuthToObject(localVarHeaderParameter, configuration)

      if (page !== undefined) {
        localVarQueryParameter['page'] = page
      }

      if (size !== undefined) {
        localVarQueryParameter['size'] = size
      }

      if (labelSelector) {
        localVarQueryParameter['labelSelector'] = labelSelector
      }

      if (fieldSelector) {
        localVarQueryParameter['fieldSelector'] = fieldSelector
      }

      setSearchParams(localVarUrlObj, localVarQueryParameter)
      let headersFromBaseOptions = baseOptions && baseOptions.headers ? baseOptions.headers : {}
      localVarRequestOptions.headers = { ...localVarHeaderParameter, ...headersFromBaseOptions, ...options.headers }

      return {
        url: toPathString(localVarUrlObj),
        options: localVarRequestOptions,
      }
    },
    /**
     * Update content.halo.run/v1alpha1/SinglePage
     * @param {string} name Name of singlepage
     * @param {SinglePage} [singlePage] Updated singlepage
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    updatecontentHaloRunV1alpha1SinglePage: async (
      name: string,
      singlePage?: SinglePage,
      options: AxiosRequestConfig = {},
    ): Promise<RequestArgs> => {
      // verify required parameter 'name' is not null or undefined
      assertParamExists('updatecontentHaloRunV1alpha1SinglePage', 'name', name)
      const localVarPath = `/apis/content.halo.run/v1alpha1/singlepages/{name}`.replace(
        `{${'name'}}`,
        encodeURIComponent(String(name)),
      )
      // use dummy base URL string because the URL constructor only accepts absolute URLs.
      const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL)
      let baseOptions
      if (configuration) {
        baseOptions = configuration.baseOptions
      }

      const localVarRequestOptions = { method: 'PUT', ...baseOptions, ...options }
      const localVarHeaderParameter = {} as any
      const localVarQueryParameter = {} as any

      // authentication BasicAuth required
      // http basic authentication required
      setBasicAuthToObject(localVarRequestOptions, configuration)

      // authentication BearerAuth required
      // http bearer authentication required
      await setBearerAuthToObject(localVarHeaderParameter, configuration)

      localVarHeaderParameter['Content-Type'] = 'application/json'

      setSearchParams(localVarUrlObj, localVarQueryParameter)
      let headersFromBaseOptions = baseOptions && baseOptions.headers ? baseOptions.headers : {}
      localVarRequestOptions.headers = { ...localVarHeaderParameter, ...headersFromBaseOptions, ...options.headers }
      localVarRequestOptions.data = serializeDataIfNeeded(singlePage, localVarRequestOptions, configuration)

      return {
        url: toPathString(localVarUrlObj),
        options: localVarRequestOptions,
      }
    },
  }
}

/**
 * ContentHaloRunV1alpha1SinglePageApi - functional programming interface
 * @export
 */
export const ContentHaloRunV1alpha1SinglePageApiFp = function (configuration?: Configuration) {
  const localVarAxiosParamCreator = ContentHaloRunV1alpha1SinglePageApiAxiosParamCreator(configuration)
  return {
    /**
     * Create content.halo.run/v1alpha1/SinglePage
     * @param {SinglePage} [singlePage] Fresh singlepage
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async createcontentHaloRunV1alpha1SinglePage(
      singlePage?: SinglePage,
      options?: AxiosRequestConfig,
    ): Promise<(axios?: AxiosInstance, basePath?: string) => AxiosPromise<SinglePage>> {
      const localVarAxiosArgs = await localVarAxiosParamCreator.createcontentHaloRunV1alpha1SinglePage(
        singlePage,
        options,
      )
      return createRequestFunction(localVarAxiosArgs, globalAxios, BASE_PATH, configuration)
    },
    /**
     * Delete content.halo.run/v1alpha1/SinglePage
     * @param {string} name Name of singlepage
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async deletecontentHaloRunV1alpha1SinglePage(
      name: string,
      options?: AxiosRequestConfig,
    ): Promise<(axios?: AxiosInstance, basePath?: string) => AxiosPromise<void>> {
      const localVarAxiosArgs = await localVarAxiosParamCreator.deletecontentHaloRunV1alpha1SinglePage(name, options)
      return createRequestFunction(localVarAxiosArgs, globalAxios, BASE_PATH, configuration)
    },
    /**
     * Get content.halo.run/v1alpha1/SinglePage
     * @param {string} name Name of singlepage
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async getcontentHaloRunV1alpha1SinglePage(
      name: string,
      options?: AxiosRequestConfig,
    ): Promise<(axios?: AxiosInstance, basePath?: string) => AxiosPromise<SinglePage>> {
      const localVarAxiosArgs = await localVarAxiosParamCreator.getcontentHaloRunV1alpha1SinglePage(name, options)
      return createRequestFunction(localVarAxiosArgs, globalAxios, BASE_PATH, configuration)
    },
    /**
     * List content.halo.run/v1alpha1/SinglePage
     * @param {number} [page] The page number. Zero indicates no page.
     * @param {number} [size] Size of one page. Zero indicates no limit.
     * @param {Array<string>} [labelSelector] Label selector for filtering.
     * @param {Array<string>} [fieldSelector] Field selector for filtering.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async listcontentHaloRunV1alpha1SinglePage(
      page?: number,
      size?: number,
      labelSelector?: Array<string>,
      fieldSelector?: Array<string>,
      options?: AxiosRequestConfig,
    ): Promise<(axios?: AxiosInstance, basePath?: string) => AxiosPromise<SinglePageList>> {
      const localVarAxiosArgs = await localVarAxiosParamCreator.listcontentHaloRunV1alpha1SinglePage(
        page,
        size,
        labelSelector,
        fieldSelector,
        options,
      )
      return createRequestFunction(localVarAxiosArgs, globalAxios, BASE_PATH, configuration)
    },
    /**
     * Update content.halo.run/v1alpha1/SinglePage
     * @param {string} name Name of singlepage
     * @param {SinglePage} [singlePage] Updated singlepage
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    async updatecontentHaloRunV1alpha1SinglePage(
      name: string,
      singlePage?: SinglePage,
      options?: AxiosRequestConfig,
    ): Promise<(axios?: AxiosInstance, basePath?: string) => AxiosPromise<SinglePage>> {
      const localVarAxiosArgs = await localVarAxiosParamCreator.updatecontentHaloRunV1alpha1SinglePage(
        name,
        singlePage,
        options,
      )
      return createRequestFunction(localVarAxiosArgs, globalAxios, BASE_PATH, configuration)
    },
  }
}

/**
 * ContentHaloRunV1alpha1SinglePageApi - factory interface
 * @export
 */
export const ContentHaloRunV1alpha1SinglePageApiFactory = function (
  configuration?: Configuration,
  basePath?: string,
  axios?: AxiosInstance,
) {
  const localVarFp = ContentHaloRunV1alpha1SinglePageApiFp(configuration)
  return {
    /**
     * Create content.halo.run/v1alpha1/SinglePage
     * @param {ContentHaloRunV1alpha1SinglePageApiCreatecontentHaloRunV1alpha1SinglePageRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    createcontentHaloRunV1alpha1SinglePage(
      requestParameters: ContentHaloRunV1alpha1SinglePageApiCreatecontentHaloRunV1alpha1SinglePageRequest = {},
      options?: AxiosRequestConfig,
    ): AxiosPromise<SinglePage> {
      return localVarFp
        .createcontentHaloRunV1alpha1SinglePage(requestParameters.singlePage, options)
        .then((request) => request(axios, basePath))
    },
    /**
     * Delete content.halo.run/v1alpha1/SinglePage
     * @param {ContentHaloRunV1alpha1SinglePageApiDeletecontentHaloRunV1alpha1SinglePageRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    deletecontentHaloRunV1alpha1SinglePage(
      requestParameters: ContentHaloRunV1alpha1SinglePageApiDeletecontentHaloRunV1alpha1SinglePageRequest,
      options?: AxiosRequestConfig,
    ): AxiosPromise<void> {
      return localVarFp
        .deletecontentHaloRunV1alpha1SinglePage(requestParameters.name, options)
        .then((request) => request(axios, basePath))
    },
    /**
     * Get content.halo.run/v1alpha1/SinglePage
     * @param {ContentHaloRunV1alpha1SinglePageApiGetcontentHaloRunV1alpha1SinglePageRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    getcontentHaloRunV1alpha1SinglePage(
      requestParameters: ContentHaloRunV1alpha1SinglePageApiGetcontentHaloRunV1alpha1SinglePageRequest,
      options?: AxiosRequestConfig,
    ): AxiosPromise<SinglePage> {
      return localVarFp
        .getcontentHaloRunV1alpha1SinglePage(requestParameters.name, options)
        .then((request) => request(axios, basePath))
    },
    /**
     * List content.halo.run/v1alpha1/SinglePage
     * @param {ContentHaloRunV1alpha1SinglePageApiListcontentHaloRunV1alpha1SinglePageRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    listcontentHaloRunV1alpha1SinglePage(
      requestParameters: ContentHaloRunV1alpha1SinglePageApiListcontentHaloRunV1alpha1SinglePageRequest = {},
      options?: AxiosRequestConfig,
    ): AxiosPromise<SinglePageList> {
      return localVarFp
        .listcontentHaloRunV1alpha1SinglePage(
          requestParameters.page,
          requestParameters.size,
          requestParameters.labelSelector,
          requestParameters.fieldSelector,
          options,
        )
        .then((request) => request(axios, basePath))
    },
    /**
     * Update content.halo.run/v1alpha1/SinglePage
     * @param {ContentHaloRunV1alpha1SinglePageApiUpdatecontentHaloRunV1alpha1SinglePageRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     */
    updatecontentHaloRunV1alpha1SinglePage(
      requestParameters: ContentHaloRunV1alpha1SinglePageApiUpdatecontentHaloRunV1alpha1SinglePageRequest,
      options?: AxiosRequestConfig,
    ): AxiosPromise<SinglePage> {
      return localVarFp
        .updatecontentHaloRunV1alpha1SinglePage(requestParameters.name, requestParameters.singlePage, options)
        .then((request) => request(axios, basePath))
    },
  }
}

/**
 * Request parameters for createcontentHaloRunV1alpha1SinglePage operation in ContentHaloRunV1alpha1SinglePageApi.
 * @export
 * @interface ContentHaloRunV1alpha1SinglePageApiCreatecontentHaloRunV1alpha1SinglePageRequest
 */
export interface ContentHaloRunV1alpha1SinglePageApiCreatecontentHaloRunV1alpha1SinglePageRequest {
  /**
   * Fresh singlepage
   * @type {SinglePage}
   * @memberof ContentHaloRunV1alpha1SinglePageApiCreatecontentHaloRunV1alpha1SinglePage
   */
  readonly singlePage?: SinglePage
}

/**
 * Request parameters for deletecontentHaloRunV1alpha1SinglePage operation in ContentHaloRunV1alpha1SinglePageApi.
 * @export
 * @interface ContentHaloRunV1alpha1SinglePageApiDeletecontentHaloRunV1alpha1SinglePageRequest
 */
export interface ContentHaloRunV1alpha1SinglePageApiDeletecontentHaloRunV1alpha1SinglePageRequest {
  /**
   * Name of singlepage
   * @type {string}
   * @memberof ContentHaloRunV1alpha1SinglePageApiDeletecontentHaloRunV1alpha1SinglePage
   */
  readonly name: string
}

/**
 * Request parameters for getcontentHaloRunV1alpha1SinglePage operation in ContentHaloRunV1alpha1SinglePageApi.
 * @export
 * @interface ContentHaloRunV1alpha1SinglePageApiGetcontentHaloRunV1alpha1SinglePageRequest
 */
export interface ContentHaloRunV1alpha1SinglePageApiGetcontentHaloRunV1alpha1SinglePageRequest {
  /**
   * Name of singlepage
   * @type {string}
   * @memberof ContentHaloRunV1alpha1SinglePageApiGetcontentHaloRunV1alpha1SinglePage
   */
  readonly name: string
}

/**
 * Request parameters for listcontentHaloRunV1alpha1SinglePage operation in ContentHaloRunV1alpha1SinglePageApi.
 * @export
 * @interface ContentHaloRunV1alpha1SinglePageApiListcontentHaloRunV1alpha1SinglePageRequest
 */
export interface ContentHaloRunV1alpha1SinglePageApiListcontentHaloRunV1alpha1SinglePageRequest {
  /**
   * The page number. Zero indicates no page.
   * @type {number}
   * @memberof ContentHaloRunV1alpha1SinglePageApiListcontentHaloRunV1alpha1SinglePage
   */
  readonly page?: number

  /**
   * Size of one page. Zero indicates no limit.
   * @type {number}
   * @memberof ContentHaloRunV1alpha1SinglePageApiListcontentHaloRunV1alpha1SinglePage
   */
  readonly size?: number

  /**
   * Label selector for filtering.
   * @type {Array<string>}
   * @memberof ContentHaloRunV1alpha1SinglePageApiListcontentHaloRunV1alpha1SinglePage
   */
  readonly labelSelector?: Array<string>

  /**
   * Field selector for filtering.
   * @type {Array<string>}
   * @memberof ContentHaloRunV1alpha1SinglePageApiListcontentHaloRunV1alpha1SinglePage
   */
  readonly fieldSelector?: Array<string>
}

/**
 * Request parameters for updatecontentHaloRunV1alpha1SinglePage operation in ContentHaloRunV1alpha1SinglePageApi.
 * @export
 * @interface ContentHaloRunV1alpha1SinglePageApiUpdatecontentHaloRunV1alpha1SinglePageRequest
 */
export interface ContentHaloRunV1alpha1SinglePageApiUpdatecontentHaloRunV1alpha1SinglePageRequest {
  /**
   * Name of singlepage
   * @type {string}
   * @memberof ContentHaloRunV1alpha1SinglePageApiUpdatecontentHaloRunV1alpha1SinglePage
   */
  readonly name: string

  /**
   * Updated singlepage
   * @type {SinglePage}
   * @memberof ContentHaloRunV1alpha1SinglePageApiUpdatecontentHaloRunV1alpha1SinglePage
   */
  readonly singlePage?: SinglePage
}

/**
 * ContentHaloRunV1alpha1SinglePageApi - object-oriented interface
 * @export
 * @class ContentHaloRunV1alpha1SinglePageApi
 * @extends {BaseAPI}
 */
export class ContentHaloRunV1alpha1SinglePageApi extends BaseAPI {
  /**
   * Create content.halo.run/v1alpha1/SinglePage
   * @param {ContentHaloRunV1alpha1SinglePageApiCreatecontentHaloRunV1alpha1SinglePageRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof ContentHaloRunV1alpha1SinglePageApi
   */
  public createcontentHaloRunV1alpha1SinglePage(
    requestParameters: ContentHaloRunV1alpha1SinglePageApiCreatecontentHaloRunV1alpha1SinglePageRequest = {},
    options?: AxiosRequestConfig,
  ) {
    return ContentHaloRunV1alpha1SinglePageApiFp(this.configuration)
      .createcontentHaloRunV1alpha1SinglePage(requestParameters.singlePage, options)
      .then((request) => request(this.axios, this.basePath))
  }

  /**
   * Delete content.halo.run/v1alpha1/SinglePage
   * @param {ContentHaloRunV1alpha1SinglePageApiDeletecontentHaloRunV1alpha1SinglePageRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof ContentHaloRunV1alpha1SinglePageApi
   */
  public deletecontentHaloRunV1alpha1SinglePage(
    requestParameters: ContentHaloRunV1alpha1SinglePageApiDeletecontentHaloRunV1alpha1SinglePageRequest,
    options?: AxiosRequestConfig,
  ) {
    return ContentHaloRunV1alpha1SinglePageApiFp(this.configuration)
      .deletecontentHaloRunV1alpha1SinglePage(requestParameters.name, options)
      .then((request) => request(this.axios, this.basePath))
  }

  /**
   * Get content.halo.run/v1alpha1/SinglePage
   * @param {ContentHaloRunV1alpha1SinglePageApiGetcontentHaloRunV1alpha1SinglePageRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof ContentHaloRunV1alpha1SinglePageApi
   */
  public getcontentHaloRunV1alpha1SinglePage(
    requestParameters: ContentHaloRunV1alpha1SinglePageApiGetcontentHaloRunV1alpha1SinglePageRequest,
    options?: AxiosRequestConfig,
  ) {
    return ContentHaloRunV1alpha1SinglePageApiFp(this.configuration)
      .getcontentHaloRunV1alpha1SinglePage(requestParameters.name, options)
      .then((request) => request(this.axios, this.basePath))
  }

  /**
   * List content.halo.run/v1alpha1/SinglePage
   * @param {ContentHaloRunV1alpha1SinglePageApiListcontentHaloRunV1alpha1SinglePageRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof ContentHaloRunV1alpha1SinglePageApi
   */
  public listcontentHaloRunV1alpha1SinglePage(
    requestParameters: ContentHaloRunV1alpha1SinglePageApiListcontentHaloRunV1alpha1SinglePageRequest = {},
    options?: AxiosRequestConfig,
  ) {
    return ContentHaloRunV1alpha1SinglePageApiFp(this.configuration)
      .listcontentHaloRunV1alpha1SinglePage(
        requestParameters.page,
        requestParameters.size,
        requestParameters.labelSelector,
        requestParameters.fieldSelector,
        options,
      )
      .then((request) => request(this.axios, this.basePath))
  }

  /**
   * Update content.halo.run/v1alpha1/SinglePage
   * @param {ContentHaloRunV1alpha1SinglePageApiUpdatecontentHaloRunV1alpha1SinglePageRequest} requestParameters Request parameters.
   * @param {*} [options] Override http request option.
   * @throws {RequiredError}
   * @memberof ContentHaloRunV1alpha1SinglePageApi
   */
  public updatecontentHaloRunV1alpha1SinglePage(
    requestParameters: ContentHaloRunV1alpha1SinglePageApiUpdatecontentHaloRunV1alpha1SinglePageRequest,
    options?: AxiosRequestConfig,
  ) {
    return ContentHaloRunV1alpha1SinglePageApiFp(this.configuration)
      .updatecontentHaloRunV1alpha1SinglePage(requestParameters.name, requestParameters.singlePage, options)
      .then((request) => request(this.axios, this.basePath))
  }
}
