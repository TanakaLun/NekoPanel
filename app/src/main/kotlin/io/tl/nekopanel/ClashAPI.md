Mihomo (Clash Meta) 完整 API 接口文档

基础信息

项目 说明
默认地址 http://127.0.0.1:9090
认证方式 Authorization: Bearer {secret} 或 WebSocket 使用 ?token={secret}
响应格式 JSON
WebSocket 支持 /logs, /traffic, /connections, /memory

---

目录

1. 系统基础接口
2. 代理管理接口
3. 代理组接口
4. 配置管理接口
5. 规则管理接口
6. 连接管理接口
7. 代理提供者接口
8. 规则提供者接口
9. DNS 接口
10. 缓存管理接口
11. 实时监控接口 (WebSocket)
12. 资源监控接口
13. 升级与重启接口
14. 调试接口
15. UI 静态文件接口
16. 外部扩展接口

---

1. 系统基础接口

端点 方法 说明 请求体 响应
/ GET 健康检查 - {"hello": "mihomo"}
/version GET 获取版本信息 - {"meta": "...", "version": "..."}
/restart POST 重启核心 - {"status": "ok"}

/version 响应示例：

```json
{
  "meta": "1.18.0",
  "version": "1.18.0"
}
```

---

2. 代理管理接口

端点 方法 说明 请求体 响应
/proxies GET 获取所有代理 - {"proxies": {...}}
/proxies/{name} GET 获取单个代理 - 代理对象
/proxies/{name} PUT 切换 Selector 代理 {"name": "proxyName"} 204
/proxies/{name} DELETE 重置 Selector 选择 - 204
/proxies/{name}/delay GET 测试代理延迟 - {"delay": ms}

/proxies 响应结构：

```json
{
  "proxies": {
    "Proxy": {
      "name": "Proxy",
      "type": "Selector",
      "now": "🇺🇸 US Node",
      "all": ["🇺🇸 US Node", "🇯🇵 JP Node"],
      "icon": "https://example.com/icon.png",
      "history": [
        {"delay": 156, "time": "2024-01-01T00:00:00Z"}
      ]
    },
    "DIRECT": {
      "name": "DIRECT",
      "type": "Direct",
      "now": "DIRECT",
      "history": []
    }
  }
}
```

/proxies/{name}/delay 参数：

参数 默认值 说明
url http://www.gstatic.com/generate_204 测试目标地址
timeout 5000 超时时间 (ms)
expected 204 期望状态码（支持范围如 200-299）

响应：

```json
{"delay": 156}
```

---

3. 代理组接口

端点 方法 说明 响应
/group GET 获取所有代理组 {"proxies": [...]}
/group/{name} GET 获取单个代理组 代理组对象
/group/{name}/delay GET 测试组内所有代理延迟 {"nodeName": delay}

/group/{name}/delay 响应示例：

```json
{
  "🇺🇸 US Node": 156,
  "🇯🇵 JP Node": 235,
  "DIRECT": 12
}
```

---

4. 配置管理接口

端点 方法 说明 请求体 响应
/configs GET 获取当前配置 - 配置对象
/configs PATCH 部分更新配置 见下方 204
/configs PUT 完整替换配置 {"path": "..."} 或 {"payload": "..."} 204
/configs/geo POST 更新 GeoIP/GeoSite 数据库 - 204

GET /configs 响应示例：

```json
{
  "port": 7890,
  "socks-port": 7891,
  "redir-port": 7892,
  "tproxy-port": 7893,
  "mixed-port": 7894,
  "allow-lan": false,
  "bind-address": "*",
  "mode": "rule",
  "log-level": "info",
  "ipv6": false,
  "sniffing": false,
  "interface-name": "",
  "routing-mark": 0,
  "tcp-concurrent": false,
  "find-process-mode": "always",
  "global-client-fingerprint": "",
  "keep-alive-interval": 30,
  "keep-alive-idle": 30,
  "disable-keep-alive": false,
  "unified-delay": false,
  "tun": {...},
  "tuic-server": {...}
}
```

PATCH /configs 支持的字段：

```json
{
  "port": 7890,
  "socks-port": 7891,
  "redir-port": 7892,
  "tproxy-port": 7893,
  "mixed-port": 7894,
  "allow-lan": true,
  "bind-address": "0.0.0.0",
  "mode": "rule",
  "log-level": "info",
  "ipv6": true,
  "sniffing": true,
  "tcp-concurrent": true,
  "interface-name": "wlan0",
  "tun": {
    "enable": true,
    "device": "tun0",
    "stack": "system",
    "mtu": 1500,
    "gso": true,
    "auto-route": true
  }
}
```

---

5. 规则管理接口

端点 方法 说明 请求体 响应
/rules GET 获取所有规则 - {"rules": [...]}
/rules/disable PATCH 批量禁用/启用规则 {"index": true/false} 204

GET /rules 响应示例：

```json
{
  "rules": [
    {
      "index": 0,
      "type": "DOMAIN-SUFFIX",
      "payload": "google.com",
      "proxy": "Proxy",
      "size": -1,
      "extra": {
        "disabled": false,
        "hitCount": 42,
        "hitAt": "2024-01-01T00:00:00Z",
        "missCount": 10,
        "missAt": "2024-01-01T00:00:00Z"
      }
    }
  ]
}
```

---

6. 连接管理接口

端点 方法 说明 响应
/connections GET 获取当前连接列表 见下方
/connections DELETE 关闭所有连接 204
/connections/{id} DELETE 关闭指定连接 204

GET /connections 响应示例：

```json
{
  "connections": [
    {
      "id": "abc123",
      "metadata": {
        "network": "tcp",
        "type": "Socks5",
        "sourceIP": "192.168.1.100",
        "sourcePort": "54321",
        "destinationIP": "1.1.1.1",
        "destinationPort": "443",
        "host": "google.com",
        "dnsMode": "fakeip"
      },
      "upload": 10240,
      "download": 20480,
      "start": "2024-01-01T00:00:00Z",
      "chains": ["Proxy", "🇺🇸 US Node"],
      "rule": "DOMAIN-SUFFIX,google.com,Proxy",
      "uploadSpeed": 1024,
      "downloadSpeed": 2048
    }
  ],
  "downloadTotal": 1073741824,
  "uploadTotal": 536870912,
  "memory": 42500000
}
```

---

7. 代理提供者接口

端点 方法 说明 响应
/providers/proxies GET 获取所有代理提供者 {"providers": {...}}
/providers/proxies/{name} GET 获取单个提供者 提供者对象
/providers/proxies/{name} PUT 手动更新提供者 204
/providers/proxies/{name}/healthcheck GET 触发健康检查 204
/providers/proxies/{name}/proxies GET 获取提供者内代理列表 ["proxy1", "proxy2"]
/providers/proxies/{name}/proxies/{proxyName} GET 获取提供者内单个代理 代理对象
/providers/proxies/{name}/proxies/{proxyName}/delay GET 测试提供者内代理延迟 {"delay": ms}

---

8. 规则提供者接口

端点 方法 说明 响应
/providers/rules GET 获取所有规则提供者 {"providers": {...}}
/providers/rules/{name} GET 获取单个规则提供者 提供者对象
/providers/rules/{name} PUT 手动更新规则提供者 204

---

9. DNS 接口

端点 方法 说明 参数 响应
/dns/query GET DNS 查询 name, type DNS 响应 JSON

参数说明：

参数 默认值 说明
name 必填 域名
type A 记录类型 (A, AAAA, MX, TXT 等)

响应示例：

```json
{
  "Status": 0,
  "Question": [{"name": "google.com.", "type": 1}],
  "Answer": [
    {
      "name": "google.com.",
      "type": 1,
      "TTL": 300,
      "data": "142.250.185.46"
    }
  ],
  "TC": false,
  "RD": true,
  "RA": true,
  "AD": false,
  "CD": false
}
```

---

10. 缓存管理接口

端点 方法 说明 响应
/cache/fakeip/flush POST 刷新 FakeIP 池 204
/cache/dns/flush POST 刷新 DNS 缓存 204

---

11. 实时监控接口 (WebSocket)

端点 方法 说明 参数
/traffic GET 实时流量监控 ?token={secret}
/logs GET 实时日志监控 ?level=info&token={secret}
/connections GET 实时连接监控 ?interval=1000&token={secret}
/memory GET 实时内存监控 ?token={secret}

WebSocket 消息格式：

/traffic 消息：

```json
{
  "up": 10240,
  "down": 20480,
  "upTotal": 1073741824,
  "downTotal": 536870912
}
```

/logs 消息（默认格式）：

```json
{
  "type": "info",
  "payload": "HTTP proxy listening at: 127.0.0.1:7890"
}
```

/logs 消息（?format=structured）：

```json
{
  "time": "15:04:05",
  "level": "info",
  "message": "HTTP proxy listening at: 127.0.0.1:7890",
  "fields": []
}
```

/connections 消息：

```json
{
  "connections": [...],
  "downloadTotal": 1073741824,
  "uploadTotal": 536870912,
  "memory": 42500000
}
```

/memory 消息：

```json
{
  "inuse": 42500000,
  "oslimit": 0
}
```

---

12. 资源监控接口

端点 方法 说明 响应
/memory GET 获取内存占用 {"inuse": 42500000, "oslimit": 0}
/traffic GET 获取流量统计 {"up": 0, "down": 0, "upTotal": 0, "downTotal": 0}

---

13. 升级与重启接口

端点 方法 说明 响应
/restart POST 重启核心 {"status": "ok"}
/upgrade POST 升级核心 {"status": "ok"}
/upgrade/ui POST 升级 UI {"status": "ok"}
/upgrade/geo POST 更新 Geo 数据库 204

/upgrade 参数：

参数 说明
channel 更新渠道 (stable, beta, dev)
force 强制更新 (true/false)

---

14. 调试接口 (需要 LogLevel = debug)

端点 方法 说明 响应
/debug/gc PUT 强制垃圾回收 204
/debug/pprof GET Go pprof 分析 pprof 数据
/debug/pprof/heap GET 堆内存分析 pprof 数据
/debug/pprof/goroutine GET 协程分析 pprof 数据

---

15. UI 静态文件接口

端点 方法 说明
/ui GET 重定向到 /ui/
/ui/* GET 静态文件服务
/ui/assets/* GET 资源文件
/ui/index.html GET 入口页面

---

16. 外部扩展接口

通过 route.Register() 可注册外部路由，用于第三方扩展。

```go
route.Register(func(r chi.Router) {
    r.Get("/custom/endpoint", customHandler)
})
```

---

通用错误响应

状态码 响应体
400 {"message": "Body invalid"}
401 {"message": "Unauthorized"}
404 {"message": "Resource not found"}
500 {"message": "Internal server error"}
504 {"message": "Timeout"}

---

接口分类汇总表

分类 接口数量 主要端点
系统基础 3 /, /version, /restart
代理管理 5 /proxies
代理组 3 /group
配置管理 4 /configs
规则管理 2 /rules
连接管理 3 /connections
代理提供者 7 /providers/proxies
规则提供者 3 /providers/rules
DNS 1 /dns/query
缓存管理 2 /cache/*
实时监控 (WS) 4 /traffic, /logs, /connections, /memory
资源监控 2 /memory, /traffic
升级重启 4 /restart, /upgrade*
调试 4 /debug/*
UI 静态文件 2 /ui/*
总计 49 -