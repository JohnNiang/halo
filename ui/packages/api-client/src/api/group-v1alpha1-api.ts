/* tslint:disable */
/* eslint-disable */
/**
 * Halo
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 2.20.10-SNAPSHOT
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


import type { Configuration } from '../configuration';
import type { AxiosPromise, AxiosInstance, RawAxiosRequestConfig } from 'axios';
import globalAxios from 'axios';
// Some imports not used depending on template conditions
// @ts-ignore
import { DUMMY_BASE_URL, assertParamExists, setApiKeyToObject, setBasicAuthToObject, setBearerAuthToObject, setOAuthToObject, setSearchParams, serializeDataIfNeeded, toPathString, createRequestFunction } from '../common';
// @ts-ignore
import { BASE_PATH, COLLECTION_FORMATS, RequestArgs, BaseAPI, RequiredError, operationServerMap } from '../base';
// @ts-ignore
import { Group } from '../models';
// @ts-ignore
import { GroupList } from '../models';
// @ts-ignore
import { JsonPatchInner } from '../models';
/**
 * GroupV1alpha1Api - axios parameter creator
 * @export
 */
export const GroupV1alpha1ApiAxiosParamCreator = function (configuration?: Configuration) {
    return {
        /**
         * Create Group
         * @param {Group} [group] Fresh group
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        createGroup: async (group?: Group, options: RawAxiosRequestConfig = {}): Promise<RequestArgs> => {
            const localVarPath = `/apis/storage.halo.run/v1alpha1/groups`;
            // use dummy base URL string because the URL constructor only accepts absolute URLs.
            const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
            let baseOptions;
            if (configuration) {
                baseOptions = configuration.baseOptions;
            }

            const localVarRequestOptions = { method: 'POST', ...baseOptions, ...options};
            const localVarHeaderParameter = {} as any;
            const localVarQueryParameter = {} as any;

            // authentication basicAuth required
            // http basic authentication required
            setBasicAuthToObject(localVarRequestOptions, configuration)

            // authentication bearerAuth required
            // http bearer authentication required
            await setBearerAuthToObject(localVarHeaderParameter, configuration)


    
            localVarHeaderParameter['Content-Type'] = 'application/json';

            setSearchParams(localVarUrlObj, localVarQueryParameter);
            let headersFromBaseOptions = baseOptions && baseOptions.headers ? baseOptions.headers : {};
            localVarRequestOptions.headers = {...localVarHeaderParameter, ...headersFromBaseOptions, ...options.headers};
            localVarRequestOptions.data = serializeDataIfNeeded(group, localVarRequestOptions, configuration)

            return {
                url: toPathString(localVarUrlObj),
                options: localVarRequestOptions,
            };
        },
        /**
         * Delete Group
         * @param {string} name Name of group
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        deleteGroup: async (name: string, options: RawAxiosRequestConfig = {}): Promise<RequestArgs> => {
            // verify required parameter 'name' is not null or undefined
            assertParamExists('deleteGroup', 'name', name)
            const localVarPath = `/apis/storage.halo.run/v1alpha1/groups/{name}`
                .replace(`{${"name"}}`, encodeURIComponent(String(name)));
            // use dummy base URL string because the URL constructor only accepts absolute URLs.
            const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
            let baseOptions;
            if (configuration) {
                baseOptions = configuration.baseOptions;
            }

            const localVarRequestOptions = { method: 'DELETE', ...baseOptions, ...options};
            const localVarHeaderParameter = {} as any;
            const localVarQueryParameter = {} as any;

            // authentication basicAuth required
            // http basic authentication required
            setBasicAuthToObject(localVarRequestOptions, configuration)

            // authentication bearerAuth required
            // http bearer authentication required
            await setBearerAuthToObject(localVarHeaderParameter, configuration)


    
            setSearchParams(localVarUrlObj, localVarQueryParameter);
            let headersFromBaseOptions = baseOptions && baseOptions.headers ? baseOptions.headers : {};
            localVarRequestOptions.headers = {...localVarHeaderParameter, ...headersFromBaseOptions, ...options.headers};

            return {
                url: toPathString(localVarUrlObj),
                options: localVarRequestOptions,
            };
        },
        /**
         * Get Group
         * @param {string} name Name of group
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        getGroup: async (name: string, options: RawAxiosRequestConfig = {}): Promise<RequestArgs> => {
            // verify required parameter 'name' is not null or undefined
            assertParamExists('getGroup', 'name', name)
            const localVarPath = `/apis/storage.halo.run/v1alpha1/groups/{name}`
                .replace(`{${"name"}}`, encodeURIComponent(String(name)));
            // use dummy base URL string because the URL constructor only accepts absolute URLs.
            const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
            let baseOptions;
            if (configuration) {
                baseOptions = configuration.baseOptions;
            }

            const localVarRequestOptions = { method: 'GET', ...baseOptions, ...options};
            const localVarHeaderParameter = {} as any;
            const localVarQueryParameter = {} as any;

            // authentication basicAuth required
            // http basic authentication required
            setBasicAuthToObject(localVarRequestOptions, configuration)

            // authentication bearerAuth required
            // http bearer authentication required
            await setBearerAuthToObject(localVarHeaderParameter, configuration)


    
            setSearchParams(localVarUrlObj, localVarQueryParameter);
            let headersFromBaseOptions = baseOptions && baseOptions.headers ? baseOptions.headers : {};
            localVarRequestOptions.headers = {...localVarHeaderParameter, ...headersFromBaseOptions, ...options.headers};

            return {
                url: toPathString(localVarUrlObj),
                options: localVarRequestOptions,
            };
        },
        /**
         * List Group
         * @param {number} [page] Page number. Default is 0.
         * @param {number} [size] Size number. Default is 0.
         * @param {Array<string>} [labelSelector] Label selector. e.g.: hidden!&#x3D;true
         * @param {Array<string>} [fieldSelector] Field selector. e.g.: metadata.name&#x3D;&#x3D;halo
         * @param {Array<string>} [sort] Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        listGroup: async (page?: number, size?: number, labelSelector?: Array<string>, fieldSelector?: Array<string>, sort?: Array<string>, options: RawAxiosRequestConfig = {}): Promise<RequestArgs> => {
            const localVarPath = `/apis/storage.halo.run/v1alpha1/groups`;
            // use dummy base URL string because the URL constructor only accepts absolute URLs.
            const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
            let baseOptions;
            if (configuration) {
                baseOptions = configuration.baseOptions;
            }

            const localVarRequestOptions = { method: 'GET', ...baseOptions, ...options};
            const localVarHeaderParameter = {} as any;
            const localVarQueryParameter = {} as any;

            // authentication basicAuth required
            // http basic authentication required
            setBasicAuthToObject(localVarRequestOptions, configuration)

            // authentication bearerAuth required
            // http bearer authentication required
            await setBearerAuthToObject(localVarHeaderParameter, configuration)

            if (page !== undefined) {
                localVarQueryParameter['page'] = page;
            }

            if (size !== undefined) {
                localVarQueryParameter['size'] = size;
            }

            if (labelSelector) {
                localVarQueryParameter['labelSelector'] = labelSelector;
            }

            if (fieldSelector) {
                localVarQueryParameter['fieldSelector'] = fieldSelector;
            }

            if (sort) {
                localVarQueryParameter['sort'] = sort;
            }


    
            setSearchParams(localVarUrlObj, localVarQueryParameter);
            let headersFromBaseOptions = baseOptions && baseOptions.headers ? baseOptions.headers : {};
            localVarRequestOptions.headers = {...localVarHeaderParameter, ...headersFromBaseOptions, ...options.headers};

            return {
                url: toPathString(localVarUrlObj),
                options: localVarRequestOptions,
            };
        },
        /**
         * Patch Group
         * @param {string} name Name of group
         * @param {Array<JsonPatchInner>} [jsonPatchInner] 
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        patchGroup: async (name: string, jsonPatchInner?: Array<JsonPatchInner>, options: RawAxiosRequestConfig = {}): Promise<RequestArgs> => {
            // verify required parameter 'name' is not null or undefined
            assertParamExists('patchGroup', 'name', name)
            const localVarPath = `/apis/storage.halo.run/v1alpha1/groups/{name}`
                .replace(`{${"name"}}`, encodeURIComponent(String(name)));
            // use dummy base URL string because the URL constructor only accepts absolute URLs.
            const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
            let baseOptions;
            if (configuration) {
                baseOptions = configuration.baseOptions;
            }

            const localVarRequestOptions = { method: 'PATCH', ...baseOptions, ...options};
            const localVarHeaderParameter = {} as any;
            const localVarQueryParameter = {} as any;

            // authentication basicAuth required
            // http basic authentication required
            setBasicAuthToObject(localVarRequestOptions, configuration)

            // authentication bearerAuth required
            // http bearer authentication required
            await setBearerAuthToObject(localVarHeaderParameter, configuration)


    
            localVarHeaderParameter['Content-Type'] = 'application/json-patch+json';

            setSearchParams(localVarUrlObj, localVarQueryParameter);
            let headersFromBaseOptions = baseOptions && baseOptions.headers ? baseOptions.headers : {};
            localVarRequestOptions.headers = {...localVarHeaderParameter, ...headersFromBaseOptions, ...options.headers};
            localVarRequestOptions.data = serializeDataIfNeeded(jsonPatchInner, localVarRequestOptions, configuration)

            return {
                url: toPathString(localVarUrlObj),
                options: localVarRequestOptions,
            };
        },
        /**
         * Update Group
         * @param {string} name Name of group
         * @param {Group} [group] Updated group
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        updateGroup: async (name: string, group?: Group, options: RawAxiosRequestConfig = {}): Promise<RequestArgs> => {
            // verify required parameter 'name' is not null or undefined
            assertParamExists('updateGroup', 'name', name)
            const localVarPath = `/apis/storage.halo.run/v1alpha1/groups/{name}`
                .replace(`{${"name"}}`, encodeURIComponent(String(name)));
            // use dummy base URL string because the URL constructor only accepts absolute URLs.
            const localVarUrlObj = new URL(localVarPath, DUMMY_BASE_URL);
            let baseOptions;
            if (configuration) {
                baseOptions = configuration.baseOptions;
            }

            const localVarRequestOptions = { method: 'PUT', ...baseOptions, ...options};
            const localVarHeaderParameter = {} as any;
            const localVarQueryParameter = {} as any;

            // authentication basicAuth required
            // http basic authentication required
            setBasicAuthToObject(localVarRequestOptions, configuration)

            // authentication bearerAuth required
            // http bearer authentication required
            await setBearerAuthToObject(localVarHeaderParameter, configuration)


    
            localVarHeaderParameter['Content-Type'] = 'application/json';

            setSearchParams(localVarUrlObj, localVarQueryParameter);
            let headersFromBaseOptions = baseOptions && baseOptions.headers ? baseOptions.headers : {};
            localVarRequestOptions.headers = {...localVarHeaderParameter, ...headersFromBaseOptions, ...options.headers};
            localVarRequestOptions.data = serializeDataIfNeeded(group, localVarRequestOptions, configuration)

            return {
                url: toPathString(localVarUrlObj),
                options: localVarRequestOptions,
            };
        },
    }
};

/**
 * GroupV1alpha1Api - functional programming interface
 * @export
 */
export const GroupV1alpha1ApiFp = function(configuration?: Configuration) {
    const localVarAxiosParamCreator = GroupV1alpha1ApiAxiosParamCreator(configuration)
    return {
        /**
         * Create Group
         * @param {Group} [group] Fresh group
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        async createGroup(group?: Group, options?: RawAxiosRequestConfig): Promise<(axios?: AxiosInstance, basePath?: string) => AxiosPromise<Group>> {
            const localVarAxiosArgs = await localVarAxiosParamCreator.createGroup(group, options);
            const localVarOperationServerIndex = configuration?.serverIndex ?? 0;
            const localVarOperationServerBasePath = operationServerMap['GroupV1alpha1Api.createGroup']?.[localVarOperationServerIndex]?.url;
            return (axios, basePath) => createRequestFunction(localVarAxiosArgs, globalAxios, BASE_PATH, configuration)(axios, localVarOperationServerBasePath || basePath);
        },
        /**
         * Delete Group
         * @param {string} name Name of group
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        async deleteGroup(name: string, options?: RawAxiosRequestConfig): Promise<(axios?: AxiosInstance, basePath?: string) => AxiosPromise<void>> {
            const localVarAxiosArgs = await localVarAxiosParamCreator.deleteGroup(name, options);
            const localVarOperationServerIndex = configuration?.serverIndex ?? 0;
            const localVarOperationServerBasePath = operationServerMap['GroupV1alpha1Api.deleteGroup']?.[localVarOperationServerIndex]?.url;
            return (axios, basePath) => createRequestFunction(localVarAxiosArgs, globalAxios, BASE_PATH, configuration)(axios, localVarOperationServerBasePath || basePath);
        },
        /**
         * Get Group
         * @param {string} name Name of group
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        async getGroup(name: string, options?: RawAxiosRequestConfig): Promise<(axios?: AxiosInstance, basePath?: string) => AxiosPromise<Group>> {
            const localVarAxiosArgs = await localVarAxiosParamCreator.getGroup(name, options);
            const localVarOperationServerIndex = configuration?.serverIndex ?? 0;
            const localVarOperationServerBasePath = operationServerMap['GroupV1alpha1Api.getGroup']?.[localVarOperationServerIndex]?.url;
            return (axios, basePath) => createRequestFunction(localVarAxiosArgs, globalAxios, BASE_PATH, configuration)(axios, localVarOperationServerBasePath || basePath);
        },
        /**
         * List Group
         * @param {number} [page] Page number. Default is 0.
         * @param {number} [size] Size number. Default is 0.
         * @param {Array<string>} [labelSelector] Label selector. e.g.: hidden!&#x3D;true
         * @param {Array<string>} [fieldSelector] Field selector. e.g.: metadata.name&#x3D;&#x3D;halo
         * @param {Array<string>} [sort] Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        async listGroup(page?: number, size?: number, labelSelector?: Array<string>, fieldSelector?: Array<string>, sort?: Array<string>, options?: RawAxiosRequestConfig): Promise<(axios?: AxiosInstance, basePath?: string) => AxiosPromise<GroupList>> {
            const localVarAxiosArgs = await localVarAxiosParamCreator.listGroup(page, size, labelSelector, fieldSelector, sort, options);
            const localVarOperationServerIndex = configuration?.serverIndex ?? 0;
            const localVarOperationServerBasePath = operationServerMap['GroupV1alpha1Api.listGroup']?.[localVarOperationServerIndex]?.url;
            return (axios, basePath) => createRequestFunction(localVarAxiosArgs, globalAxios, BASE_PATH, configuration)(axios, localVarOperationServerBasePath || basePath);
        },
        /**
         * Patch Group
         * @param {string} name Name of group
         * @param {Array<JsonPatchInner>} [jsonPatchInner] 
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        async patchGroup(name: string, jsonPatchInner?: Array<JsonPatchInner>, options?: RawAxiosRequestConfig): Promise<(axios?: AxiosInstance, basePath?: string) => AxiosPromise<Group>> {
            const localVarAxiosArgs = await localVarAxiosParamCreator.patchGroup(name, jsonPatchInner, options);
            const localVarOperationServerIndex = configuration?.serverIndex ?? 0;
            const localVarOperationServerBasePath = operationServerMap['GroupV1alpha1Api.patchGroup']?.[localVarOperationServerIndex]?.url;
            return (axios, basePath) => createRequestFunction(localVarAxiosArgs, globalAxios, BASE_PATH, configuration)(axios, localVarOperationServerBasePath || basePath);
        },
        /**
         * Update Group
         * @param {string} name Name of group
         * @param {Group} [group] Updated group
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        async updateGroup(name: string, group?: Group, options?: RawAxiosRequestConfig): Promise<(axios?: AxiosInstance, basePath?: string) => AxiosPromise<Group>> {
            const localVarAxiosArgs = await localVarAxiosParamCreator.updateGroup(name, group, options);
            const localVarOperationServerIndex = configuration?.serverIndex ?? 0;
            const localVarOperationServerBasePath = operationServerMap['GroupV1alpha1Api.updateGroup']?.[localVarOperationServerIndex]?.url;
            return (axios, basePath) => createRequestFunction(localVarAxiosArgs, globalAxios, BASE_PATH, configuration)(axios, localVarOperationServerBasePath || basePath);
        },
    }
};

/**
 * GroupV1alpha1Api - factory interface
 * @export
 */
export const GroupV1alpha1ApiFactory = function (configuration?: Configuration, basePath?: string, axios?: AxiosInstance) {
    const localVarFp = GroupV1alpha1ApiFp(configuration)
    return {
        /**
         * Create Group
         * @param {GroupV1alpha1ApiCreateGroupRequest} requestParameters Request parameters.
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        createGroup(requestParameters: GroupV1alpha1ApiCreateGroupRequest = {}, options?: RawAxiosRequestConfig): AxiosPromise<Group> {
            return localVarFp.createGroup(requestParameters.group, options).then((request) => request(axios, basePath));
        },
        /**
         * Delete Group
         * @param {GroupV1alpha1ApiDeleteGroupRequest} requestParameters Request parameters.
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        deleteGroup(requestParameters: GroupV1alpha1ApiDeleteGroupRequest, options?: RawAxiosRequestConfig): AxiosPromise<void> {
            return localVarFp.deleteGroup(requestParameters.name, options).then((request) => request(axios, basePath));
        },
        /**
         * Get Group
         * @param {GroupV1alpha1ApiGetGroupRequest} requestParameters Request parameters.
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        getGroup(requestParameters: GroupV1alpha1ApiGetGroupRequest, options?: RawAxiosRequestConfig): AxiosPromise<Group> {
            return localVarFp.getGroup(requestParameters.name, options).then((request) => request(axios, basePath));
        },
        /**
         * List Group
         * @param {GroupV1alpha1ApiListGroupRequest} requestParameters Request parameters.
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        listGroup(requestParameters: GroupV1alpha1ApiListGroupRequest = {}, options?: RawAxiosRequestConfig): AxiosPromise<GroupList> {
            return localVarFp.listGroup(requestParameters.page, requestParameters.size, requestParameters.labelSelector, requestParameters.fieldSelector, requestParameters.sort, options).then((request) => request(axios, basePath));
        },
        /**
         * Patch Group
         * @param {GroupV1alpha1ApiPatchGroupRequest} requestParameters Request parameters.
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        patchGroup(requestParameters: GroupV1alpha1ApiPatchGroupRequest, options?: RawAxiosRequestConfig): AxiosPromise<Group> {
            return localVarFp.patchGroup(requestParameters.name, requestParameters.jsonPatchInner, options).then((request) => request(axios, basePath));
        },
        /**
         * Update Group
         * @param {GroupV1alpha1ApiUpdateGroupRequest} requestParameters Request parameters.
         * @param {*} [options] Override http request option.
         * @throws {RequiredError}
         */
        updateGroup(requestParameters: GroupV1alpha1ApiUpdateGroupRequest, options?: RawAxiosRequestConfig): AxiosPromise<Group> {
            return localVarFp.updateGroup(requestParameters.name, requestParameters.group, options).then((request) => request(axios, basePath));
        },
    };
};

/**
 * Request parameters for createGroup operation in GroupV1alpha1Api.
 * @export
 * @interface GroupV1alpha1ApiCreateGroupRequest
 */
export interface GroupV1alpha1ApiCreateGroupRequest {
    /**
     * Fresh group
     * @type {Group}
     * @memberof GroupV1alpha1ApiCreateGroup
     */
    readonly group?: Group
}

/**
 * Request parameters for deleteGroup operation in GroupV1alpha1Api.
 * @export
 * @interface GroupV1alpha1ApiDeleteGroupRequest
 */
export interface GroupV1alpha1ApiDeleteGroupRequest {
    /**
     * Name of group
     * @type {string}
     * @memberof GroupV1alpha1ApiDeleteGroup
     */
    readonly name: string
}

/**
 * Request parameters for getGroup operation in GroupV1alpha1Api.
 * @export
 * @interface GroupV1alpha1ApiGetGroupRequest
 */
export interface GroupV1alpha1ApiGetGroupRequest {
    /**
     * Name of group
     * @type {string}
     * @memberof GroupV1alpha1ApiGetGroup
     */
    readonly name: string
}

/**
 * Request parameters for listGroup operation in GroupV1alpha1Api.
 * @export
 * @interface GroupV1alpha1ApiListGroupRequest
 */
export interface GroupV1alpha1ApiListGroupRequest {
    /**
     * Page number. Default is 0.
     * @type {number}
     * @memberof GroupV1alpha1ApiListGroup
     */
    readonly page?: number

    /**
     * Size number. Default is 0.
     * @type {number}
     * @memberof GroupV1alpha1ApiListGroup
     */
    readonly size?: number

    /**
     * Label selector. e.g.: hidden!&#x3D;true
     * @type {Array<string>}
     * @memberof GroupV1alpha1ApiListGroup
     */
    readonly labelSelector?: Array<string>

    /**
     * Field selector. e.g.: metadata.name&#x3D;&#x3D;halo
     * @type {Array<string>}
     * @memberof GroupV1alpha1ApiListGroup
     */
    readonly fieldSelector?: Array<string>

    /**
     * Sorting criteria in the format: property,(asc|desc). Default sort order is ascending. Multiple sort criteria are supported.
     * @type {Array<string>}
     * @memberof GroupV1alpha1ApiListGroup
     */
    readonly sort?: Array<string>
}

/**
 * Request parameters for patchGroup operation in GroupV1alpha1Api.
 * @export
 * @interface GroupV1alpha1ApiPatchGroupRequest
 */
export interface GroupV1alpha1ApiPatchGroupRequest {
    /**
     * Name of group
     * @type {string}
     * @memberof GroupV1alpha1ApiPatchGroup
     */
    readonly name: string

    /**
     * 
     * @type {Array<JsonPatchInner>}
     * @memberof GroupV1alpha1ApiPatchGroup
     */
    readonly jsonPatchInner?: Array<JsonPatchInner>
}

/**
 * Request parameters for updateGroup operation in GroupV1alpha1Api.
 * @export
 * @interface GroupV1alpha1ApiUpdateGroupRequest
 */
export interface GroupV1alpha1ApiUpdateGroupRequest {
    /**
     * Name of group
     * @type {string}
     * @memberof GroupV1alpha1ApiUpdateGroup
     */
    readonly name: string

    /**
     * Updated group
     * @type {Group}
     * @memberof GroupV1alpha1ApiUpdateGroup
     */
    readonly group?: Group
}

/**
 * GroupV1alpha1Api - object-oriented interface
 * @export
 * @class GroupV1alpha1Api
 * @extends {BaseAPI}
 */
export class GroupV1alpha1Api extends BaseAPI {
    /**
     * Create Group
     * @param {GroupV1alpha1ApiCreateGroupRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     * @memberof GroupV1alpha1Api
     */
    public createGroup(requestParameters: GroupV1alpha1ApiCreateGroupRequest = {}, options?: RawAxiosRequestConfig) {
        return GroupV1alpha1ApiFp(this.configuration).createGroup(requestParameters.group, options).then((request) => request(this.axios, this.basePath));
    }

    /**
     * Delete Group
     * @param {GroupV1alpha1ApiDeleteGroupRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     * @memberof GroupV1alpha1Api
     */
    public deleteGroup(requestParameters: GroupV1alpha1ApiDeleteGroupRequest, options?: RawAxiosRequestConfig) {
        return GroupV1alpha1ApiFp(this.configuration).deleteGroup(requestParameters.name, options).then((request) => request(this.axios, this.basePath));
    }

    /**
     * Get Group
     * @param {GroupV1alpha1ApiGetGroupRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     * @memberof GroupV1alpha1Api
     */
    public getGroup(requestParameters: GroupV1alpha1ApiGetGroupRequest, options?: RawAxiosRequestConfig) {
        return GroupV1alpha1ApiFp(this.configuration).getGroup(requestParameters.name, options).then((request) => request(this.axios, this.basePath));
    }

    /**
     * List Group
     * @param {GroupV1alpha1ApiListGroupRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     * @memberof GroupV1alpha1Api
     */
    public listGroup(requestParameters: GroupV1alpha1ApiListGroupRequest = {}, options?: RawAxiosRequestConfig) {
        return GroupV1alpha1ApiFp(this.configuration).listGroup(requestParameters.page, requestParameters.size, requestParameters.labelSelector, requestParameters.fieldSelector, requestParameters.sort, options).then((request) => request(this.axios, this.basePath));
    }

    /**
     * Patch Group
     * @param {GroupV1alpha1ApiPatchGroupRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     * @memberof GroupV1alpha1Api
     */
    public patchGroup(requestParameters: GroupV1alpha1ApiPatchGroupRequest, options?: RawAxiosRequestConfig) {
        return GroupV1alpha1ApiFp(this.configuration).patchGroup(requestParameters.name, requestParameters.jsonPatchInner, options).then((request) => request(this.axios, this.basePath));
    }

    /**
     * Update Group
     * @param {GroupV1alpha1ApiUpdateGroupRequest} requestParameters Request parameters.
     * @param {*} [options] Override http request option.
     * @throws {RequiredError}
     * @memberof GroupV1alpha1Api
     */
    public updateGroup(requestParameters: GroupV1alpha1ApiUpdateGroupRequest, options?: RawAxiosRequestConfig) {
        return GroupV1alpha1ApiFp(this.configuration).updateGroup(requestParameters.name, requestParameters.group, options).then((request) => request(this.axios, this.basePath));
    }
}

