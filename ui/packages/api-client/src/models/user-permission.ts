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


// May contain unused imports in some cases
// @ts-ignore
import { Role } from './role';

/**
 * 
 * @export
 * @interface UserPermission
 */
export interface UserPermission {
    /**
     * 
     * @type {Array<Role>}
     * @memberof UserPermission
     */
    'permissions': Array<Role>;
    /**
     * 
     * @type {Array<Role>}
     * @memberof UserPermission
     */
    'roles': Array<Role>;
    /**
     * 
     * @type {Array<string>}
     * @memberof UserPermission
     */
    'uiPermissions': Array<string>;
}

