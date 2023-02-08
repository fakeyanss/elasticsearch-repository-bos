# 实现原理
依赖 BOS 兼容的 S3 接口，可以直接复用官方 repository-s3 的实现。

在使用 repository-s3 连接 BOS 的调试过程中，逐步排查报错，定位到 s3.bj.bcebos.com 不提供批量删除 object 功能，导致没法直接使用插件直接使用 repository-s3。

修改`S3BlobContainer.java`中的批量删除方法，改为遍历循环删除 object
```
// clientReference.client().deleteObjects(deleteRequest);
deleteRequest.getKeys().stream().forEach(key -> {
    try {
        clientReference.client().deleteObject(deleteRequest.getBucketName(), key.getKey());
    } catch (AmazonClientException e) {
        LOGGER.warn("delete blobs error, key: {}", key, e);
    }
});
```
性能也许会有降低，但可以忽略，有强需求可用并行流或线程池+CompletableFuture 并发删除。