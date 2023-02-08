/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.repositories.s3;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.StorageClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.cluster.metadata.RepositoryMetaData;
import org.elasticsearch.common.blobstore.BlobContainer;
import org.elasticsearch.common.blobstore.BlobPath;
import org.elasticsearch.common.blobstore.BlobStore;
import org.elasticsearch.common.blobstore.BlobStoreException;
import org.elasticsearch.common.unit.ByteSizeValue;

import java.io.IOException;
import java.util.Locale;

class S3BlobStore implements BlobStore {

    private static final Logger LOGGER = LogManager.getLogger(S3BlobStore.class);

    private final S3Service service;

    private final String bucket;

    private final ByteSizeValue bufferSize;

    private final boolean serverSideEncryption;

    private final CannedAccessControlList cannedACL;

    private final StorageClass storageClass;

    private final RepositoryMetaData repositoryMetaData;

    S3BlobStore(S3Service service, String bucket, boolean serverSideEncryption, ByteSizeValue bufferSize,
            String cannedACL, String storageClass, RepositoryMetaData repositoryMetaData) {
        this.service = service;
        this.bucket = bucket;
        this.serverSideEncryption = serverSideEncryption;
        this.bufferSize = bufferSize;
        this.cannedACL = initCannedACL(cannedACL);
        this.storageClass = initStorageClass(storageClass);
        this.repositoryMetaData = repositoryMetaData;
    }

    @Override
    public String toString() {
        return bucket;
    }

    public AmazonS3Reference clientReference() {
        return service.client(repositoryMetaData);
    }

    int getMaxRetries() {
        return service.settings(repositoryMetaData).maxRetries;
    }

    public String bucket() {
        return bucket;
    }

    public boolean serverSideEncryption() {
        return serverSideEncryption;
    }

    public long bufferSizeInBytes() {
        return bufferSize.getBytes();
    }

    @Override
    public BlobContainer blobContainer(BlobPath path) {
        return new S3BlobContainer(path, this);
    }

    @Override
    public void close() throws IOException {
        this.service.close();
    }

    public CannedAccessControlList getCannedACL() {
        return cannedACL;
    }

    public StorageClass getStorageClass() {
        return storageClass;
    }

    public static StorageClass initStorageClass(String storageClass) {
        if ((null == storageClass) || "".equals(storageClass)) {
            return StorageClass.Standard;
        }

        try {
            final StorageClass tmpStorageClass = StorageClass.fromValue(storageClass.toUpperCase(Locale.ENGLISH));
            if (tmpStorageClass.equals(StorageClass.Glacier)) {
                throw new BlobStoreException("Glacier storage class is not supported");
            }

            return tmpStorageClass;
        } catch (final IllegalArgumentException e) {
            LOGGER.warn("initStorageClass", e);
            throw new BlobStoreException("`" + storageClass + "` is not a valid S3 Storage Class.");
        }
    }

    /**
     * Constructs canned acl from string
     */
    public static CannedAccessControlList initCannedACL(String cannedACL) {
        if ((null == cannedACL) || "".equals(cannedACL)) {
            return CannedAccessControlList.Private;
        }

        for (final CannedAccessControlList cur : CannedAccessControlList.values()) {
            if (cur.toString().equalsIgnoreCase(cannedACL)) {
                return cur;
            }
        }

        throw new BlobStoreException("cannedACL is not valid: [" + cannedACL + "]");
    }
}
