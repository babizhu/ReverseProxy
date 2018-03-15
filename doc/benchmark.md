# ReverseProxy 性能比较

采用netty自带的HexDumpProxy作为性能基准

系统架构：
    ab--代理服务器---vertx做的一个简单web服务器
    
## 性能

### 直连
` ab -k  -n 1000000 -r -c 1000 http://localhost:9090/test`

* Concurrency Level:      1000 
* Time taken for tests:   17.010 seconds
* Complete requests:      1000000
* Failed requests:        0
* Keep-Alive requests:    1000000
* Total transferred:      67000000 bytes
* HTML transferred:       5000000 bytes
* Requests per second:    `58790.12` [#/sec] (mean)
* Time per request:       17.010 [ms] (mean)
* Time per request:       0.017 [ms] (mean, across all concurrent requests)
* Transfer rate:          3846.62 [Kbytes/sec] received
 
### ReverseProxy
` ab -k  -n 1000000 -r -c 1000 http://localhost:8000/test`
 
* Concurrency Level:      1000
* Time taken for tests:   19.856 seconds
* Complete requests:      1000000
* Failed requests:        0
* Keep-Alive requests:    1000000
* Total transferred:      67000000 bytes
* HTML transferred:       5000000 bytes
* Requests per second:    `50362.00` [#/sec] (mean)
* Time per request:       19.856 [ms] (mean)
* Time per request:       0.020 [ms] (mean, across all concurrent requests)
* Transfer rate:          3295.17 [Kbytes/sec] received
 
 
`  ab -n 1000000 -r -c 1000 http://localhost:8000/test（不带-k参数）``

* Concurrency Level:      1000                                                                                                            
* Time taken for tests:   56.786 seconds                                                                                                  
* Failed requests:        0                                                                                                               
* Total transferred:      43000000 bytes                                                                                                  
* HTML transferred:       5000000 bytes                                                                                                   
* Requests per second:    `17610.00` [#/sec] (mean)                                                                                         
* Time per request:       56.786 [ms] (mean)                                                                                              
* Time per request:       0.057 [ms] (mean, across all concurrent requests)                                                               
* Transfer rate:          739.48 [Kbytes/sec] received
 
 ### nginx
 
` ab -k  -n 1000000 -r -c 1000 http://localhost:8080/test`
  
* Concurrency Level:      1000                                                                                                           
* Time taken for tests:   66.159 seconds                                                                                                 
* Complete requests:      1000000                                                                                                        
* Failed requests:        1000437                                                                                                        
*    (Connect: 0, Receive: 437, Length: 999104, Exceptions: 896)                                                                         
* Non-2xx responses:      131                                                                                                            
* Keep-Alive requests:    989561                                                                                                         
* Total transferred:      134863158 bytes                                                                                                
* HTML transferred:       5021327 bytes                                                                                                  
* Requests per second:    `15115.12` [#/sec] (mean)                                                                                        
* Time per request:       0.066 [ms] (mean, across all concurrent requests)                                                              
* Transfer rate:          1990.70 [Kbytes/sec] received 
 
` ab  -n 1000000 -r -c 1000 http://localhost:8080/test（不带-k参数）`
 
* Concurrency Level:      1000                                                                                                           
* Time taken for tests:   63.032 seconds                                                                                                 
* Complete requests:      1000000                                                                                                        
* Failed requests:        1005682                                                                                                        
*    (Connect: 0, Receive: 5718, Length: 993128, Exceptions: 6836)                                                                       
* Non-2xx responses:      67                                                                                                             
* Total transferred:      129122921 bytes                                                                                                
* HTML transferred:       4978839 bytes                                                                                                  
* Requests per second:    `15865.07` [#/sec] (mean)                                                                                        
* Time per request:       63.032 [ms] (mean)                                                                                             
* Time per request:       0.063 [ms] (mean, across all concurrent requests)                                                              
* Transfer rate:          2000.53 [Kbytes/sec] received 