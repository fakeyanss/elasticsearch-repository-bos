# elasticsearch-repository-bos

Elasticsearch 基于 BOS 的快照与恢复。

参考 [官方 repository-s3](https://github.com/elastic/elasticsearch/tree/main/modules/repository-s3) 实现。

# 使用场景

满足这些条件，可以使用这个库。

- 使用了 BOS 对象存储，很便宜
- 使用了 Elasticsearch 数据库，但没有使用百度云 Elasticsearch（是的，它太贵了）

使用这个库，你可以定时对生产环境的 Elasticsearch 集群的索引数据进行增量备份，数据保存在 BOS。

这样，你可以在集群故障后，最小损失的恢复数据。

你也可以准备一个灾备集群，借助snapshot在多套集群间同步数据，而不依赖业务模块多写。在主集群故障时，直接切换流量入口到灾备集群。

# 使用文档

## Prerequirement
了解 Elasticsearch 的 repo、snapshot，可以看看这篇文章。

Elasticsearch 支持 FS、HDFS、S3 作为备份仓库的文件存储，考虑用 BOS（兼容 S3 协议）做备份仓库。

Elasticsearch snapshot 备份 与 BOS 内部上传文件逻辑不适配，在 Elasticsearch 源码中搜索错误日志，结合 BOS 兼容 S3 接口说明，定位到问题是 BOS 兼容 S3 接口不支持批量删除文件，所以下载插件源码，修改这部分逻辑为遍历删除文件，然后编译和离线安装插件，可以成功运行备份。

## 安装 elasticsearch-repository-bos 插件

es 7.6.2 版本，可以直接下载我构建好的 [插件](https://github.com/mess-around/elasticsearch-repository-bos/releases/tag/7.6.2) 或镜像:
```
docker pull fakeyanss/elasticsearch-with-repo-bos:7.6.2
```

构建时依赖7.6.2版本，如果需要其他非兼容版本使用，可以按照以下说明自行编译构建。

先编译插件，再安装到 es 集群。

### 手动安装

maven 编译插件：
```
mvn clean package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
```

将编译的 zip 包解压，拷贝到每个 Elasticsearch 实例的 plugins 目录下，重启集群即可。

### 编译镜像预装插件

maven 编译插件：
```
mvn clean package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
```
编译镜像：
```
docker build -f build/Dockerfile -t elasticsearch-with-repo-bos:7.6.2 .
```

也可直接执行 build.sh 脚本完成这两步：
```
bash scripts/build.sh
```

## BOS Region选择

存储备份的 BOS 可用区，对照 [BOS 服务域名](https://cloud.baidu.com/doc/BOS/s/xjwvyq9l4) 自行选择。

## 创建 repo

可以直接指定 ak、sk，该方式会在后几个版本过期，可以使用elasticsearch-keystore工具设置。 base_path 可以指定为集群名称或 repo 名称，这样可以一个 bucket 设置关联多个 repo。
```
PUT /_snapshot/test-repo
{
  "type": "s3",
  "settings": {
    "bucket": "bucket",
    "endpoint": "https://s3.bj.bcebos.com",
    "access_key": "xxx",
    "secret_key": "yyy",
    "base_path": "test-repo"
  }
}
```

## 创建与删除 snapshot
**创建 snapshot**
```
PUT /_snapshot/test-repo/snapshot_1
```

**创建 snapshot，指定备份索引**
```
PUT /_snapshot/test-repo/snapshot_1
{
  "indices": "test_v1,test_v2"
}
```

**创建 snapshot，指定备份索引，以时间命名**，注意参数编码
```
PUT /_snapshot/test-repo/%3Csnapshot-%7Bnow%2Fd%7D%3E
{
  "indices": "test_v1,test_v2"
}
```

**查询 snapshot 列表**
```
GET _snapshot/test-repo/_all
```

**查询 snapshot 进度**
```
GET _snapshot/test-repo/snapshot_1/_status
```

## 恢复 snapshot

恢复快照时，如果不指定重命名方式，就必须先 close 掉已经存在的索引；如果索引不存在，会直接创建出来。

> The restore operation can be performed on a functioning cluster. However, an existing index can be only restored if it’s closed and has the same number of shards as the index in the snapshot. The restore operation automatically opens restored indices if they were closed and creates new indices if they didn’t exist in the cluster.

**集群内恢复**

匹配 test_前缀的索引，执行恢复操作，恢复数据索引重命名为 restored_index_test_前缀
```
POST /_snapshot/test-repo/snapshot-2021.06.22/_restore
{
  "rename_pattern": "test_(.+)",
  "rename_replacement": "restored_index_test_$1"
}
```

**集群间迁移**

复制 A 集群备份仓库的 snapshot 文件夹，到一个新的 bucket 或同 bucket 的另一文件夹下，将集群 B 的备份仓库设置为这个地址，然后执行 restore 即可。

## 更多操作

参考官方文档 https://www.elastic.co/guide/en/elasticsearch/reference/master/repository-s3.html