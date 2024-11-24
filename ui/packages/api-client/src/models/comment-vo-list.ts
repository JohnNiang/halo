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
import { CommentVo } from './comment-vo';

/**
 * 
 * @export
 * @interface CommentVoList
 */
export interface CommentVoList {
    /**
     * Indicates whether current page is the first page.
     * @type {boolean}
     * @memberof CommentVoList
     */
    'first': boolean;
    /**
     * Indicates whether current page has previous page.
     * @type {boolean}
     * @memberof CommentVoList
     */
    'hasNext': boolean;
    /**
     * Indicates whether current page has previous page.
     * @type {boolean}
     * @memberof CommentVoList
     */
    'hasPrevious': boolean;
    /**
     * A chunk of items.
     * @type {Array<CommentVo>}
     * @memberof CommentVoList
     */
    'items': Array<CommentVo>;
    /**
     * Indicates whether current page is the last page.
     * @type {boolean}
     * @memberof CommentVoList
     */
    'last': boolean;
    /**
     * Page number, starts from 1. If not set or equal to 0, it means no pagination.
     * @type {number}
     * @memberof CommentVoList
     */
    'page': number;
    /**
     * Size of each page. If not set or equal to 0, it means no pagination.
     * @type {number}
     * @memberof CommentVoList
     */
    'size': number;
    /**
     * Total elements.
     * @type {number}
     * @memberof CommentVoList
     */
    'total': number;
    /**
     * Indicates total pages.
     * @type {number}
     * @memberof CommentVoList
     */
    'totalPages': number;
}

