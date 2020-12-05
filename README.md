[![GitHub release (latest by date)](https://img.shields.io/github/v/release/breucode/imisu?style=for-the-badge)](https://github.com/breucode/imisu/releases)
![Test Coverage](https://raw.githubusercontent.com/breucode/imisu/badges/coverage.svg)
[![Container vulnerabilities](https://raw.githubusercontent.com/breucode/imisu/badges/container-vulns.svg)](https://github.com/breucode/imisu/blob/badges/trivy-scan-result.txt)
[![Api-Docs](https://img.shields.io/badge/api--docs-swagger--ui-brightgreen?style=for-the-badge&logo=swagger)](https://breucode.github.io/imisu/swagger-ui.html?url=swagger.json&validatorUrl=)
[![License](https://img.shields.io/github/license/breucode/imisu?style=for-the-badge)](LICENSE)

# imisu

Imisu - short for "Is my internal service up?" - exposes a healthcheck endpoint for services, which are not available on the internet

## What can I do with imisu?

Imisu is a service, which executes a healthcheck, if a certain url is called via HTTP.
For example, imisu can execute a DNS query against a DNS server. If the server responds successfully without error, imisu will answer with a simple HTTP 200.

Think of imisu as a secure gateway from the internet into your internal network for healthcheck purposes. Your internal services stay completely isolated from the internet.
Only imisu needs to be available to the outside. Imisu is designed to respond with as little data as possible (namely HTTP codes), so that you don't have to worry about leaking internal sensitive error messages.

## Configuration

Imisu accepts a [HOCON](https://github.com/lightbend/config/blob/master/HOCON.md) file for configuration the application. HOCON accepts JSON. Without a configuration, imisu will not start!

This is an example configuration. When there are defaults specified, you can leave them out.

```hocon
{
  "exposeFullApi": false,                         # !!!ONLY USE FOR DEBUGGING PURPOSES!!! Exposes a complete restful API, so you can see all configured services and routes. Default: false
  "exposeSwagger": false,                         # Exposes a swagger-ui endpoint with OpenAPI 3 capability under /swagger-ui. Default: false
  "serverPort": 8080                              # Port on which imisu runs. Default: 8080
  "services": {
    "exampleDns": {                               # Name of the service to be configured. It will be available at /services/exampleDns/health
      "dnsServer": "8.8.8.8",                     # DNS server which will be queried
      "dnsServerPort": "53",                      # Port on which the DNS server will be queried. Default: 53
      "dnsDomain": "example.org",                 # Domain, which the imisu DNS client will ask for. Default: example.org
      "enabled": true                             # Specifies, if the service is enabled
    },
    "exampleHttp": {
      "httpEndpoint": "https://example.org",      # HTTP server, which will be queried
      "enabled": false,
      "validateSsl": true                         # Specifies, if imisu validates the ssl certificate of the HTTP server. Default: true
    },
    "examplePing": {
      "pingServer": "1.1.1.1",                    # IP, which will be queried with a Ping
      "timeout": "1000"                           # Timout for the Ping in ms. Default: 1000
      "enabled": false
    },
  }
}
```

**DO NOT use `"exposeFullApi": true` in a production environment**. It will show the complete configuration of imisu, including internal IPs and URLs.

Note, that imisu determines, which type of service you configured via the keywords `dnsServer`, `httpEndpoint` and `pingServer`.
When you try to mix `httpEndpoint` and `dnsDomain` in one service, the configuration is invalid, and the application will not start.

## Run

To understand, which endpoints you can call, have a look at the [api-docs](https://breucode.github.io/imisu/swagger-ui.html?url=swagger.json).

Currently, imisu is only distributed as a Docker image. Feel free to raise an issue, if you need a different artifact.

### Docker

`docker run -p 8080:8080 -v /path/to/imisu.conf:/imisu.conf:ro ghcr.io/breucode/imisu:stable`
