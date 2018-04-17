# ReverseProxy
反向代理服务器，企图比拼nginx

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
