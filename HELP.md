# platform-gateway
网关服务

## Introduction
基于Spring Cloud Gateway的微服务网关服务。此服务是对[springcloud-gateway]服务的二开，添加了项目层权限过滤。

## Features

- 用户鉴权
- 限流
- 整体运维
- API监控

## Dependencies

* 服务依赖

```xml
<dependency>
    <groupId>net.commchina</groupId>
    <artifactId>platform-gateway</artifactId>
    <version>${gateway.service.version}</version>
</dependency>
```

