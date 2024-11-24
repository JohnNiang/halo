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
import { Condition } from './condition';

/**
 * 
 * @export
 * @interface PostStatus
 */
export interface PostStatus {
    /**
     * 
     * @type {number}
     * @memberof PostStatus
     */
    'commentsCount'?: number;
    /**
     * 
     * @type {Array<Condition>}
     * @memberof PostStatus
     */
    'conditions'?: Array<Condition>;
    /**
     * 
     * @type {Array<string>}
     * @memberof PostStatus
     */
    'contributors'?: Array<string>;
    /**
     * 
     * @type {string}
     * @memberof PostStatus
     */
    'excerpt'?: string;
    /**
     * 
     * @type {boolean}
     * @memberof PostStatus
     */
    'hideFromList'?: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof PostStatus
     */
    'inProgress'?: boolean;
    /**
     * 
     * @type {string}
     * @memberof PostStatus
     */
    'lastModifyTime'?: string;
    /**
     * 
     * @type {number}
     * @memberof PostStatus
     */
    'observedVersion'?: number;
    /**
     * 
     * @type {string}
     * @memberof PostStatus
     */
    'permalink'?: string;
    /**
     * 
     * @type {string}
     * @memberof PostStatus
     */
    'phase'?: string;
}

