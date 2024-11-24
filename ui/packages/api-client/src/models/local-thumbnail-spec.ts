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



/**
 * 
 * @export
 * @interface LocalThumbnailSpec
 */
export interface LocalThumbnailSpec {
    /**
     * 
     * @type {string}
     * @memberof LocalThumbnailSpec
     */
    'filePath': string;
    /**
     * 
     * @type {string}
     * @memberof LocalThumbnailSpec
     */
    'imageSignature': string;
    /**
     * 
     * @type {string}
     * @memberof LocalThumbnailSpec
     */
    'imageUri': string;
    /**
     * 
     * @type {string}
     * @memberof LocalThumbnailSpec
     */
    'size': LocalThumbnailSpecSizeEnum;
    /**
     * 
     * @type {string}
     * @memberof LocalThumbnailSpec
     */
    'thumbSignature': string;
    /**
     * 
     * @type {string}
     * @memberof LocalThumbnailSpec
     */
    'thumbnailUri': string;
}

export const LocalThumbnailSpecSizeEnum = {
    S: 'S',
    M: 'M',
    L: 'L',
    Xl: 'XL'
} as const;

export type LocalThumbnailSpecSizeEnum = typeof LocalThumbnailSpecSizeEnum[keyof typeof LocalThumbnailSpecSizeEnum];


