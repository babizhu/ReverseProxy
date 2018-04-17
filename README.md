# ReverseProxy
反向代理服务器，企图比拼nginx的反向代理功能

### 用法

* 修改uri
```java

val bootstrap = DefaultReverseProxyServer.bootstrap()
            .withPort(8000)
            .withHttpFilter(object : HttpFilterAdapter(){
                override fun clientToProxyRequest(httpObject: HttpObject): HttpResponse? {
                    if (httpObject is HttpRequest) {
                        httpObject.uri = "/test"//修改uri
                    }
                    return null
                }
            })
bootstrap.start()
    
```

* ip黑名单
```java

val bootstrap = DefaultReverseProxyServer.bootstrap()
            .withPort(8000)
            .withHttpFilter(BlackListFilter())
bootstrap.start()
    
```

* 采用ip hash 进行轮训
```java
val bootstrap = DefaultReverseProxyServer.bootstrap()
            .withRoutePolice(IpHashPolicy())
            .withPort(8000)
bootstrap.start()

```

* 根据uri进行路由
```java
val bootstrap = DefaultReverseProxyServer.bootstrap()
            .withRoutePolice(object : RoutePolicy {
                override fun getBackendServerAddress(request: HttpRequest, channel: Channel): InetSocketAddress? {
                    return when (request.uri()) {
                        "user" -> InetSocketAddress("user.api.com", 80)
                        "prouduct" -> InetSocketAddress("product.api.com", 80)
                        else -> InetSocketAddress("else.api.com", 80)
                    }
                }
            })
            .withPort(8000)
            .withHttpFilter(BlackListFilter())
    bootstrap.start()

```
